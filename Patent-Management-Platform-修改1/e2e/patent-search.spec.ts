import { expect, test } from '@playwright/test'

const patentHit = {
  datasetId: 1,
  datasetName: '新能源专利库',
  recordId: 'CN100001A',
  category: 'solar',
  publicNum: 'CN100001A',
  title: '一种光伏储能协同控制方法',
  abstractText: '用于提升光伏储能系统调度效率。',
  applicant: '知创未来研究院',
  inventor: '王工',
  score: 12.4
}

test.beforeEach(async ({ page }) => {
  await page.route('**/api/patents/es/search**', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 0,
        message: 'success',
        data: {
          total: 1,
          hits: [patentHit]
        }
      })
    })
  })

  await page.route('**/api/ai/chat/patent', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 0,
        message: 'success',
        data: {
          requestId: 'mock-request',
          aiAnalysis: '该需求与光伏储能协同控制相关，推荐 CN100001A。',
          extractedPublicNums: ['CN100001A'],
          notFoundPublicNums: [],
          patents: [patentHit]
        }
      })
    })
  })

  await page.route('**/api/patents/es/detail**', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 0,
        message: 'success',
        data: patentHit
      })
    })
  })
})

test('database search shows patent results and opens detail dialog', async ({ page }) => {
  await page.goto('/patent')

  await page.getByRole('textbox', { name: '数据库检索关键词' }).fill('光伏 储能')
  await page.getByRole('button', { name: '开始数据库检索' }).click()

  await expect(page.getByTestId('search-status')).toContainText('数据库检索命中 1 条')
  await expect(page.getByTestId('es-results-panel')).toContainText('一种光伏储能协同控制方法')

  await page.getByTestId('patent-title-link').click()
  await expect(page.getByRole('dialog')).toContainText('CN100001A')
})

test('ai search switches to AI result tab and shows extracted patent', async ({ page }) => {
  await page.goto('/patent')

  await page.getByRole('textbox', { name: 'AI 智能搜索需求描述' }).fill('我需要提升光伏储能系统调度效率的技术方案')
  await page.getByRole('button', { name: '开始 AI 智能搜索' }).click()

  await expect(page.getByTestId('result-tab-ai')).toHaveAttribute('aria-selected', 'true')
  await expect(page.getByTestId('search-status')).toContainText('AI 智能搜索命中 1 条')
  await expect(page.getByTestId('ai-results-panel')).toContainText('CN100001A')
})

test('mobile mode exposes AI search after switching mode tab', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/patent')

  await page.getByTestId('mode-ai').click()
  await expect(page.getByTestId('ai-search-input')).toBeVisible()
  await expect(page.getByTestId('mode-ai')).toHaveAttribute('aria-selected', 'true')
})
