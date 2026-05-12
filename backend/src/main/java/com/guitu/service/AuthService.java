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

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final DtoMapper mapper;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService, DtoMapper mapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.mapper = mapper;
    }

    @Transactional
    public UserDtos.UserProfile register(AuthDtos.RegisterRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }
        if (userRepository.existsByAccount(request.account())) {
            throw new BusinessException("账号已被注册");
        }
        if (userRepository.existsByPhone(request.phone())) {
            throw new BusinessException("手机号已被注册");
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
        User user = userRepository.findByAccount(request.account())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "账号或密码错误"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }
        if (user.getStatus() != UserStatus.NORMAL) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "账号状态异常");
        }
        return new AuthDtos.AuthResponse(tokenService.generate(user.getId()), mapper.toUserProfile(user));
    }
}
