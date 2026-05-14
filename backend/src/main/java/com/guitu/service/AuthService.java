package com.guitu.service;

import com.guitu.domain.User;
import com.guitu.domain.enums.UserStatus;
import com.guitu.dto.AuthDtos;
import com.guitu.dto.UserDtos;
import com.guitu.exception.BusinessException;
import com.guitu.mapper.DtoMapper;
import com.guitu.repository.UserRepository;
import com.guitu.security.TokenService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final DtoMapper mapper;
    private final AntiAbuseService antiAbuseService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            TokenService tokenService,
            DtoMapper mapper,
            AntiAbuseService antiAbuseService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.mapper = mapper;
        this.antiAbuseService = antiAbuseService;
    }

    @Transactional
    public UserDtos.UserProfile register(AuthDtos.RegisterRequest request) {
        antiAbuseService.check("register", request.account(), 5, Duration.ofHours(1), "Too many registration attempts, please try again later");
        if (!request.password().equals(request.confirmPassword())) {
            throw new BusinessException("The two passwords do not match");
        }
        if (userRepository.existsByAccount(request.account())) {
            throw new BusinessException("Account already exists");
        }
        if (userRepository.existsByPhone(request.phone())) {
            throw new BusinessException("Phone number already exists");
        }

        User user = new User();
        user.setAccount(request.account());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPhone(request.phone());
        user.setNickname(request.nickname());
        return mapper.toUserProfile(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        antiAbuseService.check("login", request.account(), 10, Duration.ofMinutes(10), "Too many login attempts, please try again later");
        User user = userRepository.findByAccount(request.account())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Invalid account or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Invalid account or password");
        }
        if (user.getStatus() != UserStatus.NORMAL) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Account is disabled");
        }
        return new AuthDtos.AuthResponse(tokenService.generate(user.getId()), mapper.toUserProfile(user));
    }
}
