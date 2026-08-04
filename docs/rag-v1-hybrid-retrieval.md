# RAG v1 混合检索设计

## 当前阶段

当前问答链路仍由 `PostRetrievalService` 提供 SQL 关键词召回。新增的 `PostRetriever` 和
`RetrievalQuery` 将“调用方的权限范围”和“检索实现”分开：`AiAssistantService` 只负责认证、附近学校
计算、提示词、模型调用和引用校验；检索器负责在 `allowedSchoolIds` 范围内找帖子。

因此，后续替换为混合检索时不会改变问答接口，也不会把学校权限下沉为可选逻辑。

## 目标链路

```text
帖子创建 / 修改 / 删除
  -> PostSearchEvent(eventId, postId, operation, contentVersion, occurredAt)
  -> RocketMQ
  -> 消费者回查 MySQL（事实源）
  -> 生成 searchText 与 Embedding
  -> Elasticsearch upsert / delete

用户提问
  -> Token -> user.schoolId -> nearby schoolIds
  -> HybridPostRetriever
     -> Elasticsearch 文本检索（BM25）
     -> Elasticsearch 向量检索（kNN）
     -> RRF 融合
  -> TopK 帖子 -> Prompt -> 模型 -> 引用校验
```

## 索引文档字段

| 字段 | 用途 |
| --- | --- |
| `postId` | 文档 ID 与最终引用 ID |
| `schoolId` | 两路检索均强制过滤的权限字段 |
| `categoryId` / `categoryName` | 过滤与补充上下文 |
| `title` / `content` / `searchText` | 关键词检索与 Prompt 上下文 |
| `embedding` | `dense_vector`，用于语义 kNN |
| `contentVersion` | 拒绝延迟、重复或乱序事件 |
| `updatedAt` | 排障与索引时效观测 |

`searchText` 由标题、分类名称和正文拼接而成。MySQL 始终是事实源；消息只传递变化通知，消费者不信任
消息内的帖子正文。

## 本地 Elasticsearch

Elasticsearch 使用可选的 `search` Compose profile，避免默认启动 MySQL/Redis 时额外占用内存：

```powershell
docker compose --profile search up -d elasticsearch
curl http://localhost:9200
```

本阶段的 `CAMPUSCIRCLE_SEARCH_ENABLED` 保持 `false`。下一阶段接入 Embedding 客户端、索引消费者和
`HybridPostRetriever` 后才将它设为 `true`。

## 降级原则

- Elasticsearch 不可用：退回当前 SQL 关键词检索。
- Embedding 不可用：可继续使用 Elasticsearch 文本检索；两者都不可用则退回 SQL。
- 模型不可用：返回受权限过滤的候选帖子引用和明确的失败提示，不生成未经验证的结论。
