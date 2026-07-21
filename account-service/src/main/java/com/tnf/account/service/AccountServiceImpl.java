package com.tnf.account.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;

import com.tnf.account.client.CustomerClient;
import com.tnf.account.exception.AccountNotFoundException;
import com.tnf.account.exception.AccountTransferException;
import com.tnf.account.exception.CustomerNotFoundException;
import com.tnf.account.exception.CustomerServiceUnavailableException;
import com.tnf.account.exception.InsufficientBalanceException;
import com.tnf.account.exception.InvalidAccountOperationException;
import com.tnf.account.model.AccountType;
import com.tnf.account.model.BankAccount;
import com.tnf.account.model.CurrentAccount;
import com.tnf.account.model.SavingsAccount;
import com.tnf.account.model.Transaction;
import com.tnf.account.model.TransactionType;
import com.tnf.account.repository.BankAccountRepository;
import com.tnf.account.repository.TransactionRepository;
import com.tnf.common_dto.dto.account.AccountTransferRequest;
import com.tnf.common_dto.dto.account.BankAccountDto;
import com.tnf.common_dto.dto.account.CreateAccountRequest;
import com.tnf.common_dto.dto.account.TransactionDto;
import com.tnf.common_dto.dto.common.ApiResponse;
import com.tnf.common_dto.dto.customer.CustomerDto;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {

    private static final BigDecimal DEFAULT_SAVINGS_MIN_BALANCE = new BigDecimal("500");
    private static final BigDecimal DEFAULT_SAVINGS_INTEREST_RATE = new BigDecimal("0.035");
    private static final BigDecimal DEFAULT_OVERDRAFT_LIMIT = new BigDecimal("1000");

    private final BankAccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final CustomerClient customerClient;

    @Override
    public BankAccountDto createAccount(CreateAccountRequest request) {
        validateCustomerExists(request.getCustomerId());
        AccountType type = AccountType.valueOf(request.getAccountType());
        BigDecimal initial = request.getInitialDeposit() == null ? BigDecimal.ZERO : request.getInitialDeposit();

        BankAccount account = switch (type) {
            case SAVINGS -> buildSavings(request, initial);
            case CURRENT -> buildCurrent(request, initial);
        };

        BankAccount saved = accountRepository.save(account);
        if (initial.compareTo(BigDecimal.ZERO) > 0) {
            recordTransaction(saved.getId(), null, initial, TransactionType.DEPOSIT);
        }
        log.info("Created {} account {} for customer {}", type, saved.getAccountNumber(), saved.getCustomerId());
        return toDto(saved);
    }

    // Verifies the owning customer exists in customer-service before an account is created.
    // Fails closed: a 404 -> CustomerNotFoundException; any other transport error -> unavailable.
    private void validateCustomerExists(String customerId) {
        try {
            ApiResponse<CustomerDto> response = customerClient.getCustomer(customerId);
            if (response == null || response.getData() == null || response.getData().getId() == null) {
                throw new CustomerNotFoundException("Customer " + customerId + " does not exist");
            }
        } catch (FeignException.NotFound ex) {
            throw new CustomerNotFoundException("Customer " + customerId + " does not exist");
        } catch (FeignException ex) {
            throw new CustomerServiceUnavailableException(
                    "customer-service is unavailable; cannot verify customer " + customerId, ex);
        }
    }

    private SavingsAccount buildSavings(CreateAccountRequest request, BigDecimal initial) {
        BigDecimal minBalance = request.getMinimumBalance() == null
                ? DEFAULT_SAVINGS_MIN_BALANCE : request.getMinimumBalance();
        if (initial.compareTo(minBalance) < 0) {
            throw new InvalidAccountOperationException(
                    "Initial deposit " + initial + " is below the required minimum balance " + minBalance);
        }
        return SavingsAccount.builder()
                .accountNumber(generateAccountNumber())
                .customerId(request.getCustomerId())
                .balance(initial)
                .type(AccountType.SAVINGS)
                .minimumBalance(minBalance)
                .interestRate(request.getInterestRate() == null
                        ? DEFAULT_SAVINGS_INTEREST_RATE : request.getInterestRate())
                .build();
    }

    private CurrentAccount buildCurrent(CreateAccountRequest request, BigDecimal initial) {
        return CurrentAccount.builder()
                .accountNumber(generateAccountNumber())
                .customerId(request.getCustomerId())
                .balance(initial)
                .type(AccountType.CURRENT)
                .overdraftLimit(request.getOverdraftLimit() == null
                        ? DEFAULT_OVERDRAFT_LIMIT : request.getOverdraftLimit())
                .build();
    }

    @Override
    public BankAccountDto getAccount(String accountNumber) {
        return toDto(findByAccountNumberOrThrow(accountNumber));
    }

    @Override
    public List<BankAccountDto> getAccountsByCustomer(String customerId) {
        return accountRepository.findByCustomerId(customerId).stream().map(this::toDto).toList();
    }

    @Override
    public BankAccountDto deposit(String accountNumber, BigDecimal amount) {
        BankAccount account = findByAccountNumberOrThrow(accountNumber);
        account.setBalance(account.getBalance().add(amount));
        BankAccount saved = accountRepository.save(account);
        recordTransaction(saved.getId(), null, amount, TransactionType.DEPOSIT);
        log.info("Deposited {} into account {}", amount, accountNumber);
        return toDto(saved);
    }

    @Override
    public BankAccountDto withdraw(String accountNumber, BigDecimal amount) {
        BankAccount account = findByAccountNumberOrThrow(accountNumber);
        applyWithdrawal(account, amount);
        BankAccount saved = accountRepository.save(account);
        recordTransaction(saved.getId(), null, amount, TransactionType.WITHDRAWAL);
        log.info("Withdrew {} from account {}", amount, accountNumber);
        return toDto(saved);
    }

    /**
     * Moves funds between two accounts using application-level compensation (mirrors wallet-service):
     * debit the source, then credit the target; if the credit fails, roll the debit back. This keeps
     * transfers working on a standalone MongoDB, which does not permit multi-document transactions.
     */
    @Override
    public void transfer(String sourceAccountNumber, AccountTransferRequest request) {
        log.info("Transferring {} from {} to {}",
                request.getAmount(), sourceAccountNumber, request.getTargetAccountNumber());
        if (sourceAccountNumber.equals(request.getTargetAccountNumber())) {
            throw new InvalidAccountOperationException("Cannot transfer to the same account");
        }
        BankAccount source = findByAccountNumberOrThrow(sourceAccountNumber);
        BankAccount target = findByAccountNumberOrThrow(request.getTargetAccountNumber());

        // Snapshot the source's pre-debit balance so we can roll back if the credit fails.
        BigDecimal sourceSnapshot = source.getBalance();

        // Step 1: debit the source and persist. If this throws, nothing has moved -> clean failure.
        applyWithdrawal(source, request.getAmount());
        accountRepository.save(source);

        // Step 2: credit the target and persist. If this throws, compensate the debit above.
        try {
            target.setBalance(target.getBalance().add(request.getAmount()));
            accountRepository.save(target);
        } catch (RuntimeException creditFailure) {
            log.error("Credit to account {} failed after debiting account {}; rolling back the debit",
                    request.getTargetAccountNumber(), sourceAccountNumber, creditFailure);
            try {
                source.setBalance(sourceSnapshot);
                accountRepository.save(source);
            } catch (RuntimeException rollbackFailure) {
                // Both the credit and its rollback failed: the source is debited with no matching
                // credit. This cannot be auto-healed and needs manual reconciliation.
                log.error("CRITICAL: rollback of debit on account {} failed; balances are inconsistent "
                        + "and require manual reconciliation", sourceAccountNumber, rollbackFailure);
                throw new AccountTransferException(
                        "Transfer failed and the debit could not be rolled back for account " + sourceAccountNumber
                                + ". Manual reconciliation required.",
                        false, rollbackFailure);
            }
            throw new AccountTransferException(
                    "Transfer failed; the debit on account " + sourceAccountNumber + " was rolled back. No money moved.",
                    true, creditFailure);
        }

        recordTransaction(source.getId(), target.getId(), request.getAmount(), TransactionType.TRANSFER);
        recordTransaction(target.getId(), source.getId(), request.getAmount(), TransactionType.TRANSFER);
        log.info("Transfer complete; source account {} balance: {}", sourceAccountNumber, source.getBalance());
    }

    @Override
    public List<TransactionDto> getTransactionHistory(String accountNumber) {
        BankAccount account = findByAccountNumberOrThrow(accountNumber);
        return transactionRepository.findByAccountIdOrderByTimestampDesc(account.getId())
                .stream().map(this::toDto).toList();
    }

    // --- helpers ---------------------------------------------------------------------------

    /**
     * Applies account-type-specific withdrawal rules and updates the in-memory balance.
     * Savings must not drop below its minimum balance; current must not exceed its overdraft.
     */
    private void applyWithdrawal(BankAccount account, BigDecimal amount) {
        BigDecimal newBalance = account.getBalance().subtract(amount);
        if (account instanceof SavingsAccount savings) {
            if (newBalance.compareTo(savings.getMinimumBalance()) < 0) {
                throw new InsufficientBalanceException(
                        "Withdrawal denied: balance would fall below the minimum balance "
                                + savings.getMinimumBalance());
            }
        } else if (account instanceof CurrentAccount current) {
            BigDecimal floor = current.getOverdraftLimit().negate();
            if (newBalance.compareTo(floor) < 0) {
                throw new InsufficientBalanceException(
                        "Withdrawal denied: overdraft limit " + current.getOverdraftLimit() + " exceeded");
            }
        }
        account.setBalance(newBalance);
    }

    private BankAccount findByAccountNumberOrThrow(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
    }

    private void recordTransaction(String accountId, String targetAccountId, BigDecimal amount, TransactionType type) {
        transactionRepository.save(Transaction.builder()
                .accountId(accountId)
                .targetAccountId(targetAccountId)
                .amount(amount)
                .transactionType(type)
                .timestamp(Instant.now())
                .build());
    }

    private String generateAccountNumber() {
        String candidate;
        do {
            candidate = "ACC" + ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L);
        } while (accountRepository.existsByAccountNumber(candidate));
        return candidate;
    }

    private BankAccountDto toDto(BankAccount account) {
        BankAccountDto.BankAccountDtoBuilder builder = BankAccountDto.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .customerId(account.getCustomerId())
                .accountType(account.getType().name())
                .balance(account.getBalance());
        if (account instanceof SavingsAccount savings) {
            builder.minimumBalance(savings.getMinimumBalance())
                    .interestRate(savings.getInterestRate());
        } else if (account instanceof CurrentAccount current) {
            builder.overdraftLimit(current.getOverdraftLimit());
        }
        return builder.build();
    }

    private TransactionDto toDto(Transaction txn) {
        return TransactionDto.builder()
                .id(txn.getId())
                .accountId(txn.getAccountId())
                .targetAccountId(txn.getTargetAccountId())
                .amount(txn.getAmount())
                .transactionType(txn.getTransactionType().name())
                .timestamp(txn.getTimestamp())
                .build();
    }
}
