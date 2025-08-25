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
  <div class="direct-page">
    <div class="direct-chat">
          <!-- Conversation list sidebar -->
    <ConversationSidebar />
      
      <!-- Conversation area -->
      <div class="chat-panel">
        <div class="chat-header">
          <button class="back-button" @click="goBack">
            <Icon icon="carbon:arrow-left" />
          </button>
          <h2>{{ $t('conversationManagement.title') }}</h2>
          <div class="header-actions">
            <LanguageSwitcher />
            <button class="config-button" @click="createNewConversation" :title="$t('conversationManagement.newConversation')">
              <Icon icon="carbon:add" width="20" />
            </button>
            <button class="config-button" @click="handleConfig" :title="$t('config.title')">
              <Icon icon="carbon:settings-adjust" width="20" />
            </button>
            <button class="cron-task-btn" @click="showCronTaskModal = true" :title="$t('cronTask.title')">
              <Icon icon="carbon:alarm" width="20" />
            </button>
            <button class="cron-task-btn" @click="memoryStore.toggleSidebar()" :title="$t('memory.title')">
              <Icon icon="carbon:calendar" width="20" />
            </button>
          </div>
        </div>

        <!-- Chat content area -->
        <div class="chat-content">
          <ConversationView />
        </div>
      </div>
    </div>

    <!-- Cron Task Modal -->
    <CronTaskModal v-model="showCronTaskModal" />

    <!-- Memory Modal -->
    <Memory @memory-selected="memorySelected" />

    <!-- Error toast -->
    <div v-if="error" class="error-toast" @click="clearError">
      <Icon icon="carbon:warning" />
      <span>{{ error }}</span>
      <button class="close-btn">
        <Icon icon="carbon:close" />
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Icon } from '@iconify/vue'
import ConversationSidebar from '@/components/conversation/ConversationSidebar.vue'
import ConversationView from '@/components/conversation/ConversationView.vue'
import LanguageSwitcher from '@/components/language-switcher/index.vue'
import CronTaskModal from '@/components/cron-task-modal/index.vue'
import Memory from '@/components/memory/index.vue'
import { useConversationStore } from '@/stores/conversation'
import { memoryStore } from '@/stores/memory'

// Router
const router = useRouter()
const route = useRoute()

// Store
const conversationStore = useConversationStore()

// Reactive data
const showCronTaskModal = ref(false)

// Computed properties
const { error } = conversationStore



// Methods
const goBack = () => {
  router.push('/home')
}

const createNewConversation = async () => {
  const newConversation = await conversationStore.createConversation()
  if (newConversation) {
    // Update route to new conversation
    router.push(`/conversation/${newConversation.conversationId}`)
  }
}

const handleConfig = () => {
  router.push('/configs')
}

const memorySelected = (memory: any) => {
  console.log('Memory selected:', memory)
  // 处理内存选择逻辑
}

const clearError = () => {
  conversationStore.clearError()
}

// Lifecycle
onMounted(async () => {
  console.log('[ConversationPage] onMounted - Starting initialization')
  
  // Set default user ID (in real application should get from authentication system)
  conversationStore.userId = 'default-user'
  
  // Load conversation list
  console.log('[ConversationPage] Starting to load conversation list')
  await conversationStore.loadConversations()
  console.log('[ConversationPage] Conversation list loading completed:', {
    conversations: conversationStore.conversations.length,
    hasConversations: conversationStore.hasConversations
  })
  
  // Handle route parameters: if URL has conversationId, select that conversation
  const routeConversationId = route.params.id as string
  if (routeConversationId) {
    console.log('[ConversationPage] Checking route parameter conversation:', routeConversationId)
    // Check if the conversation exists in the list
    const conversation = conversationStore.conversations.find(c => c.conversationId === routeConversationId)
    if (conversation) {
      console.log('[ConversationPage] Selecting route specified conversation')
      await conversationStore.selectConversation(routeConversationId)
    } else {
      console.log('[ConversationPage] Route specified conversation does not exist, redirecting')
      // Conversation does not exist, redirect to conversation home page
      router.replace('/conversation')
    }
  } else {
    // If no route parameters but conversation list exists, automatically select the first one
    if (!conversationStore.currentConversation && conversationStore.conversations.length > 0) {
      console.log('[ConversationPage] Automatically selecting first conversation')
      await conversationStore.selectConversation(conversationStore.conversations[0].conversationId)
    }
  }
  
  console.log('[ConversationPage] Initialization completed')
})

onBeforeUnmount(() => {
  // Clean up state
  conversationStore.clearError()
})
</script>

<style lang="less" scoped>
.direct-page {
  width: 100%;
  display: flex;
  position: relative;
}

.direct-chat {
  height: 100vh;
  width: 100%;
  background: #0a0a0a;
  display: flex;
}

.chat-panel {
  flex: 1; /* Occupy remaining space */
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
}





.chat-header {
  padding: 20px 24px;
  border-bottom: 1px solid #1a1a1a;
  display: flex;
  align-items: center;
  gap: 16px;
  background: rgba(255, 255, 255, 0.02);
  flex-shrink: 0; /* Ensure the header will not be compressed */
  position: sticky; /* Fix the header at the top */
  top: 0;
  z-index: 100;

  h2 {
    flex: 1;
    margin: 0;
    font-size: 18px;
    font-weight: 600;
    color: #ffffff;
  }
}

.chat-content {
  flex: 1; /* Occupy remaining space */
  display: flex;
  flex-direction: column;
  min-height: 0; /* Allow shrink */
  overflow: hidden; /* Prevent overflow */
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.back-button {
  padding: 8px 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.05);
  color: #ffffff;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;

  &:hover {
    background: rgba(255, 255, 255, 0.1);
    border-color: rgba(255, 255, 255, 0.2);
  }
}

.config-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.05);
  color: #ffffff;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: rgba(255, 255, 255, 0.1);
    border-color: rgba(255, 255, 255, 0.2);
  }
}

.cron-task-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.05);
  color: #ffffff;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: rgba(255, 255, 255, 0.1);
    border-color: rgba(255, 255, 255, 0.2);
  }
}



/* Message toast styles */
.error-toast {
  position: fixed;
  top: 80px;
  right: 24px;
  z-index: 9999;
  min-width: 320px;
  max-width: 480px;
  padding: 16px 20px;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  display: flex;
  align-items: center;
  justify-content: space-between;
  animation: slideInRight 0.3s ease-out;
  font-size: 14px;
  font-weight: 500;
  color: #fff2f0;
  background-color: #ff4d4f;

  .close-btn {
    background: none;
    border: none;
    color: inherit;
    cursor: pointer;
    margin-left: 8px;
  }
}

@keyframes slideInRight {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}


</style>