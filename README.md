# CampusCircle

CampusCircle 是一个基于学校地理位置的校园社区后端服务，支持学生绑定所属学校，并按距离范围查看附近学校的帖子。项目提供用户认证、个人资料、学校检索、帖子发布、评论互动、点赞、站内通知和后台管理等能力，重点放在业务建模、接口规范、持久层设计、缓存排行榜、事件解耦和集成测试等后端工程实践。

## Features

- 用户注册、登录、退出和 Token 校验
- 用户资料维护、学校绑定、公开主页、用户发帖和评论查询
- 学校搜索、附近学校查询、附近学校帖子 Feed
- 分类、帖子列表、帖子详情、发帖、编辑和删除，发帖自动归属到用户所属学校
- 评论、点赞、取消点赞、点赞状态查询
- 评论/点赞触发站内通知，支持未读数和批量已读
- 管理端隐藏/恢复帖子、禁用/启用用户
- 热门帖子排行榜、浏览量批量刷库、事件发布与消费扩展

## Tech Stack

| 类型 | 技术 |
| --- | --- |
| 语言与框架 | Java 17, Spring Boot 4, Spring Web, Spring Validation |
| 持久层 | MyBatis-Plus, MySQL |
| 缓存与排行 | Redis, ZSet |
| 消息事件 | RocketMQ |
| 安全 | BCrypt password hashing, Bearer Token |
| 工程化 | Docker, Docker Compose |
| 测试 | JUnit, Spring Boot Test, H2 |

## Architecture

```text
Controller -> Service -> Mapper(MyBatis-Plus) -> Database
```

- `Controller`：处理 REST API 请求、参数校验和统一响应。
- `Service`：承载业务规则、登录校验、权限判断、事件发布和排行榜协调。
- `Mapper`：基于 MyBatis-Plus 处理实体映射、单表 CRUD、复杂查询和统计更新。
- `Common / Exception`：提供统一响应结构、错误码、业务异常和全局异常处理。

项目结构：

```text
src/main/java/com/tyj/campuscircle
├── admin       后台管理
├── auth        注册、登录、Token、当前用户识别
├── category    帖子分类
├── comment     评论
├── common      通用响应、分页响应、错误码
├── event       领域事件、RocketMQ 发布和消费
├── exception   业务异常、全局异常处理
├── like        点赞
├── notice      站内通知
├── post        帖子、热门排行榜、浏览量批量刷新
├── school      学校检索、距离计算、附近学校查询
└── user        用户资料
```

## Highlights

- **持久层重构**：使用 MyBatis-Plus 建模用户、帖子、评论、点赞、通知等核心表，通过实体映射承接基础 CRUD，通过注解 SQL 保留复杂列表、详情和统计查询的可读性。
- **位置化社区模型**：抽象学校实体，用户和帖子都关联学校；发帖时自动写入用户所属学校，并支持按半径聚合附近学校帖子，形成区别于普通校园论坛的区域化信息流。
- **热榜缓存设计**：使用 Redis ZSet 维护热门帖子排行，MySQL `post_stat.hot_score` 作为持久化热度来源；缓存为空或过期时支持回源重建，并使用短 TTL、随机抖动和重建锁降低击穿风险。
- **事件解耦通知**：抽象评论/点赞领域事件，默认同步消费，启用 RocketMQ 后使用事务消息投递，通知侧基于 `event_key` 做幂等写入。
- **浏览量削峰**：使用 `ConcurrentHashMap + LongAdder + ScheduledExecutorService` 聚合浏览量增量，定时批量刷库，减少高频浏览场景下的数据库写压力。
- **接口一致性**：统一响应结构、错误码枚举、参数校验、业务异常和全局异常处理，保证正常返回和异常返回格式稳定。
- **集成测试覆盖**：基于 H2 内存数据库覆盖注册登录、分类、发帖、评论、点赞、通知和权限边界流程。

## API

核心接口清单见 [docs/API.md](docs/API.md)，包括认证、用户、学校、帖子、评论、点赞、通知和后台管理接口。

常见请求示例：

```http
POST /api/auth/register
Content-Type: application/json
```

```json
{
  "username": "alice",
  "password": "123456",
  "nickname": "Alice"
}
```

```http
GET /api/schools/nearby?schoolId=1&radiusKm=30
```

```http
GET /api/posts/feed?radiusKm=30
Authorization: Bearer <token>
```

```http
POST /api/posts
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "categoryId": 1,
  "title": "期末复习资料怎么整理？",
  "content": "想问问大家期末复习有什么方法。"
}
```

## Quick Start

### 方式一：Docker Compose 启动依赖

