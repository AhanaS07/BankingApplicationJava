package com.tnf.account.service;

import java.math.BigDecimal;
import java.util.List;

import com.tnf.common_dto.dto.account.AccountTransferRequest;
import com.tnf.common_dto.dto.account.BankAccountDto;
import com.tnf.common_dto.dto.account.CreateAccountRequest;
import com.tnf.common_dto.dto.account.TransactionDto;

/**
 * Banking logic sliced to accounts. Inputs are account-service request DTOs; outputs are the
 * shared cross-service DTOs from {@code common-dto}.
 */
public interface AccountService {

    BankAccountDto createAccount(CreateAccountRequest request);

    BankAccountDto getAccount(String accountNumber);

    List<BankAccountDto> getAccountsByCustomer(String customerId);

    BankAccountDto deposit(String accountNumber, BigDecimal amount);

    BankAccountDto withdraw(String accountNumber, BigDecimal amount);

    void transfer(String sourceAccountNumber, AccountTransferRequest request);

    List<TransactionDto> getTransactionHistory(String accountNumber);
}
