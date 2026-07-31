package com.tnf.wallet_service.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.tnf.common_dto.dto.common.ApiResponse;
import com.tnf.common_dto.dto.wallet.AddMoneyRequest;
import com.tnf.common_dto.dto.wallet.CreateWalletRequest;
import com.tnf.common_dto.dto.wallet.PayBillRequest;
import com.tnf.common_dto.dto.wallet.TransferRequest;
import com.tnf.common_dto.dto.wallet.WalletDTO;
import com.tnf.wallet_service.entity.Wallet;
import com.tnf.wallet_service.entity.WalletType;
import com.tnf.wallet_service.exception.UnauthorizedWalletAccessException;
import com.tnf.wallet_service.exception.WalletNotFoundException;
import com.tnf.wallet_service.service.WalletService;

/**
Jobs are authorization and entity → DTO mapping. Both are tested here against a mocked service

Two distinct rejections are asserted throughout or it would leak data:
    -> no/blank identity, or acting on someone else's customerId → 403 Forbidden;</li>
    -> acting on someone else's walletId → 404, identical to a wallet that doesn't exist, so wallet ids cannot be enumerated by status code
 */

@ExtendWith(MockitoExtension.class)
class WalletControllerTest {

    private static final String OWNER = "cust-owner";
    private static final String ATTACKER = "cust-attacker";
    private static final String WALLET_ID = "W1";

    @Mock
    private WalletService walletService;

    @InjectMocks
    private WalletController walletController;

    // helpers

    private static Wallet wallet(String walletId, String customerId, String balance) {
        Wallet w = new Wallet();
        w.setWalletId(walletId);
        w.setCustomerId(customerId);
        w.setWalletType(WalletType.PAYTM);
        w.setBalance(new BigDecimal(balance));
        return w;
    }

    private static CreateWalletRequest createRequest(String customerId, String walletType) {
        return CreateWalletRequest.builder()
                .customerId(customerId)
                .walletType(walletType)
                .openingBalance(new BigDecimal("100"))
                .build();
    }

    private static <T> T payload(ResponseEntity<ApiResponse<T>> response) {
        return response.getBody().getData();
    }

    // createWallet

    @Nested
    @DisplayName("POST /api/wallets")
    class CreateWallet {

        @Test
        @DisplayName("returns 201 with the created wallet for the caller's own customerId")
        void createsWalletForOwnCustomerId() {
            when(walletService.createWallet(OWNER, WalletType.PAYTM, new BigDecimal("100")))
                    .thenReturn(wallet(WALLET_ID, OWNER, "100"));

            ResponseEntity<ApiResponse<WalletDTO>> response =
                    walletController.createWallet(createRequest(OWNER, "PAYTM"), OWNER);

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertTrue(response.getBody().isSuccess());
            assertEquals("Wallet created successfully", response.getBody().getMessage());
            assertEquals(WALLET_ID, payload(response).getWalletId());
        }

        @Test
        @DisplayName("maps the walletType string onto the enum")
        void mapsWalletTypeStringToEnum() {
            when(walletService.createWallet(OWNER, WalletType.PHONEPE, new BigDecimal("100")))
                    .thenReturn(wallet(WALLET_ID, OWNER, "100"));

            walletController.createWallet(createRequest(OWNER, "PHONEPE"), OWNER);

            verify(walletService).createWallet(OWNER, WalletType.PHONEPE, new BigDecimal("100"));
        }

        @Test
        @DisplayName("an unknown walletType surfaces as IllegalArgumentException (handled as 400)")
        void rejectsUnknownWalletType() {
            assertThrows(IllegalArgumentException.class,
                    () -> walletController.createWallet(createRequest(OWNER, "GPAY"), OWNER));
            verifyNoInteractions(walletService);
        }

        @Test
        @DisplayName("forbids creating a wallet for a different customer")
        void forbidsCreatingForAnotherCustomer() {
            assertThrows(UnauthorizedWalletAccessException.class,
                    () -> walletController.createWallet(createRequest(OWNER, "PAYTM"), ATTACKER));
            verifyNoInteractions(walletService);
        }

