# 归途——流浪动物救助领养管理系统

本仓库根据目录中的计划书和需求规格说明书搭建，包含：

- `backend/`：Spring Boot + MySQL 后端
- `frontend/`：Vue 3 + Element Plus 前端

## 当前完成度

已完成课程演示所需的核心业务闭环：

- 用户注册、登录、Token 登录状态
- 动物档案浏览、详情、发布、审核、下架
- 救助信息浏览、发布、审核、状态更新
- 领养申请提交、取消、管理员审核
- 个人中心：资料、本人发布、申请进度、修改密码
- 管理员后台：统计、统一审核、用户管理、公告管理、申请管理
- 图片上传接口与前端上传组件
- 前后端 `/api` 路径衔接

未做或暂缓：

- AI 智能领养推荐：需求文档标为 P2 增强功能，基础版先不实现。
- 支付、短信、地图、第三方登录、即时通讯、线下回访：需求文档明确暂不接入。

## 推荐启动方式

最推荐的方式是：

1. 用 Docker 启动 MySQL
2. 用 Maven Wrapper 启动后端
3. 用 npm 启动前端

这样别人 clone 以后不需要先装 Maven。

## 前端运行

```bash
cd frontend
npm install --cache .npm-cache
npm run dev
```

默认地址通常是 `http://localhost:5173/`。如果端口被占用，Vite 会自动换端口。

前端构建验证：

```bash
cd frontend
npm run build
```

## 后端运行

后端需要 JDK 17、MySQL 8.0。

仓库已经包含 Maven Wrapper：

```text
backend/mvnw
backend/mvnw.cmd
```

所以一般不需要手动安装 Maven。

## 数据库启动

优先使用 Docker：

```bash
docker compose up -d
```

默认会启动一个 MySQL 8.0，并创建数据库 `guitu`。

默认账号配置参考 [.env.example](D:/003softwareEngineering/project/.env.example)。

```sql
CREATE DATABASE guitu DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

如果你不用 Docker，而是使用本机已有 MySQL，可以手动建库，或执行：

```powershell
.\scripts\init-db.ps1 -RootPassword "你的MySQL密码"
```

然后启动后端：

```powershell
.\scripts\run-backend.ps1 -DbPassword "你的MySQL密码"
```

也可以直接在 `backend` 目录运行：

```powershell
.\mvnw.cmd spring-boot:run
```

前提是先设置数据库环境变量：

```powershell
$env:DB_HOST="localhost"
$env:DB_PORT="3306"
$env:DB_NAME="guitu"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="你的MySQL密码"
```

默认开发管理员：

```text
账号：admin
密码：Admin123456
```

## 当前环境说明

这台机器当前验证结果：

- JDK 17：已安装
- Node / npm：已安装
- Maven Wrapper：已生成
- 前端：已构建通过
- 后端：已编译通过
- MySQL 8.0：本机服务存在，但 root 密码未知；因此更推荐通过 Docker 启动项目自己的数据库

所以 push 到 GitHub 时，推荐把 `tools/` 保持忽略，不上传本机 Maven；别人直接使用 `backend/mvnw.cmd` 即可。

后端编译验证命令：

```powershell
.\scripts\build-backend.ps1
```
