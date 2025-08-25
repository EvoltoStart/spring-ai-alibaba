/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.example.manus.conversation.service;

import com.alibaba.cloud.ai.example.manus.conversation.entity.ConversationEntity;
import com.alibaba.cloud.ai.example.manus.conversation.entity.ConversationMessageEntity;
import com.alibaba.cloud.ai.example.manus.conversation.repository.ConversationRepository;
import com.alibaba.cloud.ai.example.manus.conversation.repository.ConversationMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Conversation service class providing conversation and message management functionality
 */
@Service
@Transactional
public class ConversationService {

	private static final Logger logger = LoggerFactory.getLogger(ConversationService.class);

	@Autowired
	private ConversationRepository conversationRepository;

	@Autowired
	private ConversationMessageRepository messageRepository;

	@Autowired
	private MessageSource messageSource;

	/**
	 * Get localized message
	 */
	private String getMessage(String code, Object... args) {
		Locale locale = LocaleContextHolder.getLocale();
		return messageSource.getMessage(code, args, code, locale);
	}

	/**
	 * Create a new conversation
	 */
	public ConversationEntity createConversation(String userId, String title) {
		if (!StringUtils.hasText(userId)) {
			throw new IllegalArgumentException(getMessage("conversation.error.userIdEmpty"));
		}

		String conversationId = generateConversationId();
		String conversationTitle = StringUtils.hasText(title) ? title : getMessage("conversation.default.title");

		ConversationEntity conversation = new ConversationEntity(userId, conversationTitle);
		conversation.setConversationId(conversationId);
		conversation.setStatus(ConversationEntity.ConversationStatus.ACTIVE);

		ConversationEntity saved = conversationRepository.save(conversation);
		logger.info("Created new conversation: conversationId={}, userId={}, title={}", conversationId, userId, conversationTitle);

		return saved;
	}

	/**
	 * Get conversation by ID
	 */
	@Transactional(readOnly = true)
	public Optional<ConversationEntity> getConversation(String conversationId) {
		return conversationRepository.findByConversationId(conversationId);
	}

	/**
	 * Get all conversations for a user
	 */
	@Transactional(readOnly = true)
	public List<ConversationEntity> getUserConversations(String userId) {
		return conversationRepository.findByUserIdOrderByUpdateTimeDesc(userId);
	}

	/**
	 * Get user conversations with pagination
	 */
	@Transactional(readOnly = true)
	public Page<ConversationEntity> getUserConversations(String userId, int page, int size) {
		Pageable pageable = PageRequest.of(page, size);
		return conversationRepository.findByUserIdOrderByUpdateTimeDesc(userId, pageable);
	}

	/**
	 * Update conversation title
	 */
	public ConversationEntity updateConversationTitle(String conversationId, String newTitle) {
		Optional<ConversationEntity> conversationOpt = conversationRepository.findByConversationId(conversationId);
		if (conversationOpt.isEmpty()) {
			throw new IllegalArgumentException(getMessage("conversation.error.notFound", conversationId));
		}

		ConversationEntity conversation = conversationOpt.get();
		conversation.setTitle(newTitle);

		ConversationEntity updated = conversationRepository.save(conversation);
		logger.info("Updated conversation title: conversationId={}, newTitle={}", conversationId, newTitle);

		return updated;
	}

	/**
	 * Delete conversation
	 */
	public void deleteConversation(String conversationId) {
		Optional<ConversationEntity> conversationOpt = conversationRepository.findByConversationId(conversationId);
		if (conversationOpt.isEmpty()) {
			throw new IllegalArgumentException(getMessage("conversation.error.notFound", conversationId));
		}

		// Delete all related messages
		messageRepository.deleteByConversationId(conversationId);

		// Delete conversation
		conversationRepository.delete(conversationOpt.get());

		logger.info("Deleted conversation: conversationId={}", conversationId);
	}

	/**
	 * Add message to conversation
	 */
	public ConversationMessageEntity addMessage(String conversationId,
			ConversationMessageEntity.MessageType messageType, String content) {
		// Verify conversation exists
		Optional<ConversationEntity> conversationOpt = conversationRepository.findByConversationId(conversationId);
		if (conversationOpt.isEmpty()) {
			throw new IllegalArgumentException(getMessage("conversation.error.notFound", conversationId));
		}

		// Create message
		ConversationMessageEntity message = new ConversationMessageEntity(conversationId, messageType, content);
		ConversationMessageEntity saved = messageRepository.save(message);

		// Update conversation's last activity time and message count
		ConversationEntity conversation = conversationOpt.get();
		conversation.setMessageCount(conversation.getMessageCount() + 1);
		conversation.setLastMessageTime(LocalDateTime.now());
		conversationRepository.save(conversation);

		logger.debug("Added message to conversation: conversationId={}, messageType={}, messageId={}", conversationId, messageType,
				saved.getId());

		return saved;
	}

