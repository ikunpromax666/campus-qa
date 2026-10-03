<template>
  <div class="my-page">
    <el-header class="header">
      <div class="logo" @click="router.push('/home')">校园问答</div>
      <div class="header-right">
        <el-button link @click="router.push('/home')">首页</el-button>
        <el-button link @click="router.push('/profile')">个人中心</el-button>
        <el-button link @click="handleLogout">退出</el-button>
      </div>
    </el-header>

    <el-main class="main">
      <el-tabs v-model="activeTab" class="my-tabs" @tab-change="handleTabChange">
        <!-- ===== 我的提问（作者视角，含已关闭，不含已删除） ===== -->
        <el-tab-pane label="我的提问" name="questions">
          <div class="pane-body" v-loading="loading">
            <div
              v-for="q in myQuestions"
              :key="q.id"
              class="card"
              @click="router.push(`/question/${q.id}`)"
            >
              <div class="card-title">
                {{ q.title }}
                <el-tag v-if="q.status === 1" type="warning" size="small">已关闭</el-tag>
              </div>
              <div class="card-meta">
                <el-tag size="small" type="primary" effect="plain">{{ q.categoryName }}</el-tag>
                <el-tag v-for="t in q.tags" :key="t" size="small">{{ t }}</el-tag>
              </div>
              <div class="card-stats">
                <span>{{ q.viewCount }} 浏览</span>
                <span>{{ q.likeCount }} 点赞</span>
                <span>{{ q.answerCount }} 回答</span>
                <span>{{ formatTime(q.createTime) }}</span>
                <!-- 操作区：.stop 阻止冒泡，避免触发卡片的跳详情点击 -->
                <span class="card-actions">
                  <el-button
                    v-if="q.status === 0"
                    link
                    type="primary"
                    size="small"
                    @click.stop="router.push(`/publish?id=${q.id}`)"
                    >编辑</el-button
                  >
                  <el-button
                    v-if="q.status === 0"
                    link
                    type="warning"
                    size="small"
                    @click.stop="handleCloseQuestion(q)"
                    >关闭</el-button
                  >
                  <el-button link type="danger" size="small" @click.stop="handleDeleteQuestion(q)"
                    >删除</el-button
                  >
                </span>
              </div>
            </div>
            <el-empty v-if="!loading && myQuestions.length === 0" description="还没有发表过提问" />
            <el-pagination
              v-model:current-page="myQPage"
              :page-size="size"
              :total="myQTotal"
              layout="prev, pager, next, total"
              @current-change="loadMyQuestions"
            />
          </div>
        </el-tab-pane>

        <!-- ===== 我的回答 ===== -->
        <el-tab-pane label="我的回答" name="answers">
          <div class="pane-body" v-loading="loading">
            <div
              v-for="a in myAnswers"
              :key="a.id"
              class="card"
              @click="router.push(`/question/${a.questionId}`)"
            >
              <div class="card-title">{{ a.questionTitle }}</div>
              <div class="card-excerpt">{{ excerpt(a.content) }}</div>
              <div class="card-stats">
                <el-tag v-if="a.isAccepted === 1" type="success" size="small">已采纳</el-tag>
                <span>{{ a.likeCount }} 点赞</span>
                <span>{{ formatTime(a.createTime) }}</span>
              </div>
            </div>
            <el-empty v-if="!loading && myAnswers.length === 0" description="还没有发表过回答" />
            <el-pagination
              v-model:current-page="answersPage"
              :page-size="size"
              :total="answersTotal"
              layout="prev, pager, next, total"
              @current-change="loadMyAnswers"
            />
          </div>
        </el-tab-pane>

        <!-- ===== 我的收藏 ===== -->
        <el-tab-pane label="我的收藏" name="favorites">
          <div class="pane-body" v-loading="loading">
            <div
              v-for="q in favorites"
              :key="q.id"
              class="card"
              @click="router.push(`/question/${q.id}`)"
            >
              <div class="card-title">{{ q.title }}</div>
              <div class="card-meta">
                <span>{{ q.nickname }}</span>
                <el-tag size="small" type="primary" effect="plain">{{ q.categoryName }}</el-tag>
                <el-tag v-for="t in q.tags" :key="t" size="small">{{ t }}</el-tag>
              </div>
              <div class="card-stats">
                <span>{{ q.viewCount }} 浏览</span>
                <span>{{ q.likeCount }} 点赞</span>
                <span>{{ q.answerCount }} 回答</span>
                <span>{{ formatTime(q.createTime) }}</span>
              </div>
            </div>
            <el-empty v-if="!loading && favorites.length === 0" description="还没有收藏问题" />
            <el-pagination
              v-model:current-page="favPage"
              :page-size="size"
              :total="favTotal"
              layout="prev, pager, next, total"
              @current-change="loadFavorites"
            />
          </div>
        </el-tab-pane>

        <!-- ===== 我的点赞（问题/回答两个子类型） ===== -->
        <el-tab-pane label="我的点赞" name="likes">
          <div class="like-type-bar">
            <el-radio-group v-model="likeType" @change="handleLikeTypeChange">
              <el-radio-button value="questions">赞过的问题</el-radio-button>
              <el-radio-button value="answers">赞过的回答</el-radio-button>
            </el-radio-group>
          </div>

          <!-- 赞过的问题 -->
          <div v-if="likeType === 'questions'" class="pane-body" v-loading="loading">
            <div
              v-for="q in likedQuestions"
              :key="q.id"
              class="card"
              @click="router.push(`/question/${q.id}`)"
            >
              <div class="card-title">{{ q.title }}</div>
              <div class="card-meta">
                <span>{{ q.nickname }}</span>
                <el-tag size="small" type="primary" effect="plain">{{ q.categoryName }}</el-tag>
                <el-tag v-for="t in q.tags" :key="t" size="small">{{ t }}</el-tag>
              </div>
              <div class="card-stats">
                <span>{{ q.viewCount }} 浏览</span>
                <span>{{ q.likeCount }} 点赞</span>
                <span>{{ q.answerCount }} 回答</span>
                <span>{{ formatTime(q.createTime) }}</span>
              </div>
            </div>
            <el-empty v-if="!loading && likedQuestions.length === 0" description="还没有赞过问题" />
            <el-pagination
              v-model:current-page="likedQPage"
              :page-size="size"
              :total="likedQTotal"
              layout="prev, pager, next, total"
              @current-change="loadLikedQuestions"
            />
          </div>

          <!-- 赞过的回答 -->
          <div v-else class="pane-body" v-loading="loading">
            <div
              v-for="a in likedAnswers"
              :key="a.id"
              class="card"
              @click="router.push(`/question/${a.questionId}`)"
            >
              <div class="card-title">{{ a.questionTitle }}</div>
              <div class="card-meta">
                <span>{{ a.nickname }}</span>
                <el-tag v-if="a.isAccepted === 1" type="success" size="small">已采纳</el-tag>
              </div>
              <div class="card-excerpt">{{ excerpt(a.content) }}</div>
              <div class="card-stats">
                <span>{{ a.likeCount }} 点赞</span>
                <span>{{ formatTime(a.createTime) }}</span>
              </div>
            </div>
            <el-empty v-if="!loading && likedAnswers.length === 0" description="还没有赞过回答" />
            <el-pagination
              v-model:current-page="likedAPage"
              :page-size="size"
              :total="likedATotal"
              layout="prev, pager, next, total"
              @current-change="loadLikedAnswers"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
