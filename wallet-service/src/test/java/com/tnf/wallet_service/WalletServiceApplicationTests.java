package com.tnf.wallet_service;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.tnf.wallet_service.client.CustomerClient;
import com.tnf.wallet_service.controller.WalletController;
import com.tnf.wallet_service.exception.GlobalExceptionHandler;
import com.tnf.wallet_service.service.WalletService;

/**
  Wiring smoke test. Only check that the Spring context actually assembles — in particular that the Feign client interface
  is turned into a bean by @EnableFeignClients and that the @RestControllerAdvice is registered.
  External dependencies are switched off in src/test/resources/application.properties.
 */

@SpringBootTest
class WalletServiceApplicationTests {

	@Autowired
	private WalletController walletController;

	@Autowired
	private WalletService walletService;

	@Autowired
	private CustomerClient customerClient;

	@Autowired
	private GlobalExceptionHandler globalExceptionHandler;

	@Test
	void contextLoadsWithEveryWalletBeanWired() {
		assertNotNull(walletController);
		assertNotNull(walletService);
		assertNotNull(customerClient);
		assertNotNull(globalExceptionHandler);
	}

}
