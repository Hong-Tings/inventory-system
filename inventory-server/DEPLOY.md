# 单机部署方案

## 环境要求

| 组件 | 版本 | 说明 |
|------|------|------|
| JDK | 17+ | Spring Boot 3.2 要求 |
| MySQL | 8.0+ | 数据库 |
| Nginx | 1.20+ | 静态文件服务 + 反向代理（可选，也可直接暴露后端端口） |

最低配置：2 核 4G 云服务器，20G 磁盘。

---

## 一、后端部署

### 1. 打包

```bash
cd inventory-server
mvn clean package -DskipTests
```

打包后在 `target/` 目录生成 `inventory-server-0.0.1-SNAPSHOT.jar`。

### 2. 初始化数据库

将 `docs/sql/init.sql` 导入 MySQL（如果之前没导出，从开发库导出一份完整的）：

```bash
mysql -u root -p < init.sql
```

> **注意**：生产环境不要用 root 账号，应该新建一个专用账号：
> ```sql
> CREATE USER 'inventory'@'%' IDENTIFIED BY '你的随机密码';
> GRANT ALL PRIVILEGES ON inventory.* TO 'inventory'@'%';
> FLUSH PRIVILEGES;
> ```

### 3. 创建生产配置

在 `inventory-server/` 目录下创建 `application-prod.yml`：

```yaml
server:
  port: 8080

spring:
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 10MB
  datasource:
    url: jdbc:mysql://localhost:3306/inventory?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&nullCatalogMeansCurrent=true&autoReconnect=true
    username: inventory
    password: 你的数据库密码
    driver-class-name: com.mysql.cj.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      idle-timeout: 30000
      connection-timeout: 30000

mybatis-plus:
  configuration:
    # 生产环境关掉 SQL 日志
    log-impl: org.apache.ibatis.logging.nologging.NoLoggingImpl

app:
  upload-base-path: /data/inventory/uploads

sa-token:
  jwt-secret-key: 改成随机字符串，至少32位
```

### 4. 创建上传目录

```bash
mkdir -p /data/inventory/uploads/images
```

### 5. 启动服务

```bash
# Linux（前台运行，测试用）
java -jar inventory-server-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod

# Linux（后台运行）
nohup java -jar inventory-server-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  --app.upload-base-path=/data/inventory/uploads \
  --spring.datasource.password=你的密码 \
  > /data/inventory/app.log 2>&1 &

# Windows
java -jar inventory-server-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

> **生产密钥建议**：JWT 密钥和数据库密码不要写死在 yml 里，通过启动参数传入：
> ```bash
> java -jar app.jar \
>   --spring.profiles.active=prod \
>   --sa-token.jwt-secret-key=$(cat /data/inventory/secret.key) \
>   --spring.datasource.password=$(cat /data/inventory/db.pwd)
> ```

---

## 二、前端部署

### 1. 打包

```bash
cd inventory-admin
npm install
npm run build
```

打包后生成 `dist/` 目录，里面就是纯静态文件。

### 2. Nginx 配置

将 `dist/` 文件上传到服务器，配置 Nginx：

```nginx
server {
    listen 80;
    server_name 你的域名或IP;

    # 前端静态文件
    location / {
        root /data/inventory/admin;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # API 反向代理到后端
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # 上传文件访问
    location /uploads/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
    }
}
```

### 3. 启动 Nginx

```bash
# 验证配置
nginx -t

# 启动
nginx

# 重载
nginx -s reload
```

### 4. 无域名时直接访问

如果没有域名，Nginx 配置里 `server_name` 写服务器 IP，直接访问 `http://服务器IP`。

---

## 三、Windows 部署简化方案

如果客户用 Windows 服务器，可以跳过 Nginx，直接用 Spring Boot 同时提供前后端：

### 1. 后端启动（同上）

### 2. 前端文件复制到后端静态目录

```bash
# 将前端 dist/ 下的文件复制到后端的 static 目录
# 创建目录 src/main/resources/static/
# 将 index.html、assets/ 等复制进去
```

这样重新打包后，访问 `http://服务器IP:8080` 就能直接打开前端页面，不需要 Nginx 也不存在跨域问题。但缺点是不能同时服务 HTTPS。

### 3. Windows 开机自启

创建一个 `start.bat`：

```bat
@echo off
start javaw -jar inventory-server-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

放到「启动」文件夹实现开机自启。

---

## 四、数据库备份

### Linux 定时备份

```bash
# 创建备份脚本 /data/inventory/backup.sh
#!/bin/bash
DATE=$(date +%Y%m%d)
mysqldump -u inventory -p密码 inventory > /data/inventory/backup/inventory_$DATE.sql
find /data/inventory/backup -mtime +30 -name "*.sql" -delete
```

```bash
# crontab 每天凌晨3点备份
0 3 * * * bash /data/inventory/backup.sh
```

### Windows 定时备份

用「任务计划程序」定时执行：

```bat
mysqldump -u inventory -p密码 inventory > C:\data\inventory\backup\inventory_%date:~0,4%%date:~5,2%%date:~8,2%.sql
```

---

## 五、检查清单

- [ ] MySQL 创建专用账号，不直接用 root
- [ ] JWT 密钥改成随机字符串
- [ ] SQL 日志关掉（`NoLoggingImpl`）
- [ ] 上传目录已创建且有写入权限
- [ ] 数据库已配置定时备份
- [ ] 前端 `VITE_API_BASE` 或代理配置正确
- [ ] 生产配置中 `app.upload-base-path` 是绝对路径

---

## 六、常见问题

**Q: 图片上传后访问 404？**
检查 `app.upload-base-path` 配置的路径是否正确，确认 Nginx 或静态资源映射配了 `/uploads/`。

**Q: 前端页面白屏/刷新后 404？**
Nginx 需要配置 `try_files $uri $uri/ /index.html;` 处理 Vue 路由。

**Q: 数据库连不上？**
检查 MySQL 是否允许远程连接，用户权限是否正确，防火墙是否放行了 3306 端口。
