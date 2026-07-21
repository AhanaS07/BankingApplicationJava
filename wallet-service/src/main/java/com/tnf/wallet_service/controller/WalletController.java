package com.tnf.wallet_service.controller;

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

import com.tnf.common_dto.dto.common.ApiResponse;
import com.tnf.wallet_service.exception.UnauthorizedWalletAccessException;
import com.tnf.common_dto.dto.wallet.AddMoneyRequest;
import com.tnf.common_dto.dto.wallet.CreateWalletRequest;
import com.tnf.common_dto.dto.wallet.PayBillRequest;
import com.tnf.common_dto.dto.wallet.TransferRequest;
import com.tnf.common_dto.dto.wallet.WalletDTO;
import com.tnf.wallet_service.entity.Wallet;
import com.tnf.wallet_service.entity.WalletType;
import com.tnf.wallet_service.service.WalletService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/wallets")
public class WalletController {

    private static final Logger logger = LoggerFactory.getLogger(WalletController.class);

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WalletDTO>> createWallet(@Valid @RequestBody CreateWalletRequest request,
            @RequestHeader(value = "X-Auth-Customer-Id", required = false) String authCustomerId) {
        logger.info("POST /api/wallets - create {} wallet for customer {}", request.getWalletType(), request.getCustomerId());
        requireOwnership(authCustomerId, request.getCustomerId());
        Wallet wallet = walletService.createWallet(
                request.getCustomerId(),
                WalletType.valueOf(request.getWalletType()),
                request.getOpeningBalance());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Wallet created successfully", toDto(wallet)));
    }

    // A caller may only open a wallet for their OWN customer profile. The gateway validates the
    // JWT and injects the authenticated customerId as X-Auth-Customer-Id; we reject any request
    // that targets a different id (or arrives without the header, i.e. not via the gateway).
    private void requireOwnership(String authCustomerId, String requestedCustomerId) {
        requireAuthenticated(authCustomerId);
        if (!authCustomerId.equals(requestedCustomerId)) {
            throw new UnauthorizedWalletAccessException(
                    "You may only access your own wallet(s)");
        }
    }

    // Ensures the request carries an authenticated identity (i.e. it came through the gateway).
    private void requireAuthenticated(String authCustomerId) {
        if (authCustomerId == null || authCustomerId.isBlank()) {
            throw new UnauthorizedWalletAccessException(
                    "Missing authenticated customer identity; requests must go through the API gateway");
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WalletDTO>>> getAllWallets(
            @RequestHeader(value = "X-Auth-Customer-Id", required = false) String authCustomerId) {
        logger.info("GET /api/wallets - fetch caller's wallets");
        // Scoped to the caller: this returns only the authenticated customer's own wallets,
        // never every wallet in the system.
        requireAuthenticated(authCustomerId);
        List<WalletDTO> wallets = walletService.getWalletsByCustomerId(authCustomerId).stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Wallets fetched successfully", wallets));
    }

    @GetMapping("/{walletId}")
    public ResponseEntity<ApiResponse<WalletDTO>> getWalletById(@PathVariable String walletId,
            @RequestHeader(value = "X-Auth-Customer-Id", required = false) String authCustomerId) {
        logger.info("GET /api/wallets/{} - fetch wallet", walletId);
        Wallet wallet = walletService.getWalletById(walletId);
        requireOwnership(authCustomerId, wallet.getCustomerId());
        return ResponseEntity.ok(ApiResponse.success("Wallet fetched successfully", toDto(wallet)));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<WalletDTO>>> getWalletsByCustomer(@PathVariable String customerId,
            @RequestHeader(value = "X-Auth-Customer-Id", required = false) String authCustomerId) {
        logger.info("GET /api/wallets/customer/{} - fetch customer wallets", customerId);
        requireOwnership(authCustomerId, customerId);
        List<WalletDTO> wallets = walletService.getWalletsByCustomerId(customerId).stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Customer wallets fetched successfully", wallets));
    }

    @PostMapping("/{walletId}/add-money")
    public ResponseEntity<ApiResponse<WalletDTO>> addMoney(@PathVariable String walletId,
            @Valid @RequestBody AddMoneyRequest request,
            @RequestHeader(value = "X-Auth-Customer-Id", required = false) String authCustomerId) {
        logger.info("POST /api/wallets/{}/add-money - amount {}", walletId, request.getAmount());
        requireOwnership(authCustomerId, walletService.getWalletById(walletId).getCustomerId());
        return ResponseEntity.ok(ApiResponse.success("Money added successfully",
                toDto(walletService.addMoney(walletId, request.getAmount()))));
    }

    @PostMapping("/{walletId}/pay-bill")
    public ResponseEntity<ApiResponse<WalletDTO>> payBill(@PathVariable String walletId,
            @Valid @RequestBody PayBillRequest request,
            @RequestHeader(value = "X-Auth-Customer-Id", required = false) String authCustomerId) {
        logger.info("POST /api/wallets/{}/pay-bill - amount {}", walletId, request.getAmount());
        requireOwnership(authCustomerId, walletService.getWalletById(walletId).getCustomerId());
        return ResponseEntity.ok(ApiResponse.success("Bill paid successfully",
                toDto(walletService.payBill(walletId, request.getAmount()))));
    }

    @PostMapping("/{walletId}/transfer")
    public ResponseEntity<ApiResponse<WalletDTO>> transfer(@PathVariable String walletId,
            @Valid @RequestBody TransferRequest request,
            @RequestHeader(value = "X-Auth-Customer-Id", required = false) String authCustomerId) {
        logger.info("POST /api/wallets/{}/transfer - {} to {}", walletId, request.getAmount(), request.getTargetWalletId());
        // Only the SOURCE wallet must be owned by the caller; the target may belong to anyone.
        requireOwnership(authCustomerId, walletService.getWalletById(walletId).getCustomerId());
        Wallet source = walletService.transfer(walletId, request.getTargetWalletId(), request.getAmount());
        return ResponseEntity.ok(ApiResponse.success("Transfer completed successfully", toDto(source)));
    }

    // Maps the internal Wallet entity to the shared WalletDTO (walletType exposed as a String).
    private WalletDTO toDto(Wallet wallet) {
        return WalletDTO.builder()
                .walletId(wallet.getWalletId())
                .customerId(wallet.getCustomerId())
                .walletType(wallet.getWalletType() != null ? wallet.getWalletType().name() : null)
                .balance(wallet.getBalance())
                .build();
    }
}
