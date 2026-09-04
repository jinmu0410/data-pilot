# data-pilot

数据中台平台：任务流（DAG）编排 + 数据集成（DataX/SeaTunnel）+ 数据研发（SQL/Python/Shell）+ 数据服务（API）。

## 目录结构

```
data-pilot/
├── backend/    # Java 21 + Spring Boot 3 后端（二次开发自开源项目 data-platform-open，多模块）
│   └── data-pilot-web/   # 主运行模块（端口 8080，context-path /dp-web）
├── frontend/   # Vue3 + TypeScript + Element Plus 前端
└── docker/     # 见 backend/docker/ 部署编排
```

## 技术栈

- 后端：Java 21、Spring Boot 3.4、MyBatis-Plus、fastjson2、Redis、RabbitMQ、MySQL
- 前端：Vue 3、TypeScript、Element Plus、LogicFlow、Monaco Editor

## 本地运行

### 后端

```bash
cd backend
# 需 JDK 21
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
mvn -pl data-pilot-web -am package -DskipTests

# 依赖 MySQL/Redis/RabbitMQ，可用 docker 编排（backend/docker）
DB_URL='jdbc:mysql://localhost:3306/data_platform' \
DB_USERNAME=root DB_PASSWORD=dp123456 \
REDIS_HOST=localhost REDIS_PASSWORD=dp123456 \
RABBITMQ_HOST=localhost \
java -jar data-pilot-web/target/data-pilot-web-1.0.jar
```

### 前端

```bash
cd frontend
npm install
npm run dev
```

## 任务流节点类型

- **SQL**：数据源、SQL 类型（查询/非查询）、前置/后置 SQL、参数替换 `${变量}`、多段 SQL
- **DataX**：源/目标库表、字段映射、SQL 语句、前置/后置 SQL、限流、读取/写入批次、写入模式、并发度
- **SeaTunnel**：表同步
- **Python / Shell**：脚本执行（`python3 -u` / `bash`）

运行日志通过 `task_instance.log_path` 实时滚动，实例详情页每 1.5s 轮询刷新。

## 安全说明

本项目默认配置（`docker/*/.env`、`application-dev.yml`）中的密钥/密码均为**开发默认值**，部署到生产前请务必替换：

- `DP_PASSWORD_SECRET_KEY` / `DP_JWT_SECRET_KEY`（当前为全 0）
- 数据库 / Redis / RabbitMQ 密码
