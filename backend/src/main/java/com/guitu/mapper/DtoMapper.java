package com.guitu.mapper;

import org.springframework.stereotype.Component;

import com.guitu.domain.AdoptApply;
import com.guitu.domain.Animal;
import com.guitu.domain.AppealRecord;
import com.guitu.domain.AuditLog;
import com.guitu.domain.CommunityComment;
import com.guitu.domain.CommunityPost;
import com.guitu.domain.ContentReport;
import com.guitu.domain.DirectConversation;
import com.guitu.domain.DirectMessage;
import com.guitu.domain.DonationRecord;
import com.guitu.domain.MedicalRecord;
import com.guitu.domain.Notice;
import com.guitu.domain.Rescue;
import com.guitu.domain.RescueStation;
import com.guitu.domain.SupplyDemand;
import com.guitu.domain.SystemNotification;
import com.guitu.domain.User;
import com.guitu.domain.UserFollow;
import com.guitu.domain.VolunteerApplication;
import com.guitu.domain.VolunteerTask;
import com.guitu.dto.AdoptApplyDtos;
import com.guitu.dto.AnimalDtos;
import com.guitu.dto.AppealDtos;
import com.guitu.dto.AuditDtos;
import com.guitu.dto.CommunityDtos;
import com.guitu.dto.DirectMessageDtos;
import com.guitu.dto.DonationDtos;
import com.guitu.dto.MedicalRecordDtos;
import com.guitu.dto.NoticeDtos;
import com.guitu.dto.NotificationDtos;
import com.guitu.dto.ReportDtos;
import com.guitu.dto.RescueDtos;
import com.guitu.dto.RescueStationDtos;
import com.guitu.dto.UserDtos;
import com.guitu.dto.VolunteerTaskDtos;

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

    public DirectMessageDtos.UserSummary toUserSummary(User user) {
        return new DirectMessageDtos.UserSummary(
                user.getId(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getRole(),
                user.getRole().getLabel()
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
                report.getCreatedAt(),
                null
        );
    }

    public ReportDtos.ReportResponse toReportResponse(ContentReport report, String targetContent) {
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
                report.getCreatedAt(),
                targetContent
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

    public DirectMessageDtos.ConversationResponse toConversationResponse(DirectConversation conversation, Long currentUserId, long unreadCount) {
        User peer = conversation.getUserOne().getId().equals(currentUserId) ? conversation.getUserTwo() : conversation.getUserOne();
        return new DirectMessageDtos.ConversationResponse(
                conversation.getId(),
                toUserSummary(peer),
                conversation.getLastMessageContent(),
                conversation.getLastSender() != null ? conversation.getLastSender().getId() : null,
                conversation.getLastMessageAt(),
                unreadCount,
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }

    public DirectMessageDtos.MessageResponse toMessageResponse(DirectMessage message) {
        return new DirectMessageDtos.MessageResponse(
                message.getId(),
                message.getConversation().getId(),
                toUserSummary(message.getSender()),
                toUserSummary(message.getReceiver()),
                message.getContent(),
                message.getImageUrls(),
                message.isReadFlag(),
                message.getReadAt(),
                message.getCreatedAt()
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

    public MedicalRecordDtos.MedicalRecordResponse toMedicalRecordResponse(MedicalRecord record) {
        return new MedicalRecordDtos.MedicalRecordResponse(
                record.getId(),
                record.getType(),
                record.getType().getLabel(),
                record.getRecordDate(),
                record.getVeterinarianName(),
                record.getInstitution(),
                record.getDescription(),
                record.getImageUrls(),
                record.getCreatedAt(),
                record.getUpdatedAt()
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

    public DonationDtos.SupplyDemandResponse toSupplyDemandResponse(SupplyDemand demand) {
        return new DonationDtos.SupplyDemandResponse(
                demand.getId(),
                demand.getTitle(),
                demand.getCategory(),
                demand.getCategory().getLabel(),
                demand.getTargetQuantity(),
                demand.getCurrentQuantity(),
                demand.getDescription(),
                demand.getContactName(),
                demand.getContactPhone(),
                demand.getShippingAddress(),
                demand.getStatus(),
                demand.getStatus().getLabel(),
                demand.getImageUrl(),
                demand.getPublisher().getId(),
                demand.getPublisher().getNickname(),
                demand.getCreatedAt(),
                demand.getUpdatedAt()
        );
    }

    public DonationDtos.DonationRecordResponse toDonationRecordResponse(DonationRecord record) {
        return new DonationDtos.DonationRecordResponse(
                record.getId(),
                record.getDemand().getId(),
                record.getDemand().getTitle(),
                record.getDonor().getId(),
                record.getDonorDisplayName(),
                record.getQuantity(),
                record.getDeliveryMethod(),
                record.getTrackingNumber(),
                record.getMessage(),
                record.getStatus(),
                record.getStatus().getLabel(),
                record.getCompletedAt(),
                record.getCreatedAt()
        );
    }

    public VolunteerTaskDtos.VolunteerTaskResponse toVolunteerTaskResponse(VolunteerTask task) {
        return new VolunteerTaskDtos.VolunteerTaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getLocation(),
                task.getMaxVolunteers(),
                task.getCurrentVolunteers(),
                task.getScheduledTime(),
                task.getImageUrl(),
                task.getStatus(),
                task.getStatus().getLabel(),
                task.getReviewComment(),
                task.getPublisher().getId(),
                task.getPublisher().getNickname(),
                task.getRelatedRescue() != null ? task.getRelatedRescue().getId() : null,
                task.getRelatedRescue() != null ? task.getRelatedRescue().getLocation() : null,
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }

    public VolunteerTaskDtos.ApplicationResponse toApplicationResponse(VolunteerApplication app) {
        return new VolunteerTaskDtos.ApplicationResponse(
                app.getId(),
                app.getTask().getId(),
                app.getTask().getTitle(),
                app.getVolunteer().getId(),
                app.getVolunteer().getNickname(),
                app.getMessage(),
                app.getStatus(),
                app.getStatus().getLabel(),
                app.getReviewComment(),
                app.getCompletedAt(),
                app.getCreatedAt()
        );
    }

    public RescueStationDtos.StationResponse toStationResponse(RescueStation station) {
        return new RescueStationDtos.StationResponse(
                station.getId(),
                station.getUser().getId(),
                station.getUser().getNickname(),
                station.getUser().getAvatarUrl(),
                station.getStationName(),
                station.getDescription(),
                station.getAddress(),
                station.getContactPhone(),
                station.getImageUrl(),
                station.getCertificationStatus(),
                station.getCertificationStatus().getLabel(),
                station.getFollowerCount(),
                station.getRejectReason(),
                station.getCreatedAt()
        );
    }

    public RescueStationDtos.FollowResponse toFollowResponse(UserFollow follow) {
        return new RescueStationDtos.FollowResponse(
                follow.getId(),
                follow.getFollower().getId(),
                follow.getFollower().getNickname(),
                follow.getFollower().getAvatarUrl(),
                follow.getStationUser() != null ? follow.getStationUser().getNickname() : "",
                follow.getCreatedAt()
        );
    }
}
