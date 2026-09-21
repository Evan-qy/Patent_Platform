# Vue 3 + TypeScript 开发指南

## 一、项目初始化

### 1. 使用 Vite 创建项目
```bash
# 使用 npm
npm create vite@latest 知识产权服务平台 -- --template vue-ts

# 使用 yarn
yarn create vite 知识产权服务平台 -- --template vue-ts

# 使用 pnpm
pnpm create vite 知识产权服务平台 -- --template vue-ts
```

### 2. 进入项目目录并安装依赖
```bash
cd 知识产权服务平台
npm install
```

## 二、开发环境配置

### 1. 安装核心依赖

#### UI组件库 - Element Plus
```bash
npm install element-plus
npm install -D unplugin-vue-components unplugin-auto-import
```

#### 状态管理 - Pinia
```bash
npm install pinia
```

#### HTTP请求 - Axios
```bash
npm install axios
```

#### 路由管理 - Vue Router
```bash
npm install vue-router@4
```

#### 数据可视化 - ECharts
```bash
npm install echarts
```

#### 工具库 - VueUse
```bash
npm install @vueuse/core
```

### 2. 配置 Vite

修改 `vite.config.ts` 文件，配置 Element Plus 自动导入和其他插件：

```typescript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      resolvers: [ElementPlusResolver()],
    }),
    Components({
      resolvers: [ElementPlusResolver()],
    }),
  ],
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080', // 后端接口地址
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      }
    }
  }
})
```

### 3. 配置 TypeScript

修改 `tsconfig.json` 文件：

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "module": "ESNext",
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "skipLibCheck": true,

    /* Bundler mode */
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "resolveJsonModule": true,
    "isolatedModules": true,
    "noEmit": true,
    "jsx": "preserve",

    /* Linting */
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noFallthroughCasesInSwitch": true,

    /* Path aliases */
    "baseUrl": ".",
    "paths": {
      "@/*": ["./src/*"]
    }
  },
  "include": ["src/**/*.ts", "src/**/*.d.ts", "src/**/*.tsx", "src/**/*.vue"],
  "references": [{ "path": "./tsconfig.node.json" }]
}
```

## 三、项目结构搭建

```
src/
├── assets/          # 静态资源
│   ├── images/      # 图片资源
│   └── styles/      # 全局样式
│       ├── reset.css # 重置样式
│       └── index.css # 全局样式
├── components/      # 组件
│   ├── common/      # 通用组件
│   │   ├── AppHeader.vue      # 头部组件
│   │   ├── AppSidebar.vue     # 侧边栏组件
│   │   └── AppFooter.vue      # 页脚组件
│   └── business/    # 业务组件
│       ├── PatentCard.vue     # 专利卡片
│       ├── ExpertCard.vue     # 专家卡片
│       └── DemandCard.vue     # 需求卡片
├── views/           # 页面
│   ├── Home.vue     # 首页
│   ├── Patent/      # 专利模块
│   │   ├── PatentList.vue     # 专利列表
│   │   └── PatentDetail.vue   # 专利详情
│   ├── Expert/      # 专家模块
│   │   ├── ExpertList.vue     # 专家列表
│   │   └── ExpertDetail.vue   # 专家详情
│   ├── Demand/      # 需求模块
│   │   ├── DemandList.vue     # 需求列表
│   │   └── DemandPublish.vue  # 需求发布
│   └── Evaluation/  # 价值评估模块
│       └── PatentEvaluation.vue # 专利价值评估
├── router/          # 路由
│   └── index.ts     # 路由配置
├── store/           # 状态管理
│   ├── index.ts     # Pinia 初始化
│   ├── user.ts      # 用户状态
│   └── search.ts    # 搜索状态
├── api/             # API 接口
│   ├── index.ts     # Axios 封装
│   ├── patent.ts    # 专利相关接口
│   └── expert.ts    # 专家相关接口
├── utils/           # 工具函数
│   ├── request.ts   # 请求工具
│   └── common.ts    # 通用工具
├── types/           # TypeScript 类型定义
│   ├── index.ts     # 全局类型
│   └── patent.ts    # 专利类型
├── App.vue          # 根组件
└── main.ts          # 入口文件
```

## 四、核心配置与实现

### 1. 入口文件配置 (main.ts)

```typescript
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)
app.use(ElementPlus)
app.mount('#app')
```

### 2. 路由配置 (router/index.ts)

```typescript
import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'

