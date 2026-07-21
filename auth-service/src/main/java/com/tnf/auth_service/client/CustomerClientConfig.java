package com.tnf.auth_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import feign.RequestInterceptor;

/**
 * Feign configuration scoped to {@link CustomerClient} (referenced via
 * {@code @FeignClient(configuration = CustomerClientConfig.class)}).
 *
 * <p>Attaches the shared internal API key to every outbound call so customer-service can tell this
 * trusted service-to-service provisioning request apart from an external, JWT-bearing client. The
 * gateway strips any client-supplied {@code X-Internal-Api-Key}, so the header can only originate here.
 *
 * <p>Intentionally NOT annotated with {@code @Configuration}: as a Feign client configuration it must
 * stay out of the main component scan, otherwise it would become the default for every Feign client.
 */
public class CustomerClientConfig {

    @Bean
    public RequestInterceptor internalApiKeyInterceptor(
            @Value("${security.internal.api-key}") String internalApiKey) {
        return template -> template.header("X-Internal-Api-Key", internalApiKey);
    }
}
