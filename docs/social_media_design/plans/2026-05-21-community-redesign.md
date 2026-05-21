# 社区交流板块完善 — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将社区交流从单流帖子+二级评论升级为贴吧式分版块、强互动、楼层化讨论的社区。

**Architecture:** 在现有 Spring Boot 分层 + Vue 3 结构下原位扩展。新增 6 张表 + 修改 2 张现有表，拆分 CommunityService 为 7 个职责清晰的 Service，前端新建 17 个组件 + 重写 2 个核心页面。7 个 Phase 各为独立可上线单元。

**Tech Stack:** Spring Boot 3.3.5, Spring Data JPA, H2/MySQL, Vue 3, Element Plus, Lucide icons, Vue I18n

**Spec:** `docs/superpowers/specs/2026-05-21-community-redesign.md`

---

### Task 1: Phase 0 — 数据迁移脚本与种子版块

**Files:**
- Create: `backend/src/main/java/com/guitu/domain/CommunityCategory.java`
- Create: `backend/src/main/java/com/guitu/repository/CommunityCategoryRepository.java`
- Modify: `backend/src/main/java/com/guitu/config/DataInitializer.java`

- [ ] **Step 1: 创建 CommunityCategory 实体**

```java
package com.guitu.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "community_categories")
public class CommunityCategory extends BaseEntity {
    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 64)
    private String nameEn;

    @Column(length = 255)
    private String description;

    @Column(length = 255)
    private String icon;

    @Column(nullable = false)
    private int sortOrder = 0;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private long postCount = 0;
}
```

- [ ] **Step 2: 创建 CommunityCategoryRepository**

```java
package com.guitu.repository;

import com.guitu.domain.CommunityCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CommunityCategoryRepository extends JpaRepository<CommunityCategory, Long> {
    List<CommunityCategory> findByEnabledTrueOrderBySortOrderAsc();
    Optional<CommunityCategory> findByCode(String code);
    boolean existsByCode(String code);
}
```

- [ ] **Step 3: 在 DataInitializer 中加种子版块**

在 `DataInitializer.java` 的 `run()` 方法末尾追加:

```java
// Inject CommunityCategoryRepository
private final CommunityCategoryRepository categoryRepository;

// Add to constructor parameter list
public DataInitializer(
    UserRepository userRepository,
    RescueStationRepository stationRepository,
    PasswordEncoder passwordEncoder,
    CommunityCategoryRepository categoryRepository
) { ... this.categoryRepository = categoryRepository; }

// In run() method, add:
if (categoryRepository.count() == 0) {
    CommunityCategory c1 = new CommunityCategory();
    c1.setCode("adoption"); c1.setName("领养经验"); c1.setNameEn("Adoption");
    c1.setDescription("分享领养流程、经验与心得"); c1.setIcon("HeartHandshake"); c1.setSortOrder(1);
    categoryRepository.save(c1);

    CommunityCategory c2 = new CommunityCategory();
    c2.setCode("medical"); c2.setName("医疗护理"); c2.setNameEn("Medical");
    c2.setDescription("宠物健康、疾病防治与护理知识"); c2.setIcon("Stethoscope"); c2.setSortOrder(2);
    categoryRepository.save(c2);

    CommunityCategory c3 = new CommunityCategory();
    c3.setCode("lost"); c3.setName("寻宠送养"); c3.setNameEn("Lost & Found");
    c3.setDescription("发布走失信息或寻找新主人"); c3.setIcon("Search"); c3.setSortOrder(3);
    categoryRepository.save(c3);

    CommunityCategory c4 = new CommunityCategory();
    c4.setCode("rescue"); c4.setName("救助求助"); c4.setNameEn("Rescue");
    c4.setDescription("发布或响应救助请求"); c4.setIcon("Siren"); c4.setSortOrder(4);
    categoryRepository.save(c4);

    CommunityCategory c5 = new CommunityCategory();
    c5.setCode("dailylife"); c5.setName("日常晒宠"); c5.setNameEn("Daily Life");
    c5.setDescription("分享你家宠物的日常照片和趣事"); c5.setIcon("Camera"); c5.setSortOrder(5);
    categoryRepository.save(c5);

    CommunityCategory c6 = new CommunityCategory();
    c6.setCode("chat"); c6.setName("闲聊灌水"); c6.setNameEn("Chat");
    c6.setDescription("随便聊聊，但请保持友善"); c6.setIcon("MessageCircle"); c6.setSortOrder(6);
    categoryRepository.save(c6);
}
```

- [ ] **Step 4: 重启后端验证**

```bash
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
```

Expected: 启动日志无报错，H2 数据库自动建 `community_categories` 表并插入 6 行种子数据。

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/guitu/domain/CommunityCategory.java \
        backend/src/main/java/com/guitu/repository/CommunityCategoryRepository.java \
        backend/src/main/java/com/guitu/config/DataInitializer.java
git commit -m "feat: add community categories entity and seed data"
```

---

### Task 2: Phase 1a — 版块后端 API

**Files:**
- Create: `backend/src/main/java/com/guitu/service/CommunityCategoryService.java`
- Create: `backend/src/main/java/com/guitu/controller/CommunityCategoryController.java`
- Modify: `backend/src/main/java/com/guitu/dto/CommunityDtos.java`

- [ ] **Step 1: 在 CommunityDtos 中添加版块 DTO**

在 `CommunityDtos.java` 末尾追加:

```java
public record CategoryResponse(
    Long id, String code, String name, String nameEn,
    String description, String icon, int sortOrder,
    boolean enabled, long postCount
) {}

public record SaveCategoryRequest(
    @NotBlank String code,
    @NotBlank String name,
    String nameEn,
    String description,
    String icon,
    int sortOrder,
    boolean enabled
) {}
```

- [ ] **Step 2: 创建 CommunityCategoryService**

```java
package com.guitu.service;

import com.guitu.domain.CommunityCategory;
import com.guitu.dto.CommunityDtos.*;
import com.guitu.exception.BusinessException;
import com.guitu.repository.CommunityCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class CommunityCategoryService {
    private final CommunityCategoryRepository repo;

    public CommunityCategoryService(CommunityCategoryRepository repo) { this.repo = repo; }

    public List<CategoryResponse> listEnabled() {
        return repo.findByEnabledTrueOrderBySortOrderAsc().stream()
            .map(this::toResponse).toList();
    }

    public List<CategoryResponse> listAll() {
        return repo.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public CategoryResponse create(SaveCategoryRequest req) {
        if (repo.existsByCode(req.code()))
            throw new BusinessException("版块代码已存在");
        CommunityCategory c = new CommunityCategory();
        apply(c, req);
        return toResponse(repo.save(c));
    }

    @Transactional
    public CategoryResponse update(Long id, SaveCategoryRequest req) {
        CommunityCategory c = repo.findById(id)
            .orElseThrow(() -> new BusinessException("版块不存在"));
        apply(c, req);
        return toResponse(repo.save(c));
    }

    public CommunityCategory getEntity(Long id) {
        return repo.findById(id)
            .orElseThrow(() -> new BusinessException("版块不存在"));
    }

    private void apply(CommunityCategory c, SaveCategoryRequest req) {
        c.setCode(req.code()); c.setName(req.name()); c.setNameEn(req.nameEn());
        c.setDescription(req.description()); c.setIcon(req.icon());
        c.setSortOrder(req.sortOrder()); c.setEnabled(req.enabled());
    }

    private CategoryResponse toResponse(CommunityCategory c) {
        return new CategoryResponse(c.getId(), c.getCode(), c.getName(), c.getNameEn(),
            c.getDescription(), c.getIcon(), c.getSortOrder(), c.isEnabled(), c.getPostCount());
    }
}
```

- [ ] **Step 3: 创建版块 Controller**

```java
package com.guitu.controller;

import com.guitu.common.ApiResponse;
import com.guitu.dto.CommunityDtos.*;
import com.guitu.service.CommunityCategoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class CommunityCategoryController {
    private final CommunityCategoryService service;

    public CommunityCategoryController(CommunityCategoryService service) { this.service = service; }

    @GetMapping("/api/community/categories")
    public ApiResponse<List<CategoryResponse>> listEnabled() {
        return ApiResponse.ok(service.listEnabled());
    }

    @GetMapping("/api/admin/community/categories")
    public ApiResponse<List<CategoryResponse>> listAll() {
        return ApiResponse.ok(service.listAll());
    }

    @PostMapping("/api/admin/community/categories")
    public ApiResponse<CategoryResponse> create(@Valid @RequestBody SaveCategoryRequest req) {
        return ApiResponse.ok(service.create(req));
    }

    @PutMapping("/api/admin/community/categories/{id}")
    public ApiResponse<CategoryResponse> update(@PathVariable Long id, @Valid @RequestBody SaveCategoryRequest req) {
        return ApiResponse.ok(service.update(id, req));
    }
}
```

- [ ] **Step 4: 测试版块 API**

```bash
# 启动后测试
curl http://localhost:8080/api/community/categories
```

Expected: 返回 6 个版块的 JSON 数组。

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/guitu/service/CommunityCategoryService.java \
        backend/src/main/java/com/guitu/controller/CommunityCategoryController.java \
        backend/src/main/java/com/guitu/dto/CommunityDtos.java
git commit -m "feat: add community category CRUD API"
```

---

### Task 3: Phase 1b — CommunityPost 加 category 字段并迁移

**Files:**
- Modify: `backend/src/main/java/com/guitu/domain/CommunityPost.java`
- Modify: `backend/src/main/java/com/guitu/service/CommunityService.java`
- Modify: `backend/src/main/java/com/guitu/dto/CommunityDtos.java`
- Modify: `backend/src/main/java/com/guitu/config/DataInitializer.java`

- [ ] **Step 1: CommunityPost 加 category 字段**

在 `CommunityPost.java` 中添加:

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "category_id")
private CommunityCategory category;
```

- [ ] **Step 2: 迁移存量帖子到默认版块**

在 `DataInitializer.run()` 末尾(社区版块种子之后)追加迁移逻辑:

```java
// Migrate existing posts without category to default "chat" category
List<CommunityPost> orphanPosts = postRepository.findAll().stream()
    .filter(p -> p.getCategory() == null).toList();
if (!orphanPosts.isEmpty()) {
    CommunityCategory defaultCategory = categoryRepository.findByCode("chat").orElseThrow();
    for (CommunityPost post : orphanPosts) {
        post.setCategory(defaultCategory);
    }
    postRepository.saveAll(orphanPosts);
    log.info("Migrated {} orphan posts to default category 'chat'", orphanPosts.size());
}
```

`DataInitializer` 需注入 `CommunityPostRepository` 和 `Logger`:

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.guitu.repository.CommunityPostRepository;

private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
private final CommunityPostRepository postRepository;
```

- [ ] **Step 3: CommunityDtos 中帖子的 DTO 加 category 字段**

`SavePostRequest` — 添加:
```java
@NotNull Long categoryId
```

`CommunityPostResponse`(record) — 添加:
```java
Long categoryId, String categoryCode, String categoryName
```

- [ ] **Step 4: 更新 CommunityService.create() 和 update() 以关联 category**

`create()` 方法中添加:
```java
post.setCategory(categoryService.getEntity(request.categoryId()));
```

`update()` 同理。`CommunityService` 注入 `CommunityCategoryService`。

`toPostResponse()` 映射中添加:
```java
post.getCategory() != null ? post.getCategory().getId() : null,
post.getCategory() != null ? post.getCategory().getCode() : null,
post.getCategory() != null ? post.getCategory().getName() : null,
```

- [ ] **Step 5: 重启验证**

```bash
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
curl http://localhost:8080/api/community/posts
```

Expected: 存量帖子自动带 `categoryId`/`categoryCode`/`categoryName`。

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/guitu/domain/CommunityPost.java \
        backend/src/main/java/com/guitu/service/CommunityService.java \
        backend/src/main/java/com/guitu/dto/CommunityDtos.java \
        backend/src/main/java/com/guitu/config/DataInitializer.java
