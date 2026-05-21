# 社区交流板块完善 — 设计文档

> 创建日期: 2026-05-21  
> 状态: 已锁定,待实现

## 1. 概述

将"社区交流"从单流帖子 + 二级评论升级为**贴吧式分版块、强互动、楼层化讨论**的社区。参照百度贴吧的核心体验,选取 L1(内容组织) + L2(互动信号) + L3(评论体验)三层,不做等级/吧主/签到体系。

### 范围总览

- **L1 内容组织** — 管理员预设版块(category),发帖必选版块
- **L2 互动信号** — 帖子+评论点赞、收藏帖子、关注社区用户(独立表)、浏览量
- **L3 评论体验** — 楼层+楼号+跳页、楼中楼拍平、"A 回复 B"、@ 提及、只看楼主/倒序

### 不在范围内

- 等级/经验/签到/徽章/吧主体系
- 用户自建版块、关注版块、版块置顶、精华
- 踩(dislike)、分享、富文本/Markdown
- 评论引用回复、草稿、定时发布
- 关注用户的推送开关
- 小游戏(独立 spec 另行讨论)

---

## 2. 架构定位

完全沿用现有 Spring Boot 分层 + Vue 3 结构,不引入新框架。

**后端** (`com.guitu`): 在已有 `controller / service / domain / repository / dto / mapper` 下原位扩展,新增若干表和 Service。

**前端** (`src/`): 复用 `views / components / api / stores / i18n`,新建版块相关页面 + 重写帖子详情页评论区。

**通知**: 接入现有通知铃铛体系,新增 6 种通知类型。

**审核**: 沿用 `ContentReport` / `AppealRecord` 流程,帖子和评论默认 `PUBLISHED` 直接发布;版块/点赞/收藏/关注/浏览量本身不入审核流。

---

## 3. 数据模型

### 3.1 改动现有表

#### `community_posts` (改)

新增字段:

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `category_id` | `BIGINT` | — | 所属版块,外键 → `community_categories.id`。存量数据迁移到默认版块"闲聊灌水"后再加 NOT NULL |
| `view_count` | `BIGINT` | 0 | 浏览量 |
| `like_count` | `BIGINT` | 0 | 点赞数(冗余) |
| `comment_count` | `BIGINT` | 0 | 评论总数(楼层 + 楼中楼,冗余) |
| `favorite_count` | `BIGINT` | 0 | 收藏数(冗余) |
| `last_active_at` | `DATETIME` | =createdAt | 最后活动时间,有新楼层/楼中楼时更新,用于"最新活跃"排序 |

新增索引:
- `idx_post_category_active(category_id, last_active_at DESC)`
- `idx_post_created(created_at DESC)`
- `idx_post_like(likes DESC)` (热门排序)
- `idx_post_author_created(author_id, created_at DESC)` (某用户发的帖)

#### `community_comments` (改)

新增字段:

| 字段 | 类型 | 可空 | 说明 |
|------|------|------|------|
| `floor_no` | `INT` | 是 | 楼号,仅一级楼层有值;楼中楼为 NULL |
| `root_comment_id` | `BIGINT` | 是 | 所属一级楼层的 id;一级楼层=NULL,楼中楼指向其楼层 |
| `reply_to_comment_id` | `BIGINT` | 是 | 楼中楼:指向"被回复的那条具体评论";用于"A 回复 B"展示 |
| `like_count` | `INT` | 0 | 评论点赞数(冗余) |

字段语义:
- 一级楼层: `floor_no = N`, `root_comment_id = NULL`, `reply_to_comment_id = NULL`
- 楼中楼(回复楼层): `floor_no = NULL`, `root_comment_id = 楼层id`, `reply_to_comment_id = 楼层id`
- 楼中楼(回复另一条楼中楼): `floor_no = NULL`, `root_comment_id = 楼层id`, `reply_to_comment_id = 那条楼中楼id`

旧字段 `parent_comment_id` 弃用: 存量数据迁移时将其值搬到 `root_comment_id` / `reply_to_comment_id`,字段保留(JPA 标 `@Deprecated`,不再读写),向前兼容。