const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue'),
    meta: { title: '首页' }
  },
  {
    path: '/patent',
    name: 'PatentList',
    component: () => import('../views/Patent/PatentList.vue'),
    meta: { title: '专利列表' }
  },
  {
    path: '/patent/:id',
    name: 'PatentDetail',
    component: () => import('../views/Patent/PatentDetail.vue'),
    meta: { title: '专利详情' }
  },
  {
    path: '/expert',
    name: 'ExpertList',
    component: () => import('../views/Expert/ExpertList.vue'),
    meta: { title: '专家列表' }
  },
  {
    path: '/expert/:id',
    name: 'ExpertDetail',
    component: () => import('../views/Expert/ExpertDetail.vue'),
    meta: { title: '专家详情' }
  },
  {
    path: '/demand',
    name: 'DemandList',
    component: () => import('../views/Demand/DemandList.vue'),
    meta: { title: '需求列表' }
  },
  {
    path: '/demand/publish',
    name: 'DemandPublish',
    component: () => import('../views/Demand/DemandPublish.vue'),
    meta: { title: '发布需求' }
  },
  {
    path: '/evaluation',
    name: 'PatentEvaluation',
    component: () => import('../views/Evaluation/PatentEvaluation.vue'),
    meta: { title: '专利价值评估' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫 - 设置页面标题
router.beforeEach((to, from, next) => {
  if (to.meta.title) {
    document.title = `${to.meta.title} - 高校知识产权运营服务平台`
  }
  next()
})

export default router
```

### 3. API 封装 (api/index.ts)

```typescript
import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'

class ApiService {
  private axiosInstance: AxiosInstance

  constructor() {
    this.axiosInstance = axios.create({
      baseURL: '/api',
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json'
      }
    })

    // 请求拦截器
    this.axiosInstance.interceptors.request.use(
      (config) => {
        // 添加token
        const token = localStorage.getItem('token')
        if (token) {
          config.headers.Authorization = `Bearer ${token}`
        }
        return config
      },
      (error) => {
        return Promise.reject(error)
      }
    )

    // 响应拦截器
    this.axiosInstance.interceptors.response.use(
      (response: AxiosResponse) => {
        return response.data
      },
      (error) => {
        // 错误处理
        if (error.response) {
          switch (error.response.status) {
            case 401:
              // 未授权处理
              localStorage.removeItem('token')
              window.location.href = '/login'
              break
            case 403:
              // 权限不足处理
              ElMessage.error('没有操作权限')
              break
            case 500:
              // 服务器错误处理
              ElMessage.error('服务器内部错误')
              break
            default:
              ElMessage.error(error.response.data.message || '请求失败')
          }
        } else if (error.request) {
          ElMessage.error('网络连接失败，请检查网络设置')
        } else {
          ElMessage.error('请求配置错误')
        }
        return Promise.reject(error)
      }
    )
  }

  // GET请求
  get<T = any>(url: string, params?: any, config?: AxiosRequestConfig): Promise<T> {
    return this.axiosInstance.get(url, { params, ...config })
  }

  // POST请求
  post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return this.axiosInstance.post(url, data, config)
  }

  // PUT请求
  put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return this.axiosInstance.put(url, data, config)
  }

  // DELETE请求
  delete<T = any>(url: string, params?: any, config?: AxiosRequestConfig): Promise<T> {
    return this.axiosInstance.delete(url, { params, ...config })
  }
}

export default new ApiService()
```

### 4. 状态管理 (store/user.ts)

```typescript
import { defineStore } from 'pinia'
import type { UserInfo } from '../types'

export const useUserStore = defineStore('user', {
  state: () => ({
    userInfo: null as UserInfo | null,
    token: localStorage.getItem('token') || '',
    isLoggedIn: !!localStorage.getItem('token')
  }),
  actions: {
    // 登录
    async login(loginData: { username: string; password: string }) {
      try {
        // 调用登录接口
        const response = await api.post('/auth/login', loginData)
        const { token, userInfo } = response.data
        
        // 保存token和用户信息
        this.token = token
        this.userInfo = userInfo
        this.isLoggedIn = true
        
        localStorage.setItem('token', token)
        localStorage.setItem('userInfo', JSON.stringify(userInfo))
        
        return response.data
      } catch (error) {
        throw error
      }
    },
    // 登出
    logout() {
      this.token = ''
      this.userInfo = null
      this.isLoggedIn = false
      
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    },
    // 获取用户信息
    async getUserInfo() {
      try {
        const response = await api.get('/user/info')
        this.userInfo = response.data
        localStorage.setItem('userInfo', JSON.stringify(response.data))
        return response.data
      } catch (error) {
        throw error
      }
    }
  }
})
```

### 5. TypeScript 类型定义 (types/patent.ts)

```typescript
// 专利信息类型
export interface Patent {
  id: string
  patentNumber: string
  title: string
  abstract: string
  applicant: string[]
  inventor: string[]
  applicationDate: string
  publicationDate: string
  grantDate: string
  patentType: '发明专利' | '实用新型专利' | '外观设计专利'
  classification: string
  status: '申请中' | '已授权' | '无效' | '过期'
  keywords: string[]
  techFeatures: string[]
  transformStatus: '未转化' | '转化中' | '已转化'
  estimatedValue?: number
  createTime: string
  updateTime: string
}

