package com.tnf.account.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tnf.account.exception.AccountNotFoundException;
import com.tnf.account.exception.UnauthorizedAccountAccessException;
import com.tnf.account.service.AccountService;
import com.tnf.common_dto.dto.account.AccountTransferRequest;
import com.tnf.common_dto.dto.account.AmountRequest;
import com.tnf.common_dto.dto.account.BankAccountDto;
import com.tnf.common_dto.dto.account.CreateAccountRequest;
import com.tnf.common_dto.dto.account.TransactionDto;
import com.tnf.common_dto.dto.common.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<ApiResponse<BankAccountDto>> createAccount(@Valid @RequestBody CreateAccountRequest request,
            @RequestHeader(value = "X-Auth-Customer-Id", required = false) String authCustomerId) {
        logger.info("POST /api/accounts - create {} account for customer {}",
                request.getAccountType(), request.getCustomerId());
        requireOwnership(authCustomerId, request.getCustomerId());
        BankAccountDto account = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account created successfully", account));
    }

    // A caller may only act on accounts belonging to their OWN customer profile. The gateway
    // validates the JWT and injects the authenticated customerId as X-Auth-Customer-Id; we reject
    // any request that targets a different id (or arrives without the header, i.e. not via the gateway).
    private void requireOwnership(String authCustomerId, String requestedCustomerId) {
        requireAuthenticated(authCustomerId);
        if (!authCustomerId.equals(requestedCustomerId)) {
            throw new UnauthorizedAccountAccessException(
                    "You may only access your own account(s)");
        }
    }

    // Ensures the request carries an authenticated identity (i.e. it came through the gateway).
    private void requireAuthenticated(String authCustomerId) {
        if (authCustomerId == null || authCustomerId.isBlank()) {
            throw new UnauthorizedAccountAccessException(
                    "Missing authenticated customer identity; requests must go through the API gateway");
        }
    }

    // Resolves an account by number, but only for its owner. Unlike a customerId, an accountNumber
    // is not the caller's own identity, so ownership can only be checked after the lookup. To avoid
    // leaking which account numbers exist, an account belonging to another customer is reported
    // exactly like a missing one: the same 404 with the identical message, never a distinguishable 403.
    private BankAccountDto requireOwnedAccount(String authCustomerId, String accountNumber) {
        requireAuthenticated(authCustomerId);
        BankAccountDto account = accountService.getAccount(accountNumber); // throws AccountNotFoundException (404) if absent
        if (!authCustomerId.equals(account.getCustomerId())) {
            throw new AccountNotFoundException("Account not found: " + accountNumber);
        }
        return account;
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<ApiResponse<BankAccountDto>> getAccount(@PathVariable String accountNumber,
            @RequestHeader(value = "X-Auth-Customer-Id", required = false) String authCustomerId) {
        logger.info("GET /api/accounts/{} - fetch account", accountNumber);
        BankAccountDto account = requireOwnedAccount(authCustomerId, accountNumber);
        return ResponseEntity.ok(ApiResponse.success("Account fetched successfully", account));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<BankAccountDto>>> getAccountsByCustomer(@PathVariable String customerId,
            @RequestHeader(value = "X-Auth-Customer-Id", required = false) String authCustomerId) {
        logger.info("GET /api/accounts/customer/{} - fetch customer accounts", customerId);
        requireOwnership(authCustomerId, customerId);
        List<BankAccountDto> accounts = accountService.getAccountsByCustomer(customerId);
        return ResponseEntity.ok(ApiResponse.success("Customer accounts fetched successfully", accounts));
    }

    @PostMapping("/{accountNumber}/deposit")
    public ResponseEntity<ApiResponse<BankAccountDto>> deposit(@PathVariable String accountNumber,
                                                              @Valid @RequestBody AmountRequest request,
            @RequestHeader(value = "X-Auth-Customer-Id", required = false) String authCustomerId) {
        logger.info("POST /api/accounts/{}/deposit - amount {}", accountNumber, request.getAmount());
        requireOwnedAccount(authCustomerId, accountNumber);
        return ResponseEntity.ok(ApiResponse.success("Deposit successful",
                accountService.deposit(accountNumber, request.getAmount())));
    }

    @PostMapping("/{accountNumber}/withdraw")
    public ResponseEntity<ApiResponse<BankAccountDto>> withdraw(@PathVariable String accountNumber,
                                                               @Valid @RequestBody AmountRequest request,
            @RequestHeader(value = "X-Auth-Customer-Id", required = false) String authCustomerId) {
        logger.info("POST /api/accounts/{}/withdraw - amount {}", accountNumber, request.getAmount());
        requireOwnedAccount(authCustomerId, accountNumber);
        return ResponseEntity.ok(ApiResponse.success("Withdrawal successful",
                accountService.withdraw(accountNumber, request.getAmount())));
    }

    @PostMapping("/{accountNumber}/transfer")
    public ResponseEntity<ApiResponse<BankAccountDto>> transfer(@PathVariable String accountNumber,
                                                               @Valid @RequestBody AccountTransferRequest request,
            @RequestHeader(value = "X-Auth-Customer-Id", required = false) String authCustomerId) {
        logger.info("POST /api/accounts/{}/transfer - {} to {}",
                accountNumber, request.getAmount(), request.getTargetAccountNumber());
        // Only the SOURCE account must be owned by the caller; the target may belong to anyone.
        requireOwnedAccount(authCustomerId, accountNumber);
        accountService.transfer(accountNumber, request);
        BankAccountDto source = accountService.getAccount(accountNumber);
        return ResponseEntity.ok(ApiResponse.success("Transfer completed successfully", source));
    }

    @GetMapping("/{accountNumber}/transactions")
    public ResponseEntity<ApiResponse<List<TransactionDto>>> getTransactions(@PathVariable String accountNumber,
            @RequestHeader(value = "X-Auth-Customer-Id", required = false) String authCustomerId) {
        logger.info("GET /api/accounts/{}/transactions - fetch transaction history", accountNumber);
        requireOwnedAccount(authCustomerId, accountNumber);
        List<TransactionDto> transactions = accountService.getTransactionHistory(accountNumber);
        return ResponseEntity.ok(ApiResponse.success("Transactions fetched successfully", transactions));
    }
}
