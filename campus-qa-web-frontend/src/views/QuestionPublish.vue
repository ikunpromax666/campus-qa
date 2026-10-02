<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { categoryApi, tagApi, questionApi } from '../api'
import type { CategoryVO, TagVO } from '../types'

const router = useRouter()

// 表单数据
const title = ref('')
const content = ref('')
const categoryId = ref<number | null>(null)
const tagIds = ref<number[]>([])

const categories = ref<CategoryVO[]>([])
const tags = ref<TagVO[]>([])
const submitting = ref(false)
const previewing = ref(false)

onMounted(async () => {
  // 拦截器已返回 Result 结构 {code, message, data}，直接 res.code / res.data
  const [catRes, tagRes] = await Promise.all([categoryApi.list(), tagApi.list()])
  if (catRes.code === 200) categories.value = catRes.data || []
  if (tagRes.code === 200) tags.value = tagRes.data || []
})

// 标题字数（5-150 与后端 @Size 对齐）
const titleValid = computed(() => title.value.trim().length >= 5 && title.value.trim().length <= 150)

// Markdown 预览：同一渲染安全链（marked → DOMPurify）
const previewHtml = computed(() => {
  if (!previewing.value) return ''
  const raw = marked.parse(content.value || '', { async: false }) as string
  return DOMPurify.sanitize(raw)
})

async function submit() {
  // 前端先行校验（与后端 DTO 规则对齐，省一次 400 往返）
  if (!titleValid.value) {
    ElMessage.warning('标题长度需在 5-150 字之间')
    return
  }
  if (!categoryId.value) {
    ElMessage.warning('请选择分类')
    return
  }
  if (tagIds.value.length === 0) {
    ElMessage.warning('请至少选择一个标签')
    return
  }
  if (!content.value.trim()) {
    ElMessage.warning('内容不能为空')
    return
  }
  submitting.value = true
  try {
    const res = await questionApi.publish({
      title: title.value.trim(),
      content: content.value,
      categoryId: categoryId.value,
      tagIds: tagIds.value
    })
    if (res.code === 200) {
      ElMessage.success('发布成功')
      router.push('/')
    } else {
      ElMessage.error(res.message || '发布失败')
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="publish-page">
    <header class="navbar">
      <div class="nav-inner">
        <div class="logo" @click="router.push('/')">
          <span class="logo-icon">🎓</span>
          <span class="logo-text">校园问答</span>
        </div>
        <el-button round size="small" @click="router.push('/')">← 返回首页</el-button>
      </div>
    </header>

    <div class="container">
      <div class="card">
        <h2 class="page-title">✍️ 发布问题</h2>
        <p class="page-subtitle">提问时描述越清楚，越容易被回答和采纳</p>

        <el-form label-position="top" size="large">
          <el-form-item label="标题（5-150 字）">
            <el-input
              v-model="title"
              maxlength="150"
              show-word-limit
              placeholder="一句话说清你的问题，例如：Spring Boot 如何集成 Redis？"
            />
          </el-form-item>

          <el-form-item label="分类">
            <el-select
              v-model="categoryId"
              placeholder="选择最相关的分类"
              style="width: 100%"
            >
              <el-option
                v-for="c in categories"
                :key="c.id"
                :label="c.name"
                :value="c.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="标签（最多 10 个，必选）">
            <el-select
              v-model="tagIds"
              multiple
              :multiple-limit="10"
              placeholder="选择相关标签，让更多人看到你的问题"
              style="width: 100%"
            >
              <el-option v-for="t in tags" :key="t.id" :label="`# ${t.name}`" :value="t.id" />
            </el-select>
          </el-form-item>

          <el-form-item label="内容（支持 Markdown）">
            <div class="editor-wrap">
              <div class="editor-toolbar">
                <el-radio-group v-model="previewing" size="small">
                  <el-radio-button :value="false">编辑</el-radio-button>
                  <el-radio-button :value="true">预览</el-radio-button>
                </el-radio-group>
              </div>
              <el-input
                v-if="!previewing"
                v-model="content"
                type="textarea"
                :rows="12"
                maxlength="10000"
                show-word-limit
                placeholder="### 描述你的问题&#10;&#10;- 环境：&#10;- 已尝试：&#10;- 期望结果："
              />
              <div v-else class="markdown-body preview" v-html="previewHtml"></div>
            </div>
          </el-form-item>

          <div class="submit-row">
            <el-button round size="large" @click="router.push('/')">取消</el-button>
            <el-button
              type="primary"
              round
              size="large"
              :loading="submitting"
              @click="submit"
            >
              发布问题
            </el-button>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.publish-page {
  min-height: 100vh;
  background: #f5f7fb;
}

.navbar {
  background: #fff;
  border-bottom: 1px solid #eef0f4;
  position: sticky;
  top: 0;
  z-index: 10;
}

.nav-inner {
  max-width: 860px;
  margin: 0 auto;
  padding: 12px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-weight: bold;
  font-size: 18px;
  background: linear-gradient(135deg, #6366f1, #3b82f6);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.logo-icon {
  -webkit-text-fill-color: initial;
}

.container {
  max-width: 860px;
  margin: 0 auto;
  padding: 24px 20px 60px;
}

.card {
  background: #fff;
  border-radius: 12px;
  padding: 28px 32px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.page-title {
  margin: 0 0 4px;
  color: #111827;
}

.page-subtitle {
  margin: 0 0 20px;
  color: #9ca3af;
  font-size: 13px;
}

.editor-wrap {
  width: 100%;
}

.editor-toolbar {
  margin-bottom: 8px;
}

.markdown-body.preview {
  min-height: 240px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 14px;
  line-height: 1.75;
  background: #fafafa;
}

.markdown-body :deep(pre) {
  background: #f0f2f5;
  border-radius: 8px;
  padding: 12px;
  overflow-x: auto;
}

.markdown-body :deep(code) {
  font-family: 'Cascadia Code', Consolas, monospace;
  font-size: 13px;
}

.markdown-body :deep(blockquote) {
  border-left: 4px solid #c7d2fe;
  margin: 10px 0;
  padding: 4px 12px;
  color: #6b7280;
}

.submit-row {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 8px;
}

@media (max-width: 640px) {
  .card {
    padding: 18px;
  }
}
</style>
