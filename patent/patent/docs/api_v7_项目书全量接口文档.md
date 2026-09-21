# 高校知识产权运营服务平台接口文档-V7.0（合并版）

## 0. 说明

### 0.0 请求网址（Base URL）

> 说明：本文档中的“请求路径”均为相对路径，实际请求地址 = 请求网址 + 请求路径。

| 环境 | 请求网址 | 备注 |
| --- | --- | --- |
| 本地(local) | http://localhost:8080 | 默认端口 |
| 测试/生产 | https://<your-domain> | 按部署环境替换 |

### 0.1 统一响应格式

响应数据类型：application/json

响应参数说明：

| 名称    | 类型   | 是否必须 | 默认值 | 备注                  | 其他信息 |
| ------- | ------ | -------- | ------ | --------------------- | -------- |
| code    | number | 必须     |        | 响应码, 0-成功,1-失败 |          |
| message | string | 非必须   |        | 提示信息              |          |
| data    | any    | 非必须   |        | 返回的数据            |          |

响应数据样例：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": null
}
```

### 0.2 认证与授权（JWT）

> 用户登录成功后，系统会下发 JWT 令牌；后续请求在 Header 携带 `Authorization: Bearer <token>`。
>
> 访问需要认证的接口但未登录时，HTTP 状态码为 401。

---

## 1. 认证与用户相关接口

### 1.1 注册

#### 1.1.1 基本信息

> 请求路径：/api/auth/register
>
> 请求方式：POST
>
> 接口描述：注册新用户（写入 user_account），可同时写入 user_profile。

#### 1.1.2 请求参数

请求参数格式：application/json

请求参数说明：

| 参数名称 | 说明   | 类型   | 是否必须 | 备注                     |
| -------- | ------ | ------ | -------- | ------------------------ |
| username | 用户名 | string | 是       | 唯一                     |
| password | 密码   | string | 是       | 服务端保存 password_hash |
| phone    | 手机   | string | 否       |                          |
| email    | 邮箱   | string | 否       |                          |
| nickname | 昵称   | string | 否       | 写入 user_profile        |

请求数据样例：

```json
{
  "username": "zhangsan",
  "password": "123456",
  "phone": "13800000000",
  "email": "zhangsan@example.com",
  "nickname": "张三"
}
```

#### 1.1.3 响应数据

响应数据类型：application/json

响应数据样例：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": {
    "userId": 1
  }
}
```

### 1.2 登录

#### 1.2.1 基本信息

> 请求路径：/api/auth/login
>
> 请求方式：POST
>
> 接口描述：登录成功后返回 JWT token 字符串。

#### 1.2.2 请求参数

请求参数格式：application/json

请求参数说明：

| 参数名称 | 说明   | 类型   | 是否必须 | 备注 |
| -------- | ------ | ------ | -------- | ---- |
| username | 用户名 | string | 是       |      |
| password | 密码   | string | 是       |      |

请求数据样例：

```json
{
  "username": "zhangsan",
  "password": "123456"
}
```

#### 1.2.3 响应数据

响应数据类型：application/json

响应参数说明：

| 名称 | 类型   | 是否必须 | 默认值 | 备注               | 其他信息 |
| ---- | ------ | -------- | ------ | ------------------ | -------- |
| data | string | 必须     |        | 返回的数据,jwt令牌 |          |

响应数据样例：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": "jwt-token-string"
}
```

#### 1.2.4 备注说明

> 用户登录成功后，客户端需在后续每次请求 Header 中携带：Authorization: Bearer <token>。

### 1.3 获取当前用户信息

#### 1.3.1 基本信息

> 请求路径：/api/users/me
>
> 请求方式：GET
>
> 接口描述：返回 user_account + user_profile + expert_profile(如有) + 主机构(如有)。
>
> 认证要求：需要登录

#### 1.3.2 请求参数

无

#### 1.3.3 响应数据

响应数据类型：application/json

响应数据样例（字段可能因数据存在与否而为 null）：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": {
    "user": {
      "id": 1,
      "username": "zhangsan",
      "phone": "13800000000",
      "email": "zhangsan@example.com"
    },
    "profile": {
      "userId": 1,
      "nickname": "张三",
      "avatarUrl": null,
      "realName": null,
      "idNumber": null
    },
    "expertProfile": null,
    "primaryOrganization": null
  }
}
```

### 1.4 更新用户资料

#### 1.4.1 基本信息

> 请求路径：/api/users/me/profile
>
> 请求方式：PUT
>
> 接口描述：更新 user_profile（昵称、头像、实名信息等）。
>
> 认证要求：需要登录

#### 1.4.2 请求参数

请求参数格式：application/json

请求参数说明：

