package com.tnf.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import com.tnf.account.controller.AccountController;
import com.tnf.account.exception.AccountNotFoundException;
import com.tnf.account.exception.UnauthorizedAccountAccessException;
import com.tnf.account.service.AccountService;
import com.tnf.common_dto.dto.account.AccountTransferRequest;
import com.tnf.common_dto.dto.account.AmountRequest;
import com.tnf.common_dto.dto.account.BankAccountDto;
import com.tnf.common_dto.dto.account.CreateAccountRequest;
import com.tnf.common_dto.dto.account.TransactionDto;
import com.tnf.common_dto.dto.common.ApiResponse;

/**
 * Unit tests for the AccountController endpoints not covered by AccountOwnershipTest:
 * getAccountsByCustomer, deposit, withdraw, transfer, and getTransactions.
 *
 * Each test exercises the ownership / authentication guard in isolation, without Spring context.
 */
class AccountControllerTest {

    // ── requireAuthenticated — blank header branch ────────────────────────────

    @Test
    void createAccount_forbidsWhenBlankHeader() {
        // A non-null but whitespace-only header must be treated the same as a missing one
        AccountService service = mock(AccountService.class);
        AccountController controller = new AccountController(service);

        assertThrows(UnauthorizedAccountAccessException.class,
                () -> controller.createAccount(
                        CreateAccountRequest.builder()
                                .customerId("cust-1").accountType("CURRENT")
                                .initialDeposit(BigDecimal.ZERO).build(),
                        "   ")); // blank, not null
        verifyNoInteractions(service);
    }

    // ── getAccountsByCustomer ─────────────────────────────────────────────────

    @Test
    void getAccountsByCustomer_allowsOwner() {
        AccountService service = mock(AccountService.class);
        when(service.getAccountsByCustomer("cust-1")).thenReturn(
                List.of(BankAccountDto.builder().customerId("cust-1").accountNumber("ACC1").build()));
        AccountController controller = new AccountController(service);

        ResponseEntity<ApiResponse<List<BankAccountDto>>> resp =
                controller.getAccountsByCustomer("cust-1", "cust-1");

        assertEquals(200, resp.getStatusCode().value());
        verify(service).getAccountsByCustomer("cust-1");
    }

    @Test
    void getAccountsByCustomer_forbidsOtherCustomer() {
        AccountService service = mock(AccountService.class);
        AccountController controller = new AccountController(service);

        assertThrows(UnauthorizedAccountAccessException.class,
                () -> controller.getAccountsByCustomer("victim", "attacker"));
        verifyNoInteractions(service);
    }

    @Test
    void getAccountsByCustomer_forbidsWhenNoHeader() {
        AccountService service = mock(AccountService.class);
        AccountController controller = new AccountController(service);

        assertThrows(UnauthorizedAccountAccessException.class,
                () -> controller.getAccountsByCustomer("cust-1", null));
        verifyNoInteractions(service);
    }

    // ── deposit ───────────────────────────────────────────────────────────────

    @Test
    void deposit_allowsOwnerOfAccount() {
        AccountService service = mock(AccountService.class);
        BankAccountDto ownedAccount = BankAccountDto.builder()
                .customerId("owner").accountNumber("ACC1").build();
        when(service.getAccount("ACC1")).thenReturn(ownedAccount);
        when(service.deposit(eq("ACC1"), any())).thenReturn(ownedAccount);
        AccountController controller = new AccountController(service);

        ResponseEntity<ApiResponse<BankAccountDto>> resp =
                controller.deposit("ACC1", new AmountRequest(new BigDecimal("100")), "owner");

        assertEquals(200, resp.getStatusCode().value());
        verify(service).deposit(eq("ACC1"), any());
    }

    @Test
    void deposit_forbidsWhenNotOwnerOfAccount() {
        // requireOwnedAccount finds the account but sees a different owner → reports 404
        AccountService service = mock(AccountService.class);
        when(service.getAccount("ACC1")).thenReturn(
                BankAccountDto.builder().customerId("owner").accountNumber("ACC1").build());
        AccountController controller = new AccountController(service);

        assertThrows(AccountNotFoundException.class,
                () -> controller.deposit("ACC1", new AmountRequest(new BigDecimal("100")), "attacker"));
        verify(service, never()).deposit(any(), any());
    }

    @Test
    void deposit_forbidsWhenNoHeader() {
        AccountService service = mock(AccountService.class);
        AccountController controller = new AccountController(service);

        assertThrows(UnauthorizedAccountAccessException.class,
                () -> controller.deposit("ACC1", new AmountRequest(new BigDecimal("100")), null));
        verify(service, never()).deposit(any(), any());
    }

    // ── withdraw ──────────────────────────────────────────────────────────────

