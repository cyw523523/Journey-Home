# 归途——流浪动物救助领养管理系统

本仓库包含两个部分：

- `backend/`：Spring Boot + MySQL 后端
- `frontend/`：Vue 3 + Element Plus 前端

当前版本已经补上“AI 智能领养建议”，会根据用户输入和数据库中的动物信息、用户资料、历史申请统计，调用智谱 `GLM-4-Flash-250414` 生成自然语言建议返回前端。

## 当前功能

- 用户注册、登录、Token 鉴权
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

```bash
docker compose up -d
```

默认数据库配置参考 [.env.example](/D:/003softwareEngineering/project/.env.example)。

2. 启动后端

如果你要使用 AI 建议，先配置智谱环境变量。

PowerShell：

```powershell
$env:ZHIPU_API_KEY="你的智谱APIKey"
$env:ZHIPU_AI_MODEL="glm-4-flash-250414"
$env:ZHIPU_API_URL="https://open.bigmodel.cn/api/paas/v4/chat/completions"
```

然后启动后端：

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

如果本机已经安装 Maven，也可以执行：

```bash
cd backend
mvn spring-boot:run
```

后端默认地址：

```text
http://localhost:8080
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

## 相关文档

- [backend/README.md](/D:/003softwareEngineering/project/backend/README.md)
- [backend/src/main/resources/application.yml](/D:/003softwareEngineering/project/backend/src/main/resources/application.yml)
