# Bug 修复记录 — 2026-05-12

## 概述

本次修复了前后端共计两类 Bug：前端 Vue 组件渲染崩溃、后端 Spring Boot 接口 500。涉及三个阶段排查。

---

## 第一阶段：前端页面报错崩溃

### 错误现象

| 页面   | Console 报错                                                                |
| ---- | ------------------------------------------------------------------------- |
| 动物档案 | `Cannot destructure property 'slots' of 'undefined'` (lucide-vue-next.js) |
| 救助信息 | `Cannot read properties of null (reading 'parentNode')` (Vue runtime)     |
| 个人中心 | `Cannot read properties of null (reading 'nextSibling')` (Vue runtime)    |

### 排查过程

1. **误判**：怀疑 `AppShell.vue` 中 `<Transition mode="out-in">` 包裹 `<RouterView>` 导致异步数据加载时 DOM 不一致。移除 Transition 后问题依旧，排除。
2. **定位**：逐个检查三个页面共用的组件，发现 `EmptyState.vue` 的 `icon` prop 默认值直接写了 lucide 函数式组件 `SearchX`。

### 根因

Vue 3.5 初始化 prop 默认值时调用 `defaultValue.call(null, props)`，只传一个参数。但 `SearchX` 是 lucide 通过 `createLucideIcon` 生成的函数式组件，签名为 `(props, { slots }) => h(...)`（需要两个参数）。`{ slots }` 对 `undefined` 解构时抛出 TypeError。

连锁反应：

- **动物档案**：首次挂载时 `animals` 为空，`EmptyState` 同步渲染 → 直接触发
- **救助信息 / 个人中心**：异步数据加载后的更新周期中 `EmptyState` 渲染 → 报错损坏组件树 → 后续 DOM diff 访问 null 节点 → `parentNode` / `nextSibling` 报错

### 修复

`Frontend/src/components/EmptyState.vue` — prop 默认值从组件引用改为工厂函数：

```js
// 修复前
icon: { type: [Object, Function], default: SearchX }

// 修复后
icon: { type: [Object, Function], default: () => SearchX }
```

Vue 调用工厂函数 `() => SearchX` 时只传一个参数即可得到组件引用本身，不会触发 lucide 的函数式组件调用逻辑。

### 附带修复

`Frontend/src/views/AnimalsView.vue` — 补充缺失的 `notifyError` 导入（`submit()` 中调用但未导入）。

---

## 第二阶段：后端接口 500 错误（第一轮修复失败）

### 错误现象

用户登录后发布救助信息和动物档案，管理员审核通过后：

- 点击"个人中心"报 500：`GET /api/users/me/animals`、`GET /api/users/me/rescues`
- 审核通过的信息未公开显示

### 排查过程

定位到 `Animal.java` 和 `Rescue.java` 中的 `imageUrls` 字段使用了 `@ElementCollection` + `List<String>`，缺少 `@OrderColumn`。怀疑是 Hibernate 6.x 对 `List` 类型的 bag 集合懒加载存在兼容问题。

### 第一轮修复（失败）

给两个实体的 `@ElementCollection` 加了 `@OrderColumn(name = "image_order")`，期望 `ddl-auto: update` 自动加列。

**结果：失败，反而所有列表接口全面 500。**

### 失败原因

Hibernate 的 `@OrderColumn` 默认 `nullable = false` 且会作为主键的一部分。对已有数据的表执行 `ALTER TABLE ADD COLUMN image_order INT NOT NULL` 时，因为现有行无默认值而失败/破坏表结构，导致整个查询链路崩溃。

---

## 第三阶段：后端最终修复

### 正确方案

去掉 `@OrderColumn`，改为 `FetchType.EAGER`，绕过 Hibernate 6.5 的 `List<String>` 懒加载 bug：

`Backend/.../domain/Animal.java`：

```java
// 修复后
@ElementCollection(fetch = FetchType.EAGER)
@CollectionTable(name = "animal_images", joinColumns = @JoinColumn(name = "animal_id"))
@Column(name = "image_url", length = 500)
private List<String> imageUrls = new ArrayList<>();
```

`Backend/.../domain/Rescue.java` — 同上。

### 额外改进

`Backend/.../exception/GlobalExceptionHandler.java` — 在兜底 `@ExceptionHandler(Exception.class)` 中加入 `log.error("Unhandled exception", ex)`，后续未捕获异常会打印完整堆栈到控制台。

### 数据库清理

由于第一轮 `@OrderColumn` 可能损坏了 `animal_images` 和 `rescue_images` 表结构，需要手动清理后重启：

```sql
DROP TABLE IF EXISTS animal_images;
DROP TABLE IF EXISTS rescue_images;
DELETE FROM animals;
DELETE FROM rescues;
```

重启后端后 `ddl-auto: update` 会按正确结构重建这两张表。

---

## 涉及文件汇总

| 文件                                                  | 操作  | 说明                                     |
| --------------------------------------------------- | --- | -------------------------------------- |
| `Frontend/src/components/EmptyState.vue`            | 修改  | 根因：prop 默认值改为工厂函数                      |
| `Frontend/src/components/AppShell.vue`              | 修改  | 移除 Transition（简化代码）                    |
| `Frontend/src/assets/styles.css`                    | 修改  | 清理 page transition CSS                 |
| `Frontend/src/views/AnimalsView.vue`                | 修改  | 补充 notifyError 导入                      |
| `Backend/.../domain/Animal.java`                    | 修改  | `@ElementCollection` 加 `fetch = EAGER` |
| `Backend/.../domain/Rescue.java`                    | 修改  | `@ElementCollection` 加 `fetch = EAGER` |
| `Backend/.../exception/GlobalExceptionHandler.java` | 修改  | 补充异常日志                                 |
