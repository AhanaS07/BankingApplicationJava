package com.tnf.wallet_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tnf.common_dto.dto.common.ApiResponse;
import com.tnf.common_dto.dto.customer.CustomerDto;
import com.tnf.wallet_service.client.CustomerClient;
import com.tnf.wallet_service.entity.Wallet;
import com.tnf.wallet_service.entity.WalletType;
import com.tnf.wallet_service.exception.CustomerNotFoundException;
import com.tnf.wallet_service.exception.CustomerServiceUnavailableException;
import com.tnf.wallet_service.exception.InsufficientBalanceException;
import com.tnf.wallet_service.exception.InvalidAmountException;
import com.tnf.wallet_service.exception.WalletLimitExceededException;
import com.tnf.wallet_service.exception.WalletNotFoundException;
import com.tnf.wallet_service.exception.WalletTransferException;
import com.tnf.wallet_service.repositories.WalletRepo;

import feign.FeignException;

/*
The rules under test: MAX_BALANCE = 50000, DAILY_LIMIT = 20000, the daily-spend counter
rolls over on a new calendar day, and a transfer compensates its own debit when the credit fails. 
 */

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    private static final BigDecimal MAX_BALANCE = new BigDecimal("50000");
    private static final BigDecimal DAILY_LIMIT = new BigDecimal("20000");

    @Mock
    private WalletRepo walletRepo;

    @Mock
    private CustomerClient customerClient;

    @InjectMocks
    private WalletService walletService;

    // helpers

    private static Wallet wallet(String walletId, String balance, LocalDate spendDate, String spendTotal) {
        Wallet w = new Wallet();
        w.setWalletId(walletId);
        w.setCustomerId("cust-1");
        w.setWalletType(WalletType.PAYTM);
        w.setBalance(new BigDecimal(balance));
        w.setDailySpendDate(spendDate);
        w.setDailySpendTotal(new BigDecimal(spendTotal));
        return w;
    }

    // Wallet with today's (empty) spend counter — common starting state
    private static Wallet freshWallet(String walletId, String balance) {
        return wallet(walletId, balance, LocalDate.now(), "0");
    }

    private void stubCustomerExists(String customerId) {
        CustomerDto dto = new CustomerDto();
        dto.setId(customerId);
        when(customerClient.getCustomer(customerId)).thenReturn(ApiResponse.success("found", dto));
    }

    private void stubSaveReturnsArgument() {
        when(walletRepo.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // BigDecimal.equals() is scale-sensitive ("800" != "800.00"); compare numerically instead.
    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                () -> "expected " + expected + " but was " + actual);
    }

    // createWallet

    @Nested
    @DisplayName("createWallet")
    class CreateWallet {

        @Test
        @DisplayName("persists a wallet with a zeroed daily counter dated today")
        void createsWalletWithInitialisedDailyCounter() {
            stubCustomerExists("cust-1");
            stubSaveReturnsArgument();

            Wallet created = walletService.createWallet("cust-1", WalletType.PHONEPE, new BigDecimal("100"));

            ArgumentCaptor<Wallet> saved = ArgumentCaptor.forClass(Wallet.class);
            verify(walletRepo).save(saved.capture());
            assertEquals("cust-1", saved.getValue().getCustomerId());
            assertEquals(WalletType.PHONEPE, saved.getValue().getWalletType());
            assertAmount("100", saved.getValue().getBalance());
            assertEquals(LocalDate.now(), saved.getValue().getDailySpendDate());
            assertAmount("0", saved.getValue().getDailySpendTotal());
            assertSame(saved.getValue(), created);
        }

        @Test
        @DisplayName("treats a null opening balance as zero rather than failing")
        void defaultsNullOpeningBalanceToZero() {
            stubCustomerExists("cust-1");
            stubSaveReturnsArgument();

            Wallet created = walletService.createWallet("cust-1", WalletType.PAYTM, null);

            assertAmount("0", created.getBalance());
        }

        @Test
        @DisplayName("accepts an opening balance of exactly MAX_BALANCE")
        void acceptsOpeningBalanceAtMax() {
            stubCustomerExists("cust-1");
            stubSaveReturnsArgument();

            Wallet created = walletService.createWallet("cust-1", WalletType.PAYTM, MAX_BALANCE);

            assertAmount("50000", created.getBalance());
        }

        @Test
        @DisplayName("rejects a negative opening balance")
        void rejectsNegativeOpeningBalance() {
            stubCustomerExists("cust-1");

            assertThrows(InvalidAmountException.class,
                    () -> walletService.createWallet("cust-1", WalletType.PAYTM, new BigDecimal("-1")));
            verify(walletRepo, never()).save(any());
        }

        @Test
        @DisplayName("rejects an opening balance above MAX_BALANCE")
        void rejectsOpeningBalanceOverMax() {
            stubCustomerExists("cust-1");

            assertThrows(InvalidAmountException.class,
                    () -> walletService.createWallet("cust-1", WalletType.PAYTM, MAX_BALANCE.add(BigDecimal.ONE)));
            verify(walletRepo, never()).save(any());
        }
    }

    // customer existence check

    /*
      createWallet fails closed: no wallet is ever persisted for a customer we could not positively
      confirm. Each branch below is a different way that confirmation can fail.
     */

    @Nested
    @DisplayName("createWallet customer verification")
    class CustomerVerification {

        @Test
        @DisplayName("404 from customer-service becomes CustomerNotFoundException")
        void notFoundResponseIsTranslated() {
            when(customerClient.getCustomer("ghost")).thenThrow(mock(FeignException.NotFound.class));

            CustomerNotFoundException ex = assertThrows(CustomerNotFoundException.class,
                    () -> walletService.createWallet("ghost", WalletType.PAYTM, BigDecimal.ZERO));

            assertEquals("Customer ghost does not exist", ex.getMessage());
            verify(walletRepo, never()).save(any());
        }

        @Test
        @DisplayName("any other Feign failure becomes CustomerServiceUnavailableException (fail closed)")
        void transportFailureIsTranslated() {
            when(customerClient.getCustomer("cust-1")).thenThrow(mock(FeignException.class));

            assertThrows(CustomerServiceUnavailableException.class,
                    () -> walletService.createWallet("cust-1", WalletType.PAYTM, BigDecimal.ZERO));
            verify(walletRepo, never()).save(any());
        }

        @Test
        @DisplayName("a null envelope is not treated as a confirmed customer")
        void nullEnvelopeIsRejected() {
            when(customerClient.getCustomer("cust-1")).thenReturn(null);

            assertThrows(CustomerNotFoundException.class,
                    () -> walletService.createWallet("cust-1", WalletType.PAYTM, BigDecimal.ZERO));
            verify(walletRepo, never()).save(any());
        }

        @Test
        @DisplayName("a 200 with no data payload is not treated as a confirmed customer")
        void emptyDataIsRejected() {
            when(customerClient.getCustomer("cust-1")).thenReturn(ApiResponse.success("ok", null));

            assertThrows(CustomerNotFoundException.class,
                    () -> walletService.createWallet("cust-1", WalletType.PAYTM, BigDecimal.ZERO));
            verify(walletRepo, never()).save(any());
        }

        @Test
        @DisplayName("a customer payload without an id is not treated as a confirmed customer")
        void payloadWithoutIdIsRejected() {
            when(customerClient.getCustomer("cust-1")).thenReturn(ApiResponse.success("ok", new CustomerDto()));

            assertThrows(CustomerNotFoundException.class,
                    () -> walletService.createWallet("cust-1", WalletType.PAYTM, BigDecimal.ZERO));
            verify(walletRepo, never()).save(any());
        }
    }

    // reads

    @Nested
    @DisplayName("reads")
    class Reads {

        @Test
        void getAllWalletsReturnsEverythingTheRepoHas() {
            List<Wallet> stored = List.of(freshWallet("W1", "10"), freshWallet("W2", "20"));
            when(walletRepo.findAll()).thenReturn(stored);

            assertEquals(stored, walletService.getAllWallets());
        }

        @Test
        void getWalletByIdReturnsTheStoredWallet() {
            Wallet stored = freshWallet("W1", "10");
            when(walletRepo.findById("W1")).thenReturn(Optional.of(stored));

            assertSame(stored, walletService.getWalletById("W1"));
        }

        @Test
        @DisplayName("getWalletById raises WalletNotFoundException naming the missing id")
        void getWalletByIdThrowsWhenAbsent() {
            when(walletRepo.findById("nope")).thenReturn(Optional.empty());

            WalletNotFoundException ex = assertThrows(WalletNotFoundException.class,
                    () -> walletService.getWalletById("nope"));
            assertEquals("Wallet not found with id: nope", ex.getMessage());
        }

        @Test
        void getWalletsByCustomerIdDelegatesToTheDerivedQuery() {
            List<Wallet> stored = List.of(freshWallet("W1", "10"));
            when(walletRepo.findByCustomerId("cust-1")).thenReturn(stored);

            assertEquals(stored, walletService.getWalletsByCustomerId("cust-1"));
            verify(walletRepo).findByCustomerId("cust-1");
        }

        @Test
        void getWalletsByCustomerIdReturnsEmptyForAnUnknownCustomer() {
            when(walletRepo.findByCustomerId("nobody")).thenReturn(List.of());

            assertTrue(walletService.getWalletsByCustomerId("nobody").isEmpty());
        }
    }

    // addMoney

    @Nested
    @DisplayName("addMoney")
    class AddMoney {

        @Test
        @DisplayName("credits the wallet without touching the daily spend counter")
        void creditsBalanceAndLeavesSpendCounterAlone() {
            Wallet stored = wallet("W1", "100", LocalDate.now(), "500");
            when(walletRepo.findById("W1")).thenReturn(Optional.of(stored));
            stubSaveReturnsArgument();

            Wallet result = walletService.addMoney("W1", new BigDecimal("250"));

            assertAmount("350", result.getBalance());
            // A top-up is not spending, so the daily limit must be unaffected.
            assertAmount("500", result.getDailySpendTotal());
        }

        @Test
        @DisplayName("allows a top-up landing exactly on MAX_BALANCE")
        void allowsTopUpToExactlyMax() {
            when(walletRepo.findById("W1")).thenReturn(Optional.of(freshWallet("W1", "49000")));
            stubSaveReturnsArgument();

            assertAmount("50000", walletService.addMoney("W1", new BigDecimal("1000")).getBalance());
        }

        @Test
        @DisplayName("rejects a top-up that would push the balance one unit over MAX_BALANCE")
        void rejectsTopUpOverMax() {
            when(walletRepo.findById("W1")).thenReturn(Optional.of(freshWallet("W1", "49000")));

            assertThrows(WalletLimitExceededException.class,
                    () -> walletService.addMoney("W1", new BigDecimal("1001")));
            verify(walletRepo, never()).save(any());
        }

        @Test
        @DisplayName("rejects a non-positive amount before loading the wallet")
        void rejectsZeroAndNegativeAmounts() {
            assertThrows(InvalidAmountException.class, () -> walletService.addMoney("W1", BigDecimal.ZERO));
            assertThrows(InvalidAmountException.class, () -> walletService.addMoney("W1", new BigDecimal("-5")));
            // Amount validation short-circuits: no wallet lookup, no write.
            verifyNoInteractions(walletRepo);
        }

        @Test
        void rejectsANullAmount() {
            assertThrows(InvalidAmountException.class, () -> walletService.addMoney("W1", null));
            verifyNoInteractions(walletRepo);
        }

        @Test
        void propagatesWalletNotFound() {
            when(walletRepo.findById("nope")).thenReturn(Optional.empty());

            assertThrows(WalletNotFoundException.class, () -> walletService.addMoney("nope", BigDecimal.TEN));
        }
    }

    // payBill (the debit path)

    @Nested
    @DisplayName("payBill")
    class PayBill {

        @Test
        @DisplayName("debits the balance and accrues against the daily limit")
        void debitsBalanceAndAccruesDailySpend() {
            Wallet stored = wallet("W1", "1000", LocalDate.now(), "300");
            when(walletRepo.findById("W1")).thenReturn(Optional.of(stored));
            stubSaveReturnsArgument();

            Wallet result = walletService.payBill("W1", new BigDecimal("200"));

            assertAmount("800", result.getBalance());
            assertAmount("500", result.getDailySpendTotal());
            verify(walletRepo).save(stored);
        }

        @Test
        @DisplayName("allows spend landing exactly on DAILY_LIMIT")
        void allowsSpendUpToExactlyTheDailyLimit() {
            Wallet stored = wallet("W1", "30000", LocalDate.now(), "19000");
            when(walletRepo.findById("W1")).thenReturn(Optional.of(stored));
            stubSaveReturnsArgument();

            Wallet result = walletService.payBill("W1", new BigDecimal("1000"));

            assertAmount("20000", result.getDailySpendTotal());
        }

        @Test
        @DisplayName("rejects spend one unit past DAILY_LIMIT even when the balance covers it")
        void rejectsSpendOverTheDailyLimit() {
            Wallet stored = wallet("W1", "30000", LocalDate.now(), "19000");
            when(walletRepo.findById("W1")).thenReturn(Optional.of(stored));

            WalletLimitExceededException ex = assertThrows(WalletLimitExceededException.class,
                    () -> walletService.payBill("W1", new BigDecimal("1001")));

            assertTrue(ex.getMessage().contains(DAILY_LIMIT.toString()));
            // Rejected before mutation: the in-memory wallet must be untouched too.
            assertAmount("30000", stored.getBalance());
            assertAmount("19000", stored.getDailySpendTotal());
            verify(walletRepo, never()).save(any());
        }

        @Test
        @DisplayName("resets the daily counter when the last spend was on an earlier day")
        void rollsOverTheDailyCounterOnANewDay() {
            // Yesterday's spend already sat at the limit; today it must not block a new payment.
            Wallet stored = wallet("W1", "5000", LocalDate.now().minusDays(1), "19000");
            when(walletRepo.findById("W1")).thenReturn(Optional.of(stored));
            stubSaveReturnsArgument();

            Wallet result = walletService.payBill("W1", new BigDecimal("2000"));

            assertEquals(LocalDate.now(), result.getDailySpendDate());
            assertAmount("2000", result.getDailySpendTotal());
            assertAmount("3000", result.getBalance());
        }

        @Test
        @DisplayName("rejects a debit larger than the balance")
        void rejectsInsufficientBalance() {
            Wallet stored = freshWallet("W1", "100");
            when(walletRepo.findById("W1")).thenReturn(Optional.of(stored));

            assertThrows(InsufficientBalanceException.class,
                    () -> walletService.payBill("W1", new BigDecimal("100.01")));
            assertAmount("100", stored.getBalance());
            verify(walletRepo, never()).save(any());
        }

        @Test
        @DisplayName("allows a debit that empties the wallet exactly")
        void allowsDebitOfTheEntireBalance() {
            Wallet stored = freshWallet("W1", "100");
            when(walletRepo.findById("W1")).thenReturn(Optional.of(stored));
            stubSaveReturnsArgument();

            assertAmount("0", walletService.payBill("W1", new BigDecimal("100")).getBalance());
        }

        @Test
        void rejectsNonPositiveAmounts() {
            Wallet stored = freshWallet("W1", "100");
            when(walletRepo.findById("W1")).thenReturn(Optional.of(stored));

            assertThrows(InvalidAmountException.class, () -> walletService.payBill("W1", BigDecimal.ZERO));
            assertThrows(InvalidAmountException.class, () -> walletService.payBill("W1", new BigDecimal("-1")));
            verify(walletRepo, never()).save(any());
        }

        @Test
        void propagatesWalletNotFound() {
            when(walletRepo.findById("nope")).thenReturn(Optional.empty());

            assertThrows(WalletNotFoundException.class, () -> walletService.payBill("nope", BigDecimal.TEN));
        }
    }

    // transfer

    // Transfer is 2 separate writes, so what state the wallets are left in when the credit blows up.
    @Nested
    @DisplayName("transfer")
    class Transfer {

        @Test
        @DisplayName("moves money between wallets and returns the debited source")
        void movesMoneyAndReturnsSource() {
            Wallet source = freshWallet("W1", "1000");
            Wallet target = freshWallet("W2", "500");
            when(walletRepo.findById("W1")).thenReturn(Optional.of(source));
            when(walletRepo.findById("W2")).thenReturn(Optional.of(target));
            stubSaveReturnsArgument();

            Wallet returned = walletService.transfer("W1", "W2", new BigDecimal("200"));

            assertSame(source, returned);
            assertAmount("800", source.getBalance());
            assertAmount("200", source.getDailySpendTotal());
            assertAmount("700", target.getBalance());
            // The target is credited, not "spent from": its daily counter must not move.
            assertAmount("0", target.getDailySpendTotal());
            verify(walletRepo).save(source);
            verify(walletRepo).save(target);
        }

        @Test
        @DisplayName("refuses a self-transfer before any lookup")
        void refusesSelfTransfer() {
            InvalidAmountException ex = assertThrows(InvalidAmountException.class,
                    () -> walletService.transfer("W1", "W1", BigDecimal.TEN));

            assertEquals("Cannot transfer to the same wallet", ex.getMessage());
            verifyNoInteractions(walletRepo);
        }

        @Test
        @DisplayName("checks the credit-side cap before debiting, so neither wallet is mutated")
        void refusesWhenTargetWouldExceedMaxBalance() {
            Wallet source = freshWallet("W1", "1000");
            Wallet target = freshWallet("W2", "49900");
            when(walletRepo.findById("W1")).thenReturn(Optional.of(source));
            when(walletRepo.findById("W2")).thenReturn(Optional.of(target));

            assertThrows(WalletLimitExceededException.class,
                    () -> walletService.transfer("W1", "W2", new BigDecimal("200")));

            assertAmount("1000", source.getBalance());
            assertAmount("49900", target.getBalance());
            verify(walletRepo, never()).save(any());
        }

        @Test
        @DisplayName("allows a transfer that fills the target to exactly MAX_BALANCE")
        void allowsTransferFillingTargetToMax() {
            Wallet source = freshWallet("W1", "1000");
            Wallet target = freshWallet("W2", "49900");
            when(walletRepo.findById("W1")).thenReturn(Optional.of(source));
            when(walletRepo.findById("W2")).thenReturn(Optional.of(target));
            stubSaveReturnsArgument();

            walletService.transfer("W1", "W2", new BigDecimal("100"));

            assertAmount("50000", target.getBalance());
        }

        @Test
        void failsWhenTheSourceDoesNotExist() {
            when(walletRepo.findById("W1")).thenReturn(Optional.empty());

            assertThrows(WalletNotFoundException.class,
                    () -> walletService.transfer("W1", "W2", BigDecimal.TEN));
            verify(walletRepo, never()).save(any());
        }

        @Test
        void failsWhenTheTargetDoesNotExist() {
            when(walletRepo.findById("W1")).thenReturn(Optional.of(freshWallet("W1", "1000")));
            when(walletRepo.findById("W2")).thenReturn(Optional.empty());

            assertThrows(WalletNotFoundException.class,
                    () -> walletService.transfer("W1", "W2", BigDecimal.TEN));
            verify(walletRepo, never()).save(any());
        }

        @Test
        @DisplayName("applies the source's own daily limit to the transferred amount")
        void appliesDailyLimitToTheSource() {
            Wallet source = wallet("W1", "30000", LocalDate.now(), "19000");
            Wallet target = freshWallet("W2", "0");
            when(walletRepo.findById("W1")).thenReturn(Optional.of(source));
            when(walletRepo.findById("W2")).thenReturn(Optional.of(target));

            assertThrows(WalletLimitExceededException.class,
                    () -> walletService.transfer("W1", "W2", new BigDecimal("1001")));
            verify(walletRepo, never()).save(any());
        }

        @Test
        @DisplayName("fails cleanly when the source cannot cover the amount")
        void failsOnInsufficientSourceBalance() {
            Wallet source = freshWallet("W1", "100");
            Wallet target = freshWallet("W2", "0");
            when(walletRepo.findById("W1")).thenReturn(Optional.of(source));
            when(walletRepo.findById("W2")).thenReturn(Optional.of(target));

            assertThrows(InsufficientBalanceException.class,
                    () -> walletService.transfer("W1", "W2", new BigDecimal("200")));

            assertAmount("100", source.getBalance());
            assertAmount("0", target.getBalance());
            verify(walletRepo, never()).save(any());
        }

        @Test
        void rejectsANonPositiveAmount() {
            Wallet source = freshWallet("W1", "100");
            Wallet target = freshWallet("W2", "0");
            when(walletRepo.findById("W1")).thenReturn(Optional.of(source));
            when(walletRepo.findById("W2")).thenReturn(Optional.of(target));

            assertThrows(InvalidAmountException.class,
                    () -> walletService.transfer("W1", "W2", BigDecimal.ZERO));
            verify(walletRepo, never()).save(any());
        }

        @Test
        @DisplayName("compensates the debit when the credit write fails: no money moved, reconciled=true")
        void rollsBackTheDebitWhenTheCreditFails() {
            Wallet source = wallet("W1", "1000", LocalDate.now(), "300");
            Wallet target = freshWallet("W2", "500");
            when(walletRepo.findById("W1")).thenReturn(Optional.of(source));
            when(walletRepo.findById("W2")).thenReturn(Optional.of(target));
            when(walletRepo.save(source)).thenReturn(source);
            when(walletRepo.save(target)).thenThrow(new RuntimeException("mongo write failed"));

            WalletTransferException ex = assertThrows(WalletTransferException.class,
                    () -> walletService.transfer("W1", "W2", new BigDecimal("200")));

            assertTrue(ex.isReconciled(), "the debit was undone, so the failure is safe to retry");
            // Pre-transfer state restored exactly, including the daily-spend accrual.
            assertAmount("1000", source.getBalance());
            assertAmount("300", source.getDailySpendTotal());
            assertEquals(LocalDate.now(), source.getDailySpendDate());
            // Once for the debit, once for the compensating write.
            verify(walletRepo, times(2)).save(source);
        }

        @Test
        @DisplayName("reports reconciled=false when the compensating write also fails")
        void reportsUnreconciledWhenTheRollbackAlsoFails() {
            Wallet source = freshWallet("W1", "1000");
            Wallet target = freshWallet("W2", "500");
            when(walletRepo.findById("W1")).thenReturn(Optional.of(source));
            when(walletRepo.findById("W2")).thenReturn(Optional.of(target));
            when(walletRepo.save(source))
                    .thenReturn(source)                                    // debit persists
                    .thenThrow(new RuntimeException("rollback write failed")); // compensation does not
            when(walletRepo.save(target)).thenThrow(new RuntimeException("credit write failed"));

            WalletTransferException ex = assertThrows(WalletTransferException.class,
                    () -> walletService.transfer("W1", "W2", new BigDecimal("200")));

            assertFalse(ex.isReconciled(), "money left the source and could not be put back");
            assertTrue(ex.getMessage().contains("Manual reconciliation required"));
        }

        @Test
        @DisplayName("a failed debit write is a clean failure, not a transfer exception")
        void aFailedDebitWriteNeedsNoCompensation() {
            Wallet source = freshWallet("W1", "1000");
            Wallet target = freshWallet("W2", "500");
            when(walletRepo.findById("W1")).thenReturn(Optional.of(source));
            when(walletRepo.findById("W2")).thenReturn(Optional.of(target));
            when(walletRepo.save(source)).thenThrow(new RuntimeException("mongo write failed"));

            // Nothing has been persisted yet, so the raw failure propagates (handled as a 500)
            // rather than being dressed up as a partially-completed transfer.
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> walletService.transfer("W1", "W2", new BigDecimal("200")));

            assertFalse(ex instanceof WalletTransferException,
                    "no transfer was started, so there is nothing to reconcile");
            assertEquals("mongo write failed", ex.getMessage());
            verify(walletRepo, never()).save(target);
        }
    }
}