git commit -m "feat: add category field to posts with migration"
```

---

### Task 4: Phase 1c — 前端版块网格 + 管理员版块页 + 发帖选版块

**Files:**
- Create: `frontend/src/components/community/CategoryGrid.vue`
- Create: `frontend/src/views/AdminCategoriesView.vue`
- Modify: `frontend/src/views/CommunityView.vue`
- Modify: `frontend/src/router/index.js`
- Modify: `frontend/src/api/index.js`
- Modify: `frontend/src/i18n/zh.js`
- Modify: `frontend/src/i18n/en.js`

- [ ] **Step 1: 前端 API 加版块接口**

在 `api/index.js` 中添加:

```javascript
export const categoryApi = {
  list: () => http.get('/community/categories'),
  listAll: () => http.get('/admin/community/categories'),
  create: (data) => http.post('/admin/community/categories', data),
  update: (id, data) => http.put(`/admin/community/categories/${id}`, data)
}
```

- [ ] **Step 2: 创建 CategoryGrid 组件**

```vue
<!-- frontend/src/components/community/CategoryGrid.vue -->
<template>
  <div class="category-grid">
    <RouterLink v-for="cat in categories" :key="cat.id"
      :to="`/community/c/${cat.code}`" class="category-card lift-card">
      <div class="category-icon">
        <component :is="iconMap[cat.icon] || MessageCircle" :size="24" />
      </div>
      <div class="category-info">
        <strong>{{ locale === 'zh' ? cat.name : (cat.nameEn || cat.name) }}</strong>
        <span class="muted">{{ cat.postCount }} {{ $t('community.postCount') }}</span>
      </div>
    </RouterLink>
  </div>
</template>

<script setup>
import { onMounted, ref, computed } from 'vue'
import { RouterLink } from 'vue-router'
import * as LucideIcons from 'lucide-vue-next'
import { categoryApi } from '../../api'
import { useI18n } from 'vue-i18n'

const { locale } = useI18n()
const categories = ref([])

const iconMap = Object.keys(LucideIcons).reduce((acc, key) => {
  if (key !== 'createLucideIcon' && key !== 'default') acc[key] = LucideIcons[key]
  return acc
}, {})

onMounted(async () => {
  try { categories.value = await categoryApi.list() } catch {}
})
</script>

<style scoped>
.category-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 16px; margin-bottom: 32px; }
.category-card { display: flex; align-items: center; gap: 12px; padding: 16px; cursor: pointer; text-decoration: none; color: var(--ink); transition: transform .2s ease; }
.category-card:hover { transform: translateY(-2px); }
.category-icon { width: 44px; height: 44px; border-radius: 12px; background: var(--primary); color: white; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.category-info { display: flex; flex-direction: column; gap: 2px; }
</style>
```

- [ ] **Step 3: 更新 CommunityView — 加版块网格 + 发帖弹窗版块下拉**

在 `CommunityView.vue` 的模板顶部 `.section-head` 之后插入 `<CategoryGrid />`。

发帖弹窗表单中加版块选择:

```html
<el-form-item :label="$t('community.category')" prop="categoryId">
  <el-select v-model="editor.categoryId" :placeholder="$t('community.selectCategory')">
    <el-option v-for="cat in categories" :key="cat.id" :label="locale === 'zh' ? cat.name : (cat.nameEn || cat.name)" :value="cat.id" />
  </el-select>
</el-form-item>
```

`editor` reactive 加 `categoryId: null`,rules 加 `categoryId: [{ required: true, message: '请选择版块', trigger: 'change' }]`。

`script setup` 中 import `CategoryGrid`,添加 `categories` ref,onMounted 中加载版块列表。

- [ ] **Step 4: 创建 AdminCategoriesView**

```vue
<!-- frontend/src/views/AdminCategoriesView.vue -->
<template>
  <section class="view page">
    <div class="section-head">
      <h1>版块管理</h1>
      <el-button :icon="Plus" type="primary" @click="openEditor()">新增版块</el-button>
    </div>
    <el-table :data="categories" stripe>
      <el-table-column prop="sortOrder" label="排序" width="60" />
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="code" label="代码" />
      <el-table-column prop="postCount" label="帖子数" />
      <el-table-column label="状态">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">
            {{ row.enabled ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button text size="small" @click="openEditor(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="editorVisible" :title="editingId ? '编辑版块' : '新增版块'" width="500px" append-to-body>
      <el-form ref="formRef" :model="editor" label-position="top">
        <el-form-item label="代码" prop="code">
          <el-input v-model="editor.code" :disabled="!!editingId" />
        </el-form-item>
        <el-form-item label="名称(中)" prop="name">
          <el-input v-model="editor.name" />
        </el-form-item>
        <el-form-item label="名称(英)">
          <el-input v-model="editor.nameEn" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="editor.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="图标(Lucide图标名)">
          <el-input v-model="editor.icon" placeholder="如 HeartHandshake" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="editor.sortOrder" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="editor.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editorVisible = false">取消</el-button>
        <el-button :loading="saving" type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from 'lucide-vue-next'
import { categoryApi } from '../api'

const categories = ref([])
const editorVisible = ref(false)
const editingId = ref(null)
const saving = ref(false)
const formRef = ref()

const editor = reactive({ code: '', name: '', nameEn: '', description: '', icon: '', sortOrder: 0, enabled: true })

async function load() { categories.value = await categoryApi.listAll() }

function openEditor(row = null) {
  editingId.value = row?.id || null
  if (row) Object.assign(editor, row)
  else Object.assign(editor, { code: '', name: '', nameEn: '', description: '', icon: '', sortOrder: 0, enabled: true })
  editorVisible.value = true
}

async function save() {
  saving.value = true
  try {
    if (editingId.value) {
      await categoryApi.update(editingId.value, editor)
      ElMessage.success('版块已更新')
    } else {
      await categoryApi.create(editor)
      ElMessage.success('版块已创建')
    }
    editorVisible.value = false
    await load()
  } catch (e) { ElMessage.error(e?.response?.data?.message || '操作失败') }
  finally { saving.value = false }
}

onMounted(load)
</script>
```

- [ ] **Step 5: 路由加版块页和管理员版块页**

在 `router/index.js` 中添加:

```javascript
{ path: '/community/c/:code', name: 'community-category', component: () => import('../views/CommunityCategoryView.vue') },
{ path: '/admin/community/categories', name: 'admin-categories', component: () => import('../views/AdminCategoriesView.vue'), meta: { requiresAdmin: true } }
```

同时更新旧 `/community/:id` 路径改为 alias:

```javascript
{ path: '/community/posts/:id', name: 'community-detail', component: () => import('../views/CommunityDetailView.vue') },
{ path: '/community/:id', redirect: to => ({ name: 'community-detail', params: { id: to.params.id } }) }
```

- [ ] **Step 6: 创建 CommunityCategoryView 占位页**

```vue
<template>
  <section class="view page">
    <div class="section-head"><h1>{{ category?.name || '...' }}</h1><p>{{ category?.description }}</p></div>
    <div class="toolbar tool-panel">
      <el-input v-model="keyword" placeholder="搜索帖子..." clearable @keyup.enter="load" />
      <el-button :icon="Search" type="primary" @click="load">搜索</el-button>
    </div>
    <!-- 帖子列表将在后续 phase 中完善 -->
  </section>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Search } from 'lucide-vue-next'
import { categoryApi, communityApi } from '../api'

const route = useRoute()
const category = ref(null)
const posts = ref([])
const keyword = ref('')
const page = ref(1)
const total = ref(0)

async function loadCategory() {
  try {
    const cats = await categoryApi.list()
    category.value = cats.find(c => c.code === route.params.code)
  } catch {}
}

async function load() {
  try {
    const data = await communityApi.list({ keyword: keyword.value, categoryId: category.value?.id, page: page.value - 1, size: 10 })
    posts.value = data.content || []
    total.value = data.totalElements || 0
  } catch { posts.value = []; total.value = 0 }
}

watch(() => route.params.code, () => { loadCategory(); load() })
onMounted(async () => { await loadCategory(); await load() })
</script>
```

- [ ] **Step 7: 验证前端**

```bash
cd frontend && npm run dev
```

访问 `http://localhost:5173/community` 应看到版块卡片网格和发帖弹窗中的版块下拉。

- [ ] **Step 8: Commit**

```bash
git add frontend/src/components/community/CategoryGrid.vue \
        frontend/src/views/AdminCategoriesView.vue \
        frontend/src/views/CommunityView.vue \
        frontend/src/views/CommunityCategoryView.vue \
        frontend/src/router/index.js \
        frontend/src/api/index.js
git commit -m "feat: add category grid, admin category page, and category-picker in post editor"
```

---

### Task 5: Phase 2a — 通用点赞表与 Service

**Files:**
- Create: `backend/src/main/java/com/guitu/domain/CommunityLike.java`
- Create: `backend/src/main/java/com/guitu/repository/CommunityLikeRepository.java`
- Create: `backend/src/main/java/com/guitu/service/CommunityLikeService.java`
- Modify: `backend/src/main/java/com/guitu/dto/CommunityDtos.java`

- [ ] **Step 1: 创建 CommunityLike 实体**

```java
package com.guitu.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
@Entity
@Table(name = "community_likes",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "target_type", "target_id"}))
public class CommunityLike extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 16)
    private String targetType;  // "POST" or "COMMENT"

    @Column(nullable = false)
    private Long targetId;
}
```

- [ ] **Step 2: 创建 CommunityLikeRepository**

```java
package com.guitu.repository;

import com.guitu.domain.CommunityLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface CommunityLikeRepository extends JpaRepository<CommunityLike, Long> {
    Optional<CommunityLike> findByUserIdAndTargetTypeAndTargetId(Long userId, String targetType, Long targetId);
    boolean existsByUserIdAndTargetTypeAndTargetId(Long userId, String targetType, Long targetId);
    int countByTargetTypeAndTargetId(String targetType, Long targetId);
    List<CommunityLike> findByTargetTypeAndTargetIdIn(String targetType, List<Long> targetIds);

    @Modifying
    @Query("DELETE FROM CommunityLike l WHERE l.targetType = :targetType AND l.targetId = :targetId")
    void deleteByTarget(String targetType, Long targetId);
}
```

- [ ] **Step 3: 创建 CommunityLikeService**

```java
package com.guitu.service;

import com.guitu.domain.CommunityLike;
import com.guitu.repository.CommunityLikeRepository;
import com.guitu.repository.CommunityPostRepository;
import com.guitu.repository.CommunityCommentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommunityLikeService {
    private final CommunityLikeRepository repo;
    private final CommunityPostRepository postRepo;
    private final CommunityCommentRepository commentRepo;
    private final CommunityNotificationDispatcher notifDispatcher;

    public CommunityLikeService(CommunityLikeRepository repo, CommunityPostRepository postRepo,
        CommunityCommentRepository commentRepo, CommunityNotificationDispatcher notifDispatcher) {
        this.repo = repo; this.postRepo = postRepo; this.commentRepo = commentRepo;
        this.notifDispatcher = notifDispatcher;
    }

    @Transactional
    public boolean toggle(Long userId, String targetType, Long targetId) {
        return repo.findByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId)
            .map(like -> { repo.delete(like); updateCount(targetType, targetId, -1); return false; })
            .orElseGet(() -> {
                CommunityLike like = new CommunityLike();
                like.setUser(new User()); like.getUser().setId(userId);
                like.setTargetType(targetType); like.setTargetId(targetId);
                repo.save(like);
                updateCount(targetType, targetId, 1);
                notifDispatcher.dispatchLike(userId, targetType, targetId);
                return true;
            });
    }

    public boolean isLiked(Long userId, String targetType, Long targetId) {
        return repo.existsByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId);
    }

    public List<Long> getLikedTargetIds(Long userId, String targetType, List<Long> targetIds) {
        return repo.findByTargetTypeAndTargetIdIn(targetType, targetIds).stream()
            .filter(l -> l.getUser().getId().equals(userId))
            .map(CommunityLike::getTargetId).toList();
    }

    private void updateCount(String targetType, Long targetId, int delta) {
        if ("POST".equals(targetType))
            postRepo.findById(targetId).ifPresent(p -> { p.setLikeCount(p.getLikeCount() + delta); postRepo.save(p); });
        else
            commentRepo.findById(targetId).ifPresent(c -> { c.setLikeCount(c.getLikeCount() + delta); commentRepo.save(c); });
    }
}
```

- [ ] **Step 4: 添加点赞 API**

在 `CommunityController.java` 中添加:

```java
@PostMapping("/likes")
public ApiResponse<Boolean> like(@RequestBody Map<String, Object> body) {
    Long targetId = ((Number) body.get("targetId")).longValue();
    String targetType = (String) body.get("targetType");
    boolean liked = likeService.toggle(SecuritySupport.currentUserId(), targetType, targetId);
    return ApiResponse.ok(liked);
}

@DeleteMapping("/likes")
public ApiResponse<Boolean> unlike(@RequestBody Map<String, Object> body) {
    Long targetId = ((Number) body.get("targetId")).longValue();
    String targetType = (String) body.get("targetType");
    return ApiResponse.ok(likeService.toggle(SecuritySupport.currentUserId(), targetType, targetId));
}
```

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/guitu/domain/CommunityLike.java \
        backend/src/main/java/com/guitu/repository/CommunityLikeRepository.java \
        backend/src/main/java/com/guitu/service/CommunityLikeService.java \
        backend/src/main/java/com/guitu/controller/CommunityController.java
git commit -m "feat: add generic like system for posts and comments"
```

---

### Task 6: Phase 2b — 收藏表与 Service

**Files:**
- Create: `backend/src/main/java/com/guitu/domain/CommunityPostFavorite.java`
- Create: `backend/src/main/java/com/guitu/repository/CommunityPostFavoriteRepository.java`
- Create: `backend/src/main/java/com/guitu/service/CommunityFavoriteService.java`

- [ ] **Step 1: 创建 CommunityPostFavorite 实体**

```java
package com.guitu.domain;

import jakarta.persistence.*;
import lombok.Getter; import lombok.NoArgsConstructor; import lombok.Setter;

@Getter @Setter @NoArgsConstructor
@Entity
@Table(name = "community_post_favorites",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "post_id"}))
public class CommunityPostFavorite extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id")
    private CommunityPost post;
}
```

- [ ] **Step 2: 创建 CommunityPostFavoriteRepository**

```java
package com.guitu.repository;

