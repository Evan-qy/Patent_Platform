<template>
  <div
    ref="shellRef"
    class="ai-chat-shell"
    :class="{ expanded: isExpanded, 'compact-shell': isCompact, 'narrow-shell': isNarrow }"
  >
    <div v-if="isCompact && sidebarVisible" class="sidebar-mask" @click="toggleSidebar(false)"></div>

    <aside
      class="chat-sidebar"
      :class="{ collapsed: !sidebarVisible && isCompact, compact: isCompact }"
      :style="sidebarStyle"
    >
      <div class="sidebar-header">
        <div>
          <h3>AI 咨询</h3>
          <p>历史会话</p>
        </div>
        <el-button type="primary" size="small" class="btn-primary-small" @click="startFreshChat">
          <el-icon><Plus /></el-icon>
          新建
        </el-button>
      </div>

      <div class="chat-list-container">
        <div v-if="loadingSessions && !chatHistory.length" class="sidebar-state">正在加载会话...</div>
        <div v-else-if="!chatHistory.length" class="sidebar-state empty">
          暂无会话，发送第一条消息后会自动创建会话。
        </div>

        <div v-else class="chat-list">
          <div
            v-for="chat in sortedChatHistory"
            :key="chat.id"
            class="chat-item"
            :class="{ active: currentChat?.id === chat.id }"
            @click="selectChat(chat)"
          >
            <div class="chat-item-main">
              <div class="chat-title">{{ chat.title || '未命名对话' }}</div>
              <div class="chat-preview">{{ getChatPreview(chat) }}</div>
            </div>
            <div class="chat-meta">
              <span>{{ formatTime(chat.updatedAt) }}</span>
              <el-dropdown trigger="click" @command="(command: string | number | object) => handleChatAction(String(command), chat)">
                <el-button link @click.stop>
                  <el-icon><MoreFilled /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="rename">重命名</el-dropdown-item>
                    <el-dropdown-item command="delete">删除</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
        </div>
      </div>
    </aside>

    <div
      v-if="!isCompact"
      class="sidebar-resize-handle"
      @mousedown="startSidebarResize"
    ></div>

    <section class="chat-main" :class="{ 'sidebar-obscured': isCompact && sidebarVisible }">
      <div v-if="!currentUserId" class="login-prompt">
        <div class="prompt-content">
          <el-icon class="prompt-icon"><ChatDotRound /></el-icon>
          <h3>请先登录</h3>
          <p>登录后可保存咨询记录，并继续查看历史会话。</p>
          <el-button type="primary" class="btn-primary" @click="goToLogin">前往登录</el-button>
        </div>
      </div>

      <template v-else>
        <div class="chat-header">
          <el-button class="sidebar-toggle-btn" circle @click="toggleSidebar()">
            <el-icon><Menu /></el-icon>
          </el-button>
          <div class="chat-info">
            <h3>{{ currentChat?.title || '新对话' }}</h3>
            <p>
              {{
                currentChat
                  ? '支持 Markdown 展示，AI 回复会实时输出。'
                  : '发送第一条消息后将自动创建会话。'
              }}
            </p>
          </div>
          <div class="chat-header-actions">
            <el-button circle class="layout-btn" @click="resetLayout" title="重置布局">
              <el-icon><RefreshRight /></el-icon>
            </el-button>
            <el-button circle class="layout-btn" @click="toggleExpand" :title="isExpanded ? '退出展开' : '展开显示'">
              <el-icon>
                <component :is="isExpanded ? ScaleToOriginal : FullScreen" />
              </el-icon>
            </el-button>
          </div>
        </div>

        <div ref="messageListRef" class="message-list">
          <div v-if="messageLoading" class="message-state">正在加载消息...</div>
          <template v-else>
            <div v-if="!(currentChat?.messages.length)" class="message-empty">
              <el-icon><ChatDotRound /></el-icon>
              <h4>开始新的 AI 咨询</h4>
              <p>支持问答、专利运营建议、需求分析和成果转化咨询。</p>
            </div>

            <div
              v-for="message in currentChat?.messages || []"
              :key="message.id"
              class="message"
              :class="message.role"
            >
              <div class="message-avatar">
                <el-avatar :size="34">{{ message.role === 'user' ? '我' : 'AI' }}</el-avatar>
              </div>
              <div class="message-content">
                <div v-if="message.role === 'user'" class="message-text">{{ message.content }}</div>
                <div v-else class="message-text markdown-body" v-html="renderMarkdown(message.content)"></div>
                <div class="message-time">{{ formatTime(message.timestamp) }}</div>
              </div>
            </div>
          </template>
        </div>

        <div class="input-area">
          <div class="input-container">
            <el-input
              v-model="inputMessage"
              type="textarea"
              :rows="3"
              class="message-input"
              :disabled="sending"
              placeholder="请输入您的问题，Enter 发送，Shift + Enter 换行"
              @keydown="handleInputKeydown"
            />
            <div class="input-actions">
              <el-button
                type="primary"
                class="send-button btn-primary"
                :loading="sending"
                :disabled="!inputMessage.trim()"
                @click="sendMessage"
              >
                <el-icon><Promotion /></el-icon>
                <span>发送</span>
              </el-button>
              <el-button class="clear-button" @click="clearInput">清空</el-button>
            </div>
          </div>
        </div>
      </template>
    </section>

    <el-dialog v-model="renameDialogVisible" title="重命名对话" width="400px" class="glass-dialog">
      <el-input v-model="newChatTitle" class="glass-input" placeholder="请输入对话名称" />
      <template #footer>
        <el-button @click="renameDialogVisible = false">取消</el-button>
        <el-button type="primary" class="btn-primary" @click="saveChatTitle">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChatDotRound, FullScreen, Menu, MoreFilled, Plus, Promotion, RefreshRight, ScaleToOriginal } from '@element-plus/icons-vue'