| 参数名称  | 说明    | 类型   | 是否必须 | 备注 |
| --------- | ------- | ------ | -------- | ---- |
| nickname  | 昵称    | string | 否       |      |
| avatarUrl | 头像URL | string | 否       |      |
| realName  | 实名    | string | 否       |      |
| idNumber  | 证件号  | string | 否       |      |

请求数据样例：

```json
{
  "nickname": "新昵称",
  "avatarUrl": "https://example.com/avatar.png",
  "realName": "张三",
  "idNumber": "130***********1234"
}
```

#### 1.4.3 响应数据

响应数据类型：application/json

响应数据样例：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": {
    "userId": 1,
    "nickname": "新昵称"
  }
}
```

---

## 2. 公共专利库接口（多表）

### 2.1 查询专利（按类别）

#### 2.1.1 基本信息

> 请求路径：/api/patents
>
> 请求方式：GET
>
> 接口描述：按 category 指定分表查询专利；query 为空返回该分表分页数据，query 不为空按标题/摘要/申请人/发明人查询。

#### 2.1.2 请求参数

请求参数格式：queryString

请求参数说明：

| 参数名称 | 说明       | 类型   | 是否必须 | 备注                              |
| -------- | ---------- | ------ | -------- | --------------------------------- |
| category | 专利类别   | string | 是       | wind/solar/biomass/hydrogen/lilon |
| query    | 查询关键字 | string | 否       | 为空返回该类别分页列表            |
| page     | 页码       | number | 否       | 默认 0                            |
| size     | 每页条数   | number | 否       | 默认 10                           |

请求数据样例：

```shell
GET /api/patents?category=wind&query=人工智能&page=0&size=10
GET /api/patents?category=wind&page=0&size=10
```

#### 2.1.3 响应数据

响应数据类型：application/json

响应数据样例（data 为分页对象）：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": {
    "content": [
      {
        "publicNum": "CN123456789A",
        "title": "一种用于……的方法",
        "abstractText": "本发明公开了……",
        "applicant": "某某科技有限公司",
        "inventor": "张三",
        "ipc": "G06F 16/00"
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "number": 0,
    "size": 10
  }
}
```

#### 2.1.4 Elasticsearch 语义检索（可选增强）

>  

1.**ES 搜索**

> 请求路径：/api/patents/es/search
>
> 请求方式：GET
>
> 接口描述：基于 ES 的多字段检索（title/abstract/applicant/inventor/patent_details/ipc/cpc/nec/public_num）。
>
> 认证要求：无需登录
>
> 开关要求：`ES_ENABLED=true`，并配置 `ES_URIS`

请求参数格式：queryString

| 参数名称 | 说明 | 类型 | 是否必须 | 备注 |
| --- | --- | --- | --- | --- |
| query | 查询内容 | string | 是 | 关键词或关键语句 |
| category | 专利类别 | string | 否 | 为空则跨类别检索 |
| phrase | 短语匹配 | boolean | 否 | true=按短语（关键语句）匹配 |
| page | 页码 | number | 否 | 默认 0 |
| size | 每页条数 | number | 否 | 默认 10，最大 50 |

响应说明：

{
    "code": 0,
    "message": "操作成功",
    "data": {
        "total": 510,
        "hits": [
            {
                "category": "wind",
                "publicNum": "CN106991538A",
                "title": "一种基于风电机组部件重要度评价的维修方法及装置",
                "abstractText": "本发明提供了一种基于风电机组部件重要度评价的维修方法及装置，包括：通过划分部件的重要等级和评价的因素集，依据得到的因素集中每个因素对于重要等级中每个等级的重要程度，以及得到的因素集中包含的信息的充分程度，计算区间灰色模糊矩阵；依据区间层级分析法，计算每一个因素针对于风电机组部件重要度评价的权重，确定出因素集的权重矩阵；在OWA算子的基础上，依据区间数灰色模糊矩阵和因素集的权重矩阵，确定风电机组部件所属的部件重要等级，并依据风电机组部件所属的部件重要等级确定相应的维修策略。充分考虑了评价因素隶属于评价等级的程度以及信息的充分程度对重要度评价的影响，提高了对风电机组部件重要度评价的准确性。",
                "applicant": "华北电力大学(保定)",
                "inventor": "刘华新; 苑一鸣; 周沛; 韩中合",
                "score": 39.273407,
                "highlightTitle": "一种基于风电机组部件<em>重要</em>度评价<em>的</em>维修方法及装置",
                "highlightAbstractText": "本发明提供了一种基于风电机组部件<em>重要</em>度评价<em>的</em>维修方法及装置，包括：通过划分部件<em>的</em><em>重要</em>等级和评价<em>的</em>因素集，依据得到<em>的</em>因素集中每个因素对于<em>重要</em>等级中每个等级<em>的</em><em>重要</em>程度，以及得到<em>的</em>因素集中包含<em>的</em>信息<em>的</em>充分程度 ... ，计算区间灰色模糊矩阵；依据区间层级分析法，计算每一个因素针对于风电机组部件<em>重要</em>度评价<em>的</em>权重，确定出因素集<em>的</em>权重矩阵；在OWA算子<em>的</em>基础上，依据区间数灰色模糊矩阵和因素集<em>的</em>权重矩阵，确定风电机组部件所属<em>的</em>部件<em>重要</em>等级 ... ，并依据风电机组部件所属<em>的</em>部件<em>重要</em>等级确定相应<em>的</em>维修策略。 ... 充分考虑了评价因素隶属于评价等级<em>的</em>程度以及信息<em>的</em>充分程度对<em>重要</em>度评价<em>的</em>影响，提高了对风电机组部件<em>重要</em>度评价<em>的</em>准确性。",
                "highlightPatentDetails": null
            }
        ]
    }
}

