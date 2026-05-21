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
        <el-form-item label="名称(中)" prop="name"><el-input v-model="editor.name" /></el-form-item>
        <el-form-item label="名称(英)"><el-input v-model="editor.nameEn" /></el-form-item>
        <el-form-item label="简介"><el-input v-model="editor.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="图标(Lucide图标名)"><el-input v-model="editor.icon" placeholder="如 HeartHandshake" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="editor.sortOrder" :min="0" :max="999" /></el-form-item>
        <el-form-item label="启用"><el-switch v-model="editor.enabled" /></el-form-item>
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
    if (editingId.value) { await categoryApi.update(editingId.value, editor); ElMessage.success('版块已更新') }
    else { await categoryApi.create(editor); ElMessage.success('版块已创建') }
    editorVisible.value = false
    await load()
  } catch (e) { ElMessage.error(e?.response?.data?.message || '操作失败') }
  finally { saving.value = false }
}

onMounted(load)
</script>