新增索引:
- `uk_post_floor(post_id, floor_no)` — 唯一索引(仅 floor_no 不为 NULL 时有效)
- `idx_root(root_comment_id, created_at)` — 楼中楼展开

### 3.2 新增表

#### `community_categories` (版块)

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | PK,自增 | |
| `code` | `VARCHAR(64) UNIQUE NOT NULL` | 英文标识 (`adoption`, `medical`), 用于 URL 和 i18n 键 |
| `name` | `VARCHAR(64) NOT NULL` | 显示名(中文) |
| `name_en` | `VARCHAR(64)` | 显示名(英文) |
| `description` | `VARCHAR(255)` | 简介 |
| `icon` | `VARCHAR(255)` | 图标(Lucide 图标名或 URL) |
| `sort_order` | `INT NOT NULL DEFAULT 0` | 排序,小值在前 |
| `enabled` | `BOOLEAN NOT NULL DEFAULT TRUE` | 是否启用。禁用后不展示也不允许新发帖,已有帖子保留可访问 |
| `post_count` | `BIGINT NOT NULL DEFAULT 0` | 该版块帖子数(冗余) |
| `created_at` | | BaseEntity |
| `updated_at` | | BaseEntity |

种子数据(6 个预设版块): 领养经验(`adoption`) / 医疗护理(`medical`) / 寻宠送养(`lost`) / 救助求助(`rescue`) / 日常晒宠(`dailylife`) / 闲聊灌水(`chat`)。

#### `community_likes` (通用点赞,帖子 + 评论共用)

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | PK | |
| `user_id` | `BIGINT NOT NULL` | 点赞者 |
| `target_type` | `VARCHAR(16) NOT NULL` | 枚举: `POST` / `COMMENT` |
| `target_id` | `BIGINT NOT NULL` | 帖子或评论 id |
| `created_at` | | |

- `uk_like(user_id, target_type, target_id)` — 唯一索引,防重复点赞
- `idx_target(target_type, target_id)` — 查某目标的所有点赞者

#### `community_post_favorites` (收藏)

| 字段 | 类型 |
|------|------|
| `id` | PK |
| `user_id` | `BIGINT NOT NULL` |
| `post_id` | `BIGINT NOT NULL` |
| `created_at` | |

- `uk_fav(user_id, post_id)` — 唯一索引
- `idx_user_created(user_id, created_at DESC)` — 我的收藏列表

#### `community_user_follows` (社区关注,**独立于救助站关注**)

| 字段 | 类型 |
|------|------|
| `id` | PK |
| `follower_id` | `BIGINT NOT NULL` |
| `followee_id` | `BIGINT NOT NULL` |
| `created_at` | |

- `uk_follow(follower_id, followee_id)` — 唯一索引
- `idx_followee(followee_id)` — 看"我的粉丝"
- 应用层校验: `follower_id != followee_id`

#### `community_comment_mentions` (@ 提及)

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | PK | |
| `comment_id` | `BIGINT NOT NULL` | 哪条评论里 @ 的 |
| `mentioned_user_id` | `BIGINT NOT NULL` | 被 @ 的用户 |
| `created_at` | | |

- `uk_mention(comment_id, mentioned_user_id)` — 唯一索引

#### `community_post_view_logs` (浏览量防刷)

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | PK | |
| `post_id` | `BIGINT NOT NULL` | |
| `viewer_key` | `VARCHAR(80) NOT NULL` | 登录用户 = `u:{userId}`, 游客 = `ip:{ipHash8}` |
| `viewed_on` | `DATE NOT NULL` | 当天日期 |

- `uk_view(post_id, viewer_key, viewed_on)` — 唯一索引(同一人同一帖每天只 +1)

> 注意: 此表会随时间膨胀,后续需加 30 天归档清理脚本(本期不做,记 follow-up)。

### 3.3 通知系统新增类型

在现有通知表内新增以下 `type` 值(假定现有通知表有 type 字段;实现时需 verify 现有结构):

