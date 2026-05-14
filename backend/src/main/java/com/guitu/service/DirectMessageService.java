package com.guitu.service;

import com.guitu.common.PageResponse;
import com.guitu.domain.DirectConversation;
import com.guitu.domain.DirectMessage;
import com.guitu.domain.User;
import com.guitu.dto.DirectMessageDtos;
import com.guitu.exception.BusinessException;
import com.guitu.mapper.DtoMapper;
import com.guitu.repository.DirectConversationRepository;
import com.guitu.repository.DirectMessageRepository;
import com.guitu.websocket.MessageSocketHandler;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
public class DirectMessageService {
    private final DirectConversationRepository conversationRepository;
    private final DirectMessageRepository messageRepository;
    private final UserService userService;
    private final DtoMapper mapper;
    private final MessageSocketHandler messageSocketHandler;

    public DirectMessageService(
            DirectConversationRepository conversationRepository,
            DirectMessageRepository messageRepository,
            UserService userService,
            DtoMapper mapper,
            MessageSocketHandler messageSocketHandler
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userService = userService;
        this.mapper = mapper;
        this.messageSocketHandler = messageSocketHandler;
    }

    @Transactional(readOnly = true)
    public PageResponse<DirectMessageDtos.ConversationResponse> listMine(int page, int size) {
        Long userId = userService.currentUser().getId();
        Page<DirectConversation> result = conversationRepository.findMine(
                userId,
                PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 50)))
        );
        return PageResponse.from(result, conversation ->
                mapper.toConversationResponse(
                        conversation,
                        userId,
                        messageRepository.countByConversationIdAndReceiverIdAndReadFlagFalse(conversation.getId(), userId)
                )
        );
    }

    @Transactional
    public DirectMessageDtos.ConversationDetailResponse detail(Long conversationId) {
        User currentUser = userService.currentUser();
        DirectConversation conversation = getOwnedConversation(conversationId, currentUser.getId());
        return buildDetail(conversation, currentUser.getId(), true);
    }

    @Transactional
    public DirectMessageDtos.ConversationDetailResponse detailWithUser(Long targetUserId) {
        User currentUser = userService.currentUser();
        if (currentUser.getId().equals(targetUserId)) {
            throw new BusinessException("You cannot start a private chat with yourself");
        }
        User targetUser = userService.getById(targetUserId);
        DirectConversation conversation = getOrCreateConversation(currentUser, targetUser);
        return buildDetail(conversation, currentUser.getId(), true);
    }

    @Transactional
    public DirectMessageDtos.MessageResponse send(Long conversationId, DirectMessageDtos.SendMessageRequest request) {
        User currentUser = userService.currentUser();
        DirectConversation conversation = getOwnedConversation(conversationId, currentUser.getId());
        User receiver = resolvePeer(conversation, currentUser.getId());
        String normalizedContent = normalizeContent(request.content());
        List<String> normalizedImageUrls = normalizeImageUrls(request.imageUrls());
        if (normalizedContent.isEmpty() && normalizedImageUrls.isEmpty()) {
            throw new BusinessException("Message content or images are required");
        }

        DirectMessage message = new DirectMessage();
        message.setConversation(conversation);
        message.setSender(currentUser);
        message.setReceiver(receiver);
        message.setContent(normalizedContent);
        message.getImageUrls().clear();
        message.getImageUrls().addAll(normalizedImageUrls);

        DirectMessage saved = messageRepository.save(message);
        conversation.setLastMessageContent(buildPreview(saved.getContent(), saved.getImageUrls()));
        conversation.setLastSender(currentUser);
        conversation.setLastMessageAt(saved.getCreatedAt());
        messageSocketHandler.notifyMessageCreated(conversation.getId(), currentUser.getId(), receiver.getId());
        return mapper.toMessageResponse(saved);
    }

    @Transactional(readOnly = true)
    public DirectMessageDtos.UnreadSummary summary() {
        Long userId = userService.currentUser().getId();
        return new DirectMessageDtos.UnreadSummary(messageRepository.countByReceiverIdAndReadFlagFalse(userId));
    }

    private DirectConversation getOrCreateConversation(User currentUser, User targetUser) {
        Long firstId = Math.min(currentUser.getId(), targetUser.getId());
        Long secondId = Math.max(currentUser.getId(), targetUser.getId());
        return conversationRepository.findByUserOneIdAndUserTwoId(firstId, secondId)
                .orElseGet(() -> {
                    DirectConversation conversation = new DirectConversation();
                    conversation.setUserOne(firstId.equals(currentUser.getId()) ? currentUser : targetUser);
                    conversation.setUserTwo(secondId.equals(targetUser.getId()) ? targetUser : currentUser);
                    return conversationRepository.save(conversation);
                });
    }

    private DirectConversation getOwnedConversation(Long conversationId, Long currentUserId) {
        return conversationRepository.findOwnedConversation(conversationId, currentUserId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Conversation does not exist"));
    }

    private DirectMessageDtos.ConversationDetailResponse buildDetail(DirectConversation conversation, Long currentUserId, boolean markRead) {
        if (markRead) {
            markConversationRead(conversation.getId(), currentUserId);
        }
        List<DirectMessage> messages = messageRepository.findTop50ByConversationIdOrderByCreatedAtDesc(conversation.getId());
        Collections.reverse(messages);
        return new DirectMessageDtos.ConversationDetailResponse(
                mapper.toConversationResponse(conversation, currentUserId, 0),
                messages.stream().map(mapper::toMessageResponse).toList()
        );
    }

    private void markConversationRead(Long conversationId, Long currentUserId) {
        List<DirectMessage> unreadMessages = messageRepository.findByConversationIdAndReceiverIdAndReadFlagFalse(conversationId, currentUserId);
        if (unreadMessages.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Set<Long> senderIds = unreadMessages.stream()
                .map(message -> message.getSender().getId())
                .filter(senderId -> !senderId.equals(currentUserId))
                .collect(java.util.stream.Collectors.toSet());
        unreadMessages.forEach(message -> {
            message.setReadFlag(true);
            message.setReadAt(now);
        });
        senderIds.forEach(senderId -> messageSocketHandler.notifyConversationRead(conversationId, currentUserId, senderId));
    }

    private User resolvePeer(DirectConversation conversation, Long currentUserId) {
        if (conversation.getUserOne().getId().equals(currentUserId)) {
            return conversation.getUserTwo();
        }
        if (conversation.getUserTwo().getId().equals(currentUserId)) {
            return conversation.getUserOne();
        }
        throw new BusinessException(HttpStatus.FORBIDDEN, "Current user cannot access this conversation");
    }

    private String buildPreview(String content, List<String> imageUrls) {
        String normalized = content == null ? "" : content.replaceAll("\\s+", " ").trim();
        if (normalized.isEmpty() && imageUrls != null && !imageUrls.isEmpty()) {
            return imageUrls.size() == 1 ? "[图片]" : "[图片 " + imageUrls.size() + "]";
        }
        if (!normalized.isEmpty() && imageUrls != null && !imageUrls.isEmpty()) {
            normalized = normalized + " [图片]";
        }
        if (normalized.length() <= 120) {
            return normalized;
        }
        return normalized.substring(0, 120) + "...";
    }

    private String normalizeContent(String content) {
        return content == null ? "" : content.trim();
    }

    private List<String> normalizeImageUrls(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return List.of();
        }
        List<String> normalized = new ArrayList<>();
        for (String url : imageUrls) {
            if (url == null) {
                continue;
            }
            String trimmed = url.trim();
            if (!trimmed.isEmpty()) {
                normalized.add(trimmed);
            }
        }
        return normalized;
    }
}
