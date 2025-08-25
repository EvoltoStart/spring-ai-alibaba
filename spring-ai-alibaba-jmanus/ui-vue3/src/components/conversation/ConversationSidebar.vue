<!--
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
-->

<template>
  <div class="conversation-sidebar">
    <!-- 头部操作区 -->
    <div class="sidebar-header">
      <button @click="createNewConversation" class="new-conversation-btn" :disabled="loading">
        <Icon icon="carbon:add" />
        <span>{{ $t('conversationManagement.newConversation') }}</span>
      </button>
      
      <!-- 搜索框 -->
      <div class="search-container" v-if="hasConversations">
        <input
          type="text"
          v-model="searchKeyword"
          @input="handleSearch"
          :placeholder="$t('conversationManagement.searchPlaceholder')"
          class="search-input"
        />
        <Icon icon="carbon:search" class="search-icon" />
      </div>
    </div>

    <!-- 会话列表 -->
    <div class="conversation-list" v-if="hasConversations">
      <div
        v-for="conversation in displayConversations"
        :key="conversation.conversationId"
        :class="[
          'conversation-item',
          { 'active': conversation.conversationId === currentConversationId }
        ]"
        @click="selectConversation(conversation.conversationId)"
      >
        <div class="conversation-content">
          <div class="conversation-title" v-if="!conversation.editing">
            {{ conversation.title }}
          </div>
          <input
            v-else
            type="text"
            v-model="conversation.editTitle"
            @blur="finishEditTitle(conversation)"
            @keyup.enter="finishEditTitle(conversation)"
            @keyup.esc="cancelEditTitle(conversation)"
            class="title-input"
            ref="titleInput"
          />
          
          <div class="conversation-meta">
            <span class="message-count">{{ conversation.messageCount }} {{ $t('conversationManagement.messageCount') }}</span>
            <span class="last-time">{{ formatTime(conversation.lastMessageTime || conversation.updateTime) }}</span>
          </div>
        </div>

        <div class="conversation-actions">
          <button
            @click.stop="startEditTitle(conversation)"
            class="action-btn"
            title="重命名"
          >
            <Icon icon="carbon:edit" />
          </button>
          <button
            @click.stop="deleteConversation(conversation.conversationId)"
            class="action-btn delete-btn"
            title="删除"
          >
            <Icon icon="carbon:trash-can" />
          </button>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else-if="!loading" class="empty-state">
      <Icon icon="carbon:chat" class="empty-icon" />
              <p class="empty-text">{{ $t('conversationManagement.noConversations') }}</p>
        <p class="empty-hint">{{ $t('conversationManagement.noConversationsHint') }}</p>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <Icon icon="carbon:circle-dash" class="loading-icon" />
              <p>{{ $t('conversationManagement.loading') }}</p>
    </div>

    <!-- 错误提示 -->
    <div v-if="error" class="error-state">
      <Icon icon="carbon:warning" class="error-icon" />
              <p class="error-text">{{ error }}</p>
        <button @click="retryLoad" class="retry-btn">{{ $t('conversationManagement.retry') }}</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { storeToRefs } from 'pinia'
import { Icon } from '@iconify/vue'
import { useConversationStore } from '@/stores/conversation'
import type { Conversation } from '@/api/conversation-api-service'

// Store
const conversationStore = useConversationStore()

// Router
const router = useRouter()

// I18n
const { t } = useI18n()

// 响应式数据
const searchKeyword = ref('')
const searchResults = ref<Conversation[]>([])
const isSearching = ref(false)

// 使用 storeToRefs 保持响应性
const {
  conversations, 
  currentConversationId, 
  hasConversations, 
  loading, 
  error,
  initialized
} = storeToRefs(conversationStore)

const displayConversations = computed(() => {
  if (isSearching.value && searchResults.value.length > 0) {
    return searchResults.value
  }
  return conversations.value
})

// 现在editing和editTitle属性直接在store中管理，无需额外处理

// 方法
const createNewConversation = async () => {
  const newConversation = await conversationStore.createConversation(t('conversationManagement.newConversation'))
  if (newConversation) {
    // 更新路由到新会话
    router.push(`/conversation/${newConversation.conversationId}`)
  }
}

const selectConversation = async (conversationId: string) => {
  await conversationStore.selectConversation(conversationId)
  // 更新路由到选中的会话
  router.push(`/conversation/${conversationId}`)
}

const handleSearch = async () => {
  const keyword = searchKeyword.value.trim()
  
  if (keyword === '') {
    isSearching.value = false
    searchResults.value = []
    return
  }

  isSearching.value = true
  try {
    searchResults.value = await conversationStore.searchConversations(keyword)
      } catch (error) {
      console.error('Search failed:', error)
      searchResults.value = []
    }
}

const startEditTitle = (conversation: any) => {
  console.log('startEditTitle triggered!', conversation)
  
  // 现在conversation就是store中的响应式对象，直接修改即可
  conversation.editing = true
  conversation.editTitle = conversation.title
  console.log('After setting:', { editing: conversation.editing, editTitle: conversation.editTitle })
  
  nextTick(() => {
    const input = document.querySelector('.title-input') as HTMLInputElement
    console.log('Looking for input element:', input)
    if (input) {
      input.focus()
      input.select()
    }
  })
}

const finishEditTitle = async (conversation: any) => {
  const newTitle = conversation.editTitle?.trim()
  
  if (newTitle && newTitle !== conversation.title) {
    const success = await conversationStore.updateConversationTitle(conversation.conversationId, newTitle)
    if (success) {
      // updateConversationTitle 已经更新了 store 中的对象
      return
    }
  }
  
  conversation.editing = false
}