| 类型常量 | 触发条件 | Payload 关键字段 |
|---------|---------|-----------------|
| `COMMUNITY_POST_COMMENTED` | 有人在我的帖子下发布新楼层(楼中楼不触发此类型) | `postId, commentId, fromUserId` |
| `COMMUNITY_COMMENT_REPLIED` | 有人回复了我的评论(楼中楼) | `postId, commentId, fromUserId` |
| `COMMUNITY_POST_LIKED` | 有人点赞我的帖子 | `postId, fromUserId` |
| `COMMUNITY_COMMENT_LIKED` | 有人点赞我的评论 | `postId, commentId, fromUserId` |
| `COMMUNITY_MENTIONED` | 有人在评论中 @ 我 | `postId, commentId, fromUserId` |
| `COMMUNITY_FOLLOWED_NEW_POST` | 我关注的人发了新帖 | `postId, fromUserId` |

去重策略: 同一 `(toUser, fromUser, type, postId)` 在 1 分钟内只产生一条通知。取消点赞不撤回已发通知。

---

## 4. 后端设计

### 4.1 Service 拆分

当前 `CommunityService` 已承载帖子 + 评论。本次拆为 7 个职责清晰的 Service:

| Service | 核心职责 | 预估行数 |
|---------|---------|---------|
| `CommunityCategoryService` | 版块 CRUD、启停、`post_count` 维护 | ~120 |
| `CommunityPostService` | 帖子 CRUD、列表(三种排序)、详情、`view_count` 自增、计数同步、作者帖子列表 | ~250 |
| `CommunityCommentService` | 楼层发布/删除(软删)、楼中楼发布/删除、楼层分页(含跳页)、楼中楼分页、只看楼主、@ 解析触发、计数同步 | ~280 |
| `CommunityLikeService` | 通用点赞/取消(帖子+评论)、幂等、计数同步、通知触发 | ~120 |
| `CommunityFavoriteService` | 收藏/取消、我的收藏分页列表、计数同步 | ~80 |
| `CommunityFollowService` | 关注/取关、粉丝列表、关注列表、批量关注状态查询、关注 feed、新帖广播触发 | ~150 |
| `CommunityNotificationDispatcher` | 封装"发什么类型 → 调通知系统"、1 分钟去重、新帖广播(阈值: ≤200 同步, >200 `@Async`) | ~120 |

工具类: `CommunityMentionParser` (从纯文本解析 `@昵称` → 用户列表,~60 行)。

### 4.2 关键算法

#### 楼号生成

```
@Transactional
addFloor(postId, content, images):
  post = postRepo.findById(postId) WITH PESSIMISTIC_WRITE lock  // SELECT ... FOR UPDATE
  maxFloor = SELECT COALESCE(MAX(floor_no), 0) FROM community_comments
             WHERE post_id = ? AND floor_no IS NOT NULL
  newFloor = maxFloor + 1
  INSERT comment(floor_no=newFloor, root_comment_id=NULL, ...)
  UPDATE community_posts SET comment_count = comment_count + 1, last_active_at = NOW()
  RETURN comment
```

并发冲突时(FOR UPDATE 锁或 uk_post_floor 冲突): 抛出 `BusinessException(409)`,响应"当前评论较多,请稍后再试",前端自动重试 1 次。

#### 楼中楼回复

```
@Transactional
addSubReply(postId, replyToCommentId, content):
  target = commentRepo.findById(replyToCommentId)
  rootId = target.floor_no != null ? target.id : target.root_comment_id
  INSERT comment(floor_no=NULL, root_comment_id=rootId, reply_to_comment_id=replyToCommentId, ...)
  UPDATE community_posts SET comment_count = comment_count + 1, last_active_at = NOW()
```

#### @ 提及解析

- 正则 `@([^\s@]{1,20})` 从评论纯文本中抽取候选昵称
- `findByNicknameIn(...)` 批量查回用户列表(防 N+1)
- 匹配成功的写 `community_comment_mentions` + 触发 `COMMUNITY_MENTIONED` 通知
- 未匹配的昵称静默保留为文本,不报错

#### 浏览量防刷

- 详情接口内: `INSERT IGNORE INTO community_post_view_logs(post_id, viewer_key, viewed_on)`
- 受影响行数 = 1 时,再 `UPDATE community_posts SET view_count = view_count + 1`
- `viewer_key`: 登录用户 = `u:{userId}`, 游客 = `ip:{sha1前8位}`
- 浏览量自增 `@Async`,失败静默吞掉

#### 计数维护

