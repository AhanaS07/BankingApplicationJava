package com.tnf.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.tnf.account.client.CustomerClient;
import com.tnf.account.exception.AccountNotFoundException;
import com.tnf.account.exception.AccountTransferException;
import com.tnf.account.exception.CustomerNotFoundException;
import com.tnf.account.exception.InsufficientBalanceException;
import com.tnf.account.exception.InvalidAccountOperationException;
import com.tnf.account.model.AccountType;
import com.tnf.account.model.CurrentAccount;
import com.tnf.account.model.SavingsAccount;
import com.tnf.account.model.Transaction;
import com.tnf.account.model.TransactionType;
import com.tnf.account.repository.BankAccountRepository;
import com.tnf.account.repository.TransactionRepository;
import com.tnf.account.service.AccountServiceImpl;
import com.tnf.common_dto.dto.account.AccountTransferRequest;
import com.tnf.common_dto.dto.account.BankAccountDto;
import com.tnf.common_dto.dto.account.CreateAccountRequest;
import com.tnf.common_dto.dto.account.TransactionDto;
import com.tnf.common_dto.dto.common.ApiResponse;
import com.tnf.common_dto.dto.customer.CustomerDto;

/**
 * Unit tests for AccountServiceImpl covering createAccount (SAVINGS rules and deposit recording),
 * deposit, withdraw (savings minimum balance and current overdraft limits), transfer (happy path,
 * same-account guard, not-found cases, insufficient balance, credit failure with rollback,
 * credit failure with rollback failure), getAccountsByCustomer, and getTransactionHistory.
 */
class AccountServiceImplTest {

    private BankAccountRepository accountRepo;
    private TransactionRepository txnRepo;
    private CustomerClient customerClient;
    private AccountServiceImpl service;

