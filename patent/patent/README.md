# Patent Backend（高校知识产权运营服务平台 · 后端）

Spring Boot 服务，为「我的知识产权」平台提供专利检索、需求匹配、价值评估、AI 问答等接口。

## 技术栈

| 项 | 选型 |
| --- | --- |
| 框架 | Spring Boot 3.2.2 / Java 17 |
| 持久化 | Spring Data JPA + MySQL 8（`ddl-auto=none`，schema 手工执行） |
| 检索 | Spring Data Elasticsearch（**可选**，见下） |
| 安全 | Spring Security + JWT，用户端与管理端双 Token |
| 实时通信 | 原生 WebSocket（`/ws/ai-chat`，承载 AI 流式输出） |
| 大模型 | 阿里云百炼 DashScope（OpenAI 兼容模式，DeepSeek V3.2） |

> 项目计划书里规划的 Redis 缓存**当前未实现**，代码未引入 Redis starter。

## 配置

仓库内配置文件**不提供任何可用的密钥默认值**，缺失时一律安全兜底：

| 配置项 | 未配置时的行为 |
| --- | --- |
| `JWT_SECRET` / `ADMIN_JWT_SECRET` | 每次启动生成随机密钥，旧 Token 全部失效 |
| `ADMIN_AUTH_PASSWORD` | 后台登录直接返回 503「后台账号未配置」（fail-closed） |
| `DASHSCOPE_API_KEY` | 调用 AI 接口时抛「未配置」错误 |

真实值请写入本地覆盖文件（已被 `.gitignore` 排除），从模板复制即可：

```bash
cp config/application-local.properties.example config/application-local.properties
```

可选的覆盖位置（优先级从高到低）：

1. `application-local.properties`
2. `config/application-local.properties`
3. `src/main/resources/application-secrets.properties`

**生产环境请全部经环境变量注入**：`DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`JWT_SECRET`、`ADMIN_JWT_SECRET`、`ADMIN_AUTH_PASSWORD`、`DASHSCOPE_API_KEY`、`ES_ENABLED` 等。

## 数据库

MySQL 8，库名 `patent`。按顺序执行 `docs/` 下的脚本：

```bash
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS patent DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"

mysql -uroot -p patent < docs/database_schema.sql   # 28 张表：5 张专利分表 + 用户/组织/专家/需求/匹配/评估/转化/通知/同步/审计/AI 会话
mysql -uroot -p patent < docs/cms_schema.sql        # CMS：home_content、cms_article
```

两个脚本都是 `CREATE TABLE IF NOT EXISTS`，可重复执行。

`src/main/resources/*_upgrade.sql` 是后续增量补丁（审计日志扩展列、需求联系人与预算、评估报告的数据集归属）。注意 `audit_log_upgrade.sql` 用的是 MariaDB 的 `ADD COLUMN IF NOT EXISTS` 语法，**MySQL 8 会报错**，需改写为普通 `ALTER TABLE ... ADD COLUMN`。

## 启动

```bash
mvn spring-boot:run
```

或先打包再运行：

```bash
mvn -DskipTests package
java -jar target/patent-0.0.1-SNAPSHOT.jar --server.port=8080
```

### Elasticsearch 是可选的

本项目**不使用** Spring Data ES 的 Repository 模式，检索通过 `ElasticsearchOperations` 与查询 DSL 手动封装（见 `search/`）。

未部署 ES 时须显式关闭，否则启动时索引初始化会失败：

```bash
java -jar target/patent-0.0.1-SNAPSHOT.jar --search.es.enabled=false
```

关闭后 `/api/patents/es/**` 系列接口不可用，专利检索降级为 MySQL 分表直查：`GET /api/patents?category=wind|solar|biomass|hydrogen|lilon`。

## 目录结构

```
src/main/java/org/ihebut/patent/patent/
├── PatentApplication.java   启动入口（已排除 ES Repository 自动配置）
├── controller/              REST 控制器（/api/**）
├── service/                 业务逻辑、AI 调用、ES 封装
├── mapper/                  Spring Data JPA Repository
├── entity/                  JPA 实体
├── dto/                     请求/响应对象
├── security/                JwtAuthFilter、JwtService（双 Token）
├── search/                  ES 文档、索引管理、启动初始化
├── websocket/               AI 流式对话
└── config/                  其他配置

docs/                        接口文档、数据库 schema、Postman 样例
ai/oss_qa/                   OSS 文档智能问答（RAG）模块
```

## 接口文档与测试

- `docs/api_v7_项目书全量接口文档.md` —— 全量接口说明
- `docs/Postman测试样例_V7_完整流程.md` —— 端到端测试流程
- `postman/` —— Postman 集合与环境变量（V7 测试样例 53 个请求）
- `docs/elasticsearch/` —— ES 索引与查询说明

## 安全须知

- 后台管理接口（`/api/admin/**`）与 `/api/admin/sql/execute`（SQL 控制台）风险较高，仅应在可信内网或加访问控制后开放。
- 生产部署前务必确认 `ADMIN_AUTH_PASSWORD`、`JWT_SECRET`、`ADMIN_JWT_SECRET` 均已设置为强随机值——这三项若沿用早期默认值，管理端 Token 可被伪造。
