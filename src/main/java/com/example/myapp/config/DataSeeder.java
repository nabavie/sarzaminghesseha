package com.example.myapp.config;

import com.example.myapp.model.Category;
import com.example.myapp.model.Role;
import com.example.myapp.model.User;
import com.example.myapp.repository.CategoryRepository;
import com.example.myapp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(1) // before TaleSeeder, which needs the categories
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final List<String> DEFAULT_CATEGORIES = List.of(
            "قصه‌های محلی", "افسانه", "لالایی", "پندآموز", "حیوانات", "ماجراجویی", "شعر و ترانه");

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;
    private final String testAdminUsername;
    private final String testAdminPassword;

    public DataSeeder(UserRepository userRepository,
                      CategoryRepository categoryRepository,
                      PasswordEncoder passwordEncoder,
                      @Value("${app.seed.admin-username}") String adminUsername,
                      @Value("${app.seed.admin-password}") String adminPassword,
                      @Value("${app.seed.test-admin-username:}") String testAdminUsername,
                      @Value("${app.seed.test-admin-password:}") String testAdminPassword) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.testAdminUsername = testAdminUsername;
        this.testAdminPassword = testAdminPassword;
    }

    @Override
    public void run(String... args) {
        if (!userRepository.existsByUsername(adminUsername)) {
            User admin = new User(adminUsername, passwordEncoder.encode(adminPassword), "مدیر سرزمین قصه‌ها");
            admin.getRoles().add(Role.LISTENER);
            admin.getRoles().add(Role.ADMIN);
            userRepository.save(admin);
            log.info("Seeded admin user '{}'", adminUsername);
        }
        if (!testAdminUsername.isBlank() && !userRepository.existsByUsername(testAdminUsername)) {
            User admin = new User(testAdminUsername, passwordEncoder.encode(testAdminPassword), "مدیر آزمایشی");
            admin.getRoles().add(Role.LISTENER);
            admin.getRoles().add(Role.ADMIN);
            userRepository.save(admin);
            log.info("Seeded test admin user '{}'", testAdminUsername);
        }
        for (String name : DEFAULT_CATEGORIES) {
            if (categoryRepository.findByName(name).isEmpty()) {
                categoryRepository.save(new Category(name));
                log.info("Seeded category '{}'", name);
            }
        }
    }
}
