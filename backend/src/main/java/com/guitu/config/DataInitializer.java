package com.guitu.config;

import com.guitu.domain.CommunityCategory;
import com.guitu.domain.CommunityPost;
import com.guitu.domain.RescueStation;
import com.guitu.domain.User;
import com.guitu.domain.enums.CertificationStatus;
import com.guitu.domain.enums.UserRole;
import com.guitu.repository.CommunityCategoryRepository;
import com.guitu.repository.CommunityPostRepository;
import com.guitu.repository.RescueStationRepository;
import com.guitu.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final RescueStationRepository stationRepository;
    private final CommunityCategoryRepository categoryRepository;
    private final CommunityPostRepository postRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, RescueStationRepository stationRepository, CommunityCategoryRepository categoryRepository, CommunityPostRepository postRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.stationRepository = stationRepository;
        this.categoryRepository = categoryRepository;
        this.postRepository = postRepository;
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

        if (categoryRepository.count() == 0) {
            CommunityCategory c1 = new CommunityCategory();
            c1.setCode("adoption"); c1.setName("领养经验"); c1.setNameEn("Adoption");
            c1.setDescription("分享领养流程、经验与心得"); c1.setIcon("HeartHandshake"); c1.setSortOrder(1);
            categoryRepository.save(c1);

            CommunityCategory c2 = new CommunityCategory();
            c2.setCode("medical"); c2.setName("医疗护理"); c2.setNameEn("Medical");
            c2.setDescription("宠物健康、疾病防治与护理知识"); c2.setIcon("Stethoscope"); c2.setSortOrder(2);
            categoryRepository.save(c2);

            CommunityCategory c3 = new CommunityCategory();
            c3.setCode("lost"); c3.setName("寻宠送养"); c3.setNameEn("Lost & Found");
            c3.setDescription("发布走失信息或寻找新主人"); c3.setIcon("Search"); c3.setSortOrder(3);
            categoryRepository.save(c3);

            CommunityCategory c4 = new CommunityCategory();
            c4.setCode("rescue"); c4.setName("救助求助"); c4.setNameEn("Rescue");
            c4.setDescription("发布或响应救助请求"); c4.setIcon("Siren"); c4.setSortOrder(4);
            categoryRepository.save(c4);

            CommunityCategory c5 = new CommunityCategory();
            c5.setCode("dailylife"); c5.setName("日常晒宠"); c5.setNameEn("Daily Life");
            c5.setDescription("分享你家宠物的日常照片和趣事"); c5.setIcon("Camera"); c5.setSortOrder(5);
            categoryRepository.save(c5);

            CommunityCategory c6 = new CommunityCategory();
            c6.setCode("chat"); c6.setName("闲聊灌水"); c6.setNameEn("Chat");
            c6.setDescription("随便聊聊，但请保持友善"); c6.setIcon("MessageCircle"); c6.setSortOrder(6);
            categoryRepository.save(c6);
        }

        // Migrate existing posts without category to default "chat" category
        List<CommunityPost> orphanPosts = postRepository.findAll().stream()
            .filter(p -> p.getCategory() == null).toList();
        if (!orphanPosts.isEmpty()) {
            CommunityCategory defaultCategory = categoryRepository.findByCode("chat").orElseThrow();
            for (CommunityPost post : orphanPosts) {
                post.setCategory(defaultCategory);
            }
            postRepository.saveAll(orphanPosts);
            log.info("Migrated {} orphan posts to default category 'chat'", orphanPosts.size());
        }
    }
}
