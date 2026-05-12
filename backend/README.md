# 归途后端

这是根据目录中的《软件计划任务书》和《软件需求规格说明书》搭建的后端基础环境。

## 技术栈

- JDK 17
- Spring Boot 3.3.5
- Spring Web / Validation / Security / Data JPA
- MySQL 8.0
- Maven

## 已落地模块

- 用户注册、登录、退出、当前用户信息
- 个人资料、密码修改、本人发布/申请记录
- 动物档案发布、查询、修改、下架、状态更新
- 救助信息发布、查询、修改、下架、状态更新
- 领养申请提交、查询、取消、管理员审核
- 统一审核入口和审核日志
- 公告公开查询与后台管理
- 管理员统计接口
- 图片上传与静态资源回显
- Token 权限过滤、角色校验、统一返回、统一异常处理

接口清单见 `docs/api.md`。

## 本次对文档的保守补全

需求文档未给出具体 REST URL，本项目按模块统一补为：

- 公开接口：`/api/auth/**`、`/api/animals/**`、`/api/rescues/**`、`/api/notices/**`、`/api/home/**`
- 登录后接口：`/api/users/me/**`、`/api/adoptions/**`、`/api/files/upload`
- 管理员接口：`/api/admin/**`

需求文档中“公告发布与展示”“数据统计”的异常流程、后置条件、输入、输出仍为“【填写】”。当前实现按同文档的接口需求章节补齐为管理员可管理、游客/用户只看已发布公告，统计接口仅管理员可访问。

## 运行方式

1. 创建 MySQL 数据库：

```sql
CREATE DATABASE guitu DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 修改 `src/main/resources/application.yml` 中的数据库账号密码。

3. 在 `backend` 目录运行：

```bash
mvn spring-boot:run
```

> 当前机器已安装 JDK 17，但没有检测到 `mvn` 或 `gradle` 命令。需要安装 Maven，或在 IDE 中用 Maven 导入该项目。

## 默认约定

- 注册用户默认角色为 `USER`，状态为 `NORMAL`。
- 本地开发默认启动时创建管理员账号：`admin` / `Admin123456`，可在 `application.yml` 关闭或修改。
- 发布类数据默认状态为 `PENDING_REVIEW`。
- 动物审核通过后进入 `WAITING_ADOPTION`。
- 救助信息审核通过后进入 `PENDING_PROCESS`。
- 领养申请通过后，申请状态为 `APPROVED`，动物状态同步为 `ADOPTED`。
- 上传图片限制为 JPG/JPEG/PNG/WEBP，单文件最大 5MB。
