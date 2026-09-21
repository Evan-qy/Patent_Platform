import { expect, test } from '@playwright/test'

const requirementItem = {
  id: 1001,
  title: '光伏储能联合调度需求',
  description: '希望寻找可提升园区微电网调度效率的专利与专家方案。',
  keywords: '光伏, 储能, 微电网',
  techDirection: '新能源',
  cooperationMode: '技术许可',
  contactInfo: '张老师 13800138000',
  budget: 120,
  deadline: '2026-06-30',
  status: 'PENDING',
  createdDate: '2026-04-20'
}

test.beforeEach(async ({ page }) => {
  await page.route('**/api/requirements**', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 0,
        message: 'success',
        data: {
          content: [requirementItem],
          totalElements: 1,
          totalPages: 1,
          size: 10,
          number: 0,
          first: true,
          last: true,
          empty: false
        }
      })
    })
  })
})

test('requirement detail dialog shows contact phone and budget', async ({ page }) => {
  await page.goto('/requirement')

  await expect(page.getByRole('heading', { name: '需求广场' })).toBeVisible()
  await expect(page.getByText('光伏储能联合调度需求')).toBeVisible()

  await page.getByRole('button', { name: '查看详情' }).first().click()

  const dialog = page.getByRole('dialog', { name: '需求详情' })
  await expect(dialog).toContainText('联系电话')
  await expect(dialog).toContainText('张老师 13800138000')
  await expect(dialog).toContainText('预算')
  await expect(dialog).toContainText('120 万元')
  await expect(dialog).toContainText('截止日期')
  await expect(dialog).toContainText('2026-06-30')
})