import com.guitu.domain.CommunityPostFavorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CommunityPostFavoriteRepository extends JpaRepository<CommunityPostFavorite, Long> {
    Optional<CommunityPostFavorite> findByUserIdAndPostId(Long userId, Long postId);
    boolean existsByUserIdAndPostId(Long userId, Long postId);
    Page<CommunityPostFavorite> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
```

- [ ] **Step 3: 创建 CommunityFavoriteService**

```java
package com.guitu.service;

import com.guitu.domain.CommunityPost;
import com.guitu.domain.CommunityPostFavorite;
import com.guitu.domain.User;
import com.guitu.repository.CommunityPostFavoriteRepository;
import com.guitu.repository.CommunityPostRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommunityFavoriteService {
    private final CommunityPostFavoriteRepository repo;
    private final CommunityPostRepository postRepo;

    public CommunityFavoriteService(CommunityPostFavoriteRepository repo, CommunityPostRepository postRepo) {
        this.repo = repo; this.postRepo = postRepo;
    }

    @Transactional
    public boolean toggle(Long userId, Long postId) {
        return repo.findByUserIdAndPostId(userId, postId)
            .map(fav -> { repo.delete(fav); updateCount(postId, -1); return false; })
            .orElseGet(() -> {
                CommunityPostFavorite fav = new CommunityPostFavorite();
                fav.setUser(new User()); fav.getUser().setId(userId);
                CommunityPost post = new CommunityPost(); post.setId(postId);
                fav.setPost(post);
                repo.save(fav);
                updateCount(postId, 1);
                return true;
            });
    }

    public boolean isFavorited(Long userId, Long postId) {
        return repo.existsByUserIdAndPostId(userId, postId);
    }

    public Page<CommunityPostFavorite> listMine(Long userId, Pageable pageable) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    private void updateCount(Long postId, int delta) {
        postRepo.findById(postId).ifPresent(p -> { p.setFavoriteCount(p.getFavoriteCount() + delta); postRepo.save(p); });
    }
}
```

- [ ] **Step 4: 在 CommunityController 中加收藏接口**

```java
@PostMapping("/posts/{id}/favorite")
public ApiResponse<Boolean> favorite(@PathVariable Long id) {
    return ApiResponse.ok(favoriteService.toggle(SecuritySupport.currentUserId(), id));
}

@DeleteMapping("/posts/{id}/favorite")
public ApiResponse<Boolean> unfavorite(@PathVariable Long id) {
    return ApiResponse.ok(favoriteService.toggle(SecuritySupport.currentUserId(), id));
}

@GetMapping("/mine/favorites")
public ApiResponse<PageResponse<CommunityDtos.CommunityPostResponse>> listFavorites(
    @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
    Page<CommunityPostFavorite> favs = favoriteService.listMine(SecuritySupport.currentUserId(), PageRequest.of(page, size));
    List<CommunityDtos.CommunityPostResponse> list = favs.stream()
        .map(f -> communityService.toPostResponse(f.getPost(), SecuritySupport.currentUserId())).toList();
    return ApiResponse.ok(new PageResponse<>(list, favs.getTotalElements(), page, size));
}
```

- [ ] **Step 5: Commit**

---

### Task 7: Phase 2c — 浏览量防刷

**Files:**
- Create: `backend/src/main/java/com/guitu/domain/CommunityPostViewLog.java`
- Create: `backend/src/main/java/com/guitu/repository/CommunityPostViewLogRepository.java`
- Modify: `backend/src/main/java/com/guitu/service/CommunityService.java`

- [ ] **Step 1: 创建 CommunityPostViewLog 实体**

```java
package com.guitu.domain;

import jakarta.persistence.*;
import lombok.Getter; import lombok.NoArgsConstructor; import lombok.Setter;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor
@Entity
@Table(name = "community_post_view_logs",
    uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "viewer_key", "viewed_on"}))
public class CommunityPostViewLog extends BaseEntity {
    @Column(nullable = false)
    private Long postId;
    @Column(nullable = false, length = 80)
    private String viewerKey;
    @Column(nullable = false)
    private LocalDate viewedOn;
}
```

- [ ] **Step 2: 创建 Repository**

```java
package com.guitu.repository;

import com.guitu.domain.CommunityPostViewLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface CommunityPostViewLogRepository extends JpaRepository<CommunityPostViewLog, Long> {
    @Modifying
    @Query(value = "INSERT IGNORE INTO community_post_view_logs (post_id, viewer_key, viewed_on, created_at, updated_at) VALUES (?1, ?2, ?3, NOW(), NOW())", nativeQuery = true)
    int insertIgnore(Long postId, String viewerKey, java.time.LocalDate viewedOn);
}
```

- [ ] **Step 3: 在 CommunityService.detailPublic() 中触发 view_count**

```java
// 在 detailPublic 方法末尾添加(在 return 之前):
@Async
public void incrementViewCount(Long postId, String viewerKey) {
    LocalDate today = LocalDate.now();
    int affected = viewLogRepo.insertIgnore(postId, viewerKey, today);
    if (affected > 0) {
        postRepo.findById(postId).ifPresent(p -> {
            p.setViewCount(p.getViewCount() + 1);
            postRepo.save(p);
        });
    }
}
```

详情接口中调用:

```java
String viewerKey = SecuritySupport.currentUserId() != null
    ? "u:" + SecuritySupport.currentUserId()
    : "ip:" + getIpHash();
incrementViewCount(id, viewerKey);
```

- [ ] **Step 4: Commit**

---

### Task 8: Phase 2d — 前端点赞/收藏/浏览量组件

**Files:**
- Create: `frontend/src/components/community/LikeButton.vue`
- Create: `frontend/src/components/community/FavoriteButton.vue`
- Modify: `frontend/src/api/index.js`

- [ ] **Step 1: 创建 LikeButton 组件**

```vue
<template>
  <el-button text size="small" :class="{ 'is-liked': liked }" @click="toggle">
    <Heart :size="15" :fill="liked ? 'var(--coral)' : 'none'" :stroke="liked ? 'var(--coral)' : undefined" />
    {{ count || '' }}
  </el-button>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Heart } from 'lucide-vue-next'
import { communityApi } from '../../api'

const props = defineProps({ targetType: String, targetId: Number, initialLiked: Boolean, initialCount: Number })
const liked = ref(props.initialLiked || false)
const count = ref(props.initialCount || 0)

async function toggle() {
  const prevLiked = liked.value; const prevCount = count.value
  liked.value = !liked.value; count.value += liked.value ? 1 : -1
  try {
    const result = await communityApi.toggleLike({ targetType: props.targetType, targetId: props.targetId })
    liked.value = result; count.value = prevCount + (result ? 1 : -1)
  } catch { liked.value = prevLiked; count.value = prevCount }
}
</script>

<style scoped>
.is-liked { color: var(--coral); }
</style>
```

- [ ] **Step 2: 创建 FavoriteButton 组件**

```vue
<template>
  <el-button text size="small" :class="{ 'is-favorited': favorited }" @click="toggle">
    <Star :size="15" :fill="favorited ? 'var(--amber)' : 'none'" :stroke="favorited ? 'var(--amber)' : undefined" />
    {{ $t('community.post.favorite') }}
  </el-button>
</template>

<script setup>
import { ref } from 'vue'
import { Star } from 'lucide-vue-next'
import { communityApi } from '../../api'

const props = defineProps({ postId: Number, initialFavorited: Boolean })
const favorited = ref(props.initialFavorited || false)

async function toggle() {
  favorited.value = !favorited.value
  try { await communityApi.toggleFavorite(props.postId) } catch { favorited.value = !favorited.value }
}
</script>
```

- [ ] **Step 3: API 加点赞/收藏方法**

```javascript
// 在 communityApi 中添加
toggleLike: (data) => http.post('/community/likes', data),
toggleFavorite: (id) => http.post(`/community/posts/${id}/favorite`),
```

- [ ] **Step 4: Commit**

```bash
git add frontend/src/components/community/LikeButton.vue \
        frontend/src/components/community/FavoriteButton.vue \
        frontend/src/api/index.js
git commit -m "feat: add LikeButton and FavoriteButton frontend components"
```

---

### Task 9: Phase 3a — CommunityComment 改字段 + CommunityPost 加计数字段

**Files:**
- Modify: `backend/src/main/java/com/guitu/domain/CommunityComment.java`
- Modify: `backend/src/main/java/com/guitu/domain/CommunityPost.java`
- Modify: `backend/src/main/java/com/guitu/config/DataInitializer.java`

- [ ] **Step 1: CommunityComment 加新字段**

在 `CommunityComment.java` 中添加:

```java
@Column(nullable = true)
private Integer floorNo;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "root_comment_id")
private CommunityComment rootComment;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "reply_to_comment_id")
private CommunityComment replyToComment;

@Column(nullable = false)
private int likeCount = 0;
```

- [ ] **Step 2: CommunityPost 加计数字段**

在 `CommunityPost.java` 中添加:

```java
@Column(nullable = false)
private long viewCount = 0;

@Column(nullable = false)
private long likeCount = 0;

@Column(nullable = false)
private long favoriteCount = 0;

@Column(nullable = false)
private LocalDateTime lastActiveAt = LocalDateTime.now();
```

- [ ] **Step 3: 在 DataInitializer 中加迁移脚本**

添加迁移逻辑(在版块种子之后),将存量评论的 `parentComment` 语义迁到新字段:

```java
// Migrate existing comment parentComment to root_comment_id + reply_to_comment_id
if (commentRepository.count() > 0) {
    List<CommunityComment> allComments = commentRepository.findAll();
    int migrated = 0;
    for (CommunityComment comment : allComments) {
        if (comment.getParentComment() != null && comment.getRootComment() == null) {
            CommunityComment parent = comment.getParentComment();
            if (parent.getParentComment() == null) {
                // parent is a floor-level comment
                comment.setRootComment(parent);
                comment.setReplyToComment(parent);
            } else {
                // parent is itself a sub-reply
                comment.setRootComment(parent.getRootComment() != null ? parent.getRootComment() : parent.getParentComment());
                comment.setReplyToComment(parent);
            }
            migrated++;
        }
    }
    if (migrated > 0) { commentRepository.saveAll(allComments); log.info("Migrated {} comments to new floor model", migrated); }
}
```

需注入 `CommunityCommentRepository`。

- [ ] **Step 4: 验证建表**

```bash
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
```

Expected: `community_comments` 有新字段,存量数据正确迁移;`community_posts` 有计数字段。

- [ ] **Step 5: Commit**

---

### Task 10: Phase 3b — 楼层/楼中楼 Service 与接口

**Files:**
- Create: `backend/src/main/java/com/guitu/service/CommunityCommentService.java`
- Modify: `backend/src/main/java/com/guitu/controller/CommunityController.java`
- Modify: `backend/src/main/java/com/guitu/dto/CommunityDtos.java`
- Modify: `backend/src/main/java/com/guitu/repository/CommunityCommentRepository.java`

- [ ] **Step 1: CommunityCommentRepository 加查询方法**

```java
// 楼层分页
Page<CommunityComment> findByPostIdAndFloorNoIsNotNullOrderByFloorNoAsc(Long postId, Pageable pageable);
Page<CommunityComment> findByPostIdAndFloorNoIsNotNullOrderByFloorNoDesc(Long postId, Pageable pageable);
Page<CommunityComment> findByPostIdAndAuthorIdAndFloorNoIsNotNullOrderByFloorNoAsc(Long postId, Long authorId, Pageable pageable);
Page<CommunityComment> findByPostIdAndAuthorIdAndFloorNoIsNotNullOrderByFloorNoDesc(Long postId, Long authorId, Pageable pageable);

