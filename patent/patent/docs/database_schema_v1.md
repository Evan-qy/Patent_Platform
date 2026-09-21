# 数据库结构说明（V1）

本说明依据项目计划书功能点（专利/专家查询、需求发布与智能匹配、转化成果展示、价值评估预测、权限管理、数据同步、统计分析等）整理，并以你的 5 张专利分表结构为基准。

对应完整建表 SQL：

- 主业务表：`docs/database_schema_v1.sql`
- AI 聊天会话（持久化）：`docs/database_schema_ai_chat.sql`

## 0. 总览（表格视图）

| 模块 | 表名 | 说明 | 主键 | 主要关联 |
|---|---|---|---|---|
| 专利库 | patent_wind / patent_solar / patent_biomass / patent_hydrogen / patent_lilon | 专利分表（同结构，按领域） | public_num | 被匹配/评估/成果表通过 (patent_category, patent_public_num) 引用 |
| 机构/专家 | organization | 高校/企业/服务机构统一建模 | id | expert.org_id、user_account.org_id、requirement.requester_org_id、transformation_result.partner_org_id |
| 机构/专家 | expert | 专家库 | id | user_account.expert_id、requirement_expert_match.expert_id、transformation_result.expert_id |
| 用户权限 | user_account | 登录账号与身份类型 | id | user_role.user_id、requirement.requester_user_id、通知/审计/同步 created_by 等 |
| 用户权限 | role / permission | RBAC 权限模型 | id | role_permission、user_role |
| 需求 | requirement | 需求发布 | id | requirement_*_match.requirement_id、transformation_result.requirement_id、通知 related_requirement_id |
| 匹配 | requirement_patent_match | 需求-专利匹配结果 | id | FK -> requirement.id |
| 匹配 | requirement_expert_match | 需求-专家匹配结果 | id | FK -> requirement.id、expert.id |
| 评估 | patent_valuation_report | 专利评估报告 | id | 通过 (patent_category, patent_public_num) 关联专利 |
| 评估 | patent_valuation_model_param | 评估模型参数 | id | updated_by -> user_account.id |
| 成果 | transformation_result | 转化成果/案例 | id | expert_id、requirement_id、partner_org_id |
| 运营 | user_notification | 通知/推送 | id | user_id、related_requirement_id |
| 运营 | data_sync_job | 数据同步任务 | id | created_by -> user_account.id |
| 运营 | audit_log | 审计日志 | id | user_id -> user_account.id |
| AI/对话 | chat_session / chat_message | 聊天会话与消息记录（持久化多轮对话） | id | chat_session.user_id -> user_account.id（逻辑关联）；chat_message.session_id -> chat_session.id（逻辑关联） |

## 1. 专利库（5 张分表，同结构）

用于存放不同行业/方向的专利数据（按你的 DDL 原样定义）。

- `patent_wind`（风能）
- `patent_solar`（光伏）
- `patent_biomass`（生物质）
- `patent_hydrogen`（氢能）
- `patent_lilon`（锂电）

**主键**
- `public_num` varchar(32) PK（公开号）

**核心字段**
- `title`、`abstract`、`applicant`、`inventor`、`appli_num`、`appli_date`、`public_date`
- `IPC` / `CPC` / `NEC`
- `legal_status`、`latest_legal_status`、`status`、`patent_details`

**索引**
- `public_num`
- `applicant(255)`、`inventor(255)`

### 1.1 专利分表字段（表格视图）

适用表：`patent_wind` / `patent_solar` / `patent_biomass` / `patent_hydrogen` / `patent_lilon`

