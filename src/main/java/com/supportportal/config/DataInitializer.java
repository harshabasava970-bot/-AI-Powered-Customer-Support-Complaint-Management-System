package com.supportportal.config;

import com.supportportal.entity.ComplaintCategory;
import com.supportportal.entity.Role;
import com.supportportal.entity.User;
import com.supportportal.repository.ComplaintCategoryRepository;
import com.supportportal.repository.RoleRepository;
import com.supportportal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Seeds default roles, categories, and an admin account on first startup.
 * All operations are idempotent — safe to run on every restart.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ComplaintCategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        seedRoles();
        seedAdmin();
        seedCategories();
    }

    private void seedRoles() {
        for (Role.RoleName name : Role.RoleName.values()) {
            if (roleRepository.findByName(name).isEmpty()) {
                roleRepository.save(new Role(null, name));
                log.info("Created role: {}", name);
            }
        }
    }

    private void seedAdmin() {
        if (userRepository.existsByUsername("admin")) {
            return;
        }
        Role adminRole = roleRepository.findByName(Role.RoleName.ROLE_ADMIN)
                .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN not found"));

        User admin = User.builder()
                .username("admin")
                .email("admin@supportportal.com")
                .password(passwordEncoder.encode("Admin@1234"))
                .firstName("System")
                .lastName("Admin")
                .enabled(true)
                .roles(Set.of(adminRole))
                .build();
        userRepository.save(admin);
        log.info("Default admin account created (username=admin, password=Admin@1234)");
    }

    private void seedCategories() {
        List<String[]> defaults = List.of(
                new String[]{"Billing",        "Issues related to invoices, charges, and refunds"},
                new String[]{"Payment",         "Issues related to payments, transactions, and deductions"},
                new String[]{"Technical Issue", "Software bugs, crashes, and technical problems"},
                new String[]{"Account",         "Account management, profile updates, and verification"},
                new String[]{"Login/Access",    "Login failures, password resets, and access problems"},
                new String[]{"Delivery",        "Shipping, delivery, and logistics issues"},
                new String[]{"Product",         "Product quality, defects, and replacements"},
                new String[]{"Service",         "Customer service experience and support quality"},
                new String[]{"Other",           "General queries and miscellaneous complaints"}
        );

        for (String[] entry : defaults) {
            if (categoryRepository.findByNameIgnoreCase(entry[0]).isEmpty()) {
                categoryRepository.save(ComplaintCategory.builder()
                        .name(entry[0])
                        .description(entry[1])
                        .active(true)
                        .build());
                log.info("Created category: {}", entry[0]);
            }
        }
    }
}
