# 进销存管理后台

PC 管理后台前端项目，基于 Vue 3 + TypeScript + Element Plus。

## 启动

```bash
npm install
npm run dev        # 开发模式，默认 http://localhost:3000
```

## 构建

```bash
npm run build      # 生产构建，产物在 dist/
```

## 配置修改

### 1. API 代理地址（开发环境）

`vite.config.ts` 中的 proxy 配置：

```typescript
server: {
  port: 3000,
  proxy: {
    '/api': { target: 'http://localhost:8080', changeOrigin: true },
    '/uploads': { target: 'http://localhost:8080', changeOrigin: true },
  },
}
```

如果后端端口不是 8080，同步修改 target 地址。

### 2. API 基础路径

`src/api/request.ts` 中 Axios 的 `baseURL` 默认为空（通过 Vite 代理转发），生产环境如不使用代理，需修改为后端地址。

### 3. 后端地址配置（生产环境）

构建前检查所有 API 请求路径是否以 `/api/v1` 开头（已统一配置），确保与 Nginx 反向代理规则匹配。

## 可用命令

| 命令 | 说明 |
|------|------|
| `npm run dev` | 启动开发服务器 |
| `npm run build` | 构建生产包 |
| `npm run preview` | 预览构建产物 |
| `npm run typecheck` | TypeScript 类型检查 |

## 目录结构

```
src/
├── api/          # API 接口封装（Axios）
├── assets/       # 静态资源
├── components/   # 公共组件
├── layouts/      # 布局组件（侧边栏 + 顶栏）
├── router/       # Vue Router 路由配置
├── store/        # Pinia 状态管理
├── types/        # TypeScript 类型定义
├── utils/        # 工具函数
├── views/        # 页面组件
│   ├── dashboard/     # 工作台
│   ├── product/       # 商品管理
│   ├── purchase/      # 采购入库
│   ├── sales/         # 销售出库
│   ├── inventory/     # 库存管理
│   ├── stocktake/     # 库存盘点
│   ├── transfer/      # 库存调拨
│   ├── report/        # 统计报表
│   ├── system/        # 系统管理
│   ├── help/          # 系统使用说明
│   └── login/         # 登录
├── App.vue
└── main.ts
```