// 最大楼号
@Query("SELECT COALESCE(MAX(c.floorNo), 0) FROM CommunityComment c WHERE c.post.id = :postId AND c.floorNo IS NOT NULL")
int maxFloorNo(@Param("postId") Long postId);

// 某楼层下的楼中楼
Page<CommunityComment> findByRootCommentIdOrderByCreatedAtAsc(Long rootCommentId, Pageable pageable);

// 某楼层楼中楼总数
int countByRootCommentId(Long rootCommentId);

// 前3条楼中楼
List<CommunityComment> findTop3ByRootCommentIdOrderByCreatedAtAsc(Long rootCommentId);

// 批量查前3条楼中楼
@Query("SELECT c FROM CommunityComment c WHERE c.rootComment.id IN :rootIds AND c.createdAt IN " +
    "(SELECT MIN(c2.createdAt) FROM CommunityComment c2 WHERE c2.rootComment.id = c.rootComment.id GROUP BY c2.rootComment.id) " +
    "ORDER BY c.createdAt ASC")
List<CommunityComment> findTopRepliesByRootIds(@Param("rootIds") List<Long> rootIds);

// 评论总数(含楼层+楼中楼)
int countByPostId(Long postId);
```

- [ ] **Step 2: 在 CommunityDtos 中添加楼层/回复 DTO**

```java
public record FloorResponse(
    Long id, Integer floorNo, String content, List<String> imageUrls,
    Long authorId, String authorNickname, String authorAvatarUrl, String authorRoleText,
    LocalDateTime createdAt, String status, int likeCount, boolean liked,
    boolean isPostAuthor, int replyCount,
    List<ReplyResponse> topReplies
) {}

public record ReplyResponse(
    Long id, String content, List<String> imageUrls,
    Long authorId, String authorNickname, String authorAvatarUrl, String authorRoleText,
    Long replyToUserId, String replyToUserNickname,
    LocalDateTime createdAt, String status, int likeCount, boolean liked,
    List<MentionInfo> mentions
) {}

public record MentionInfo(Long userId, String nickname) {}

public record SaveFloorRequest(@NotBlank String content, List<String> imageUrls) {}

public record SaveReplyRequest(@NotNull Long replyToCommentId, @NotBlank String content, List<String> imageUrls) {}
```

- [ ] **Step 3: 创建 CommunityCommentService**

```java
package com.guitu.service;

import com.guitu.domain.*;
import com.guitu.dto.CommunityDtos.*;
import com.guitu.exception.BusinessException;
import com.guitu.repository.*;
import com.guitu.security.SecuritySupport;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class CommunityCommentService {
    private final CommunityCommentRepository commentRepo;
    private final CommunityPostRepository postRepo;
    private final CommunityLikeService likeService;
    private final CommunityMentionParser mentionParser;
    private final CommunityNotificationDispatcher notifDispatcher;

    public CommunityCommentService(CommunityCommentRepository commentRepo, CommunityPostRepository postRepo,
        CommunityLikeService likeService, CommunityMentionParser mentionParser,
        CommunityNotificationDispatcher notifDispatcher) {
        this.commentRepo = commentRepo; this.postRepo = postRepo;
        this.likeService = likeService; this.mentionParser = mentionParser;
        this.notifDispatcher = notifDispatcher;
    }

    @Transactional
    public FloorResponse createFloor(Long postId, SaveFloorRequest req) {
        CommunityPost post = postRepo.findById(postId)
            .orElseThrow(() -> new BusinessException("帖子不存在"));
        int maxFloor = commentRepo.maxFloorNo(postId);
        int newFloor = maxFloor + 1;

        CommunityComment comment = new CommunityComment();
        comment.setPost(post);
        comment.setAuthor(SecuritySupport.currentUser());
        comment.setContent(req.content());
        comment.setImageUrls(req.imageUrls() != null ? req.imageUrls() : List.of());
        comment.setFloorNo(newFloor);
        comment.setRootComment(null);
        comment.setReplyToComment(null);
        comment.setStatus(CommunityCommentStatus.PUBLISHED);
        commentRepo.save(comment);

        post.setCommentCount(post.getCommentCount() + 1);
        post.setLastActiveAt(LocalDateTime.now());
        postRepo.save(post);

        if (post.getAuthor() != null && !post.getAuthor().getId().equals(SecuritySupport.currentUserId()))
            notifDispatcher.dispatchPostCommented(post.getAuthor().getId(), postId, comment.getId(), SecuritySupport.currentUserId());

        return toFloorResponse(comment, post.getAuthor().getId(), SecuritySupport.currentUserId());
    }

    @Transactional
    public ReplyResponse createReply(Long postId, Long floorId, SaveReplyRequest req) {
        CommunityComment floor = commentRepo.findById(floorId)
            .orElseThrow(() -> new BusinessException("楼层不存在"));
        CommunityPost post = postRepo.findById(postId)
            .orElseThrow(() -> new BusinessException("帖子不存在"));

        CommunityComment replyTo = commentRepo.findById(req.replyToCommentId())
            .orElseThrow(() -> new BusinessException("回复目标不存在"));

        // Determine root: if replyTo is a floor, that's root; otherwise replyTo's root
        Long rootId = replyTo.getFloorNo() != null ? replyTo.getId() : replyTo.getRootComment().getId();

        CommunityComment reply = new CommunityComment();
        reply.setPost(post);
        reply.setAuthor(SecuritySupport.currentUser());
        reply.setContent(req.content());
        reply.setImageUrls(req.imageUrls() != null ? req.imageUrls() : List.of());
        reply.setFloorNo(null);
        reply.setRootComment(new CommunityComment()); reply.getRootComment().setId(rootId);
        reply.setReplyToComment(replyTo);
        reply.setStatus(CommunityCommentStatus.PUBLISHED);
        commentRepo.save(reply);

        post.setCommentCount(post.getCommentCount() + 1);
        post.setLastActiveAt(LocalDateTime.now());
        postRepo.save(post);

        // Parse @mentions
        List<CommunityDtos.MentionInfo> mentions = mentionParser.parse(reply.getContent(), reply.getId());

        // Notify replyTo author
        if (replyTo.getAuthor() != null && !replyTo.getAuthor().getId().equals(SecuritySupport.currentUserId()))
            notifDispatcher.dispatchCommentReplied(replyTo.getAuthor().getId(), postId, reply.getId(), SecuritySupport.currentUserId());

        // Notify @mentions
        for (CommunityDtos.MentionInfo m : mentions)
            notifDispatcher.dispatchMentioned(m.userId(), postId, reply.getId(), SecuritySupport.currentUserId());

        return toReplyResponse(reply, mentions, SecuritySupport.currentUserId());
    }

    public Page<FloorResponse> listFloors(Long postId, Long currentUserId, boolean onlyAuthor,
        boolean desc, Long postAuthorId, int page, int size) {
        Page<CommunityComment> floorPage;
        if (onlyAuthor) {
            floorPage = desc ? commentRepo.findByPostIdAndAuthorIdAndFloorNoIsNotNullOrderByFloorNoDesc(postId, postAuthorId, PageRequest.of(page, size))
                : commentRepo.findByPostIdAndAuthorIdAndFloorNoIsNotNullOrderByFloorNoAsc(postId, postAuthorId, PageRequest.of(page, size));
        } else {
            floorPage = desc ? commentRepo.findByPostIdAndFloorNoIsNotNullOrderByFloorNoDesc(postId, PageRequest.of(page, size))
                : commentRepo.findByPostIdAndFloorNoIsNotNullOrderByFloorNoAsc(postId, PageRequest.of(page, size));
        }

        Set<Long> userIds = new HashSet<>();
        for (CommunityComment f : floorPage.getContent()) userIds.add(f.getAuthor().getId());
        List<Long> floorIds = floorPage.stream().map(CommunityComment::getId).toList();

        // Batch-load liked status
        List<Long> likedCommentIds = currentUserId != null
            ? likeService.getLikedTargetIds(currentUserId, "COMMENT", floorIds) : List.of();

        return floorPage.map(floor -> toFloorResponse(floor, postAuthorId, currentUserId, likedCommentIds));
    }

    public Page<ReplyResponse> listReplies(Long floorId, Long currentUserId, int page, int size) {
        Page<CommunityComment> replies = commentRepo.findByRootCommentIdOrderByCreatedAtAsc(floorId, PageRequest.of(page, size));
        return replies.map(r -> toReplyResponse(r, mentionParser.parse(r.getContent(), r.getId()), currentUserId));
    }

    @Transactional
    public void deleteComment(Long id) {
        CommunityComment comment = commentRepo.findById(id)
            .orElseThrow(() -> new BusinessException("评论不存在"));
        comment.setStatus(CommunityCommentStatus.DELETED);
        comment.setContent("该评论已删除");
        commentRepo.save(comment);

        postRepo.findById(comment.getPost().getId()).ifPresent(p -> {
            p.setCommentCount(Math.max(0, p.getCommentCount() - 1));
            postRepo.save(p);
        });
    }

    private FloorResponse toFloorResponse(CommunityComment c, Long postAuthorId, Long currentUser, List<Long> likedIds) {
        List<CommunityComment> topReplies = commentRepo.findTop3ByRootCommentIdOrderByCreatedAtAsc(c.getId());
        int replyCount = commentRepo.countByRootCommentId(c.getId());

        return new FloorResponse(c.getId(), c.getFloorNo(), c.getContent(), c.getImageUrls(),
            c.getAuthor().getId(), c.getAuthor().getNickname(), c.getAuthor().getAvatarUrl(), c.getAuthor().getRole().getLabel(),
            c.getCreatedAt(), c.getStatus().name(), c.getLikeCount(),
            likedIds.contains(c.getId()),
            c.getAuthor().getId().equals(postAuthorId), replyCount,
            topReplies.stream().map(r -> toReplyResponse(r, mentionParser.parse(r.getContent(), r.getId()), currentUser)).toList()
        );
    }
}
```

- [ ] **Step 4: 在 CommunityController 中添加楼层接口**

```java
@GetMapping("/posts/{id}/floors")
public ApiResponse<PageResponse<CommunityDtos.FloorResponse>> listFloors(
    @PathVariable Long id,
    @RequestParam(defaultValue = "false") boolean onlyAuthor,
    @RequestParam(defaultValue = "asc") String order,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size) {
    CommunityPost post = communityService.getEntity(id);
    Long currentUserId = SecuritySupport.currentUserIdOrNull();
    return ApiResponse.ok(commentService.listFloors(id, currentUserId, onlyAuthor,
        "desc".equals(order), post.getAuthor().getId(), page, size));
}

@GetMapping("/comments/{floorId}/replies")
public ApiResponse<PageResponse<CommunityDtos.ReplyResponse>> listReplies(
    @PathVariable Long floorId,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size) {
    return ApiResponse.ok(commentService.listReplies(floorId, SecuritySupport.currentUserIdOrNull(), page, size));
}

@PostMapping("/posts/{id}/floors")
public ApiResponse<CommunityDtos.FloorResponse> createFloor(@PathVariable Long id, @Valid @RequestBody CommunityDtos.SaveFloorRequest req) {
    return ApiResponse.ok(commentService.createFloor(id, req));
}

