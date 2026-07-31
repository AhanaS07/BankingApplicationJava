package com.tnf.wallet_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import feign.RequestInterceptor;

/*
 Propagates the gateway-injected identity header (X-Auth-Customer-Id) from the incoming request
 onto outgoing Feign calls. This lets customer-service enforce ownership on service-to-service
 lookups (which bypass the gateway) exactly as it does on end-user traffic.
 */

@Configuration
public class FeignClientConfig {

    private static final String CUSTOMER_ID_HEADER = "X-Auth-Customer-Id";

    @Bean
    RequestInterceptor authHeaderPropagation() {
        return template -> {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
                String customerId = attrs.getRequest().getHeader(CUSTOMER_ID_HEADER);
                if (customerId != null && !customerId.isBlank()) {
                    template.header(CUSTOMER_ID_HEADER, customerId);
                }
            }
        };
    }
}
