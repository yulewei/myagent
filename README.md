# MyAgent

一个基于 Spring AI 实现的 AI Agent 示例程序，提供多模型、多工具、多知识库 RAG 增强的对话式 AI 服务。

## 特性

- **多模型支持**：集成 OpenAI、DeepSeek、GLM、Qwen，支持动态切换
- **工具调用**：Agent 可使用文件读写、Shell 执行、文本搜索、网页抓取、天气查询等工具
- **Skill 系统**：支持从目录加载自定义 Skill，扩展 Agent 能力
- **会话管理**：创建/更新/删除/分页查询会话，支持会话级别工具配置
- **流式响应**：基于 SSE（Server-Sent Events）的流式对话
- **知识库 & RAG**：基于文档向量化的知识库管理，提供检索增强生成能力
- **配置热加载**：`config.yaml` 文件变更自动生效，无需重启

## 内置工具

| 工具           | 能力                 |
|--------------|--------------------|
| **File**     | 读取、写入、编辑文件         |
| **Shell**    | 执行 Bash 命令         |
| **Grep**     | 文本内容搜索             |
| **Glob**     | 文件名模式匹配            |
| **WebFetch** | 抓取网页内容             |
| **Weather**  | 查询城市天气（调用 wttr.in） |
| **Skill**    | 加载自定义技能            |

## 快速开始

1. **配置 API 密钥**

通过环境变量设置 AI 提供商 API 密钥：

```bash
export OPENAI_API_KEY=sk-xxx
export DEEPSEEK_API_KEY=sk-xxx
export GLM_API_KEY=xxx
export QWEN_API_KEY=sk-xxx
```

> API 密钥也可以在 `config.yaml` 中直接配置，但推荐使用环境变量。

2. **启动服务**

依赖 JDK 17+

```bash
# 启动服务
./gradlew bootRun
```

服务启动后，访问：

