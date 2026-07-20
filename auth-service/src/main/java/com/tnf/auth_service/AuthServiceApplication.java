package com.tnf.auth_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

import com.tnf.auth_service.config.JwtProperties;

/**
 * Entry point for the auth-service.
 *
 * <p>Registers with Eureka as {@code auth-service} (port 8081) and issues / validates the JWTs the
 * other banking microservices trust. {@link EnableFeignClients} enables the {@code customer-service}
 * client used to create a customer profile at registration.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableConfigurationProperties(JwtProperties.class)
public class AuthServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthServiceApplication.class, args);
	}

}
