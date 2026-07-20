package com.tnf.auth_service.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import com.tnf.auth_service.entity.User;
import com.tnf.auth_service.service.JwtService;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private CustomUserDetailsService userDetailsService;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private JwtAuthenticationFilter filter() {
        return new JwtAuthenticationFilter(jwtService, userDetailsService);
    }

    private User user() {
        return User.builder().id("user-1").username("alice").password("hash")
                .roles(Set.of("ROLE_USER")).enabled(true).build();
    }

    @Test
    void populatesSecurityContextForValidToken() throws Exception {
        when(jwtService.isTokenValid("good")).thenReturn(true);
        when(jwtService.extractUsername("good")).thenReturn("alice");
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(CustomUserDetails.from(user()));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer good");
        MockFilterChain chain = new MockFilterChain();

        filter().doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("alice");
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void leavesContextEmptyForInvalidToken() throws Exception {
        when(jwtService.isTokenValid("bad")).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bad");
        MockFilterChain chain = new MockFilterChain();

        filter().doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtService).isTokenValid("bad");
    }

    @Test
    void ignoresRequestWithoutBearerHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockFilterChain chain = new MockFilterChain();

        filter().doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
