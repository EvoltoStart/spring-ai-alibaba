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

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Conversation entity class
 */
@Entity
@Table(name = "conversations")
public class ConversationEntity {

	@Id
	private String id; // Use UUID as conversation ID

	@Column(nullable = false)
	private String conversationId; // External conversation identifier

	@Column(nullable = false)
	private String userId; // User identifier

	@Column(nullable = false)
	private String title;

	@Column(length = 500)
	private String description;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@Column(nullable = false)
	private LocalDateTime createTime;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@Column(nullable = false)
	private LocalDateTime updateTime;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private ConversationStatus status; // ACTIVE, PROCESSING, ARCHIVED, DELETED

	@Column(nullable = true)
	private String currentPlanId; // If a conversation is linked to an ongoing plan

	@Column(nullable = false)
	private int messageCount = 0; // Total message count

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@Column(nullable = true)
	private LocalDateTime lastMessageTime; // Time of last message

	@Column(length = 1000)
	private String metadata; // Additional metadata as JSON

	public ConversationEntity() {
		this.id = UUID.randomUUID().toString();
		this.conversationId = UUID.randomUUID().toString();
		this.createTime = LocalDateTime.now();
		this.updateTime = LocalDateTime.now();
		this.status = ConversationStatus.ACTIVE;
	}

	public ConversationEntity(String userId, String title) {
		this();
		this.userId = userId;
		this.title = title;
	}

	// Getters and Setters
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getConversationId() {
		return conversationId;
	}

	public void setConversationId(String conversationId) {
		this.conversationId = conversationId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
		this.updateTime = LocalDateTime.now();
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
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

	public ConversationStatus getStatus() {
		return status;
	}

	public void setStatus(ConversationStatus status) {
		this.status = status;
		this.updateTime = LocalDateTime.now();
	}

	public String getCurrentPlanId() {
		return currentPlanId;
	}

	public void setCurrentPlanId(String currentPlanId) {
		this.currentPlanId = currentPlanId;
		this.updateTime = LocalDateTime.now();
	}

	public int getMessageCount() {
		return messageCount;
	}

	public void setMessageCount(int messageCount) {
		this.messageCount = messageCount;
		this.updateTime = LocalDateTime.now();
	}

	public void incrementMessageCount() {
		this.messageCount++;
		this.updateTime = LocalDateTime.now();
	}

	public LocalDateTime getLastMessageTime() {
		return lastMessageTime;
	}

	public void setLastMessageTime(LocalDateTime lastMessageTime) {
		this.lastMessageTime = lastMessageTime;
		this.updateTime = LocalDateTime.now();
	}

	public String getMetadata() {
		return metadata;
	}

	public void setMetadata(String metadata) {
		this.metadata = metadata;
		this.updateTime = LocalDateTime.now();
	}

	/**
	 * Conversation status enumeration
	 */
	public enum ConversationStatus {

		ACTIVE, PROCESSING, // Indicates an AI response is being generated
		ARCHIVED, DELETED

	}

	@Override
	public String toString() {
		return "ConversationEntity{" + "id='" + id + '\'' + ", conversationId='" + conversationId + '\'' + ", userId='"
				+ userId + '\'' + ", title='" + title + '\'' + ", status=" + status + ", messageCount=" + messageCount
				+ ", createTime=" + createTime + ", updateTime=" + updateTime + '}';
	}

}
