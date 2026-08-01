package com.tnf.auth_service.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.tnf.auth_service.entity.User;

class CustomUserDetailsTest {

    private User user(boolean enabled) {
        return User.builder()
                .id("u1")
                .username("alice")
                .password("hashed")
                .roles(Set.of("ROLE_USER", "ROLE_ADMIN"))
                .enabled(enabled)
                .build();
    }

    @Test
    void exposesWrappedUserFields() {
        CustomUserDetails details = CustomUserDetails.from(user(true));

        assertThat(details.getId()).isEqualTo("u1");
        assertThat(details.getUsername()).isEqualTo("alice");
        assertThat(details.getPassword()).isEqualTo("hashed");
        assertThat(details.getUser().getUsername()).isEqualTo("alice");
    }

    @Test
    void mapsRolesToAuthorities() {
        CustomUserDetails details = CustomUserDetails.from(user(true));

        assertThat(details.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void accountFlagsFollowEnabledState() {
        CustomUserDetails enabled = CustomUserDetails.from(user(true));
        assertThat(enabled.isEnabled()).isTrue();
        assertThat(enabled.isAccountNonExpired()).isTrue();
        assertThat(enabled.isAccountNonLocked()).isTrue();
        assertThat(enabled.isCredentialsNonExpired()).isTrue();

        CustomUserDetails disabled = CustomUserDetails.from(user(false));
        assertThat(disabled.isEnabled()).isFalse();
    }
}