    @Test
    void withdraw_allowsOwnerOfAccount() {
        AccountService service = mock(AccountService.class);
        BankAccountDto ownedAccount = BankAccountDto.builder()
                .customerId("owner").accountNumber("ACC1").build();
        when(service.getAccount("ACC1")).thenReturn(ownedAccount);
        when(service.withdraw(eq("ACC1"), any())).thenReturn(ownedAccount);
        AccountController controller = new AccountController(service);

        ResponseEntity<ApiResponse<BankAccountDto>> resp =
                controller.withdraw("ACC1", new AmountRequest(new BigDecimal("50")), "owner");

        assertEquals(200, resp.getStatusCode().value());
        verify(service).withdraw(eq("ACC1"), any());
    }

    @Test
    void withdraw_forbidsWhenNotOwnerOfAccount() {
        AccountService service = mock(AccountService.class);
        when(service.getAccount("ACC1")).thenReturn(
                BankAccountDto.builder().customerId("owner").accountNumber("ACC1").build());
        AccountController controller = new AccountController(service);

        assertThrows(AccountNotFoundException.class,
                () -> controller.withdraw("ACC1", new AmountRequest(new BigDecimal("50")), "attacker"));
        verify(service, never()).withdraw(any(), any());
    }

    @Test
    void withdraw_forbidsWhenNoHeader() {
        AccountService service = mock(AccountService.class);
        AccountController controller = new AccountController(service);

        assertThrows(UnauthorizedAccountAccessException.class,
                () -> controller.withdraw("ACC1", new AmountRequest(new BigDecimal("50")), null));
        verify(service, never()).withdraw(any(), any());
    }

    // ── transfer ──────────────────────────────────────────────────────────────

    @Test
    void transfer_allowsOwnerOfSourceAccount() {
        AccountService service = mock(AccountService.class);
        BankAccountDto sourceAccount = BankAccountDto.builder()
                .customerId("owner").accountNumber("SRC").build();
        // getAccount is called twice: once in requireOwnedAccount, once to return the updated source
        when(service.getAccount("SRC")).thenReturn(sourceAccount);
        AccountController controller = new AccountController(service);

        AccountTransferRequest req = AccountTransferRequest.builder()
                .targetAccountNumber("TGT").amount(new BigDecimal("100")).build();

        ResponseEntity<ApiResponse<BankAccountDto>> resp =
                controller.transfer("SRC", req, "owner");

        assertEquals(200, resp.getStatusCode().value());
        verify(service).transfer("SRC", req);
    }

    @Test
    void transfer_forbidsWhenNotOwnerOfSourceAccount() {
        AccountService service = mock(AccountService.class);
        when(service.getAccount("SRC")).thenReturn(
                BankAccountDto.builder().customerId("owner").accountNumber("SRC").build());
        AccountController controller = new AccountController(service);

        AccountTransferRequest req = AccountTransferRequest.builder()
                .targetAccountNumber("TGT").amount(new BigDecimal("100")).build();

        assertThrows(AccountNotFoundException.class,
                () -> controller.transfer("SRC", req, "attacker"));
        verify(service, never()).transfer(any(), any());
    }

    @Test
    void transfer_forbidsWhenNoHeader() {
        AccountService service = mock(AccountService.class);
        AccountController controller = new AccountController(service);

        AccountTransferRequest req = AccountTransferRequest.builder()
                .targetAccountNumber("TGT").amount(new BigDecimal("100")).build();

        assertThrows(UnauthorizedAccountAccessException.class,
                () -> controller.transfer("SRC", req, null));
        verify(service, never()).transfer(any(), any());
    }

    // ── getTransactions ───────────────────────────────────────────────────────

    @Test
    void getTransactions_allowsOwnerOfAccount() {
        AccountService service = mock(AccountService.class);
        when(service.getAccount("ACC1")).thenReturn(
                BankAccountDto.builder().customerId("owner").accountNumber("ACC1").build());
        when(service.getTransactionHistory("ACC1")).thenReturn(List.of());
        AccountController controller = new AccountController(service);

        ResponseEntity<ApiResponse<List<TransactionDto>>> resp =
                controller.getTransactions("ACC1", "owner");

        assertEquals(200, resp.getStatusCode().value());
        verify(service).getTransactionHistory("ACC1");
    }

    @Test
    void getTransactions_forbidsWhenNotOwnerOfAccount() {
        AccountService service = mock(AccountService.class);
        when(service.getAccount("ACC1")).thenReturn(
                BankAccountDto.builder().customerId("owner").accountNumber("ACC1").build());
        AccountController controller = new AccountController(service);

        assertThrows(AccountNotFoundException.class,
                () -> controller.getTransactions("ACC1", "attacker"));
        verify(service, never()).getTransactionHistory(any());
    }

    @Test
    void getTransactions_forbidsWhenNoHeader() {
        AccountService service = mock(AccountService.class);
        AccountController controller = new AccountController(service);

        assertThrows(UnauthorizedAccountAccessException.class,
                () -> controller.getTransactions("ACC1", null));
        verify(service, never()).getTransactionHistory(any());
    }
}
