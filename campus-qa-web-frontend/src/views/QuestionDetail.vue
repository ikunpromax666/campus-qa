<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'
import dayjs from 'dayjs'
import { questionApi, answerApi, interactionApi } from '../api'
import { useUserStore } from '../stores/user'
import type { QuestionDetailVO, AnswerVO } from '../types'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const question = ref<QuestionDetailVO | null>(null)
const answers = ref<AnswerVO[]>([])
const loading = ref(true)
const answerContent = ref('')
const publishing = ref(false)
const renderedContent = ref('')

// Markdown 渲染链：marked 解析 → DOMPurify 消毒（防 XSS，用户输入不可信）
function renderMarkdown(md: string): string {
  const rawHtml = marked.parse(md, { async: false }) as string
  return DOMPurify.sanitize(rawHtml)
}

// 渲染后对 pre>code 逐个高亮
function highlightCode() {
  nextTick(() => {
    document.querySelectorAll('pre code').forEach((el) => {
      hljs.highlightElement(el as HTMLElement)
    })
  })
}

async function loadDetail() {
  const id = Number(route.params.id)
  if (!id) {
    ElMessage.error('问题不存在')
    router.replace('/')
    return
  }
  loading.value = true
  try {
    const res = await questionApi.getDetail(id)
    if (res.code !== 200) {
      ElMessage.error(res.message)
      router.replace('/')
      return
    }
    question.value = res.data
    renderedContent.value = renderMarkdown(question.value.content || '')
    highlightCode()
  } finally {
    loading.value = false
  }
}

async function loadAnswers() {
  const id = Number(route.params.id)
  const res = await answerApi.list(id)
  if (res.code === 200) {
    // 被采纳的回答置顶展示
    const list = res.data.records || []
    answers.value = [...list].sort((a, b) => b.isAccepted - a.isAccepted)
    highlightCode()
  }
}

onMounted(() => {
  loadDetail()
  loadAnswers()
})