@PostMapping("/comments/{floorId}/replies")
public ApiResponse<CommunityDtos.ReplyResponse> createReply(@PathVariable Long floorId, @Valid @RequestBody CommunityDtos.SaveReplyRequest req) {
    Long postId = commentService.getFloor(floorId).getPost().getId();
    return ApiResponse.ok(commentService.createReply(postId, floorId, req));
}
```

- [ ] **Step 5: Commit**

---

### Task 11: Phase 3c — 前端楼层列表 + 跳页 + 只看楼主 + 倒序

**Files:**
- Create: `frontend/src/components/community/FloorList.vue`
- Create: `frontend/src/components/community/FloorItem.vue`
- Create: `frontend/src/components/community/ReplyList.vue`
- Create: `frontend/src/components/community/ReplyItem.vue`
- Modify: `frontend/src/views/CommunityDetailView.vue`
- Modify: `frontend/src/api/index.js`

This is the most complex frontend task. Each component needs careful implementation.

- [ ] **Step 1: API 加楼层接口**

```javascript
// 在 communityApi 中添加
listFloors: (postId, params) => http.get(`/community/posts/${postId}/floors`, { params }),
listReplies: (floorId, params) => http.get(`/community/comments/${floorId}/replies`, { params }),
createFloor: (postId, data) => http.post(`/community/posts/${postId}/floors`, data),
createReply: (floorId, data) => http.post(`/community/comments/${floorId}/replies`, data),
```

- [ ] **Step 2: 创建 FloorItem 组件**

```vue
<template>
  <div :id="`L${floor.floorNo}`" class="floor-item" :class="{ highlight: showHighlight }">
    <div class="floor-head">
      <span class="floor-no">{{ floor.floorNo }}L</span>
      <RouterLink :to="`/users/${floor.authorId}`" class="floor-author">{{ floor.authorNickname }}</RouterLink>
      <span class="floor-time muted">{{ formatTime(floor.createdAt) }}</span>
    </div>
    <p class="floor-content">{{ floor.content }}</p>
    <div v-if="floor.imageUrls?.length" class="floor-images">
      <img v-for="url in floor.imageUrls" :key="url" :src="getFullUrl(url)" class="floor-thumb" />
    </div>
    <div class="floor-actions">
      <LikeButton targetType="COMMENT" :targetId="floor.id" :initialLiked="floor.liked" :initialCount="floor.likeCount" />
      <el-button text size="small" @click="$emit('reply', floor)">回复</el-button>
    </div>

    <!-- 楼中楼前3条 -->
    <div v-if="floor.topReplies?.length" class="sub-replies">
      <ReplyItem v-for="r in floor.topReplies" :key="r.id" :reply="r" @click-mention="(uId) => $emit('click-mention', uId)" />
      <el-button v-if="floor.replyCount > 3" text size="small" @click="$emit('expand-replies', floor)">
        展开剩余 {{ floor.replyCount - 3 }} 条
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import LikeButton from './LikeButton.vue'
import ReplyItem from './ReplyItem.vue'

const props = defineProps({ floor: Object, highlightFor: Number })
defineEmits(['reply', 'expand-replies', 'click-mention'])

const showHighlight = ref(false)
const API_BASE = window.location.origin

function getFullUrl(url) {
  if (!url) return ''; if (url.startsWith('http') || url.startsWith('data:')) return url; return API_BASE + url
}
function formatTime(v) { return v ? new Date(v).toLocaleString() : '-' }

onMounted(() => {
  if (props.highlightFor && props.highlightFor > 0) {
    showHighlight.value = true
    setTimeout(() => { showHighlight.value = false }, 1500)
  }
})
</script>

<style scoped>
.floor-item { padding: 16px 0; border-bottom: 1px solid var(--line); }
.floor-item.highlight { background: rgba(31,138,112,0.08); transition: background 0.3s; }
.floor-head { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.floor-no { font-weight: 700; color: var(--primary); font-size: 13px; }
.floor-author { font-weight: 600; color: var(--ink); text-decoration: none; }
.floor-content { line-height: 1.7; white-space: pre-wrap; margin: 0 0 8px; }
.floor-actions { display: flex; gap: 8px; }
.sub-replies { margin-top: 12px; padding-left: 24px; border-left: 2px solid var(--primary); opacity: 0.92; }
</style>
```

- [ ] **Step 3: 创建 ReplyItem 组件**

```vue
<template>
  <div class="reply-item">
    <RouterLink :to="`/users/${reply.authorId}`" class="reply-author">{{ reply.authorNickname }}</RouterLink>
    <span v-if="reply.replyToUserId" class="reply-to">
      回复 <RouterLink :to="`/users/${reply.replyToUserId}`" @click.stop="">@{{ reply.replyToUserNickname }}</RouterLink>
    </span>
    : <span class="reply-content"><MentionText :text="reply.content" :mentions="reply.mentions" /></span>
    <div class="reply-actions">
      <LikeButton targetType="COMMENT" :targetId="reply.id" :initialLiked="reply.liked" :initialCount="reply.likeCount" />
      <el-button text size="small" @click="$emit('reply-to', reply)">回复</el-button>
    </div>
  </div>
</template>

<script setup>
import { RouterLink } from 'vue-router'
import LikeButton from './LikeButton.vue'
import MentionText from './MentionText.vue'
defineProps({ reply: Object })
defineEmits(['reply-to'])
</script>

<style scoped>
.reply-item { padding: 6px 0; font-size: 14px; }
.reply-author { font-weight: 600; color: var(--ink); text-decoration: none; }
.reply-to { color: var(--muted); font-size: 13px; }
.reply-content { white-space: pre-wrap; }
.reply-actions { display: flex; gap: 8px; margin-top: 4px; }
</style>
```

- [ ] **Step 4: 创建 FloorList 组件(含跳页/只看楼主/倒序)**

```vue
<template>
  <div class="floor-list">
    <div class="floor-toolbar">
      <span>全 {{ totalComments }} 楼</span>
      <el-button text size="small" :type="onlyAuthor ? 'primary' : ''" @click="toggleOnlyAuthor">只看楼主</el-button>
      <el-button text size="small" :type="desc ? 'primary' : ''" @click="toggleOrder">{{ desc ? '正序' : '倒序' }}</el-button>
      <span style="margin-left:auto;display:flex;align-items:center;gap:6px;font-size:13px">
        跳到第 <el-input-number v-model="jumpFloor" :min="1" :max="totalComments" size="small" style="width:80px" controls-position="right" /> 楼
        <el-button size="small" @click="doJump">跳</el-button>
      </span>
    </div>

    <FloorItem v-for="floor in floors" :key="floor.id" :floor="floor" :highlightFor="highlightFloor"
      @reply="startReply(floor)" @expand-replies="expandReplies(floor)" @click-mention="goUser" />

    <el-pagination v-if="totalPages > 1" v-model:current-page="currentPage" :page-size="pageSize" :total="totalComments"
      layout="prev, pager, next" @current-change="load" style="justify-content:center;margin-top:16px" />
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import FloorItem from './FloorItem.vue'
import { communityApi } from '../../api'

const route = useRoute(); const router = useRouter()
const props = defineProps({ postId: Number, totalComments: Number })
const emit = defineEmits(['reply'])

const floors = ref([])
const onlyAuthor = ref(false)
const desc = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const totalPages = ref(1)
const jumpFloor = ref(1)
const highlightFloor = ref(0)

async function load() {
  try {
    const params = { page: currentPage.value - 1, size: pageSize.value, onlyAuthor: onlyAuthor.value, order: desc.value ? 'desc' : 'asc' }
    const data = await communityApi.listFloors(props.postId, params)
    floors.value = data.content || []
    totalPages.value = Math.ceil((data.totalElements || 0) / pageSize.value)
  } catch { floors.value = [] }
}

function toggleOnlyAuthor() { onlyAuthor.value = !onlyAuthor.value; currentPage.value = 1; load() }
function toggleOrder() { desc.value = !desc.value; currentPage.value = 1; load() }
function doJump() {
  const targetPage = Math.ceil(jumpFloor.value / pageSize.value)
  currentPage.value = targetPage
  highlightFloor.value = jumpFloor.value
  router.replace({ query: { floor: jumpFloor.value } })
  load()
}
function startReply(floor) { emit('reply', { floor, type: 'floor', replyToId: floor.id }) }
async function expandReplies(floor) {
  try {
    const data = await communityApi.listReplies(floor.id, { page: 0, size: 50 })
    floor.allReplies = data.content || []
    floor._expanded = true
  } catch {}
}

watch(() => route.query.floor, (val) => {
  if (val) { jumpFloor.value = parseInt(val); doJump() }
})

onMounted(() => {
  if (route.query.floor) { jumpFloor.value = parseInt(route.query.floor); doJump() }
  else if (route.query.page) { currentPage.value = parseInt(route.query.page) }
  load()
})
</script>

<style scoped>
.floor-toolbar { display: flex; align-items: center; gap: 12px; padding: 10px 0; border-bottom: 2px solid var(--primary); margin-bottom: 0; font-size: 14px; }
</style>
```

- [ ] **Step 5: 重写 CommunityDetailView 使用新组件**

替换 `CommunityDetailView.vue` 的评论区部分为 `FloorList` + `CommentEditor`:

```html
<!-- 帖子主体保留,评论区替换为: -->
<div class="post-actions-bar">
  <LikeButton targetType="POST" :targetId="post.id" :initialLiked="post.liked" :initialCount="post.likeCount" />
  <FavoriteButton :postId="post.id" :initialFavorited="post.favorited" />
  <span class="muted">阅读 {{ post.viewCount }}</span>
  <FollowUserButton :userId="post.authorId" :initialFollowed="post.authorFollowed" v-if="!isOwnPost" />
</div>

<CommentEditor :placeholder="'写下你的评论...'" @submit="submitFloor" />

<FloorList :postId="post.id" :totalComments="post.commentCount" @reply="handleReply" />
```

- [ ] **Step 6: 创建 CommentEditor 组件(初版,不含 @)**

```vue
<template>
  <div class="comment-editor">
    <el-input v-model="text" type="textarea" :rows="3" maxlength="5000" show-word-limit :placeholder="placeholder" />
    <div class="editor-actions">
      <EmojiPicker @select="(e) => text += e" />
      <ImageUploader v-model="images" usage="community-comment" :limit="3" />
      <el-button :loading="submitting" :icon="Send" type="primary" size="small" @click="submit">发表</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Send } from 'lucide-vue-next'
import EmojiPicker from '../EmojiPicker.vue'
import ImageUploader from '../ImageUploader.vue'

const props = defineProps({ placeholder: String })
const emit = defineEmits(['submit'])
const text = ref('')
const images = ref([])
const submitting = ref(false)

async function submit() {
  if (!text.value.trim()) return
  submitting.value = true
  try { await emit('emit')({ content: text.value, imageUrls: images.value }); text.value = ''; images.value = [] }
  catch {} finally { submitting.value = false }
}
</script>
```

- [ ] **Step 7: Commit**

```bash
git add frontend/src/components/community/FloorList.vue \
        frontend/src/components/community/FloorItem.vue \
        frontend/src/components/community/ReplyList.vue \
        frontend/src/components/community/ReplyItem.vue \
        frontend/src/components/community/CommentEditor.vue \
        frontend/src/views/CommunityDetailView.vue \
        frontend/src/api/index.js
git commit -m "feat: floor-based comment UI with jump-to-page, author-only, and reverse order"
```

---

### Task 12: Phase 4 — @ 提及解析器 + 前端 @ 选择器

**Files:**
- Create: `backend/src/main/java/com/guitu/service/CommunityMentionParser.java`
- Create: `backend/src/main/java/com/guitu/domain/CommunityCommentMention.java`
- Create: `backend/src/main/java/com/guitu/repository/CommunityCommentMentionRepository.java`
- Create: `frontend/src/components/community/MentionPopover.vue`
- Create: `frontend/src/components/community/MentionText.vue`
- Modify: `backend/src/main/java/com/guitu/repository/UserRepository.java`
- Modify: `frontend/src/components/community/CommentEditor.vue`

- [ ] **Step 1: 创建 CommunityCommentMention 实体**

```java
package com.guitu.domain;