const cancelEditTitle = (conversation: any) => {
  conversation.editing = false
  conversation.editTitle = conversation.title
}

const deleteConversation = async (conversationId: string) => {
        if (confirm(t('conversationManagement.deleteConfirm'))) {
    await conversationStore.deleteConversation(conversationId)
  }
}

const retryLoad = () => {
  conversationStore.loadConversations()
}

const formatTime = (timeStr: string): string => {
  if (!timeStr) return ''
  
  const time = new Date(timeStr)
  
  // Check if date is valid
  if (isNaN(time.getTime())) {
    console.warn('Invalid date received:', timeStr)
    return t('conversationManagement.lastActivity')
  }
  
  const now = new Date()
  const diffMs = now.getTime() - time.getTime()
  const diffMinutes = Math.floor(diffMs / 60000)
  const diffHours = Math.floor(diffMs / 3600000)
  const diffDays = Math.floor(diffMs / 86400000)
  
  if (diffMinutes < 1) return t('conversationManagement.justNow')
  if (diffMinutes < 60) return `${diffMinutes} ${t('conversationManagement.minutesAgo')}`
  if (diffHours < 24) return `${diffHours} ${t('conversationManagement.hoursAgo')}`
  if (diffDays < 30) return `${diffDays} ${t('conversationManagement.daysAgo')}`
  
  return time.toLocaleDateString('zh-CN')
}

// Lifecycle hooks
onMounted(() => {
  // Only load if never initialized to avoid race conditions with parent component
  if (!initialized.value) {
    console.log('[ConversationSidebar] onMounted - Detected uninitialized, reloading conversation list')
    conversationStore.loadConversations()
  } else {
    console.log('[ConversationSidebar] onMounted - Data already initialized, skipping reload')
  }
})
</script>

<style lang="less" scoped>
.conversation-sidebar {
  width: 300px; /* 固定宽度，类似计划模板侧边栏 */
  height: 100vh;
  background: #1a1a1a; /* 深色背景 */
  border-right: 1px solid #2a2a2a;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  flex-shrink: 0; /* 防止被压缩 */
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid #2a2a2a; /* 深色边框 */
  background: rgba(255, 255, 255, 0.02); /* 半透明背景 */
}

.new-conversation-btn {
  width: 100%;
  padding: 12px 16px;
  background: #667eea; /* 主题色 */
  color: #ffffff;
  border: none;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;
  margin-bottom: 12px;

  &:hover:not(:disabled) {
    background: #5a67d8;
    transform: translateY(-1px);
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}

.search-container {
  position: relative;
}

.search-input {
  width: 100%;
  padding: 8px 12px 8px 36px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  font-size: 14px;
  background: rgba(255, 255, 255, 0.05);
  color: #ffffff;

  &::placeholder {
    color: rgba(255, 255, 255, 0.5);
  }

  &:focus {
    outline: none;
    border-color: #667eea;
    background: rgba(255, 255, 255, 0.08);
    box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
  }
}

.search-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: rgba(255, 255, 255, 0.5);
  font-size: 16px;
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;

  /* 自定义滚动条 */
  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(255, 255, 255, 0.1);
    border-radius: 3px;

    &:hover {
      background: rgba(255, 255, 255, 0.2);
    }
  }
}

.conversation-item {
  padding: 12px;
  margin-bottom: 4px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid transparent;
  color: #ffffff;

  &:hover {
    background: rgba(255, 255, 255, 0.05);
    border-color: rgba(255, 255, 255, 0.1);
    transform: translateY(-1px);
  }

  &.active {
    background: rgba(102, 126, 234, 0.1);
    border-color: #667eea;
  }
}

.conversation-content {
  flex: 1;
  min-width: 0;
}

.conversation-title {
  font-size: 14px;
  font-weight: 500;
  color: #ffffff; /* 白色文字 */
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.title-input {
  width: 100%;
  padding: 2px 4px;
  border: 1px solid #667eea;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 500;
  background: rgba(255, 255, 255, 0.05);
  color: #ffffff;

  &:focus {
    outline: none;
    box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2);
    background: rgba(255, 255, 255, 0.08);
  }
}

.conversation-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6); /* 半透明白色 */
}

.conversation-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.conversation-item:hover .conversation-actions {
  opacity: 1;
}

.action-btn {
  padding: 4px;
  border: none;
  background: none;
  color: rgba(255, 255, 255, 0.6);
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s;

  &:hover {
    background: rgba(255, 255, 255, 0.1);
    color: #ffffff;
  }

  &.delete-btn:hover {
    background: rgba(239, 68, 68, 0.1);
    color: #ef4444;
  }
}

.empty-state,
.loading-state,
.error-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 16px;
  text-align: center;
  color: rgba(255, 255, 255, 0.6);
}

.empty-icon,
.loading-icon,
.error-icon {
  font-size: 48px;
  color: rgba(255, 255, 255, 0.3);
  margin-bottom: 16px;
}

.loading-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.empty-text,
.error-text {
  font-size: 16px;
  font-weight: 500;
  color: #4b5563;
  margin-bottom: 8px;
}

.empty-hint {
  font-size: 14px;
  color: #6b7280;
}

.retry-btn {
  padding: 8px 16px;
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  margin-top: 12px;
}

.retry-btn:hover {
  background: #2563eb;
}
</style>