// 专利搜索参数类型
export interface PatentSearchParams {
  keyword?: string
  patentNumber?: string
  applicant?: string
  inventor?: string
  patentType?: string
  classification?: string
  status?: string
  transformStatus?: string
  page: number
  size: number
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
}

// 专利搜索结果类型
export interface PatentSearchResult {
  list: Patent[]
  total: number
  page: number
  size: number
}
```

## 五、核心功能实现示例

### 1. 专利列表组件 (views/Patent/PatentList.vue)

```vue
<template>
  <div class="patent-list-container">
    <!-- 搜索条件 -->
    <el-card class="search-card">
      <el-form :model="searchForm" :inline="true" label-width="80px">
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="专利名称、摘要、申请人等" clearable />
        </el-form-item>
        <el-form-item label="专利类型">
          <el-select v-model="searchForm.patentType" placeholder="选择专利类型" clearable>
            <el-option label="发明专利" value="发明专利" />
            <el-option label="实用新型专利" value="实用新型专利" />
            <el-option label="外观设计专利" value="外观设计专利" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="选择状态" clearable>
            <el-option label="申请中" value="申请中" />
            <el-option label="已授权" value="已授权" />
            <el-option label="无效" value="无效" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 专利列表 -->
    <el-card class="patent-card-container">
      <div class="patent-list" v-if="patentList.length > 0">
        <patent-card
          v-for="patent in patentList"
          :key="patent.id"
          :patent="patent"
          @click="goToDetail(patent.id)"
        />
      </div>
      <div class="empty-state" v-else>
        <el-empty description="暂无专利数据" />
      </div>
    </el-card>

    <!-- 分页 -->
    <div class="pagination-container">
      <el-pagination
        v-model:current-page="searchForm.page"
        v-model:page-size="searchForm.size"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import PatentCard from '../../components/business/PatentCard.vue'
import api from '../../api'
import type { Patent, PatentSearchParams, PatentSearchResult } from '../../types/patent'

const router = useRouter()

// 搜索参数
const searchForm = reactive<PatentSearchParams>({
  keyword: '',
  patentNumber: '',
  applicant: '',
  inventor: '',
  patentType: '',
  classification: '',
  status: '',
  transformStatus: '',
  page: 1,
  size: 10,
  sortBy: 'applicationDate',
  sortOrder: 'desc'
})

// 专利列表数据
const patentList = ref<Patent[]>([])
const total = ref(0)

// 搜索加载状态
const loading = ref(false)

// 搜索方法
const handleSearch = () => {
  searchForm.page = 1
  getPatentList()
}

// 重置搜索条件
const resetForm = () => {
  Object.assign(searchForm, {
    keyword: '',
    patentNumber: '',
    applicant: '',
    inventor: '',
    patentType: '',
    classification: '',
    status: '',
    transformStatus: '',
    page: 1,
    size: 10
  })
  getPatentList()
}