import jakarta.persistence.*;
import lombok.Getter; import lombok.NoArgsConstructor; import lombok.Setter;

@Getter @Setter @NoArgsConstructor
@Entity
@Table(name = "community_comment_mentions",
    uniqueConstraints = @UniqueConstraint(columnNames = {"comment_id", "mentioned_user_id"}))
public class CommunityCommentMention extends BaseEntity {
    @Column(nullable = false)
    private Long commentId;
    @Column(nullable = false)
    private Long mentionedUserId;
}
```

- [ ] **Step 2: 创建 Repository**

```java
package com.guitu.repository;

import com.guitu.domain.CommunityCommentMention;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommunityCommentMentionRepository extends JpaRepository<CommunityCommentMention, Long> {
    List<CommunityCommentMention> findByCommentId(Long commentId);
}
```

- [ ] **Step 3: 创建 CommunityMentionParser**

```java
package com.guitu.service;

import com.guitu.domain.CommunityCommentMention;
import com.guitu.domain.User;
import com.guitu.dto.CommunityDtos.MentionInfo;
import com.guitu.repository.CommunityCommentMentionRepository;
import com.guitu.repository.UserRepository;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.regex.*;

@Service
public class CommunityMentionParser {
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([^\\s@]{1,20})");
    private final UserRepository userRepo;
    private final CommunityCommentMentionRepository mentionRepo;

    public CommunityMentionParser(UserRepository userRepo, CommunityCommentMentionRepository mentionRepo) {
        this.userRepo = userRepo; this.mentionRepo = mentionRepo;
    }

    public List<MentionInfo> parse(String content, Long commentId) {
        if (content == null) return List.of();
        Matcher matcher = MENTION_PATTERN.matcher(content);
        Set<String> candidateNicks = new LinkedHashSet<>();
        while (matcher.find()) candidateNicks.add(matcher.group(1));
        if (candidateNicks.isEmpty()) return List.of();

        List<User> matched = userRepo.findByNicknameIn(new ArrayList<>(candidateNicks));
        Map<String, User> nickToUser = new HashMap<>();
        for (User u : matched) nickToUser.put(u.getNickname(), u);

        List<MentionInfo> result = new ArrayList<>();
        for (String nick : candidateNicks) {
            User u = nickToUser.get(nick);
            if (u != null) {
                CommunityCommentMention m = new CommunityCommentMention();
                m.setCommentId(commentId); m.setMentionedUserId(u.getId());
                mentionRepo.save(m);
                result.add(new MentionInfo(u.getId(), u.getNickname()));
            }
        }
        return result;
    }
}
```

- [ ] **Step 4: UserRepository 加昵称搜索方法**

```java
List<User> findByNicknameIn(List<String> nicknames);

@Query("SELECT u FROM User u WHERE u.nickname LIKE CONCAT(:prefix, '%') AND u.status = 'NORMAL' ORDER BY u.nickname")
List<User> searchByNicknamePrefix(@Param("prefix") String prefix, Pageable pageable);
```

- [ ] **Step 5: 创建 /api/users/search 接口(若不存在)**

在 `UserController` 中添加:

```java
@GetMapping("/api/users/search")
public ApiResponse<List<UserDtos.UserBrief>> searchUsers(@RequestParam String keyword, @RequestParam(defaultValue = "10") int limit) {
    Pageable pageable = PageRequest.of(0, limit);
    List<User> users = userRepo.searchByNicknamePrefix(keyword, pageable);
    return ApiResponse.ok(users.stream().map(u -> new UserDtos.UserBrief(u.getId(), u.getNickname(), u.getAvatarUrl())).toList());
}
```

- [ ] **Step 6: 创建 MentionPopover 前端组件**

```vue
<template>
  <div v-if="visible" class="mention-popover" :style="{ top: pos.y + 'px', left: pos.x + 'px' }">
    <div v-if="users.length" class="mention-list">
      <button v-for="u in users" :key="u.id" class="mention-item" @click="$emit('select', u)">
        <el-avatar :src="getFullUrl(u.avatarUrl)" :size="24">{{ u.nickname?.slice(0,1) }}</el-avatar>
        <span>{{ u.nickname }}</span>
      </button>
    </div>
    <div v-else class="mention-empty">无匹配用户</div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { http } from '../../api/http'

const props = defineProps({ visible: Boolean, query: String, position: Object })
defineEmits(['select'])

const users = ref([])
const API_BASE = window.location.origin

let timer = null
watch(() => props.query, (val) => {
  clearTimeout(timer)
  if (!val) { users.value = []; return }
  timer = setTimeout(async () => {
    try { const data = await http.get('/users/search', { params: { keyword: val, limit: 10 } }); users.value = data || [] }
    catch { users.value = [] }
  }, 300)
})

function getFullUrl(url) {
  if (!url) return ''; if (url.startsWith('http') || url.startsWith('data:')) return url; return API_BASE + url
}
</script>

<style scoped>
.mention-popover { position: fixed; z-index: 9999; background: var(--panel); border: 1px solid var(--line); border-radius: 8px; box-shadow: var(--shadow); width: 200px; max-height: 240px; overflow-y: auto; }
.mention-item { display: flex; align-items: center; gap: 8px; width: 100%; padding: 8px 12px; border: none; background: none; cursor: pointer; font-size: 14px; }
.mention-item:hover { background: var(--panel-soft); }
.mention-empty { padding: 12px; text-align: center; color: var(--muted); font-size: 14px; }
</style>
```

- [ ] **Step 7: 创建 MentionText 渲染组件**

```vue
<template>
  <template v-for="(segment, i) in segments" :key="i">
    <RouterLink v-if="segment.userId" :to="`/users/${segment.userId}`" class="mention-link">@{{ segment.text }}</RouterLink>
    <span v-else>{{ segment.text }}</span>
  </template>
</template>

<script setup>
import { computed } from 'vue'
import { RouterLink } from 'vue-router'

const props = defineProps({ text: String, mentions: Array })

const segments = computed(() => {
  if (!props.mentions?.length) return [{ text: props.text }]
  const result = []
  const sorted = [...props.mentions].sort((a, b) => {
    const iA = props.text.indexOf('@' + a.nickname); const iB = props.text.indexOf('@' + b.nickname)
    return iA - iB
  })
  let lastIdx = 0
  for (const m of sorted) {
    const idx = props.text.indexOf('@' + m.nickname, lastIdx)
    if (idx === -1) continue
    if (idx > lastIdx) result.push({ text: props.text.slice(lastIdx, idx) })
    result.push({ text: m.nickname, userId: m.userId })
    lastIdx = idx + m.nickname.length + 1
  }
  if (lastIdx < props.text.length) result.push({ text: props.text.slice(lastIdx) })
  return result.length ? result : [{ text: props.text }]
})
</script>

<style scoped>
.mention-link { color: var(--primary); font-weight: 500; text-decoration: none; }
.mention-link:hover { text-decoration: underline; }
</style>
```

- [ ] **Step 8: 升级 CommentEditor 支持 @**

在 `CommentEditor.vue` 的 `el-input` 上监听 `@keyup`:

```javascript
const mentionVisible = ref(false)
const mentionQuery = ref('')
const mentionPos = ref({ x: 0, y: 0 })
const textareaRef = ref(null)

function onTextareaKeyup(e) {
  const textarea = e.target
  const cursorPos = textarea.selectionStart
  const beforeCursor = text.value.slice(0, cursorPos)
  const match = beforeCursor.match(/@([^\s@]*)$/)
  if (match) {
    mentionQuery.value = match[1]
    const rect = textarea.getBoundingClientRect()
    mentionPos.value = { x: rect.left + 10, y: rect.bottom - 200 }
    mentionVisible.value = true
  } else { mentionVisible.value = false }
}

function selectMention(user) {
  const cursorPos = textareaRef.value?.selectionStart || text.value.length
  const beforeCursor = text.value.slice(0, cursorPos)
  const lastAt = beforeCursor.lastIndexOf('@')
  text.value = text.value.slice(0, lastAt) + '@' + user.nickname + ' ' + text.value.slice(cursorPos)
  mentionVisible.value = false
}
```

- [ ] **Step 9: Commit**

---

### Task 13: Phase 5 — 关注社区用户 + 关注 feed

**Files:**
- Create: `backend/src/main/java/com/guitu/domain/CommunityUserFollow.java`
- Create: `backend/src/main/java/com/guitu/repository/CommunityUserFollowRepository.java`
- Create: `backend/src/main/java/com/guitu/service/CommunityFollowService.java`
- Create: `frontend/src/components/community/FollowUserButton.vue`
- Modify: `backend/src/main/java/com/guitu/controller/CommunityController.java`

- [ ] **Step 1: 创建 CommunityUserFollow 实体**

```java
package com.guitu.domain;

import jakarta.persistence.*;
import lombok.Getter; import lombok.NoArgsConstructor; import lombok.Setter;

@Getter @Setter @NoArgsConstructor
@Entity
@Table(name = "community_user_follows",
    uniqueConstraints = @UniqueConstraint(columnNames = {"follower_id", "followee_id"}))
public class CommunityUserFollow extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "follower_id")
    private User follower;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "followee_id")
    private User followee;
}
```

- [ ] **Step 2: 创建 Repository**

```java
package com.guitu.repository;

import com.guitu.domain.CommunityUserFollow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CommunityUserFollowRepository extends JpaRepository<CommunityUserFollow, Long> {
    Optional<CommunityUserFollow> findByFollowerIdAndFolloweeId(Long followerId, Long followeeId);
    boolean existsByFollowerIdAndFolloweeId(Long followerId, Long followeeId);
    Page<CommunityUserFollow> findByFollowerIdOrderByCreatedAtDesc(Long followerId, Pageable pageable);
    Page<CommunityUserFollow> findByFolloweeIdOrderByCreatedAtDesc(Long followeeId, Pageable pageable);
    long countByFolloweeId(Long followeeId);
    long countByFollowerId(Long followerId);
    List<CommunityUserFollow> findByFolloweeIdAndFollowerIdIn(Long followeeId, List<Long> followerIds);
}
```

- [ ] **Step 3: 创建 CommunityFollowService**

```java
package com.guitu.service;

import com.guitu.domain.CommunityUserFollow;
import com.guitu.domain.User;
import com.guitu.exception.BusinessException;
import com.guitu.repository.CommunityUserFollowRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommunityFollowService {
    private final CommunityUserFollowRepository repo;

    public CommunityFollowService(CommunityUserFollowRepository repo) { this.repo = repo; }

    @Transactional
    public void follow(Long followerId, Long followeeId) {
        if (followerId.equals(followeeId)) throw new BusinessException("不能关注自己");
        if (repo.existsByFollowerIdAndFolloweeId(followerId, followeeId)) return;
        CommunityUserFollow f = new CommunityUserFollow();
        f.setFollower(new User()); f.getFollower().setId(followerId);
        f.setFollowee(new User()); f.getFollowee().setId(followeeId);
        repo.save(f);
    }

    @Transactional
    public void unfollow(Long followerId, Long followeeId) {
        repo.findByFollowerIdAndFolloweeId(followerId, followeeId).ifPresent(repo::delete);
    }

    public boolean isFollowing(Long followerId, Long followeeId) {
        return repo.existsByFollowerIdAndFolloweeId(followerId, followeeId);
    }

    public Page<CommunityUserFollow> listFollowing(Long userId, Pageable pageable) {
        return repo.findByFollowerIdOrderByCreatedAtDesc(userId, pageable);
    }

    public Page<CommunityUserFollow> listFollowers(Long userId, Pageable pageable) {
        return repo.findByFolloweeIdOrderByCreatedAtDesc(userId, pageable);
    }
}
```

- [ ] **Step 4: 在 CommunityController 加关注接口**

```java
private final CommunityFollowService followService;

@PostMapping("/follows/{userId}")
public ApiResponse<Void> follow(@PathVariable Long userId) {
    followService.follow(SecuritySupport.currentUserId(), userId);
    return ApiResponse.ok();
}

@DeleteMapping("/follows/{userId}")
public ApiResponse<Void> unfollow(@PathVariable Long userId) {
    followService.unfollow(SecuritySupport.currentUserId(), userId);
    return ApiResponse.ok();
}

