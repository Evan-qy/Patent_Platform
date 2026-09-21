import { Capacitor } from '@capacitor/core'

type RuntimeWindow = Window & {
  __APP_API_ORIGIN__?: string
  __APP_API_BASE_URL__?: string
  __APP_WS_BASE_URL__?: string
}

const DEFAULT_NATIVE_API_ORIGIN = 'http://60.205.242.113:8080'
const API_ORIGIN_STORAGE_KEY = 'runtime.apiOrigin'

const trimValue = (value: unknown) => String(value || '').replace(/['"]/g, '').trim()

const stripTrailingSlash = (value: string) => value.replace(/\/+$/, '')

const getRuntimeWindow = () => window as RuntimeWindow

export const isNativeApp = () => Capacitor.isNativePlatform()

export const getStoredApiOrigin = () => stripTrailingSlash(trimValue(localStorage.getItem(API_ORIGIN_STORAGE_KEY)))

export const setStoredApiOrigin = (value: string) => {
  const nextValue = stripTrailingSlash(trimValue(value))
  if (!nextValue) {
    localStorage.removeItem(API_ORIGIN_STORAGE_KEY)
    return
  }
  localStorage.setItem(API_ORIGIN_STORAGE_KEY, nextValue)
}

const getNativeApiOrigin = () => {
  const win = getRuntimeWindow()
  const configuredOrigin =
    trimValue(win.__APP_API_ORIGIN__)
    || getStoredApiOrigin()
    || trimValue(import.meta.env.VITE_NATIVE_API_ORIGIN)
    || trimValue(import.meta.env.VITE_API_ORIGIN)
    || DEFAULT_NATIVE_API_ORIGIN

  return stripTrailingSlash(configuredOrigin)
}

const resolveRelativeBase = (basePath: string) => {
  if (!isNativeApp()) {
    return basePath
  }

  return `${getNativeApiOrigin()}${basePath.startsWith('/') ? basePath : `/${basePath}`}`
}

export const resolveApiBaseUrl = () => {
  const win = getRuntimeWindow()
  const runtimeBaseUrl = trimValue(win.__APP_API_BASE_URL__)
  if (runtimeBaseUrl) {
    return stripTrailingSlash(runtimeBaseUrl)
  }

  const configuredBaseUrl = trimValue(import.meta.env.VITE_API_BASE_URL || '/api')
  if (/^https?:\/\//i.test(configuredBaseUrl)) {
    return stripTrailingSlash(configuredBaseUrl)
  }

  return resolveRelativeBase(configuredBaseUrl || '/api')
}

export const resolveWsBaseUrl = () => {
  const win = getRuntimeWindow()
  const runtimeBaseUrl = trimValue(win.__APP_WS_BASE_URL__)
  if (runtimeBaseUrl) {
    return stripTrailingSlash(runtimeBaseUrl)
  }

  const configuredBaseUrl = trimValue(import.meta.env.VITE_WS_BASE_URL)
  if (configuredBaseUrl) {
    return stripTrailingSlash(configuredBaseUrl)
  }

  if (isNativeApp()) {
    return stripTrailingSlash(
      getNativeApiOrigin()
        .replace(/^https:\/\//i, 'wss://')
        .replace(/^http:\/\//i, 'ws://')
    )
  }

  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}`
}