// 获取专利列表
const getPatentList = async () => {
  try {
    loading.value = true
    const result = await api.get<PatentSearchResult>('/patent/list', searchForm)
    patentList.value = result.list
    total.value = result.total
  } catch (error) {
    console.error('获取专利列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 分页大小变化
const handleSizeChange = () => {
  getPatentList()
}

// 当前页变化
const handleCurrentChange = () => {
  getPatentList()
}

// 跳转到专利详情
const goToDetail = (id: string) => {
  router.push(`/patent/${id}`)
}

// 页面挂载时获取专利列表
onMounted(() => {
  getPatentList()
})
</script>

<style scoped>
.patent-list-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.search-card {
  margin-bottom: 20px;
}

.patent-card-container {
  margin-bottom: 20px;
}

.patent-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(380px, 1fr));
  gap: 20px;
}

.empty-state {
  text-align: center;
  padding: 50px 0;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
```

### 2. 专利卡片组件 (components/business/PatentCard.vue)

```vue
<template>
  <el-card class="patent-card" hoverable @click="$emit('click')">
    <div class="patent-header">
      <div class="patent-title">{{ patent.title }}</div>
      <el-tag :type="getPatentTypeTagType(patent.patentType)">
        {{ patent.patentType }}
      </el-tag>
    </div>
    
    <div class="patent-info">
      <div class="info-item">
        <span class="label">专利号：</span>
        <span class="value">{{ patent.patentNumber }}</span>
      </div>
      <div class="info-item">
        <span class="label">申请人：</span>
        <span class="value">{{ patent.applicant.join('、') }}</span>
      </div>
      <div class="info-item">
        <span class="label">申请日期：</span>
        <span class="value">{{ formatDate(patent.applicationDate) }}</span>
      </div>
      <div class="info-item">
        <span class="label">状态：</span>
        <el-tag :type="getStatusTagType(patent.status)">
          {{ patent.status }}
        </el-tag>
      </div>
    </div>
    
    <div class="patent-abstract">
      <div class="label">摘要：</div>
      <div class="value">{{ patent.abstract }}</div>
    </div>
    
    <div class="patent-footer">
      <el-button type="primary" size="small" @click.stop="viewDetail">
        查看详情
      </el-button>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import type { Patent } from '../../types/patent'

const router = useRouter()

// Props
const props = defineProps<{
  patent: Patent
}>()

// Emits
const emit = defineEmits<{
  click: []
}>()

// 专利类型标签样式
const getPatentTypeTagType = (type: string): string => {
  switch (type) {
    case '发明专利':
      return 'danger'
    case '实用新型专利':
      return 'success'
    case '外观设计专利':
      return 'warning'
    default:
      return 'info'
  }
}

// 状态标签样式
const getStatusTagType = (status: string): string => {
  switch (status) {
    case '申请中':
      return 'warning'
    case '已授权':
      return 'success'
    case '无效':
      return 'danger'
    case '过期':
      return 'info'
    default:
      return 'info'
  }
}

// 格式化日期
const formatDate = (dateStr: string): string => {
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN')
}

// 查看详情
const viewDetail = () => {
  router.push(`/patent/${props.patent.id}`)
}
</script>

<style scoped>
.patent-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  cursor: pointer;
}

.patent-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 15px;
}

.patent-title {
  font-size: 16px;
  font-weight: bold;
  color: #333;
  flex: 1;
  margin-right: 10px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.patent-info {
  margin-bottom: 15px;
}

.info-item {
  display: flex;
  margin-bottom: 8px;
}

.info-item .label {
  width: 70px;
  color: #666;
  font-size: 14px;
}

.info-item .value {
  flex: 1;
  color: #333;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.patent-abstract {
  flex: 1;
  margin-bottom: 15px;
}

.patent-abstract .label {
  color: #666;
  font-size: 14px;
  margin-bottom: 5px;
  display: block;
}

.patent-abstract .value {
  color: #333;
  font-size: 14px;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

.patent-footer {
  display: flex;
  justify-content: flex-end;
}
</style>
```

## 六、测试与部署

### 1. 单元测试 - Vitest

安装 Vitest：
```bash
npm install -D vitest @vue/test-utils
```

运行测试：
```bash
npm run test
```

### 2. E2E测试 - Cypress

安装 Cypress：
```bash
npm install -D cypress
```

运行 Cypress：
```bash
npx cypress open
```

### 3. 构建生产版本

```bash
npm run build
```

构建产物将输出到 `dist` 目录。

### 4. 部署

- **Nginx**：将 `dist` 目录部署到 Nginx 服务器
- **云服务**：可部署到阿里云、腾讯云等云服务平台
- **CDN**：静态资源可使用 CDN 加速

## 七、开发规范

### 1. 代码规范

- 使用 ESLint 和 Prettier 进行代码检查和格式化
- 遵循 Vue 3 Composition API 规范
- 组件命名使用 PascalCase
- 文件命名使用 kebab-case

### 2. 提交规范

使用 Conventional Commits 规范：
- `feat`：新功能
- `fix`：修复 bug
- `docs`：文档更新
- `style`：代码格式调整
- `refactor`：代码重构
- `test`：测试相关
- `chore`：构建或工具更新

### 3. 注释规范

- 为复杂逻辑添加注释
- 组件添加 props 和 emit 注释
- 工具函数添加 JSDoc 注释

## 八、开发工具推荐

- **VS Code**：推荐的代码编辑器
- **Volar**：Vue 3 官方插件，提供语法高亮和智能提示
- **TypeScript Vue Plugin**：TypeScript 支持
- **ESLint**：代码检查
- **Prettier**：代码格式化
- **GitLens**：Git 增强插件

## 九、学习资源

- [Vue 3 官方文档](https://cn.vuejs.org/)
- [TypeScript 官方文档](https://www.typescriptlang.org/docs/)
- [Element Plus 文档](https://element-plus.org/zh-CN/)
- [Pinia 文档](https://pinia.vuejs.org/)
- [Vite 文档](https://cn.vitejs.dev/)

## 总结

本指南提供了 Vue 3 + TypeScript 开发知识产权服务平台的完整流程，从项目初始化到核心功能实现。在实际开发中，建议根据具体需求灵活调整技术方案，注重代码质量和用户体验，定期进行代码审查和测试，确保项目的可维护性和稳定性。