#### 2.2.1 基本信息

> 请求路径：/api/patents
>
> 请求方式：POST
>
> 接口描述：向 category 对应分表写入专利数据；publicNum 为主键，重复会覆盖更新。

#### 2.2.2 请求参数

请求参数格式：queryString + application/json

请求参数说明（QueryString）：

| 参数名称 | 说明     | 类型   | 是否必须 | 备注                              |
| -------- | -------- | ------ | -------- | --------------------------------- |
| category | 专利类别 | string | 是       | wind/solar/biomass/hydrogen/lilon |

请求参数说明（Body，字段与 PatentUpsertRequest 对齐，以下为常用字段）：

| 参数名称     | 说明   | 类型   | 是否必须 | 备注 |
| ------------ | ------ | ------ | -------- | ---- |
| publicNum    | 公开号 | string | 是       | 主键 |
| title        | 标题   | string | 否       |      |
| abstractText | 摘要   | string | 否       |      |
| applicant    | 申请人 | string | 否       |      |
| inventor     | 发明人 | string | 否       |      |
| ipc          | IPC    | string | 否       |      |
| cpc          | CPC    | string | 否       |      |
| nec          | NEC    | string | 否       |      |

请求数据样例：

```shell
POST /api/patents?category=wind
```

```json
{
  "publicNum": "CN123456789A",
  "title": "一种用于专利匹配的方法",
  "abstractText": "本发明公开了……",
  "ipc": "G06F 16/00",
  "inventor": "张三",
  "applicant": "某某科技有限公司"
}
```

#### 2.2.3 响应数据

响应数据类型：application/json

响应数据样例：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": {
    "publicNum": "CN123456789A",
    "title": "一种用于专利匹配的方法"
  }
}
```

### 2.3 获取单条专利（按类别 + 公开号）

#### 2.3.1 基本信息

> 请求路径：/api/patents/{category}/{publicNum}
>
> 请求方式：GET
>
> 接口描述：根据 category + publicNum 获取单条专利记录。

#### 2.3.2 请求参数

请求参数格式：path

请求参数说明：

| 参数名称  | 说明     | 类型   | 是否必须 | 备注                              |
| --------- | -------- | ------ | -------- | --------------------------------- |
| category  | 专利类别 | string | 是       | wind/solar/biomass/hydrogen/lilon |
| publicNum | 公开号   | string | 是       |                                   |

请求数据样例：

```shell
GET /api/patents/wind/CN123456789A
```

#### 2.3.3 响应数据

响应数据类型：application/json

响应数据样例：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": {
    "publicNum": "CN123456789A",
    "title": "一种用于……的方法"
  }
}
```

---

## 3. 个人专利管理接口

### 3.1 上传个人专利

#### 3.1.1 基本信息

> 请求路径：/api/user-patents
>
> 请求方式：POST
>
> 接口描述：上传个人专利记录（ownerUserId 自动取当前登录用户）。
>
> 认证要求：需要登录

#### 3.1.2 请求参数

请求参数格式：application/json

请求参数说明（字段与 UserPatentUpsertRequest 对齐，以下为常用字段）：

| 参数名称      | 说明      | 类型   | 是否必须 | 备注                         |
| ------------- | --------- | ------ | -------- | ---------------------------- |
| category      | 专利类别  | string | 是       |                              |
| title         | 标题      | string | 是       |                              |
| publicNum     | 公开号    | string | 否       |                              |
| abstractText  | 摘要      | string | 否       |                              |
| applicant     | 申请人    | string | 否       |                              |
| inventor      | 发明人    | string | 否       |                              |
| patentDetails | 详情/全文 | string | 否       |                              |
| visibility    | 可见性    | string | 否       | PUBLIC/PRIVATE，默认 PUBLIC |

请求数据样例：

```json
{
  "category": "solar",
  "title": "高效太阳能板",
  "publicNum": "CN987654",
  "visibility": "PUBLIC",
  "abstractText": "……"
}
```

