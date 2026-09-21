import { expect, test } from '@playwright/test'

test('ai patent results normalize compact public numbers and snake_case fields', async ({ page }) => {
  await page.route('**/api/ai/chat/patent', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 0,
        message: 'success',
        data: {
          requestId: 'normalize-case',
          aiAnalysis: '推荐专利 CN106895254A、CN101249945A。',
          extractedPublicNums: ['CN106895254ACN101249945A'],
          notFoundPublicNums: [],
          patents: [
            {
              dataset_id: 1,
              dataset_name: '新能源专利库',
              record_id: 'CN106895254A',
              category: 'hydrogen',
              public_num: 'CN106895254A',
              title: '一种储能系统控制方法',
              abstract: '用于提升储能调度效率',
              applicant: '上海柯来浦能源科技有限公司',
              inventor: '张三'
            }
          ]
        }
      })
    })
  })

  await page.goto('/patent')
  await page.getByTestId('ai-search-input').fill('帮我找储能系统控制方向的专利')
  await page.getByTestId('ai-search-submit').click()

  await expect(page.getByTestId('ai-results-panel')).toContainText('CN106895254A')
  await expect(page.getByTestId('ai-results-panel')).toContainText('CN101249945A')
  await expect(page.getByTestId('ai-results-panel')).toContainText('一种储能系统控制方法')
  await expect(page.getByTestId('ai-results-panel')).toContainText('新能源专利库')
  await expect(page.getByTestId('ai-results-panel')).toContainText('上海柯来浦能源科技有限公司')
})