        @ParameterizedTest(name = "identity header = [{0}]")
        @NullSource
        @ValueSource(strings = { "", "   " })
        @DisplayName("forbids a request without a usable gateway identity")
        void forbidsMissingOrBlankIdentity(String authCustomerId) {
            assertThrows(UnauthorizedWalletAccessException.class,
                    () -> walletController.createWallet(createRequest(OWNER, "PAYTM"), authCustomerId));
            verifyNoInteractions(walletService);
        }
    }

    // reads

    @Nested
    @DisplayName("GET /api/wallets")
    class GetAllWallets {

        @Test
        @DisplayName("returns only the caller's own wallets, never the whole collection")
        void isScopedToTheCaller() {
            when(walletService.getWalletsByCustomerId(OWNER))
                    .thenReturn(List.of(wallet(WALLET_ID, OWNER, "100"), wallet("W2", OWNER, "250")));

            ResponseEntity<ApiResponse<List<WalletDTO>>> response = walletController.getAllWallets(OWNER);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(List.of(WALLET_ID, "W2"), payload(response).stream().map(WalletDTO::getWalletId).toList());
            // The unscoped "every wallet in the system" query must never be reachable over HTTP.
            verify(walletService, never()).getAllWallets();
        }

        @Test
        void returnsAnEmptyListWhenTheCallerHasNoWallets() {
            when(walletService.getWalletsByCustomerId(OWNER)).thenReturn(List.of());

            assertTrue(payload(walletController.getAllWallets(OWNER)).isEmpty());
        }

        @ParameterizedTest(name = "identity header = [{0}]")
        @NullSource
        @ValueSource(strings = { "", "   " })
        void forbidsMissingOrBlankIdentity(String authCustomerId) {
            assertThrows(UnauthorizedWalletAccessException.class,
                    () -> walletController.getAllWallets(authCustomerId));
            verifyNoInteractions(walletService);
        }
    }

    @Nested
    @DisplayName("GET /api/wallets/{walletId}")
    class GetWalletById {

        @Test
        void returnsTheCallersOwnWallet() {
            when(walletService.getWalletById(WALLET_ID)).thenReturn(wallet(WALLET_ID, OWNER, "100"));

            ResponseEntity<ApiResponse<WalletDTO>> response = walletController.getWalletById(WALLET_ID, OWNER);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(WALLET_ID, payload(response).getWalletId());
        }

        @Test
        @DisplayName("reports another customer's wallet as not found, with the identical message")
        void hidesAnotherCustomersWalletAsNotFound() {
            when(walletService.getWalletById(WALLET_ID)).thenReturn(wallet(WALLET_ID, OWNER, "100"));

            WalletNotFoundException ex = assertThrows(WalletNotFoundException.class,
                    () -> walletController.getWalletById(WALLET_ID, ATTACKER));

            // Byte-identical to the service's own "absent wallet" message, so a 404 body cannot
            // distinguish "exists but not yours" from "does not exist".
            assertEquals("Wallet not found with id: " + WALLET_ID, ex.getMessage());
        }

        @Test
        @DisplayName("propagates a genuine not-found from the service")
        void propagatesNotFoundForAnAbsentWallet() {
            when(walletService.getWalletById("nope"))
                    .thenThrow(new WalletNotFoundException("Wallet not found with id: nope"));

            assertThrows(WalletNotFoundException.class, () -> walletController.getWalletById("nope", OWNER));
        }

        @ParameterizedTest(name = "identity header = [{0}]")
        @NullSource
        @ValueSource(strings = { "", "   " })
        @DisplayName("rejects before the lookup, so it reveals nothing about the wallet's existence")
        void forbidsMissingOrBlankIdentity(String authCustomerId) {
            assertThrows(UnauthorizedWalletAccessException.class,
                    () -> walletController.getWalletById(WALLET_ID, authCustomerId));
            verify(walletService, never()).getWalletById(any());
        }
    }

