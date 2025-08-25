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
package com.alibaba.cloud.ai.example.manus.conversation.repository;

import com.alibaba.cloud.ai.example.manus.conversation.entity.ConversationMessageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话消息数据访问层
 */
@Repository
public interface ConversationMessageRepository extends JpaRepository<ConversationMessageEntity, Long> {

	/**
	 * 根据会话ID查找所有消息，按创建时间顺序
	 */
	List<ConversationMessageEntity> findByConversationIdOrderByCreateTimeAsc(String conversationId);

	/**
	 * 根据会话ID分页查找消息，按创建时间顺序
	 */
	Page<ConversationMessageEntity> findByConversationIdOrderByCreateTimeAsc(String conversationId, Pageable pageable);

	/**
	 * 根据会话ID查找最新的N条消息
	 */
	@Query("SELECT m FROM ConversationMessageEntity m WHERE m.conversationId = :conversationId ORDER BY m.createTime DESC")
	List<ConversationMessageEntity> findRecentMessages(@Param("conversationId") String conversationId,
			Pageable pageable);

	/**
	 * 根据planId查找关联的消息
	 */
	List<ConversationMessageEntity> findByPlanId(String planId);

	/**
	 * 根据消息状态查找消息
	 */
	List<ConversationMessageEntity> findByStatus(ConversationMessageEntity.MessageStatus status);

	/**
	 * 统计会话的消息总数
	 */
	long countByConversationId(String conversationId);

	/**
	 * 查找指定时间之前的消息（用于清理）
	 */
	List<ConversationMessageEntity> findByCreateTimeBefore(LocalDateTime cutoffTime);

	/**
	 * 删除会话的所有消息
	 */
	void deleteByConversationId(String conversationId);

	/**
	 * 查找会话中最后一条消息
	 */
	@Query("SELECT m FROM ConversationMessageEntity m WHERE m.conversationId = :conversationId ORDER BY m.createTime DESC LIMIT 1")
	ConversationMessageEntity findLastMessage(@Param("conversationId") String conversationId);

	/**
	 * 查找会话中指定类型的消息
	 */
	List<ConversationMessageEntity> findByConversationIdAndMessageTypeOrderByCreateTimeAsc(String conversationId,
			ConversationMessageEntity.MessageType messageType);

	/**
	 * 统计会话中指定状态的消息数量
	 */
	long countByConversationIdAndStatus(String conversationId, ConversationMessageEntity.MessageStatus status);

}
