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
  <div class="conversation-view">
    <!-- Main chat area -->
    <div class="chat-area" v-if="conversationStore.currentConversation">

      <!-- Message list - using same structure as ChatContainer -->
      <div class="messages" ref="messagesContainer">
        <div
          v-for="message in conversationStore.currentMessages"
          :key="message.id"
          class="message"
          :class="{ 
            user: message.messageType === 'USER', 
            assistant: message.messageType === 'ASSISTANT',
            sending: message.status === 'SENDING'
          }"
        >
          <div class="message-content">
            <!-- User message -->
            <div v-if="message.messageType === 'USER'" class="user-message">
              {{ message.content }}
            </div>

            <!-- AI message - using same response-section structure as plan-act -->
            <div v-else-if="message.messageType === 'ASSISTANT'" class="assistant-message">
              <div class="response-section">
                <div class="response-header">
                  <div class="response-avatar">
                    <Icon icon="carbon:bot" class="bot-icon" />
                  </div>
                  <div class="response-name">{{ $t('chat.botName') }}</div>
                </div>
                <div class="response-content">
                  <div class="final-response">
                    <div class="response-text" v-html="formatResponseText(message.content)"></div>
                  </div>
                </div>
              </div>
            </div>

            <!-- System message -->
            <div v-else-if="message.messageType === 'SYSTEM'" class="system-message">
              <span class="system-text">{{ message.content }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Input area - reuse InputArea component -->
      <InputArea
        :key="$i18n.locale"
        ref="inputRef"
        :disabled="sendingMessage"
        :placeholder="sendingMessage ? $t('conversationManagement.sending') : $t('conversationManagement.inputPlaceholder')"
        :initial-value="messageInput"
        @send="handleSendMessage"
        @clear="handleInputClear"
        @plan-mode-clicked="handlePlanModeClicked"
        @conversation-mode-clicked="handleConversationModeClicked"
      />
    </div>

    <!-- Empty state -->
    <div v-else class="empty-chat">
      <div class="empty-content">
        <h3 class="empty-title">{{ $t('conversationManagement.selectConversation') }}</h3>
        <p class="empty-hint">{{ $t('conversationManagement.selectConversationHint') }}</p>
        <button @click="createNewConversation" class="create-new-btn">
          {{ $t('conversationManagement.newConversation') }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useConversationStore } from '@/stores/conversation'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { Icon } from '@iconify/vue'
import InputArea from '@/components/input/index.vue'
import type { InputMessage } from '@/stores/memory'

// Store
const conversationStore = useConversationStore()

// Router
const router = useRouter()

const { t } = useI18n()

// Reactive data
const messageInput = ref('')
const sendingMessage = ref(false)
const messagesContainer = ref<HTMLElement>()
const inputRef = ref()



const formatResponseText = (text: string): string => {
  if (!text) return ''

  try {
    const rawHtml = marked.parse(text)
    // Sanitize to avoid XSS
    return DOMPurify.sanitize(rawHtml as string)
  } catch (e) {
    console.error('Markdown render error:', e)
    // Fallback: preserve original simple formatting
    let fallback = text.replace(/\n\n/g, '<br><br>').replace(/\n/g, '<br>')
    fallback = fallback.replace(/(<br><br>)/g, '</p><p>')
    if (fallback.includes('</p><p>')) fallback = `<p>${fallback}</p>`
    return fallback
  }
}

// 方法 - 来自InputArea组件的事件处理
const handleSendMessage = async (message: InputMessage) => {
  if (!message.input.trim() || !conversationStore.currentConversation) return

  const conversationId = conversationStore.currentConversation.conversationId
  const userMessage = message.input.trim()
  
  let tempUserMessage: any = null
  let tempAIMessage: any = null

  try {
    sendingMessage.value = true
    
    console.log('[ConversationView] Sending message:', userMessage)
    
    // Immediately add user message to local state (optimistic update)
    tempUserMessage = {
      id: Date.now(), // Temporary ID
      conversationId: conversationId,
      messageType: 'USER' as const,
      content: userMessage,
      status: 'SUCCESS' as const,
      createTime: new Date().toISOString(),
      updateTime: new Date().toISOString()
    }
    
    // Immediately update UI to display user message
    conversationStore.currentMessages.push(tempUserMessage)
    
    // Immediately scroll to bottom to display user message
    nextTick(() => {
      if (messagesContainer.value) {
        messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
      }
    })
    
    // Add temporary AI loading message
    tempAIMessage = {
      id: Date.now() + 1, // Temporary ID
      conversationId: conversationId,
      messageType: 'ASSISTANT' as const,
      content: '<span class="thinking-dots">' + t('conversationManagement.thinking') + '</span>',
      status: 'SENDING' as const,
      createTime: new Date().toISOString(),
      updateTime: new Date().toISOString()
    }
    
    conversationStore.currentMessages.push(tempAIMessage)
    
    // Scroll to bottom to display loading message
    nextTick(() => {
      if (messagesContainer.value) {
        messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
      }
    })
    
    // Send message to backend
    await conversationStore.addMessage(conversationId, 'USER', userMessage)
    
    // Wait for AI response (poll for new messages)
    await waitForAIResponse(conversationId)
    
    // Clean up temporary messages after AI response
    if (tempUserMessage || tempAIMessage) {
      conversationStore.currentMessages = conversationStore.currentMessages.filter(msg => 
        !(msg.id === tempUserMessage?.id || msg.id === tempAIMessage?.id)
      )
    }
    
  } catch (error) {
    console.error('Failed to send message:', error)
    // Also clean up temporary messages on error
    if (tempUserMessage || tempAIMessage) {
      conversationStore.currentMessages = conversationStore.currentMessages.filter(msg => 
        !(msg.id === tempUserMessage?.id || msg.id === tempAIMessage?.id)
      )
    }
  } finally {
    sendingMessage.value = false
  }
}

// Polling method to wait for AI response
const waitForAIResponse = async (conversationId: string) => {
  const maxAttempts = 60 // Maximum wait time 60 seconds (AI processing may take time)
  let attempts = 0
  
  // Get backend actual message count as baseline (excluding temporary messages)
  let initialMessageCount = 0
  let lastUserMessageId = null
  try {
    const conversationApiService = (await import('@/api/conversation-api-service')).conversationApiService
    const initialResponse = await conversationApiService.getConversationMessages(conversationId)
    if (initialResponse.success && initialResponse.messages) {
      initialMessageCount = initialResponse.messages.length
      // Record the last message ID before sending to determine if there's a new AI response
      if (initialResponse.messages.length > 0) {
        const lastMessage = initialResponse.messages[initialResponse.messages.length - 1]
        if (lastMessage.messageType === 'USER') {
          lastUserMessageId = lastMessage.id
        }
      }
    }
  } catch (error) {
    console.error('[ConversationView] Failed to get initial message count:', error)
    return
  }
  
  console.log('[ConversationView] Starting to wait for AI response, backend initial message count:', initialMessageCount, 'Last user message ID:', lastUserMessageId)
  
  while (attempts < maxAttempts) {
    await new Promise(resolve => setTimeout(resolve, 1000)) // 等待1秒
    
    try {
      // Directly call API to get messages without triggering store loading state
      const conversationApiService = (await import('@/api/conversation-api-service')).conversationApiService
      const response = await conversationApiService.getConversationMessages(conversationId)
      
      if (response.success && response.messages) {
        const currentMessageCount = response.messages.length
        console.log('[ConversationView] Polling check, current message count:', currentMessageCount, 'Initial count:', initialMessageCount)
        
        // Check if there's a new AI response:
        // 1. Message count increased
        // 2. Last message is ASSISTANT type
        // 3. Last message status is SUCCESS
        if (currentMessageCount > initialMessageCount && response.messages.length > 0) {
          const lastMessage = response.messages[response.messages.length - 1]
          console.log('[ConversationView] Last message:', lastMessage.messageType, lastMessage.status)
          
          if (lastMessage.messageType === 'ASSISTANT' && lastMessage.status === 'SUCCESS') {
            console.log('[ConversationView] AI response detected, stopping polling, updating local messages')
            
            // Update local message state (without triggering loading)
            conversationStore.currentMessages = response.messages
            
            // Scroll to bottom
            nextTick(() => {
              if (messagesContainer.value) {
                messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
              }
            })
            break
          }
        }
      }
    } catch (error) {
      console.error('[ConversationView] Failed to check messages during polling:', error)
    }
    
    attempts++
  }
  
  if (attempts >= maxAttempts) {
    console.log('[ConversationView] AI response wait timeout')
    // Also try to update messages once on timeout
    try {
      const conversationApiService = (await import('@/api/conversation-api-service')).conversationApiService
      const response = await conversationApiService.getConversationMessages(conversationId)
      if (response.success && response.messages) {
        conversationStore.currentMessages = response.messages
      }
    } catch (error) {
      console.error('[ConversationView] Failed to update messages after timeout:', error)
    }
  }
}

// Clear input handler - event from InputArea component
const handleInputClear = () => {
  messageInput.value = ''
}

const createNewConversation = async () => {
  await conversationStore.createConversation()
}

// Mode switching handlers
const handlePlanModeClicked = () => {
  console.log('[ConversationView] Plan mode button clicked')
  // Navigate to plan-act page
  router.push('/direct')
}

const handleConversationModeClicked = () => {
  console.log('[ConversationView] Conversation mode button clicked')
  // Already on conversation page, no action needed
}
</script>

<style lang="less" scoped>
.conversation-view {
  flex: 1;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #0a0a0a; /* Dark background */
}

.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

/* Message area styles - consistent with ChatContainer */
.messages {
  padding: 24px;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow-y: auto;
  min-height: 0;

  scroll-behavior: smooth;

  scrollbar-width: thin;
  scrollbar-color: rgba(255, 255, 255, 0.3) transparent;

  &::-webkit-scrollbar {
    width: 8px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(255, 255, 255, 0.3);
    border-radius: 4px;
  }

  &::-webkit-scrollbar-thumb:hover {
    background: rgba(255, 255, 255, 0.5);
  }
}

.message {
  display: flex;

  &.user {
    justify-content: flex-end;

    .message-content {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: #ffffff;
      max-width: 80%;
    }
  }

  &.assistant {
    justify-content: flex-start;

    .message-content {
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid rgba(255, 255, 255, 0.1);
      color: #ffffff;
      max-width: 85%;
    }
  }
}

.message-content {
  padding: 16px 20px;
  border-radius: 16px;
  backdrop-filter: blur(20px);
}

.user-message {
  line-height: 1.5;
}

.assistant-message {
  line-height: 1.6;

  /* Markdown渲染样式 */
  :deep(h1), :deep(h2), :deep(h3), :deep(h4), :deep(h5), :deep(h6) {
    margin: 0.5em 0;
    color: #ffffff;
  }

  :deep(p) {
    margin: 0.5em 0;
    line-height: 1.6;
  }

  :deep(ul), :deep(ol) {
    margin: 0.5em 0;
    padding-left: 1.5em;
  }

  :deep(li) {
    margin: 0.2em 0;
  }

  :deep(code) {
    background: rgba(255, 255, 255, 0.1);
    padding: 2px 6px;
    border-radius: 4px;
    font-family: 'Courier New', monospace;
    font-size: 0.9em;
  }

  :deep(pre) {
    background: rgba(0, 0, 0, 0.3);
    padding: 12px;
    border-radius: 8px;
    overflow-x: auto;
    margin: 0.5em 0;

    code {
      background: none;
      padding: 0;
    }
  }

  :deep(blockquote) {
    border-left: 3px solid #667eea;
    padding-left: 12px;
    margin: 0.5em 0;
    font-style: italic;
    color: rgba(255, 255, 255, 0.8);
  }

  :deep(table) {
    border-collapse: collapse;
    margin: 0.5em 0;
    width: 100%;
  }

  :deep(th), :deep(td) {
    border: 1px solid rgba(255, 255, 255, 0.2);
    padding: 8px 12px;
    text-align: left;
  }

  :deep(th) {
    background: rgba(255, 255, 255, 0.1);
    font-weight: 600;
  }
}

.system-message {
  text-align: center;
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
  padding: 8px;
}

/* AI回答框样式 - 与ChatContainer的response-section保持完全一致 */
.response-section {
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 18px;
  background: linear-gradient(
      135deg,
      rgba(255, 255, 255, 0.12) 0%,
      rgba(255, 255, 255, 0.06) 100%
  );
  overflow: hidden;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
  backdrop-filter: blur(12px);
  margin-top: 16px;
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 8px 25px rgba(0, 0, 0, 0.2);
  }

  .response-header {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 18px 24px 14px 24px;
    background: linear-gradient(
        135deg,
        rgba(102, 126, 234, 0.18) 0%,
        rgba(118, 75, 162, 0.12) 100%
    );
    border-bottom: 1px solid rgba(255, 255, 255, 0.15);

    .response-avatar {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 36px;
      height: 36px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      border-radius: 50%;
      box-shadow: 0 3px 12px rgba(102, 126, 234, 0.4);
      transition: transform 0.2s ease;

      &:hover {
        transform: scale(1.05);
      }

      .bot-icon {
        font-size: 20px;
        color: #ffffff;
      }
    }

    .response-name {
      font-weight: 700;
      font-size: 17px;
      color: #667eea;
      letter-spacing: 0.8px;
      text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
      font-family:
          -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei',
          sans-serif;
    }
  }

  .response-content {
    padding: 24px;

    .final-response {
      .response-text {
        word-break: break-all;
        line-height: 1.8;
        color: #ffffff;
        font-size: 15px;
        font-weight: 400;
        text-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
        letter-spacing: 0.4px;
        word-spacing: 1.2px;
        text-align: left;
        font-family:
            -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei',
            sans-serif;

        p {
          margin: 0 0 12px 0;

          &:last-child {
            margin-bottom: 0;
          }
        }

        strong {
          color: #f8fafc;
          font-weight: 600;
        }

        em {
          color: #e2e8f0;
          font-style: italic;
        }

        /* Headings */
        h1, h2, h3, h4, h5, h6 {
          margin: 12px 0 8px;
          font-weight: 700;
          line-height: 1.4;
        }
        h1 { font-size: 22px; border-bottom: 1px solid rgba(255,255,255,0.1); padding-bottom: 6px; }
        h2 { font-size: 20px; margin-top: 16px; }
        h3 { font-size: 18px; }

        /* Lists */
        ul, ol {
          margin: 6px 0 12px 22px;
          padding-left: 18px;
        }
        li { margin: 4px 0; }

        /* Blockquote */
        blockquote {
          margin: 10px 0;
          padding: 8px 12px;
          border-left: 3px solid #667eea;
          background: rgba(102, 126, 234, 0.08);
          color: #e5e7eb;
        }

        /* Inline code */
        code {
          background: rgba(0,0,0,0.35);
          padding: 2px 6px;
          border-radius: 4px;
          font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono',
            'Courier New', monospace;
          font-size: 13px;
        }

        /* Code blocks */
        pre {
          background: rgba(0,0,0,0.5);
          border: 1px solid rgba(255, 255, 255, 0.08);
          border-radius: 8px;
          padding: 12px 14px;
          overflow: auto;
          margin: 10px 0 14px;
        }
        pre code {
          background: transparent;
          padding: 0;
          font-size: 13px;
          line-height: 1.6;
          color: #e5e7eb;
          white-space: pre;
        }

        /* Tables */
        table {
          border-collapse: collapse;
          margin: 12px 0;
          width: 100%;
          border: 1px solid rgba(255, 255, 255, 0.1);
        }
        th, td {
          border: 1px solid rgba(255, 255, 255, 0.1);
          padding: 8px 12px;
          text-align: left;
        }
        th {
          background: rgba(255, 255, 255, 0.05);
          font-weight: 600;
        }

        /* Links */
        a {
          color: #667eea;
          text-decoration: underline;
          transition: color 0.2s ease;

          &:hover {
            color: #764ba2;
          }
        }
      }
    }
  }
}



.empty-chat {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #0a0a0a;
  min-height: 0;
}

.empty-content {
  text-align: center;
  color: rgba(255, 255, 255, 0.6);
  padding: 48px;
}

.empty-title {
  font-size: 20px;
  font-weight: 600;
  color: #4b5563;
  margin: 0 0 8px 0;
}

.empty-hint {
  font-size: 14px;
  color: #6b7280;
  margin: 0 0 24px 0;
}

.create-new-btn {
  padding: 12px 24px;
  background: rgba(255, 255, 255, 0.05);
  color: #ffffff;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;

  &:hover {
    background: rgba(255, 255, 255, 0.1);
    border-color: rgba(255, 255, 255, 0.2);
  }
}

/* AI思考中的动画效果 */
.thinking-dots {
  &::after {
    content: '';
    animation: thinking 1.5s infinite;
  }
}

@keyframes thinking {
  0%, 20% { content: ''; }
  25%, 45% { content: '.'; }
  50%, 70% { content: '..'; }
  75%, 95% { content: '...'; }
  100% { content: ''; }
}

/* 发送中状态的消息样式 */
.message.sending {
  opacity: 0.7;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 0.7; }
  50% { opacity: 1; }
}

</style>