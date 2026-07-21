package com.tnf.auth_service.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.tnf.auth_service.repository.UserRepository;

/**
 * Loads users for authentication by delegating to {@link UserRepository}.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.debug("Loading user details for username: {}", username);
        return userRepository.findByUsername(username)
                .map(CustomUserDetails::from)
                .orElseThrow(() -> {
                    // DEBUG (not WARN): a missing user is a routine failed-login case; the resulting
                    // BadCredentialsException is logged at WARN by AuthServiceImpl.login.
                    logger.debug("No user found with username: {}", username);
                    return new UsernameNotFoundException("No user found with username: " + username);
                });
    }
}