#### 3.1.3 响应数据

响应数据类型：application/json

响应数据样例：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": {
    "id": 1,
    "ownerUserId": 1001,
    "category": "solar",
    "title": "高效太阳能板",
    "visibility": "PUBLIC"
  }
}
```

### 3.2 查看个人专利列表

#### 3.2.1 基本信息

> 请求路径：/api/user-patents
>
> 请求方式：GET
>
> 接口描述：owner=me 查看“我的专利”；不传 owner 查看所有公开个人专利。
>
> 认证要求：需要登录

#### 3.2.2 请求参数

请求参数格式：queryString

| 参数名称 | 说明 | 类型 | 是否必须 | 备注 |
| --- | --- | --- | --- | --- |
| query | 查询关键字 | string | 否 | 模糊匹配标题或关键词 |
| category | 专利类别/技术方向 | string | 否 | wind/solar/biomass/hydrogen/lilon |
| mine | 是否仅本人 | boolean | 否 | true=仅本人 |
| page | 页码 | number | 否 | 默认0 |
| size | 页大小 | number | 否 | 默认10 |

#### 3.2.3 响应数据

响应数据样例：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": {
    "content": [
      {
        "id": 1,
        "title": "需求标题",
        "description": "需求描述",
        "keywords": "关键词1,关键词2",
        "techDirection": "wind",
        "requesterUserId": 101,
        "createdDate": "2026-02-10T12:00:00"
      }
    ],
    "pageable": { ... },
    "totalElements": 1,
    "totalPages": 1
  }
}
```

### 3.3 查看个人专利详情

#### 3.3.1 基本信息

> 请求路径：/api/user-patents/{id}
>
> 请求方式：GET
>
> 接口描述：本人可查看自己的全部；非本人仅可查看 PUBLIC。
>
> 认证要求：需要登录

#### 3.3.2 请求参数

请求参数格式：path

| 参数名称 | 说明 | 类型   | 是否必须 | 备注 |
| -------- | ---- | ------ | -------- | ---- |
| id       | ID   | number | 是       |      |

#### 3.3.3 响应数据

响应数据类型：application/json

### 3.4 修改个人专利

#### 3.4.1 基本信息

> 请求路径：/api/user-patents/{id}
>
> 请求方式：PUT
>
> 接口描述：仅本人可修改。
>
> 认证要求：需要登录

#### 3.4.2 请求参数

请求参数格式：application/json

请求参数说明：与“上传个人专利”相同，按需传字段即可。

#### 3.4.3 响应数据

响应数据类型：application/json

### 3.5 删除个人专利

#### 3.5.1 基本信息

> 请求路径：/api/user-patents/{id}
>
> 请求方式：DELETE
>
> 接口描述：仅本人可删除。
>
> 认证要求：需要登录

#### 3.5.2 请求参数

请求参数格式：path

| 参数名称 | 说明 | 类型   | 是否必须 | 备注 |
| -------- | ---- | ------ | -------- | ---- |
| id       | ID   | number | 是       |      |

#### 3.5.3 响应数据

响应数据类型：application/json

响应数据样例：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": null
}
```

---

## 4. 专家相关接口

### 4.1 查询专家

#### 4.1.1 基本信息

> 请求路径：/api/experts
>
> 请求方式：GET
>
> 接口描述：query 为空返回所有已通过认证(APPROVED)专家；query 不为空按真实姓名/领域/专长模糊查询。

#### 4.1.2 请求参数

请求参数格式：queryString

| 参数名称 | 说明       | 类型   | 是否必须 | 备注 |
| -------- | ---------- | ------ | -------- | ---- |
| query    | 查询关键字 | string | 否       |      |

#### 4.1.3 响应数据

响应数据类型：application/json

响应数据样例（返回 ExpertProfile 列表）：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": [
    {
      "userId": 1,
      "field": "人工智能",
      "expertise": "知识图谱",
      "contactInfo": "13800000000",
      "certStatus": "APPROVED"
    }
  ]
}
```

### 4.2 申请/更新专家资料（提交认证）

#### 4.2.1 基本信息

> 请求路径：/api/experts/me
>
> 请求方式：PUT
>
> 接口描述：当前登录用户提交/更新专家资料，状态会设置为 PENDING。
>
> 认证要求：需要登录

#### 4.2.2 请求参数

请求参数格式：application/json

| 参数名称     | 说明     | 类型   | 是否必须 | 备注 |
| ------------ | -------- | ------ | -------- | ---- |
| field        | 领域     | string | 否       |      |
| expertise    | 专长     | string | 否       |      |
| achievements | 成果     | string | 否       |      |
| contactInfo  | 联系方式 | string | 否       |      |

#### 4.2.3 响应数据

响应数据类型：application/json

### 4.3 审核专家（管理员）

