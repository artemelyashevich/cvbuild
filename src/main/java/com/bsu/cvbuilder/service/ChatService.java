package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.domain.entity.AiChat;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ChatService {

    AiChat createAiChat(UUID chatId);

    /**
     * Internal lookup that creates the chat for the current user when missing. Performs no ownership check.
     */
    AiChat getOrCreateChat(UUID chatId);

    /**
     * Returns the current user's chat. Never creates it.
     *
     * @throws com.bsu.cvbuilder.exception.AppException 404 when the chat does not exist, 403 when it belongs to another user
     */
    AiChat getOwnChat(UUID chatId);

    /**
     * True only when the chat exists and belongs to the given user.
     */
    boolean isOwnedBy(UUID chatId, String userId);

    /**
     * True when the chat does not exist yet or belongs to the given user. Never creates the chat.
     */
    boolean isAccessible(UUID chatId, String userId);

    /**
     * Returns the user's chat, creating it for {@code user} when missing. Does not rely on the security context.
     *
     * @throws com.bsu.cvbuilder.exception.AppException 403 when the chat belongs to another user
     */
    AiChat getOrCreateOwnChat(UUID chatId, UserProfile user);

    AiChat saveAiChat(AiChat aiChat);

    Page<AiChat> findAllByCurrentUser(Pageable pageable);

    void deleteAllByUserId(String id);
}
