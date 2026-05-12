package com.guitu.mapper;

import com.guitu.domain.AdoptApply;
import com.guitu.domain.Animal;
import com.guitu.domain.AuditLog;
import com.guitu.domain.Notice;
import com.guitu.domain.Rescue;
import com.guitu.domain.User;
import com.guitu.dto.AdoptApplyDtos;
import com.guitu.dto.AnimalDtos;
import com.guitu.dto.AuditDtos;
import com.guitu.dto.NoticeDtos;
import com.guitu.dto.RescueDtos;
import com.guitu.dto.UserDtos;
import org.springframework.stereotype.Component;

@Component
public class DtoMapper {
    public UserDtos.UserProfile toUserProfile(User user) {
        return new UserDtos.UserProfile(
                user.getId(),
                user.getAccount(),
                user.getNickname(),
                user.getPhone(),
                user.getAvatarUrl(),
                user.getRole(),
                user.getRole().getLabel(),
                user.getStatus(),
                user.getStatus().getLabel(),
                user.getCreatedAt()
        );
    }

    public AnimalDtos.AnimalResponse toAnimalResponse(Animal animal) {
        return new AnimalDtos.AnimalResponse(
                animal.getId(),
                animal.getType(),
                animal.getType().getLabel(),
                animal.getGender(),
                animal.getGender().getLabel(),
                animal.getAge(),
                animal.getFoundRegion(),
                animal.getHealthCondition(),
                animal.getCoverImageUrl(),
                animal.getImageUrls(),
                animal.getDescription(),
                animal.getStatus(),
                animal.getStatus().getLabel(),
                animal.getReviewComment(),
                animal.getPublisher().getId(),
                animal.getPublisher().getNickname(),
                animal.getCreatedAt(),
                animal.getUpdatedAt()
        );
    }

    public RescueDtos.RescueResponse toRescueResponse(Rescue rescue) {
        return new RescueDtos.RescueResponse(
                rescue.getId(),
                rescue.getLocation(),
                rescue.getAnimalCondition(),
                rescue.getContact(),
                rescue.getDescription(),
                rescue.getImageUrls(),
                rescue.getStatus(),
                rescue.getStatus().getLabel(),
                rescue.getReviewComment(),
                rescue.getPublisher().getId(),
                rescue.getPublisher().getNickname(),
                rescue.getCreatedAt(),
                rescue.getUpdatedAt()
        );
    }

    public AdoptApplyDtos.ApplyResponse toApplyResponse(AdoptApply apply) {
        return new AdoptApplyDtos.ApplyResponse(
                apply.getId(),
                apply.getAnimal().getId(),
                apply.getAnimal().getType().getLabel(),
                apply.getApplicant().getId(),
                apply.getApplicantName(),
                apply.getContact(),
                apply.getReason(),
                apply.getLivingCondition(),
                apply.getExperience(),
                apply.getStatus(),
                apply.getStatus().getLabel(),
                apply.getAuditOpinion(),
                apply.getCreatedAt(),
                apply.getUpdatedAt()
        );
    }

    public NoticeDtos.NoticeResponse toNoticeResponse(Notice notice) {
        return new NoticeDtos.NoticeResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getPublisher().getId(),
                notice.getPublisher().getNickname(),
                notice.getPublishedAt(),
                notice.getStatus(),
                notice.getStatus().getLabel(),
                notice.getCreatedAt(),
                notice.getUpdatedAt()
        );
    }

    public AuditDtos.AuditLogResponse toAuditLogResponse(AuditLog log) {
        return new AuditDtos.AuditLogResponse(
                log.getId(),
                log.getTargetType(),
                log.getTargetType().getLabel(),
                log.getTargetId(),
                log.getAuditor().getId(),
                log.getAuditor().getNickname(),
                log.getAction(),
                log.getAction().getLabel(),
                log.getOpinion(),
                log.getAuditTime()
        );
    }
}