#### 4.3.1 基本信息

> 请求路径：/api/admin/experts/{userId}/audit
>
> 请求方式：POST
>
> 接口描述：审核专家资料（APPROVED/REJECTED/PENDING）。
>
> 认证要求：需要登录

#### 4.3.2 请求参数

请求参数格式：path + application/json

Path 参数：

| 参数名称 | 说明   | 类型   | 是否必须 | 备注 |
| -------- | ------ | ------ | -------- | ---- |
| userId   | 用户ID | number | 是       |      |

Body 参数：

| 参数名称  | 说明     | 类型   | 是否必须 | 备注 |
| --------- | -------- | ------ | -------- | ---- |
| certStatus| 审核结果 | string | 是       | APPROVED/REJECTED/PENDING |

请求数据样例：

```json
{
  "certStatus": "APPROVED"
}
```

---

## 5. 需求与匹配相关接口

### 5.1 创建需求

#### 5.1.1 基本信息

> 请求路径：/api/requirements
>
> 请求方式：POST
>
> 接口描述：发布一条需求记录。
>
> 认证要求：需要登录

#### 5.1.2 请求参数

请求参数格式：application/json

| 参数名称        | 说明         | 类型   | 是否必须 | 备注 |
| --------------- | ------------ | ------ | -------- | ---- |
| title           | 标题         | string | 是       |      |
| description     | 详情         | string | 否       |      |
| keywords        | 关键词       | string | 否       | 用于匹配 |
| techDirection   | 技术方向     | string | 否       |      |
| cooperationMode | 合作模式     | string | 否       |      |
| requesterOrgId  | 需求方机构ID | number | 否       |      |

请求数据样例：

```json
{
  "title": "设备预测性维护",
  "keywords": "预测性维护,异常检测",
  "techDirection": "时序异常检测",
  "cooperationMode": "技术转让"
}
```

#### 5.1.3 响应数据

响应数据类型：application/json

### 5.2 需求匹配专利

#### 5.2.1 基本信息

> 请求路径：/api/requirements/{id}/match-patents
>
> 请求方式：GET
>
> 接口描述：基于需求 keywords 或 title 在 5 张公共专利表 + 用户专利中检索匹配。
>
> 认证要求：需要登录

#### 5.2.2 请求参数

请求参数格式：path

| 参数名称 | 说明   | 类型   | 是否必须 | 备注 |
| -------- | ------ | ------ | -------- | ---- |
| id       | 需求ID | number | 是       |      |

#### 5.2.3 响应数据

响应数据类型：application/json

