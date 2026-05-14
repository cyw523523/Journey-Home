# 归途——流浪动物救助领养管理系统

本仓库包含两个部分：

- `backend/`：Spring Boot + MySQL 后端
- `frontend/`：Vue 3 + Element Plus 前端

当前版本已经补上“AI 智能领养建议”，会根据用户输入和数据库中的动物信息、用户资料、历史申请统计，调用智谱 `GLM-4-Flash-250414` 生成自然语言建议返回前端。

后端默认使用持久化 MySQL，重启服务后公告、用户、动物、救助和领养申请数据都会保留，不会再因为内存数据库被清空。

## 当前功能

- 用户注册、登录、Token 鉴权
- 社区交流：发帖、查看帖子详情、评论互动
- 动物档案浏览、详情、发布、审核、下架
- 救助信息浏览、发布、审核、状态更新
- 领养申请提交、取消、管理员审核
- AI 智能领养建议
- 个人中心：资料、本人发布、申请进度、修改密码
- 管理员后台：统计、统一审核、用户管理、公告管理、申请管理
- 图片上传与文件回显

## 快速启动

推荐按下面顺序启动：

1. 启动数据库

方式 A：使用 Docker

```bash
docker compose up -d
```

方式 B：使用本机已经安装的 MySQL

- 确保本机 MySQL 已启动
- 创建数据库 `guitu`
- 也可以直接使用脚本初始化：

```powershell
.\scripts\init-db.ps1 -RootPassword "你的MySQL密码"
```

默认数据库配置参考 [.env.example](/D:/003softwareEngineering/project/.env.example) 和 [scripts/init-db.ps1](/D:/003softwareEngineering/project/scripts/init-db.ps1)。

后端默认读取这些数据库环境变量：

```text
DB_HOST=localhost
DB_PORT=3306
DB_NAME=guitu
DB_USERNAME=root
DB_PASSWORD=root
```

2. 启动后端

如果你要使用 AI 建议，先配置智谱环境变量。

PowerShell：

```powershell
$env:ZHIPU_API_KEY="你的智谱APIKey"
$env:ZHIPU_AI_MODEL="glm-4-flash-250414"
$env:ZHIPU_API_URL="https://open.bigmodel.cn/api/paas/v4/chat/completions"
```

然后启动后端：

方式 A：直接启动

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

如果本机已经安装 Maven，也可以执行：

```bash
cd backend
mvn spring-boot:run
```

方式 B：使用脚本启动

```powershell
.\scripts\run-backend.ps1 -DbPassword "你的MySQL密码"
```

如果数据库账号、库名或端口不是默认值，也可以这样传：

```powershell
.\scripts\run-backend.ps1 -DbHost "localhost" -DbPort 3306 -DbName "guitu" -DbUser "root" -DbPassword "你的MySQL密码"
```

后端默认地址：

```text
http://localhost:8080
```

如果你只是临时想使用本地 H2，也可以显式启用：

```powershell
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=h2"
```

3. 启动前端

```bash
cd frontend
npm install --cache .npm-cache
npm run dev
```

前端默认地址通常是：

```text
http://localhost:5173
```

## 默认管理员

系统启动后会自动创建管理员账号：

```text
账号：admin
密码：Admin123456
```

## AI 说明

当前 AI 智能领养使用智谱 `GLM-4-Flash-250414`，接口为：

```text
POST /api/adoptions/smart-match
```

后端会把这些信息一起发给模型：

- 动物档案
- 当前用户资料
- 当前动物的历史申请统计
- 用户本次填写的领养申请与偏好输入

如果没有配置 `ZHIPU_API_KEY`，项目普通功能仍可运行，但 AI 建议接口不能使用。

## 公告说明

管理员发布的公告会存到数据库表 `notices`，并显示在这些位置：

- 顶部导航“平台公告”
- 首页右侧最新公告预览
- 公告列表页 `/notices`
- 公告详情页 `/notices/:id`

## 社区说明

社区交流功能支持：

- 登录用户发帖
- 公开查看帖子列表和帖子详情
- 登录用户发表评论
- 帖子作者或管理员可下架帖子
- 评论作者或管理员可删除评论

## 常用验证命令

前端构建：

```bash
cd frontend
npm run build
```

后端编译：

```bash
cd backend
mvn -q -DskipTests package
```

离线编译：

```bash
cd backend
mvn -q -DskipTests -o package
```

也可以使用脚本编译后端：

```powershell
.\scripts\build-backend.ps1
```

## 相关文档

- [backend/README.md](/D:/003softwareEngineering/project/backend/README.md)
- [backend/src/main/resources/application.yml](/D:/003softwareEngineering/project/backend/src/main/resources/application.yml)
