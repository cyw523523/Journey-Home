package com.guitu.config;

import com.guitu.domain.User;
import com.guitu.domain.enums.UserRole;
import com.guitu.domain.enums.UserStatus;
import com.guitu.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrapRunner implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.bootstrap-enabled:true}")
    private boolean bootstrapEnabled;

    @Value("${app.admin.account:admin}")
    private String account;

    @Value("${app.admin.password:Admin123456}")
    private String password;

    @Value("${app.admin.phone:19900000000}")
    private String phone;

    @Value("${app.admin.nickname:系统管理员}")
    private String nickname;

    public AdminBootstrapRunner(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!bootstrapEnabled || userRepository.existsByAccount(account) || userRepository.existsByPhone(phone)) {
            return;
        }
        User admin = new User();
        admin.setAccount(account);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setPhone(phone);
        admin.setNickname(nickname);
        admin.setRole(UserRole.ADMIN);
        admin.setStatus(UserStatus.NORMAL);
        userRepository.save(admin);
    }
}
