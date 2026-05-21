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
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import * as LucideIcons from 'lucide-vue-next'
import { categoryApi } from '../../api'
import { useI18n } from 'vue-i18n'

const { locale } = useI18n()
const categories = ref([])

const iconMap = {}
for (const [key, value] of Object.entries(LucideIcons)) {
  if (key !== 'createLucideIcon' && key !== 'default') iconMap[key] = value
}

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
