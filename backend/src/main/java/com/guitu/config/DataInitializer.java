package com.guitu.config;

import com.guitu.domain.RescueStation;
import com.guitu.domain.User;
import com.guitu.domain.enums.CertificationStatus;
import com.guitu.domain.enums.UserRole;
import com.guitu.repository.RescueStationRepository;
import com.guitu.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RescueStationRepository stationRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, RescueStationRepository stationRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.stationRepository = stationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            User admin = new User();
            admin.setAccount("admin");
            admin.setPasswordHash(passwordEncoder.encode("Admin123456"));
            admin.setNickname("系统管理员");
            admin.setPhone("13800138000");
            admin.setRole(UserRole.ADMIN);
            userRepository.save(admin);

            User rescuer1 = new User();
            rescuer1.setAccount("elricklee");
            rescuer1.setPasswordHash(passwordEncoder.encode("Admin123456"));
            rescuer1.setNickname("武大救助站");
            rescuer1.setPhone("17838890338");
            rescuer1.setRole(UserRole.RESCUER);
            userRepository.save(rescuer1);

            User rescuer2 = new User();
            rescuer2.setAccount("aixin");
            rescuer2.setPasswordHash(passwordEncoder.encode("Admin123456"));
            rescuer2.setNickname("爱心救助站");
            rescuer2.setPhone("13900139000");
            rescuer2.setRole(UserRole.RESCUER);
            userRepository.save(rescuer2);

            User rescuer3 = new User();
            rescuer3.setAccount("yangguang");
            rescuer3.setPasswordHash(passwordEncoder.encode("Admin123456"));
            rescuer3.setNickname("阳光宠物救助中心");
            rescuer3.setPhone("13800138001");
            rescuer3.setRole(UserRole.RESCUER);
            userRepository.save(rescuer3);
        }

        if (stationRepository.count() == 0) {
            User rescuer1 = userRepository.findByAccount("elricklee").orElse(null);
            if (rescuer1 != null) {
                RescueStation station1 = new RescueStation();
                station1.setUser(rescuer1);
                station1.setStationName("武大救助站");
                station1.setDescription("武汉大学流浪动物救助站，致力于校园流浪动物的救助与保护。我们提供医疗救助、领养服务和志愿者培训。");
                station1.setAddress("武汉大学信息学部");
                station1.setContactPhone("17838890338");
                station1.setImageUrl("https://neeko-copilot.bytedance.net/api/text_to_image?prompt=cute%20dog%20animal%20rescue%20station&image_size=landscape_4_3");
                station1.setCertificationStatus(CertificationStatus.APPROVED);
                station1.setFollowerCount(0);
                stationRepository.save(station1);
            }

            User rescuer2 = userRepository.findByAccount("aixin").orElse(null);
            if (rescuer2 != null) {
                RescueStation station2 = new RescueStation();
                station2.setUser(rescuer2);
                station2.setStationName("爱心救助站");
                station2.setDescription("成立于2018年，专注于流浪猫狗的救助、寄养和领养工作。");
                station2.setAddress("武汉市洪山区");
                station2.setContactPhone("13900139000");
                station2.setCertificationStatus(CertificationStatus.APPROVED);
                station2.setFollowerCount(0);
                stationRepository.save(station2);
            }

            User rescuer3 = userRepository.findByAccount("yangguang").orElse(null);
            if (rescuer3 != null) {
                RescueStation station3 = new RescueStation();
                station3.setUser(rescuer3);
                station3.setStationName("阳光宠物救助中心");
                station3.setDescription("专业的宠物救助机构，提供流浪动物收容、医疗和领养服务。");
                station3.setAddress("武汉市武昌区");
                station3.setContactPhone("13800138001");
                station3.setCertificationStatus(CertificationStatus.APPROVED);
                station3.setFollowerCount(0);
                stationRepository.save(station3);
            }
        }
    }
}
