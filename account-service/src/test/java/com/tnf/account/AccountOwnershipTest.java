package com.tnf.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import com.tnf.account.client.CustomerClient;
import com.tnf.account.controller.AccountController;
import com.tnf.account.exception.AccountNotFoundException;
import com.tnf.account.exception.CustomerNotFoundException;
import com.tnf.account.exception.CustomerServiceUnavailableException;
import com.tnf.account.exception.UnauthorizedAccountAccessException;
import com.tnf.account.repository.BankAccountRepository;
import com.tnf.account.repository.TransactionRepository;
import com.tnf.account.service.AccountService;
import com.tnf.account.service.AccountServiceImpl;
import com.tnf.common_dto.dto.account.BankAccountDto;
import com.tnf.common_dto.dto.account.CreateAccountRequest;
import com.tnf.common_dto.dto.common.ApiResponse;
import com.tnf.common_dto.dto.customer.CustomerDto;

import feign.FeignException;

/**
 * Unit tests for the two protections added to account-service:
 *  - existence check: createAccount verifies the customer exists in customer-service (service layer);
 *  - ownership check: a caller may only act on their own customerId (controller layer).
 */
class AccountOwnershipTest {

    private CreateAccountRequest currentAccountReq(String customerId) {
        return CreateAccountRequest.builder()
                .customerId(customerId)
                .accountType("CURRENT")
                .initialDeposit(BigDecimal.ZERO)
                .build();
    }

    // ---------- existence check (service -> Feign customer-service) ----------

    @Test
    void createAccount_succeeds_whenCustomerExists() {
        BankAccountRepository accountRepo = mock(BankAccountRepository.class);
        TransactionRepository txnRepo = mock(TransactionRepository.class);
        CustomerClient client = mock(CustomerClient.class);
        CustomerDto cust = new CustomerDto();
        cust.setId("cust-1");
        when(client.getCustomer("cust-1")).thenReturn(ApiResponse.success("ok", cust));
        when(accountRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AccountServiceImpl service = new AccountServiceImpl(accountRepo, txnRepo, client);
        BankAccountDto dto = service.createAccount(currentAccountReq("cust-1"));

        assertEquals("cust-1", dto.getCustomerId());
        verify(client).getCustomer("cust-1");
        verify(accountRepo).save(any());
    }

    @Test
    void createAccount_rejects_whenCustomerMissing() {
        BankAccountRepository accountRepo = mock(BankAccountRepository.class);
        TransactionRepository txnRepo = mock(TransactionRepository.class);
        CustomerClient client = mock(CustomerClient.class);
        when(client.getCustomer("ghost")).thenThrow(mock(FeignException.NotFound.class));

        AccountServiceImpl service = new AccountServiceImpl(accountRepo, txnRepo, client);
        assertThrows(CustomerNotFoundException.class, () -> service.createAccount(currentAccountReq("ghost")));
        verify(accountRepo, never()).save(any());
    }

    @Test
    void createAccount_rejects_whenCustomerServiceDown() {
        BankAccountRepository accountRepo = mock(BankAccountRepository.class);
        TransactionRepository txnRepo = mock(TransactionRepository.class);
        CustomerClient client = mock(CustomerClient.class);
        when(client.getCustomer(any())).thenThrow(mock(FeignException.class));

        AccountServiceImpl service = new AccountServiceImpl(accountRepo, txnRepo, client);
        assertThrows(CustomerServiceUnavailableException.class,
                () -> service.createAccount(currentAccountReq("cust-1")));
        verify(accountRepo, never()).save(any());
    }

    // ---------- ownership check (controller) ----------

    @Test
    void createAccount_allowsCreatingForSelf() {
        AccountService service = mock(AccountService.class);
        when(service.createAccount(any()))
                .thenReturn(BankAccountDto.builder().customerId("cust-1").accountNumber("ACC1").build());
        AccountController controller = new AccountController(service);

        ResponseEntity<ApiResponse<BankAccountDto>> resp = controller.createAccount(currentAccountReq("cust-1"), "cust-1");

        assertEquals(201, resp.getStatusCode().value());
        verify(service).createAccount(any());
    }

    @Test
    void createAccount_forbidsCreatingForAnotherCustomer() {
        AccountService service = mock(AccountService.class);
        AccountController controller = new AccountController(service);
        assertThrows(UnauthorizedAccountAccessException.class,
                () -> controller.createAccount(currentAccountReq("victim"), "attacker"));
        verifyNoInteractions(service);
    }

    @Test
    void createAccount_forbidsWhenNoIdentityHeader() {
        AccountService service = mock(AccountService.class);
        AccountController controller = new AccountController(service);
        assertThrows(UnauthorizedAccountAccessException.class,
                () -> controller.createAccount(currentAccountReq("cust-1"), null));
        verifyNoInteractions(service);
    }

    @Test
    void getAccount_allowsReadingOwnAccount() {
        AccountService service = mock(AccountService.class);
        when(service.getAccount("ACC1"))
                .thenReturn(BankAccountDto.builder().customerId("owner").accountNumber("ACC1").build());
        AccountController controller = new AccountController(service);

        ResponseEntity<ApiResponse<BankAccountDto>> resp = controller.getAccount("ACC1", "owner");
        assertEquals(200, resp.getStatusCode().value());
    }

    @Test
    void getAccount_hidesAnotherCustomersAccount_asNotFound() {
        // An account owned by someone else must be indistinguishable from a non-existent one
        // (both 404 with the same message), so the status code can't be used to enumerate numbers.
        AccountService service = mock(AccountService.class);
        when(service.getAccount("ACC1"))
                .thenReturn(BankAccountDto.builder().customerId("owner").accountNumber("ACC1").build());
        AccountController controller = new AccountController(service);

        AccountNotFoundException ex = assertThrows(AccountNotFoundException.class,
                () -> controller.getAccount("ACC1", "attacker"));
        assertEquals("Account not found: ACC1", ex.getMessage());
    }

    @Test
    void getAccount_forbidsWhenNoIdentityHeader() {
        // No gateway identity at all is still a 403 (the request didn't come through the gateway),
        // and this reveals nothing about whether the account exists.
        AccountService service = mock(AccountService.class);
        AccountController controller = new AccountController(service);
        assertThrows(UnauthorizedAccountAccessException.class, () -> controller.getAccount("ACC1", null));
        verify(service, never()).getAccount(any());
    }
}