import { aiApi, authApi } from '@/api'
import { resolveWsBaseUrl } from '@/lib/runtime'
import router from '@/router'
import { renderMarkdown } from '@/lib/markdown'
import type { ChatMessageResponse, ChatSessionResponse, UIMessage, UISession } from '@/types'

interface Chat extends UISession {}

interface WsPayloadMap {
  'session.created': { sessionId: number; title?: string }
  'answer.delta': { delta?: string }
  'answer.done': { sessionId: number; answer?: string; model?: string; requestId?: string }
  'answer.error': { message?: string }
}

type WsEnvelope = {
  type: keyof WsPayloadMap | string
  payload?: Record<string, unknown>
}

const viewportWidth = ref(window.innerWidth)
const isMobile = computed(() => viewportWidth.value <= 768)
const shellRef = ref<HTMLElement | null>(null)
const shellWidth = ref(0)
const isCompact = computed(() => shellWidth.value > 0 && shellWidth.value <= 860)
const isNarrow = computed(() => shellWidth.value > 0 && shellWidth.value <= 620)
const sidebarVisible = ref(true)
const currentUserId = ref<number | null>(null)
const isExpanded = ref(false)
const sidebarWidth = ref(280)
const isSidebarResizing = ref(false)

const loadingSessions = ref(false)
const sending = ref(false)
const messageLoading = ref(false)

const chatHistory = ref<Chat[]>([])
const currentChat = ref<Chat | null>(null)

const inputMessage = ref('')
const messageListRef = ref<HTMLElement>()
const renameDialogVisible = ref(false)
const newChatTitle = ref('')

const pendingChatId = ref<string | null>(null)
const pendingAssistantMessageId = ref<string | null>(null)
const pendingQuestion = ref('')

const sortedChatHistory = computed(() => [...chatHistory.value].sort((a, b) => b.updatedAt - a.updatedAt))
const overlaySidebarWidth = computed(() => {
  if (!shellWidth.value) return 320
  return Math.max(260, Math.min(360, shellWidth.value - 56))
})
const sidebarStyle = computed(() => {
  if (isCompact.value) {
    return {
      width: `${overlaySidebarWidth.value}px`,
      flex: '0 0 auto'
    }
  }
  return {
    width: `${sidebarWidth.value}px`,
    flex: `0 0 ${sidebarWidth.value}px`
  }
})

let socket: WebSocket | null = null
let socketReadyPromise: Promise<void> | null = null
let shellResizeObserver: ResizeObserver | null = null
let wasCompact = false
let socketEverOpened = false
let suppressSocketCloseError = false
let activeRequestTransport: 'ws' | 'rest' | null = null
let realtimeUnavailableUntil = 0
let realtimeFallbackNotified = false

const REALTIME_RETRY_COOLDOWN_MS = 30_000

const handleWindowResize = () => {
  viewportWidth.value = window.innerWidth
  syncShellLayout()
  if (sidebarWidth.value > window.innerWidth - 360) {
    sidebarWidth.value = Math.max(220, window.innerWidth - 360)
  }
}

