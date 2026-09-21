# 我的知识产权 · 前端

高校知识产权运营服务平台的 Web 前端，同时通过 Capacitor 打包为 Android 应用。

## 技术栈

| 项 | 选型 |
| --- | --- |
| 框架 | Vue 3（Composition API）+ TypeScript |
| 构建 | Vite 4 |
| UI | Element Plus（按需自动导入） |
| 图表 | ECharts（数据大屏） |
| 富文本 | TipTap（CMS 文章编辑器） |
| HTTP | Axios |
| 移动端 | Capacitor 8（Android） |
| E2E | Playwright |

## 开发

```bash
npm install
npm run dev          # http://localhost:5173
```

开发环境由 Vite 代理到后端（见 `vite.config.ts`）：

- `/api` → `http://127.0.0.1:8080`
- `/ws` → `ws://127.0.0.1:8080`（WebSocket，AI 流式对话）

后端不在默认端口时用环境变量覆盖：

```bash
VITE_API_PROXY_TARGET=http://127.0.0.1:8081 VITE_WS_PROXY_TARGET=ws://127.0.0.1:8081 npm run dev
```

## 构建

```bash
npm run build        # vue-tsc 类型检查 + vite build，产物在 dist/
npm run preview      # 本地预览构建产物（4173）
```

生产环境由 Nginx 托管 `dist/`，`/api` 与 `/ws` 反代到后端。

## 环境变量

`.env.development` / `.env.production` 中定义：

| 变量 | 说明 |
| --- | --- |
| `VITE_API_BASE_URL` | 接口前缀，默认 `/api` |
| `VITE_APP_TITLE` | 页面标题 |
| `VITE_AI_MODEL` | 前端展示的 AI 模型名 |

## 移动端（Android）

```bash
npm run android:build    # 构建 + cap sync android，再用 Android Studio 打包
```

原生环境的接口地址由 `src/lib/runtime.ts` 解析，优先级为：

1. `window.__APP_API_ORIGIN__`
2. `localStorage.runtime.apiOrigin`（App 内「设置页切服务器」写入）
3. `VITE_NATIVE_API_ORIGIN` / `VITE_API_ORIGIN`
4. 代码内默认值

> **注意**：`runtime.ts` 里的默认值已指向当前生产环境 `https://hebut-ip.com`。注意不要改回带端口的形式——服务器只对外放行了 80/443/8888/22，后端 8080 并未暴露，App 必须经域名走 443。如需换服务器，同时更新 `runtime.ts` 与 `android/app/src/main/res/xml/network_security_config.xml`。

## E2E 测试

`playwright.config.ts` 配置了双工程（Desktop Chrome + Pixel 5 移动端），`webServer` 会自动执行 `npm run build && npm run preview` 并监听 4173。

```bash
npm run test:e2e
npm run test:e2e:ui
```

## 目录结构

```
src/
├── api/         接口封装（index.ts / admin.ts / cms.ts）
├── components/  通用组件
├── composables/ 组合式函数
├── constants/   常量
├── content/     页面文案
├── lib/         运行时工具（runtime.ts 负责接口地址解析）
├── router/      路由与鉴权守卫
├── styles/      全局样式
├── types/       TypeScript 类型
└── views/       页面（含 views/admin/ 管理端）
```

## 相关文档

- `docs/` —— WCAG 对比度报告、品牌视觉规范、文案复用表、用户行为日志说明
- `Vue3+TypeScript开发指南.md` —— 编码约定
- `DESIGN_SYSTEM.md` —— 设计系统
- `我的知识产权_软件说明书_V2.0.md` —— 软件说明书