所有计数更新走 JPQL: `UPDATE ... SET xx_count = xx_count ± 1 WHERE id = ?`,避免 "读-改-写" 竞态。

### 4.3 REST API

原有路由保持在 `/api/community` 下。

#### 版块

| Method | Path | 权限 | 说明 |
|--------|------|------|------|
| GET | `/api/community/categories` | 公开 | 已启用版块列表,带 `post_count` |
| GET | `/api/admin/community/categories` | ADMIN | 全部(含禁用) |
| POST | `/api/admin/community/categories` | ADMIN | 新建 |
| PUT | `/api/admin/community/categories/{id}` | ADMIN | 修改(改名/图标/简介/排序/启停) |

#### 帖子

| Method | Path | 权限 | 变化 |
|--------|------|------|------|
| GET | `/api/community/posts` | 公开 | 新增 query: `categoryId`, `sort=latest_active\|created\|hot`, `authorId` |
| GET | `/api/community/posts/{id}` | 公开 | 触发 view_count; 响应带 `liked`, `favorited`(登录态) |
| POST | `/api/community/posts` | 登录 | body 必须含 `categoryId` |
| GET | `/api/community/users/{id}/posts` | 公开 | 某用户发的帖 |
| GET | `/api/community/feed/following` | 登录 | 关注的人的最新帖 |

#### 评论 (新版接口,评论改动较大)

| Method | Path | 权限 | 说明 |
|--------|------|------|------|
| GET | `/api/community/posts/{id}/floors` | 公开 | 楼层分页(page/size), query: `onlyAuthor`、`order=asc\|desc`; 每楼附带前 3 条楼中楼 |
| GET | `/api/community/comments/{floorId}/replies` | 公开 | 某楼层的楼中楼分页(page/size) |
| POST | `/api/community/posts/{id}/floors` | 登录 | 发一级楼层 |
| POST | `/api/community/comments/{floorId}/replies` | 登录 | 楼中楼; body: `replyToCommentId`, `content`, `imageUrls` |
| DELETE | `/api/community/comments/{id}` | 本人/ADMIN | 软删(置 DELETED,内容替换为"该评论已删除"),楼号保留,计数减 1 |

> 兼容: 旧 `POST /posts/{id}/comments` 路径保留,内部 fallthrough 到"发一级楼层",前端迁移完成后下线。

#### 点赞 / 收藏

| Method | Path | 权限 |
|--------|------|------|
| POST | `/api/community/likes` body=`{targetType, targetId}` | 登录,幂等 |
| DELETE | `/api/community/likes` body=`{targetType, targetId}` | 登录,幂等 |
| POST | `/api/community/posts/{id}/favorite` | 登录,幂等 |
| DELETE | `/api/community/posts/{id}/favorite` | 登录,幂等 |
| GET | `/api/community/mine/favorites` | 登录,分页 |

#### 关注

| Method | Path | 权限 |
|--------|------|------|
| POST | `/api/community/follows/{userId}` | 登录,幂等 |
| DELETE | `/api/community/follows/{userId}` | 登录,幂等 |
| GET | `/api/community/users/{id}/followers` | 公开,分页 |
| GET | `/api/community/users/{id}/following` | 公开,分页 |
| GET | `/api/community/mine/follow-status?userIds=1,2,3` | 登录,批量查 |

#### 用户搜索 (供 @ 使用)

| Method | Path | 权限 |
|--------|------|------|
| GET | `/api/users/search?keyword=...&limit=10` | 登录 |

> 如果此接口已存在则复用;不存在则新建,按 nickname 前缀模糊匹配。

### 4.4 关键 DTO 字段

`CommunityPostResponse` (列表) 新增:
```
categoryId, categoryName,
viewCount, likeCount, commentCount, favoriteCount,
liked, favorited, authorFollowed,
lastActiveAt
```

`CommunityFloorResponse` (楼层):
```
id, floorNo, content, imageUrls,
authorId, authorNickname, authorAvatarUrl, authorRoleText,
createdAt, status,
likeCount, liked,
isPostAuthor, replyCount,
topReplies: List<CommunityReplyResponse>  // 前 3 条楼中楼
```

