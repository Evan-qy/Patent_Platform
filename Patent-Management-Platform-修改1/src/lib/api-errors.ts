const SERVICE_UNAVAILABLE_MESSAGE = '服务暂时不可用，请稍后重试'

export const isServiceUnavailableError = (error: unknown) => {
  const responseStatus = (error as any)?.response?.status
  const errorCode = (error as any)?.code
  const errorMessage = String((error as any)?.message || '').toLowerCase()

  return (
    responseStatus === 502 ||
    responseStatus === 503 ||
    responseStatus === 504 ||
    errorCode === 'ERR_NETWORK' ||
    errorCode === 'ECONNABORTED' ||
    errorMessage.includes('network') ||
    errorMessage.includes('timeout') ||
    errorMessage.includes('bad gateway') ||
    errorMessage.includes('网关') ||
    errorMessage.includes('服务不可用')
  )
}

export const getApiErrorMessage = (error: unknown, fallback: string) => {
  if (isServiceUnavailableError(error)) {
    return SERVICE_UNAVAILABLE_MESSAGE
  }

  return (
    (error as any)?.response?.data?.message ||
    (error as any)?.response?.data?.error ||
    (error as any)?.message ||
    fallback
  )
}