	/**
	 * Update message status
	 */
	public ConversationMessageEntity updateMessageStatus(Long messageId,
			ConversationMessageEntity.MessageStatus status) {
		Optional<ConversationMessageEntity> messageOpt = messageRepository.findById(messageId);
		if (messageOpt.isEmpty()) {
			throw new IllegalArgumentException(getMessage("conversation.message.error.notFound"));
		}

		ConversationMessageEntity message = messageOpt.get();
		message.setStatus(status);

		return messageRepository.save(message);
	}

	/**
	 * Update message content
	 */
	public ConversationMessageEntity updateMessageContent(Long messageId, String content) {
		Optional<ConversationMessageEntity> messageOpt = messageRepository.findById(messageId);
		if (messageOpt.isEmpty()) {
			throw new IllegalArgumentException(getMessage("conversation.message.error.notFound"));
		}

		ConversationMessageEntity message = messageOpt.get();
		message.setContent(content);
		message.setStatus(ConversationMessageEntity.MessageStatus.SUCCESS);

		return messageRepository.save(message);
	}

	/**
	 * Link message to planId
	 */
	public ConversationMessageEntity linkMessageToPlan(Long messageId, String planId) {
		Optional<ConversationMessageEntity> messageOpt = messageRepository.findById(messageId);
		if (messageOpt.isEmpty()) {
			throw new IllegalArgumentException(getMessage("conversation.message.error.notFound"));
		}

		ConversationMessageEntity message = messageOpt.get();
		message.setPlanId(planId);

		return messageRepository.save(message);
	}

	/**
	 * Get all messages for a conversation
	 */
	@Transactional(readOnly = true)
	public List<ConversationMessageEntity> getConversationMessages(String conversationId) {
		return messageRepository.findByConversationIdOrderByCreateTimeAsc(conversationId);
	}

	/**
	 * Get conversation messages with pagination
	 */
	@Transactional(readOnly = true)
	public Page<ConversationMessageEntity> getConversationMessages(String conversationId, int page, int size) {
		Pageable pageable = PageRequest.of(page, size);
		return messageRepository.findByConversationIdOrderByCreateTimeAsc(conversationId, pageable);
	}

	/**
	 * Get the latest N messages for a conversation
	 */
	@Transactional(readOnly = true)
	public List<ConversationMessageEntity> getRecentMessages(String conversationId, int limit) {
		Pageable pageable = PageRequest.of(0, limit);
		return messageRepository.findRecentMessages(conversationId, pageable);
	}

	/**
	 * Search conversations
	 */
	@Transactional(readOnly = true)
	public List<ConversationEntity> searchConversations(String userId, String keyword) {
		return conversationRepository.findByUserIdAndTitleContaining(userId, keyword);
	}

	/**
	 * Clean up expired conversations
	 */
	public int cleanupExpiredConversations(int daysToKeep) {
		LocalDateTime cutoffTime = LocalDateTime.now().minusDays(daysToKeep);
		List<ConversationEntity> expiredConversations = conversationRepository.findByUpdateTimeBefore(cutoffTime);

		int deletedCount = 0;
		for (ConversationEntity conversation : expiredConversations) {
			try {
				deleteConversation(conversation.getConversationId());
				deletedCount++;
			}
			catch (Exception e) {
				logger.error("Failed to clean up expired conversation: conversationId={}", conversation.getConversationId(), e);
			}
		}

		logger.info("Cleanup of expired conversations completed: deleted {} conversations", deletedCount);
		return deletedCount;
	}

	/**
	 * Generate conversation ID
	 */
	private String generateConversationId() {
		return "conv-" + UUID.randomUUID().toString().replace("-", "");
	}

	/**
	 * Count user conversations
	 */
	@Transactional(readOnly = true)
	public long countUserConversations(String userId) {
		return conversationRepository.countByUserId(userId);
	}

	/**
	 * Check if conversation exists
	 */
	@Transactional(readOnly = true)
	public boolean conversationExists(String conversationId) {
		return conversationRepository.existsByConversationId(conversationId);
	}

}