- **Swagger REST API 文档**: [http://localhost:8989/swagger-ui/index.html](http://localhost:8989/swagger-ui/index.html)
- **API 根路径**: `http://localhost:8989/api`

4. **快速体验**

```bash
# 创建新会话
curl -X POST http://localhost:8989/api/session/new \
  -H "Content-Type: application/json" \
  -d '{ "sessionId": "deepseek:agent", "modelId": "deepseek-v4-flash", "tools": [] }'

# 发送消息（同步）
curl -X POST http://localhost:8989/api/session/chat \
  -H "Content-Type: application/json" \
  -d '{"sessionId": "deepseek:agent", "content": "查询杭州天气"}'

# 流式对话（SSE）
curl -X POST http://localhost:8989/api/session/chat/stream \
  -H "Content-Type: application/json" \
  -d '{"sessionId": "deepseek:chat", "content": "查询杭州天气"}'

# 使用知识库，增强大模型的对话生成（RAG）
curl -X POST http://localhost:8989/api/session/chat \
  -H "Content-Type: application/json" \
  -d '{ "sessionId": "deepseek:chat", "content": "MyAgent 项目有哪些内置工具？", "knowledgeId": "kb" }'
```

其他接口调用示例，参见 [api-test.http](api-test.http) 文件。

## 配置说明

1. **应用配置** (`application.yaml`)

主要配置项：

```yaml
myagent:
  data-dir: ~/myagent              # 默认数据目录，目录下包含 SQLite 文件、config.yaml、skills目录、知识库文件上传目录等
  db-file: data.sqlite             # SQLite 数据库文件名
  config-file: config.yaml         # Agent 配置文件名
  skills-dir: ~/myagent/skills, ~/.agents/skills   # skills目录，多个用逗号分隔
  knowledge:
    vector-db-file: vectordb.json  # 向量数据库文件名
    upload-file-dir: files         # 知识库文件上传目录
  env:
    # API_KEY 环境变量名
    openai-api-key: OPENAI_API_KEY
    deepseek-api-key: DEEPSEEK_API_KEY
    glm-api-key: GLM_API_KEY
    qwen-api-key: QWEN_API_KEY
```

完整的 `application.yaml` 配置文件参见 [application.yaml](src/main/resources/application.yaml)。

2. **Agent 配置** (`config.yaml`)

默认位置在 `~/myagent/config.yaml`，主要配置项：

```yaml
default:
  # 默认会话配置
  session:
    model-id: deepseek-v4-flash
    tools: [ File, Shell, Glob, Grep, WebFetch, Skill ]
  # 默认嵌入模型配置
  embedding:
    provider-id: GLM
    model-id: embedding-3
    dimensions: 256

# 模型提供商配置
providers:
  - id: DeepSeek
    name: DeepSeek
    baseUrl: https://api.deepseek.com
    apiKey: ${DEEPSEEK_API_KEY}
    models:
      # 模型列表
      - id: deepseek-v4-flash
        name: DeepSeek V4 Flash
      - id: deepseek-v4-pro
        name: DeepSeek V4 Pro
  - id: GLM
    name: 智谱GLM
    base-url: https://open.bigmodel.cn/api/paas/v4
    api-key: ${GLM_API_KEY}
    models:
      - id: glm-5
      - id: glm-5.1
      - id: embedding-3
        type: embedding
```

+ 完整的默认配置文件参见 [config.yaml](src/main/resources/init/config.yaml)。服务首次启动时，该文件会自动拷贝到
`~/myagent/config.yaml` 。
+ 模型提供商 provider 的 id 必须是 `DeepSeek`、`OpenAI`、`GLM`、`Qwen` 中的一个，否则无法识别。
若某模型提供商提供兼容 OpenAI 的接口，可以将 id 配置未 `OpenAI`，其他配置项根据实际情况填写。
+ 若要服务正常运行，`config.yaml` 设置的默认模型的 API_KEY，必须先配置对应的环境变量或直接在 `config.yaml` 中设置，若同时配置，环境变量优先。
+ 修改 `config.yaml` 后配置会自动热加载，无需重启服务。

## API 概览

服务启动后，可以访问完整的 **Swagger REST API 文档
**: [http://localhost:8989/swagger-ui/index.html](http://localhost:8989/swagger-ui/index.html)

### 配置相关

| 方法     | 路径                         | 说明         |
|--------|----------------------------|------------|
| `GET`  | `/api/config/models`       | 获取可用模型列表   |
| `POST` | `/api/config/switch-model` | 切换默认模型     |
| `GET`  | `/api/config/tools`        | 获取内置工具列表   |
| `GET`  | `/api/config/skills`       | 获取可加载的技能列表 |

### 会话相关

| 方法       | 路径                                              | 说明               |
|----------|-------------------------------------------------|------------------|
| `GET`    | `/api/session/page`                             | 分页查询会话列表         |
| `GET`    | `/api/session/{sessionId}`                      | 获取会话详情           |
| `POST`   | `/api/session/new`                              | 创建新会话            |
| `POST`   | `/api/session/update`                           | 更新会话，包括标题和模型     |
| `DELETE` | `/api/session/{sessionId}`                      | 删除会话             |
| `GET`    | `/api/session/{sessionId}/message/all`          | 获取会话所有消息         |
| `GET`    | `/api/session/{sessionId}/req-resp/{messageId}` | 获取消息对（用户请求+AI响应） |

### 对话相关

| 方法     | 路径                         | 说明        |
|--------|----------------------------|-----------|
| `POST` | `/api/session/chat`        | 同步对话      |
| `POST` | `/api/session/chat/stream` | 流式对话（SSE） |

**流式事件类型**：

| 事件        | 说明                |
|-----------|-------------------|
| `MSG`     | 消息内容片段            |
| `DONE`    | 单条消息完成            |
| `SESSION` | 会话信息更新（自动生成标题时触发） |
| `END`     | 所有消息完成，流结束        |

### 知识库相关

| 方法       | 路径                                           | 说明         |
|----------|----------------------------------------------|------------|
| `GET`    | `/api/knowledge/list`                        | 查询知识库列表    |
| `GET`    | `/api/knowledge/{knowledgeId}`               | 查询知识库详情    |
| `POST`   | `/api/knowledge/new`                         | 创建知识库      |
| `POST`   | `/api/knowledge/update`                      | 更新知识库      |
| `DELETE` | `/api/knowledge/{knowledgeId}`               | 删除知识库      |
| `GET`    | `/api/knowledge/{knowledgeId}/doc/list`      | 查询知识库列表    |
| `GET`    | `/api/knowledge/{knowledgeId}/{docId}`       | 查询知识库列表    |
| `POST`   | `/api/knowledge/{knowledgeId}/upload/file`   | 添加文档文件到知识库 |
| `POST`   | `/api/knowledge/{knowledgeId}/upload/text`   | 添加纯文本到知识库  |
| `POST`   | `/api/knowledge/{knowledgeId}/{docId}/embed` | 添加纯文本到知识库  |
| `DELETE` | `/api/knowledge/{knowledgeId}/{docId}`       | 删除知识库文档    |
| `GET`    | `/api/knowledge/file/{fileKey}`              | 下载知识库文件    |



## 数据库表设计

| 表名                  | 说明     |
|---------------------|--------|
| `t_session`         | 会话表    |
| `t_session_message` | 会话消息表  |
| `t_knowledge`       | 知识库表   |
| `t_knowledge_doc`   | 知识库文档表 |

+ 完整表定义参见 [schema.sql](src/main/resources/schema.sql)。
+ 服务运行时使用 SQLite 数据库，内嵌数据库，无需额外配置。

## 技术栈

| 组件                    | 版本     | 用途                          |
|-----------------------|--------|-----------------------------|
| Java                  | 17+    | 运行环境                        |
| Spring Boot           | 4.1.0  | 应用框架                        |
| Spring AI             | 2.0.0  | AI 集成框架                     |
| Spring AI Agent Utils | 0.10.0 | Agent 工具（File、Shell、Grep 等） |
| MyBatis               | 3.5    | ORM 持久层                     |
| SQLite                | 3.53   | 关系型数据库                      |
| SpringDoc OpenAPI     | 3.0    | API 文档 / Swagger UI         |

## 许可证

[MIT](LICENSE)
