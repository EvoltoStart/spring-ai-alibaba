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
package com.alibaba.cloud.ai.example.manus.conversation.controller;

import com.alibaba.cloud.ai.example.manus.conversation.entity.ConversationEntity;
import com.alibaba.cloud.ai.example.manus.conversation.entity.ConversationMessageEntity;
import com.alibaba.cloud.ai.example.manus.conversation.service.ConversationService;
import com.alibaba.cloud.ai.example.manus.conversation.service.ConversationIntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 会话管理控制器 提供会话和消息的REST API
 */
@RestController
@RequestMapping("/api/conversation")
@CrossOrigin(origins = "*")
public class ConversationController {

	private static final Logger logger = LoggerFactory.getLogger(ConversationController.class);

	@Autowired
	private ConversationService conversationService;

	@Autowired
	private ConversationIntegrationService integrationService;

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
	 * 创建新会话
	 */
	@PostMapping("/create")
	public ResponseEntity<Map<String, Object>> createConversation(@RequestBody Map<String, Object> request) {
		try {
			String userId = (String) request.get("userId");
			String title = (String) request.get("title");

			if (!StringUtils.hasText(userId)) {
				return ResponseEntity.badRequest().body(Map.of("error", getMessage("conversation.error.userIdEmpty")));
			}

			ConversationEntity conversation = conversationService.createConversation(userId, title);

			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("conversation", conversation);
			response.put("message", getMessage("conversation.success.created"));

			return ResponseEntity.ok(response);

		}
		catch (Exception e) {
			logger.error("创建会话失败", e);
			return ResponseEntity.internalServerError().body(Map.of("error", getMessage("conversation.error.createFailed") + ": " + e.getMessage()));
		}
	}

	/**
	 * 获取用户的所有会话
	 */
	@GetMapping("/list")
	public ResponseEntity<Map<String, Object>> getUserConversations(@RequestParam String userId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		try {
			if (!StringUtils.hasText(userId)) {
				return ResponseEntity.badRequest().body(Map.of("error", getMessage("conversation.error.userIdEmpty")));
			}

			Page<ConversationEntity> conversations = conversationService.getUserConversations(userId, page, size);

			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("conversations", conversations.getContent());
			response.put("totalPages", conversations.getTotalPages());
			response.put("totalElements", conversations.getTotalElements());
			response.put("currentPage", page);

			return ResponseEntity.ok(response);

		}
		catch (Exception e) {
			logger.error("获取用户会话失败", e);
			return ResponseEntity.internalServerError().body(Map.of("error", getMessage("conversation.error.getFailed") + ": " + e.getMessage()));
		}
	}

	/**
	 * 获取会话详情
	 */
	@GetMapping("/{conversationId}")
	public ResponseEntity<Map<String, Object>> getConversation(@PathVariable String conversationId) {
		try {
			Optional<ConversationEntity> conversationOpt = conversationService.getConversation(conversationId);

			if (conversationOpt.isEmpty()) {
				return ResponseEntity.notFound().build();
			}

			ConversationEntity conversation = conversationOpt.get();
			List<ConversationMessageEntity> messages = conversationService.getConversationMessages(conversationId);

			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("conversation", conversation);
			response.put("messages", messages);

			return ResponseEntity.ok(response);

		}
		catch (Exception e) {
			logger.error("获取会话详情失败: conversationId={}", conversationId, e);
			return ResponseEntity.internalServerError().body(Map.of("error", getMessage("conversation.error.getDetailFailed") + ": " + e.getMessage()));
		}
	}

	/**
	 * 更新会话标题
	 */
	@PutMapping("/{conversationId}/title")
	public ResponseEntity<Map<String, Object>> updateConversationTitle(@PathVariable String conversationId,
			@RequestBody Map<String, Object> request) {
		try {
			String newTitle = (String) request.get("title");

			if (!StringUtils.hasText(newTitle)) {
				return ResponseEntity.badRequest().body(Map.of("error", getMessage("conversation.error.titleEmpty")));
			}

			ConversationEntity conversation = conversationService.updateConversationTitle(conversationId, newTitle);

			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("conversation", conversation);
			response.put("message", getMessage("conversation.success.titleUpdated"));

			return ResponseEntity.ok(response);

		}
		catch (IllegalArgumentException e) {
			return ResponseEntity.notFound().build();
		}
		catch (Exception e) {
			logger.error("更新会话标题失败: conversationId={}", conversationId, e);
			return ResponseEntity.internalServerError().body(Map.of("error", getMessage("conversation.error.updateTitleFailed") + ": " + e.getMessage()));
		}
	}

	/**
	 * 删除会话
	 */
	@DeleteMapping("/{conversationId}")
	public ResponseEntity<Map<String, Object>> deleteConversation(@PathVariable String conversationId) {
		try {
			conversationService.deleteConversation(conversationId);

			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("message", getMessage("conversation.success.deleted"));

			return ResponseEntity.ok(response);

		}
		catch (IllegalArgumentException e) {
			return ResponseEntity.notFound().build();
		}
		catch (Exception e) {
			logger.error("删除会话失败: conversationId={}", conversationId, e);
			return ResponseEntity.internalServerError().body(Map.of("error", getMessage("conversation.error.deleteFailed") + ": " + e.getMessage()));
		}
	}

