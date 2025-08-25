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

/**
 * 会话类型定义
 */
export interface Conversation {
  id: number
  conversationId: string
  userId: string
  title: string
  description?: string
  status: 'ACTIVE' | 'ARCHIVED' | 'DELETED'
  messageCount: number
  lastMessageTime?: string
  createTime: string
  updateTime: string
  metadata?: string
  // 前端编辑状态相关属性
  editing?: boolean
  editTitle?: string
}

/**
 * 消息类型定义
 */
export interface ConversationMessage {
  id: number
  conversationId: string
  messageType: 'USER' | 'ASSISTANT' | 'SYSTEM'
  content: string
  planId?: string
  status: 'SENDING' | 'SUCCESS' | 'FAILED'
  errorMessage?: string
  metadata?: string
  createTime: string
  updateTime: string
}

/**
 * API响应类型
 */

export interface ApiResponse {
  success: boolean
  conversation?: Conversation
  conversations?: Conversation[]
  messages?: ConversationMessage[]
  message?: ConversationMessage | string
  error?: string
}

/**
 * 分页响应类型
 */
export interface PageResponse<T> {
  success: boolean
  conversations?: T[]
  messages?: T[]
  totalPages: number
  totalElements: number
  currentPage: number
}

/**
 * 会话API服务
 */
export class ConversationApiService {
  
  /**
   * 创建新会话
   */
  async createConversation(userId: string, title?: string): Promise<ApiResponse> {
    try {
      // 获取当前语言设置
      const currentLocale = localStorage.getItem('LOCAL_STORAGE_LOCALE') || 'en'
      
      const response = await fetch(`/api/conversation/create?lang=${currentLocale}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          userId,
          title: title
        })
      })
      
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      
      const data = await response.json()
      return data
    } catch (error) {
      console.error('Failed to create conversation:', error)
      throw error
    }
  }

  /**
   * 获取用户的会话列表
   */
  async getUserConversations(userId: string, page: number = 0, size: number = 20): Promise<PageResponse<Conversation>> {
    try {
      const url = new URL('/api/conversation/list', window.location.origin)
      url.searchParams.set('userId', userId)
      url.searchParams.set('page', page.toString())
      url.searchParams.set('size', size.toString())
      
      const response = await fetch(url.toString())
      
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      
      const data = await response.json()
      return data
    } catch (error) {
      console.error('Failed to get conversation list:', error)
      
      throw error
    }
  }

  /**
   * 获取会话详情
   */
  async getConversation(conversationId: string): Promise<ApiResponse> {
    try {
      const response = await fetch(`/api/conversation/${conversationId}`)
      
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      
      const data = await response.json()
      return data
    } catch (error) {
      console.error('Failed to get conversation details:', error)
      throw error
    }
  }

  /**
   * 更新会话标题
   */
  async updateConversationTitle(conversationId: string, title: string): Promise<ApiResponse> {
    try {
      const response = await fetch(`/api/conversation/${conversationId}/title`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ title })
      })
      
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      
      const data = await response.json()
      return data
    } catch (error) {
      console.error('Failed to update conversation title:', error)
      throw error
    }
  }

  /**
   * 删除会话
   */
  async deleteConversation(conversationId: string): Promise<ApiResponse> {
    try {
      const response = await fetch(`/api/conversation/${conversationId}`, {
        method: 'DELETE'
      })
      
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      
      const data = await response.json()
      return data
    } catch (error) {
      console.error('Failed to delete conversation:', error)
      throw error
    }
  }

  /**
   * 添加消息到会话
   */
  async addMessage(conversationId: string, messageType: 'USER' | 'ASSISTANT' | 'SYSTEM', content: string): Promise<ApiResponse> {
    try {
      const response = await fetch(`/api/conversation/${conversationId}/messages`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          messageType,
          content
        })
      })
      
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      
      const data = await response.json()
      return data
    } catch (error) {
      console.error('添加消息失败:', error)
      throw error
    }
  }

  /**
   * 获取会话消息
   */
  async getConversationMessages(conversationId: string, page: number = 0, size: number = 50): Promise<PageResponse<ConversationMessage>> {
    try {
      const url = new URL(`/api/conversation/${conversationId}/messages`, window.location.origin)
      url.searchParams.set('page', page.toString())
      url.searchParams.set('size', size.toString())
      
      const response = await fetch(url.toString())
      
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      
      const data = await response.json()
      return data
    } catch (error) {
      console.error('获取会话消息失败:', error)
      throw error
    }
  }

  /**
   * 搜索会话
   */
  async searchConversations(userId: string, keyword: string): Promise<ApiResponse> {
    try {
      const url = new URL('/api/conversation/search', window.location.origin)
      url.searchParams.set('userId', userId)
      url.searchParams.set('keyword', keyword)
      
      const response = await fetch(url.toString())
      
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      
      const data = await response.json()
      return data
    } catch (error) {
      console.error('搜索会话失败:', error)
      throw error
    }
  }

  /**
   * 更新消息状态
   */
  async updateMessageStatus(messageId: number, status: 'SENDING' | 'SUCCESS' | 'FAILED'): Promise<ApiResponse> {
    try {
      const response = await fetch(`/api/conversation/messages/${messageId}/status`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ status })
      })
      
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      
      const data = await response.json()
      return data
    } catch (error) {
      console.error('更新消息状态失败:', error)
      throw error
    }
  }

  /**
   * 关联消息到计划
   */
  async linkMessageToPlan(messageId: number, planId: string): Promise<ApiResponse> {
    try {
      const response = await fetch(`/api/conversation/messages/${messageId}/link-plan`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ planId })
      })
      
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      
      const data = await response.json()
      return data
    } catch (error) {
      console.error('关联消息到计划失败:', error)
      throw error
    }
  }
}

// 导出单例实例
export const conversationApiService = new ConversationApiService()