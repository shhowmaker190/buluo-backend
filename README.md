# 部落闲聊 · 兴趣部落社区系统（后端）

基于 Spring Boot 3 的社区后端：部落、帖子、评论、私聊、好友、签到等级、内容审核与后台管理。
前端 Vue 3 仓库：[vote-frontend](https://github.com/你的用户名/vote-frontend)；微信小程序端为同一后端的另一个渲染层。

## 技术栈

| 层 | 选型 |
|---|---|
| 框架 | Spring Boot 3.2.5、Spring MVC、Spring Security（JWT 无状态认证） |
| 持久层 | MyBatis-Plus 3.5.9、MySQL 8 |
| 缓存 | Redis（可选，不可用时自动降级为本地缓存） |
| 实时推送 | SSE（`SseEmitter`） |
| 其他 | 线程池异步通知、图片压缩上传、AI 助手多通道降级 |

## 快速开始

### 1. 环境要求

- JDK 17+
- MySQL 8（库名 `vote_db`）
- Redis 6+（可选，不装也能跑）

### 2. 初始化数据库

```bash
mysql -uroot -p < sql/vote_db.sql
```

只建表结构，不含任何业务数据。

### 3. 配置本地密码

数据库密码和 JWT 密钥不进版本库，通过本地配置文件提供：

```bash
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
# 编辑 application-local.yml，填入你自己的 MySQL 密码
```

也可以改用环境变量：`DB_PASSWORD`、`JWT_SECRET`、`REDIS_HOST`、`REDIS_PORT`、`CACHE_REDIS_ENABLED`。

### 4. 启动

```bash
./mvnw spring-boot:run
```

预期输出：`Tomcat started on port 8080`。验证接口：

```bash
curl http://localhost:8080/api/post/list?page=1&size=5
```

返回 `{"code":200,"message":"success","data":{...}}` 即成功。

## 配置说明

| 配置项 | 作用 | 默认 |
|---|---|---|
| `server.port` | 服务端口 | 8080 |
| `spring.datasource.url` | 数据库连接 | `jdbc:mysql://localhost:3306/vote_db` |
| `jwt.secret` / `jwt.expiration` | 签名密钥与有效期 | 24 小时 |
| `app.upload-path` | 上传文件根目录（建议绝对路径） | `uploads` |
| `ai.deepseek.api-key` | 大模型 Key（不配则走免费接口） | 空 |
| `spring.config.import` | 加载 `application-local.yml` | 可选 |

## 目录结构

```
src/main/java/com/vote/
├── controller/   13 个 REST 控制器，只做参数与身份转换
├── service/      业务规则、事务、权限判断、缓存
├── mapper/       MyBatis 接口与手写 SQL
├── entity/       与表一一对应的实体
├── dto/          接口入参/出参
├── config/       Security、JWT、MyBatis、跨域、线程池
├── common/       统一返回 Result
└── exception/    全局异常处理
```

## 几个实现要点

- **内容治理**：发帖、建部落、改资料、改名、举报统一走 0 待审 / 1 通过 / 2 拒绝的状态机，拒绝必带原因并回执通知。
- **并发正确性**：签到、点赞、加好友用唯一索引兜底重复提交，捕获 `DuplicateKeyException` 转友好提示；经验值用 `UPDATE user SET exp = exp + ?` 原子累加。
- **查询性能**：帖子列表的热评用窗口函数 `ROW_NUMBER() OVER (PARTITION BY post_id ...)` 一次取回，把 1 + N×3 次查询降到 2 次。
- **认证**：JWT 里同时写入 `sub`（用户名）和 `uid`，避免用户改名后旧 token 失效；写操作一律在服务端校验数据归属。
- **缓存降级**：Redis 不可用时熔断并回退本地缓存，业务不中断。

## 接口约定

所有接口统一返回：

```json
{ "code": 200, "message": "success", "data": {} }
```

业务失败返回 HTTP 200 + `code: 500` + 可读的 `message`；未登录 401、无权限 403 由 Spring Security 返回。

## 截图

> 截图放在 `docs/screenshots/` 下，然后把下面注释里的表格取消注释。建议放三张：帖子列表、帖子详情、后台审核。

<!--
| 首页 | 后台 |
|---|---|
| ![首页](docs/screenshots/home.png) | ![后台](docs/screenshots/admin.png) |
-->

## 已知不足

1. 没有自动化测试（仅有 `contextLoads`）；
2. 密钥与数据库密码靠本地配置文件，未接入配置中心；
3. 单体应用，无消息队列与对象存储，图片落本地磁盘；
4. 分页仍是 offset，数据量大时需要改游标分页。
