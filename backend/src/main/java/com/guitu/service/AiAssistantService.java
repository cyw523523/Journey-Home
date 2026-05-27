package com.guitu.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guitu.common.PageResponse;
import com.guitu.domain.enums.AnimalGender;
import com.guitu.domain.enums.AnimalStatus;
import com.guitu.domain.enums.AnimalType;
import com.guitu.domain.enums.DonationStatus;
import com.guitu.domain.enums.RescueStatus;
import com.guitu.domain.enums.SupplyCategory;
import com.guitu.domain.enums.VolunteerTaskStatus;
import com.guitu.dto.AiAssistantDtos;
import com.guitu.dto.AnimalDtos;
import com.guitu.dto.CommunityDtos;
import com.guitu.dto.DonationDtos;
import com.guitu.dto.NoticeDtos;
import com.guitu.dto.RescueDtos;
import com.guitu.dto.RescueStationDtos;
import com.guitu.dto.StatsDtos;
import com.guitu.dto.UserDtos;
import com.guitu.dto.VolunteerTaskDtos;
import com.guitu.exception.BusinessException;
import com.guitu.security.SecuritySupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AiAssistantService {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ObjectMapper objectMapper;
    private final StatisticsService statisticsService;
    private final AnimalService animalService;
    private final RescueService rescueService;
    private final NoticeService noticeService;
    private final DonationService donationService;
    private final VolunteerTaskService volunteerTaskService;
    private final RescueStationService rescueStationService;
    private final CommunityService communityService;
    private final UserService userService;

    @Value("${app.ai.zhipu.api-url:https://open.bigmodel.cn/api/paas/v4/chat/completions}")
    private String apiUrl;

    @Value("${app.ai.zhipu.api-key:}")
    private String apiKey;

    @Value("${app.ai.zhipu.model:glm-4-flash-250414}")
    private String model;

    @Value("${app.ai.zhipu.connect-timeout-ms:5000}")
    private int connectTimeoutMs;

    @Value("${app.ai.zhipu.read-timeout-ms:30000}")
    private int readTimeoutMs;

    public AiAssistantService(
            ObjectMapper objectMapper,
            StatisticsService statisticsService,
            AnimalService animalService,
            RescueService rescueService,
            NoticeService noticeService,
            DonationService donationService,
            VolunteerTaskService volunteerTaskService,
            RescueStationService rescueStationService,
            CommunityService communityService,
            UserService userService
    ) {
        this.objectMapper = objectMapper;
        this.statisticsService = statisticsService;
        this.animalService = animalService;
        this.rescueService = rescueService;
        this.noticeService = noticeService;
        this.donationService = donationService;
        this.volunteerTaskService = volunteerTaskService;
        this.rescueStationService = rescueStationService;
        this.communityService = communityService;
        this.userService = userService;
    }

    public AiAssistantDtos.ChatResponse chat(AiAssistantDtos.ChatRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "AI 助手服务暂未配置，请联系管理员");
        }

        String directReply = tryDirectAnswer(request);
        if (hasText(directReply)) {
            return new AiAssistantDtos.ChatResponse(directReply, model, LocalDateTime.now());
        }

        String reply = callModel(request.message(), request.history(), buildSiteContext(request.pageContext()));
        return new AiAssistantDtos.ChatResponse(reply, model, LocalDateTime.now());
    }

    private String tryDirectAnswer(AiAssistantDtos.ChatRequest request) {
        AiAssistantDtos.PageContext pageContext = request.pageContext();
        if (pageContext == null) {
            return null;
        }

        String message = safeLower(request.message());

        String entityReply = answerEntityQuestion(pageContext, message);
        if (hasText(entityReply)) {
            return entityReply;
        }

        if (isPagePurposeQuestion(message)) {
            return answerPagePurpose(pageContext);
        }

        String route = safeRoute(pageContext);
        return switch (route) {
            case "home" -> isHomeQuestion(message) ? answerHomeQuestion(message) : null;
            case "animals" -> isAnimalListQuestion(pageContext, message) ? answerAnimalListQuestion(pageContext, message) : null;
            case "rescues" -> isRescueListQuestion(message) ? answerRescueListQuestion(pageContext, message) : null;
            case "notices" -> isNoticeListQuestion(message) ? answerNoticeListQuestion(pageContext, message) : null;
            case "donations" -> isDonationListQuestion(message) ? answerDonationListQuestion(pageContext, message) : null;
            case "volunteer-tasks" -> isVolunteerListQuestion(message) ? answerVolunteerListQuestion(pageContext, message) : null;
            case "community" -> isCommunityListQuestion(message) ? answerCommunityListQuestion(pageContext, message) : null;
            case "community-category" -> isCommunityCategoryQuestion(message) ? answerCommunityCategoryQuestion(pageContext, message) : null;
            case "messages" -> isMessagePageQuestion(message) ? answerMessagePageQuestion(pageContext) : null;
            case "profile" -> isProfileQuestion(message) ? answerProfileQuestion(pageContext, message) : null;
            case "rescue-station" -> isRescueStationQuestion(message) ? answerRescueStationQuestion(pageContext, message) : null;
            case "user-profile" -> isUserProfileQuestion(message) ? answerUserProfileQuestion(pageContext) : null;
            case "map" -> isMapQuestion(message) ? answerMapQuestion(pageContext) : null;
            default -> null;
        };
    }

    private String answerEntityQuestion(AiAssistantDtos.PageContext pageContext, String message) {
        String route = safeRoute(pageContext);
        String entityType = trimToEmpty(pageContext.entityType()).toUpperCase(Locale.ROOT);

        if ("adoption-new".equals(route) && isAdoptionApplyQuestion(message)) {
            return answerAdoptionApplyQuestion(pageContext);
        }

        return switch (entityType) {
            case "ANIMAL" -> isCurrentAnimalQuestion(route, message) ? answerCurrentAnimalQuestion(pageContext, route) : null;
            case "RESCUE" -> isCurrentRescueQuestion(message) ? answerCurrentRescueQuestion(pageContext) : null;
            case "NOTICE" -> isCurrentNoticeQuestion(message) ? answerCurrentNoticeQuestion(pageContext) : null;
            case "DONATION_DEMAND" -> isCurrentDonationQuestion(message) ? answerCurrentDonationQuestion(pageContext) : null;
            case "VOLUNTEER_TASK" -> isCurrentVolunteerQuestion(message) ? answerCurrentVolunteerQuestion(pageContext) : null;
            case "COMMUNITY_POST" -> isCurrentCommunityPostQuestion(message) ? answerCurrentCommunityPostQuestion(pageContext) : null;
            case "USER_PROFILE" -> isUserProfileQuestion(message) ? answerUserProfileQuestion(pageContext) : null;
            case "RESCUE_STATION" -> isRescueStationQuestion(message) ? answerRescueStationQuestion(pageContext, message) : null;
            default -> null;
        };
    }

    private String answerPagePurpose(AiAssistantDtos.PageContext pageContext) {
        String route = safeRoute(pageContext);
        return switch (route) {
            case "home" -> "你现在在首页，可以先看平台概况、最新待领养动物、最新救助信息和最新公告，再跳转到动物、救助或社区页面继续看。";
            case "animals" -> "你现在在动物档案页，可以按关键字、类型、性别、状态和地区筛选，查看详情后再决定是否申请领养。";
            case "animal-detail" -> "你现在在动物详情页，可以看这只动物的状态、健康情况、医疗记录；如果它处于待领养状态，还可以继续提交领养申请。";
            case "adoption-new" -> "你现在在领养申请页，可以填写申请资料，也可以先生成 AI 领养建议，再正式提交申请。";
            case "rescues" -> "你现在在救助信息页，可以筛选公开救助信息，打开详情看联系方式和进展，也可以自己发布新的救助求助。";
            case "notices" -> "你现在在公告模块，可以看公告列表，进入单条公告查看全文和发布时间。";
            case "notice-detail" -> "你现在在公告详情页，主要就是查看这条公告的完整内容和发布时间。";
            case "donations" -> "你现在在物资捐赠页，可以筛选需求、看进度、查看捐赠记录，也可以直接发起捐赠或发布新的物资需求。";
            case "volunteer-tasks" -> "你现在在志愿任务页，可以筛选任务、查看人数和时间安排，符合条件的话还可以直接报名。";
            case "community" -> "你现在在社区首页，可以按最新、热门、关注排序看帖子，也可以发帖、进详情页看评论互动。";
            case "community-category" -> "你现在在社区分类页，主要是在当前版块里筛选和查看帖子。";
            case "community-detail" -> "你现在在帖子详情页，可以看正文、评论、点赞收藏，也可以继续评论或回复别人。";
            case "messages" -> "你现在在私聊消息页，可以查看会话、未读消息和当前聊天内容，也可以继续发送文字或图片。";
            case "profile" -> "你现在在个人中心，可以维护资料，查看自己发布的数据、领养申请、通知、举报、申诉和密码修改。";
            case "rescue-station" -> "你现在在救助站中心，可以申请认证救助站、查看自己的站点数据，也可以发现并关注其他救助站。";
            case "user-profile" -> "你现在在用户主页，可以看这个用户的公开资料、发布数量，以及它是否有关联的救助站。";
            case "map" -> "你现在在地图找寻页，可以定位当前位置后查看附近动物和救助站点位，还能按距离范围筛选。";
            default -> hasText(pageContext.pageSummary()) ? pageContext.pageSummary() : null;
        };
    }

    private String answerHomeQuestion(String message) {
        StatsDtos.HomeOverviewResponse homeOverview = statisticsService.homeOverview();
        StringBuilder reply = new StringBuilder();
        reply.append("首页当前概况：注册用户 ")
                .append(homeOverview.overview().userCount())
                .append("，动物档案 ")
                .append(homeOverview.overview().animalCount())
                .append("，救助信息 ")
                .append(homeOverview.overview().rescueCount())
                .append("，领养申请 ")
                .append(homeOverview.overview().applyCount())
                .append("，待审核 ")
                .append(homeOverview.overview().pendingAuditCount())
                .append("。");

        if (containsAny(message, "最新动物", "最新领养", "最新档案", "动物")) {
            appendLatestAnimals(reply, homeOverview.latestAnimals());
        }
        if (containsAny(message, "最新救助", "救助")) {
            appendLatestRescues(reply, homeOverview.latestRescues());
        }
        if (containsAny(message, "公告", "通知")) {
            appendLatestNotices(reply, homeOverview.latestNotices());
        }
        return reply.toString();
    }

    private String answerAnimalListQuestion(AiAssistantDtos.PageContext pageContext, String message) {
        AnimalType type = detectAnimalType(message);
        AnimalStatus status = detectAnimalStatus(message);
        Map<String, Object> filters = getMap(pageContext.viewData(), "filters");
        String keyword = getString(filters, "keyword");
        String region = getString(filters, "region");
        AnimalGender gender = parseAnimalGender(getString(filters, "gender"));

        if (type == null) {
            type = parseAnimalType(getString(filters, "type"));
        }
        if (status == null) {
            status = parseAnimalStatus(getString(filters, "status"));
        }
        if (status == null && containsAny(message, "领养", "可领养", "待领养")) {
            status = AnimalStatus.WAITING_ADOPTION;
        }

        PageResponse<AnimalDtos.AnimalResponse> result = animalService.listPublic(
                blankToNull(keyword),
                type,
                gender,
                status,
                blankToNull(region),
                0,
                6
        );

        String targetName = type == AnimalType.CAT ? "猫" : type == AnimalType.DOG ? "狗" : "动物";
        StringBuilder reply = new StringBuilder();
        if (result.content().isEmpty()) {
            reply.append("我按当前页面条件查了一下，暂时没有找到");
            if (hasText(region)) {
                reply.append(region).append("地区的");
            }
            if (status == AnimalStatus.WAITING_ADOPTION) {
                reply.append("待领养");
            }
            reply.append(targetName).append("。");
            return reply.toString();
        }

        reply.append("我按当前页面条件查到了 ")
                .append(result.totalElements())
                .append(" 条相关动物档案，先给你看前 ")
                .append(result.content().size())
                .append(" 条：");

        for (int i = 0; i < result.content().size(); i++) {
            AnimalDtos.AnimalResponse animal = result.content().get(i);
            reply.append("\n")
                    .append(i + 1)
                    .append(". #")
                    .append(animal.id())
                    .append("，")
                    .append(defaultText(animal.typeText(), animalTypeText(animal.type())))
                    .append("，")
                    .append(defaultText(animal.statusText(), animalStatusText(animal.status())))
                    .append("，地区 ")
                    .append(defaultText(animal.foundRegion(), "未填写"))
                    .append("，年龄 ")
                    .append(animal.age() == null ? "未知" : animal.age() + " 岁");
            if (hasText(animal.healthCondition())) {
                reply.append("，健康情况 ").append(animal.healthCondition());
            }
        }
        return reply.toString();
    }

    private String answerCurrentAnimalQuestion(AiAssistantDtos.PageContext pageContext, String route) {
        AnimalDtos.AnimalResponse animal = animalService.detail(pageContext.entityId());
        Integer medicalRecordCount = getInteger(pageContext.viewData(), "medicalRecordCount");
        StringBuilder reply = new StringBuilder();
        reply.append("这只动物的真实档案是：")
                .append("\n1. 编号 #").append(animal.id())
                .append("\n2. 类型：").append(defaultText(animal.typeText(), animalTypeText(animal.type())))
                .append("\n3. 性别：").append(defaultText(animal.genderText(), animalGenderText(animal.gender())))
                .append("\n4. 年龄：").append(animal.age() == null ? "未知" : animal.age() + " 岁")
                .append("\n5. 地区：").append(defaultText(animal.foundRegion(), "未填写"))
                .append("\n6. 健康情况：").append(defaultText(animal.healthCondition(), "暂未补充"))
                .append("\n7. 当前状态：").append(defaultText(animal.statusText(), animalStatusText(animal.status())));

        if (medicalRecordCount != null) {
            reply.append("\n8. 医疗记录数：").append(medicalRecordCount);
        }

        if (animal.status() == AnimalStatus.WAITING_ADOPTION) {
            reply.append("\n它现在处于待领养状态，可以继续走领养申请流程。");
        } else if ("adoption-new".equals(route)) {
            reply.append("\n它当前不是待领养状态，所以即使你在申请页，也建议先确认是否还能正常提交申请。");
        } else {
            reply.append("\n它现在不是待领养状态。");
        }
        return reply.toString();
    }

    private String answerAdoptionApplyQuestion(AiAssistantDtos.PageContext pageContext) {
        AnimalDtos.AnimalResponse animal = animalService.detail(pageContext.entityId());
        boolean formReady = Boolean.TRUE.equals(getBoolean(pageContext.viewData(), "formReady"));
        boolean smartMatchGenerated = Boolean.TRUE.equals(getBoolean(pageContext.viewData(), "smartMatchGenerated"));
        StringBuilder reply = new StringBuilder();
        reply.append("你现在正在给动物档案 #")
                .append(animal.id())
                .append(" 提交领养申请。它当前状态是 ")
                .append(defaultText(animal.statusText(), animalStatusText(animal.status())))
                .append("。");

        reply.append(formReady
                ? " 你当前表单里的核心信息已经填写到可提交状态。"
                : " 你当前表单里的领养理由、居住条件、饲养经验还没全部补齐到可提交状态。");

        reply.append(smartMatchGenerated
                ? " AI 领养建议已经生成，你可以结合建议再决定是否直接提交。"
                : " AI 领养建议还没生成，如果你想先做匹配判断，可以先点生成建议。");
        return reply.toString();
    }

    private String answerRescueListQuestion(AiAssistantDtos.PageContext pageContext, String message) {
        Map<String, Object> filters = getMap(pageContext.viewData(), "filters");
        String keyword = getString(filters, "keyword");
        String region = getString(filters, "region");
        RescueStatus status = detectRescueStatus(message);
        if (status == null) {
            status = parseRescueStatus(getString(filters, "status"));
        }

        PageResponse<RescueDtos.RescueResponse> result = rescueService.listPublic(
                blankToNull(keyword),
                blankToNull(region),
                status,
                0,
                6
        );

        if (result.content().isEmpty()) {
            return "我按当前页面条件查了一下，暂时没有找到符合条件的公开救助信息。";
        }

        StringBuilder reply = new StringBuilder();
        reply.append("当前筛选下共有 ")
                .append(result.totalElements())
                .append(" 条公开救助信息，先给你看前 ")
                .append(result.content().size())
                .append(" 条：");

        for (int i = 0; i < result.content().size(); i++) {
            RescueDtos.RescueResponse rescue = result.content().get(i);
            reply.append("\n")
                    .append(i + 1)
                    .append(". #")
                    .append(rescue.id())
                    .append("，地点 ")
                    .append(defaultText(rescue.location(), "未填写"))
                    .append("，状态 ")
                    .append(defaultText(rescue.statusText(), rescueStatusText(rescue.status())))
                    .append("，动物情况 ")
                    .append(defaultText(rescue.animalCondition(), "未填写"));
        }
        return reply.toString();
    }

    private String answerCurrentRescueQuestion(AiAssistantDtos.PageContext pageContext) {
        RescueDtos.RescueResponse rescue = rescueService.detail(pageContext.entityId());
        return "这条救助信息的真实内容是："
                + "\n1. 编号 #" + rescue.id()
                + "\n2. 地点：" + defaultText(rescue.location(), "未填写")
                + "\n3. 动物情况：" + defaultText(rescue.animalCondition(), "未填写")
                + "\n4. 联系方式：" + defaultText(rescue.contact(), "未填写")
                + "\n5. 当前状态：" + defaultText(rescue.statusText(), rescueStatusText(rescue.status()))
                + "\n6. 说明：" + defaultText(shorten(rescue.description(), 120), "暂无说明");
    }

    private String answerNoticeListQuestion(AiAssistantDtos.PageContext pageContext, String message) {
        String keyword = getString(pageContext.viewData(), "keyword");
        PageResponse<NoticeDtos.NoticeResponse> result = noticeService.listPublic(blankToNull(keyword), 0, 6);
        if (result.content().isEmpty()) {
            return "当前公告列表里暂时没有可展示的公告。";
        }

        StringBuilder reply = new StringBuilder();
        reply.append("当前公告列表里共有 ")
                .append(result.totalElements())
                .append(" 条公告，前几条是：");
        for (int i = 0; i < result.content().size(); i++) {
            NoticeDtos.NoticeResponse notice = result.content().get(i);
            reply.append("\n")
                    .append(i + 1)
                    .append(". #")
                    .append(notice.id())
                    .append("《")
                    .append(defaultText(notice.title(), "未命名公告"))
                    .append("》");
            if (notice.publishedAt() != null) {
                reply.append("，发布时间 ").append(notice.publishedAt().format(TIME_FORMATTER));
            }
        }
        return reply.toString();
    }

    private String answerCurrentNoticeQuestion(AiAssistantDtos.PageContext pageContext) {
        NoticeDtos.NoticeResponse notice = noticeService.detailPublic(pageContext.entityId());
        StringBuilder reply = new StringBuilder();
        reply.append("这条公告的真实内容是：")
                .append("\n标题：").append(defaultText(notice.title(), "未命名公告"));
        if (notice.publishedAt() != null) {
            reply.append("\n发布时间：").append(notice.publishedAt().format(TIME_FORMATTER));
        }
        reply.append("\n状态：").append(defaultText(notice.statusText(), "未知"))
                .append("\n内容摘要：").append(defaultText(shorten(notice.content(), 180), "暂无内容"));
        return reply.toString();
    }

    private String answerDonationListQuestion(AiAssistantDtos.PageContext pageContext, String message) {
        Map<String, Object> filters = getMap(pageContext.viewData(), "filters");
        String keyword = getString(filters, "keyword");
        SupplyCategory category = detectSupplyCategory(message);
        DonationStatus status = detectDonationStatus(message);

        if (category == null) {
            category = parseSupplyCategory(getString(filters, "category"));
        }
        if (status == null) {
            status = parseDonationStatus(getString(filters, "status"));
        }

        PageResponse<DonationDtos.SupplyDemandResponse> result = donationService.listPublic(
                blankToNull(keyword),
                category,
                status,
                0,
                6
        );

        if (result.content().isEmpty()) {
            return "我按当前页面条件查了一下，暂时没有找到符合条件的物资需求。";
        }

        StringBuilder reply = new StringBuilder();
        reply.append("当前物资需求里共有 ")
                .append(result.totalElements())
                .append(" 条符合条件的记录，前几条是：");
        for (int i = 0; i < result.content().size(); i++) {
            DonationDtos.SupplyDemandResponse demand = result.content().get(i);
            int remaining = Math.max(0, safeInt(demand.targetQuantity()) - safeInt(demand.currentQuantity()));
            reply.append("\n")
                    .append(i + 1)
                    .append(". #")
                    .append(demand.id())
                    .append("《")
                    .append(defaultText(demand.title(), "未命名需求"))
                    .append("》，类别 ")
                    .append(defaultText(demand.categoryLabel(), supplyCategoryText(demand.category())))
                    .append("，状态 ")
                    .append(defaultText(demand.statusText(), donationStatusText(demand.status())))
                    .append("，还差 ")
                    .append(remaining)
                    .append("。");
        }
        return reply.toString();
    }

    private String answerCurrentDonationQuestion(AiAssistantDtos.PageContext pageContext) {
        DonationDtos.SupplyDemandResponse demand = donationService.detail(pageContext.entityId());
        Integer recordCount = getInteger(pageContext.viewData(), "recordCount");
        int remaining = Math.max(0, safeInt(demand.targetQuantity()) - safeInt(demand.currentQuantity()));
        StringBuilder reply = new StringBuilder();
        reply.append("这条物资需求的真实情况是：")
                .append("\n1. 标题：").append(defaultText(demand.title(), "未命名需求"))
                .append("\n2. 类别：").append(defaultText(demand.categoryLabel(), supplyCategoryText(demand.category())))
                .append("\n3. 当前状态：").append(defaultText(demand.statusText(), donationStatusText(demand.status())))
                .append("\n4. 目标数量：").append(safeInt(demand.targetQuantity()))
                .append("\n5. 已有数量：").append(safeInt(demand.currentQuantity()))
                .append("\n6. 还差数量：").append(remaining)
                .append("\n7. 收货地址：").append(defaultText(demand.shippingAddress(), "未填写"));
        if (recordCount != null) {
            reply.append("\n8. 当前页面已加载的捐赠记录数：").append(recordCount);
        }
        if (demand.status() == DonationStatus.COMPLETED || demand.status() == DonationStatus.CANCELLED) {
            reply.append("\n这条需求目前已经关闭，不能继续按正常流程发起捐赠。");
        } else {
            reply.append("\n这条需求目前还可以继续捐赠。");
        }
        return reply.toString();
    }

    private String answerVolunteerListQuestion(AiAssistantDtos.PageContext pageContext, String message) {
        Map<String, Object> filters = getMap(pageContext.viewData(), "filters");
        String keyword = getString(filters, "keyword");
        String region = getString(filters, "region");
        VolunteerTaskStatus status = detectVolunteerTaskStatus(message);
        if (status == null) {
            status = parseVolunteerTaskStatus(getString(filters, "status"));
        }

        PageResponse<VolunteerTaskDtos.VolunteerTaskResponse> result = volunteerTaskService.listPublic(
                blankToNull(keyword),
                blankToNull(region),
                status,
                0,
                6
        );

        if (result.content().isEmpty()) {
            return "我按当前页面条件查了一下，暂时没有找到符合条件的志愿任务。";
        }

        StringBuilder reply = new StringBuilder();
        reply.append("当前志愿任务里共有 ")
                .append(result.totalElements())
                .append(" 条符合条件的记录，前几条是：");
        for (int i = 0; i < result.content().size(); i++) {
            VolunteerTaskDtos.VolunteerTaskResponse task = result.content().get(i);
            int remaining = Math.max(0, safeInt(task.maxVolunteers()) - safeInt(task.currentVolunteers()));
            reply.append("\n")
                    .append(i + 1)
                    .append(". #")
                    .append(task.id())
                    .append("《")
                    .append(defaultText(task.title(), "未命名任务"))
                    .append("》，地点 ")
                    .append(defaultText(task.location(), "未填写"))
                    .append("，状态 ")
                    .append(defaultText(task.statusText(), volunteerTaskStatusText(task.status())))
                    .append("，还缺 ")
                    .append(remaining)
                    .append(" 人。");
        }
        return reply.toString();
    }

    private String answerCurrentVolunteerQuestion(AiAssistantDtos.PageContext pageContext) {
        VolunteerTaskDtos.VolunteerTaskResponse task = volunteerTaskService.detail(pageContext.entityId());
        Integer applicationCount = getInteger(pageContext.viewData(), "applicationCount");
        int remaining = Math.max(0, safeInt(task.maxVolunteers()) - safeInt(task.currentVolunteers()));
        StringBuilder reply = new StringBuilder();
        reply.append("这个志愿任务的真实情况是：")
                .append("\n1. 标题：").append(defaultText(task.title(), "未命名任务"))
                .append("\n2. 地点：").append(defaultText(task.location(), "未填写"))
                .append("\n3. 当前状态：").append(defaultText(task.statusText(), volunteerTaskStatusText(task.status())))
                .append("\n4. 已报名人数：").append(safeInt(task.currentVolunteers()))
                .append(" / ").append(safeInt(task.maxVolunteers()))
                .append("\n5. 还缺人数：").append(remaining);
        if (task.scheduledTime() != null) {
            reply.append("\n6. 计划时间：").append(task.scheduledTime().format(TIME_FORMATTER));
        }
        if (applicationCount != null) {
            reply.append("\n7. 当前页面已加载的报名记录数：").append(applicationCount);
        }
        reply.append(task.status() == VolunteerTaskStatus.RECRUITING && remaining > 0
                ? "\n它目前还可以继续报名。"
                : "\n它现在不是一个可直接报名的空余状态。");
        return reply.toString();
    }

    private String answerCommunityListQuestion(AiAssistantDtos.PageContext pageContext, String message) {
        Map<String, Object> viewData = defaultMap(pageContext.viewData());
        String sort = getString(viewData, "sort");
        String keyword = getString(viewData, "keyword");

        PageResponse<CommunityDtos.CommunityPostResponse> result;
        if ("following".equals(sort)) {
            result = communityService.feedFollowing(0, 6);
        } else {
            result = communityService.listPublic(blankToNull(keyword), null, null, blankToNull(sort), 0, 6);
        }

        if (result.content().isEmpty()) {
            return "当前社区列表里暂时没有符合条件的帖子。";
        }

        StringBuilder reply = new StringBuilder();
        reply.append("当前社区列表里共有 ")
                .append(result.totalElements())
                .append(" 条帖子，前几条是：");
        for (int i = 0; i < result.content().size(); i++) {
            CommunityDtos.CommunityPostResponse post = result.content().get(i);
            reply.append("\n")
                    .append(i + 1)
                    .append(". #")
                    .append(post.id())
                    .append("《")
                    .append(defaultText(post.title(), "未命名帖子"))
                    .append("》");
            if (hasText(post.authorNickname())) {
                reply.append("，作者 ").append(post.authorNickname());
            }
            if (hasText(post.categoryName())) {
                reply.append("，分类 ").append(post.categoryName());
            }
            reply.append("，评论 ").append(post.commentCount()).append(" 条。");
        }
        return reply.toString();
    }

    private String answerCommunityCategoryQuestion(AiAssistantDtos.PageContext pageContext, String message) {
        Map<String, Object> viewData = defaultMap(pageContext.viewData());
        Map<String, Object> category = getMap(viewData, "category");
        Long categoryId = getLong(category, "id");
        String keyword = getString(viewData, "keyword");
        String categoryName = getString(category, "name");

        PageResponse<CommunityDtos.CommunityPostResponse> result = communityService.listPublic(
                blankToNull(keyword),
                categoryId,
                null,
                "latest_active",
                0,
                6
        );

        if (result.content().isEmpty()) {
            return "当前分类下暂时没有符合条件的帖子。";
        }

        StringBuilder reply = new StringBuilder();
        reply.append("当前分类")
                .append(hasText(categoryName) ? "“" + categoryName + "”" : "")
                .append("下共有 ")
                .append(result.totalElements())
                .append(" 条帖子，前几条是：");
        for (int i = 0; i < result.content().size(); i++) {
            CommunityDtos.CommunityPostResponse post = result.content().get(i);
            reply.append("\n")
                    .append(i + 1)
                    .append(". #")
                    .append(post.id())
                    .append("《")
                    .append(defaultText(post.title(), "未命名帖子"))
                    .append("》，作者 ")
                    .append(defaultText(post.authorNickname(), "未知"))
                    .append("，评论 ")
                    .append(post.commentCount())
                    .append(" 条。");
        }
        return reply.toString();
    }

    private String answerCurrentCommunityPostQuestion(AiAssistantDtos.PageContext pageContext) {
        Map<String, Object> currentPost = getMap(pageContext.viewData(), "currentPost");
        String title = getString(currentPost, "title");
        String authorNickname = getString(currentPost, "authorNickname");
        Integer commentCount = getInteger(currentPost, "commentCount");
        Integer viewCount = getInteger(currentPost, "viewCount");
        String contentPreview = getString(currentPost, "contentPreview");

        if (!hasText(title) && !hasText(pageContext.pageTitle())) {
            return null;
        }

        StringBuilder reply = new StringBuilder();
        reply.append("当前这篇帖子是");
        if (hasText(title)) {
            reply.append("《").append(title).append("》");
        } else {
            reply.append("《").append(pageContext.pageTitle()).append("》");
        }
        if (hasText(authorNickname)) {
            reply.append("，作者是 ").append(authorNickname);
        }
        if (commentCount != null) {
            reply.append("，现在有 ").append(commentCount).append(" 条评论");
        }
        if (viewCount != null) {
            reply.append("，阅读量 ").append(viewCount);
        }
        reply.append("。");
        if (hasText(contentPreview)) {
            reply.append(" 内容大意是：").append(contentPreview);
        }
        return reply.toString();
    }

    private String answerMessagePageQuestion(AiAssistantDtos.PageContext pageContext) {
        Map<String, Object> viewData = defaultMap(pageContext.viewData());
        Integer unreadCount = getInteger(viewData, "unreadCount");
        Integer conversationCount = getInteger(viewData, "conversationCount");
        Integer visibleMessageCount = getInteger(viewData, "visibleMessageCount");
        Map<String, Object> activeConversation = getMap(viewData, "activeConversation");
        String peerNickname = getString(activeConversation, "peerNickname");
        String peerRoleText = getString(activeConversation, "peerRoleText");

        StringBuilder reply = new StringBuilder();
        reply.append("你当前私聊页里共有 ")
                .append(safeInt(conversationCount))
                .append(" 个会话");
        if (unreadCount != null) {
            reply.append("，未读消息 ").append(unreadCount).append(" 条");
        }
        reply.append("。");
        if (hasText(peerNickname)) {
            reply.append(" 你现在打开的是和 ")
                    .append(peerNickname);
            if (hasText(peerRoleText)) {
                reply.append("（").append(peerRoleText).append("）");
            }
            reply.append(" 的会话");
            if (visibleMessageCount != null) {
                reply.append("，当前页面已显示 ").append(visibleMessageCount).append(" 条消息");
            }
            reply.append("。");
        }
        return reply.toString();
    }

    private String answerProfileQuestion(AiAssistantDtos.PageContext pageContext, String message) {
        Map<String, Object> viewData = defaultMap(pageContext.viewData());
        Map<String, Object> profile = getMap(viewData, "profile");
        Map<String, Object> summary = getMap(viewData, "summary");
        String activeTab = getString(viewData, "activeTab");
        String nickname = getString(profile, "nickname");
        String roleText = getString(profile, "roleText");

        StringBuilder reply = new StringBuilder();
        reply.append("你现在在个人中心");
        if (hasText(nickname)) {
            reply.append("，当前账号昵称是 ").append(nickname);
        }
        if (hasText(roleText)) {
            reply.append("，角色是 ").append(roleText);
        }
        reply.append("。");

        reply.append(" 当前统计里：动物档案 ")
                .append(safeInt(getInteger(summary, "animalCount")))
                .append("，救助信息 ")
                .append(safeInt(getInteger(summary, "rescueCount")))
                .append("，领养申请 ")
                .append(safeInt(getInteger(summary, "applicationCount")))
                .append("，未读通知 ")
                .append(safeInt(getInteger(summary, "unreadNotificationCount")))
                .append("，举报 ")
                .append(safeInt(getInteger(summary, "reportCount")))
                .append("，申诉 ")
                .append(safeInt(getInteger(summary, "appealCount")))
                .append("。");

        if (hasText(activeTab)) {
            reply.append(" 你当前打开的标签是 ").append(profileTabText(activeTab)).append("。");
        }

        if (containsAny(message, "领养跟进", "managed", "托管")) {
            reply.append(" 当前还能看到你管理侧的领养跟进数量为 ")
                    .append(safeInt(getInteger(summary, "managedApplicationCount")))
                    .append("。");
        }
        return reply.toString();
    }

    private String answerRescueStationQuestion(AiAssistantDtos.PageContext pageContext, String message) {
        Map<String, Object> viewData = defaultMap(pageContext.viewData());
        boolean hasStation = Boolean.TRUE.equals(getBoolean(viewData, "hasStation"));
        Map<String, Object> myStation = getMap(viewData, "myStation");
        Map<String, Object> dashboard = getMap(viewData, "dashboard");
        Integer followersCount = getInteger(viewData, "followersCount");
        Integer followingCount = getInteger(viewData, "followingCount");
        List<Map<String, Object>> discoverStations = getListOfMaps(viewData, "discoverStations");

        if (!hasStation) {
            return "你当前还没有自己的救助站档案。这个页面现在主要可以做两件事：提交救助站认证申请，或者先浏览/关注其他救助站。当前发现区里展示了 "
                    + discoverStations.size()
                    + " 个救助站。";
        }

        StringBuilder reply = new StringBuilder();
        reply.append("你当前的救助站是 ")
                .append(defaultText(getString(myStation, "stationName"), "未命名救助站"))
                .append("，认证状态 ")
                .append(defaultText(getString(myStation, "certificationStatusText"), "未知"))
                .append("，粉丝数 ")
                .append(safeInt(getInteger(myStation, "followerCount")))
                .append("。");

        if (containsAny(message, "地址", "电话", "联系方式", "简介")) {
            reply.append(" 站点简介：")
                    .append(defaultText(getString(myStation, "description"), "暂无简介"))
                    .append("；地址：")
                    .append(defaultText(getString(myStation, "address"), "未填写"))
                    .append("；电话：")
                    .append(defaultText(getString(myStation, "contactPhone"), "未填写"))
                    .append("。");
        }

        if (dashboard != null && !dashboard.isEmpty()) {
            reply.append(" 数据看板里：救助信息 ")
                    .append(safeInt(getInteger(dashboard, "rescueCount")))
                    .append("，动物档案 ")
                    .append(safeInt(getInteger(dashboard, "animalCount")))
                    .append("，物资需求 ")
                    .append(safeInt(getInteger(dashboard, "donationDemandCount")))
                    .append("，捐赠记录 ")
                    .append(safeInt(getInteger(dashboard, "totalDonationRecords")))
                    .append("，志愿任务 ")
                    .append(safeInt(getInteger(dashboard, "volunteerTaskCount")))
                    .append("，志愿报名 ")
                    .append(safeInt(getInteger(dashboard, "totalVolunteerApplications")))
                    .append("。");
        }

        if (containsAny(message, "粉丝", "关注")) {
            reply.append(" 当前页面已加载的粉丝列表数量是 ")
                    .append(safeInt(followersCount))
                    .append("，我关注的救助站数量是 ")
                    .append(safeInt(followingCount))
                    .append("。");
        }
        return reply.toString();
    }

    private String answerUserProfileQuestion(AiAssistantDtos.PageContext pageContext) {
        Map<String, Object> viewData = defaultMap(pageContext.viewData());
        Map<String, Object> profile = getMap(viewData, "profile");
        Map<String, Object> stationInfo = getMap(viewData, "stationInfo");

        StringBuilder reply = new StringBuilder();
        reply.append("这个公开用户主页显示：")
                .append("\n1. 昵称：").append(defaultText(getString(profile, "nickname"), "未知"))
                .append("\n2. 角色：").append(defaultText(getString(profile, "roleText"), "未知"))
                .append("\n3. 动物档案数：").append(safeInt(getInteger(profile, "animalCount")))
                .append("\n4. 救助信息数：").append(safeInt(getInteger(profile, "rescueCount")));

        if (stationInfo != null && !stationInfo.isEmpty()) {
            reply.append("\n5. 关联救助站：").append(defaultText(getString(stationInfo, "stationName"), "未命名"))
                    .append("\n6. 救助站认证状态：").append(defaultText(getString(stationInfo, "certificationStatusText"), "未知"))
                    .append("\n7. 救助站粉丝数：").append(safeInt(getInteger(stationInfo, "followerCount")));
        }
        return reply.toString();
    }

    private String answerMapQuestion(AiAssistantDtos.PageContext pageContext) {
        Map<String, Object> viewData = defaultMap(pageContext.viewData());
        boolean hasCurrentLocation = Boolean.TRUE.equals(getBoolean(viewData, "hasCurrentLocation"));
        Integer resultCount = getInteger(viewData, "resultCount");
        Double distanceKm = getDouble(viewData, "distanceKm");
        List<String> visibleTypes = getStringList(viewData, "visibleTypes");
        List<Map<String, Object>> points = getListOfMaps(viewData, "visiblePoints");

        if (!hasCurrentLocation) {
            return "你现在还没有完成定位，所以地图页还不能准确告诉你附近有哪些点位。先点“一键查看周边”或手动输入地址/经纬度会更准确。";
        }

        StringBuilder reply = new StringBuilder();
        reply.append("地图页当前已经按 ")
                .append(distanceKm == null ? "默认范围" : formatOneDecimal(distanceKm) + " km")
                .append(" 搜到了 ")
                .append(safeInt(resultCount))
                .append(" 个点位");
        if (!visibleTypes.isEmpty()) {
            reply.append("，当前显示类型是 ").append(describeVisibleTypes(visibleTypes)).append("。");
        } else {
            reply.append("。");
        }

        if (!points.isEmpty()) {
            reply.append(" 离你最近的几个点位是：");
            for (int i = 0; i < Math.min(points.size(), 5); i++) {
                Map<String, Object> point = points.get(i);
                reply.append("\n")
                        .append(i + 1)
                        .append(". ")
                        .append(defaultText(getString(point, "title"), "未命名点位"))
                        .append("，")
                        .append(defaultText(getString(point, "type"), "未知类型"))
                        .append("，距离 ")
                        .append(formatDistance(getDouble(point, "distanceKm")));
            }
        }
        return reply.toString();
    }

    private String callModel(String userMessage, List<AiAssistantDtos.ConversationMessage> history, String siteContext) {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.max(connectTimeoutMs, 1000)))
                .build();

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", systemPrompt()));
        messages.add(new ChatMessage("system", siteContext));
        messages.addAll(buildConversationHistory(history));
        messages.add(new ChatMessage("user", userMessage));

        ModelRequest payload = new ModelRequest(
                model,
                messages,
                false,
                0.4,
                1200
        );

        try {
            String requestBody = objectMapper.writeValueAsString(payload);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofMillis(Math.max(readTimeoutMs, 3000)))
                    .header("Authorization", "Bearer " + apiKey.trim())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(HttpStatus.BAD_GATEWAY, "AI 服务暂时不可用，请稍后再试");
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
            String content = readContent(contentNode);
            if (content.isBlank()) {
                throw new BusinessException(HttpStatus.BAD_GATEWAY, "AI 助手暂时无法回答这个问题，请稍后再试");
            }
            return content.trim();
        } catch (IOException ex) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "AI 服务响应异常，请稍后再试");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "AI 服务请求被中断");
        }
    }

    private String buildSiteContext(AiAssistantDtos.PageContext pageContext) {
        StringBuilder builder = new StringBuilder();
        builder.append("Use the following real site context to answer. Prefer this context over generic knowledge.\n");
        builder.append("serverTime=").append(LocalDateTime.now().format(TIME_FORMATTER)).append('\n');

        SecuritySupport.currentUser().ifPresent(user -> builder
                .append("currentUser.id=").append(user.id())
                .append(", currentUser.role=").append(user.role())
                .append('\n'));

        appendPlatformOverview(builder);
        appendPageContext(builder, pageContext);
        return builder.toString();
    }

    private void appendPlatformOverview(StringBuilder builder) {
        try {
            StatsDtos.HomeOverviewResponse homeOverview = statisticsService.homeOverview();
            builder.append('\n')
                    .append("platform.overview.users=").append(homeOverview.overview().userCount()).append('\n')
                    .append("platform.overview.animals=").append(homeOverview.overview().animalCount()).append('\n')
                    .append("platform.overview.rescues=").append(homeOverview.overview().rescueCount()).append('\n')
                    .append("platform.overview.applications=").append(homeOverview.overview().applyCount()).append('\n')
                    .append("platform.overview.pendingAudits=").append(homeOverview.overview().pendingAuditCount()).append('\n');

            appendLatestAnimalList(builder, homeOverview.latestAnimals());
            appendLatestRescueList(builder, homeOverview.latestRescues());
            appendLatestNoticeList(builder, homeOverview.latestNotices());
        } catch (RuntimeException ignored) {
            builder.append("\nplatform.overview=unavailable\n");
        }
    }

    private void appendPageContext(StringBuilder builder, AiAssistantDtos.PageContext pageContext) {
        if (pageContext == null) {
            builder.append("\npageContext=missing\n");
            return;
        }

        builder.append('\n');
        appendIfPresent(builder, "page.routeName=", pageContext.routeName());
        appendIfPresent(builder, "page.routePath=", pageContext.routePath());
        appendIfPresent(builder, "page.title=", pageContext.pageTitle());
        appendIfPresent(builder, "page.summary=", pageContext.pageSummary());
        appendIfPresent(builder, "page.entityType=", pageContext.entityType());
        if (pageContext.entityId() != null) {
            builder.append("page.entityId=").append(pageContext.entityId()).append('\n');
        }

        appendResolvedEntityContext(builder, pageContext);
        appendFrontendSnapshot(builder, pageContext.viewData());
    }

    private void appendResolvedEntityContext(StringBuilder builder, AiAssistantDtos.PageContext pageContext) {
        if (pageContext.entityType() == null || pageContext.entityId() == null) {
            return;
        }

        String entityType = pageContext.entityType().trim().toUpperCase(Locale.ROOT);
        Long entityId = pageContext.entityId();

        try {
            switch (entityType) {
                case "ANIMAL" -> appendAnimalDetails(builder, animalService.detail(entityId));
                case "RESCUE" -> appendRescueDetails(builder, rescueService.detail(entityId));
                case "NOTICE" -> appendNoticeDetails(builder, noticeService.detailPublic(entityId));
                case "DONATION_DEMAND" -> appendDonationDetails(builder, donationService.detail(entityId));
                case "VOLUNTEER_TASK" -> appendVolunteerTaskDetails(builder, volunteerTaskService.detail(entityId));
                case "USER_PROFILE" -> appendUserProfileDetails(builder, userService.getPublicProfile(entityId));
                default -> builder.append("page.entityResolver=unsupported:").append(entityType).append('\n');
            }
        } catch (RuntimeException ex) {
            builder.append("page.entityResolver=failed:").append(entityType).append('#').append(entityId).append('\n');
        }
    }

    private void appendAnimalDetails(StringBuilder builder, AnimalDtos.AnimalResponse animal) {
        builder.append("animal.id=").append(animal.id()).append('\n')
                .append("animal.type=").append(animal.type()).append('\n')
                .append("animal.gender=").append(animal.gender()).append('\n')
                .append("animal.age=").append(animal.age()).append('\n')
                .append("animal.region=").append(defaultText(animal.foundRegion(), "")).append('\n')
                .append("animal.health=").append(defaultText(animal.healthCondition(), "")).append('\n')
                .append("animal.status=").append(animal.status()).append('\n')
                .append("animal.publisher=").append(defaultText(animal.publisherNickname(), "")).append('\n')
                .append("animal.description=").append(shorten(animal.description(), 280)).append('\n');
    }

    private void appendRescueDetails(StringBuilder builder, RescueDtos.RescueResponse rescue) {
        builder.append("rescue.id=").append(rescue.id()).append('\n')
                .append("rescue.location=").append(defaultText(rescue.location(), "")).append('\n')
                .append("rescue.condition=").append(defaultText(rescue.animalCondition(), "")).append('\n')
                .append("rescue.contact=").append(defaultText(rescue.contact(), "")).append('\n')
                .append("rescue.status=").append(rescue.status()).append('\n')
                .append("rescue.publisher=").append(defaultText(rescue.publisherNickname(), "")).append('\n')
                .append("rescue.description=").append(shorten(rescue.description(), 280)).append('\n');
    }

    private void appendNoticeDetails(StringBuilder builder, NoticeDtos.NoticeResponse notice) {
        builder.append("notice.id=").append(notice.id()).append('\n')
                .append("notice.title=").append(defaultText(notice.title(), "")).append('\n')
                .append("notice.status=").append(notice.status()).append('\n')
                .append("notice.publishedAt=").append(notice.publishedAt()).append('\n')
                .append("notice.content=").append(shorten(notice.content(), 320)).append('\n');
    }

    private void appendDonationDetails(StringBuilder builder, DonationDtos.SupplyDemandResponse demand) {
        builder.append("donation.id=").append(demand.id()).append('\n')
                .append("donation.title=").append(defaultText(demand.title(), "")).append('\n')
                .append("donation.category=").append(demand.category()).append('\n')
                .append("donation.targetQuantity=").append(demand.targetQuantity()).append('\n')
                .append("donation.currentQuantity=").append(demand.currentQuantity()).append('\n')
                .append("donation.status=").append(demand.status()).append('\n')
                .append("donation.publisher=").append(defaultText(demand.publisherNickname(), "")).append('\n')
                .append("donation.description=").append(shorten(demand.description(), 280)).append('\n');
    }

    private void appendVolunteerTaskDetails(StringBuilder builder, VolunteerTaskDtos.VolunteerTaskResponse task) {
        builder.append("volunteerTask.id=").append(task.id()).append('\n')
                .append("volunteerTask.title=").append(defaultText(task.title(), "")).append('\n')
                .append("volunteerTask.location=").append(defaultText(task.location(), "")).append('\n')
                .append("volunteerTask.status=").append(task.status()).append('\n')
                .append("volunteerTask.maxVolunteers=").append(task.maxVolunteers()).append('\n')
                .append("volunteerTask.currentVolunteers=").append(task.currentVolunteers()).append('\n')
                .append("volunteerTask.scheduledTime=").append(task.scheduledTime()).append('\n')
                .append("volunteerTask.relatedRescue=").append(defaultText(task.relatedRescueLocation(), "")).append('\n')
                .append("volunteerTask.description=").append(shorten(task.description(), 280)).append('\n');
    }

    private void appendUserProfileDetails(StringBuilder builder, UserDtos.PublicUserProfile profile) {
        builder.append("userProfile.id=").append(profile.id()).append('\n')
                .append("userProfile.nickname=").append(defaultText(profile.nickname(), "")).append('\n')
                .append("userProfile.role=").append(profile.role()).append('\n')
                .append("userProfile.animalCount=").append(profile.animalCount()).append('\n')
                .append("userProfile.rescueCount=").append(profile.rescueCount()).append('\n')
                .append("userProfile.createdAt=").append(profile.createdAt()).append('\n');
    }

    private void appendFrontendSnapshot(StringBuilder builder, Map<String, Object> viewData) {
        if (viewData == null || viewData.isEmpty()) {
            return;
        }

        try {
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(viewData);
            builder.append("page.viewData=").append(shorten(json, 2500)).append('\n');
        } catch (IOException ignored) {
            builder.append("page.viewData=unavailable\n");
        }
    }

    private void appendLatestAnimalList(StringBuilder builder, List<AnimalDtos.AnimalResponse> animals) {
        for (int i = 0; i < Math.min(animals.size(), 3); i++) {
            AnimalDtos.AnimalResponse animal = animals.get(i);
            builder.append("latestAnimal[").append(i).append("]=")
                    .append(animal.id()).append('|')
                    .append(animal.type()).append('|')
                    .append(animal.status()).append('|')
                    .append(defaultText(animal.foundRegion(), ""))
                    .append('\n');
        }
    }

    private void appendLatestRescueList(StringBuilder builder, List<RescueDtos.RescueResponse> rescues) {
        for (int i = 0; i < Math.min(rescues.size(), 3); i++) {
            RescueDtos.RescueResponse rescue = rescues.get(i);
            builder.append("latestRescue[").append(i).append("]=")
                    .append(rescue.id()).append('|')
                    .append(defaultText(rescue.location(), "")).append('|')
                    .append(rescue.status())
                    .append('\n');
        }
    }

    private void appendLatestNoticeList(StringBuilder builder, List<NoticeDtos.NoticeResponse> notices) {
        for (int i = 0; i < Math.min(notices.size(), 3); i++) {
            NoticeDtos.NoticeResponse notice = notices.get(i);
            builder.append("latestNotice[").append(i).append("]=")
                    .append(notice.id()).append('|')
                    .append(defaultText(notice.title(), ""))
                    .append('\n');
        }
    }

    private void appendLatestAnimals(StringBuilder builder, List<AnimalDtos.AnimalResponse> animals) {
        if (animals == null || animals.isEmpty()) {
            return;
        }
        builder.append(" 最新动物有：");
        for (int i = 0; i < Math.min(animals.size(), 3); i++) {
            AnimalDtos.AnimalResponse animal = animals.get(i);
            if (i > 0) {
                builder.append("；");
            }
            builder.append("#")
                    .append(animal.id())
                    .append(" ")
                    .append(defaultText(animal.typeText(), animalTypeText(animal.type())))
                    .append(" / ")
                    .append(defaultText(animal.statusText(), animalStatusText(animal.status())))
                    .append(" / ")
                    .append(defaultText(animal.foundRegion(), "地区未填写"));
        }
        builder.append("。");
    }

    private void appendLatestRescues(StringBuilder builder, List<RescueDtos.RescueResponse> rescues) {
        if (rescues == null || rescues.isEmpty()) {
            return;
        }
        builder.append(" 最新救助有：");
        for (int i = 0; i < Math.min(rescues.size(), 3); i++) {
            RescueDtos.RescueResponse rescue = rescues.get(i);
            if (i > 0) {
                builder.append("；");
            }
            builder.append("#")
                    .append(rescue.id())
                    .append(" ")
                    .append(defaultText(rescue.location(), "地点未填写"))
                    .append(" / ")
                    .append(defaultText(rescue.statusText(), rescueStatusText(rescue.status())));
        }
        builder.append("。");
    }

    private void appendLatestNotices(StringBuilder builder, List<NoticeDtos.NoticeResponse> notices) {
        if (notices == null || notices.isEmpty()) {
            return;
        }
        builder.append(" 最新公告有：");
        for (int i = 0; i < Math.min(notices.size(), 3); i++) {
            NoticeDtos.NoticeResponse notice = notices.get(i);
            if (i > 0) {
                builder.append("；");
            }
            builder.append("#")
                    .append(notice.id())
                    .append("《")
                    .append(defaultText(notice.title(), "未命名公告"))
                    .append("》");
        }
        builder.append("。");
    }

    private String readContent(JsonNode contentNode) {
        if (contentNode == null || contentNode.isMissingNode() || contentNode.isNull()) {
            return "";
        }
        if (contentNode.isTextual()) {
            return contentNode.asText();
        }
        if (contentNode.isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode item : contentNode) {
                JsonNode textNode = item.path("text");
                if (textNode.isTextual()) {
                    if (builder.length() > 0) {
                        builder.append("\n");
                    }
                    builder.append(textNode.asText());
                }
            }
            return builder.toString();
        }
        return contentNode.toString();
    }

    private List<ChatMessage> buildConversationHistory(List<AiAssistantDtos.ConversationMessage> history) {
        if (history == null || history.isEmpty()) {
            return List.of();
        }

        List<ChatMessage> messages = new ArrayList<>();
        int start = Math.max(0, history.size() - 10);
        for (int i = start; i < history.size(); i++) {
            AiAssistantDtos.ConversationMessage item = history.get(i);
            if (item == null || !hasText(item.content())) {
                continue;
            }
            String role = normalizeHistoryRole(item.role());
            if (role == null) {
                continue;
            }
            messages.add(new ChatMessage(role, item.content().trim()));
        }
        return messages;
    }

    private String normalizeHistoryRole(String role) {
        if (!hasText(role)) {
            return null;
        }
        String normalized = role.trim().toLowerCase(Locale.ROOT);
        if ("user".equals(normalized) || "assistant".equals(normalized)) {
            return normalized;
        }
        return null;
    }

    private boolean isPagePurposeQuestion(String message) {
        return containsAny(message, "这个页面", "当前页面", "这一页", "这里是干嘛", "这里能做什么", "这里可以做什么", "怎么用这个页面", "当前页");
    }

    private boolean isHomeQuestion(String message) {
        return containsAny(message, "首页", "平台概况", "概况", "概览", "最新", "公告", "救助", "动物");
    }

    private boolean isAnimalListQuestion(AiAssistantDtos.PageContext pageContext, String message) {
        if (!"animals".equals(safeRoute(pageContext))) {
            return false;
        }
        return containsAny(message, "动物", "档案", "领养", "猫", "狗", "有哪些", "有什么", "推荐", "看看");
    }

    private boolean isCurrentAnimalQuestion(String route, String message) {
        return "animal-detail".equals(route)
                || "adoption-new".equals(route)
                || containsAny(message, "这只", "这个动物", "当前动物", "能领养", "可以领养", "健康", "状态", "介绍", "信息");
    }

    private boolean isAdoptionApplyQuestion(String message) {
        return containsAny(message, "领养申请", "申请", "表单", "资料", "ai 建议", "ai建议", "智能建议", "适合我", "能不能提交");
    }

    private boolean isRescueListQuestion(String message) {
        return containsAny(message, "救助", "求助", "有哪些", "有什么", "待处理", "处理中", "已完成", "地点", "联系");
    }

    private boolean isCurrentRescueQuestion(String message) {
        return containsAny(message, "这条救助", "这个救助", "当前救助", "救助信息", "地点", "联系方式", "状态", "进展", "说明");
    }

    private boolean isNoticeListQuestion(String message) {
        return containsAny(message, "公告", "通知", "最新公告", "有哪些公告", "有什么公告");
    }

    private boolean isCurrentNoticeQuestion(String message) {
        return containsAny(message, "这条公告", "这个公告", "公告讲了什么", "公告内容", "发布时间", "什么时候发的");
    }

    private boolean isDonationListQuestion(String message) {
        return containsAny(message, "物资", "捐赠", "需求", "猫粮", "狗粮", "猫砂", "药品", "有哪些", "有什么");
    }

    private boolean isCurrentDonationQuestion(String message) {
        return containsAny(message, "这条物资", "这个需求", "还缺多少", "还能捐吗", "能不能捐", "进度", "状态", "捐赠记录");
    }

    private boolean isVolunteerListQuestion(String message) {
        return containsAny(message, "志愿", "任务", "报名", "招募", "有哪些", "有什么", "还缺几个人");
    }

    private boolean isCurrentVolunteerQuestion(String message) {
        return containsAny(message, "这个任务", "这条任务", "还能报名", "能不能报名", "还缺几个人", "状态", "地点", "时间", "报名情况");
    }

    private boolean isCommunityListQuestion(String message) {
        return containsAny(message, "社区", "帖子", "热门", "最新", "关注", "有哪些帖子", "有什么帖子");
    }

    private boolean isCommunityCategoryQuestion(String message) {
        return containsAny(message, "分类", "版块", "帖子", "这个分类", "这个版块");
    }

    private boolean isCurrentCommunityPostQuestion(String message) {
        return containsAny(message, "这篇帖子", "这个帖子", "帖子讲了什么", "评论", "作者", "阅读量", "互动");
    }

    private boolean isMessagePageQuestion(String message) {
        return containsAny(message, "未读", "私聊", "消息", "会话", "聊天", "跟谁聊", "当前会话");
    }

    private boolean isProfileQuestion(String message) {
        return containsAny(message, "个人中心", "我的资料", "我的通知", "我的申诉", "我的举报", "我的申请", "我有多少", "当前标签");
    }

    private boolean isRescueStationQuestion(String message) {
        return containsAny(message, "救助站", "认证", "粉丝", "关注", "数据看板", "发现救助站", "我的站");
    }

    private boolean isUserProfileQuestion(String message) {
        return containsAny(message, "这个用户", "这个人", "用户主页", "发过多少", "救助站", "公开资料");
    }

    private boolean isMapQuestion(String message) {
        return containsAny(message, "地图", "附近", "周边", "最近", "距离", "点位");
    }

    private Map<String, Object> getMap(Map<String, Object> source, String key) {
        if (source == null) {
            return null;
        }
        return castMap(source.get(key));
    }

    private Map<String, Object> defaultMap(Map<String, Object> source) {
        return source == null ? Map.of() : source;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        if (value instanceof Map<?, ?> mapValue) {
            return (Map<String, Object>) mapValue;
        }
        return null;
    }

    private String getString(Map<String, Object> source, String key) {
        if (source == null || !source.containsKey(key)) {
            return null;
        }
        Object value = source.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private Integer getInteger(Map<String, Object> source, String key) {
        if (source == null || !source.containsKey(key)) {
            return null;
        }
        Object value = source.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Long getLong(Map<String, Object> source, String key) {
        if (source == null || !source.containsKey(key)) {
            return null;
        }
        Object value = source.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Double getDouble(Map<String, Object> source, String key) {
        if (source == null || !source.containsKey(key)) {
            return null;
        }
        Object value = source.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Boolean getBoolean(Map<String, Object> source, String key) {
        if (source == null || !source.containsKey(key)) {
            return null;
        }
        Object value = source.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String text && !text.isBlank()) {
            return Boolean.parseBoolean(text);
        }
        return null;
    }

    private List<Map<String, Object>> getListOfMaps(Map<String, Object> source, String key) {
        if (source == null || !source.containsKey(key)) {
            return List.of();
        }
        Object value = source.get(key);
        if (!(value instanceof List<?> listValue)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : listValue) {
            Map<String, Object> map = castMap(item);
            if (map != null) {
                result.add(map);
            }
        }
        return result;
    }

    private List<String> getStringList(Map<String, Object> source, String key) {
        if (source == null || !source.containsKey(key)) {
            return List.of();
        }
        Object value = source.get(key);
        if (!(value instanceof List<?> listValue)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object item : listValue) {
            if (item != null) {
                result.add(String.valueOf(item));
            }
        }
        return result;
    }

    private AnimalType detectAnimalType(String message) {
        if (containsAny(message, "猫", "猫咪", "小猫")) {
            return AnimalType.CAT;
        }
        if (containsAny(message, "狗", "狗狗", "小狗")) {
            return AnimalType.DOG;
        }
        return null;
    }

    private AnimalStatus detectAnimalStatus(String message) {
        if (containsAny(message, "待领养", "可领养", "能领养", "可以领养", "领养")) {
            return AnimalStatus.WAITING_ADOPTION;
        }
        if (containsAny(message, "已领养")) {
            return AnimalStatus.ADOPTED;
        }
        if (containsAny(message, "待救助")) {
            return AnimalStatus.WAITING_RESCUE;
        }
        if (containsAny(message, "救助中")) {
            return AnimalStatus.RESCUING;
        }
        return null;
    }

    private RescueStatus detectRescueStatus(String message) {
        if (containsAny(message, "待处理")) {
            return RescueStatus.PENDING_PROCESS;
        }
        if (containsAny(message, "处理中")) {
            return RescueStatus.PROCESSING;
        }
        if (containsAny(message, "已完成", "完成")) {
            return RescueStatus.COMPLETED;
        }
        return null;
    }

    private DonationStatus detectDonationStatus(String message) {
        if (containsAny(message, "待认领")) {
            return DonationStatus.PENDING;
        }
        if (containsAny(message, "已认领")) {
            return DonationStatus.CLAIMED;
        }
        if (containsAny(message, "运输中")) {
            return DonationStatus.IN_TRANSIT;
        }
        if (containsAny(message, "已完成", "完成")) {
            return DonationStatus.COMPLETED;
        }
        if (containsAny(message, "已取消", "取消")) {
            return DonationStatus.CANCELLED;
        }
        return null;
    }

    private VolunteerTaskStatus detectVolunteerTaskStatus(String message) {
        if (containsAny(message, "招募中")) {
            return VolunteerTaskStatus.RECRUITING;
        }
        if (containsAny(message, "进行中")) {
            return VolunteerTaskStatus.IN_PROGRESS;
        }
        if (containsAny(message, "已完成", "完成")) {
            return VolunteerTaskStatus.COMPLETED;
        }
        return null;
    }

    private SupplyCategory detectSupplyCategory(String message) {
        if (containsAny(message, "猫粮")) {
            return SupplyCategory.CAT_FOOD;
        }
        if (containsAny(message, "狗粮")) {
            return SupplyCategory.DOG_FOOD;
        }
        if (containsAny(message, "猫砂")) {
            return SupplyCategory.CAT_LITTER;
        }
        if (containsAny(message, "药", "药品")) {
            return SupplyCategory.MEDICINE;
        }
        if (containsAny(message, "玩具")) {
            return SupplyCategory.TOYS;
        }
        if (containsAny(message, "垫子", "窝")) {
            return SupplyCategory.BEDDING;
        }
        if (containsAny(message, "清洁")) {
            return SupplyCategory.CLEANING;
        }
        return null;
    }

    private AnimalType parseAnimalType(String value) {
        try {
            return hasText(value) ? AnimalType.valueOf(value) : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private AnimalStatus parseAnimalStatus(String value) {
        try {
            return hasText(value) ? AnimalStatus.valueOf(value) : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private AnimalGender parseAnimalGender(String value) {
        try {
            return hasText(value) ? AnimalGender.valueOf(value) : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private RescueStatus parseRescueStatus(String value) {
        try {
            return hasText(value) ? RescueStatus.valueOf(value) : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private DonationStatus parseDonationStatus(String value) {
        try {
            return hasText(value) ? DonationStatus.valueOf(value) : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private SupplyCategory parseSupplyCategory(String value) {
        try {
            return hasText(value) ? SupplyCategory.valueOf(value) : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private VolunteerTaskStatus parseVolunteerTaskStatus(String value) {
        try {
            return hasText(value) ? VolunteerTaskStatus.valueOf(value) : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String animalTypeText(AnimalType type) {
        return type == null ? "未知" : type.getLabel();
    }

    private String animalGenderText(AnimalGender gender) {
        return gender == null ? "未知" : gender.getLabel();
    }

    private String animalStatusText(AnimalStatus status) {
        return status == null ? "未知" : status.getLabel();
    }

    private String rescueStatusText(RescueStatus status) {
        return status == null ? "未知" : status.getLabel();
    }

    private String donationStatusText(DonationStatus status) {
        return status == null ? "未知" : status.getLabel();
    }

    private String volunteerTaskStatusText(VolunteerTaskStatus status) {
        return status == null ? "未知" : status.getLabel();
    }

    private String supplyCategoryText(SupplyCategory category) {
        return category == null ? "未知" : category.getLabel();
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private void appendIfPresent(StringBuilder builder, String prefix, String value) {
        if (hasText(value)) {
            builder.append(prefix).append(value).append('\n');
        }
    }

    private String shorten(String value, int maxLength) {
        if (!hasText(value)) {
            return "";
        }
        String normalized = value.replace('\r', ' ').replace('\n', ' ').trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String blankToNull(String value) {
        return hasText(value) ? value : null;
    }

    private String defaultText(String value, String fallback) {
        return hasText(value) ? value : fallback;
    }

    private String safeRoute(AiAssistantDtos.PageContext pageContext) {
        return trimToEmpty(pageContext.routeName());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String formatDistance(Double value) {
        if (value == null) {
            return "未知";
        }
        return formatOneDecimal(value) + " km";
    }

    private String describeVisibleTypes(List<String> visibleTypes) {
        List<String> labels = new ArrayList<>();
        for (String item : visibleTypes) {
            if ("animal".equals(item)) {
                labels.add("动物");
            } else if ("station".equals(item)) {
                labels.add("救助站");
            } else {
                labels.add(item);
            }
        }
        return String.join("、", labels);
    }

    private String profileTabText(String tab) {
        return switch (tab) {
            case "animals" -> "动物档案";
            case "rescues" -> "救助信息";
            case "applications" -> "领养申请";
            case "managedApplications" -> "领养跟进";
            case "communityPosts" -> "社区帖子";
            case "communityComments" -> "社区评论";
            case "notifications" -> "通知";
            case "reports" -> "我的举报";
            case "appeals" -> "我的申诉";
            case "password" -> "修改密码";
            default -> tab;
        };
    }

    private String formatOneDecimal(Double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private String systemPrompt() {
        return """
                你是“归途”平台的 AI 助手。
                你必须只用简体中文回答。
                优先根据我提供的网站真实上下文回答，不要脱离当前页面泛化空谈。
                如果上下文不足，就明确说不确定，不要编造站内事实。
                不要使用 Markdown。
                回答尽量简短、直接，像网站里的内置助手，而不是百科问答。
                """;
    }

    private record ModelRequest(
            String model,
            List<ChatMessage> messages,
            boolean stream,
            double temperature,
            int max_tokens
    ) {}

    private record ChatMessage(String role, String content) {}
}
