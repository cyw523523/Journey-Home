package com.guitu.mapper;

import com.guitu.domain.AdoptApply;
import com.guitu.domain.AppealRecord;
import com.guitu.domain.Animal;
import com.guitu.domain.AuditLog;
import com.guitu.domain.ContentReport;
import com.guitu.domain.CommunityComment;
import com.guitu.domain.CommunityPost;
import com.guitu.domain.Notice;
import com.guitu.domain.Rescue;
import com.guitu.domain.SystemNotification;
import com.guitu.domain.User;
import com.guitu.dto.AdoptApplyDtos;
import com.guitu.dto.AppealDtos;
import com.guitu.dto.AnimalDtos;
import com.guitu.dto.AuditDtos;
import com.guitu.dto.CommunityDtos;
import com.guitu.dto.NotificationDtos;
import com.guitu.dto.NoticeDtos;
import com.guitu.dto.ReportDtos;
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

    public CommunityDtos.CommunityPostResponse toCommunityPostResponse(CommunityPost post, long commentCount) {
        return new CommunityDtos.CommunityPostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getAuthor().getId(),
                post.getAuthor().getNickname(),
                post.getAuthor().getAvatarUrl(),
                post.getAuthor().getRole(),
                post.getAuthor().getRole().getLabel(),
                post.getStatus(),
                post.getStatus().getLabel(),
                post.getImageUrls(),
                commentCount,
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }

    public CommunityDtos.CommunityCommentResponse toCommunityCommentResponse(CommunityComment comment) {
        return new CommunityDtos.CommunityCommentResponse(
                comment.getId(),
                comment.getPost().getId(),
                comment.getParentComment() != null ? comment.getParentComment().getId() : null,
                comment.getParentComment() != null ? comment.getParentComment().getAuthor().getId() : null,
                comment.getParentComment() != null ? comment.getParentComment().getAuthor().getNickname() : null,
                comment.getAuthor().getId(),
                comment.getAuthor().getNickname(),
                comment.getAuthor().getAvatarUrl(),
                comment.getAuthor().getRole(),
                comment.getAuthor().getRole().getLabel(),
                comment.getContent(),
                comment.getImageUrls(),
                comment.getStatus(),
                comment.getStatus().getLabel(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }

    public ReportDtos.ReportResponse toReportResponse(ContentReport report) {
        return new ReportDtos.ReportResponse(
                report.getId(),
                report.getTargetType(),
                report.getTargetType().getLabel(),
                report.getTargetId(),
                report.getReporter().getId(),
                report.getReporter().getNickname(),
                report.getTargetOwner() != null ? report.getTargetOwner().getId() : null,
                report.getTargetOwner() != null ? report.getTargetOwner().getNickname() : null,
                report.getReasonType(),
                report.getReasonType().getLabel(),
                report.getDescription(),
                report.getEvidenceImageUrls(),
                report.getStatus(),
                report.getStatus().getLabel(),
                report.getResolutionAction(),
                report.getResolutionAction() != null ? report.getResolutionAction().getLabel() : null,
                report.getResolutionOpinion(),
                report.getReviewer() != null ? report.getReviewer().getId() : null,
                report.getReviewer() != null ? report.getReviewer().getNickname() : null,
                report.getReviewedAt(),
                report.getCreatedAt()
        );
    }

    public NotificationDtos.NotificationResponse toNotificationResponse(SystemNotification notification) {
        return new NotificationDtos.NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getType().getLabel(),
                notification.getTitle(),
                notification.getContent(),
                notification.getRelatedTargetType(),
                notification.getRelatedTargetId(),
                notification.isReadFlag(),
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }

    public AppealDtos.AppealResponse toAppealResponse(AppealRecord appeal) {
        return new AppealDtos.AppealResponse(
                appeal.getId(),
                appeal.getApplicant().getId(),
                appeal.getApplicant().getNickname(),
                appeal.getTargetType(),
                appeal.getTargetType().getLabel(),
                appeal.getTargetId(),
                appeal.getReason(),
                appeal.getStatus(),
                appeal.getStatus().getLabel(),
                appeal.getFirstReviewer() != null ? appeal.getFirstReviewer().getId() : null,
                appeal.getFirstReviewer() != null ? appeal.getFirstReviewer().getNickname() : null,
                appeal.getFirstReviewOpinion(),
                appeal.getSecondReviewer() != null ? appeal.getSecondReviewer().getId() : null,
                appeal.getSecondReviewer() != null ? appeal.getSecondReviewer().getNickname() : null,
                appeal.getFinalReviewOpinion(),
                appeal.getReviewedAt(),
                appeal.getCreatedAt()
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