    @Nested
    @DisplayName("GET /api/wallets/customer/{customerId}")
    class GetWalletsByCustomer {

        @Test
        void returnsWalletsForTheCallersOwnCustomerId() {
            when(walletService.getWalletsByCustomerId(OWNER)).thenReturn(List.of(wallet(WALLET_ID, OWNER, "100")));

            ResponseEntity<ApiResponse<List<WalletDTO>>> response =
                    walletController.getWalletsByCustomer(OWNER, OWNER);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(1, payload(response).size());
        }

        @Test
        @DisplayName("forbids listing another customer's wallets")
        void forbidsListingAnotherCustomersWallets() {
            assertThrows(UnauthorizedWalletAccessException.class,
                    () -> walletController.getWalletsByCustomer(OWNER, ATTACKER));
            verifyNoInteractions(walletService);
        }

        @ParameterizedTest(name = "identity header = [{0}]")
        @NullSource
        @ValueSource(strings = { "", "   " })
        void forbidsMissingOrBlankIdentity(String authCustomerId) {
            assertThrows(UnauthorizedWalletAccessException.class,
                    () -> walletController.getWalletsByCustomer(OWNER, authCustomerId));
            verifyNoInteractions(walletService);
        }
    }

    // ------------------------------------------------------------------ money movement

    @Nested
    @DisplayName("POST /api/wallets/{walletId}/add-money")
    class AddMoney {

        private final AddMoneyRequest request = AddMoneyRequest.builder().amount(new BigDecimal("50")).build();

        @Test
        void topsUpTheCallersOwnWallet() {
            when(walletService.getWalletById(WALLET_ID)).thenReturn(wallet(WALLET_ID, OWNER, "100"));
            when(walletService.addMoney(WALLET_ID, new BigDecimal("50")))
                    .thenReturn(wallet(WALLET_ID, OWNER, "150"));

            ResponseEntity<ApiResponse<WalletDTO>> response =
                    walletController.addMoney(WALLET_ID, request, OWNER);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Money added successfully", response.getBody().getMessage());
            assertEquals(new BigDecimal("150"), payload(response).getBalance());
        }

        @Test
        @DisplayName("cannot top up someone else's wallet")
        void refusesAnotherCustomersWallet() {
            when(walletService.getWalletById(WALLET_ID)).thenReturn(wallet(WALLET_ID, OWNER, "100"));

            assertThrows(WalletNotFoundException.class,
                    () -> walletController.addMoney(WALLET_ID, request, ATTACKER));
            verify(walletService, never()).addMoney(any(), any());
        }

        @Test
        void forbidsMissingIdentity() {
            assertThrows(UnauthorizedWalletAccessException.class,
                    () -> walletController.addMoney(WALLET_ID, request, null));
            verifyNoInteractions(walletService);
        }
    }

    @Nested
    @DisplayName("POST /api/wallets/{walletId}/pay-bill")
    class PayBill {

        private final PayBillRequest request = PayBillRequest.builder().amount(new BigDecimal("40")).build();

        @Test
        void paysFromTheCallersOwnWallet() {
            when(walletService.getWalletById(WALLET_ID)).thenReturn(wallet(WALLET_ID, OWNER, "100"));
            when(walletService.payBill(WALLET_ID, new BigDecimal("40")))
                    .thenReturn(wallet(WALLET_ID, OWNER, "60"));

            ResponseEntity<ApiResponse<WalletDTO>> response =
                    walletController.payBill(WALLET_ID, request, OWNER);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Bill paid successfully", response.getBody().getMessage());
            assertEquals(new BigDecimal("60"), payload(response).getBalance());
        }

        @Test
        @DisplayName("cannot pay a bill out of someone else's wallet")
        void refusesAnotherCustomersWallet() {
            when(walletService.getWalletById(WALLET_ID)).thenReturn(wallet(WALLET_ID, OWNER, "100"));

            assertThrows(WalletNotFoundException.class,
                    () -> walletController.payBill(WALLET_ID, request, ATTACKER));
            verify(walletService, never()).payBill(any(), any());
        }

