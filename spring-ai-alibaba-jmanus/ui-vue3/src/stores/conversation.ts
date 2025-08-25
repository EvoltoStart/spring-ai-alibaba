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

import { defineStore } from 'pinia'
import { ref, computed, reactive } from 'vue'
import { conversationApiService, type Conversation, type ConversationMessage } from '@/api/conversation-api-service'

/**
 * 会话状态管理
 */
export const useConversationStore = defineStore('conversation', () => {
  // 状态
  const conversations = ref<Conversation[]>([])
  const currentConversation = ref<Conversation | null>(null)
  const currentMessages = ref<ConversationMessage[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)
  const initialized = ref(false)  // 标记是否已经初始化过数据
  
  // 简化版本：硬编码用户ID
  const userId = ref('default-user')

  // 计算属性
  const currentConversationId = computed(() => currentConversation.value?.conversationId || null)
  const hasConversations = computed(() => conversations.value.length > 0)

  // 辅助方法
  const setLoading = (value: boolean) => {
    loading.value = value
  }

  const setError = (message: string | null) => {
    error.value = message
  }

  const clearError = () => {
    error.value = null
  }

  // 创建新会话
  const createConversation = async (title?: string): Promise<Conversation | null> => {
    try {
      setLoading(true)
      clearError()

      const response = await conversationApiService.createConversation(userId.value, title)
      
      if (response.success && response.conversation) {
        const newConversation = reactive({
          ...response.conversation,
          editing: false,
          editTitle: response.conversation.title
        })
        conversations.value.unshift(newConversation)
        
        // 自动切换到新会话
        await selectConversation(newConversation.conversationId)
        
        return newConversation
      } else {
        setError(response.error || 'Failed to create conversation')
        return null
      }
    } catch (error) {
      setError('Failed to create conversation')
      console.error('Failed to create conversation:', error)
      return null
    } finally {
      setLoading(false)
    }
  }

  // 加载用户会话列表
  const loadConversations = async (): Promise<void> => {
    try {
      setLoading(true)
      clearError()

      const response = await conversationApiService.getUserConversations(userId.value)
      
      if (response.success && response.conversations) {
        // 为每个conversation添加编辑相关的响应式属性
        conversations.value = response.conversations.map(conv => reactive({
          ...conv,
          editing: false,
          editTitle: conv.title
        }))
      } else {
        setError('Failed to load conversations')
      }
    } catch (error) {
      setError('Failed to load conversations')
      console.error('Failed to load conversations:', error)
    } finally {
      setLoading(false)
      initialized.value = true  // 标记已经初始化过，无论成功还是失败
    }
  }

  // 选择会话
  const selectConversation = async (conversationId: string): Promise<void> => {
    try {
      setLoading(true)
      clearError()

      const response = await conversationApiService.getConversation(conversationId)
      
      if (response.success && response.conversation) {
        currentConversation.value = response.conversation
        currentMessages.value = response.messages || []
        
        // 更新本地会话列表中的信息
        const index = conversations.value.findIndex(c => c.conversationId === conversationId)
        if (index >= 0) {
          conversations.value[index] = response.conversation
        }
      } else {
        setError('Failed to load conversation')
      }
    } catch (error) {
      setError('Failed to load conversation')
      console.error('Failed to load conversation:', error)
    } finally {
      setLoading(false)
    }
  }

  // 更新会话标题
  const updateConversationTitle = async (conversationId: string, title: string): Promise<boolean> => {
    try {
      clearError()

      const response = await conversationApiService.updateConversationTitle(conversationId, title)
      
      if (response.success && response.conversation) {
        // 更新本地会话列表
        const index = conversations.value.findIndex(c => c.conversationId === conversationId)
        if (index >= 0) {
          conversations.value[index] = reactive({
            ...response.conversation,
            editing: false,
            editTitle: response.conversation.title
          })
        }
        
        // 如果是当前会话，也更新当前会话
        if (currentConversation.value?.conversationId === conversationId) {
          currentConversation.value = response.conversation
        }
        
        return true
      } else {
        setError(response.error || 'Failed to update title')
        return false
      }
    } catch (error) {
      setError('Failed to update title')
      console.error('Failed to update title:', error)
      return false
    }
  }

  // 删除会话
  const deleteConversation = async (conversationId: string): Promise<boolean> => {
    try {
      clearError()

      const response = await conversationApiService.deleteConversation(conversationId)
      
      if (response.success) {
        // 从本地列表中移除
        const index = conversations.value.findIndex(c => c.conversationId === conversationId)
        if (index >= 0) {
          conversations.value.splice(index, 1)
        }
        
        // 如果删除的是当前会话，清空当前会话
        if (currentConversation.value?.conversationId === conversationId) {
          currentConversation.value = null
          currentMessages.value = []
        }
        
        return true
      } else {
        setError(response.error || '删除会话失败')
        return false
      }
    } catch (error) {
      setError('删除会话失败')
      console.error('删除会话失败:', error)
      return false
    }
  }

  // 添加消息
  const addMessage = async (
    conversationId: string,
    messageType: 'USER' | 'ASSISTANT' | 'SYSTEM',
    content: string
  ): Promise<ConversationMessage | null> => {
    try {
      clearError()

      const response = await conversationApiService.addMessage(conversationId, messageType, content)
      
      if (response.success && response.message) {
        // 简化版本：如果message是对象，则处理
        if (typeof response.message === 'object' && response.message) {
          const newMessage = response.message as ConversationMessage
          // 如果是当前会话，添加到消息列表
          if (currentConversation.value?.conversationId === conversationId) {
            currentMessages.value.push(newMessage)
          }
          
          // 更新会话的消息计数和最后消息时间
          const conversation = conversations.value.find(c => c.conversationId === conversationId)
          if (conversation) {
            conversation.messageCount += 1
            if (newMessage.createTime) {
              conversation.lastMessageTime = newMessage.createTime
              conversation.updateTime = newMessage.createTime
            }
          }
          
          return newMessage
        }
        
        return null
      } else {
        setError(response.error || '添加消息失败')
        return null
      }
    } catch (error) {
      setError('添加消息失败')
      console.error('添加消息失败:', error)
      return null
    }
  }

  // 搜索会话
  const searchConversations = async (keyword: string): Promise<Conversation[]> => {
    try {
      clearError()

      const response = await conversationApiService.searchConversations(userId.value, keyword)
      
      if (response.success && response.conversations) {
        return response.conversations
      } else {
        setError(response.error || '搜索失败')
        return []
      }
    } catch (error) {
      setError('搜索失败')
      console.error('搜索失败:', error)
      return []
    }
  }

  return {
    // 状态
    conversations,
    currentConversation,
    currentMessages,
    loading,
    error,
    initialized,
    userId,
    
    // 计算属性
    currentConversationId,
    hasConversations,
    
    // 方法
    createConversation,
    loadConversations,
    selectConversation,
    updateConversationTitle,
    deleteConversation,
    addMessage,
    searchConversations,
    setError,
    clearError
  }
})