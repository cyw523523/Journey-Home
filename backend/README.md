# 归途后端

这是流浪动物救助领养系统的 Spring Boot 后端。

## 技术栈

- JDK 17
- Spring Boot 3.3.5
- Spring Web
- Spring Security
- Spring Data JPA
- MySQL 8

## 启动前准备

### 1. 数据库

方式 A：使用 Docker

```bash
docker compose up -d
```

方式 B：使用本机已经安装的 MySQL

- 确保 MySQL 服务已启动
- 手动创建数据库 `guitu`
- 或者在项目根目录执行：

```powershell
.\scripts\init-db.ps1 -RootPassword "你的MySQL密码"
```

默认数据库配置参考：

- [../.env.example](/D:/003softwareEngineering/project/.env.example)
- [../scripts/init-db.ps1](/D:/003softwareEngineering/project/scripts/init-db.ps1)

### 2. 可选：配置智谱 AI

如果你要启用“AI 智能领养建议”，请先配置以下环境变量：

Windows `cmd`：

```bash
set ZHIPU_API_KEY=你的智谱APIKey
set ZHIPU_AI_MODEL=glm-4-flash-250414
set ZHIPU_API_URL=https://open.bigmodel.cn/api/paas/v4/chat/completions
```

PowerShell：

```powershell
$env:ZHIPU_API_KEY="你的智谱APIKey"
$env:ZHIPU_AI_MODEL="glm-4-flash-250414"
$env:ZHIPU_API_URL="https://open.bigmodel.cn/api/paas/v4/chat/completions"
```

如果不配置 `ZHIPU_API_KEY`，普通业务功能仍可运行，但 AI 建议接口不能使用。

### 3. 数据库说明

后端默认使用持久化 MySQL，不再默认使用内存 H2。只要 MySQL 数据卷还在，重启后端后公告和其他业务数据都会保留。

默认读取这些环境变量：

```text
DB_HOST=localhost
DB_PORT=3306
DB_NAME=guitu
DB_USERNAME=root
DB_PASSWORD=root
```

如果你只是临时想用 H2，可以显式启用 `h2` profile：

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=h2"
```

## 启动方式

进入当前目录：

```bash
cd backend
```

方式 A：直接启动

如果本机已经安装 Maven：

```bash
mvn spring-boot:run
```

如果本机没有安装 Maven，使用项目自带 Wrapper：

```powershell
.\mvnw.cmd spring-boot:run
```

方式 B：使用脚本启动

在项目根目录执行：

```powershell
.\scripts\run-backend.ps1 -DbPassword "你的MySQL密码"
```

如果数据库连接不是默认值，也可以显式指定：

```powershell
.\scripts\run-backend.ps1 -DbHost "localhost" -DbPort 3306 -DbName "guitu" -DbUser "root" -DbPassword "你的MySQL密码"
```

启动成功后访问：

```text
http://localhost:8080
```

## 默认管理员

系统启动时会自动创建管理员账号：

```text
账号：admin
密码：Admin123456
```

## 常用命令

编译打包：

```bash
mvn -q -DskipTests package
```

离线编译打包：

```bash
mvn -q -DskipTests -o package
```

也可以在项目根目录执行脚本：

```powershell
.\scripts\build-backend.ps1
```

## AI 接口说明

当前 AI 智能领养走的是智谱 `GLM-4-Flash-250414`，接口配置来自：

- `ZHIPU_API_KEY`
- `ZHIPU_AI_MODEL`
- `ZHIPU_API_URL`

对应后端配置位置：

- [src/main/resources/application.yml](/D:/003softwareEngineering/project/backend/src/main/resources/application.yml)

当前 AI 建议接口：

```text
POST /api/adoptions/smart-match
```

它会把以下内容一起发给模型：

- 动物档案
- 当前用户资料
- 当前动物的历史申请统计
- 用户本次填写的领养信息和偏好

## 相关文件

- [src/main/resources/application.yml](/D:/003softwareEngineering/project/backend/src/main/resources/application.yml)
- [docs/api.md](/D:/003softwareEngineering/project/backend/docs/api.md)