@GetMapping("/users/{id}/followers")
public ApiResponse<PageResponse<?>> listFollowers(@PathVariable Long id, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
    Page<CommunityUserFollow> p = followService.listFollowers(id, PageRequest.of(page, size));
    return ApiResponse.ok(new PageResponse<>(p.stream().map(f -> Map.of("userId", f.getFollower().getId(), "nickname", f.getFollower().getNickname(), "avatarUrl", f.getFollower().getAvatarUrl())).toList(), p.getTotalElements(), page, size));
}

@GetMapping("/users/{id}/following")
public ApiResponse<PageResponse<?>> listFollowing(@PathVariable Long id, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
    Page<CommunityUserFollow> p = followService.listFollowing(id, PageRequest.of(page, size));
    return ApiResponse.ok(new PageResponse<>(p.stream().map(f -> Map.of("userId", f.getFollowee().getId(), "nickname", f.getFollowee().getNickname(), "avatarUrl", f.getFollowee().getAvatarUrl())).toList(), p.getTotalElements(), page, size));
}

@GetMapping("/mine/follow-status")
public ApiResponse<Map<Long, Boolean>> followStatus(@RequestParam String userIds) {
    List<Long> ids = Arrays.stream(userIds.split(",")).map(Long::parseLong).toList();
    Map<Long, Boolean> result = new HashMap<>();
    for (Long id : ids) result.put(id, followService.isFollowing(SecuritySupport.currentUserId(), id));
    return ApiResponse.ok(result);
}
```

- [ ] **Step 5: 创建 FollowUserButton 前端组件**

```vue
<template>
  <el-button :type="isFollowed ? 'default' : 'primary'" :loading="loading" :plain="!isFollowed" size="small" @click="toggle">
    {{ isFollowed ? '已关注' : '关注' }}
  </el-button>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { communityApi } from '../../api'

const props = defineProps({ userId: Number, initialFollowed: Boolean })
const loading = ref(false)
const isFollowed = ref(props.initialFollowed || false)

async function toggle() {
  loading.value = true
  try {
    if (isFollowed.value) { await communityApi.unfollow(props.userId); isFollowed.value = false; ElMessage.success('已取消关注') }
    else { await communityApi.follow(props.userId); isFollowed.value = true; ElMessage.success('已关注') }
  } catch (e) { ElMessage.error(e?.response?.data?.message || '操作失败') }
  finally { loading.value = false }
}
</script>
```

- [ ] **Step 6: API 加关注方法**

```javascript
// 在 communityApi 中添加
follow: (userId) => http.post(`/community/follows/${userId}`),
unfollow: (userId) => http.delete(`/community/follows/${userId}`),
followStatus: (userIds) => http.get('/community/mine/follow-status', { params: { userIds: userIds.join(',') } }),
feedFollowing: (params) => http.get('/community/feed/following', { params }),
```

- [ ] **Step 7: Commit**

---

### Task 14: Phase 6 — 通知 Dispatcher + 6 种新通知类型

**Files:**
- Create: `backend/src/main/java/com/guitu/service/CommunityNotificationDispatcher.java`
- Modify: `backend/src/main/java/com/guitu/domain/enums/NotificationType.java`
- Modify: `frontend/src/i18n/zh.js`
- Modify: `frontend/src/i18n/en.js`
- Modify: `frontend/src/components/AppShell.vue`

- [ ] **Step 1: NotificationType 加新枚举值**

```java
COMMUNITY_POST_COMMENTED("Community post commented"),
COMMUNITY_COMMENT_REPLIED("Community comment replied"),
COMMUNITY_POST_LIKED("Community post liked"),
COMMUNITY_COMMENT_LIKED("Community comment liked"),
COMMUNITY_MENTIONED("Community mentioned"),
COMMUNITY_FOLLOWED_NEW_POST("Following new post");
```

- [ ] **Step 2: 创建 CommunityNotificationDispatcher**

```java
package com.guitu.service;

import com.guitu.domain.User;
import com.guitu.domain.enums.NotificationType;
import com.guitu.repository.CommunityPostRepository;
import com.guitu.repository.CommunityCommentRepository;
import com.guitu.repository.CommunityUserFollowRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CommunityNotificationDispatcher {
    private final NotificationService notificationService;
    private final CommunityFollowService followService;
    private final CommunityUserFollowRepository followRepo;
    private final CommunityPostRepository postRepo;
    private final CommunityCommentRepository commentRepo;

    // Simple in-memory dedup: key = "toUser|fromUser|type|postId", value = last dispatch time
    private final Map<String, LocalDateTime> dedupCache = new ConcurrentHashMap<>();

    public CommunityNotificationDispatcher(NotificationService notificationService,
        CommunityFollowService followService, CommunityUserFollowRepository followRepo,
        CommunityPostRepository postRepo, CommunityCommentRepository commentRepo) {
        this.notificationService = notificationService; this.followService = followService;
        this.followRepo = followRepo; this.postRepo = postRepo; this.commentRepo = commentRepo;
    }

    private boolean shouldDispatch(String key) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime last = dedupCache.get(key);
        if (last != null && java.time.Duration.between(last, now).toMinutes() < 1) return false;
        dedupCache.put(key, now);
        return true;
    }

    public void dispatchPostCommented(Long toUserId, Long postId, Long commentId, Long fromUserId) {
        String key = toUserId + "|" + fromUserId + "|POST_COMMENTED|" + postId;
        if (!shouldDispatch(key)) return;
        notificationService.notifyUser(getUser(toUserId), NotificationType.COMMUNITY_POST_COMMENTED,
            "COMMUNITY_POST_COMMENTED", fromUserId + "|" + postId, "COMMUNITY_POST", postId);
    }

    public void dispatchCommentReplied(Long toUserId, Long postId, Long commentId, Long fromUserId) {
        String key = toUserId + "|" + fromUserId + "|COMMENT_REPLIED|" + postId;
        if (!shouldDispatch(key)) return;
        notificationService.notifyUser(getUser(toUserId), NotificationType.COMMUNITY_COMMENT_REPLIED,
            "COMMUNITY_COMMENT_REPLIED", fromUserId + "|" + postId + "|" + commentId, "COMMUNITY_COMMENT", commentId);
    }

    public void dispatchLike(Long fromUserId, String targetType, Long targetId) {
        // Determine the owner of the liked target
        Long ownerId = null;
        if ("POST".equals(targetType)) {
            ownerId = postRepo.findById(targetId).map(p -> p.getAuthor().getId()).orElse(null);
        } else {
            ownerId = commentRepo.findById(targetId).map(c -> c.getAuthor().getId()).orElse(null);
        }
        if (ownerId == null || ownerId.equals(fromUserId)) return;

        String key = ownerId + "|" + fromUserId + "|LIKED|" + targetId;
        if (!shouldDispatch(key)) return;

        NotificationType type = "POST".equals(targetType) ? NotificationType.COMMUNITY_POST_LIKED : NotificationType.COMMUNITY_COMMENT_LIKED;
        notificationService.notifyUser(getUser(ownerId), type, type.name(), targetType + "|" + targetId, "COMMUNITY_" + targetType, targetId);
    }

    public void dispatchMentioned(Long toUserId, Long postId, Long commentId, Long fromUserId) {
        String key = toUserId + "|" + fromUserId + "|MENTIONED|" + postId;
        if (!shouldDispatch(key)) return;
        notificationService.notifyUser(getUser(toUserId), NotificationType.COMMUNITY_MENTIONED,
            "COMMUNITY_MENTIONED", postId + "|" + commentId, "COMMUNITY_COMMENT", commentId);
    }

    @Async
    public void broadcastNewPost(Long authorId, Long postId) {
        long followerCount = followRepo.countByFolloweeId(authorId);
        List<Long> followerIds;
        if (followerCount <= 200) {
            Pageable pageable = PageRequest.of(0, (int) Math.min(followerCount, 200));
            followerIds = followRepo.findByFolloweeIdOrderByCreatedAtDesc(pageable).stream()
                .map(f -> f.getFollower().getId()).toList();
        } else {
            Pageable pageable = PageRequest.of(0, 200);
            followerIds = followRepo.findByFolloweeIdOrderByCreatedAtDesc(pageable).stream()
                .map(f -> f.getFollower().getId()).toList();
        }
        for (Long fid : followerIds) {
            String key = fid + "|" + authorId + "|FOLLOWED_POST|" + postId;
            if (!shouldDispatch(key)) continue;
            notificationService.notifyUser(getUser(fid), NotificationType.COMMUNITY_FOLLOWED_NEW_POST,
                "COMMUNITY_FOLLOWED_NEW_POST", authorId + "|" + postId, "COMMUNITY_POST", postId);
        }
    }

    private User getUser(Long id) { User u = new User(); u.setId(id); return u; }
}
```

- [ ] **Step 3: 前端通知处理加新类型**

在 `AppShell.vue` 的 `openNotification` 方法中添加新通知类型的跳转:

```javascript
function openNotification(item) {
  markRead(item.id)
  switch (item.type) {
    case 'COMMUNITY_POST_COMMENTED':
    case 'COMMUNITY_POST_LIKED':
      if (item.relatedTargetId) router.push(`/community/posts/${item.relatedTargetId}`)
      break
    case 'COMMUNITY_COMMENT_REPLIED':
    case 'COMMUNITY_COMMENT_LIKED':
    case 'COMMUNITY_MENTIONED':
      // content format: fromUserId|postId|commentId
      const parts = item.content.split('|')
      const postId = parts[1]
      router.push(`/community/posts/${postId}?floor=${findFloorForComment(parts[2])}`)
      break
    case 'COMMUNITY_FOLLOWED_NEW_POST':
      if (item.relatedTargetId) router.push(`/community/posts/${item.relatedTargetId}`)
      break
  }
}
```

- [ ] **Step 4: i18n 加通知文案**

`zh.js`:
```javascript
notification: {
  // 已有...
  COMMUNITY_POST_COMMENTED: '有人评论了你的帖子',
  COMMUNITY_COMMENT_REPLIED: '有人回复了你的评论',
  COMMUNITY_POST_LIKED: '有人赞了你的帖子',
  COMMUNITY_COMMENT_LIKED: '有人赞了你的评论',
  COMMUNITY_MENTIONED: '有人在评论中提到了你',
  COMMUNITY_FOLLOWED_NEW_POST: '你关注的人发布了新帖子',
}
```

`en.js`:
```javascript
notification: {
  COMMUNITY_POST_COMMENTED: 'Someone commented on your post',
  COMMUNITY_COMMENT_REPLIED: 'Someone replied to your comment',
  COMMUNITY_POST_LIKED: 'Someone liked your post',
  COMMUNITY_COMMENT_LIKED: 'Someone liked your comment',
  COMMUNITY_MENTIONED: 'Someone mentioned you in a comment',
  COMMUNITY_FOLLOWED_NEW_POST: 'Someone you follow posted',
}
```

- [ ] **Step 5: Commit**

---

### Task 15: Phase 7 — 收尾: 帖子排序后端支持 + 用户页接入 + 关注 feed

**Files:**
- Modify: `backend/src/main/java/com/guitu/service/CommunityService.java`
- Modify: `frontend/src/views/CommunityHomeView.vue` (从 CommunityView 重写)
- Modify: `frontend/src/views/UserProfileView.vue`

- [ ] **Step 1: listPublic 支持 hot 排序 + authorId 筛选**

```java
public PageResponse<CommunityPostResponse> listPublic(String keyword, Long categoryId, Long authorId, String sort, int page, int size, Long currentUserId) {
    Pageable pageable;
    if ("hot".equals(sort)) pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "likeCount"));
    else if ("created".equals(sort)) pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    else pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "lastActiveAt"));

    Specification<CommunityPost> spec = (root, query, cb) -> {
        var predicates = new ArrayList<Predicate>();
        predicates.add(cb.equal(root.get("status"), CommunityPostStatus.PUBLISHED));
        if (keyword != null && !keyword.isBlank()) {
            String kw = "%" + keyword.trim() + "%";
            predicates.add(cb.or(cb.like(root.get("title"), kw), cb.like(root.get("content"), kw)));
        }
        if (categoryId != null) predicates.add(cb.equal(root.get("category").get("id"), categoryId));
        if (authorId != null) predicates.add(cb.equal(root.get("author").get("id"), authorId));
        return cb.and(predicates.toArray(new Predicate[0]));
    };

    Page<CommunityPost> result = postRepo.findAll(spec, pageable);
    List<CommunityPostResponse> list = result.getContent().stream()
        .map(p -> mapper.toPostResponse(p, currentUserId)).toList();
    return new PageResponse<>(list, result.getTotalElements(), page, size);
}
```

- [ ] **Step 2: 创建 CommunityHomeView**

从 `CommunityView.vue` 复制核心逻辑,加载版块列表 + tab 切换:

```vue
<template>
  <section class="view page">
    <div class="section-head"><h1>{{ $t('community.title') }}</h1><p>{{ $t('community.description') }}</p>
      <el-button v-if="auth.isLoggedIn.value" :icon="SquarePen" type="primary" size="large" @click="openEditor()">{{ $t('community.publish') }}</el-button>
    </div>

    <CategoryGrid />

    <div class="tabs">
      <el-radio-group v-model="sort" @change="load">
        <el-radio-button value="latest_active">{{ $t('community.tab.latest') }}</el-radio-button>
        <el-radio-button value="hot">{{ $t('community.tab.hot') }}</el-radio-button>
        <el-radio-button v-if="auth.isLoggedIn.value" value="following">{{ $t('community.tab.following') }}</el-radio-button>
      </el-radio-group>
    </div>

    <!-- Post list using PostCard component (same as before, extracted) -->
    <div v-if="posts.length" class="community-feed">
      <PostCard v-for="post in posts" :key="post.id" :post="post" />
    </div>
    <EmptyState v-else :title="$t('community.empty.' + sort)" :description="$t('community.noDataDesc')" />

    <el-pagination v-if="total > pageSize" v-model:current-page="page" :page-size="pageSize" :total="total"
      layout="prev, pager, next" @current-change="load" style="justify-content:center;margin-top:24px" />

    <!-- Editor dialog (same as before) -->
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Send, SquarePen } from 'lucide-vue-next'
import EmptyState from '../../components/EmptyState.vue'
import EmojiPicker from '../../components/EmojiPicker.vue'
import ImageUploader from '../../components/ImageUploader.vue'
import CategoryGrid from '../../components/community/CategoryGrid.vue'
import PostCard from '../../components/community/PostCard.vue'
import { categoryApi, communityApi } from '../../api'
import { notifyError } from '../../api/http'
import { useAuth } from '../../stores/auth'

