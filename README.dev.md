# 开发环境部署指南

> 面向开发人员的本地环境搭建、项目配置与开发规范。

---

## 目录

1. [技术栈概览](#一技术栈概览)
2. [环境准备](#二环境准备)
3. [数据库初始化](#三数据库初始化)
4. [启动后端服务](#四启动后端服务)
5. [启动管理后台](#五启动管理后台)
6. [启动微信小程序](#六启动微信小程序)
7. [Docker 开发环境](#七docker-开发环境)
8. [项目结构](#八项目结构)
9. [开发规范](#九开发规范)
10. [常见问题](#十常见问题)

---

## 一、技术栈概览

| 模块 | 技术 | 框架/库 |
|------|------|---------|
| **后端** | Java 17 + Spring Boot 3.2.5 | MyBatis-Plus 3.5.5, Sa-Token 1.37, Hutool 5.8, EasyExcel 3.3, ZXing 3.5 |
| **PC 后台** | Vue 3.4 + TypeScript | Element Plus 2.13, Pinia 3, Vue Router 4.6, ECharts 6, Axios |
| **小程序** | UniApp 3.x + Vue 3 | Pinia 2.1, Axios 1.6 |
| **数据库** | MySQL 8.0 | 21 张业务表 |
| **部署** | Docker + Docker Compose | Nginx 反向代理 |

---

## 二、环境准备

### 2.1 必需软件

| 软件 | 版本要求 | 验证命令 |
|------|---------|---------|
| JDK | 17+ | `java -version` |
| Maven | 3.8+ | `mvn -version` |
| MySQL | 8.0+ | `mysql --version` |
| Node.js | 20+ | `node -v` |
| npm | 10+ | `npm -v` |
| Docker（可选）| 最新版 | `docker --version` |

### 2.2 推荐工具

- **IDE**：IntelliJ IDEA（后端）、VS Code / WebStorm（前端）
- **数据库管理**：Navicat、DBeaver、MySQL Workbench
- **接口调试**：浏览器访问 Knife4j 文档（后端启动后自动可用）
- **微信小程序**：微信开发者工具

### 2.3 环境变量

确保 Java 和 Maven 已加入系统 PATH，Node.js 安装正常。

---

## 三、数据库初始化

### 3.1 创建数据库

```bash
mysql -u root -p
```

```sql
CREATE DATABASE IF NOT EXISTS inventory
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
```

### 3.2 导入表结构

```bash
mysql -u root -p inventory < sql/01-schema.sql
```

### 3.3 插入测试数据（可选）

```sql
-- 插入测试账号：admin/123456
INSERT INTO sys_user(username, password, real_name, status)
VALUES ('admin', '$2a$10$...', '管理员', 1);
-- 实际密码需 BCrypt 加密，建议启动项目后通过注册接口创建
```

测试账号（需通过后端初始化或运行 SQL 插入）：

| 账号 | 密码 | 角色 |
|------|------|------|
| admin | 123456 | 系统管理员 |
| warehouse | 123456 | 仓库管理员 |
| sales | 123456 | 销售员 |

### 3.4 数据库配置说明

后端默认连接配置（`inventory-server/src/main/resources/application.yml`）：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/inventory?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&nullCatalogMeansCurrent=true
    username: root
    password: 123456
```

本地开发可以直接使用 root 账号，按实际密码修改 `password` 即可。

### 3.5 数据库升级脚本

数据库表结构变更使用增量升级脚本，位于 `sql/` 目录下：

| 脚本 | 说明 |
|------|------|
| `01-schema.sql` | 初始建库脚本（21 张表），仅首次部署使用 |
| `02-upgrade-approval-audit.sql` | 审批审计字段升级：为采购/销售/调拨表添加 `approver_id`、`approve_time` 字段及状态注释更新 |

升级方式：

```bash
# 已有数据库升级
mysql -u root -p inventory < sql/02-upgrade-approval-audit.sql
```

> **注意**：`01-schema.sql` 已包含最新的审批审计字段，新部署直接执行即可。已有数据库需单独执行升级脚本。

---

## 四、启动后端服务

### 4.1 配置检查

#### 数据库连接

确认 `application.yml` 中的数据库连接信息正确，特别是：

- `spring.datasource.url` — 数据库地址和库名
- `spring.datasource.username` — 数据库用户名
- `spring.datasource.password` — 数据库密码

#### Sa-Token 认证配置

```yaml
sa-token:
  token-name: Authorization
  token-prefix: Bearer
  jwt-secret-key: inventory-system-jwt-secret-key-2024   # JWT 密钥，开发环境可用默认值
  timeout: 86400                # Token 有效期（秒），默认 24 小时
  active-timeout: 1800          # 活跃超时（秒），1800=30 分钟无操作需重新登录
  is-concurrent: false          # 不允许同一账号同时登录（新登录踢掉旧登录）
  is-share: false               # 每个登录互不共享
```

> **生产环境必须修改 `jwt-secret-key` 为随机字符串（至少 32 位）**。

#### 审批审计字段

系统支持完整的审批流程（草稿 → 待审批 → 已入库/已出库）。后端 Service 层已实现状态流转控制：

| 状态 | 说明 | 可执行操作 |
|------|------|-----------|
| 0 (DRAFT) | 草稿 | 编辑、提交、取消、作废（管理员） |
| 4 (PENDING) | 待审批 | 审核通过（管理员）、驳回（管理员） |
| 1 (CONFIRMED) | 已入库/已出库/已完成 | 取消（管理员，涉及库存回滚） |
| 2 (CANCELED) | 已取消 | 作废（管理员） |
| 3 (VOIDED) | 已作废 | 仅可查看 |

> **权限要点**：取消已影响库存的单据（CONFIRMED 状态）仅管理员可操作，Service 层有角色校验。

### 4.2 启动

```bash
cd inventory-server

# 开发模式启动（自动热重载）
mvn spring-boot:run

# 或先编译再启动
mvn clean package -DskipTests
java -jar target/inventory-server-*.jar
```

### 4.3 验证启动

启动日志出现 `Started InventoryApplication in X seconds` 即为成功。

访问以下地址验证：

| 地址 | 说明 |
|------|------|
| http://localhost:8080/doc.html | Knife4j API 文档 |
| http://localhost:8080/v3/api-docs | OpenAPI JSON |

### 4.4 开发注意事项

- **热重载**：`mvn spring-boot:run` 下修改 Java 代码会自动重启，修改 `resources/` 下的文件（Mapper XML 等）会立即生效
- **SQL 日志**：开发模式下 `application.yml` 中 `logging.level.com.inventory: debug` 会打印 MyBatis SQL，方便调试
- **文件上传**：开发模式下图片上传到 `{user.dir}/uploads/images/`，访问路径 `http://localhost:8080/uploads/images/{文件名}`
- **跨域**：Vite 开发服务器已在 `vite.config.ts` 中配置 `/api` 代理，无跨域问题

---

## 五、启动管理后台

### 5.1 安装依赖

```bash
cd inventory-admin
npm install
```

### 5.2 启动开发服务器

```bash
npm run dev
```

启动后访问 http://localhost:3000。

### 5.3 Vite 代理配置

开发模式下 API 和上传文件请求通过 Vite 代理转发到后端（`inventory-admin/vite.config.ts`）：

```typescript
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
    },
    '/uploads': {
      target: 'http://localhost:8080',
      changeOrigin: true,
    },
  },
},
```

前端代码中所有 `/api/xxx` 和 `/uploads/xxx` 的请求自动转发至后端 `localhost:8080`，无需配置跨域。

> **配置修改**：如果后端端口不是 8080，同步修改 `vite.config.ts` 中的 `target` 地址。

### 5.4 构建生产包

```bash
npm run build      # 输出到 dist/
```

### 5.5 可用命令

| 命令 | 说明 |
|------|------|
| `npm run dev` | 启动开发服务器 |
| `npm run build` | 构建生产包（含 TypeScript 类型检查） |
| `npm run preview` | 预览构建后的生产包 |
| `npm run test` | 运行单元测试 |

---

## 六、启动微信小程序

### 6.1 安装依赖

```bash
cd inventory-miniapp
npm install
```

### 6.2 编译开发版

```bash
npm run dev:mp-weixin
```

产物在 `dist/dev/mp-weixin/`。

### 6.3 配置后端地址

编辑 `src/api/request.js`，将 BASE_URL 改为你的开发机 IP：

```js
const BASE_URL = 'http://192.168.x.x:8080/api/v1'
```

> **注意**：微信小程序不支持 `localhost`，必须填局域网 IP。手机和电脑必须在同一网络。

### 6.4 微信开发者工具

1. 打开微信开发者工具
2. 项目目录选择 `dist/dev/mp-weixin/`
3. 填入 AppID（`project.config.json` 中已配置）
4. 勾选「不校验合法域名」— 开发阶段跳过 HTTPS 校验

### 6.5 构建生产包

```bash
npm run build:mp-weixin     # 产物在 dist/build/mp-weixin/
```

发布前需将 BASE_URL 改为已备案域名，并配置 HTTPS。

### 6.6 目录说明

```
inventory-miniapp/
├── src/
│   ├── api/          # API 接口封装
│   ├── components/   # 公共组件
│   ├── pages/        # 页面（19 个）
│   │   ├── index/         # 首页仪表盘
│   │   ├── purchase/      # 采购入库
│   │   ├── sales/         # 销售出库
│   │   ├── inventory/     # 库存查询/流水/预警
│   │   └── my/            # 我的/设置
│   ├── store/        # Pinia 状态管理
│   └── utils/        # 工具函数
├── pages.json        # 路由配置
├── manifest.json     # UniApp 配置
└── project.config.json    # 微信项目配置
```

---

## 七、Docker 开发环境

### 7.1 前置条件

安装 Docker Desktop（Windows）或 Docker Engine（Linux）。

### 7.2 启动所有服务

```bash
docker-compose up -d
```

这会同时启动 MySQL、后端和前端三个容器。

### 7.3 仅启动 MySQL

如果只需要数据库，其他服务本地运行：

```bash
docker-compose up -d mysql
```

### 7.4 查看日志

```bash
docker-compose logs -f       # 全部日志
docker-compose logs -f server  # 仅后端日志
```

### 7.5 停止和清理

```bash
docker-compose stop          # 停止服务
docker-compose down          # 停止并删除容器（数据卷保留）
docker-compose down -v       # 停止并删除容器和数据卷（数据会丢失）
```

### 7.6 端口映射

| 服务 | 容器内端口 | 宿主机端口 |
|------|-----------|-----------|
| MySQL | 3306 | 3307 |
| 后端 | 8080 | 8080 |
| 管理后台 | 80 | 80 |

---

## 八、项目结构

```
inventory/
├── inventory-server/            # 后端 (Spring Boot)
│   ├── src/main/java/com/inventory/
│   │   ├── common/              # 公共模块
│   │   │   ├── config/          # 配置类（WebMvc、CORS、Sa-Token）
│   │   │   ├── controller/      # 文件上传等公共接口
│   │   │   ├── aspect/          # AOP 操作日志切面
│   │   │   ├── exception/       # 全局异常处理
│   │   │   └── result/          # 统一响应封装
│   │   ├── system/              # 系统管理模块
│   │   │   ├── controller/      # 用户/角色/配置接口
│   │   │   ├── service/         # 业务逻辑
│   │   │   └── mapper/          # 数据访问
│   │   ├── product/             # 商品管理模块
│   │   ├── warehouse/           # 仓库/库位模块
│   │   ├── purchase/            # 采购入库模块
│   │   ├── sales/               # 销售出库模块
│   │   ├── inventory/           # 库存管理模块
│   │   └── report/              # 报表模块
│   ├── src/main/resources/
│   │   ├── application.yml      # 开发配置
│   │   └── mapper/              # MyBatis XML
│   └── pom.xml
│
├── inventory-admin/             # PC 管理后台 (Vue 3)
│   ├── src/
│   │   ├── api/                 # API 接口封装（Axios）
│   │   ├── assets/              # 静态资源
│   │   ├── components/          # 公共组件
│   │   ├── layouts/             # 布局组件
│   │   ├── router/              # Vue Router 路由
│   │   ├── store/               # Pinia 状态管理
│   │   ├── views/               # 页面组件（27 个）
│   │   ├── utils/               # 工具函数
│   │   └── App.vue / main.ts    # 入口
│   ├── vite.config.ts
│   └── tsconfig.json
│
├── inventory-miniapp/           # 微信小程序 (UniApp)
│   ├── src/
│   │   ├── api/                 # API 接口
│   │   ├── components/          # 公共组件
│   │   ├── pages/               # 页面（19 个）
│   │   ├── store/               # Pinia 状态管理
│   │   └── utils/               # 工具函数
│   ├── pages.json               # 页面路由
│   └── manifest.json
│
├── sql/
│   └── 01-schema.sql            # 建表脚本（21 张表）
├── docs/                        # 项目文档
└── docker-compose.yml           # Docker 编排
```

---

## 九、开发规范

### 9.1 代码风格

- **后端**：遵循阿里巴巴 Java 开发手册，使用 Lombok 简化代码
- **前端**：TypeScript 严格模式，组件使用 `<script setup lang="ts">` 语法
- **命名**：后端类名 PascalCase，方法名 camelCase；前端组件名 PascalCase，文件名 kebab-case

### 9.2 接口规范

- RESTful 风格，统一前缀 `/api/v1`
- 统一响应格式：`{ code: 200, msg: "success", data: ... }`
- 分页参数：`page` / `size`，响应：`{ records, total, pages }`
- 关键操作记录操作日志（AOP 自动切入）

### 9.3 Git 提交

```bash
git commit -m "feat: 添加xxx功能"
git commit -m "fix: 修复xxx问题"
git commit -m "refactor: 重构xxx模块"
```

### 9.4 数据库变更

- 不要修改 `01-schema.sql` 中已发布的表结构（该脚本仅用于首次建库）
- 新的表结构变更使用增量脚本 `sql/02-xxx.sql`、`sql/03-xxx.sql` 依次递增
- 增量脚本文件名格式：`{两位数字}-{描述}.sql`

---

## 十、常见问题

### Q1：后端启动报数据库连接失败

```
Cannot create PoolConnection
The last packet sent successfully to the server was 0 ms ago.
```

检查：
- MySQL 服务是否已启动
- `application.yml` 中的用户名密码是否正确
- 数据库 `inventory` 是否已创建
- MySQL 端口是否为 3306（可在配置中修改）

### Q2：Maven 依赖下载慢

在 `~/.m2/settings.xml` 中配置阿里云镜像：

```xml
<mirrors>
    <mirror>
        <id>aliyunmaven</id>
        <mirrorOf>*</mirrorOf>
        <name>阿里云公共仓库</name>
        <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
</mirrors>
```

### Q3：npm install 报错

```bash
# 清除缓存重试
npm cache clean --force
rm -rf node_modules package-lock.json
npm install
```

### Q4：小程序打开空白

- 确认 `request.js` 中的 `BASE_URL` 填的是局域网 IP 而非 `localhost`
- 手机和电脑必须在同一局域网
- 微信开发者工具中勾选「不校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书」

### Q5：跨域报错

开发阶段访问 `http://localhost:3000`（Vite 开发服务器），`/api` 已配置代理，不会跨域。
如果直接访问 `http://localhost:8080`（后端地址）会出现跨域，这是正常的，请使用 `localhost:3000`。

### Q6：后端修改代码后未生效

```bash
# 重新编译
mvn clean compile

# 或直接重启（mvn spring-boot:run 支持自动重启）
# 按 Ctrl+C 停止后重新运行
```

### Q7：图片上传后无法访问

确认图片上传后返回的 URL 路径格式为 `/uploads/images/{文件名}`，直接访问 `http://localhost:8080/uploads/images/{文件名}` 验证。若 404，检查 `WebMvcConfig.java` 中的静态资源映射配置。

### Q8：Maven 打包报错

```bash
# 跳过测试打包
mvn clean package -DskipTests

# 如果依赖冲突
mvn dependency:tree
```
