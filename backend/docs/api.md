# 后端接口草案

需求文档没有指定具体 URL，本文件按模块补齐 REST 路径，供前端联调使用。

所有接口默认返回：

```json
{
  "success": true,
  "message": "操作成功",
  "data": {}
}
```

需要登录的接口使用请求头：

```http
Authorization: Bearer <token>
```

## 认证

| 方法   | 路径                   | 权限   | 说明              |
| ---- | -------------------- | ---- | --------------- |
| POST | `/api/auth/register` | 游客   | 注册普通用户          |
| POST | `/api/auth/login`    | 游客   | 登录并返回 Token     |
| POST | `/api/auth/logout`   | 登录用户 | 退出登录，前端删除 Token |
| GET  | `/api/auth/me`       | 登录用户 | 获取当前用户          |

## 个人中心

| 方法  | 路径                           | 权限   | 说明     |
| --- | ---------------------------- | ---- | ------ |
| GET | `/api/users/me`              | 登录用户 | 查看个人信息 |
| PUT | `/api/users/me`              | 登录用户 | 修改个人信息 |
| PUT | `/api/users/me/password`     | 登录用户 | 修改密码   |
| GET | `/api/users/me/animals`      | 登录用户 | 本人动物档案，支持 `status/page/size` |
| GET | `/api/users/me/rescues`      | 登录用户 | 本人救助信息，支持 `status/page/size` |
| GET | `/api/users/me/applications` | 登录用户 | 本人领养申请 |

## 动物档案

| 方法     | 路径                         | 权限      | 说明                                                    |
| ------ | -------------------------- | ------- | ----------------------------------------------------- |
| GET    | `/api/animals`             | 游客/用户   | 动物列表，支持 `keyword/type/gender/status/region/page/size` |
| GET    | `/api/animals/{id}`        | 游客/用户   | 动物详情                                                  |
| POST   | `/api/animals`             | 登录用户    | 发布动物档案，默认待审核                                          |
| PUT    | `/api/animals/{id}`        | 发布者/管理员 | 修改动物档案                                                |
| DELETE | `/api/animals/{id}`        | 发布者/管理员 | 下架动物档案                                                |
| PATCH  | `/api/animals/{id}/status` | 管理员     | 更新动物状态                                                |

## 救助信息

| 方法     | 路径                         | 权限      | 说明                                        |
| ------ | -------------------------- | ------- | ----------------------------------------- |
| GET    | `/api/rescues`             | 游客/用户   | 救助列表，支持 `keyword/region/status/page/size` |
| GET    | `/api/rescues/{id}`        | 游客/用户   | 救助详情                                      |
| POST   | `/api/rescues`             | 登录用户    | 发布救助信息，默认待审核                              |
| PUT    | `/api/rescues/{id}`        | 发布者/管理员 | 修改救助信息                                    |
| DELETE | `/api/rescues/{id}`        | 发布者/管理员 | 下架救助信息                                    |
| PATCH  | `/api/rescues/{id}/status` | 发布者/管理员 | 更新救助处理状态                                  |

## 领养申请

| 方法    | 路径                           | 权限      | 说明      |
| ----- | ---------------------------- | ------- | ------- |
| POST  | `/api/adoptions`             | 登录用户    | 提交领养申请  |
| GET   | `/api/adoptions`             | 登录用户    | 查询本人申请  |
| GET   | `/api/adoptions/{id}`        | 申请人/管理员 | 申请详情    |
| PATCH | `/api/adoptions/{id}/cancel` | 申请人     | 取消待审核申请 |

## 社区交流

| 方法     | 路径                                | 权限      | 说明                             |
| ------ | --------------------------------- | ------- | ------------------------------ |
| GET    | `/api/community/posts`            | 游客/用户   | 帖子列表，支持 `keyword/page/size`    |
| GET    | `/api/community/posts/{id}`       | 游客/用户   | 帖子详情，包含评论列表                    |
| POST   | `/api/community/posts`            | 登录用户    | 发帖                             |
| PUT    | `/api/community/posts/{id}`       | 作者/管理员  | 修改帖子                           |
| DELETE | `/api/community/posts/{id}`       | 作者/管理员  | 下架帖子                           |
| POST   | `/api/community/posts/{id}/comments` | 登录用户 | 对帖子发表评论                        |
| DELETE | `/api/community/comments/{id}`    | 评论作者/管理员 | 删除评论                          |

## 管理员

| 方法     | 路径                                          | 权限  | 说明                    |
| ------ | ------------------------------------------- | --- | --------------------- |
| GET    | `/api/admin/audits/pending`                 | 管理员 | 待审核列表，支持 `targetType` |
| GET    | `/api/admin/audits/{targetType}/{targetId}` | 管理员 | 审核对象详情                |
| POST   | `/api/admin/audits`                         | 管理员 | 统一审核动物/救助/领养申请        |
| GET    | `/api/admin/audit-logs`                     | 管理员 | 审核日志                  |
| GET    | `/api/admin/users`                          | 管理员 | 用户管理列表                |
| PATCH  | `/api/admin/users/{id}`                     | 管理员 | 更新用户角色或状态             |
| GET    | `/api/admin/applications`                   | 管理员 | 领养申请列表                |
| PATCH  | `/api/admin/applications/{id}/audit`        | 管理员 | 审核指定申请                |
| GET    | `/api/admin/notices`                        | 管理员 | 公告管理列表                |
| POST   | `/api/admin/notices`                        | 管理员 | 新增公告                  |
| PUT    | `/api/admin/notices/{id}`                   | 管理员 | 修改公告                  |
| PATCH  | `/api/admin/notices/{id}/offline`           | 管理员 | 下架公告                  |
| DELETE | `/api/admin/notices/{id}`                   | 管理员 | 删除公告                  |
| GET    | `/api/admin/stats/overview`                 | 管理员 | 统计概览                  |
| GET    | `/api/admin/stats/animals/status`           | 管理员 | 动物状态分布                |
| GET    | `/api/admin/stats/rescues/status`           | 管理员 | 救助状态分布                |
| GET    | `/api/admin/stats/applications/status`      | 管理员 | 申请状态分布                |

## 公共展示和上传

| 方法   | 路径                   | 权限    | 说明             |
| ---- | -------------------- | ----- | -------------- |
| GET  | `/api/notices`       | 游客/用户 | 已发布公告列表        |
| GET  | `/api/notices/{id}`  | 游客/用户 | 公告详情           |
| GET  | `/api/home/overview` | 游客/用户 | 首页概览           |
| POST | `/api/files/upload`  | 登录用户  | 图片上传，返回可访问 URL |