const auth = useAuth()
const loading = ref(false); const saving = ref(false)
const posts = ref([]); const total = ref(0); const page = ref(1); const pageSize = 10
const sort = ref('latest_active')
const editorVisible = ref(false); const editingId = ref(null); const formRef = ref()
const categories = ref([])

const editor = reactive({ title: '', content: '', imageUrls: [], categoryId: null })
const rules = {
  title: [{ required: true, message: '请输入帖子标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入帖子内容', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择版块', trigger: 'change' }]
}

function openEditor(post = null) {
  editingId.value = post?.id || null
  editor.title = post?.title || ''; editor.content = post?.content || ''
  editor.imageUrls = post?.imageUrls || []; editor.categoryId = post?.categoryId || null
  editorVisible.value = true
}

async function load() {
  loading.value = true
  try {
    const params = { keyword: '', page: page.value - 1, size: pageSize, sort: sort.value }
    const data = sort.value === 'following'
      ? await communityApi.feedFollowing(params)
      : await communityApi.list(params)
    posts.value = data.content || []; total.value = data.totalElements || 0
  } catch (e) { notifyError(e); posts.value = []; total.value = 0 }
  finally { loading.value = false }
}

async function submitPost() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (editingId.value) { await communityApi.update(editingId.value, editor); ElMessage.success('帖子已更新') }
    else { await communityApi.create(editor); ElMessage.success('帖子已发布') }
    editorVisible.value = false; editingId.value = null
    Object.assign(editor, { title: '', content: '', imageUrls: [], categoryId: null })
    page.value = 1; await load()
  } catch (e) { notifyError(e) }
  finally { saving.value = false }
}

onMounted(async () => {
  try { categories.value = await categoryApi.list() } catch {}
  await load()
})
</script>
```

- [ ] **Step 3: UserProfileView 加社区 tab**

在 `UserProfileView.vue` 中添加社区相关 tab:

```html
<el-tab-pane label="帖子" name="posts">
  <PostCard v-for="post in userPosts" :key="post.id" :post="post" />
  <el-empty v-if="!userPosts.length" description="暂无帖子" />
</el-tab-pane>
<el-tab-pane label="关注" name="following">
  <div v-for="f in following" :key="f.userId" class="follow-item">
    <RouterLink :to="`/users/${f.userId}`">{{ f.nickname }}</RouterLink>
  </div>
</el-tab-pane>
<el-tab-pane label="粉丝" name="followers">
  <div v-for="f in followers" :key="f.userId" class="follow-item">
    <RouterLink :to="`/users/${f.userId}`">{{ f.nickname }}</RouterLink>
  </div>
</el-tab-pane>
```

- [ ] **Step 4: 路由调整: CommunityView → CommunityHomeView**

```javascript
{ path: '/community', name: 'community', component: () => import('../views/CommunityHomeView.vue') },
```

- [ ] **Step 5: 验证全局流程**

```bash
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=h2 &
cd frontend && npm run dev
```

手动测试 QA 用例(spec 第 10 节列出的 10 条)。

- [ ] **Step 6: Commit**

```bash
git add .
git commit -m "feat: community redesign wrap-up - sorting, following feed, user profile integration"
```

---

### Task 16: 编写后端单元测试

**Files:**
- Create: `backend/src/test/java/com/guitu/service/CommunityCategoryServiceTest.java`
- Create: `backend/src/test/java/com/guitu/service/CommunityCommentServiceTest.java`
- Create: `backend/src/test/java/com/guitu/service/CommunityLikeServiceTest.java`
- Create: `backend/src/test/java/com/guitu/service/CommunityFavoriteServiceTest.java`
- Create: `backend/src/test/java/com/guitu/service/CommunityFollowServiceTest.java`
- Create: `backend/src/test/java/com/guitu/service/CommunityMentionParserTest.java`
- Create: `backend/src/test/java/com/guitu/service/CommunityPostServiceTest.java`

- [ ] **Step 1: CommunityMentionParserTest**

```java
package com.guitu.service;

import com.guitu.domain.CommunityCommentMention;
import com.guitu.domain.User;
import com.guitu.dto.CommunityDtos.MentionInfo;
import com.guitu.repository.CommunityCommentMentionRepository;
import com.guitu.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommunityMentionParserTest {
    @Mock UserRepository userRepo;
    @Mock CommunityCommentMentionRepository mentionRepo;
    @InjectMocks CommunityMentionParser parser;

    @Test
    void shouldParseSingleMention() {
        User alice = new User(); alice.setId(1L); alice.setNickname("小明");
        when(userRepo.findByNicknameIn(List.of("小明"))).thenReturn(List.of(alice));
        when(mentionRepo.save(any())).thenReturn(new CommunityCommentMention());

        List<MentionInfo> result = parser.parse("谢谢你 @小明 分享", 1L);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).nickname()).isEqualTo("小明");
        assertThat(result.get(0).userId()).isEqualTo(1L);
    }

    @Test
    void shouldHandleUnmatchedNickname() {
        when(userRepo.findByNicknameIn(List.of("不存在的用户"))).thenReturn(List.of());
        List<MentionInfo> result = parser.parse("@不存在的用户 你好", 1L);
        assertThat(result).isEmpty();
    }

    @Test
    void shouldHandleMultipleMentions() {
        User alice = new User(); alice.setId(1L); alice.setNickname("小明");
        User bob = new User(); bob.setId(2L); bob.setNickname("小红");
        when(userRepo.findByNicknameIn(List.of("小明", "小红"))).thenReturn(List.of(alice, bob));
        when(mentionRepo.save(any())).thenReturn(new CommunityCommentMention());

        List<MentionInfo> result = parser.parse("@小明 @小红 谢谢", 1L);
        assertThat(result).hasSize(2);
    }

    @Test
    void shouldIgnoreDoubleAt() {
        when(userRepo.findByNicknameIn(List.of("小明"))).thenReturn(List.of());
        List<MentionInfo> result = parser.parse("@@小明", 1L);
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyForNoMention() {
        List<MentionInfo> result = parser.parse("普通评论没有提及", 1L);
        assertThat(result).isEmpty();
    }
}
```

- [ ] **Step 2: CommunityFollowServiceTest**

```java
package com.guitu.service;

import com.guitu.domain.CommunityUserFollow;
import com.guitu.exception.BusinessException;
import com.guitu.repository.CommunityUserFollowRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommunityFollowServiceTest {
    @Mock CommunityUserFollowRepository repo;
    @InjectMocks CommunityFollowService service;

    @Test
    void shouldRejectSelfFollow() {
        assertThatThrownBy(() -> service.follow(1L, 1L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("不能关注自己");
    }

    @Test
    void shouldBeIdempotentOnFollow() {
        when(repo.existsByFollowerIdAndFolloweeId(1L, 2L)).thenReturn(true);
        service.follow(1L, 2L);
        verify(repo, never()).save(any());
    }

    @Test
    void shouldSaveOnFirstFollow() {
        when(repo.existsByFollowerIdAndFolloweeId(1L, 2L)).thenReturn(false);
        when(repo.save(any())).thenReturn(new CommunityUserFollow());
        service.follow(1L, 2L);
        verify(repo).save(any());
    }

    @Test
    void shouldBeIdempotentOnUnfollow() {
        when(repo.findByFollowerIdAndFolloweeId(1L, 2L)).thenReturn(Optional.empty());
        service.unfollow(1L, 2L);
        verify(repo, never()).delete(any());
    }
}
```

- [ ] **Step 3: 运行测试**

```bash
cd backend && ./mvnw test
```

Expected: 所有新增测试 PASS。

- [ ] **Step 4: Commit**

```bash
git add backend/src/test/
git commit -m "test: add unit tests for community services"
```

---

## 文件清单总结

按 Phase 汇总创建/修改的所有文件:

| Phase | 创建 | 修改 |
|-------|------|------|
| 0 | `CommunityCategory.java`, `CommunityCategoryRepository.java` | `DataInitializer.java` |
| 1 | `CommunityCategoryService.java`, `CommunityCategoryController.java`, `CategoryGrid.vue`, `AdminCategoriesView.vue`, `CommunityCategoryView.vue` | `CommunityDtos.java`, `CommunityPost.java`, `CommunityService.java`, `CommunityView.vue`, `router/index.js`, `api/index.js` |
| 2 | `CommunityLike.java`, `CommunityLikeRepository.java`, `CommunityLikeService.java`, `CommunityPostFavorite.java`, `CommunityPostFavoriteRepository.java`, `CommunityFavoriteService.java`, `CommunityPostViewLog.java`, `CommunityPostViewLogRepository.java`, `LikeButton.vue`, `FavoriteButton.vue` | `CommunityController.java`, `CommunityService.java` |
| 3 | `CommunityCommentService.java`, `FloorList.vue`, `FloorItem.vue`, `ReplyList.vue`, `ReplyItem.vue`, `CommentEditor.vue` | `CommunityComment.java`, `CommunityPost.java`, `CommunityCommentRepository.java`, `CommunityDtos.java`, `CommunityController.java`, `DataInitializer.java`, `CommunityDetailView.vue`, `api/index.js` |
| 4 | `CommunityCommentMention.java`, `CommunityCommentMentionRepository.java`, `CommunityMentionParser.java`, `MentionPopover.vue`, `MentionText.vue` | `UserRepository.java`, `UserController.java`, `CommentEditor.vue` |
| 5 | `CommunityUserFollow.java`, `CommunityUserFollowRepository.java`, `CommunityFollowService.java`, `FollowUserButton.vue` | `CommunityController.java`, `api/index.js` |
| 6 | `CommunityNotificationDispatcher.java` | `NotificationType.java`, `zh.js`, `en.js`, `AppShell.vue` |
| 7 | `CommunityHomeView.vue` | `CommunityService.java`, `UserProfileView.vue`, `router/index.js` |
| Test | 7 个 Test 文件 | — |

预计总代码量: 后端 ~2500 行,前端 ~2000 行,测试 ~300 行。