        @Test
        void forbidsMissingIdentity() {
            assertThrows(UnauthorizedWalletAccessException.class,
                    () -> walletController.payBill(WALLET_ID, request, null));
            verifyNoInteractions(walletService);
        }
    }

    @Nested
    @DisplayName("POST /api/wallets/{walletId}/transfer")
    class Transfer {

        private final TransferRequest request = TransferRequest.builder()
                .targetWalletId("W2").amount(new BigDecimal("30")).build();

        @Test
        @DisplayName("transfers out of the caller's own wallet and returns the debited source")
        void transfersFromTheCallersOwnWallet() {
            when(walletService.getWalletById(WALLET_ID)).thenReturn(wallet(WALLET_ID, OWNER, "100"));
            when(walletService.transfer(WALLET_ID, "W2", new BigDecimal("30")))
                    .thenReturn(wallet(WALLET_ID, OWNER, "70"));

            ResponseEntity<ApiResponse<WalletDTO>> response =
                    walletController.transfer(WALLET_ID, request, OWNER);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Transfer completed successfully", response.getBody().getMessage());
            assertEquals(WALLET_ID, payload(response).getWalletId());
            assertEquals(new BigDecimal("70"), payload(response).getBalance());
        }

        @Test
        @DisplayName("only the source must be owned; the target may belong to anyone")
        void doesNotRequireOwnershipOfTheTarget() {
            when(walletService.getWalletById(WALLET_ID)).thenReturn(wallet(WALLET_ID, OWNER, "100"));
            when(walletService.transfer(WALLET_ID, "W2", new BigDecimal("30")))
                    .thenReturn(wallet(WALLET_ID, OWNER, "70"));

            walletController.transfer(WALLET_ID, request, OWNER);

            // The target is never fetched for an ownership check — only the source is.
            verify(walletService).getWalletById(WALLET_ID);
            verify(walletService, never()).getWalletById("W2");
        }

        @Test
        @DisplayName("cannot transfer out of someone else's wallet")
        void refusesAnotherCustomersSourceWallet() {
            when(walletService.getWalletById(WALLET_ID)).thenReturn(wallet(WALLET_ID, OWNER, "100"));

            assertThrows(WalletNotFoundException.class,
                    () -> walletController.transfer(WALLET_ID, request, ATTACKER));
            verify(walletService, never()).transfer(any(), any(), any());
        }

        @Test
        void forbidsMissingIdentity() {
            assertThrows(UnauthorizedWalletAccessException.class,
                    () -> walletController.transfer(WALLET_ID, request, null));
            verifyNoInteractions(walletService);
        }
    }

    // entity -> DTO mapping

    @Nested
    @DisplayName("WalletDTO mapping")
    class DtoMapping {

        @Test
        @DisplayName("exposes the wallet type as its enum name")
        void mapsEveryFieldOntoTheDto() {
            when(walletService.getWalletById(WALLET_ID)).thenReturn(wallet(WALLET_ID, OWNER, "123.45"));

            WalletDTO dto = payload(walletController.getWalletById(WALLET_ID, OWNER));

            assertEquals(WALLET_ID, dto.getWalletId());
            assertEquals(OWNER, dto.getCustomerId());
            assertEquals("PAYTM", dto.getWalletType());
            assertEquals(new BigDecimal("123.45"), dto.getBalance());
        }

        @Test
        @DisplayName("tolerates a wallet with no type instead of throwing a NullPointerException")
        void mapsANullWalletTypeToNull() {
            Wallet typeless = wallet(WALLET_ID, OWNER, "10");
            typeless.setWalletType(null);
            when(walletService.getWalletById(WALLET_ID)).thenReturn(typeless);

            assertNull(payload(walletController.getWalletById(WALLET_ID, OWNER)).getWalletType());
        }
    }
}
