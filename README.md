# Atlantic Coffee 智能客服系统

对标瑞幸小程序 & Pacific Coffee 的咖啡点单 Web 应用，内嵌 AI 智能客服（RAG 知识问答 + Function Calling 只读工具），求职作品集项目。

> 模块化单体：Spring Boot 3.5 · JDK 21 虚拟线程 · Spring AI Alibaba 1.1.2.0（qwen-plus / qwen-turbo）· MySQL 8 · Redis 8（会话缓存 + 向量检索）· Vue 3 + TypeScript + Element Plus

## 设计文档（五阶段决策链，docs/）

| 阶段 | 文档 |
|------|------|
| 一、产品需求分析 | [01-产品需求分析 v1.2](docs/01-产品需求分析%20v1.2.md) |
| 二、技术栈确定 | [02-技术栈选型 v1.3](docs/02-技术栈选型%20v1.3.md) |
| 三、总体设计 | [03-总体设计 v1.3](docs/03-总体设计%20v1.3.md) |
| 四、详细设计 | [04-详细设计 v1.4](docs/04-详细设计%20v1.4.md) |
| 五、编码实现 | 进行中（本仓库即产出） |

## 仓库布局

```
backend/    Maven 工程（Spring Boot，含 Dockerfile）
frontend/   Vite 工程（Vue3 + TS，构建产物 dist/ 由 nginx 挂载）
nginx/      反代配置与证书
docs/       五阶段设计文档
```

## 快速启动

```bash
# 1. 中间件（MySQL 8 + Redis 8）
docker compose up -d mysql redis

# 2. 后端（首次启动 Flyway 自动建表；需在 .env 填入 DASHSCOPE_API_KEY）
cd backend && ./mvnw spring-boot:run

# 3. 前端（开发期 /api 代理至 8080）
cd frontend && npm install && npm run dev
```

环境变量见 [.env.example](.env.example)（复制为 `.env` 后填写；`.env` 不入库）。