响应数据样例（返回 PatentMatchResult 列表）：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": [
    {
      "patentSource": "EXTERNAL",
      "category": "wind",
      "publicNum": "CN123456789A",
      "userPatentId": null,
      "title": "一种用于……的方法",
      "applicant": "某某科技有限公司",
      "inventor": "张三"
    }
  ]
}
```

### 5.3 需求匹配专家

#### 5.3.1 基本信息

> 请求路径：/api/requirements/{id}/match-experts
>
> 请求方式：GET
>
> 接口描述：在已通过认证的专家资料中按关键词匹配。
>
> 认证要求：需要登录

#### 5.3.2 请求参数

请求参数格式：path

| 参数名称 | 说明   | 类型   | 是否必须 | 备注 |
| -------- | ------ | ------ | -------- | ---- |
| id       | 需求ID | number | 是       |      |

#### 5.3.3 响应数据

响应数据类型：application/json

### 5.4 保存专利匹配结果

#### 5.4.1 基本信息

> 请求路径：/api/requirements/{id}/match-patents/persist
>
> 请求方式：POST
>
> 接口描述：将匹配到的专利结果写入 requirement_patent_match。
>
> 认证要求：需要登录

#### 5.4.2 请求参数

请求参数格式：path + application/json

Path 参数：

| 参数名称 | 说明   | 类型   | 是否必须 | 备注 |
| -------- | ------ | ------ | -------- | ---- |
| id       | 需求ID | number | 是       |      |

Body 参数：

| 参数名称          | 说明       | 类型   | 是否必须 | 备注                              |
| ----------------- | ---------- | ------ | -------- | --------------------------------- |
| items             | 匹配项列表 | array  | 是       |                                   |
| \|-patentCategory  | 专利类别   | string | 是       | wind/solar/biomass/hydrogen/lilon |
| \|-patentPublicNum | 公开号     | string | 是       |                                   |
| \|-matchScore      | 分数       | number | 否       |                                   |
| \|-matchReason     | 原因       | string | 否       |                                   |

请求数据样例：

```json
{
  "items": [
    {
      "patentCategory": "wind",
      "patentPublicNum": "CN123456789A",
      "matchScore": 0.912,
      "matchReason": "关键词命中"
    }
  ]
}
```

#### 5.4.3 响应数据

响应数据类型：application/json

### 5.5 保存专家匹配结果

#### 5.5.1 基本信息

> 请求路径：/api/requirements/{id}/match-experts/persist
>
> 请求方式：POST
>
> 接口描述：将匹配到的专家结果写入 requirement_expert_match。
>
> 认证要求：需要登录

#### 5.5.2 请求参数

请求参数格式：path + application/json

请求数据样例：

```json
{
  "items": [
    {
      "expertId": 1,
      "matchScore": 0.855,
      "matchReason": "expertise 匹配"
    }
  ]
}
```

---

## 6. 专利转化成果接口

### 6.1 查询全部成果

#### 6.1.1 基本信息

> 请求路径：/api/transformations
>
> 请求方式：GET
>
> 接口描述：查询全部转化成果记录（transformation_result）。

#### 6.1.2 请求参数

无

#### 6.1.3 响应数据

响应数据类型：application/json

### 6.2 创建成果记录

#### 6.2.1 基本信息

> 请求路径：/api/transformations
>
> 请求方式：POST
>
> 接口描述：新增一条转化成果记录。

#### 6.2.2 请求参数

请求参数格式：application/json

请求参数说明：字段与 TransformationResult 对齐。

#### 6.2.3 响应数据

响应数据类型：application/json

---

## 7. 专利价值评估接口

### 7.1 生成评估报告

#### 7.1.1 基本信息

> 请求路径：/api/valuations
>
> 请求方式：POST
>
> 接口描述：对指定专利生成价值评估报告并保存（写入 patent_valuation_report）。

#### 7.1.2 请求参数

请求参数格式：application/json

请求参数说明：

| 参数名称        | 说明       | 类型   | 是否必须 | 备注 |
| --------------- | ---------- | ------ | -------- | ---- |
| patentSource    | 专利来源   | string | 是       | EXTERNAL/USER |
| patentCategory  | 专利类别   | string | 否       | source=EXTERNAL 必填 |
| patentPublicNum | 公开号     | string | 否       | source=EXTERNAL 必填 |
| userPatentId    | 用户专利ID | number | 否       | source=USER 必填 |
| modelVersion    | 模型版本   | string | 否       |      |

#### 7.1.3 响应数据

响应数据类型：application/json

### 7.2 查询评估报告列表

#### 7.2.1 基本信息

> 请求路径：/api/valuations
>
> 请求方式：GET
>
> 接口描述：按专利定位信息查询报告列表。

#### 7.2.2 请求参数

请求参数格式：queryString

| 参数名称        | 说明       | 类型   | 是否必须 | 备注 |
| --------------- | ---------- | ------ | -------- | ---- |
| patentSource    | 专利来源   | string | 是       | EXTERNAL/USER |
| patentCategory  | 专利类别   | string | 否       | source=EXTERNAL 必填 |
| patentPublicNum | 公开号     | string | 否       | source=EXTERNAL 必填 |
| userPatentId    | 用户专利ID | number | 否       | source=USER 必填 |

### 7.3 维护评估模型参数

#### 7.3.1 基本信息

> 请求路径：/api/valuation-params/{key}
>
> 请求方式：PUT
>
> 接口描述：更新评估模型参数（写入 patent_valuation_model_param）。

#### 7.3.2 请求参数

请求参数格式：path + application/json

Body 参数：

| 参数名称    | 说明     | 类型   | 是否必须 | 备注 |
| ----------- | -------- | ------ | -------- | ---- |
| paramValue  | 参数值   | string | 否       |      |
| description | 参数描述 | string | 否       |      |

---

## 8. 机构相关接口

### 8.1 创建机构

#### 8.1.1 基本信息

> 请求路径：/api/organizations
>
> 请求方式：POST
>
> 接口描述：创建机构记录。

#### 8.1.2 请求参数

请求参数格式：application/json

请求参数说明：

| 参数名称     | 说明         | 类型   | 是否必须 | 备注 |
| ------------ | ------------ | ------ | -------- | ---- |
| name         | 机构名称     | string | 是       |      |
| type         | 机构类型     | string | 是       |      |
| creditCode   | 统一信用代码 | string | 否       |      |
| address      | 地址         | string | 否       |      |
| contactName  | 联系人       | string | 否       |      |
| contactPhone | 联系电话     | string | 否       |      |
| contactEmail | 联系邮箱     | string | 否       |      |

#### 8.1.3 响应数据

响应数据类型：application/json

### 8.2 查询机构

#### 8.2.1 基本信息

> 请求路径：/api/organizations
>
> 请求方式：GET
>
> 接口描述：按名称关键字与类型筛选机构。

#### 8.2.2 请求参数

请求参数格式：queryString

| 参数名称 | 说明       | 类型   | 是否必须 | 备注 |
| -------- | ---------- | ------ | -------- | ---- |
| query    | 名称关键字 | string | 否       |      |
| type     | 机构类型   | string | 否       |      |

#### 8.2.3 响应数据

响应数据类型：application/json

### 8.3 加入/设置机构成员关系

#### 8.3.1 基本信息

> 请求路径：/api/organizations/{orgId}/members
>
> 请求方式：POST
>
> 接口描述：当前用户加入机构或更新与机构的关系（可设置主机构）。

#### 8.3.2 请求参数

请求参数格式：path + application/json

Path 参数：

| 参数名称 | 说明   | 类型   | 是否必须 | 备注 |
| -------- | ------ | ------ | -------- | ---- |
| orgId    | 机构ID | number | 是       |      |

Body 参数：

| 参数名称      | 说明       | 类型    | 是否必须 | 备注 |
| ------------- | ---------- | ------- | -------- | ---- |
| relationType  | 关系类型   | string  | 否       |      |
| positionTitle | 职位/头衔  | string  | 否       |      |
| isPrimary     | 是否主机构 | boolean | 否       | true/false |

---

## 9. 通知相关接口

### 9.1 查询通知列表

#### 9.1.1 基本信息

> 请求路径：/api/notifications
>
> 请求方式：GET
>
> 接口描述：查询当前用户通知；可按 read 过滤。

#### 9.1.2 请求参数

请求参数格式：queryString

| 参数名称 | 说明         | 类型    | 是否必须 | 备注 |
| -------- | ------------ | ------- | -------- | ---- |
| read     | 是否已读过滤 | boolean | 否       |      |

#### 9.1.3 响应数据

响应数据类型：application/json

### 9.2 创建通知

#### 9.2.1 基本信息

> 请求路径：/api/notifications
>
> 请求方式：POST
>
> 接口描述：创建一条用户通知（用于系统推送/匹配提醒等）。

#### 9.2.2 请求参数

请求参数格式：application/json

| 参数名称             | 说明       | 类型   | 是否必须 | 备注 |
| -------------------- | ---------- | ------ | -------- | ---- |
| userId               | 用户ID     | number | 是       |      |
| type                 | 类型       | string | 是       |      |
| title                | 标题       | string | 否       |      |
| content              | 内容       | string | 否       |      |
| relatedRequirementId | 关联需求ID | number | 否       |      |

### 9.3 标记通知为已读

#### 9.3.1 基本信息

> 请求路径：/api/notifications/{id}/read
>
> 请求方式：PUT
>
> 接口描述：将当前用户的通知标记为已读。

---

## 10. 审计日志接口

### 10.1 查询审计日志

#### 10.1.1 基本信息

> 请求路径：/api/audit-logs
>
> 请求方式：GET
>
> 接口描述：按 userId/action/from/to 过滤查询审计日志。

#### 10.1.2 请求参数

请求参数格式：queryString

| 参数名称 | 说明       | 类型   | 是否必须 | 备注               |
| -------- | ---------- | ------ | -------- | ------------------ |
| userId   | 用户ID     | number | 否       |                    |
| action   | 动作关键字 | string | 否       |                    |
| from     | 起始时间   | string | 否       | LocalDateTime 格式 |
| to       | 结束时间   | string | 否       | LocalDateTime 格式 |

#### 10.1.3 响应数据

响应数据类型：application/json

---

## 11. 数据同步任务接口

### 11.1 创建同步任务

#### 11.1.1 基本信息

> 请求路径：/api/sync-jobs
>
> 请求方式：POST
>
> 接口描述：创建一条数据同步任务记录。

#### 11.1.2 请求参数

请求参数格式：application/json

| 参数名称       | 说明     | 类型   | 是否必须 | 备注 |
| -------------- | -------- | ------ | -------- | ---- |
| jobType        | 任务类型 | string | 是       |      |
| targetCategory | 目标类别 | string | 否       |      |
| source         | 数据来源 | string | 否       |      |

#### 11.1.3 响应数据

响应数据类型：application/json

### 11.2 查询同步任务列表

#### 11.2.1 基本信息

> 请求路径：/api/sync-jobs
>
> 请求方式：GET
>
> 接口描述：mine=true 仅返回本人创建的任务。

#### 11.2.2 请求参数

请求参数格式：queryString

| 参数名称 | 说明     | 类型    | 是否必须 | 备注 |
| -------- | -------- | ------- | -------- | ---- |
| mine     | 是否仅本人 | boolean | 否       |      |

#### 11.2.3 响应数据

响应数据类型：application/json

### 11.3 查询同步任务详情

#### 11.3.1 基本信息

> 请求路径：/api/sync-jobs/{id}
>
> 请求方式：GET
>
> 接口描述：查询任务详情。

#### 11.3.2 请求参数

请求参数格式：path

| 参数名称 | 说明   | 类型   | 是否必须 | 备注 |
| -------- | ------ | ------ | -------- | ---- |
| id       | 任务ID | number | 是       |      |

#### 11.3.3 响应数据

响应数据类型：application/json

---

## 12. AI 中转接口（通义千问）

> 安全说明：前端禁止直连通义千问（API Key 可被抓包泄露），必须通过后端中转。

### 12.1 AI 问答（后端中转）

> 请求路径：/api/ai/chat
>
> 请求方式：POST
>
> 接口描述：前端发送问题到后端；后端携带服务端保存的通义千问 API Key 调用 DashScope，再返回结果给前端。
>
> 认证要求：无需登录（纯中转，不保存会话）

请求参数格式：application/json

| 参数名称 | 说明 | 类型 | 是否必须 | 备注 |
| --- | --- | --- | --- | --- |
| question | 问题内容 | string | 是 | |
| model | 模型 | string | 否 | 默认 qwen-plus |
| temperature | 随机性 | number | 否 | 例如 0.7 |
| maxTokens | 最大输出 | number | 否 | |

请求数据样例：

```json
{
  "question": "帮我总结一下高校知识产权运营平台的核心功能",
  "model": "qwen-plus",
  "temperature": 0.7
}
```

响应数据样例：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": {
    "answer": "......",
    "model": "qwen-plus",
    "requestId": "xxx"
  }
}
```