`CommunityReplyResponse` (楼中楼):
```
id, content, imageUrls,
authorId, authorNickname, authorAvatarUrl, authorRoleText,
replyToUserId, replyToUserNickname,
createdAt, status,
likeCount, liked,
mentions: List<{userId, nickname}>
```

---

## 5. 前端设计

### 5.1 路由

| 路径 | 页面 | 说明 |
|------|------|------|
| `/community` | `CommunityHomeView.vue` | 社区首页: 版块网格 + 全站帖流(tab: 最新/最热/关注) + 发帖按钮 |
| `/community/c/:code` | `CommunityCategoryView.vue` | 单版块帖流 + 发帖按钮(版块锁定) |
| `/community/posts/:id` | `CommunityPostDetailView.vue` | 帖子详情 + 楼层 + 跳页(支持 `?page=N` 和 `?floor=N`) |
| `/community/users/:id` | `UserProfileView.vue`(扩展) | 加 tab: ta的帖子 / 关注 / 粉丝 / 收藏(本人可见) |
| `/community/mine/favorites` | `MyFavoritesView.vue` | 我的收藏 |
| `/admin/community/categories` | `AdminCategoriesView.vue` | 管理员维护版块 |

> `/community/:id` 旧详情路径做路由 alias 到 `/community/posts/:id`,避免老链接 404。

### 5.2 新组件 (放在 `src/components/community/`)

| 组件 | 职责 |
|------|------|
| `CategoryGrid` | 版块卡片网格 |
| `PostCard` | 帖子卡(抽出现有逻辑,加点赞/收藏/浏览数) |
| `PostMeta` | 作者行 + 关注按钮 |
| `FloorList` | 楼层列表 + 分页 + 跳页 + 只看楼主/倒序切换 |
| `FloorItem` | 单个楼层(楼号/内容/点赞/回复入口/楼中楼前3条) |
| `ReplyList` | 楼中楼列表(展开/折叠/分页) |
| `ReplyItem` | 单条楼中楼("A 回复 B"格式) |
| `CommentEditor` | 评论输入框(纯文本 + 表情 + 图片 + @ 触发) |
| `MentionPopover` | 输入 `@` 时弹出的用户搜索浮层(300ms debounce) |
| `LikeButton` | 通用点赞按钮,乐观更新 |
| `FavoriteButton` | 收藏按钮 |
| `FollowUserButton` | 关注用户按钮(不复用现有 `FollowButton`,那是救助站的) |
| `MentionText` | 渲染含 @ 的文本,用 `mentions` 列表精确替换为可点击链接 |

### 5.3 关键交互

#### 社区首页 (`/community`)

- 顶部: `CategoryGrid` — 6 张版块卡片,点击进入版块页
- Tab: 最新(按 `last_active_at` 倒序) / 最热(按 `like_count + comment_count * 2`, 7 日内加权) / 关注(登录态)
- "关注" tab 空时,引导"去看看版块"
- 右上角"发帖"按钮 → 弹窗,必选版块下拉

#### 帖子详情布局

```
┌─ 帖子主体 ───────────────────────────────────┐
│  [版块] 标题                                    │
│  作者行                                         │
│  正文 + 图片                                    │
│  [👍 12] [⭐ 3] [👁 234]                        │
│  作者行右侧: [关注] / [已关注]                    │
└───────────────────────────────────────────────┘

┌─ 评论编辑器 ───────────────────────────────────┐
│  [纯文本 + 表情 + 图片 + @]                      │
└───────────────────────────────────────────────┘

┌─ 楼层列表 ─────────────────────────────────────┐
│  全 56 楼  [只看楼主] [倒序] [跳到第 N 楼]       │
│  ── 1L ── 张三 ── 时间                          │
│  内容 + [👍 5] [回复]                           │
│   ┗ 李四 回复 张三: xxx                          │
│   ┗ 王五 回复 李四: yyy                          │
│   ┗ [展开剩余 5 条]                              │
│  ── 2L ── ...                                  │
│  分页: ‹ 1 2 3 ... 6 ›  [跳至 __ 页]            │
└───────────────────────────────────────────────┘
```

#### 跳页

