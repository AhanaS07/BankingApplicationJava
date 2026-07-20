package com.tnf.account.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tnf.account.dto.AmountRequest;
import com.tnf.account.dto.CreateAccountRequest;
import com.tnf.account.dto.TransferRequest;
import com.tnf.account.service.AccountService;
import com.tnf.common_dto.dto.account.BankAccountDto;
import com.tnf.common_dto.dto.account.TransactionDto;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<BankAccountDto> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createAccount(request));
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<BankAccountDto> getAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getAccount(accountNumber));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<BankAccountDto>> getAccountsByCustomer(@PathVariable String customerId) {
        return ResponseEntity.ok(accountService.getAccountsByCustomer(customerId));
    }

    @PostMapping("/{accountNumber}/deposit")
    public ResponseEntity<BankAccountDto> deposit(@PathVariable String accountNumber,
                                                  @Valid @RequestBody AmountRequest request) {
        return ResponseEntity.ok(accountService.deposit(accountNumber, request.getAmount()));
    }

    @PostMapping("/{accountNumber}/withdraw")
    public ResponseEntity<BankAccountDto> withdraw(@PathVariable String accountNumber,
                                                   @Valid @RequestBody AmountRequest request) {
        return ResponseEntity.ok(accountService.withdraw(accountNumber, request.getAmount()));
    }

    @PostMapping("/{accountNumber}/transfer")
    public ResponseEntity<Void> transfer(@PathVariable String accountNumber,
                                         @Valid @RequestBody TransferRequest request) {
        accountService.transfer(accountNumber, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{accountNumber}/transactions")
    public ResponseEntity<List<TransactionDto>> getTransactions(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getTransactionHistory(accountNumber));
    }
}
