<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { navTree, sections, type NavItem } from './data/guideData'

const searchKeyword = ref('')
const activeId = ref('dashboard')
const collapsedSections = ref<Set<string>>(new Set())
const contentRef = ref<HTMLDivElement>()
const scrollContainer = ref<HTMLElement | null>(null)

// 搜索逻辑
const matchedIds = computed(() => {
  const kw = searchKeyword.value.trim().toLowerCase()
  if (!kw) return new Set<string>()
  const ids = new Set<string>()
  for (const [id, section] of Object.entries(sections)) {
    for (const block of section.content) {
      const text = (block.body || '').toLowerCase()
      if (text.includes(kw)) { ids.add(id); break }
      if (block.headers) {
        for (const h of block.headers) {
          if (h.toLowerCase().includes(kw)) { ids.add(id); break }
        }
      }
      if (block.rows) {
        for (const row of block.rows) {
          if (row.some(c => c.toLowerCase().includes(kw))) { ids.add(id); break }
        }
      }
    }
  }
  return ids
})

// 内容区渲染的章节列表（仅包含有实际内容的章节，排除无内容的组节点）
const flatNav = computed(() => {
  const result: { id: string; title: string; parentTitle?: string }[] = []
  for (const item of navTree) {
    if (sections[item.id]?.content?.length) {
      result.push({ id: item.id, title: item.title })
    }
    if (item.children) {
      for (const child of item.children) {
        result.push({ id: child.id, title: child.title, parentTitle: item.title })
      }
    }
  }
  return result
})