// ---------- 点赞 ----------
const likeLoading = ref(false)
async function toggleQuestionLike() {
  if (!userStore.token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  if (likeLoading.value || !question.value) return
  likeLoading.value = true
  try {
    const res = await interactionApi.like(question.value.id, 1)
    if (res.code === 200) {
      question.value.likeCount = res.data.likeCount
      question.value.likeStatus = res.data.toggleStatus
    } else {
      ElMessage.error(res.message)
    }
  } finally {
    likeLoading.value = false
  }
}

const answerLikeLoading = ref<number | null>(null)
async function toggleAnswerLike(answer: AnswerVO) {
  if (!userStore.token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  if (answerLikeLoading.value) return
  answerLikeLoading.value = answer.id
  try {
    const res = await interactionApi.like(answer.id, 2)
    if (res.code === 200) {
      answer.likeCount = res.data.likeCount
      answer.likeStatus = res.data.toggleStatus
    } else {
      ElMessage.error(res.message)
    }
  } finally {
    answerLikeLoading.value = null
  }
}

// ---------- 收藏 ----------
const favLoading = ref(false)
async function toggleFavorite() {
  if (!userStore.token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  if (favLoading.value || !question.value) return
  favLoading.value = true
  try {
    const res = await interactionApi.favorite(question.value.id)
    if (res.code === 200) {
      question.value.isFavorited = res.data.toggleStatus === 1
      ElMessage.success(question.value.isFavorited ? '已收藏' : '已取消收藏')
    } else {
      ElMessage.error(res.message)
    }
  } finally {
    favLoading.value = false
  }
}

// ---------- 采纳回答（仅问题作者可用） ----------
// 后端约束"一个问题只能有一个最佳答案"：已有采纳回答时隐藏所有采纳按钮，
// 避免用户点击后必然收到"该问题已有最佳答案"的错误（防御式 UI，约束前后端对齐）
const hasAcceptedAnswer = computed(() => answers.value.some(a => a.isAccepted === 1))
const accepting = ref(false)
async function acceptAnswer(answer: AnswerVO) {
  try {
    await ElMessageBox.confirm('采纳后不可更改，确定采纳这条回答吗？', '采纳确认', {
      confirmButtonText: '确定采纳',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return // 用户取消
  }
  accepting.value = true
  try {
    const res = await answerApi.accept(answer.id, question.value!.id)
    if (res.code === 200) {
      ElMessage.success('已采纳该回答')
      await loadAnswers()
    } else {
      ElMessage.error(res.message || '采纳失败')
    }
  } finally {
    accepting.value = false
  }
}

// ---------- 管理操作（问题作者或管理员） ----------
// 面试点：前端隐藏按钮只是体验层，越权防线永远在后端（作者/管理员校验 + 状态机校验）。
// 管理员能看到"关闭/删除"任意问题的按钮，用于处理违规内容。
const isAdmin = computed(() => userStore.user?.role === 1)
const canManage = computed(() => !!question.value && (question.value.isOwner || isAdmin.value))

const closing = ref(false)
async function closeQuestion() {
  if (!question.value) return
  try {
    await ElMessageBox.confirm('关闭后任何人不能再回答，确定关闭吗？', '关闭问题', {
      confirmButtonText: '关闭',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return // 用户取消
  }
  closing.value = true
  try {
    const res = await questionApi.close(question.value.id)
    if (res.code === 200) {
      ElMessage.success('已关闭')
      await loadDetail()
    } else {
      ElMessage.error(res.message)
    }
  } finally {
    closing.value = false
  }
}

async function deleteQuestion() {
  if (!question.value) return
  try {
    await ElMessageBox.confirm(`确定删除《${question.value.title}》吗？删除后所有人不可见。`, '删除问题', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  const res = await questionApi.remove(question.value.id)
  if (res.code === 200) {
    ElMessage.success('已删除')
    router.push('/')
  } else {
    ElMessage.error(res.message)
  }
}

const deletingAnswerId = ref<number | null>(null)
async function deleteAnswer(answer: AnswerVO) {
  try {
    await ElMessageBox.confirm('确定删除这条回答吗？删除后所有人不可见。', '删除回答', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  deletingAnswerId.value = answer.id
  try {
    const res = await answerApi.remove(answer.id)
    if (res.code === 200) {
      ElMessage.success('已删除')
      if (question.value && question.value.answerCount > 0) question.value.answerCount -= 1
      await loadAnswers()
    } else {
      ElMessage.error(res.message)
    }
  } finally {
    deletingAnswerId.value = null
  }
}

// ---------- 发布回答 ----------
async function publishAnswer() {
  const content = answerContent.value.trim()
  if (!content) {
    ElMessage.warning('回答内容不能为空')
    return
  }
  if (!userStore.token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  publishing.value = true
  try {
    const res = await answerApi.publish({ questionId: Number(route.params.id), content })
    if (res.code === 200) {
      ElMessage.success('回答发布成功')
      answerContent.value = ''
      if (question.value) question.value.answerCount += 1
      await loadAnswers()
    } else {
      ElMessage.error(res.data.message)
    }
  } finally {
    publishing.value = false
  }
}

function fmt(time: string) {
  return dayjs(time).format('YYYY-MM-DD HH:mm')
}

function goBack() {
  router.push('/')
}
</script>

<template>
  <div class="detail-page">
    <!-- 顶部导航 -->
    <header class="navbar">
      <div class="nav-inner">
        <div class="logo" @click="goBack">
          <span class="logo-icon">🎓</span>
          <span class="logo-text">校园问答</span>
        </div>
        <el-button round size="small" @click="goBack">← 返回首页</el-button>
      </div>
    </header>

    <div v-loading="loading" class="container">
      <template v-if="question">
        <!-- 问题主体 -->
        <article class="card question-card">
          <div class="meta-row">
            <el-tag type="primary" effect="light" round>{{ question.category?.name }}</el-tag>
            <el-tag
              v-for="tag in question.tags"
              :key="tag.id"
              type="info"
              effect="plain"
              round
              class="tag-item"
            >
              # {{ tag.name }}
            </el-tag>
            <span v-if="question.isTop === 1" class="top-badge">置顶</span>
          </div>

          <h1 class="title">{{ question.title }}</h1>

          <el-alert
            v-if="question.status === 1"
            title="该问题已关闭，不能再回答"
            type="warning"
            :closable="false"
            show-icon
            class="closed-banner"
          />

          <div class="author-row">
            <el-avatar :size="32" :src="question.avatar">
              {{ question.nickname?.charAt(0) }}
            </el-avatar>
            <span class="nickname">{{ question.nickname }}</span>
            <span class="time">{{ fmt(question.createTime) }}</span>
            <span class="stats">
              {{ question.viewCount }} 浏览 · {{ question.answerCount }} 回答
            </span>
          </div>

          <!-- Markdown 正文 -->
          <div class="markdown-body" v-html="renderedContent"></div>

          <!-- 操作条 -->
          <div class="action-bar">
            <el-button
              :type="question.likeStatus === 1 ? 'primary' : 'default'"
              round
              :loading="likeLoading"
              @click="toggleQuestionLike"
            >
              👍 {{ question.likeStatus === 1 ? '已赞' : '点赞' }} {{ question.likeCount }}
            </el-button>
            <el-button
              :type="question.isFavorited ? 'warning' : 'default'"
              round
              :loading="favLoading"
              @click="toggleFavorite"
            >
              ⭐ {{ question.isFavorited ? '已收藏' : '收藏' }}
            </el-button>
            <!-- 管理按钮：问题作者或管理员可见；已关闭的问题不重复显示关闭按钮 -->
            <el-button
              v-if="canManage && question.status === 0"
              type="warning"
              plain
              round
              :loading="closing"
              @click="closeQuestion"
            >
              关闭问题
            </el-button>
            <el-button v-if="canManage" type="danger" plain round @click="deleteQuestion">
              删除问题
            </el-button>
          </div>
        </article>

        <!-- 回答列表 -->
        <section class="answers-section">
          <h2 class="section-title">
            {{ answers.length }} 个回答
          </h2>

          <div
            v-for="answer in answers"
            :key="answer.id"
            class="card answer-card"
            :class="{ accepted: answer.isAccepted === 1 }"
          >
            <div class="author-row">
              <el-avatar :size="30" :src="answer.avatar">
                {{ answer.nickname?.charAt(0) }}
              </el-avatar>
              <span class="nickname">{{ answer.nickname }}</span>
              <span class="time">{{ fmt(answer.createTime) }}</span>
              <el-tag v-if="answer.isAccepted === 1" type="success" size="small" effect="dark">
                ✔ 已采纳
              </el-tag>
            </div>

            <div class="markdown-body answer-content" v-html="renderMarkdown(answer.content)"></div>

            <div class="answer-actions">
              <el-button
                link
                size="small"
                :type="answer.likeStatus === 1 ? 'primary' : 'info'"
                :loading="answerLikeLoading === answer.id"
                @click="toggleAnswerLike(answer)"
              >
                👍 {{ answer.likeCount }}
              </el-button>
              <!-- 采纳按钮：仅问题作者可见；未采纳的回答 + 问题尚无采纳答案时才显示 -->
              <el-button
                v-if="question.isOwner && !hasAcceptedAnswer && answer.isAccepted === 0"
                link
                size="small"
                type="success"
                @click="acceptAnswer(answer)"
              >
                ✔ 采纳为答案
              </el-button>
              <!-- 删除回答：回答作者或管理员可见 -->
              <el-button
                v-if="answer.isOwner || isAdmin"
                link
                size="small"
                type="danger"
                :loading="deletingAnswerId === answer.id"
                @click="deleteAnswer(answer)"
              >
                删除
              </el-button>
            </div>
          </div>

          <el-empty v-if="answers.length === 0" description="还没有回答，来抢沙发吧" />
        </section>

        <!-- 回答输入：已关闭的问题不再放行编辑器（防御式 UI，后端也会拒绝） -->
        <section
          v-if="userStore.token && question.status === 0"
          class="card answer-editor"
        >
          <h3 class="section-title">写下你的回答</h3>
          <el-input
            v-model="answerContent"
            type="textarea"
            :rows="6"
            maxlength="5000"
            show-word-limit
            placeholder="支持 Markdown 语法，认真回答会被采纳哦～"
          />
          <div class="editor-footer">
            <el-button
              type="primary"
              round
              :loading="publishing"
              @click="publishAnswer"
            >
              发布回答
            </el-button>
          </div>
        </section>
        <section v-else class="card answer-editor login-hint">
          <el-button type="primary" round @click="router.push('/login')">登录后回答</el-button>
        </section>
      </template>
    </div>
  </div>
</template>

<style scoped>
.detail-page {
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
  max-width: 960px;
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
  max-width: 960px;
  margin: 0 auto;
  padding: 20px;
  min-height: 60vh;
}

.card {
  background: #fff;
  border-radius: 12px;
  padding: 24px 28px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.question-card {
  margin-bottom: 16px;
}

.meta-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.top-badge {
  color: #ef4444;
  font-size: 12px;
  font-weight: bold;
}

.title {
  font-size: 24px;
  margin: 14px 0 10px;
  color: #111827;
  line-height: 1.4;
}

.author-row {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #6b7280;
  font-size: 13px;
}

.author-row .nickname {
  font-weight: 500;
  color: #374151;
}

.stats {
  margin-left: auto;
}

.markdown-body {
  margin-top: 18px;
  line-height: 1.75;
  color: #1f2937;
  word-break: break-word;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3) {
  margin: 18px 0 10px;
  color: #111827;
}

.markdown-body :deep(pre) {
  background: #f6f8fa;
  border-radius: 8px;
  padding: 14px;
  overflow-x: auto;
}

.markdown-body :deep(code) {
  font-family: 'Cascadia Code', Consolas, monospace;
  font-size: 13px;
}

.markdown-body :deep(blockquote) {
  border-left: 4px solid #c7d2fe;
  margin: 12px 0;
  padding: 6px 14px;
  color: #6b7280;
  background: #f8fafc;
}

.markdown-body :deep(img) {
  max-width: 100%;
  border-radius: 8px;
}

.action-bar {
  margin-top: 22px;
  padding-top: 16px;
  border-top: 1px solid #f1f3f7;
  display: flex;
  gap: 12px;
}

.closed-banner {
  margin-top: 12px;
}

.answers-section {
  margin-bottom: 16px;
}

.section-title {
  font-size: 17px;
  color: #111827;
  margin: 0 0 14px;
}

.answer-card {
  margin-bottom: 12px;
  padding: 18px 22px;
}

.answer-card.accepted {
  border: 1px solid #a7f3d0;
  background: linear-gradient(180deg, #f0fdf4, #fff);
}

.answer-content {
  margin-top: 12px;
  font-size: 14.5px;
}

.answer-actions {
  margin-top: 8px;
}

.answer-editor {
  margin-bottom: 40px;
}

.editor-footer {
  margin-top: 12px;
  text-align: right;
}

.login-hint {
  text-align: center;
  color: #6b7280;
}

@media (max-width: 640px) {
  .card {
    padding: 16px;
  }

  .title {
    font-size: 20px;
  }
}
</style>