| 字段名 | 类型 | 约束/说明 |
|---|---|---|
| public_num | varchar(32) | PK，公开号 |
| legal_status | text | 法律状态（历史/过程） |
| latest_legal_status | text | 最新法律状态 |
| status | text | 专利状态（如公开/授权等） |
| title | text | 标题 |
| type | text | 类型 |
| abstract | text | 摘要 |
| appli_num | text | 申请号 |
| appli_date | text | 申请日 |
| public_date | text | 公布日 |
| applicant | text | 申请人 |
| applicant_address | text | 申请人地址 |
| patentee | text | 专利权人 |
| patentee_address | text | 专利权人地址 |
| inventor | text | 发明人 |
| agent | text | 代理机构/代理人 |
| IPC | text | IPC 分类号 |
| CPC | text | CPC 分类号 |
| NEC | text | NEC 分类号 |
| patent_details | text | 详情/全文 |

**索引（各表一致，仅名称不同）**

| 索引名 | 字段 | 说明 |
|---|---|---|
| idx_*_public_num | public_num | 公开号快速定位 |
| idx_*_applicant | applicant(255) | 申请人检索 |
| idx_*_inventor | inventor(255) | 发明人检索 |

## 2. 机构与专家库

### 2.1 `organization`（机构）

用于统一表达高校、企业、知识产权服务机构等实体。

- `id` bigint PK
- `name` varchar(255) NOT NULL（机构名称）
- `type` varchar(32) NOT NULL（机构类型：UNIVERSITY / ENTERPRISE / SERVICE 等）
- `credit_code` varchar(64)（统一社会信用代码，可选）
- `address` varchar(512)（地址，可选）
- `contact_name` / `contact_phone` / `contact_email`（联系人信息）
- `created_at` / `updated_at`

#### 2.1.1 organization 字段表

| 字段名 | 类型 | 约束/说明 |
|---|---|---|
| id | bigint | PK，AUTO_INCREMENT |
| name | varchar(255) | NOT NULL，机构名称 |
| type | varchar(32) | NOT NULL，机构类型 |
| credit_code | varchar(64) | 统一社会信用代码（可选） |
| address | varchar(512) | 地址（可选） |
| contact_name | varchar(64) | 联系人（可选） |
| contact_phone | varchar(32) | 联系电话（可选） |
| contact_email | varchar(128) | 联系邮箱（可选） |
| created_at | datetime(3) | 创建时间 |
| updated_at | datetime(3) | 更新时间 |

### 2.2 `expert`（专家）

专家数据库，可关联所属机构。

- `id` bigint PK
- `name` varchar(128) NOT NULL
- `field` varchar(255)（领域，可选）
- `expertise` text（专长）
- `achievements` text（成果）
- `contact_info` varchar(255)（联系方式，可选）
- `org_id` bigint FK -> `organization.id`（所属机构，可选）
- `position_title` varchar(128)（职称/岗位，可选）
- `created_at` / `updated_at`

#### 2.2.1 expert 字段表

| 字段名 | 类型 | 约束/说明 |
|---|---|---|
| id | bigint | PK，AUTO_INCREMENT |
| name | varchar(128) | NOT NULL，姓名 |
| field | varchar(255) | 领域（可选） |
| expertise | text | 专长（可选） |
| achievements | text | 成果（可选） |
| contact_info | varchar(255) | 联系方式（可选） |
| org_id | bigint | FK -> organization.id（可选） |
| position_title | varchar(128) | 职称/岗位（可选） |
| created_at | datetime(3) | 创建时间 |
| updated_at | datetime(3) | 更新时间 |

## 3. 用户与权限（RBAC）

### 3.1 `user_account`（用户账号）

- `id` bigint PK
- `username` varchar(64) UNIQUE（登录名）
- `password_hash` varchar(255)（密码哈希）
- `phone` / `email`
- `user_type` varchar(32) NOT NULL（PERSONAL / ORG / EXPERT / ADMIN 等）
- `org_id` bigint FK -> `organization.id`（机构用户可选）
- `expert_id` bigint FK -> `expert.id`（专家用户可选）
- `status` varchar(32) NOT NULL DEFAULT 'ACTIVE'
- `created_at` / `updated_at`

#### 3.1.1 user_account 字段表

