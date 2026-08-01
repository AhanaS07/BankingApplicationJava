package com.tnf.auth_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tnf.auth_service.entity.User;
import com.tnf.auth_service.exception.RoleNotFoundException;
import com.tnf.auth_service.exception.UserAlreadyExistsException;
import com.tnf.auth_service.exception.UserNotFoundException;
import com.tnf.auth_service.repository.RoleRepository;
import com.tnf.auth_service.repository.UserRepository;
import com.tnf.auth_service.service.impl.UserServiceImpl;
import com.tnf.common_dto.dto.auth.RegisterRequest;
import com.tnf.common_dto.dto.auth.UserResponse;

import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private RegisterRequest request() {
        return RegisterRequest.builder()
                .username("alice")
                .email("alice@example.com")
                .password("Str0ng@Pass")
                .build();
    }

    @Test
    void registerHashesPasswordAndDefaultsRole() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Str0ng@Pass")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User saved = userService.register(request(), "cust-1");

        assertThat(saved.getPassword()).isEqualTo("hashed");
        assertThat(saved.getCustomerId()).isEqualTo("cust-1");
        assertThat(saved.getRoles()).containsExactly("ROLE_USER");
        assertThat(saved.isEnabled()).isTrue();
    }

    @Test
    void registerRejectsDuplicateUsername() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request(), "cust-1"))
                .isInstanceOf(UserAlreadyExistsException.class);
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request(), "cust-1"))
                .isInstanceOf(UserAlreadyExistsException.class);
    }

    @Test
    void registerDefaultsRoleWhenRequestedEmpty() {
        RegisterRequest req = request();
        req.setRoles(Set.of());
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Str0ng@Pass")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User saved = userService.register(req, "cust-1");

        assertThat(saved.getRoles()).containsExactly("ROLE_USER");
    }

    @Test
    void registerRejectsUnknownRole() {
        RegisterRequest req = request();
        req.setRoles(Set.of("ROLE_WIZARD"));
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(roleRepository.existsByName("ROLE_WIZARD")).thenReturn(false);

        assertThatThrownBy(() -> userService.register(req, "cust-1"))
                .isInstanceOf(RoleNotFoundException.class);
    }

    @Test
    void getByUsernameThrowsWhenMissing() {
        when(userRepository.findByUsername(anyString())).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> userService.getByUsername("ghost"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void getByUsernameReturnsUserWhenPresent() {
        User user = User.builder().id("u1").username("alice").build();
        when(userRepository.findByUsername("alice")).thenReturn(java.util.Optional.of(user));

        assertThat(userService.getByUsername("alice")).isSameAs(user);
    }

    @Test
    void registerAcceptsKnownRequestedRole() {
        RegisterRequest req = request();
        req.setRoles(Set.of("ROLE_ADMIN"));
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(roleRepository.existsByName("ROLE_ADMIN")).thenReturn(true);
        when(passwordEncoder.encode("Str0ng@Pass")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User saved = userService.register(req, "cust-1");

        assertThat(saved.getRoles()).containsExactly("ROLE_ADMIN");
    }

    @Test
    void toResponseMapsAllFields() {
        User user = User.builder()
                .id("u1").username("alice").email("alice@example.com")
                .customerId("cust-1").roles(Set.of("ROLE_USER")).enabled(true).build();

        UserResponse response = userService.toResponse(user);

        assertThat(response.getId()).isEqualTo("u1");
        assertThat(response.getUsername()).isEqualTo("alice");
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
        assertThat(response.getCustomerId()).isEqualTo("cust-1");
        assertThat(response.getRoles()).containsExactly("ROLE_USER");
        assertThat(response.isEnabled()).isTrue();
    }
}