// ElMessage/ElMessageBox 必须显式 import（老坑：漏 import 点击时静默抛 ReferenceError）
import { ElMessage, ElMessageBox } from 'element-plus'
import { answerApi, interactionApi, questionApi } from '../api'
import { useUserStore } from '../stores/user'
import type { QuestionListVO, MyAnswerVO, MyQuestionVO } from '../types'

const router = useRouter()
const userStore = useUserStore()

const size = 10
const loading = ref(false)
const activeTab = ref<'questions' | 'answers' | 'favorites' | 'likes'>('questions')
const likeType = ref<'questions' | 'answers'>('questions')

// 每个列表独立维护 数据 + 页码 + 总数（tab 切换互不干扰）
const myQuestions = ref<MyQuestionVO[]>([])
const myQPage = ref(1)
const myQTotal = ref(0)

const myAnswers = ref<MyAnswerVO[]>([])
const answersPage = ref(1)
const answersTotal = ref(0)

const favorites = ref<QuestionListVO[]>([])
const favPage = ref(1)
const favTotal = ref(0)

const likedQuestions = ref<QuestionListVO[]>([])
const likedQPage = ref(1)
const likedQTotal = ref(0)

const likedAnswers = ref<MyAnswerVO[]>([])
const likedAPage = ref(1)
const likedATotal = ref(0)

const loadMyQuestions = async () => {
  loading.value = true
  try {
    const res = await questionApi.my({ page: myQPage.value, size })
    myQuestions.value = res.data.records
    myQTotal.value = res.data.total
  } finally {
    loading.value = false
  }
}

const loadMyAnswers = async () => {
  loading.value = true
  try {
    const res = await answerApi.my({ page: answersPage.value, size })
    myAnswers.value = res.data.records
    answersTotal.value = res.data.total
  } finally {
    loading.value = false
  }
}

