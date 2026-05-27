package com.guitu.service;

import com.guitu.domain.AdoptApply;
import com.guitu.domain.AdoptionAgreement;
import com.guitu.domain.AdoptionFollowUp;
import com.guitu.domain.User;
import com.guitu.domain.enums.AdoptionAgreementStatus;
import com.guitu.domain.enums.AdoptionFollowUpStatus;
import com.guitu.domain.enums.NotificationType;
import com.guitu.domain.enums.OperationTargetType;
import com.guitu.domain.enums.OperationType;
import com.guitu.dto.AdoptApplyDtos;
import com.guitu.exception.BusinessException;
import com.guitu.mapper.DtoMapper;
import com.guitu.repository.AdoptApplyRepository;
import com.guitu.repository.AdoptionAgreementRepository;
import com.guitu.repository.AdoptionFollowUpRepository;
import com.guitu.security.SecuritySupport;
import com.guitu.util.AgreementPdfGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AdoptionExtensionService {
    private static final Logger log = LoggerFactory.getLogger(AdoptionExtensionService.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AdoptionAgreementRepository adoptionAgreementRepository;
    private final AdoptionFollowUpRepository adoptionFollowUpRepository;
    private final AdoptApplyRepository adoptApplyRepository;
    private final UserService userService;
    private final DtoMapper mapper;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;
    private final OperationLogService operationLogService;

    public AdoptionExtensionService(
            AdoptionAgreementRepository adoptionAgreementRepository,
            AdoptionFollowUpRepository adoptionFollowUpRepository,
            AdoptApplyRepository adoptApplyRepository,
            UserService userService,
            DtoMapper mapper,
            FileStorageService fileStorageService,
            NotificationService notificationService,
            OperationLogService operationLogService
    ) {
        this.adoptionAgreementRepository = adoptionAgreementRepository;
        this.adoptionFollowUpRepository = adoptionFollowUpRepository;
        this.adoptApplyRepository = adoptApplyRepository;
        this.userService = userService;
        this.mapper = mapper;
        this.fileStorageService = fileStorageService;
        this.notificationService = notificationService;
        this.operationLogService = operationLogService;
    }

    @Transactional
    public void initializeForApprovedApply(AdoptApply apply, User operator, String opinion) {
        AdoptionAgreement agreement = adoptionAgreementRepository.findByApplyId(apply.getId()).orElse(null);
        if (agreement == null) {
            agreement = new AdoptionAgreement();
            agreement.setApply(apply);
            agreement.setAnimal(apply.getAnimal());
            agreement.setAdopter(apply.getApplicant());
            agreement.setPublisher(apply.getAnimal().getPublisher());
            agreement.setAgreementNo(generateAgreementNo(apply));
            agreement.setTitle("归途平台流浪动物领养协议");
            agreement.setContent(buildAgreementContent(agreement.getAgreementNo(), apply, opinion));
            agreement.setStatus(AdoptionAgreementStatus.PENDING_ADOPTER);
            agreement.setPdfUrl(tryStoreAgreementPdf(agreement));
            adoptionAgreementRepository.saveAndFlush(agreement);
            operationLogService.record(
                    operator,
                    OperationTargetType.ADOPTION_AGREEMENT,
                    apply.getId(),
                    agreement.getTitle() + " / 申请#" + apply.getId(),
                    OperationType.CREATE_AGREEMENT,
                    "系统已为审核通过的领养申请生成领养协议",
                    null,
                    Map.of("agreementNo", agreement.getAgreementNo(), "status", agreement.getStatus().name())
            );
        }

        if (!adoptionFollowUpRepository.existsByApplyId(apply.getId())) {
            LocalDateTime baseTime = LocalDateTime.now();
            createFollowUp(apply, "WEEK_1", "领养后 1 周回访", baseTime.plusWeeks(1));
            createFollowUp(apply, "MONTH_1", "领养后 1 个月回访", baseTime.plusMonths(1));
            createFollowUp(apply, "MONTH_3", "领养后 3 个月回访", baseTime.plusMonths(3));
            operationLogService.record(
                    operator,
                    OperationTargetType.ADOPTION_FOLLOW_UP,
                    apply.getId(),
                    "申请#" + apply.getId() + " 回访计划",
                    OperationType.CREATE_FOLLOW_UP_PLAN,
                    "系统已自动生成 3 个回访计划节点",
                    null,
                    Map.of("stages", List.of("WEEK_1", "MONTH_1", "MONTH_3"))
            );
        }

        notificationService.notifyUser(
                apply.getApplicant(),
                NotificationType.AUDIT_RESULT,
                "领养协议待签署",
                "你的领养申请已通过，请尽快查看并签署领养协议。",
                "ADOPT_APPLY",
                apply.getId()
        );
    }

    @Transactional
    public AdoptApplyDtos.AgreementResponse getAgreement(Long applyId) {
        AdoptionAgreement agreement = getAgreementEntity(applyId);
        ensureParticipantOrAdmin(agreement.getApply());
        return mapper.toAgreementResponse(agreement);
    }

    @Transactional
    public AdoptApplyDtos.AgreementResponse signAgreement(Long applyId, AdoptApplyDtos.SignAgreementRequest request) {
        AdoptionAgreement agreement = getAgreementEntity(applyId);
        User currentUser = userService.currentUser();
        ensureParticipantOrAdmin(agreement.getApply());

        Map<String, Object> before = new LinkedHashMap<>();
        before.put("status", agreement.getStatus().name());
        before.put("adopterSignatureName", agreement.getAdopterSignatureName());
        before.put("adopterSignatureImageUrl", agreement.getAdopterSignatureImageUrl());
        before.put("counterpartSignatureName", agreement.getCounterpartSignatureName());
        before.put("counterpartSignatureImageUrl", agreement.getCounterpartSignatureImageUrl());

        if (currentUser.getId().equals(agreement.getAdopter().getId())) {
            if (agreement.getAdopterSignedAt() != null) {
                throw new BusinessException("领养人已完成签署，请勿重复提交");
            }
            validateSignatureRequest(request);
            String signatureName = resolveSignatureName(request.signatureName(), currentUser);
            String signatureImageUrl = storeSignatureImage(request.signatureDataUrl(), agreement, "adopter");
            LocalDateTime signedAt = LocalDateTime.now();
            agreement.setAdopterSignatureName(signatureName);
            agreement.setAdopterSignatureImageUrl(signatureImageUrl);
            agreement.setAdopterSignedAt(signedAt);
            if (isSamePartyAgreement(agreement)) {
                agreement.setCounterpartSignatureName(signatureName);
                agreement.setCounterpartSignatureImageUrl(signatureImageUrl);
                agreement.setCounterpartSignedAt(signedAt);
            }
        } else if (currentUser.getId().equals(agreement.getPublisher().getId()) || SecuritySupport.isAdmin()) {
            if (agreement.getCounterpartSignedAt() != null) {
                throw new BusinessException("救助方已完成签署，请勿重复提交");
            }
            validateSignatureRequest(request);
            String signatureName = resolveSignatureName(request.signatureName(), currentUser);
            String signatureImageUrl = storeSignatureImage(request.signatureDataUrl(), agreement, "counterpart");
            LocalDateTime signedAt = LocalDateTime.now();
            agreement.setCounterpartSignatureName(signatureName);
            agreement.setCounterpartSignatureImageUrl(signatureImageUrl);
            agreement.setCounterpartSignedAt(signedAt);
            if (isSamePartyAgreement(agreement) && agreement.getAdopterSignedAt() == null) {
                agreement.setAdopterSignatureName(signatureName);
                agreement.setAdopterSignatureImageUrl(signatureImageUrl);
                agreement.setAdopterSignedAt(signedAt);
            }
        } else {
            throw new BusinessException(HttpStatus.FORBIDDEN, "当前账号无权签署该协议");
        }

        refreshAgreementStatus(agreement);
        agreement.setPdfUrl(tryStoreAgreementPdf(agreement));

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("status", agreement.getStatus().name());
        after.put("adopterSignatureName", agreement.getAdopterSignatureName());
        after.put("adopterSignatureImageUrl", agreement.getAdopterSignatureImageUrl());
        after.put("counterpartSignatureName", agreement.getCounterpartSignatureName());
        after.put("counterpartSignatureImageUrl", agreement.getCounterpartSignatureImageUrl());

        operationLogService.record(
                currentUser,
                OperationTargetType.ADOPTION_AGREEMENT,
                agreement.getApply().getId(),
                agreement.getTitle() + " / 申请#" + agreement.getApply().getId(),
                OperationType.SIGN_AGREEMENT,
                "签署领养协议",
                before,
                after
        );

        if (agreement.getStatus() == AdoptionAgreementStatus.COMPLETED) {
            notificationService.notifyUser(
                    agreement.getAdopter(),
                    NotificationType.AUDIT_RESULT,
                    "领养协议已完成签署",
                    "双方已完成领养协议签署，后续回访计划已生效。",
                    "ADOPT_APPLY",
                    agreement.getApply().getId()
            );
            notificationService.notifyUser(
                    agreement.getPublisher(),
                    NotificationType.AUDIT_RESULT,
                    "领养协议已完成签署",
                    "申请#" + agreement.getApply().getId() + " 的领养协议已完成签署，请按计划进行回访。",
                    "ADOPT_APPLY",
                    agreement.getApply().getId()
            );
        }

        return mapper.toAgreementResponse(agreement);
    }

    @Transactional
    public List<AdoptApplyDtos.FollowUpResponse> listFollowUps(Long applyId) {
        AdoptionAgreement agreement = getAgreementEntity(applyId);
        ensureParticipantOrAdmin(agreement.getApply());
        return adoptionFollowUpRepository.findByApplyIdOrderByPlannedAtAsc(applyId)
                .stream()
                .map(mapper::toFollowUpResponse)
                .toList();
    }

    @Transactional
    public AdoptApplyDtos.FollowUpResponse completeFollowUp(Long followUpId, AdoptApplyDtos.CompleteFollowUpRequest request) {
        AdoptionFollowUp followUp = adoptionFollowUpRepository.findById(followUpId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "回访记录不存在"));
        ensureCounterpartManager(followUp.getApply());
        User currentUser = userService.currentUser();

        Map<String, Object> before = Map.of(
                "status", followUp.getStatus().name(),
                "note", followUp.getNote(),
                "imageCount", followUp.getImageUrls().size()
        );

        followUp.setCreator(currentUser);
        followUp.setNote(request.note());
        followUp.getImageUrls().clear();
        if (request.imageUrls() != null) {
            followUp.getImageUrls().addAll(request.imageUrls());
        }
        followUp.setCompletedAt(LocalDateTime.now());
        followUp.setStatus(AdoptionFollowUpStatus.COMPLETED);

        operationLogService.record(
                currentUser,
                OperationTargetType.ADOPTION_FOLLOW_UP,
                followUp.getId(),
                followUp.getStageLabel() + " / 申请#" + followUp.getApply().getId(),
                OperationType.COMPLETE_FOLLOW_UP,
                "完成领养回访记录",
                before,
                Map.of(
                        "status", followUp.getStatus().name(),
                        "note", followUp.getNote(),
                        "imageCount", followUp.getImageUrls().size(),
                        "completedAt", followUp.getCompletedAt()
                )
        );

        notificationService.notifyUser(
                followUp.getAdopter(),
                NotificationType.AUDIT_RESULT,
                "新增领养回访记录",
                followUp.getStageLabel() + " 已完成记录，请及时查看。",
                "ADOPT_APPLY",
                followUp.getApply().getId()
        );
        return mapper.toFollowUpResponse(followUp);
    }

    private void createFollowUp(AdoptApply apply, String stageCode, String stageLabel, LocalDateTime plannedAt) {
        AdoptionFollowUp followUp = new AdoptionFollowUp();
        followUp.setApply(apply);
        followUp.setAnimal(apply.getAnimal());
        followUp.setAdopter(apply.getApplicant());
        followUp.setStageCode(stageCode);
        followUp.setStageLabel(stageLabel);
        followUp.setPlannedAt(plannedAt);
        adoptionFollowUpRepository.save(followUp);
    }

    private String generateAgreementNo(AdoptApply apply) {
        return "AGR-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "-" + apply.getId();
    }

    private AdoptionAgreement getAgreementEntity(Long applyId) {
        AdoptApply apply = adoptApplyRepository.findById(applyId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "领养申请不存在"));
        if (apply.getStatus() != com.guitu.domain.enums.ApplyStatus.APPROVED) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "该领养申请暂未生成协议");
        }
        ensureExtensionInitialized(apply);
        return adoptionAgreementRepository.findByApplyId(applyId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "该领养申请暂未生成协议"));
    }

    private void ensureExtensionInitialized(AdoptApply apply) {
        if (!adoptionAgreementRepository.existsByApplyId(apply.getId())) {
            AdoptionAgreement agreement = new AdoptionAgreement();
            agreement.setApply(apply);
            agreement.setAnimal(apply.getAnimal());
            agreement.setAdopter(apply.getApplicant());
            agreement.setPublisher(apply.getAnimal().getPublisher());
            agreement.setAgreementNo(generateAgreementNo(apply));
            agreement.setTitle("归途平台流浪动物领养协议");
            agreement.setContent(buildAgreementContent(agreement.getAgreementNo(), apply, apply.getAuditOpinion()));
            agreement.setStatus(AdoptionAgreementStatus.PENDING_ADOPTER);
            agreement.setPdfUrl(tryStoreAgreementPdf(agreement));
            adoptionAgreementRepository.saveAndFlush(agreement);
        }

        if (!adoptionFollowUpRepository.existsByApplyId(apply.getId())) {
            LocalDateTime baseTime = LocalDateTime.now();
            createFollowUp(apply, "WEEK_1", "领养后 1 周回访", baseTime.plusWeeks(1));
            createFollowUp(apply, "MONTH_1", "领养后 1 个月回访", baseTime.plusMonths(1));
            createFollowUp(apply, "MONTH_3", "领养后 3 个月回访", baseTime.plusMonths(3));
            adoptionFollowUpRepository.flush();
        }
    }

    private void ensureParticipantOrAdmin(AdoptApply apply) {
        Long currentUserId = SecuritySupport.requireUser().id();
        boolean isParticipant = currentUserId.equals(apply.getApplicant().getId()) || currentUserId.equals(apply.getAnimal().getPublisher().getId());
        if (!isParticipant && !SecuritySupport.isAdmin()) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "当前账号无权查看该领养扩展信息");
        }
    }

    private void ensureCounterpartManager(AdoptApply apply) {
        Long currentUserId = SecuritySupport.requireUser().id();
        boolean isManager = currentUserId.equals(apply.getAnimal().getPublisher().getId()) || SecuritySupport.isAdmin();
        if (!isManager) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "只有救助发布者或管理员可以维护回访记录");
        }
    }

    private void refreshAgreementStatus(AdoptionAgreement agreement) {
        if (agreement.getAdopterSignedAt() == null) {
            agreement.setStatus(AdoptionAgreementStatus.PENDING_ADOPTER);
            agreement.setCompletedAt(null);
            return;
        }
        if (agreement.getCounterpartSignedAt() == null) {
            agreement.setStatus(AdoptionAgreementStatus.PENDING_COUNTERPART);
            agreement.setCompletedAt(null);
            return;
        }
        agreement.setStatus(AdoptionAgreementStatus.COMPLETED);
        if (agreement.getCompletedAt() == null) {
            agreement.setCompletedAt(LocalDateTime.now());
        }
    }

    private String storeAgreementPdf(AdoptionAgreement agreement) {
        String content = buildSignedAgreementText(agreement);
        byte[] pdfBytes = AgreementPdfGenerator.generate(agreement.getTitle(), content);
        String filename = agreement.getAgreementNo() + "-" + UUID.randomUUID().toString().substring(0, 8) + ".pdf";
        return fileStorageService.saveBytes(pdfBytes, "agreement", filename);
    }

    private String tryStoreAgreementPdf(AdoptionAgreement agreement) {
        try {
            return storeAgreementPdf(agreement);
        } catch (RuntimeException ex) {
            log.error("Failed to generate agreement pdf for apply {}", agreement.getApply().getId(), ex);
            return null;
        }
    }

    private String storeSignatureImage(String signatureDataUrl, AdoptionAgreement agreement, String signerRole) {
        if (signatureDataUrl == null || signatureDataUrl.isBlank()) {
            return null;
        }

        int commaIndex = signatureDataUrl.indexOf(',');
        if (!signatureDataUrl.startsWith("data:image/") || commaIndex < 0) {
            throw new BusinessException("签名图片格式无效");
        }

        String header = signatureDataUrl.substring(0, commaIndex);
        String extension = header.contains("image/jpeg") ? "jpg" : header.contains("image/webp") ? "webp" : "png";
        byte[] imageBytes;
        try {
            imageBytes = Base64.getDecoder().decode(signatureDataUrl.substring(commaIndex + 1));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("签名图片格式无效");
        }

        String filename = agreement.getAgreementNo() + "-" + signerRole + "-signature-" + UUID.randomUUID().toString().substring(0, 8) + "." + extension;
        return fileStorageService.saveBytes(imageBytes, "agreement-signature", filename);
    }

    private String resolveSignatureName(String requestedName, User currentUser) {
        if (requestedName != null && !requestedName.isBlank()) {
            return requestedName.trim();
        }
        return currentUser.getNickname();
    }

    private void validateSignatureRequest(AdoptApplyDtos.SignAgreementRequest request) {
        boolean hasName = request.signatureName() != null && !request.signatureName().isBlank();
        boolean hasImage = request.signatureDataUrl() != null && !request.signatureDataUrl().isBlank();
        if (!hasName && !hasImage) {
            throw new BusinessException("请先填写签名或完成手写签名");
        }
    }

    private boolean isSamePartyAgreement(AdoptionAgreement agreement) {
        return agreement.getAdopter() != null
                && agreement.getPublisher() != null
                && agreement.getAdopter().getId() != null
                && agreement.getAdopter().getId().equals(agreement.getPublisher().getId());
    }

    private String buildAgreementContent(String agreementNo, AdoptApply apply, String opinion) {
        String publisherName = apply.getAnimal().getPublisher().getNickname();
        return String.join("\n",
                "协议编号：" + agreementNo,
                "签署时间：" + LocalDateTime.now().format(TIME_FORMATTER),
                "",
                "1. 协议主体",
                "领养人：" + apply.getApplicantName() + "，联系方式：" + apply.getContact(),
                "救助发布方：" + publisherName,
                "",
                "2. 动物信息",
                "动物编号：" + apply.getAnimal().getId() + "，类型：" + apply.getAnimal().getType().getLabel(),
                "发现地区：" + apply.getAnimal().getFoundRegion(),
                "健康情况：" + valueOrDash(apply.getAnimal().getHealthCondition()),
                "",
                "3. 领养人承诺",
                "领养理由：" + apply.getReason(),
                "居住条件：" + apply.getLivingCondition(),
                "饲养经验：" + apply.getExperience(),
                "领养人承诺为动物提供稳定、安全、合法的照护环境，并主动配合后续回访。",
                "",
                "4. 平台与救助方说明",
                "审核意见：" + valueOrDash(opinion),
                "救助方保留在约定时间节点进行回访的权利，领养人应尽量配合提供近况反馈。",
                "",
                "5. 回访约定",
                "系统已默认生成领养后 1 周、1 个月、3 个月三个回访节点，相关记录将归档保存。",
                "",
                "6. 争议处理",
                "若出现严重虐待、转卖、弃养等情况，平台和救助方有权依据记录采取进一步处置。"
        );
    }

    private String buildSignedAgreementText(AdoptionAgreement agreement) {
        return agreement.getContent() + "\n\n" + String.join("\n",
                "签署状态：",
                "领养人签名：" + valueOrDash(agreement.getAdopterSignatureName()) +
                        "，签署时间：" + formatTime(agreement.getAdopterSignedAt()),
                "救助方签名：" + valueOrDash(agreement.getCounterpartSignatureName()) +
                        "，签署时间：" + formatTime(agreement.getCounterpartSignedAt())
        );
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? "-" : time.format(TIME_FORMATTER);
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
