package com.guitu.service;

import com.guitu.domain.User;
import com.guitu.dto.UserDtos;
import com.guitu.exception.BusinessException;
import com.guitu.mapper.DtoMapper;
import com.guitu.repository.AnimalRepository;
import com.guitu.repository.RescueRepository;
import com.guitu.repository.UserRepository;
import com.guitu.security.SecuritySupport;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final AnimalRepository animalRepository;
    private final RescueRepository rescueRepository;
    private final PasswordEncoder passwordEncoder;
    private final DtoMapper mapper;

    public UserService(UserRepository userRepository, AnimalRepository animalRepository, RescueRepository rescueRepository, PasswordEncoder passwordEncoder, DtoMapper mapper) {
        this.userRepository = userRepository;
        this.animalRepository = animalRepository;
        this.rescueRepository = rescueRepository;
        this.passwordEncoder = passwordEncoder;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public UserDtos.UserProfile currentProfile() {
        return mapper.toUserProfile(currentUser());
    }

    @Transactional
    public UserDtos.UserProfile updateCurrent(UserDtos.UpdateProfileRequest request) {
        User user = currentUser();
        if (request.phone() != null && !request.phone().isBlank() && !request.phone().equals(user.getPhone())) {
            if (userRepository.existsByPhone(request.phone())) {
                throw new BusinessException("手机号已被注册");
            }
            user.setPhone(request.phone());
        }
        user.setNickname(request.nickname());
        user.setAvatarUrl(request.avatarUrl());
        return mapper.toUserProfile(user);
    }

    @Transactional
    public void changePassword(UserDtos.ChangePasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }
        User user = currentUser();
        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new BusinessException("原密码错误");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    @Transactional(readOnly = true)
    public UserDtos.PublicUserProfile getPublicProfile(Long id) {
        User user = getById(id);
        return new UserDtos.PublicUserProfile(
                user.getId(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getRole(),
                user.getRole().getLabel(),
                animalRepository.countByPublisherId(user.getId()),
                rescueRepository.countByPublisherId(user.getId()),
                user.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new BusinessException("用户不存在"));
    }

    @Transactional(readOnly = true)
    public User currentUser() {
        Long userId = SecuritySupport.requireUser().id();
        return getById(userId);
    }
}