const loadFavorites = async () => {
  loading.value = true
  try {
    const res = await interactionApi.myFavorites({ page: favPage.value, size })
    favorites.value = res.data.records
    favTotal.value = res.data.total
  } finally {
    loading.value = false
  }
}

const loadLikedQuestions = async () => {
  loading.value = true
  try {
    const res = await interactionApi.myLikedQuestions({ page: likedQPage.value, size })
    likedQuestions.value = res.data.records
    likedQTotal.value = res.data.total
  } finally {
    loading.value = false
  }
}

const loadLikedAnswers = async () => {
  loading.value = true
  try {
    const res = await interactionApi.myLikedAnswers({ page: likedAPage.value, size })
    likedAnswers.value = res.data.records
    likedATotal.value = res.data.total
  } finally {
    loading.value = false
  }
}

const handleTabChange = (tab: string | number) => {
  if (tab === 'questions') loadMyQuestions()
  else if (tab === 'answers') loadMyAnswers()
  else if (tab === 'favorites') loadFavorites()
  else likeType.value === 'questions' ? loadLikedQuestions() : loadLikedAnswers()
}

// 关闭/删除是破坏性操作，必须二次确认；确认后刷新当前列表
// 面试考点：前端确认只是体验层，真正的防线在后端（作者校验 + 状态机校验），
// 绕过前端直接调接口依然会被 403/业务错误拦下
const handleCloseQuestion = async (q: MyQuestionVO) => {
  await ElMessageBox.confirm(`确定关闭《${q.title}》吗？关闭后不能再回答。`, '关闭问题', {
    confirmButtonText: '关闭',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await questionApi.close(q.id)
  ElMessage.success('已关闭')
  loadMyQuestions()
}

const handleDeleteQuestion = async (q: MyQuestionVO) => {
  await ElMessageBox.confirm(`确定删除《${q.title}》吗？删除后所有人不可见。`, '删除问题', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await questionApi.remove(q.id)
  ElMessage.success('已删除')
  loadMyQuestions()
}

const handleLikeTypeChange = () => {
  likeType.value === 'questions' ? loadLikedQuestions() : loadLikedAnswers()
}

// Markdown 原文 → 纯文本摘要（列表页不渲染 Markdown，只展示截断文本）
const excerpt = (md: string, len = 120) => {
  const plain = md
    .replace(/```[\s\S]*?```/g, ' ') // 代码块
    .replace(/!\[.*?\]\(.*?\)/g, ' ') // 图片
    .replace(/\[([^\]]*)\]\(.*?\)/g, '$1') // 链接保留文字
    .replace(/[#>*`~_\-]/g, ' ') // Markdown 符号
    .replace(/\s+/g, ' ')
    .trim()
  return plain.length > len ? plain.slice(0, len) + '...' : plain
}

const formatTime = (time: string) => dayjs(time).format('YYYY-MM-DD HH:mm')

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}

onMounted(() => {
  loadMyQuestions()
})
</script>

<style scoped>
.header {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 60px;
  padding: 0 32px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.logo {
  font-size: 22px;
  font-weight: bold;
  background: linear-gradient(90deg, #409eff, #7c3aed);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  letter-spacing: 1px;
  cursor: pointer;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #606266;
}

.main {
  max-width: 1080px;
  margin: 0 auto;
  width: 100%;
  padding: 20px 24px;
}

.my-tabs :deep(.el-tabs__item) {
  font-size: 15px;
}

.like-type-bar {
  margin-bottom: 14px;
}

.pane-body {
  min-height: 300px;
}

.pane-body :deep(.el-pagination) {
  justify-content: center;
  margin-top: 20px;
}

/* 通用卡片（问题卡 / 回答卡共用） */
.card {
  background: #fff;
  border-radius: 12px;
  padding: 18px 20px;
  margin-bottom: 12px;
  cursor: pointer;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
  transition: all 0.15s;
}

.card:hover {
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.12);
  transform: translateY(-2px);
}

.card:hover .card-title {
  color: #409eff;
}

.card-title {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 10px;
}

.card-excerpt {
  color: #606266;
  font-size: 13px;
  line-height: 1.6;
  margin-bottom: 10px;
}

.card-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
  color: #909399;
  font-size: 13px;
}

.card-stats {
  display: flex;
  align-items: center;
  gap: 20px;
  color: #909399;
  font-size: 13px;
}

/* 我的提问卡片右下角操作区：贴右侧 */
.card-actions {
  margin-left: auto;
}
</style>