| 字段名 | 类型 | 约束/说明 |
|---|---|---|
| id | bigint | PK，AUTO_INCREMENT |
| username | varchar(64) | UNIQUE，登录名 |
| password_hash | varchar(255) | NOT NULL，密码哈希 |
| phone | varchar(32) | 手机（可选） |
| email | varchar(128) | 邮箱（可选） |
| user_type | varchar(32) | NOT NULL，用户类型 |
| org_id | bigint | FK -> organization.id（可选） |
| expert_id | bigint | FK -> expert.id（可选） |
| status | varchar(32) | NOT NULL DEFAULT 'ACTIVE' |
| created_at | datetime(3) | 创建时间 |
| updated_at | datetime(3) | 更新时间 |

3.12 user_profiles

| 字段名     | 类型         | 是否为空 | 键类型  | 默认值               | 说明                           |
| ---------- | ------------ | -------- | ------- | -------------------- | ------------------------------ |
| user_id    | BIGINT       | 否       | PK / FK | —                    | 用户ID，对应 `user_account.id` |
| nickname   | VARCHAR(128) | 是       | —       | NULL                 | 昵称                           |
| avatar_url | VARCHAR(512) | 是       | —       | NULL                 | 头像URL                        |
| real_name  | VARCHAR(128) | 是       | —       | NULL                 | 真实姓名                       |
| id_number  | VARCHAR(64)  | 是       | —       | NULL                 | 证件号码                       |
| bio        | TEXT         | 是       | —       | NULL                 | 个人简介                       |
| updated_at | DATETIME(3)  | 否       | —       | CURRENT_TIMESTAMP(3) | 更新时间（自动更新）           |

### 3.2 `role`（角色）

- `id` bigint PK
- `code` varchar(64) UNIQUE（如 ADMIN、ORG_USER、EXPERT）
- `name` varchar(128)
- `created_at`

#### 3.2.1 role 字段表

| 字段名 | 类型 | 约束/说明 |
|---|---|---|
| id | bigint | PK，AUTO_INCREMENT |
| code | varchar(64) | UNIQUE，角色编码 |
| name | varchar(128) | NOT NULL，角色名称 |
| created_at | datetime(3) | 创建时间 |

### 3.3 `permission`（权限点）

- `id` bigint PK
- `code` varchar(128) UNIQUE（如 PATENT_READ、REQ_CREATE）
- `name` varchar(255)
- `created_at`

#### 3.3.1 permission 字段表

| 字段名 | 类型 | 约束/说明 |
|---|---|---|
| id | bigint | PK，AUTO_INCREMENT |
| code | varchar(128) | UNIQUE，权限编码 |
| name | varchar(255) | NOT NULL，权限名称 |
| created_at | datetime(3) | 创建时间 |

### 3.4 关系表

- `role_permission`：`role_id` + `permission_id`（多对多）
- `user_role`：`user_id` + `role_id`（多对多）

#### 3.4.1 role_permission 字段表

| 字段名 | 类型 | 约束/说明 |
|---|---|---|
| role_id | bigint | PK(联合)，FK -> role.id |
| permission_id | bigint | PK(联合)，FK -> permission.id |

#### 3.4.2 user_role 字段表

| 字段名 | 类型 | 约束/说明 |
|---|---|---|
| user_id | bigint | PK(联合)，FK -> user_account.id |
| role_id | bigint | PK(联合)，FK -> role.id |

## 4. 需求（供需对接）

### `requirement`（需求）

需求方发布的技术需求信息。

- `id` bigint PK
- `title` varchar(255) NOT NULL
- `description` text（详情）
- `keywords` varchar(1024)（关键词）
- `tech_direction` varchar(255)（技术方向，可选）
- `cooperation_mode` varchar(255)（合作模式，可选）
- `requester_type` varchar(32) NOT NULL DEFAULT 'PERSONAL'（需求方类型）
- `requester_user_id` bigint FK -> `user_account.id`（个人需求方）
- `requester_org_id` bigint FK -> `organization.id`（机构需求方）
- `status` varchar(32) NOT NULL DEFAULT 'PENDING'
- `created_date` / `updated_at`

