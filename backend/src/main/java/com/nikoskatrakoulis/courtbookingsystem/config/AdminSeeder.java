package com.nikoskatrakoulis.courtbookingsystem.config;

import com.nikoskatrakoulis.courtbookingsystem.entity.Role;
import com.nikoskatrakoulis.courtbookingsystem.entity.User;
import com.nikoskatrakoulis.courtbookingsystem.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public AdminSeeder(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${admin.seed.email}") String adminEmail,
            @Value("${admin.seed.password}") String adminPassword
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {

        if (userRepository.existsByEmail(adminEmail)) {
            log.info("Admin user already exists, skipping seed.");
            return;
        }

        User admin = new User();
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setFirstName("ADMIN");
        admin.setLastName("User");
        admin.setRole(Role.ADMIN);

        userRepository.save(admin);
    }
}
