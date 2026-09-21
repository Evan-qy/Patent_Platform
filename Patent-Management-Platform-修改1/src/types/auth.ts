export interface RegisterParams {
  username: string
  password: string
  phone?: string
  email?: string
  nickname?: string
}

export interface RegisterResponse {
  userId: number
}

export interface LoginParams {
  username: string
  password: string
  captchaId: string
  captchaAnswer: string
}

export type LoginResponse = string

export interface LoginCaptcha {
  captchaId: string
  question: string
  expiresInSeconds: number
}

export interface PresenceUserSummary {
  displayName: string
  type: 'user' | 'guest'
  page: string
  lastSeenAt: number
}

export interface PresenceStatus {
  onlineUsers: number
  authenticatedUsers: number
  guestUsers: number
  heartbeatWindowSeconds: number
  activeUsers: PresenceUserSummary[]
}

export interface UserInfo {
  userId: number
  username: string
  phone?: string
  email?: string
  nickname?: string
  avatarUrl?: string
  realName?: string
}

export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}
