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

import com.alibaba.cloud.ai.example.manus.conversation.entity.ConversationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Conversation data access layer
 */
@Repository
public interface ConversationRepository extends JpaRepository<ConversationEntity, Long> {

	/**
	 * Find conversation by conversation ID
	 */
	Optional<ConversationEntity> findByConversationId(String conversationId);

	/**
	 * Find all conversations by user ID, ordered by update time descending
	 */
	List<ConversationEntity> findByUserIdOrderByUpdateTimeDesc(String userId);

	/**
	 * Find conversations by user ID with pagination, ordered by update time descending
	 */
	Page<ConversationEntity> findByUserIdOrderByUpdateTimeDesc(String userId, Pageable pageable);

	/**
	 * Find conversations by conversation status
	 */
	List<ConversationEntity> findByStatus(ConversationEntity.ConversationStatus status);

	/**
	 * Find conversations before specified time (for cleanup)
	 */
	List<ConversationEntity> findByUpdateTimeBefore(LocalDateTime cutoffTime);

	/**
	 * Count total conversations for a user
	 */
	long countByUserId(String userId);

	/**
	 * Find user's recent N conversations
	 */
	@Query("SELECT c FROM ConversationEntity c WHERE c.userId = :userId ORDER BY c.updateTime DESC")
	List<ConversationEntity> findRecentConversations(@Param("userId") String userId, Pageable pageable);

	/**
	 * Search conversations by title (fuzzy search)
	 */
	@Query("SELECT c FROM ConversationEntity c WHERE c.userId = :userId AND c.title LIKE %:title% ORDER BY c.updateTime DESC")
	List<ConversationEntity> findByUserIdAndTitleContaining(@Param("userId") String userId,
			@Param("title") String title);

	/**
	 * Delete all conversations for a user
	 */
	void deleteByUserId(String userId);

	/**
	 * Check if conversation ID exists
	 */
	boolean existsByConversationId(String conversationId);

}
