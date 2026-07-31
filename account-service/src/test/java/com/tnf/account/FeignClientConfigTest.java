package com.tnf.account;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.tnf.account.config.FeignClientConfig;

import feign.RequestInterceptor;
import feign.RequestTemplate;

/**
 * Unit tests for the FeignClientConfig request interceptor.
 *
 * The interceptor reads X-Auth-Customer-Id from the active Spring request context and forwards it
 * onto outgoing Feign calls. Tests drive this by populating RequestContextHolder directly with a
 * MockHttpServletRequest — no Spring context needed.
 */
class FeignClientConfigTest {

    private static final String HEADER = "X-Auth-Customer-Id";

    private final FeignClientConfig config = new FeignClientConfig();

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void propagatesHeader_whenRequestHasValidCustomerId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HEADER, "cust-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        RequestTemplate template = new RequestTemplate();
        config.authHeaderPropagation().apply(template);

        assertTrue(template.headers().containsKey(HEADER));
        assertTrue(template.headers().get(HEADER).contains("cust-1"));
    }

    @Test
    void doesNotAddHeader_whenNoRequestContextIsActive() {
        // No active request (e.g. a background thread or async job) — header must not be injected
        RequestTemplate template = new RequestTemplate();
        config.authHeaderPropagation().apply(template);

        assertFalse(template.headers().containsKey(HEADER));
    }

    @Test
    void doesNotAddHeader_whenHeaderIsAbsentFromRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        // X-Auth-Customer-Id header intentionally omitted
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        RequestTemplate template = new RequestTemplate();
        config.authHeaderPropagation().apply(template);

        assertFalse(template.headers().containsKey(HEADER));
    }

    @Test
    void doesNotAddHeader_whenHeaderIsBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HEADER, "   ");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        RequestTemplate template = new RequestTemplate();
        config.authHeaderPropagation().apply(template);

        assertFalse(template.headers().containsKey(HEADER));
    }
}
