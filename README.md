# MyAgent

一个基于 Spring AI 实现的 AI Agent 示例程序，提供多模型、多工具、多会话的对话式 AI 服务。

[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-green)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-2.0.0-blue)](https://spring.io/projects/spring-ai)
[![Gradle](https://img.shields.io/badge/Gradle-8.14-02303A)](https://gradle.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

## 特性

-   **多模型支持** — 集成 OpenAI、DeepSeek、GLM、Qwen，支持动态切换
-   **工具调用** — Agent 可使用文件读写、Shell 执行、代码搜索、网页抓取、天气查询等工具
-   **Skill系统** — 支持从目录加载自定义 Skill，扩展 Agent 能力
-   **会话管理** — 创建/更新/删除/分页查询会话，支持会话级别工具配置
-   **流式响应** — 基于 SSE（Server-Sent Events）的流式对话
-   **配置热加载** — `config.yaml` 文件变更自动生效，无需重启

## 内置工具

| 工具 | 能力 |
|------|------|
| **File** | 读取、写入、编辑文件 |
| **Shell** | 执行 Bash 命令 |
| **Grep** | 文本内容搜索 |
| **Glob** | 文件名模式匹配 |
| **WebFetch** | 抓取网页内容 |
| **Weather** | 查询城市天气（通过 wttr.in） |
| **Skill** | 加载自定义技能 |

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

-   **REST API 文档**: [http://localhost:8989/swagger-ui/index.html](http://localhost:8989/swagger-ui/index.html)
-   **API 根路径**: `http://localhost:8989/api`

3. **快速体验**

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
```

其他接口调用示例，参见 [api-test.http](api-test.http) 文件


## 配置说明

1. **应用配置** (`application.yaml`) 

主要配置项：

```yaml
myagent:
  data-dir: ~/myagent          # 默认数据目录
  db-file: data.sqlite         # SQLite 数据库文件名
  config-file: config.yaml     # Agent 配置文件名
  skills-dir: ~/myagent/skills, ~/.agents/skills   # skills目录，多个用逗号分隔
  env:
    # API_KEY 环境变量名
    openai-api-key: OPENAI_API_KEY
    deepseek-api-key: DEEPSEEK_API_KEY     
    glm-api-key: GLM_API_KEY
    qwen-api-key: QWEN_API_KEY
```

2. **Agent 配置** (`config.yaml`)

默认位置在 `~/myagent/config.yaml`，主要配置项：

```yaml
# 默认会话配置
session-default:
  title: Untitled
  modelId: deepseek-v4-flash
  tools: [File, Shell, Glob, Grep, WebFetch, Skill]

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
```

**注意**：模型提供商 provider 的 id 必须是 `DeepSeek`、`OpenAI`、`GLM`、`Qwen` 中的一个，否则无法识别。
若某模型提供商提供兼容 OpenAI 的接口，可以将 id 配置未 `OpenAI`，其他配置项根据实际情况填写。

> 修改 `config.yaml` 后配置会自动热加载，无需重启服务。

## API 概览

### 配置相关

| 方法 | 路径 | 说明         |
|------|------|------------|
| `GET` | `/api/config/models` | 获取可用模型列表   |
| `POST` | `/api/config/switch-model` | 切换默认模型     |
| `GET` | `/api/config/tools` | 获取内置工具列表   |
| `GET` | `/api/config/skills` | 获取可加载的技能列表 |

### 会话相关

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/session/page` | 分页查询会话列表 |
| `GET` | `/api/session/{sessionId}` | 获取会话详情 |
| `POST` | `/api/session/new` | 创建新会话 |
| `POST` | `/api/session/update` | 更新会话（标题、模型） |
| `POST` | `/api/session/delete/{sessionId}` | 删除会话 |
| `GET` | `/api/session/{sessionId}/message/all` | 获取会话所有消息 |
| `GET` | `/api/session/{sessionId}/req-resp/{messageId}` | 获取消息对（用户请求+AI响应） |

### 对话相关

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/session/chat` | 同步对话 |
| `POST` | `/api/session/chat/stream` | 流式对话（SSE） |

### 流式事件类型

| 事件 | 说明 |
|------|------|
| `MSG` | 消息内容片段 |
| `DONE` | 单条消息完成 |
| `SESSION` | 会话信息更新（自动生成标题时触发） |
| `END` | 所有消息完成，流结束 |


## 技术栈

| 组件 | 版本  | 用途                         |
|------|-----|----------------------------|
| Java | 17  | 运行环境                       |
| Spring Boot | 4.1.0 | 应用框架                       |
| Spring AI | 2.0.0 | AI 集成框架                    |
| Spring AI Agent Utils | 0.10.0 | Agent 工具（File、Shell、Grep 等） |
| MyBatis | 3.5 | ORM 持久层                    |
| SQLite | 3.53 | 关系型数据库                     |
| SpringDoc OpenAPI | 3.0 | API 文档 / Swagger UI        |


## 许可证

[MIT](LICENSE)
