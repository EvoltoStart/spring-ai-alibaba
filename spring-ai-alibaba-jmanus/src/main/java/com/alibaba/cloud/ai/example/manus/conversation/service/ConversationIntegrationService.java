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
import com.alibaba.cloud.ai.example.manus.llm.LlmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Transactional
public class ConversationIntegrationService {

	private static final Logger logger = LoggerFactory.getLogger(ConversationIntegrationService.class);

	@Autowired
	private ConversationService conversationService;

	@Autowired
	private LlmService llmService;

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
	 * Process user message in conversation - directly communicate with LLM without going through planning system
	 */
	public CompletableFuture<ConversationMessageEntity> processUserMessage(String conversationId, String userMessage) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				// 1. Verify conversation exists
				Optional<ConversationEntity> conversationOpt = conversationService.getConversation(conversationId);
				if (conversationOpt.isEmpty()) {
					throw new IllegalArgumentException(getMessage("conversation.error.notFound", conversationId));
				}

				ConversationEntity conversation = conversationOpt.get();

				// 2. Check if this is the first message, if so generate title based on message content
				String currentTitle = conversation.getTitle();
				int messageCount = conversation.getMessageCount();
				
				// Check against possible default titles by checking with different locales
				boolean isDefaultTitle = false;
				try {
					// Check English default title
					String enTitle = messageSource.getMessage("conversation.default.title", null, Locale.ENGLISH);
					// Check Chinese default title
					String zhTitle = messageSource.getMessage("conversation.default.title", null, Locale.CHINESE);
					String zhCnTitle = messageSource.getMessage("conversation.default.title", null, Locale.SIMPLIFIED_CHINESE);
					
					isDefaultTitle = enTitle.equals(currentTitle) || 
						zhTitle.equals(currentTitle) || 
						zhCnTitle.equals(currentTitle);
				} catch (Exception e) {
					// Fallback: check if title matches current locale's default
					String defaultTitle = getMessage("conversation.default.title");
					isDefaultTitle = defaultTitle.equals(currentTitle);
				}
				
				logger.debug("Title generation check: messageCount={}, currentTitle='{}', isDefaultTitle={}", 
					messageCount, currentTitle, isDefaultTitle);
				
				boolean shouldGenerateTitle = messageCount == 0 && isDefaultTitle;

				// 3. Add user message to conversation
				ConversationMessageEntity userMsg = conversationService.addMessage(conversationId,
						ConversationMessageEntity.MessageType.USER, userMessage);

				// 4. Generate title after adding the message if needed
				if (shouldGenerateTitle) {
					String autoTitle = generateConversationTitle(userMessage);
					logger.info("Auto-generating title for conversation {}: '{}' -> '{}'", 
						conversationId, currentTitle, autoTitle);
					conversationService.updateConversationTitle(conversationId, autoTitle);
				} else {
					logger.debug("Skipping title generation: shouldGenerateTitle={}", shouldGenerateTitle);
				}

				// 5. Create AI response message (initial status: SENDING) 
				ConversationMessageEntity assistantMsg = conversationService.addMessage(conversationId,
						ConversationMessageEntity.MessageType.ASSISTANT, "");