#### 4.1 requirement 字段表

| 字段名 | 类型 | 约束/说明 |
|---|---|---|
| id | bigint | PK，AUTO_INCREMENT |
| title | varchar(255) | NOT NULL，标题 |
| description | text | 详情（可选） |
| keywords | varchar(1024) | 关键词（可选） |
| tech_direction | varchar(255) | 技术方向（可选） |
| cooperation_mode | varchar(255) | 合作模式（可选） |
| requester_type | varchar(32) | NOT NULL DEFAULT 'PERSONAL' |
| requester_user_id | bigint | FK -> user_account.id（可选） |
| requester_org_id | bigint | FK -> organization.id（可选） |
| status | varchar(32) | NOT NULL DEFAULT 'PENDING' |
| created_date | datetime(3) | 创建时间 |
| updated_at | datetime(3) | 更新时间 |

## 5. 匹配结果（需求 ↔ 专利/专家）

### 5.1 `requirement_patent_match`（需求-专利匹配）

由于专利分表存储，通过 `patent_category + patent_public_num` 定位专利。

- `id` bigint PK
- `requirement_id` bigint FK -> `requirement.id`
- `patent_category` varchar(32)（wind/solar/biomass/hydrogen/lilon）
- `patent_public_num` varchar(32)（对应分表 public_num）
- `match_score` decimal(6,3)（可选）
- `match_reason` text（可选）
- UNIQUE：`(requirement_id, patent_category, patent_public_num)`
- 索引：`(patent_category, patent_public_num)`

#### 5.1.1 requirement_patent_match 字段表

| 字段名 | 类型 | 约束/说明 |
|---|---|---|
| id | bigint | PK，AUTO_INCREMENT |
| requirement_id | bigint | FK -> requirement.id |
| patent_category | varchar(32) | NOT NULL |
| patent_public_num | varchar(32) | NOT NULL |
| match_score | decimal(6,3) | 匹配分数（可选） |
| match_reason | text | 匹配原因（可选） |
| created_at | datetime(3) | 创建时间 |

### 5.2 `requirement_expert_match`（需求-专家匹配）

- `id` bigint PK
- `requirement_id` bigint FK -> `requirement.id`
- `expert_id` bigint FK -> `expert.id`
- `match_score` decimal(6,3)（可选）
- `match_reason` text（可选）
- UNIQUE：`(requirement_id, expert_id)`

#### 5.2.1 requirement_expert_match 字段表

| 字段名 | 类型 | 约束/说明 |
|---|---|---|
| id | bigint | PK，AUTO_INCREMENT |
| requirement_id | bigint | FK -> requirement.id |
| expert_id | bigint | FK -> expert.id |
| match_score | decimal(6,3) | 匹配分数（可选） |
| match_reason | text | 匹配原因（可选） |
| created_at | datetime(3) | 创建时间 |

## 6. 专利价值评估与预测

### 6.1 `patent_valuation_report`（评估报告）

每次评估产生一条报告记录，用于展示技术价值、市场价值、转化潜力与价值预测。

- `id` bigint PK
- `patent_category` varchar(32)
- `patent_public_num` varchar(32)
- `tech_value_score` / `market_value_score` / `transformation_potential_score` / `overall_score`（decimal(6,3) 可选）
- `predicted_value` decimal(18,2)（可选）
- `report_json` json（结构化报告，可选）
- `model_version` varchar(64)（可选）
- `created_at`

#### 6.1.1 patent_valuation_report 字段表