### 12.2 AI 专利检索（需求/场景驱动）

> 请求路径：/api/ai/chat/patent
>
> 请求方式：POST
>
> 接口描述：用户输入一段“需求描述/应用场景”，后端调用专利检索专用 AI 应用分析并输出相关专利公开号（空格分隔），再通过 ES 批量检索专利详情并返回。
>
> 认证要求：无需登录

请求参数格式：application/json

| 参数名称 | 说明 | 类型 | 是否必须 | 备注 |
| --- | --- | --- | --- | --- |
| requirement | 需求描述/应用场景 | string | 是 | 例如“风力发电机叶片除冰装置” |
| sessionId | 会话ID | string | 否 | 传入后可用于保持多轮对话上下文 |

请求数据样例：

```json
{
  "requirement": "我想找关于风力发电机叶片除冰的技术方案",
  "sessionId": "session_123"
}
```

响应数据样例：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": {
    "requestId": "b6fbdb1f-8ddb-4be2-bc6a-d9719045bd9d",
    "aiAnalysis": "CN1620552A RU2012137235A CN1291683A",
    "extractedPublicNums": [
      "CN1620552A",
      "RU2012137235A",
      "CN1291683A"
    ],
    "notFoundPublicNums": [],
    "patents": [
      {
        "category": "wind",
        "publicNum": "CN1620552A",
        "title": "xxx",
        "abstractText": "xxx"
      }
    ]
  }
}
```

---

## 13. 聊天会话接口（持久化）

> 说明：用于“登录后继续聊天”的场景。后端会把每次提问与回答写入数据库，会话列表/消息记录可供前端展示；提问时会携带历史消息作为上下文让 AI 续写回答。

### 13.1 创建会话

> 请求路径：/api/chat/sessions
>
> 请求方式：POST
>
> 认证要求：需要登录

请求参数格式：application/json（可选）

| 参数名称 | 说明 | 类型 | 是否必须 | 备注 |
| --- | --- | --- | --- | --- |
| title | 会话标题 | string | 否 | |

响应数据样例：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": {
    "id": 1,
    "title": "我的聊天",
    "createdAt": "2026-01-28T00:00:00",
    "updatedAt": "2026-01-28T00:00:00"
  }
}
```

### 13.2 查询会话列表

> 请求路径：/api/chat/sessions
>
> 请求方式：GET
>
> 认证要求：需要登录

### 13.3 查询会话消息记录

> 请求路径：/api/chat/sessions/{sessionId}/messages
>
> 请求方式：GET
>
> 认证要求：需要登录

### 13.4 继续对话（带历史上下文）

> 请求路径：/api/chat/ask
>
> 请求方式：POST
>
> 认证要求：需要登录

请求参数格式：application/json

| 参数名称 | 说明 | 类型 | 是否必须 | 备注 |
| --- | --- | --- | --- | --- |
| sessionId | 会话ID | number | 否 | 不传则自动创建新会话 |
| question | 问题内容 | string | 是 | |
| model | 模型 | string | 否 | 默认 qwen-plus |
| temperature | 随机性 | number | 否 | |
| maxTokens | 最大输出 | number | 否 | |
| historyLimit | 历史条数 | number | 否 | 默认 20，最大 50 |

请求数据样例：

```json
{
  "sessionId": 1,
  "question": "根据我们之前的对话，继续补充细节",
  "model": "qwen-plus",
  "temperature": 0.7,
  "historyLimit": 20
}
```