    @BeforeEach
    void setUp() {
        accountRepo = mock(BankAccountRepository.class);
        txnRepo = mock(TransactionRepository.class);
        customerClient = mock(CustomerClient.class);
        service = new AccountServiceImpl(accountRepo, txnRepo, customerClient);
        // generateAccountNumber() loops until existsByAccountNumber returns false
        when(accountRepo.existsByAccountNumber(any())).thenReturn(false);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private SavingsAccount savings(String num, String custId, BigDecimal balance, BigDecimal min) {
        return SavingsAccount.builder()
                .id("id-" + num).accountNumber(num).customerId(custId)
                .balance(balance).type(AccountType.SAVINGS)
                .minimumBalance(min).interestRate(new BigDecimal("0.035"))
                .build();
    }

    private CurrentAccount current(String num, String custId, BigDecimal balance, BigDecimal overdraft) {
        return CurrentAccount.builder()
                .id("id-" + num).accountNumber(num).customerId(custId)
                .balance(balance).type(AccountType.CURRENT)
                .overdraftLimit(overdraft)
                .build();
    }

    private void stubCustomerExists(String customerId) {
        CustomerDto cust = new CustomerDto();
        cust.setId(customerId);
        when(customerClient.getCustomer(customerId)).thenReturn(ApiResponse.success("ok", cust));
    }

    // ── createAccount — SAVINGS type ──────────────────────────────────────────

    @Test
    void createSavingsAccount_succeeds_whenDepositMeetsMinimumBalance() {
        stubCustomerExists("cust-1");
        when(accountRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateAccountRequest req = CreateAccountRequest.builder()
                .customerId("cust-1").accountType("SAVINGS")
                .initialDeposit(new BigDecimal("600"))
                .minimumBalance(new BigDecimal("500"))
                .build();

        BankAccountDto dto = service.createAccount(req);

        assertEquals("cust-1", dto.getCustomerId());
        assertEquals("SAVINGS", dto.getAccountType());
        assertEquals(new BigDecimal("600"), dto.getBalance());
        verify(txnRepo).save(any()); // initial deposit transaction must be recorded
    }

    @Test
    void createSavingsAccount_fails_whenDepositBelowExplicitMinimumBalance() {
        stubCustomerExists("cust-1");

        CreateAccountRequest req = CreateAccountRequest.builder()
                .customerId("cust-1").accountType("SAVINGS")
                .initialDeposit(new BigDecimal("300"))
                .minimumBalance(new BigDecimal("500"))
                .build();

        assertThrows(InvalidAccountOperationException.class, () -> service.createAccount(req));
        verify(accountRepo, never()).save(any());
    }

    @Test
    void createSavingsAccount_fails_whenDepositBelowDefaultMinimumBalance() {
        stubCustomerExists("cust-1");
        // Default minimum is 500; deposit of 100 must be rejected
        CreateAccountRequest req = CreateAccountRequest.builder()
                .customerId("cust-1").accountType("SAVINGS")
                .initialDeposit(new BigDecimal("100"))
                .build();

        assertThrows(InvalidAccountOperationException.class, () -> service.createAccount(req));
        verify(accountRepo, never()).save(any());
    }

    @Test
    void createCurrentAccount_recordsDepositTransaction_whenInitialDepositIsPositive() {
        stubCustomerExists("cust-1");
        when(accountRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateAccountRequest req = CreateAccountRequest.builder()
                .customerId("cust-1").accountType("CURRENT")
                .initialDeposit(new BigDecimal("200"))
                .build();

        service.createAccount(req);

        verify(txnRepo).save(any());
    }

    @Test
    void createCurrentAccount_doesNotRecordTransaction_whenInitialDepositIsZero() {
        stubCustomerExists("cust-1");
        when(accountRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateAccountRequest req = CreateAccountRequest.builder()
                .customerId("cust-1").accountType("CURRENT")
                .initialDeposit(BigDecimal.ZERO)
                .build();

        service.createAccount(req);

        verifyNoInteractions(txnRepo);
    }

    // ── deposit ───────────────────────────────────────────────────────────────

    @Test
    void deposit_increasesBalance_andRecordsTransaction() {
        CurrentAccount account = current("ACC1", "cust-1", new BigDecimal("1000"), new BigDecimal("500"));
        when(accountRepo.findByAccountNumber("ACC1")).thenReturn(Optional.of(account));
        when(accountRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BankAccountDto result = service.deposit("ACC1", new BigDecimal("300"));

        assertEquals(new BigDecimal("1300"), result.getBalance());
        verify(txnRepo).save(any());
    }

    @Test
    void deposit_throws_whenAccountNotFound() {
        when(accountRepo.findByAccountNumber("GHOST")).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> service.deposit("GHOST", new BigDecimal("100")));
    }

    // ── withdraw ──────────────────────────────────────────────────────────────

    @Test
    void withdraw_currentAccount_succeeds_withinBalance() {
        CurrentAccount account = current("ACC1", "cust-1", new BigDecimal("1000"), new BigDecimal("500"));
        when(accountRepo.findByAccountNumber("ACC1")).thenReturn(Optional.of(account));
        when(accountRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BankAccountDto result = service.withdraw("ACC1", new BigDecimal("400"));

        assertEquals(new BigDecimal("600"), result.getBalance());
        verify(txnRepo).save(any());
    }

    @Test
    void withdraw_currentAccount_succeeds_intoOverdraft() {
        // overdraft limit = 500 → floor is -500; balance 200 − 600 = -400 (within limit)
        CurrentAccount account = current("ACC1", "cust-1", new BigDecimal("200"), new BigDecimal("500"));
        when(accountRepo.findByAccountNumber("ACC1")).thenReturn(Optional.of(account));
        when(accountRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BankAccountDto result = service.withdraw("ACC1", new BigDecimal("600"));

        assertEquals(new BigDecimal("-400"), result.getBalance());
    }

    @Test
    void withdraw_currentAccount_fails_whenExceedingOverdraftLimit() {
        // overdraft limit = 500 → floor is -500; balance 100 − 700 = -600 (below floor)
        CurrentAccount account = current("ACC1", "cust-1", new BigDecimal("100"), new BigDecimal("500"));
        when(accountRepo.findByAccountNumber("ACC1")).thenReturn(Optional.of(account));

        assertThrows(InsufficientBalanceException.class,
                () -> service.withdraw("ACC1", new BigDecimal("700")));
        verify(accountRepo, never()).save(any());
    }

    @Test
    void withdraw_savingsAccount_succeeds_whenBalanceStaysAtMinimum() {
        // balance 1000, min 500; withdraw 500 → balance lands exactly at minimum (allowed)
        SavingsAccount account = savings("ACC2", "cust-1", new BigDecimal("1000"), new BigDecimal("500"));
        when(accountRepo.findByAccountNumber("ACC2")).thenReturn(Optional.of(account));
        when(accountRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BankAccountDto result = service.withdraw("ACC2", new BigDecimal("500"));

        assertEquals(new BigDecimal("500"), result.getBalance());
    }

    @Test
    void withdraw_savingsAccount_fails_whenBalanceDropsBelowMinimum() {
        // balance 1000, min 500; withdraw 600 → balance would be 400 (below minimum)
        SavingsAccount account = savings("ACC2", "cust-1", new BigDecimal("1000"), new BigDecimal("500"));
        when(accountRepo.findByAccountNumber("ACC2")).thenReturn(Optional.of(account));

        assertThrows(InsufficientBalanceException.class,
                () -> service.withdraw("ACC2", new BigDecimal("600")));
        verify(accountRepo, never()).save(any());
    }

    @Test
    void withdraw_throws_whenAccountNotFound() {
        when(accountRepo.findByAccountNumber("GHOST")).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> service.withdraw("GHOST", new BigDecimal("100")));
    }

    // ── transfer ──────────────────────────────────────────────────────────────

    @Test
    void transfer_succeeds_updatesBalancesAndRecordsBothLegs() {
        CurrentAccount source = current("SRC", "cust-1", new BigDecimal("1000"), new BigDecimal("500"));
        CurrentAccount target = current("TGT", "cust-2", new BigDecimal("200"), new BigDecimal("500"));
        when(accountRepo.findByAccountNumber("SRC")).thenReturn(Optional.of(source));
        when(accountRepo.findByAccountNumber("TGT")).thenReturn(Optional.of(target));
        when(accountRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AccountTransferRequest req = AccountTransferRequest.builder()
                .targetAccountNumber("TGT").amount(new BigDecimal("300")).build();
        service.transfer("SRC", req);

        assertEquals(new BigDecimal("700"), source.getBalance());
        assertEquals(new BigDecimal("500"), target.getBalance());
        verify(txnRepo, times(2)).save(any()); // one record per leg
    }

    @Test
    void transfer_fails_whenSourceEqualsTarget() {
        AccountTransferRequest req = AccountTransferRequest.builder()
                .targetAccountNumber("ACC1").amount(new BigDecimal("100")).build();

        assertThrows(InvalidAccountOperationException.class, () -> service.transfer("ACC1", req));
        verifyNoInteractions(accountRepo);
    }

    @Test
    void transfer_fails_whenSourceNotFound() {
        when(accountRepo.findByAccountNumber("SRC")).thenReturn(Optional.empty());

        AccountTransferRequest req = AccountTransferRequest.builder()
                .targetAccountNumber("TGT").amount(new BigDecimal("100")).build();

        assertThrows(AccountNotFoundException.class, () -> service.transfer("SRC", req));
    }

    @Test
    void transfer_fails_whenTargetNotFound() {
        CurrentAccount source = current("SRC", "cust-1", new BigDecimal("1000"), new BigDecimal("500"));
        when(accountRepo.findByAccountNumber("SRC")).thenReturn(Optional.of(source));
        when(accountRepo.findByAccountNumber("TGT")).thenReturn(Optional.empty());

        AccountTransferRequest req = AccountTransferRequest.builder()
                .targetAccountNumber("TGT").amount(new BigDecimal("100")).build();

        assertThrows(AccountNotFoundException.class, () -> service.transfer("SRC", req));
    }

    @Test
    void transfer_fails_whenInsufficientBalance() {
        // savings min=500, balance=600; transferring 200 would leave 400 < 500
        SavingsAccount source = savings("SRC", "cust-1", new BigDecimal("600"), new BigDecimal("500"));
        CurrentAccount target = current("TGT", "cust-2", new BigDecimal("200"), new BigDecimal("500"));
        when(accountRepo.findByAccountNumber("SRC")).thenReturn(Optional.of(source));
        when(accountRepo.findByAccountNumber("TGT")).thenReturn(Optional.of(target));

        AccountTransferRequest req = AccountTransferRequest.builder()
                .targetAccountNumber("TGT").amount(new BigDecimal("200")).build();

        assertThrows(InsufficientBalanceException.class, () -> service.transfer("SRC", req));
        verify(accountRepo, never()).save(any()); // no debit was persisted
    }

    @Test
    void transfer_rollsBackDebit_whenCreditFails() {
        CurrentAccount source = current("SRC", "cust-1", new BigDecimal("1000"), new BigDecimal("500"));
        CurrentAccount target = current("TGT", "cust-2", new BigDecimal("200"), new BigDecimal("500"));
        when(accountRepo.findByAccountNumber("SRC")).thenReturn(Optional.of(source));
        when(accountRepo.findByAccountNumber("TGT")).thenReturn(Optional.of(target));
        when(accountRepo.save(any()))
                .thenAnswer(inv -> inv.getArgument(0))           // 1st call: source debit succeeds
                .thenThrow(new RuntimeException("DB write error")) // 2nd call: target credit fails
                .thenAnswer(inv -> inv.getArgument(0));          // 3rd call: rollback save succeeds

        AccountTransferRequest req = AccountTransferRequest.builder()
                .targetAccountNumber("TGT").amount(new BigDecimal("300")).build();

        AccountTransferException ex = assertThrows(AccountTransferException.class,
                () -> service.transfer("SRC", req));

        assertTrue(ex.isReconciled(), "debit should have been rolled back (reconciled=true)");
        assertEquals(new BigDecimal("1000"), source.getBalance()); // restored to pre-transfer snapshot
        verify(txnRepo, never()).save(any()); // no transactions recorded for a failed transfer
    }

    @Test
    void transfer_reportsCriticalInconsistency_whenRollbackAlsoFails() {
        CurrentAccount source = current("SRC", "cust-1", new BigDecimal("1000"), new BigDecimal("500"));
        CurrentAccount target = current("TGT", "cust-2", new BigDecimal("200"), new BigDecimal("500"));
        when(accountRepo.findByAccountNumber("SRC")).thenReturn(Optional.of(source));
        when(accountRepo.findByAccountNumber("TGT")).thenReturn(Optional.of(target));
        when(accountRepo.save(any()))
                .thenAnswer(inv -> inv.getArgument(0))               // 1st call: source debit succeeds
                .thenThrow(new RuntimeException("credit error"))     // 2nd call: target credit fails
                .thenThrow(new RuntimeException("rollback error"));  // 3rd call: rollback also fails

        AccountTransferRequest req = AccountTransferRequest.builder()
                .targetAccountNumber("TGT").amount(new BigDecimal("300")).build();

        AccountTransferException ex = assertThrows(AccountTransferException.class,
                () -> service.transfer("SRC", req));

        assertFalse(ex.isReconciled(), "should signal unreconciled — manual intervention required");
        verify(txnRepo, never()).save(any());
    }

    // ── getAccountsByCustomer ─────────────────────────────────────────────────

    @Test
    void getAccountsByCustomer_returnsMappedDtoList() {
        CurrentAccount acc1 = current("ACC1", "cust-1", new BigDecimal("500"), new BigDecimal("1000"));
        SavingsAccount acc2 = savings("ACC2", "cust-1", new BigDecimal("2000"), new BigDecimal("500"));
        when(accountRepo.findByCustomerId("cust-1")).thenReturn(List.of(acc1, acc2));

        List<BankAccountDto> result = service.getAccountsByCustomer("cust-1");

        assertEquals(2, result.size());
        assertEquals("ACC1", result.get(0).getAccountNumber());
        assertEquals("ACC2", result.get(1).getAccountNumber());
    }

    // ── getTransactionHistory ─────────────────────────────────────────────────

    @Test
    void getTransactionHistory_returnsTransactionsForAccount() {
        CurrentAccount account = current("ACC1", "cust-1", new BigDecimal("1000"), new BigDecimal("500"));
        when(accountRepo.findByAccountNumber("ACC1")).thenReturn(Optional.of(account));
        Transaction tx = Transaction.builder()
                .id("tx-1").accountId("id-ACC1")
                .amount(new BigDecimal("200")).transactionType(TransactionType.DEPOSIT)
                .timestamp(Instant.now()).build();
        when(txnRepo.findByAccountIdOrderByTimestampDesc("id-ACC1")).thenReturn(List.of(tx));

        List<TransactionDto> result = service.getTransactionHistory("ACC1");

        assertEquals(1, result.size());
        assertEquals("tx-1", result.get(0).getId());
    }

    @Test
    void getTransactionHistory_throws_whenAccountNotFound() {
        when(accountRepo.findByAccountNumber("GHOST")).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> service.getTransactionHistory("GHOST"));
    }

    // ── getAccount ────────────────────────────────────────────────────────────

    @Test
    void getAccount_returnsDto_forExistingAccount() {
        CurrentAccount account = current("ACC1", "cust-1", new BigDecimal("1000"), new BigDecimal("500"));
        when(accountRepo.findByAccountNumber("ACC1")).thenReturn(Optional.of(account));

        BankAccountDto result = service.getAccount("ACC1");

        assertEquals("ACC1", result.getAccountNumber());
        assertEquals("cust-1", result.getCustomerId());
    }

    @Test
    void getAccount_throws_whenAccountNotFound() {
        when(accountRepo.findByAccountNumber("GHOST")).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> service.getAccount("GHOST"));
    }

    // ── createAccount — additional branch coverage ────────────────────────────

    @Test
    void createCurrentAccount_withNullInitialDeposit_treatsAsZero() {
        stubCustomerExists("cust-1");
        when(accountRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateAccountRequest req = CreateAccountRequest.builder()
                .customerId("cust-1").accountType("CURRENT")
                .initialDeposit(null)
                .build();

        BankAccountDto dto = service.createAccount(req);

        assertEquals(BigDecimal.ZERO, dto.getBalance());
        verifyNoInteractions(txnRepo); // zero deposit → no transaction recorded
    }

    @Test
    void createSavingsAccount_usesCustomInterestRate_whenProvided() {
        stubCustomerExists("cust-1");
        when(accountRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateAccountRequest req = CreateAccountRequest.builder()
                .customerId("cust-1").accountType("SAVINGS")
                .initialDeposit(new BigDecimal("1000"))
                .minimumBalance(new BigDecimal("500"))
                .interestRate(new BigDecimal("0.05"))
                .build();

        BankAccountDto dto = service.createAccount(req);

        assertEquals(new BigDecimal("0.05"), dto.getInterestRate());
    }

    @Test
    void createCurrentAccount_usesCustomOverdraftLimit_whenProvided() {
        stubCustomerExists("cust-1");
        when(accountRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateAccountRequest req = CreateAccountRequest.builder()
                .customerId("cust-1").accountType("CURRENT")
                .initialDeposit(BigDecimal.ZERO)
                .overdraftLimit(new BigDecimal("2000"))
                .build();

        BankAccountDto dto = service.createAccount(req);

        assertEquals(new BigDecimal("2000"), dto.getOverdraftLimit());
    }

    // ── validateCustomerExists — null body branch ─────────────────────────────

    @Test
    void createAccount_throwsCustomerNotFound_whenCustomerServiceReturnsNullBody() {
        // customer-service responds 200 but with a null data payload (treated as non-existent)
        when(customerClient.getCustomer("cust-1")).thenReturn(ApiResponse.success("ok", null));

        CreateAccountRequest req = CreateAccountRequest.builder()
                .customerId("cust-1").accountType("CURRENT")
                .initialDeposit(BigDecimal.ZERO)
                .build();

        assertThrows(CustomerNotFoundException.class, () -> service.createAccount(req));
        verify(accountRepo, never()).save(any());
    }

    // ── generateAccountNumber — collision retry ───────────────────────────────

    @Test
    void createAccount_retriesAccountNumber_whenFirstCandidateAlreadyExists() {
        stubCustomerExists("cust-1");
        // First generated number collides; second one is free
        when(accountRepo.existsByAccountNumber(any())).thenReturn(true).thenReturn(false);
        when(accountRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateAccountRequest req = CreateAccountRequest.builder()
                .customerId("cust-1").accountType("CURRENT")
                .initialDeposit(BigDecimal.ZERO)
                .build();

        BankAccountDto dto = service.createAccount(req);

        assertNotNull(dto.getAccountNumber());
        verify(accountRepo, atLeast(2)).existsByAccountNumber(any());
    }
}
