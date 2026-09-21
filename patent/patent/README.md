# Patent Backend

## 当前状态

仓库里的前后端代码都还能正常构建，当前最主要的启动阻塞点是后端本地配置丢失后，服务默认使用 `root / 123456` 连接 MySQL，和你这台机器现在的实际数据库账号密码不一致，所以 Spring Boot 在启动阶段直接失败。

已经补上的修复：

- 支持从以下本地文件覆盖运行配置，不再依赖 IDE 里的临时环境变量
- `application-local.properties`
- `config/application-local.properties`
- `src/main/resources/application-secrets.properties`
- 提供了可直接复制填写的示例文件 [config/application-local.properties.example](/d:/patent-all/patent/patent/config/application-local.properties.example)

## 后端启动

1. 复制示例文件，并改成你本机真实配置

可选位置之一：

- [application-local.properties](/d:/patent-all/patent/patent/application-local.properties)
- [config/application-local.properties](/d:/patent-all/patent/patent/config/application-local.properties)
- [application-secrets.properties](/d:/patent-all/patent/patent/src/main/resources/application-secrets.properties)

推荐直接复制：

- 从 [config/application-local.properties.example](/d:/patent-all/patent/patent/config/application-local.properties.example)
- 到 [config/application-local.properties](/d:/patent-all/patent/patent/config/application-local.properties)

2. 确认依赖服务

- MySQL 已启动，并且存在 `patent` 数据库
- Elasticsearch 已启动，默认地址 `http://localhost:9200`

3. 启动命令

```powershell
mvn spring-boot:run
```

或先打包再运行：

```powershell
mvn -DskipTests package
java -jar target/patent-0.0.1-SNAPSHOT.jar
```

## 前端联调

前端已经改回本地代理模式，开发环境默认走：

- HTTP: `/api` -> `http://localhost:8080`
- WebSocket: `/ws/ai-chat` -> `ws://localhost:8080/ws/ai-chat`

前端目录：

- [app](/d:/patent-all/app)

启动命令：

```powershell
npm run dev
```

## 这次排查结论

- 前端之前指向了远程 IP，且 WebSocket 路径和后端实际路径不一致
- 这部分已经修正，重新构建通过
- 后端当前不是编译问题，而是本地数据库凭据不匹配导致无法启动
- 只要把本机真实 MySQL 配置补回本地覆盖文件，项目就会回到一个稳定、可重复启动的状态