适合本地开发。Compose 会启动 MySQL 和 Redis，并自动执行 `src/main/resources/db` 下的初始化脚本。

1. 准备本地环境变量：

```bash
cp .env.example .env
```

Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

2. 启动 MySQL 和 Redis：

```bash
docker compose up -d mysql redis
```

3. 使用 Redis profile 启动后端服务：

Linux/macOS:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=redis
```

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=redis"
```

默认地址：

```text
http://localhost:8080
```

常用命令：

```bash
docker compose ps
docker compose logs -f mysql
docker compose logs -f redis
docker compose down
```

### 方式二：完整容器化启动

适合验证部署链路。该方式会构建后端镜像，并启动 MySQL、Redis 和应用服务。

```bash
docker compose --profile app up -d --build
```

查看应用日志：

```bash
docker compose logs -f app
```

停止服务：

```bash
docker compose --profile app down
```

### 方式三：手动初始化数据库

如果不使用 Docker Compose，可以创建 MySQL 数据库后执行：

```sql
source src/main/resources/db/schema.sql;
source src/main/resources/db/data.sql;
```

然后参考 `.env.example` 准备本地配置。`.env` 仅用于本地记录，不提交到仓库；启动前可将变量配置到系统环境变量、IDE 运行配置或命令行会话中。

### 环境变量

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | 服务端口 |
| `CAMPUSCIRCLE_MYSQL_PORT` | `3307` | Compose 暴露到宿主机的 MySQL 端口，避免和本机 MySQL 默认端口冲突 |
| `CAMPUSCIRCLE_DB_URL` | `jdbc:mysql://localhost:3307/campuscircle?...` | MySQL 连接地址 |
| `CAMPUSCIRCLE_DB_USERNAME` | `root` | MySQL 用户名 |
| `CAMPUSCIRCLE_DB_PASSWORD` | `campuscircle_dev_pwd` | MySQL 密码 |
| `CAMPUSCIRCLE_REDIS_HOST` | `localhost` | Redis 主机 |
| `CAMPUSCIRCLE_REDIS_PORT` | `6379` | Redis 端口 |
| `CAMPUSCIRCLE_REDIS_DATABASE` | `0` | Redis 逻辑库 |
| `CAMPUSCIRCLE_VIEW_COUNT_FLUSH_INTERVAL_SECONDS` | `10` | 浏览量批量刷库间隔 |
| `CAMPUSCIRCLE_HOT_POST_CACHE_TTL_SECONDS` | `300` | 热门帖子缓存 TTL |
| `CAMPUSCIRCLE_HOT_POST_CACHE_JITTER_SECONDS` | `60` | 热门帖子缓存随机抖动 |
| `CAMPUSCIRCLE_HOT_POST_EMPTY_CACHE_TTL_SECONDS` | `30` | 空结果缓存 TTL |
| `CAMPUSCIRCLE_HOT_POST_REBUILD_LOCK_TTL_SECONDS` | `10` | 热榜回源重建锁 TTL |

### 本地 Maven 启动

Linux/macOS:

```bash
./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

默认地址：

```text
http://localhost:8080
```

## Optional Profiles

### Redis

默认 profile 不依赖 Redis：

- `InMemoryTokenStore` 存储登录 Token。
- `NoOpHotPostRankStore` 直接从 MySQL 查询热门帖子。

启用 `redis` profile 后：

- `RedisTokenStore` 使用 Redis 存储 Token。
- `RedisHotPostRankStore` 使用 Redis ZSet 维护热门帖子排行榜。

启动命令：

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=redis
```

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=redis"
```

### RocketMQ

默认 profile 下，评论和点赞事件同步处理，不依赖 RocketMQ。

启用 `rocketmq` profile 后：

- `RocketMqDomainEventPublisher` 发布评论/点赞事件。
- `RocketMqEventConsumer` 消费事件并生成站内通知。

配置文件：

```text
src/main/resources/application-rocketmq.yaml
```

启动命令：

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=rocketmq
```

同时启用 Redis 和 RocketMQ：

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=redis,rocketmq
```

## Test

测试环境使用 H2 内存数据库，不依赖本地 MySQL、Redis 或 RocketMQ。

Linux/macOS:

```bash
./mvnw test
```

Windows PowerShell:

```powershell
.\mvnw.cmd test
```

当前集成测试覆盖：

- Spring Boot 上下文启动
- 用户注册、登录和重复注册
- 学校检索、附近学校查询和附近学校帖子 Feed
- 分类查询、发帖、评论创建和评论查询
- 点赞、重复点赞、取消点赞和重复取消点赞
- 通知查询、未读数统计
- 未登录、非法分页、普通用户访问管理接口等边界场景