| 字段名 | 类型 | 约束/说明 |
|---|---|---|
| id | bigint | PK，AUTO_INCREMENT |
| patent_category | varchar(32) | NOT NULL |
| patent_public_num | varchar(32) | NOT NULL |
| tech_value_score | decimal(6,3) | 技术价值（可选） |
| market_value_score | decimal(6,3) | 市场价值（可选） |
| transformation_potential_score | decimal(6,3) | 转化潜力（可选） |
| overall_score | decimal(6,3) | 综合评分（可选） |
| predicted_value | decimal(18,2) | 价值预测（可选） |
| report_json | json | 报告详情（可选） |
| model_version | varchar(64) | 模型版本（可选） |
| created_at | datetime(3) | 创建时间 |

### 6.2 `patent_valuation_model_param`（评估模型参数）

用于管理员维护评估模型的参数配置。

- `id` bigint PK
- `param_key` varchar(128) UNIQUE
- `param_value` text
- `updated_by` bigint FK -> `user_account.id`
- `updated_at`

#### 6.2.1 patent_valuation_model_param 字段表

| 字段名 | 类型 | 约束/说明 |
|---|---|---|
| id | bigint | PK，AUTO_INCREMENT |
| param_key | varchar(128) | UNIQUE，参数键 |
| param_value | text | 参数值（可选） |
| updated_by | bigint | FK -> user_account.id（可选） |
| updated_at | datetime(3) | 更新时间 |

## 7. 转化成果（案例/记录）

### `transformation_result`（转化成果）

展示专利转化成功案例（合作方、效益等），并与需求/专家可选关联。

- `id` bigint PK
- `patent_category` varchar(32)
- `patent_public_num` varchar(32)
- `expert_id` bigint FK -> `expert.id`（可选）
- `requirement_id` bigint FK -> `requirement.id`（可选）
- `partner_org_id` bigint FK -> `organization.id`（合作方，可选）
- `description` varchar(1000)
- `transformation_date` date
- `status` varchar(64)
- `benefit_amount` decimal(18,2)（可选）
- `created_at`

#### 7.1 transformation_result 字段表

| 字段名 | 类型 | 约束/说明 |
|---|---|---|
| id | bigint | PK，AUTO_INCREMENT |
| patent_category | varchar(32) | NOT NULL |
| patent_public_num | varchar(32) | NOT NULL |
| expert_id | bigint | FK -> expert.id（可选） |
| requirement_id | bigint | FK -> requirement.id（可选） |
| partner_org_id | bigint | FK -> organization.id（可选） |
| description | varchar(1000) | 描述（可选） |
| transformation_date | date | 转化日期（可选） |
| status | varchar(64) | 状态（可选） |
| benefit_amount | decimal(18,2) | 效益金额（可选） |
| created_at | datetime(3) | 创建时间 |

## 8. 运营支撑：通知、同步、审计

### 8.1 `user_notification`（消息通知/推送）

用于需求匹配推送、成果方/需求方通知等。

- `id` bigint PK
- `user_id` bigint FK -> `user_account.id`
- `type` varchar(64)
- `title` varchar(255)
- `content` text
- `related_requirement_id` bigint FK -> `requirement.id`（可选）
- `read_flag` tinyint(1) NOT NULL DEFAULT 0
- `created_at`

#### 8.1.1 user_notification 字段表

| 字段名 | 类型 | 约束/说明 |
|---|---|---|
| id | bigint | PK，AUTO_INCREMENT |
| user_id | bigint | FK -> user_account.id |
| type | varchar(64) | NOT NULL |
| title | varchar(255) | 标题（可选） |
| content | text | 内容（可选） |
| related_requirement_id | bigint | FK -> requirement.id（可选） |
| read_flag | tinyint(1) | NOT NULL DEFAULT 0 |
| created_at | datetime(3) | 创建时间 |

### 8.2 `data_sync_job`（数据同步任务）

用于管理员上传/同步专利库、专家库基础数据的任务记录。

- `id` bigint PK
- `job_type` varchar(64)
- `target_category` varchar(32)（针对专利分表可选）
- `source` varchar(255)（来源可选）
- `status` varchar(32) NOT NULL DEFAULT 'PENDING'
- `message` text
- `started_at` / `finished_at`
- `created_by` bigint FK -> `user_account.id`
- `created_at`