function scrollTo(id: string) {
  activeId.value = id
  const el = document.getElementById('section-' + id)
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

// 滚动监听
function handleScroll() {
  if (searchKeyword.value.trim()) return
  let closestId = flatNav.value[0]?.id || 'dashboard'
  let closestDist = Infinity
  for (const item of flatNav.value) {
    const el = document.getElementById('section-' + item.id)
    if (!el) continue
    const rect = el.getBoundingClientRect()
    const dist = Math.abs(rect.top - 100)
    if (dist < closestDist) { closestDist = dist; closestId = item.id }
  }
  if (closestId !== activeId.value) activeId.value = closestId
}

function toggleCollapse(id: string) {
  if (collapsedSections.value.has(id)) collapsedSections.value.delete(id)
  else collapsedSections.value.add(id)
}

// 高亮搜索关键词
function highlightText(text: string): string {
  const kw = searchKeyword.value.trim()
  if (!kw) return text
  const escaped = kw.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const regex = new RegExp(`(${escaped})`, 'gi')
  return text.replace(regex, '<mark class="search-highlight">$1</mark>')
}

function isNavMatch(item: NavItem): boolean {
  if (!searchKeyword.value.trim()) return true
  if (matchedIds.value.has(item.id)) return true
  if (item.children) return item.children.some(c => matchedIds.value.has(c.id))
  return false
}

onMounted(() => {
  collapsedSections.value.clear()
  // 实际滚动容器是 AppLayout 中的 .app-main（overflow-y: auto）
  scrollContainer.value = document.querySelector('.app-main')
  scrollContainer.value?.addEventListener('scroll', handleScroll, { passive: true })
})

onUnmounted(() => {
  scrollContainer.value?.removeEventListener('scroll', handleScroll)
})
</script>

<template>
  <div class="help-layout">
    <!-- 左侧目录 -->
    <aside class="help-sidebar">
      <div class="help-search">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索使用说明…"
          clearable
          prefix-icon="Search"
          size="large"
        />
      </div>
      <nav class="help-nav">
        <template v-for="item in navTree" :key="item.id">
          <div v-if="item.children" class="nav-group" v-show="isNavMatch(item)">
            <div class="nav-group-title">{{ item.title }}</div>
            <div
              v-for="child in item.children"
              :key="child.id"
              class="nav-item nav-item-child"
              :class="{ active: activeId === child.id }"
              :style="{ display: (!searchKeyword.trim() || matchedIds.has(child.id)) ? '' : 'none' }"
              @click="scrollTo(child.id)"
            >
              {{ child.title }}
            </div>
          </div>
          <div
            v-else
            class="nav-item"
            :class="{ active: activeId === item.id }"
            v-show="(!searchKeyword.trim() || matchedIds.has(item.id))"
            @click="scrollTo(item.id)"
          >
            {{ item.title }}
          </div>
        </template>
      </nav>
      <div v-if="searchKeyword.trim() && matchedIds.size === 0" class="search-empty-state">
        未找到相关内容，试试其他关键词
      </div>
    </aside>

    <!-- 右侧内容 -->
    <main ref="contentRef" class="help-content">
      <h1 class="help-title">系统使用说明</h1>
      <p class="help-subtitle">本指南涵盖系统的所有功能模块，帮助您快速上手和深入了解各模块的使用方法。</p>

      <template v-for="item in flatNav" :key="item.id">
        <div
          :id="'section-' + item.id"
          class="help-section"
          :class="{ 'search-match': searchKeyword.trim() && matchedIds.has(item.id) }"
        >
          <div class="section-header" @click="toggleCollapse(item.id)">
            <h2 class="section-title">{{ item.title }}</h2>
            <el-tag v-if="item.parentTitle" size="small" type="info">{{ item.parentTitle }}</el-tag>
            <span class="collapse-icon">{{ collapsedSections.has(item.id) ? '▶' : '▼' }}</span>
          </div>

          <div v-show="!collapsedSections.has(item.id)" class="section-body">
            <template v-for="(block, bi) in (sections[item.id]?.content || [])" :key="bi">
              <!-- 纯文本 -->
              <div v-if="block.type === 'text'" class="content-block">
                <h4 v-if="block.title" class="block-title">{{ block.title }}</h4>
                <div class="block-body" v-html="highlightText(block.body || '')"></div>
              </div>

              <!-- 表格 -->
              <div v-if="block.type === 'table'" class="content-block">
                <h4 v-if="block.title" class="block-title">{{ block.title }}</h4>
                <el-table :data="block.rows || []" stripe border size="small">
                  <el-table-column
                    v-for="(header, hi) in (block.headers || [])"
                    :key="hi"
                    :label="header"
                    min-width="130"
                  >
                    <template #default="{ row }">
                      <span v-html="highlightText(row[hi] || '')"></span>
                    </template>
                  </el-table-column>
                </el-table>
              </div>

              <!-- 状态流转 -->
              <div v-if="block.type === 'flow'" class="content-block">
                <h4 v-if="block.title" class="block-title">{{ block.title }}</h4>
                <div class="flow-list">
                  <div v-for="(step, si) in (block.steps || [])" :key="si" class="flow-item">
                    <el-tag size="small" type="info">{{ step.from }}</el-tag>
                    <span class="flow-arrow">{{ step.arrow }}</span>
                    <el-tag size="small" type="success">{{ step.to }}</el-tag>
                    <span class="flow-desc">{{ step.desc }}</span>
                  </div>
                </div>
              </div>

              <!-- 注意 -->
              <div v-if="block.type === 'note'" class="content-block note-block">
                <span class="note-icon">💡</span>
                <div class="block-body" v-html="highlightText(block.body || '')"></div>
              </div>

              <!-- 警告 -->
              <div v-if="block.type === 'warning'" class="content-block warning-block">
                <span class="note-icon">⚠️</span>
                <div class="block-body" v-html="highlightText(block.body || '')"></div>
              </div>
            </template>
          </div>
        </div>
      </template>

      <!-- 搜索无结果 -->
      <div
        v-if="searchKeyword.trim() && matchedIds.size === 0"
        class="no-results"
      >
        <el-empty description="未找到相关内容" />
        <p style="color:#999;font-size:13px;margin-top:8px;">试试其他关键词，如"入库"、"审核"、"权限"</p>
      </div>
    </main>
  </div>
</template>

<style scoped>
.help-layout {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.help-sidebar {
  width: 220px;
  flex-shrink: 0;
  position: sticky;
  top: 24px;
  max-height: calc(100vh - 120px);
  overflow-y: auto;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e8ece8;
  padding: 16px 0;
}

.help-search {
  padding: 0 12px 12px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 8px;
}

.search-empty-state {
  font-size: 12px;
  color: #999;
  margin-top: 8px;
  text-align: center;
  padding: 0 12px;
}

.help-nav {
  padding: 0 8px;
}

.nav-group-title {
  font-size: 12px;
  font-weight: 600;
  color: #999;
  padding: 8px 12px 4px;
  letter-spacing: 0.5px;
}

.nav-item {
  padding: 8px 12px;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  border-radius: 6px;
  margin: 1px 0;
  transition: all 0.15s;
}

.nav-item:hover {
  background: #e8f5e9;
  color: #2e7d32;
}

.nav-item.active {
  background: #2e7d32;
  color: #fff;
  font-weight: 500;
}

.nav-item-child {
  padding-left: 24px;
  font-size: 12px;
}

.help-content {
  flex: 1;
  min-width: 0;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e8ece8;
  padding: 32px;
}

.help-title {
  font-size: 24px;
  color: #303133;
  margin: 0 0 8px;
  border: none;
  padding: 0;
}

.help-subtitle {
  font-size: 14px;
  color: #909399;
  margin: 0 0 32px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.help-section {
  margin-bottom: 8px;
  border: 1px solid #e8ece8;
  border-radius: 8px;
  overflow: hidden;
  transition: border-color 0.2s;
}

.help-section.search-match {
  border-color: #2e7d32;
  box-shadow: 0 0 0 1px #2e7d32;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 16px;
  cursor: pointer;
  background: #fafbfa;
  user-select: none;
}

.section-header:hover {
  background: #f0f5f0;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0;
  border: none;
  padding: 0;
  flex: 1;
}

.collapse-icon {
  font-size: 10px;
  color: #909399;
}

.section-body {
  padding: 4px 16px 16px;
  border-top: 1px solid #f0f0f0;
}

.content-block {
  margin-top: 16px;
}

.block-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px;
}

.block-body {
  font-size: 13px;
  line-height: 1.8;
  color: #606266;
  white-space: pre-line;
}

:deep(.search-highlight) {
  background: #fff3cd;
  padding: 0 2px;
  border-radius: 2px;
  color: #e6a23c;
}

.flow-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.flow-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  padding: 4px 8px;
  background: #fafbfa;
  border-radius: 4px;
}

.flow-arrow {
  color: #909399;
  font-weight: bold;
}

.flow-desc {
  color: #909399;
  font-size: 12px;
}

.note-block {
  background: #f0f9eb;
  border-radius: 6px;
  padding: 12px 16px;
  display: flex;
  gap: 8px;
}

.warning-block {
  background: #fef0f0;
  border-radius: 6px;
  padding: 12px 16px;
  display: flex;
  gap: 8px;
}

.note-icon {
  font-size: 16px;
  flex-shrink: 0;
  margin-top: 1px;
}

.no-results {
  text-align: center;
  padding: 60px 0;
}
</style>