const syncShellLayout = () => {
  shellWidth.value = shellRef.value?.clientWidth || 0
  const compact = shellWidth.value > 0 && shellWidth.value <= 860
  if (compact !== wasCompact) {
    sidebarVisible.value = !compact
    wasCompact = compact
  } else if (!compact) {
    sidebarVisible.value = true
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

const toggleSidebar = (next?: boolean) => {
  sidebarVisible.value = typeof next === 'boolean' ? next : !sidebarVisible.value
}

const startSidebarResize = (event: MouseEvent) => {
  if (isCompact.value) return
  isSidebarResizing.value = true
  document.body.classList.add('ai-chat-resizing')
  document.addEventListener('mousemove', handleSidebarResize)
  document.addEventListener('mouseup', stopSidebarResize)
  event.preventDefault()
}

const handleSidebarResize = (event: MouseEvent) => {
  if (!isSidebarResizing.value) return
  const width = Math.min(420, Math.max(220, event.clientX))
  sidebarWidth.value = width
}

const stopSidebarResize = () => {
  isSidebarResizing.value = false
  document.body.classList.remove('ai-chat-resizing')
  document.removeEventListener('mousemove', handleSidebarResize)
  document.removeEventListener('mouseup', stopSidebarResize)
}

const resetLayout = () => {
  sidebarWidth.value = 280
}

const toggleExpand = () => {
  isExpanded.value = !isExpanded.value
  document.body.classList.toggle('ai-chat-expanded-lock', isExpanded.value)
  nextTick(() => {
    scrollToBottom()
  })
}

const goToLogin = () => {
  router.push('/login')
}

const formatTime = (timestamp: number) => {
  const date = new Date(timestamp)
  const diff = Date.now() - timestamp
  if (diff < 60_000) return '刚刚'
  if (diff < 3_600_000) return `${Math.floor(diff / 60_000)} 分钟前`
  if (diff < 86_400_000) return `${Math.floor(diff / 3_600_000)} 小时前`
  return date.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

const stripMarkdown = (value: string) =>
  value
    .replace(/```[\s\S]*?```/g, ' ')
    .replace(/`([^`]+)`/g, '$1')
    .replace(/!\[.*?\]\(.*?\)/g, ' ')
    .replace(/\[(.*?)\]\(.*?\)/g, '$1')
    .replace(/^#{1,6}\s+/gm, '')
    .replace(/[*_~>-]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()

const getChatPreview = (chat: Chat) => {
  const latest = [...chat.messages].reverse().find(item => item.role !== 'system')
  if (latest?.content) {
    return stripMarkdown(latest.content).slice(0, 48) || '点击继续当前对话'
  }
  return '点击继续当前对话'
}

const toUISession = (session: ChatSessionResponse): Chat => ({
  id: session.id.toString(),
  title: session.title || '未命名对话',
  messages: [],
  createdAt: new Date(session.createdAt).getTime(),
  updatedAt: new Date(session.updatedAt).getTime(),
  backendId: session.id
})

const toUIMessage = (message: ChatMessageResponse): UIMessage => ({
  id: message.id.toString(),
  role: message.role === 'assistant' ? 'assistant' : message.role === 'system' ? 'system' : 'user',
  content: message.content,
  timestamp: new Date(message.createdAt).getTime(),
  backendId: message.id
})

const mergeMessages = (localMessages: UIMessage[], fetchedMessages: UIMessage[]) => {
  if (!localMessages.length) return fetchedMessages
  if (!fetchedMessages.length) return localMessages

  const merged = [...fetchedMessages]
  const fetchedBackendIds = new Set(
    fetchedMessages
      .map(message => message.backendId)
      .filter((value): value is number => typeof value === 'number')
  )
  const fetchedFingerprints = new Set(
    fetchedMessages.map(message => `${message.role}:${message.content.trim()}`)
  )

  for (const message of localMessages) {
    if (typeof message.backendId === 'number' && fetchedBackendIds.has(message.backendId)) {
      continue
    }

    const fingerprint = `${message.role}:${message.content.trim()}`
    if (!message.backendId && fetchedFingerprints.has(fingerprint)) {
      continue
    }

    merged.push(message)
    if (typeof message.backendId === 'number') {
      fetchedBackendIds.add(message.backendId)
    }
    fetchedFingerprints.add(fingerprint)
  }

  return merged.sort((left, right) => left.timestamp - right.timestamp)
}

const syncCurrentChat = (chat: Chat) => {
  const index = chatHistory.value.findIndex(item => item.id === chat.id)
  if (index >= 0) {
    chatHistory.value[index] = { ...chat }
  } else {
    chatHistory.value = [chat, ...chatHistory.value]
  }

  if (currentChat.value?.id === chat.id || currentChat.value?.backendId === chat.backendId) {
    currentChat.value = { ...chat }
  }
}

const replaceChat = (matcher: (chat: Chat) => boolean, nextChat: Chat) => {
  let matched = false
  const nextHistory = chatHistory.value.map(item => {
    if (matcher(item)) {
      matched = true
      return nextChat
    }
    return item
  })
  chatHistory.value = matched ? nextHistory : [nextChat, ...nextHistory]

  if (currentChat.value && matcher(currentChat.value)) {
    currentChat.value = { ...nextChat }
  }
}

const ensureLogin = async () => {
  const token = localStorage.getItem('token')
  if (!token) {
    currentUserId.value = null
    return
  }

  try {
    const response = await authApi.getCurrentUser()
    const userInfo = response?.user || response
    currentUserId.value = userInfo?.id || null
  } catch (error: any) {
    currentUserId.value = null
    if (error?.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    }
  }
}

const loadMessages = async (chat: Chat) => {
  if (!chat.backendId) return
  messageLoading.value = true
  try {
    const response = await aiApi.getMessages(chat.backendId)
    const fetchedMessages = Array.isArray(response) ? response.map(toUIMessage) : []
    const nextChat = {
      ...chat,
      messages: mergeMessages(chat.messages, fetchedMessages),
      updatedAt: Date.now()
    }
    syncCurrentChat(nextChat)
  } catch (error: any) {
    ElMessage.error(error.message || '加载消息失败')
  } finally {
    messageLoading.value = false
    scrollToBottom()
  }
}

const hydrateSessionMessages = async (chat: Chat) => {
  if (!chat.backendId) return chat
  try {
    const response = await aiApi.getMessages(chat.backendId)
    const fetchedMessages = Array.isArray(response) ? response.map(toUIMessage) : []
    return {
      ...chat,
      messages: mergeMessages(chat.messages, fetchedMessages)
    }
  } catch {
    return chat
  }
}

const selectChat = async (chat: Chat) => {
  currentChat.value = { ...chat }
  if (isCompact.value) {
    sidebarVisible.value = false
  }
  if (chat.messages.length || !chat.backendId) {
    scrollToBottom()
    return
  }
  await loadMessages(chat)
}

const loadSessions = async () => {
  if (!currentUserId.value) return

  loadingSessions.value = true
  try {
    const response = await aiApi.getSessions()
    const baseList = Array.isArray(response) ? response.map(toUISession) : []
    const hydrated = await Promise.all(baseList.map(hydrateSessionMessages))
    chatHistory.value = hydrated
    currentChat.value = hydrated[0] ? { ...hydrated[0] } : null
    scrollToBottom()
  } catch (error: any) {
    ElMessage.error(error.message || '加载会话失败')
  } finally {
    loadingSessions.value = false
  }
}

const createTempMessage = (role: UIMessage['role'], content: string): UIMessage => ({
  id: `${role}_${Date.now()}_${Math.random().toString(16).slice(2)}`,
  role,
  content,
  timestamp: Date.now()
})

const createLocalSession = (seedText: string): Chat => ({
  id: `local_${Date.now()}_${Math.random().toString(16).slice(2)}`,
  title: seedText.trim().slice(0, 20) || '新对话',
  messages: [],
  createdAt: Date.now(),
  updatedAt: Date.now()
})

const startFreshChat = () => {
  if (!currentUserId.value) {
    ElMessage.warning('请先登录后再创建对话')
    return
  }
  currentChat.value = null
  inputMessage.value = ''
  messageLoading.value = false
  pendingChatId.value = null
  pendingAssistantMessageId.value = null
  if (isCompact.value) {
    sidebarVisible.value = false
  }
}

const handleChatAction = (command: string, chat: Chat) => {
  currentChat.value = { ...chat }
  if (command === 'rename') {
    newChatTitle.value = chat.title
    renameDialogVisible.value = true
  }
  if (command === 'delete') {
    void deleteChat(chat)
  }
}

const saveChatTitle = async () => {
  if (!currentChat.value?.backendId) {
    ElMessage.warning('请先发送消息创建会话')
    return
  }

  const nextTitle = newChatTitle.value.trim()
  if (!nextTitle) {
    ElMessage.warning('请输入对话名称')
    return
  }

  try {
    const response = await aiApi.updateSessionTitle(currentChat.value.backendId, nextTitle)
    const updated = toUISession(response)
    updated.messages = currentChat.value.messages
    syncCurrentChat(updated)
    renameDialogVisible.value = false
    ElMessage.success('对话标题已更新')
  } catch (error: any) {
    ElMessage.error(error.message || '更新标题失败')
  }
}

const deleteChat = async (chat: Chat) => {
  try {
    await ElMessageBox.confirm(`确定删除对话“${chat.title || '未命名对话'}”吗？`, '确认删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })

    if (chat.backendId) {
      await aiApi.deleteSession(chat.backendId)
    }

    chatHistory.value = chatHistory.value.filter(item => item.id !== chat.id)
    if (currentChat.value?.id === chat.id) {
      currentChat.value = chatHistory.value[0] ? { ...chatHistory.value[0] } : null
    }
    ElMessage.success('对话已删除')
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '删除失败')
    }
  }
}

const clearInput = () => {
  inputMessage.value = ''
}

const createWsUrl = (token: string) => {
  const base = resolveWsBaseUrl()
  return `${base}/ws/ai-chat?token=${encodeURIComponent(token)}`
}

const isRealtimeTemporarilyUnavailable = () => Date.now() < realtimeUnavailableUntil

const resetRealtimeFallbackState = () => {
  realtimeUnavailableUntil = 0
  realtimeFallbackNotified = false
}

const markRealtimeUnavailable = () => {
  if (!isRealtimeTemporarilyUnavailable()) {
    realtimeFallbackNotified = false
  }
  realtimeUnavailableUntil = Date.now() + REALTIME_RETRY_COOLDOWN_MS
}

const notifyRealtimeFallback = () => {
  if (realtimeFallbackNotified) return
  realtimeFallbackNotified = true
  ElMessage.warning('实时连接不可用，已切换为普通问答模式')
}

const closeSocket = (suppressError = true) => {
  suppressSocketCloseError = suppressError
  socket?.close()
  socket = null
  socketReadyPromise = null
  socketEverOpened = false
  activeRequestTransport = null
}

const findPendingChat = () => {
  if (!pendingChatId.value) return currentChat.value
  return chatHistory.value.find(item => item.id === pendingChatId.value) || currentChat.value
}

const updatePendingAssistant = (updater: (message: UIMessage) => void) => {
  const chat = findPendingChat()
  if (!chat || !pendingAssistantMessageId.value) return
  const messageIndex = chat.messages.findIndex(item => item.id === pendingAssistantMessageId.value)
  if (messageIndex < 0) return

  const nextMessages = [...chat.messages]
  const target = { ...nextMessages[messageIndex] }
  updater(target)
  nextMessages[messageIndex] = target

  const nextChat = {
    ...chat,
    messages: nextMessages,
    updatedAt: Date.now()
  }
  syncCurrentChat(nextChat)
  scrollToBottom()
}

const getUiErrorMessage = (error: any, fallback: string) =>
  error?.response?.data?.message
  || error?.message
  || fallback

const applyPendingAssistantMessage = (content: string) => {
  updatePendingAssistant(message => {
    message.content = content
    message.timestamp = Date.now()
  })
}

const refreshChatMessagesBySession = async (sessionId?: number) => {
  if (!sessionId) return
  const target = chatHistory.value.find(item => item.backendId === sessionId)
  if (target) {
    await loadMessages(target)
  }
}

const sendMessageViaRest = async (question: string, chat: Chat, notifyFallback = false) => {
  if (notifyFallback) {
    notifyRealtimeFallback()
  }
  activeRequestTransport = 'rest'

  try {
    const response = await aiApi.ask({
      question,
      sessionId: chat.backendId,
      model: 'qwen-plus',
      historyLimit: 20
    })

    const answer = response.answer?.trim()
    if (answer) {
      applyPendingAssistantMessage(answer)
    } else {
      const emptyMessage = 'AI 暂时没有返回内容，请稍后重试。'
      applyPendingAssistantMessage(emptyMessage)
      ElMessage.warning(emptyMessage)
    }

    const nextChat = {
      ...chat,
      id: response.sessionId ? String(response.sessionId) : chat.id,
      backendId: response.sessionId || chat.backendId,
      updatedAt: Date.now()
    }
    replaceChat(item => item.id === chat.id || item.backendId === response.sessionId, nextChat)
    currentChat.value = nextChat
    pendingChatId.value = nextChat.id
    await refreshChatMessagesBySession(response.sessionId)
  } catch (error: any) {
    const message = getUiErrorMessage(error, 'AI 问答失败，请稍后重试。')
    applyPendingAssistantMessage(message)
    ElMessage.error(message)
  } finally {
    activeRequestTransport = null
    sending.value = false
    pendingAssistantMessageId.value = null
    pendingQuestion.value = ''
  }
}

const handleSocketMessage = (event: MessageEvent<string>) => {
  let envelope: WsEnvelope
  try {
    envelope = JSON.parse(event.data) as WsEnvelope
  } catch {
    return
  }

  const payload = envelope.payload || {}

  if (envelope.type === 'session.created') {
    const pending = findPendingChat()
    if (!pending) return

    const sessionPayload = payload as WsPayloadMap['session.created']
    const nextChat: Chat = {
      ...pending,
      id: String(sessionPayload.sessionId),
      backendId: sessionPayload.sessionId,
      title: sessionPayload.title || pending.title
    }
    replaceChat(item => item.id === pending.id || item.backendId === sessionPayload.sessionId, nextChat)
    currentChat.value = nextChat
    pendingChatId.value = nextChat.id
    return
  }

  if (envelope.type === 'answer.delta') {
    const deltaPayload = payload as WsPayloadMap['answer.delta']
    updatePendingAssistant(message => {
      message.content += deltaPayload.delta || ''
      message.timestamp = Date.now()
    })
    return
  }

  if (envelope.type === 'answer.done') {
    const donePayload = payload as WsPayloadMap['answer.done']
    const chat = findPendingChat()
    applyPendingAssistantMessage((donePayload.answer || '').trim() || 'AI 暂时没有返回内容，请稍后重试。')
    if (chat) {
      const nextChat = {
        ...chat,
        id: donePayload.sessionId ? String(donePayload.sessionId) : chat.id,
        backendId: donePayload.sessionId || chat.backendId,
        updatedAt: Date.now()
      }
      replaceChat(item => item.id === chat.id || item.backendId === donePayload.sessionId, nextChat)
      currentChat.value = nextChat
      void refreshChatMessagesBySession(donePayload.sessionId)
    }

    activeRequestTransport = null
    sending.value = false
    pendingAssistantMessageId.value = null
    pendingQuestion.value = ''
    return
  }

  if (envelope.type === 'answer.error') {
    const errorPayload = payload as WsPayloadMap['answer.error']
    const errorMessage = errorPayload.message || 'AI 问答失败，请稍后重试。'
    applyPendingAssistantMessage(errorMessage)
    activeRequestTransport = null
    sending.value = false
    pendingAssistantMessageId.value = null
    pendingQuestion.value = ''
    ElMessage.error(errorMessage)
    return
  }
}

const ensureSocket = async () => {
  if (socket?.readyState === WebSocket.OPEN) return
  if (socketReadyPromise) return socketReadyPromise

  const token = localStorage.getItem('token')
  if (!token) {
    throw new Error('未登录，无法建立 AI 连接')
  }

  socketReadyPromise = new Promise<void>((resolve, reject) => {
    const ws = new WebSocket(createWsUrl(token))
    socket = ws
    socketEverOpened = false
    suppressSocketCloseError = false

    ws.onopen = () => {
      socketEverOpened = true
      socketReadyPromise = null
      resetRealtimeFallbackState()
      resolve()
    }

    ws.onmessage = handleSocketMessage

    ws.onerror = () => {
      markRealtimeUnavailable()
      socketReadyPromise = null
      reject(new Error('AI 连接失败'))
    }

    ws.onclose = () => {
      const shouldNotifyDisconnect =
        sending.value &&
        activeRequestTransport === 'ws' &&
        socketEverOpened &&
        !suppressSocketCloseError
      socket = null
      socketReadyPromise = null
      socketEverOpened = false
      if (shouldNotifyDisconnect) {
        const errorMessage = 'AI 连接已断开，请重试'
        markRealtimeUnavailable()
        applyPendingAssistantMessage(errorMessage)
        activeRequestTransport = null
        pendingQuestion.value = ''
        sending.value = false
        pendingAssistantMessageId.value = null
        ElMessage.error(errorMessage)
      }
    }
  })

  return socketReadyPromise
}

const sendMessage = async () => {
  const question = inputMessage.value.trim()
  if (!question || sending.value || !currentUserId.value) return

  try {
    let chat = currentChat.value ? { ...currentChat.value } : createLocalSession(question)
    if (!chatHistory.value.some(item => item.id === chat.id)) {
      chatHistory.value = [chat, ...chatHistory.value]
    }

    const userMessage = createTempMessage('user', question)
    const assistantMessage = createTempMessage('assistant', '')
    chat = {
      ...chat,
      title: chat.title || question.slice(0, 20) || '新对话',
      messages: [...chat.messages, userMessage, assistantMessage],
      updatedAt: Date.now()
    }

    currentChat.value = chat
    syncCurrentChat(chat)
    pendingChatId.value = chat.id
    pendingAssistantMessageId.value = assistantMessage.id
    pendingQuestion.value = question
    inputMessage.value = ''
    sending.value = true
    activeRequestTransport = null
    scrollToBottom()

    if (isRealtimeTemporarilyUnavailable()) {
      await sendMessageViaRest(question, chat, true)
      return
    }

    try {
      await ensureSocket()
      if (!socket || socket.readyState !== WebSocket.OPEN) {
        throw new Error('AI websocket unavailable')
      }
      activeRequestTransport = 'ws'
      socket.send(
        JSON.stringify({
          type: 'chat.ask',
          sessionId: chat.backendId ?? null,
          question,
          model: 'qwen-plus',
          historyLimit: 20
        })
      )
    } catch {
      markRealtimeUnavailable()
      await sendMessageViaRest(question, chat, true)
    }
  } catch (error: any) {
    const message = getUiErrorMessage(error, 'AI 问答失败，请稍后重试。')
    applyPendingAssistantMessage(message)
    activeRequestTransport = null
    sending.value = false
    pendingAssistantMessageId.value = null
    pendingQuestion.value = ''
    ElMessage.error(message)
  }
}

const handleInputKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    void sendMessage()
  }
}

onMounted(async () => {
  window.addEventListener('resize', handleWindowResize)
  shellResizeObserver = new ResizeObserver(() => {
    syncShellLayout()
  })
  if (shellRef.value) {
    shellResizeObserver.observe(shellRef.value)
  }
  syncShellLayout()
  await ensureLogin()
  if (currentUserId.value) {
    await loadSessions()
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', handleWindowResize)
  shellResizeObserver?.disconnect()
  shellResizeObserver = null
  stopSidebarResize()
  document.body.classList.remove('ai-chat-expanded-lock')
  closeSocket()
})
</script>

<style scoped>
.ai-chat-shell {
  display: flex;
  width: 100%;
  height: 100%;
  min-height: 100%;
  min-width: 0;
  min-width: 0;
  background: linear-gradient(180deg, rgba(250, 252, 255, 0.95), rgba(242, 247, 255, 0.9));
  color: var(--text-primary);
  overflow: hidden;
  position: relative;
  container-type: inline-size;
}

.ai-chat-shell.expanded {
  position: fixed;
  inset: 16px;
  z-index: 12050;
  border-radius: 24px;
  border: 1px solid rgba(61, 125, 255, 0.18);
  box-shadow: 0 24px 60px rgba(16, 38, 63, 0.24);
}

.sidebar-mask {
  position: absolute;
  inset: 0;
  background: rgba(20, 34, 58, 0.28);
  z-index: 18;
}

.chat-sidebar {
  width: 280px;
  flex: 0 0 280px;
  background: rgba(255, 255, 255, 0.82);
  border-right: 1px solid rgba(61, 125, 255, 0.12);
  display: flex;
  flex-direction: column;
  backdrop-filter: blur(12px);
  transition: transform 0.25s ease;
  z-index: 19;
}

.sidebar-resize-handle {
  width: 8px;
  flex: 0 0 8px;
  cursor: col-resize;
  background: linear-gradient(180deg, rgba(61, 125, 255, 0.02), rgba(61, 125, 255, 0.12), rgba(61, 125, 255, 0.02));
}

.sidebar-resize-handle:hover {
  background: linear-gradient(180deg, rgba(61, 125, 255, 0.1), rgba(61, 125, 255, 0.3), rgba(61, 125, 255, 0.1));
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid rgba(61, 125, 255, 0.12);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.sidebar-header h3 {
  margin: 0;
  font-size: 16px;
  color: #335688;
}

.sidebar-header p {
  margin: 4px 0 0;
  font-size: 12px;
  color: #7a8fab;
}

.chat-list-container {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.sidebar-state {
  min-height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6f84a8;
  font-size: 13px;
  text-align: center;
}

.chat-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.chat-item {
  padding: 12px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.75);
  border: 1px solid rgba(61, 125, 255, 0.1);
  cursor: pointer;
  transition: all 0.2s ease;
}

.chat-item:hover {
  background: #fff;
  border-color: rgba(61, 125, 255, 0.22);
}

.chat-item.active {
  background: rgba(61, 125, 255, 0.1);
  border-color: rgba(61, 125, 255, 0.38);
  box-shadow: 0 8px 20px rgba(61, 125, 255, 0.08);
}

.chat-item-main {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.chat-title {
  font-weight: 600;
  color: #26447a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-preview {
  color: #6f84a8;
  font-size: 12px;
  line-height: 1.4;
  min-height: 32px;
}

.chat-meta {
  margin-top: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #8a9bb5;
  font-size: 12px;
}

.chat-main {
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: opacity 0.2s ease, filter 0.2s ease;
}

.chat-main.sidebar-obscured {
  pointer-events: none;
  opacity: 0.22;
  filter: blur(1px);
}

.chat-header {
  padding: 16px 20px;
  border-bottom: 1px solid rgba(61, 125, 255, 0.12);
  display: flex;
  align-items: center;
  gap: 12px;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(10px);
}

.chat-header-actions {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 8px;
}

.sidebar-toggle-btn {
  border-color: rgba(61, 125, 255, 0.18);
  color: #3b61a2;
  background: rgba(255, 255, 255, 0.82);
}

.layout-btn {
  border-color: rgba(61, 125, 255, 0.18);
  color: #3b61a2;
  background: rgba(255, 255, 255, 0.82);
}

.chat-info h3 {
  margin: 0;
  color: #26447a;
  font-size: 16px;
}

.chat-info p {
  margin: 4px 0 0;
  color: #7a8fab;
  font-size: 12px;
}

.message-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 22px 20px 28px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.message-state,
.message-empty {
  min-height: 180px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #6f84a8;
  text-align: center;
}

.message-empty :deep(.el-icon) {
  font-size: 28px;
  color: #4776c3;
}

.message {
  display: flex;
  gap: 12px;
  width: 100%;
  min-width: 0;
  align-items: flex-start;
}

.message.user {
  flex-direction: row-reverse;
}

.message-content {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
  width: fit-content;
  max-width: min(82%, 820px);
}

.message.user .message-content {
  text-align: right;
}

.message-text {
  max-width: 100%;
  padding: 12px 16px;
  border-radius: 18px;
  line-height: 1.6;
  font-size: 14px;
  white-space: pre-wrap;
  word-break: break-word;
  overflow-wrap: anywhere;
  overflow: hidden;
}

.message.user .message-text {
  background: linear-gradient(135deg, #2f7bf6, #36cfc9);
  color: white;
  border-bottom-right-radius: 6px;
}

.message.assistant .message-text,
.message.system .message-text {
  background: rgba(255, 255, 255, 0.9);
  color: var(--text-primary);
  border: 1px solid rgba(61, 125, 255, 0.12);
  border-bottom-left-radius: 6px;
  max-width: 100%;
}

.message-time {
  margin-top: 6px;
  font-size: 12px;
  color: #8a9bb5;
}

.input-area {
  padding: 16px 20px calc(18px + env(safe-area-inset-bottom));
  border-top: 1px solid rgba(61, 125, 255, 0.12);
  background: linear-gradient(180deg, rgba(232, 240, 251, 0.98), rgba(223, 234, 249, 0.96));
  backdrop-filter: blur(10px);
}

.input-container {
  display: flex;
  gap: 10px;
  align-items: flex-end;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.99), rgba(245, 249, 255, 0.98));
  border-radius: 18px;
  padding: 10px 12px 10px 14px;
  border: 1px solid rgba(39, 90, 173, 0.28);
  box-shadow: 0 12px 28px rgba(44, 76, 128, 0.08);
  transition: border-color 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
}

.message-input {
  flex: 1;
}

:deep(.message-input .el-textarea) {
  width: 100%;
}

:deep(.message-input .el-textarea__inner) {
  background: linear-gradient(180deg, rgba(240, 246, 255, 0.99), rgba(224, 235, 249, 0.98));
  border: 1px solid rgba(69, 115, 181, 0.34);
  box-shadow: inset 0 1px 2px rgba(255, 255, 255, 0.84), inset 0 0 0 1px rgba(255, 255, 255, 0.46);
  resize: none;
  color: #0b1e38;
  font-weight: 700;
  caret-color: #184fc7;
  padding: 10px 12px;
  border-radius: 12px;
  line-height: 1.7;
}

:deep(.message-input .el-textarea__inner::placeholder) {
  color: #4f6482;
  opacity: 1;
}

:deep(.message-input .el-textarea__inner:focus) {
  background: #ffffff;
  border-color: rgba(31, 83, 189, 0.48);
}

.input-container:focus-within {
  border-color: rgba(31, 95, 209, 0.44);
  box-shadow: 0 0 0 4px rgba(31, 95, 209, 0.12), 0 16px 32px rgba(44, 76, 128, 0.12);
}

.input-actions {
  display: flex;
  gap: 8px;
}

.send-button,
.clear-button,
.btn-primary-small {
  border-radius: 999px;
}

.send-button {
  min-width: 78px;
}

.clear-button {
  color: #16365b;
  background: rgba(245, 249, 255, 0.92);
  border: 1px solid rgba(78, 121, 186, 0.28);
  font-weight: 600;
}

.clear-button:disabled,
.input-actions :deep(.el-button.is-disabled) {
  color: rgba(83, 109, 144, 0.72) !important;
  background: rgba(234, 240, 248, 0.9) !important;
  border-color: rgba(169, 191, 222, 0.22) !important;
}

.login-prompt {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.prompt-content {
  text-align: center;
  padding: 32px 28px;
  background: rgba(255, 255, 255, 0.84);
  border-radius: 22px;
  border: 1px solid rgba(61, 125, 255, 0.14);
  max-width: 360px;
}

.prompt-icon {
  font-size: 40px;
  color: #2f7bf6;
  margin-bottom: 12px;
}

.prompt-content h3 {
  margin: 0 0 8px;
}

.prompt-content p {
  margin: 0 0 18px;
  color: #6f84a8;
  line-height: 1.6;
}

.markdown-body :deep(pre) {
  max-width: 100%;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  padding: 12px;
  border-radius: 12px;
  background: rgba(16, 38, 63, 0.06);
  white-space: pre-wrap;
  word-break: break-word;
}

.markdown-body :deep(code) {
  font-family: Consolas, 'Courier New', monospace;
}

.markdown-body :deep(table) {
  display: block;
  max-width: 100%;
  width: 100%;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  border-collapse: collapse;
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  border: 1px solid rgba(61, 125, 255, 0.12);
  padding: 8px 10px;
}

.markdown-body :deep(img) {
  display: block;
  max-width: 100%;
  height: auto;
  border-radius: 12px;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  padding-left: 1.25em;
}

.markdown-body :deep(blockquote) {
  margin: 0;
  padding-left: 12px;
  border-left: 3px solid rgba(61, 125, 255, 0.26);
  color: #46648d;
}

.markdown-body :deep(p:first-child) {
  margin-top: 0;
}

.markdown-body :deep(p:last-child) {
  margin-bottom: 0;
}

.compact-shell .chat-sidebar {
  position: absolute;
  inset: 0 auto 0 0;
  max-width: calc(100% - 56px);
  transform: translateX(-100%);
  box-shadow: 12px 0 28px rgba(20, 34, 58, 0.18);
}

.compact-shell .chat-sidebar:not(.collapsed) {
  transform: translateX(0);
}

.narrow-shell .chat-sidebar {
  max-width: calc(100% - 40px);
}

@container (max-width: 860px) {

  .sidebar-resize-handle {
    display: none;
  }

  .chat-header {
    flex-wrap: wrap;
    align-items: flex-start;
  }

  .chat-info {
    min-width: 0;
    flex: 1 1 220px;
  }

  .chat-header-actions {
    margin-left: 0;
  }

  .message-content {
    max-width: min(92%, 100%);
  }
}

@container (max-width: 620px) {
  .chat-header {
    padding: 14px 16px;
    align-items: flex-start;
  }

  .chat-info {
    min-width: 0;
  }

  .chat-info h3,
  .chat-info p {
    overflow-wrap: anywhere;
  }

  .message-content {
    width: auto;
    flex: 1 1 auto;
    max-width: calc(100% - 46px);
  }

  .message.user .message-content {
    text-align: left;
  }

  .message-text {
    width: 100%;
  }

  .input-container {
    flex-direction: column;
    align-items: stretch;
  }

  .input-actions {
    justify-content: flex-end;
    flex-wrap: wrap;
  }
}

@media (max-width: 768px) {
  .ai-chat-shell {
    background: linear-gradient(180deg, rgba(248, 251, 255, 0.98), rgba(238, 244, 253, 0.96));
  }

  .ai-chat-shell.expanded {
    inset: 0;
    border-radius: 0;
    border: none;
  }

  .sidebar-mask {
    background: rgba(5, 18, 35, 0.42);
    backdrop-filter: blur(4px);
  }

  .compact-shell .chat-sidebar,
  .chat-sidebar {
    border-right-color: rgba(134, 196, 255, 0.16);
    box-shadow: 18px 0 40px rgba(5, 14, 30, 0.28);
  }

  .compact-shell .chat-sidebar {
    border-radius: 0 22px 22px 0;
  }

  .sidebar-header,
  .chat-header {
    padding: 14px 16px;
  }

  .message-list {
    padding: 18px 14px 22px;
    gap: 14px;
    scroll-padding-bottom: 18px;
    overscroll-behavior: contain;
  }

  .message-content {
    max-width: calc(100% - 48px);
  }

  .message-text {
    padding: 12px 14px;
    border-radius: 16px;
    font-size: 13px;
  }

  .message-avatar {
    flex: 0 0 auto;
  }

  .message-time {
    padding-inline: 4px;
  }

  .input-area {
    padding: 12px 12px calc(14px + env(safe-area-inset-bottom));
    background: rgba(240, 246, 255, 0.96);
  }

  .input-container {
    border-radius: 16px;
    padding: 10px;
    box-shadow: 0 12px 26px rgba(44, 76, 128, 0.1);
  }

  .input-actions {
    width: 100%;
  }

  .input-actions :deep(.el-button) {
    margin: 0;
  }

  .chat-header-actions {
    gap: 6px;
  }
}

:global(body.ai-chat-expanded-lock) {
  overflow: hidden;
}

:global(body.ai-chat-resizing) {
  cursor: col-resize !important;
  user-select: none !important;
}
</style>

