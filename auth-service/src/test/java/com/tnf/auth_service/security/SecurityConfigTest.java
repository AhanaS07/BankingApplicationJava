package com.tnf.auth_service.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

class SecurityConfigTest {

    private final SecurityConfig config = new SecurityConfig(
            mock(JwtAuthenticationFilter.class),
            mock(CustomUserDetailsService.class),
            mock(RestAuthenticationEntryPoint.class));

    @Test
    void passwordEncoderIsBCryptAndVerifiesHashes() {
        PasswordEncoder encoder = config.passwordEncoder();

        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
        String hash = encoder.encode("Str0ng@Pass");
        assertThat(encoder.matches("Str0ng@Pass", hash)).isTrue();
        assertThat(encoder.matches("wrong", hash)).isFalse();
    }

    @Test
    void authenticationProviderIsConfigured() {
        DaoAuthenticationProvider provider = config.authenticationProvider();

        assertThat(provider).isNotNull();
    }

    @Test
    void authenticationManagerDelegatesToConfiguration() throws Exception {
        AuthenticationConfiguration authConfig = mock(AuthenticationConfiguration.class);
        AuthenticationManager manager = mock(AuthenticationManager.class);
        when(authConfig.getAuthenticationManager()).thenReturn(manager);

        assertThat(config.authenticationManager(authConfig)).isSameAs(manager);
    }

    /**
     * Drives {@code securityFilterChain} with a genuinely-constructed {@link HttpSecurity} so the
     * method body and its session/authorization/exception-handling customiser lambdas all execute,
     * and asserts the resulting chain wires in our JWT filter.
     */
    @Test
    void securityFilterChainBuildsStatelessChainWithJwtFilter() throws Exception {
        JwtAuthenticationFilter jwtFilter = mock(JwtAuthenticationFilter.class);
        SecurityConfig chainConfig = new SecurityConfig(
                jwtFilter, mock(CustomUserDetailsService.class), mock(RestAuthenticationEntryPoint.class));

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.refresh();

            ObjectPostProcessor<Object> postProcessor = new ObjectPostProcessor<>() {
                @Override
                public <O> O postProcess(O object) {
                    return object;
                }
            };
            AuthenticationManagerBuilder authBuilder = new AuthenticationManagerBuilder(postProcessor);
            Map<Class<?>, Object> sharedObjects = new HashMap<>();
            sharedObjects.put(ApplicationContext.class, context);
            HttpSecurity http = new HttpSecurity(postProcessor, authBuilder, sharedObjects);

            SecurityFilterChain chain = chainConfig.securityFilterChain(http);

            assertThat(chain).isNotNull();
            assertThat(chain.getFilters()).contains(jwtFilter);
        }
    }
}