	/**
	 * 添加消息到会话
	 */
	@PostMapping("/{conversationId}/messages")
	public ResponseEntity<Map<String, Object>> addMessage(@PathVariable String conversationId,
			@RequestBody Map<String, Object> request) {
		try {
			String messageTypeStr = (String) request.get("messageType");
			String content = (String) request.get("content");

			if (!StringUtils.hasText(messageTypeStr) || !StringUtils.hasText(content)) {
				return ResponseEntity.badRequest().body(Map.of("error", getMessage("conversation.error.messageTypeOrContentEmpty")));
			}

			ConversationMessageEntity.MessageType messageType = ConversationMessageEntity.MessageType
				.valueOf(messageTypeStr.toUpperCase());

			// 如果是用户消息，使用集成服务处理（包含AI响应）
			if (messageType == ConversationMessageEntity.MessageType.USER) {
				// 检查会话是否可以接收新消息
				if (!integrationService.canAcceptNewMessage(conversationId)) {
					return ResponseEntity.badRequest().body(Map.of("error", getMessage("conversation.error.cannotAcceptMessage")));
				}

				// 异步处理用户消息并生成AI响应
				integrationService.processUserMessage(conversationId, content);

				Map<String, Object> response = new HashMap<>();
				response.put("success", true);
				response.put("message", getMessage("conversation.success.messageProcessing"));

				return ResponseEntity.ok(response);
			}
			else {
				// 直接添加系统消息或其他类型消息
				ConversationMessageEntity message = conversationService.addMessage(conversationId, messageType,
						content);

				Map<String, Object> response = new HashMap<>();
				response.put("success", true);
				response.put("message", message);

				return ResponseEntity.ok(response);
			}

		}
		catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("error", getMessage("conversation.error.invalidMessageTypeOrConversationNotFound")));
		}
		catch (Exception e) {
			logger.error("添加消息失败: conversationId={}", conversationId, e);
			return ResponseEntity.internalServerError().body(Map.of("error", getMessage("conversation.error.addMessageFailed") + ": " + e.getMessage()));
		}
	}

	/**
	 * 获取会话消息
	 */
	@GetMapping("/{conversationId}/messages")
	public ResponseEntity<Map<String, Object>> getConversationMessages(@PathVariable String conversationId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
		try {
			Page<ConversationMessageEntity> messages = conversationService.getConversationMessages(conversationId, page,
					size);

			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("messages", messages.getContent());
			response.put("totalPages", messages.getTotalPages());
			response.put("totalElements", messages.getTotalElements());
			response.put("currentPage", page);

			return ResponseEntity.ok(response);

		}
		catch (Exception e) {
			logger.error("获取会话消息失败: conversationId={}", conversationId, e);
			return ResponseEntity.internalServerError().body(Map.of("error", getMessage("conversation.error.getMessagesFailed") + ": " + e.getMessage()));
		}
	}

	/**
	 * 搜索会话
	 */
	@GetMapping("/search")
	public ResponseEntity<Map<String, Object>> searchConversations(@RequestParam String userId,
			@RequestParam String keyword) {
		try {
			if (!StringUtils.hasText(userId) || !StringUtils.hasText(keyword)) {
				return ResponseEntity.badRequest().body(Map.of("error", getMessage("conversation.error.userIdOrKeywordEmpty")));
			}

			List<ConversationEntity> conversations = conversationService.searchConversations(userId, keyword);

			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("conversations", conversations);

			return ResponseEntity.ok(response);

		}
		catch (Exception e) {
			logger.error("搜索会话失败: userId={}, keyword={}", userId, keyword, e);
			return ResponseEntity.internalServerError().body(Map.of("error", getMessage("conversation.error.searchFailed") + ": " + e.getMessage()));
		}
	}

	/**
	 * 更新消息状态
	 */
	@PutMapping("/messages/{messageId}/status")
	public ResponseEntity<Map<String, Object>> updateMessageStatus(@PathVariable Long messageId,
			@RequestBody Map<String, Object> request) {
		try {
			String statusStr = (String) request.get("status");

			if (!StringUtils.hasText(statusStr)) {
				return ResponseEntity.badRequest().body(Map.of("error", getMessage("conversation.error.statusEmpty")));
			}

			ConversationMessageEntity.MessageStatus status = ConversationMessageEntity.MessageStatus
				.valueOf(statusStr.toUpperCase());
			ConversationMessageEntity message = conversationService.updateMessageStatus(messageId, status);

			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("message", message);

			return ResponseEntity.ok(response);

		}
		catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("error", getMessage("conversation.error.invalidStatusOrMessageNotFound")));
		}
		catch (Exception e) {
			logger.error("更新消息状态失败: messageId={}", messageId, e);
			return ResponseEntity.internalServerError().body(Map.of("error", getMessage("conversation.error.updateStatusFailed") + ": " + e.getMessage()));
		}
	}

	/**
	 * 关联消息到计划
	 */
	@PutMapping("/messages/{messageId}/link-plan")
	public ResponseEntity<Map<String, Object>> linkMessageToPlan(@PathVariable Long messageId,
			@RequestBody Map<String, Object> request) {
		try {
			String planId = (String) request.get("planId");

			if (!StringUtils.hasText(planId)) {
				return ResponseEntity.badRequest().body(Map.of("error", getMessage("conversation.error.planIdEmpty")));
			}

			ConversationMessageEntity message = conversationService.linkMessageToPlan(messageId, planId);

			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("message", message);

			return ResponseEntity.ok(response);

		}
		catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("error", getMessage("conversation.error.messageNotFound")));
		}
		catch (Exception e) {
			logger.error("关联消息到计划失败: messageId={}", messageId, e);
			return ResponseEntity.internalServerError().body(Map.of("error", getMessage("conversation.error.linkFailed") + ": " + e.getMessage()));
		}
	}

}