- URL 支持 `?page=N` 或 `?floor=N`(优先 floor)
- `?floor=37` → 后端算 `page = ceil(37 / 10)`,前端定位锚点 `#L37` 并高亮 1.5s
- 通知跳转统一使用 `?floor=N`

#### @ 提及

- 编辑器输入 `@` → `MentionPopover` 出现
- 继续输入时 300ms debounce 调 `/api/users/search`
- 选中后文本插入 `@昵称 `(末尾带空格),popover 关闭
- 提交: 前端纯文本提交; 后端解析 `@昵称` → 匹配 → 建 mention + 通知
- 渲染: `MentionText` 用后端返回 `mentions` 列表精确替换,避免正文中 `@` 字符误识别

#### 优化

- 点赞/收藏/关注都做**乐观更新**: UI 立即反映,失败回滚 + toast
- 列表页数据走页面级 `ref`,不做全局缓存(版块列表可选入 store)

### 5.4 风格规范 (强约束)

- 色板: 只使用 `UI-DESIGN.md` 中定义的全部 CSS 变量,**不新增颜色**
- 卡片: 沿用 `lift-card`, 圆角 8px, 统一阴影
- 按钮: Element Plus + Lucide 图标,**不引入新图标库**
- 入场动画: 沿用 `reveal-on-scroll`(列表项) 和 `.page` 全局淡入
- 响应式: 沿用 `clamp()` 流式排版,不新增断点
- 字体: 沿用 Inter / 微软雅黑 / 苹方栈

### 5.5 i18n 关键 Key

```
community.category.{adoption|medical|lost|rescue|dailylife|chat}.{name|desc}
community.tab.{latest|hot|following}
community.post.{like|favorite|view|comment|follow|followed}
community.floor.{floorNo|onlyAuthor|reverse|jumpTo|expand|collapse}
community.reply.{replyTo|placeholder|empty}
community.mention.{searchPlaceholder|noResult}
community.notify.{commented|replied|postLiked|commentLiked|mentioned|followingNewPost}
community.empty.{following|favorites|category}
admin.community.category.{title|new|edit|enable|disable|code|icon|sortOrder}
```

---

## 6. 错误处理

| 场景 | HTTP | 行为 |
|------|------|------|
| 版块不存在或已禁用 | 400 | 返回明确错误信息 |
| 楼号并发冲突 | 409 | 前端自动重试 1 次 |
| 重复点赞/收藏/关注 | 200 | 服务端幂等,返回当前状态,不报错 |
| 取消未点赞/未收藏 | 200 | 幂等返回 |
| 关注自己 | 400 | "不能关注自己" |
| 内容不存在或已删除 | 404 | "内容不存在或已删除" |
| 楼中楼 `replyTo` 不在本帖 | 400 | "回复目标无效" |
| 跳页 page 越界 | 200 | 自动收敛到最后一页,不报错 |
| view_count 自增失败 | — | 静默吞掉,不阻塞详情主响应 |
| 通知发送失败 | — | 静默吞掉 + 写日志 |
| @ 解析失败的昵称 | — | 静默保留为文本 |

计数同步提供管理员对账接口: `POST /api/admin/community/recount?postId=X`(人工触发)。定时对账脚本记 follow-up。

---

## 7. 测试策略

### 后端单元测试 (Spring Boot Test + H2)

最小覆盖集:

- `CommunityCategoryServiceTest` — 增改启停、计数同步
- `CommunityCommentServiceTest` — 楼号连续递增、20 并发不重号(CountDownLatch)、root 推导、软删保留楼号
- `CommunityLikeServiceTest` — 重复点赞幂等、计数同步、跨 target_type 隔离
- `CommunityFavoriteServiceTest` — 幂等 + 计数
- `CommunityFollowServiceTest` — 不能关注自己、幂等、双向状态查询
- `CommunityMentionParserTest` — 中文昵称/带空格/`@@`/未匹配等边界
- `CommunityPostServiceTest` — view_count 防刷(同一 viewer 同日只 +1)、三种排序

### 后端集成测试 (@SpringBootTest + MockMvc + H2)

一条完整旅程: 登录 → 发帖 → 发楼层 → 发楼中楼 → @ 某人 → 点赞 → 收藏 → 关注作者 → 验证通知列表包含 6 类通知。

### 前端

