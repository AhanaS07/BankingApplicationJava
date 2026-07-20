package com.tnf.auth_service.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.tnf.auth_service.entity.Role;
import com.tnf.auth_service.repository.RoleRepository;

/**
 * Seeds the baseline roles ({@code ROLE_USER}, {@code ROLE_ADMIN}) on startup if they are absent,
 * so registration can validate requested roles against a known set.
 */
@Component
public class RoleInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RoleInitializer.class);

    private record SeedRole(String name, String description) {
    }

    private static final List<SeedRole> DEFAULT_ROLES = List.of(
            new SeedRole("ROLE_USER", "Standard authenticated banking customer"),
            new SeedRole("ROLE_ADMIN", "Administrative user with elevated privileges"));

    private final RoleRepository roleRepository;

    public RoleInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        DEFAULT_ROLES.forEach(seed -> {
            if (!roleRepository.existsByName(seed.name())) {
                roleRepository.save(Role.builder()
                        .name(seed.name())
                        .description(seed.description())
                        .build());
                log.info("Seeded role {}", seed.name());
            }
        });
    }
}
