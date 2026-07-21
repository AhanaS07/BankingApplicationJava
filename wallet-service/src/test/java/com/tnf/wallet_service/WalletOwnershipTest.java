package com.tnf.wallet_service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.tnf.common_dto.dto.common.ApiResponse;
import com.tnf.common_dto.dto.customer.CustomerDto;
import com.tnf.common_dto.dto.wallet.CreateWalletRequest;
import com.tnf.wallet_service.client.CustomerClient;
import com.tnf.wallet_service.controller.WalletController;
import com.tnf.wallet_service.entity.Wallet;
import com.tnf.wallet_service.entity.WalletType;
import com.tnf.wallet_service.exception.CustomerNotFoundException;
import com.tnf.wallet_service.exception.CustomerServiceUnavailableException;
import com.tnf.wallet_service.exception.UnauthorizedWalletAccessException;
import com.tnf.wallet_service.exception.WalletNotFoundException;
import com.tnf.wallet_service.repositories.WalletRepo;
import com.tnf.wallet_service.service.WalletService;

import feign.FeignException;

/**
 * Unit tests for the two protections added to wallet-service:
 *  - existence check: createWallet verifies the customer exists in customer-service (service layer);
 *  - ownership check: a caller may only act on their own wallets (controller layer).
 */
class WalletOwnershipTest {

    private CreateWalletRequest req(String customerId) {
        return CreateWalletRequest.builder()
                .customerId(customerId).walletType("PAYTM").openingBalance(BigDecimal.ZERO).build();
    }

    private Wallet walletOwnedBy(String customerId) {
        Wallet w = new Wallet();
        w.setCustomerId(customerId);
        w.setWalletType(WalletType.PAYTM);
        w.setBalance(BigDecimal.TEN);
        return w;
    }

    // ---------- existence check (service -> Feign customer-service) ----------

    @Test
    void createWallet_succeeds_whenCustomerExists() {
        WalletRepo repo = mock(WalletRepo.class);
        CustomerClient client = mock(CustomerClient.class);
        CustomerDto cust = new CustomerDto();
        cust.setId("cust-1");
        when(client.getCustomer("cust-1")).thenReturn(ApiResponse.success("ok", cust));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WalletService service = new WalletService(repo, client);
        Wallet w = service.createWallet("cust-1", WalletType.PAYTM, BigDecimal.ZERO);

        assertEquals("cust-1", w.getCustomerId());
        verify(client).getCustomer("cust-1");
        verify(repo).save(any());
    }

    @Test
    void createWallet_rejects_whenCustomerMissing() {
        WalletRepo repo = mock(WalletRepo.class);
        CustomerClient client = mock(CustomerClient.class);
        when(client.getCustomer("ghost")).thenThrow(mock(FeignException.NotFound.class));

        WalletService service = new WalletService(repo, client);
        assertThrows(CustomerNotFoundException.class,
                () -> service.createWallet("ghost", WalletType.PAYTM, BigDecimal.ZERO));
        verify(repo, never()).save(any());
    }

    @Test
    void createWallet_rejects_whenCustomerServiceDown() {
        WalletRepo repo = mock(WalletRepo.class);
        CustomerClient client = mock(CustomerClient.class);
        when(client.getCustomer(any())).thenThrow(mock(FeignException.class));

        WalletService service = new WalletService(repo, client);
        assertThrows(CustomerServiceUnavailableException.class,
                () -> service.createWallet("cust-1", WalletType.PAYTM, BigDecimal.ZERO));
        verify(repo, never()).save(any());
    }

    // ---------- ownership check (controller) ----------

    @Test
    void createWallet_forbidsCreatingForAnotherCustomer() {
        WalletService service = mock(WalletService.class);
        WalletController controller = new WalletController(service);
        assertThrows(UnauthorizedWalletAccessException.class,
                () -> controller.createWallet(req("victim"), "attacker"));
        verifyNoInteractions(service);
    }

    @Test
    void createWallet_forbidsWhenNoIdentityHeader() {
        WalletService service = mock(WalletService.class);
        WalletController controller = new WalletController(service);
        assertThrows(UnauthorizedWalletAccessException.class,
                () -> controller.createWallet(req("cust-1"), null));
        verifyNoInteractions(service);
    }

    @Test
    void getWalletById_allowsReadingOwnWallet() {
        WalletService service = mock(WalletService.class);
        when(service.getWalletById("W1")).thenReturn(walletOwnedBy("owner"));
        WalletController controller = new WalletController(service);
        assertEquals(200, controller.getWalletById("W1", "owner").getStatusCode().value());
    }

    @Test
    void getWalletById_hidesAnotherCustomersWallet_asNotFound() {
        // A wallet owned by someone else must be indistinguishable from a non-existent one
        // (both 404 with the same message), so the status code can't be used to enumerate ids.
        WalletService service = mock(WalletService.class);
        when(service.getWalletById("W1")).thenReturn(walletOwnedBy("owner"));
        WalletController controller = new WalletController(service);
        WalletNotFoundException ex = assertThrows(WalletNotFoundException.class,
                () -> controller.getWalletById("W1", "attacker"));
        assertEquals("Wallet not found with id: W1", ex.getMessage());
    }

    @Test
    void getWalletById_forbidsWhenNoIdentityHeader() {
        // No gateway identity at all is still a 403 (the request didn't come through the gateway),
        // and this reveals nothing about whether the wallet exists.
        WalletService service = mock(WalletService.class);
        WalletController controller = new WalletController(service);
        assertThrows(UnauthorizedWalletAccessException.class, () -> controller.getWalletById("W1", null));
        verify(service, never()).getWalletById(any());
    }

    @Test
    void getAllWallets_isScopedToCaller_notEveryWallet() {
        WalletService service = mock(WalletService.class);
        when(service.getWalletsByCustomerId("owner")).thenReturn(List.of());
        WalletController controller = new WalletController(service);

        controller.getAllWallets("owner");

        verify(service).getWalletsByCustomerId("owner");
        verify(service, never()).getAllWallets();
    }
}
