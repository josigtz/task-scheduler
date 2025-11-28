package com.example.scheduler.service;

import com.example.scheduler.model.User;
import com.example.scheduler.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DynamicTaskSchedulerService schedulerService;
    
    @PostConstruct
    public void initialize() {
        // Create default admin user if not exists
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123")); // Change this in production!
            admin.setEmail("admin@example.com");
            admin.setEnabled(true);
            userRepository.save(admin);
            log.info("Default admin user created (username: admin, password: admin123)");
        }
        
        // Initialize scheduled tasks
        schedulerService.initializeScheduledTasks();
    }
}