#### 8.2.1 data_sync_job 字段表

| 字段名 | 类型 | 约束/说明 |
|---|---|---|
| id | bigint | PK，AUTO_INCREMENT |
| job_type | varchar(64) | NOT NULL |
| target_category | varchar(32) | 目标分表类别（可选） |
| source | varchar(255) | 来源（可选） |
| status | varchar(32) | NOT NULL DEFAULT 'PENDING' |
| message | text | 任务信息（可选） |
| started_at | datetime(3) | 开始时间（可选） |
| finished_at | datetime(3) | 结束时间（可选） |
| created_by | bigint | FK -> user_account.id（可选） |
| created_at | datetime(3) | 创建时间 |

### 8.3 `audit_log`（审计日志）

用于关键操作留痕（权限管控、数据维护等）。

- `id` bigint PK
- `user_id` bigint FK -> `user_account.id`（可选）
- `action` varchar(128)
- `resource_type` varchar(64)（可选）
- `resource_id` varchar(128)（可选）
- `detail` json（可选）
- `created_at`

#### 8.3.1 audit_log 字段表

| 字段名 | 类型 | 约束/说明 |
|---|---|---|
| id | bigint | PK，AUTO_INCREMENT |
| user_id | bigint | FK -> user_account.id（可选） |
| action | varchar(128) | NOT NULL |
| resource_type | varchar(64) | 资源类型（可选） |
| resource_id | varchar(128) | 资源ID（可选） |
| detail | json | 结构化详情（可选） |
| created_at | datetime(3) | 创建时间 |

## 9. AI 聊天会话（持久化多轮对话）

用于支持“下次登录继续聊天”的体验：

- `chat_session`：会话元信息（归属用户、标题、更新时间）
- `chat_message`：会话消息流水（role + content + created_at），用于前端展示聊天记录，同时作为 AI 继续对话的上下文

> 建表 SQL：`docs/database_schema_ai_chat.sql`

### 9.1 `chat_session`（会话）

- `id` bigint PK
- `user_id` bigint（逻辑关联 `user_account.id`）
- `title` varchar(255)（可选，会话标题）
- `created_at` / `updated_at`

#### 9.1.1 chat_session 字段表

| 字段名 | 类型 | 约束/说明 |
|---|---|---|
| id | bigint | PK，AUTO_INCREMENT |
| user_id | bigint | NOT NULL，归属用户 |
| title | varchar(255) | 会话标题（可选） |
| created_at | datetime(3) | 创建时间 |
| updated_at | datetime(3) | 更新时间（自动更新） |

#### 9.1.2 chat_session 索引

| 索引名 | 字段 | 说明 |
|---|---|---|
| idx_chat_session_user_updated | (user_id, updated_at) | 查询用户会话列表（按更新时间倒序） |

### 9.2 `chat_message`（会话消息记录）

- `id` bigint PK
- `session_id` bigint（逻辑关联 `chat_session.id`）
- `user_id` bigint（冗余字段，便于按用户维度检索/审计）
- `role` varchar(16)（user / assistant / system）
- `content` longtext（消息内容）
- `created_at`

#### 9.2.1 chat_message 字段表

| 字段名 | 类型 | 约束/说明 |
|---|---|---|
| id | bigint | PK，AUTO_INCREMENT |
| session_id | bigint | NOT NULL，会话ID |
| user_id | bigint | NOT NULL，归属用户 |
| role | varchar(16) | NOT NULL，user/assistant/system |
| content | longtext | NOT NULL，消息内容 |
| created_at | datetime(3) | 创建时间 |

#### 9.2.2 chat_message 索引

| 索引名 | 字段 | 说明 |
|---|---|---|
| idx_chat_msg_session_created | (session_id, created_at) | 按会话查询消息记录（时间正序/倒序） |
| idx_chat_msg_user_created | (user_id, created_at) | 按用户查询消息记录（可选） |
