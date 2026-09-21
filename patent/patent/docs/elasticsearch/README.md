# Elasticsearch（IK 分词）部署与接入

## 1. 启动 Elasticsearch（Docker）

在 `docs/elasticsearch` 目录执行：

```bash
docker compose up -d --build
```

默认对外端口：`http://localhost:9200`

如果你构建镜像时报错（常见原因：网络无法访问插件下载地址），可以先不用 IK，直接启动官方 ES 镜像：

```bash
docker compose -f docker-compose-no-ik.yml up -d
```

> 没有 IK 也能用本项目的 ES 检索功能，但中文分词效果会变差；后端会自动降级使用 `standard` analyzer 创建索引。

后端默认不会强制使用 IK 分词（避免在无 IK 环境下创建索引失败）。如你后续成功安装 IK，并希望优先使用 IK，可设置：

- `ES_PREFER_IK=true`

说明：

- 旧的 GitHub releases 链接在部分版本上已不存在（会 404），本仓库的 IK 安装方式已改为使用 `get.infini.cloud` 的下载地址。

如果提示 `docker: 无法将“docker”项识别为...`，说明当前机器未安装 Docker Desktop 或未加入 PATH。

推荐安装方式（Windows）：

1. 安装 Docker Desktop（启用 WSL2）
2. 重启电脑或至少重启终端
3. 在 PowerShell 执行 `docker -v` 与 `docker compose version`，确认能输出版本号
4. 再回到本目录执行上面的 `docker compose up -d --build`

启动成功后可用以下命令验证 ES 是否可访问：

```powershell
iwr http://localhost:9200 -UseBasicParsing
```

如果出现 `基础连接已经关闭: 连接被意外关闭`，通常是 ES 容器还在启动或已崩溃重启。按下面排查：

1) 查看容器状态：

```bash
docker compose -f docker-compose-no-ik.yml ps
```

2) 查看日志（最后 200 行）：

```bash
docker compose -f docker-compose-no-ik.yml logs --tail 200 es
```

常见原因与处理：

- 内存不足：把 `ES_JAVA_OPTS` 调小（本仓库已默认 512m）
- 端口占用：把 `9200:9200` 改成 `9201:9200`，并相应设置 `ES_URIS=http://localhost:9201`

## 1.1 不使用 Docker（本机直接运行 ES）

如果你暂时不想装 Docker，也可以在本机直接运行 Elasticsearch/OpenSearch（无需改后端代码，只要保证 `ES_URIS` 指向可访问的 HTTP 地址即可）。

注意：

- 不同版本 ES 的插件安装方式不同；如果你用 OpenSearch，可改用其自带的中文分析插件或通过 OpenSearch 插件体系安装
- 本项目的“短语/关键词/全文检索”不强依赖 IK，但中文效果会明显变差

## 2. 后端启用 ES

在后端运行环境设置：

- `ES_ENABLED=true`
- `ES_URIS=http://localhost:9200`

可选开关：

- `ES_AUTO_CREATE_INDEX=true`：后端启动/调用接口时若索引不存在则自动创建（默认开启）
- `ES_AUTO_REINDEX=false`：后端启动时自动全量重建索引并同步数据库数据（默认关闭，数据量大时不建议开启）

（可选）索引名：

- `ES_INDEX_PATENT=patents`

## 3. 初始化索引与数据

1) 先登录（拿到 token）

> 说明：当 `ES_AUTO_CREATE_INDEX=true` 时，如果 ES 里还没有索引，后端会自动创建索引结构；但不会自动把数据库数据灌入 ES（除非你开启 `ES_AUTO_REINDEX=true` 或手动调用重建接口）。

2) 重建索引（把数据库专利数据同步到 ES）：

```http
POST /api/patents/es/reindex
Authorization: Bearer <token>
```

3) 搜索（支持关键词/关键语句/全文）：

```http
GET /api/patents/es/search?query=人工智能&category=wind&page=0&size=10
GET /api/patents/es/search?query=一种用于专利匹配的方法&phrase=true&page=0&size=10
```