				try {
					// 6. Get conversation history messages as context
					List<ConversationMessageEntity> historyMessages = conversationService
						.getConversationMessages(conversationId);

					// 7. Build conversation context (only include recent messages to avoid long context)
					List<Message> messages = buildChatMessages(historyMessages, userMessage);

					// 8. Call LLM directly
					ChatClient chatClient = llmService.getDefaultChatClient();
					ChatResponse response = chatClient.prompt().messages(messages).call().chatResponse();

					// 9. Get AI response content
					String responseContent = response.getResult().getOutput().getText();

					// 10. Update assistant message content and status
					conversationService.updateMessageContent(assistantMsg.getId(), responseContent);
					conversationService.updateMessageStatus(assistantMsg.getId(),
							ConversationMessageEntity.MessageStatus.SUCCESS);

					logger.info("Conversation message processing completed: conversationId={}, responseLength={}", conversationId,
							responseContent.length());

					return assistantMsg;

				}
				catch (Exception e) {
					logger.error("Failed to process conversation message: conversationId={}", conversationId, e);

					// Update assistant message to failed status
					conversationService.updateMessageStatus(assistantMsg.getId(),
							ConversationMessageEntity.MessageStatus.FAILED);
					conversationService.updateMessageContent(assistantMsg.getId(), getMessage("conversation.error.processingFailed") + ": " + e.getMessage());

					return assistantMsg;
				}

					}
		catch (Exception e) {
			logger.error("Conversation message processing exception: conversationId={}", conversationId, e);
			throw new RuntimeException(getMessage("conversation.error.processFailed") + ": " + e.getMessage(), e);
		}
		});
	}

	/**
	 * Build chat message list including conversation history context
	 */
	private List<Message> buildChatMessages(List<ConversationMessageEntity> historyMessages,
			String currentUserMessage) {
		List<Message> messages = historyMessages.stream()
			.filter(msg -> msg.getStatus() == ConversationMessageEntity.MessageStatus.SUCCESS) // Only include successful messages
			.sorted((a, b) -> a.getCreateTime().compareTo(b.getCreateTime())) // Sort by time
			.limit(10) // Only keep the latest 10 messages to avoid long context
			.map(msg -> {
				if (msg.getMessageType() == ConversationMessageEntity.MessageType.USER) {
					return new UserMessage(msg.getContent());
				}
				else {
					return new AssistantMessage(msg.getContent());
				}
			})
			.collect(Collectors.toList());

		// Add current user message
		messages.add(new UserMessage(currentUserMessage));

		return messages;
	}

	/**
	 * Get conversation message history for displaying conversation records
	 */
	public String getConversationHistory(String conversationId) {
		try {
			List<ConversationMessageEntity> messages = conversationService.getConversationMessages(conversationId);

			StringBuilder history = new StringBuilder();
			history.append("Conversation History:\n");

			for (ConversationMessageEntity message : messages) {
				String messageType = message.getMessageType() == ConversationMessageEntity.MessageType.USER ? "User"
						: "Assistant";
				history.append(
						String.format("- [%s] %s: %s\n", message.getCreateTime(), messageType, message.getContent()));
			}

			return history.toString();

		}
		catch (Exception e) {
			logger.error("Failed to get conversation history: conversationId={}", conversationId, e);
			return getMessage("conversation.error.historyFailed") + ": " + e.getMessage();
		}
	}

	/**
	 * Check if conversation can accept new messages
	 */
	public boolean canAcceptNewMessage(String conversationId) {
		try {
			Optional<ConversationEntity> conversationOpt = conversationService.getConversation(conversationId);
			if (conversationOpt.isEmpty()) {
				return false;
			}

			ConversationEntity conversation = conversationOpt.get();

			// Check conversation status
			if (conversation.getStatus() != ConversationEntity.ConversationStatus.ACTIVE) {
				return false;
			}

			// Can add other restrictions like message count limits, time limits, etc.
			return true;

		}
		catch (Exception e) {
			logger.error("Failed to check conversation status: conversationId={}", conversationId, e);
			return false;
		}
	}

	/**
	 * Generate conversation title based on user message
	 */
	private String generateConversationTitle(String userMessage) {
		if (userMessage == null || userMessage.trim().isEmpty()) {
			return getMessage("conversation.default.title");
		}

		// Simple title generation logic: take first 30 characters
		String title = userMessage.trim();
		if (title.length() > 30) {
			title = title.substring(0, 30) + "...";
		}

		// Remove line breaks and extra spaces
		title = title.replaceAll("[\\r\\n]+", " ").replaceAll("\\s+", " ");

		return title;
	}

}
