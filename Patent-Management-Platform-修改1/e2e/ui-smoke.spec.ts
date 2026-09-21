import { expect, test } from '@playwright/test'

async function stubCommonApis(page: import('@playwright/test').Page) {
  await page.route('**/api/home-content**', async route => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 0, message: 'ok', data: {} }) })
  })
  await page.route('**/api/articles**', async route => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 0, message: 'ok', data: [] }) })
  })
  await page.route('**/api/public/presence**', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 0, message: 'ok', data: { onlineUsers: 1, authenticatedUsers: 0, guestUsers: 1, activeUsers: [] } })
    })
  })
  await page.route('**/api/auth/captcha**', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 0, message: 'ok', data: { captchaId: 'mock-captcha', question: '1 + 1 = ?', expiresInSeconds: 300 } })
    })
  })
  await page.route('**/api/requirements**', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 0,
        message: 'ok',
        data: { content: [], totalElements: 0, totalPages: 0, size: 10, number: 0, first: true, last: true, empty: true }
      })
    })
  })
}

test('public pages load without runtime errors', async ({ page }) => {
  const runtimeErrors: string[] = []
  page.on('pageerror', error => runtimeErrors.push(error.message))

  await stubCommonApis(page)

  await page.goto('/')
  await expect(page.getByRole('button', { name: 'AI 咨询' })).toBeVisible()

  await page.goto('/patent')
  await expect(page.getByTestId('search-status')).toBeVisible()

  await page.goto('/requirement')
  await expect(page.getByRole('heading', { name: '需求广场' })).toBeVisible()

  expect(runtimeErrors).toEqual([])
})

test('protected pages redirect to login instead of crashing', async ({ page }) => {
  await stubCommonApis(page)

  await page.goto('/evaluation')
  await expect(page).toHaveURL(/\/login/)
  await expect(page.getByRole('button', { name: '登录' })).toBeVisible()

  await page.goto('/valuation')
  await expect(page).toHaveURL(/\/login/)

  await page.goto('/transformation')
  await expect(page).toHaveURL(/\/login/)
})