本期不引入 Vitest。在 spec 附录写入 10 条手动 QA 用例清单。

---

## 8. 实施顺序 (每 phase 独立可上线)

| Phase | 内容 | 后端核心产出 | 前端核心产出 |
|-------|------|-------------|-------------|
| **0** | 数据迁移 + 种子 | `DataInitializer` 幂等脚本: 补 category_id、建 6 个默认版块 | — |
| **1** | 版块 | `Category` 实体/repo/service/接口 + 管理员接口 | `CategoryGrid` + `AdminCategoriesView` + 发帖弹窗版块下拉 |
| **2** | 点赞 + 收藏 + 浏览量 | `Like` + `Favorite` + `ViewLog` 三张表 + 计数同步 + 接口 | `LikeButton` + `FavoriteButton` + 详情页 view_count |
| **3** | 楼层重构 + 跳页 + 只看楼主 + 倒序 | 楼号生成 + 楼层/楼中楼接口 + 软删 | `FloorList`/`FloorItem`/`ReplyList`/`ReplyItem` + 跳页转楼层 |
| **4** | 评论点赞 + @ 提及 | 评论 like 接通用 like + Mention 表 + 解析器 + 通知 | `CommentEditor` + `MentionPopover` + `MentionText` |
| **5** | 关注用户 + 关注 feed + 新帖广播 | `CommunityUserFollow` + 接口 + 广播 dispatcher | `FollowUserButton` + 首页"关注"tab + 用户主页 followers/following |
| **6** | 6 类通知 + 去重 | `CommunityNotificationDispatcher` 收口 + 1 分钟去重 | 通知列表识别新 type + 跳转 |
| **7** | 收尾 | explain 索引检查 | i18n 校对 + 旧路由 alias |

每个 phase 可单独 PR / 单独上线,不互相阻塞。

---

## 9. 风险与缓解

| 风险 | 缓解 |
|------|------|
| 楼号并发冲突 → 用户偶发 409 | `FOR UPDATE` + 前端自动重试 1 次 |
| 计数漂移(冗余字段长期跑偏) | 管理员手动对账接口; 后续加定时任务(follow-up) |
| 新帖广播阻塞(粉丝量大) | `@Async` + `>200` 阈值切异步 |
| `view_log` 表膨胀 | 唯一索引限制 + follow-up 加 30 天清理 |
| `parent_comment_id` 旧字段混淆 | spec 明确 deprecated; 迁移脚本一次性迁语义 |
| 旧 API 被缓存调用 | 旧 `POST /posts/{id}/comments` 保留, 内部 fallthrough, 一个版本后下线 |
| 视觉风格漂移 | 强约束写入 spec 第 5.4 节; 实施时只引用现有 CSS 变量 |

---

## 10. 附录: 手动 QA 用例

1. 未登录用户: 浏览社区首页 → 看到 6 个版块 → 点入版块看帖子列表 → 点帖子看详情和楼层 → 不能发帖/评论/点赞
2. 登录用户: 发帖(选版块) → 帖子出现在版块页和首页 → 自己发楼层 → 楼号 = 2L → 发楼中楼 → "A 回复 B"格式正确
3. 同一用户: 对帖子点赞 → 计数 +1 → 再次点赞(取消) → 计数 -1 → 对评论点赞同理
4. 用户 A 收藏帖子 → 收藏列表出现 → 取消收藏 → 列表移除
5. 用户 A 关注用户 B → A 的关注列表有 B,B 的粉丝列表有 A → A 不可关注自己
6. 用户 B 发帖 → 用户 A(关注了 B)的通知铃铛收到"关注的人发新帖"通知
7. 用户在评论中输入 `@小明` → 小明收到 @ 通知 → 点通知跳转到对应帖子的对应楼层并高亮
8. 帖子详情: 点"只看楼主"只显示楼主楼层 → 点"倒序"最后一个楼层变 1L → 输入"跳至 5 页"加载第 5 页 → URL 更新为 `?page=5`
9. 评论被删除 → 内容显示"该评论已删除" → 楼号不变 → 计数 -1
10. 管理员: 进入版块管理 → 新增版块 → 修改版块名/介绍 → 禁用某版块 → 禁用后用户不可在该版块发帖
