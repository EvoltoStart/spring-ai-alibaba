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
package com.alibaba.cloud.ai.example.manus.conversation.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 会话消息实体类 用于存储会话中的每条消息
 */
@Entity
@Table(name = "conversation_message", indexes = { @Index(name = "idx_conversation_id", columnList = "conversationId"),
		@Index(name = "idx_create_time", columnList = "createTime") })
public class ConversationMessageEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * 关联的会话ID
	 */
	@Column(nullable = false)
	private String conversationId;

	/**
	 * 消息类型：USER, ASSISTANT, SYSTEM
	 */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private MessageType messageType;

	/**
	 * 消息内容
	 */
	@Column(columnDefinition = "TEXT")
	private String content;

	/**
	 * 关联的planId（如果消息触发了plan执行）
	 */
	@Column
	private String planId;

	/**
	 * 消息状态：SENDING, SUCCESS, FAILED
	 */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private MessageStatus status;

	/**
	 * 错误信息（如果消息处理失败）
	 */
	@Column(columnDefinition = "TEXT")
	private String errorMessage;

	/**
	 * 消息元数据（JSON格式，存储额外信息如文件引用等）
	 */
	@Column(columnDefinition = "TEXT")
	private String metadata;

	/**
	 * 创建时间
	 */
	@Column(nullable = false)
	private LocalDateTime createTime;

	/**
	 * 更新时间
	 */
	@Column(nullable = false)
	private LocalDateTime updateTime;

	public enum MessageType {

		USER, // 用户消息
		ASSISTANT, // AI助手回复
		SYSTEM // 系统消息

	}

	public enum MessageStatus {

		SENDING, // 发送中
		SUCCESS, // 成功
		FAILED // 失败

	}

	public ConversationMessageEntity() {
		this.createTime = LocalDateTime.now();
		this.updateTime = LocalDateTime.now();
		this.status = MessageStatus.SENDING;
	}

	public ConversationMessageEntity(String conversationId, MessageType messageType, String content) {
		this();
		this.conversationId = conversationId;
		this.messageType = messageType;
		this.content = content;
	}

	// Getters and Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getConversationId() {
		return conversationId;
	}

	public void setConversationId(String conversationId) {
		this.conversationId = conversationId;
	}

	public MessageType getMessageType() {
		return messageType;
	}

	public void setMessageType(MessageType messageType) {
		this.messageType = messageType;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
		this.updateTime = LocalDateTime.now();
	}

	public String getPlanId() {
		return planId;
	}

	public void setPlanId(String planId) {
		this.planId = planId;
		this.updateTime = LocalDateTime.now();
	}

	public MessageStatus getStatus() {
		return status;
	}

	public void setStatus(MessageStatus status) {
		this.status = status;
		this.updateTime = LocalDateTime.now();
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
		this.updateTime = LocalDateTime.now();
	}

	public String getMetadata() {
		return metadata;
	}

	public void setMetadata(String metadata) {
		this.metadata = metadata;
		this.updateTime = LocalDateTime.now();
	}

	public LocalDateTime getCreateTime() {
		return createTime;
	}

	public void setCreateTime(LocalDateTime createTime) {
		this.createTime = createTime;
	}

	public LocalDateTime getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(LocalDateTime updateTime) {
		this.updateTime = updateTime;
	}

	@Override
	public String toString() {
		return "ConversationMessageEntity{" + "id=" + id + ", conversationId='" + conversationId + '\''
				+ ", messageType=" + messageType + ", content='" + content + '\'' + ", planId='" + planId + '\''
				+ ", status=" + status + ", createTime=" + createTime + '}';
	}

}
