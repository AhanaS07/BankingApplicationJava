package com.tnf.wallet_service.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import feign.RequestInterceptor;
import feign.RequestTemplate;

/*

Outgoing service-to-service calls bypass the gateway, so customer-service can only enforce
ownership if wallet-service forwards the caller's {@code X-Auth-Customer-Id}. Negative case: the 
interceptor must never invent a header when there is no authenticated identity to forward, 
otherwise a blank/absent identity would be sent as a real one.
 
 */

class FeignClientConfigTest {

    private static final String HEADER = "X-Auth-Customer-Id";

    private final RequestInterceptor interceptor = new FeignClientConfig().authHeaderPropagation();

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    private static void bindIncomingRequestWithHeader(String value) {
        MockHttpServletRequest incoming = new MockHttpServletRequest();
        if (value != null) {
            incoming.addHeader(HEADER, value);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(incoming));
    }

    @Test
    @DisplayName("forwards the caller's identity onto the outgoing request")
    void propagatesTheIdentityHeader() {
        bindIncomingRequestWithHeader("cust-1");
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertIterableEquals(java.util.List.of("cust-1"), template.headers().get(HEADER));
    }

    @ParameterizedTest(name = "incoming header = [{0}]")
    @NullSource
    @ValueSource(strings = { "", "   " })
    @DisplayName("sends no identity header when the incoming request carries no usable one")
    void doesNotPropagateAMissingOrBlankIdentity(String incomingValue) {
        bindIncomingRequestWithHeader(incomingValue);
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertFalse(template.headers().containsKey(HEADER));
    }

    @Test
    @DisplayName("is a no-op outside a servlet request (e.g. a scheduled or startup-time call)")
    void doesNothingWithoutARequestContext() {
        RequestContextHolder.resetRequestAttributes();
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertEquals(0, template.headers().size());
    }
}
