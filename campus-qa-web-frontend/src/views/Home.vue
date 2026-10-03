<template>
  <div class="home">
    <el-header class="header">
      <div class="logo">校园问答</div>
      <div class="header-right">
        <span v-if="userStore.user" class="nickname-link" @click="router.push('/profile')">
          {{ userStore.user.nickname }}
        </span>
        <el-button v-if="userStore.token" link @click="router.push('/my')">我的问答</el-button>
        <el-button v-if="userStore.user?.role === 1" link type="danger" @click="router.push('/admin')">管理后台</el-button>
        <el-button v-if="userStore.token" link @click="router.push('/profile')">个人中心</el-button>
        <el-button v-if="userStore.token" link @click="handleLogout">退出</el-button>
        <el-button v-else link type="primary" @click="router.push('/login')">登录</el-button>
      </div>
    </el-header>

    <el-main class="main">
      <!-- 欢迎横幅（可关闭）：整图铺满，真按钮覆盖图内"立即提问" -->
      <div v-if="showBanner" class="banner">
        <img
          class="banner-img"
          src="/images/home-banner.png"
          alt="校园问答广场"
        />
        <el-button class="banner-ask-btn" @click="handleAskClick">立即提问</el-button>
        <el-button class="banner-close" link @click="showBanner = false">收起</el-button>
      </div>
      <!-- 收起后保留展开入口 -->
      <div v-else class="banner-collapsed">
        <el-button size="small" round @click="showBanner = true">展开横幅</el-button>
      </div>

      <!-- 搜索 + 排序 -->
      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索问题..."
          clearable
          style="width: 300px"
          @keyup.enter="loadQuestions"
          @clear="loadQuestions"
        >
          <template #append>
            <el-button @click="loadQuestions">搜索</el-button>
          </template>
        </el-input>
        <el-radio-group v-model="sort" @change="loadQuestions">
          <el-radio-button value="time">最新</el-radio-button>
          <el-radio-button value="hot">热门</el-radio-button>
        </el-radio-group>
      </div>

      <div class="content">
        <!-- 左侧分类筛选 -->
        <div class="sidebar">
          <div
            class="category-item"
            :class="{ active: categoryId === undefined }"
            @click="selectCategory(undefined)"
          >
            全部
          </div>
          <div
            v-for="cat in categories"
            :key="cat.id"
            class="category-item"
            :class="{ active: categoryId === cat.id }"
            @click="selectCategory(cat.id)"
          >
            {{ cat.name }}
          </div>
        </div>

        <!-- 问题列表 -->
        <div class="list" v-loading="loading">
          <div v-for="q in questions" :key="q.id" class="question-card" @click="router.push(`/question/${q.id}`)">
            <div class="q-title">{{ q.title }}</div>
            <div class="q-meta">
              <span>{{ q.nickname }}</span>
              <el-tag size="small" type="primary" effect="plain">{{ q.categoryName }}</el-tag>
              <el-tag v-for="t in q.tags" :key="t" size="small">{{ t }}</el-tag>
            </div>
            <div class="q-stats">
              <span>{{ q.viewCount }} 浏览</span>
              <span>{{ q.likeCount }} 点赞</span>
              <span>{{ q.answerCount }} 回答</span>
              <span>{{ formatTime(q.createTime) }}</span>
            </div>
          </div>
          <el-empty v-if="!loading && questions.length === 0" description="暂无问题" />

          <el-pagination
            v-model:current-page="page"
            :page-size="size"
            :total="total"
            layout="prev, pager, next, total"
            @current-change="loadQuestions"
          />
        </div>

        <!-- 右侧排行榜：后端按 赞×2+浏览×0.1+回答×3（问题）/ 回答×1+采纳×5（回答者）算分，Redis 缓存 -->
        <div class="rankbar">
          <section class="rank-card">
            <div class="rank-title">🔥 热门问题 TOP10</div>
            <div
              v-for="q in hotQuestions"
              :key="q.questionId"
              class="rank-item"
              @click="router.push(`/question/${q.questionId}`)"
            >
              <span class="rank-no" :class="{ top: q.rank <= 3 }">{{ q.rank }}</span>
              <div class="rank-body">
                <div class="rank-q-title">{{ q.title }}</div>
                <div class="rank-q-stat">{{ q.viewCount }} 浏览 · {{ q.answerCount }} 回答</div>
              </div>
            </div>
            <el-empty v-if="hotQuestions.length === 0" description="暂无数据" :image-size="50" />
          </section>

          <section class="rank-card">
            <div class="rank-title">🏆 优秀回答者 TOP10</div>
            <div v-for="u in answerers" :key="u.userId" class="rank-item user-item">
              <span class="rank-no" :class="{ top: u.rank <= 3 }">{{ u.rank }}</span>
              <el-avatar :size="30" :src="u.avatar || ''">{{ u.nickname?.charAt(0) }}</el-avatar>
              <div class="rank-body">
                <div class="rank-u-name">{{ u.nickname }}</div>
                <div class="rank-q-stat">{{ u.answerCount }} 回答 · {{ u.acceptedCount }} 采纳</div>
              </div>
              <span class="rank-score">{{ u.score }}分</span>
            </div>
            <el-empty v-if="answerers.length === 0" description="暂无数据" :image-size="50" />
          </section>
        </div>
      </div>
    </el-main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { questionApi, categoryApi, rankApi } from '../api'
import { useUserStore } from '../stores/user'
import type { QuestionListVO, CategoryVO, HotQuestionVO, AnswererRankVO } from '../types'

const router = useRouter()
const userStore = useUserStore()

// banner 显示状态
const showBanner = ref(true)

// 跳转发布问题页
const handleAskClick = () => {
  router.push('/publish')
}

// 列表状态
const questions = ref<QuestionListVO[]>([])
const categories = ref<CategoryVO[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const categoryId = ref<number | undefined>(undefined)
const keyword = ref('')
const sort = ref<'time' | 'hot'>('time')

const loadQuestions = async () => {
  loading.value = true
  try {
    const res = await questionApi.page({
      page: page.value,
      size: size.value,
      categoryId: categoryId.value,
      keyword: keyword.value || undefined,
      sort: sort.value
    })
    questions.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const loadCategories = async () => {
  const res = await categoryApi.list()
  categories.value = res.data
}

// ---------- 排行榜（与问题列表独立加载，互不阻塞） ----------
const hotQuestions = ref<HotQuestionVO[]>([])
const answerers = ref<AnswererRankVO[]>([])

const loadRanks = async () => {
  // 两个榜单并发拉取；各自独立失败，不影响主列表（榜单是增强信息，降级为空态）
  const [hotRes, answererRes] = await Promise.all([
    rankApi.hot(10),
    rankApi.answerer(10)
  ])
  if (hotRes.code === 200) hotQuestions.value = hotRes.data || []
  if (answererRes.code === 200) answerers.value = answererRes.data || []
}

const selectCategory = (id: number | undefined) => {
  categoryId.value = id
  page.value = 1
  loadQuestions()
}

const formatTime = (time: string) => {
  return dayjs(time).format('YYYY-MM-DD HH:mm')
}

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}

onMounted(() => {
  loadCategories()
  loadQuestions()
  loadRanks()
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
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #606266;
}

.nickname-link {
  cursor: pointer;
  font-weight: 500;
}

.main {
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
  padding: 20px 24px;
}

/* 欢迎横幅：aspect-ratio 完整显示图片不裁剪 —— 图内百分比 == banner 百分比，按钮定位与视口宽度无关 */
.banner {
  position: relative;
  aspect-ratio: 16 / 9;
  height: auto;
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 20px;
  background: linear-gradient(90deg, #e0f2fe 0%, #dbeafe 100%);
}

.banner-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

/* 真实按钮覆盖图内"立即提问"（图内按钮 x 6.3%-16.6%, y 55.2%-63.5%；整图显示下纯百分比精确定位） */
.banner-ask-btn {
  position: absolute;
  left: 5.6%;
  top: 55%;
  width: 15%;
  height: 7%;
  min-width: 90px;
  min-height: 38px;
  padding: 0;
  font-size: clamp(13px, 1.55vw, 24px);
  font-weight: bold;
  letter-spacing: 2px;
  color: #fff;
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.4);
}

.banner-ask-btn:hover {
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  transform: translateY(-1px);
}

.banner-close {
  position: absolute;
  right: 12px;
  top: 10px;
  color: #fff;
  background: rgba(0, 0, 0, 0.25);
  border-radius: 4px;
}

/* 收起后的展开入口条 */
.banner-collapsed {
  display: flex;
  justify-content: center;
  padding: 8px 0 12px;
  margin-bottom: 4px;
}

/* 工具栏 */
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

/* 内容区 */
.content {
  display: flex;
  gap: 16px;
}

/* 侧栏：与右侧列表等高（去掉 align-self，flex 默认 stretch 拉伸） */
.sidebar {
  width: 150px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 12px;
  padding: 10px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

.category-item {
  padding: 10px 14px;
  border-radius: 8px;
  cursor: pointer;
  color: #606266;
  font-size: 14px;
  transition: all 0.15s;
}

.category-item:hover {
  background: #f5f7fa;
  color: #409eff;
}

.category-item.active {
  background: linear-gradient(90deg, #ecf5ff, #f3e8ff);
  color: #409eff;
  font-weight: bold;
}

/* 问题列表 */
.list {
  flex: 1;
  min-width: 0;
}

/* 右侧排行榜栏 */
.rankbar {
  width: 260px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.rank-card {
  background: #fff;
  border-radius: 12px;
  padding: 14px 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

.rank-title {
  font-size: 15px;
  font-weight: bold;
  padding: 0 4px 10px;
  border-bottom: 1px solid #f1f3f7;
  margin-bottom: 8px;
}

.rank-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 4px;
  border-radius: 8px;
  cursor: pointer;
}

.rank-item:hover {
  background: #f7f9fc;
}

/* 回答者行不可点（暂无用户主页），去掉 hover/指针 */
.user-item {
  cursor: default;
}

.user-item:hover {
  background: transparent;
}

.rank-no {
  width: 20px;
  flex-shrink: 0;
  text-align: center;
  font-size: 13px;
  font-weight: bold;
  color: #9ca3af;
}

/* 前三名金橙铜配色 */
.rank-no.top {
  color: #fff;
  background: linear-gradient(135deg, #f59e0b, #ef4444);
  border-radius: 6px;
  font-size: 12px;
  height: 20px;
  line-height: 20px;
}

.rank-body {
  flex: 1;
  min-width: 0;
}

.rank-q-title {
  font-size: 13px;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.rank-u-name {
  font-size: 13px;
  font-weight: 500;
}

.rank-q-stat {
  font-size: 11px;
  color: #9ca3af;
  margin-top: 2px;
}

.rank-score {
  font-size: 12px;
  color: #f59e0b;
  font-weight: bold;
  flex-shrink: 0;
}

/* 窄屏隐藏榜单栏，保证列表区可读宽度 */
@media (max-width: 1100px) {
  .rankbar {
    display: none;
  }
}

.question-card {
  background: #fff;
  border-radius: 12px;
  padding: 18px 20px;
  margin-bottom: 12px;
  cursor: pointer;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
  transition: all 0.15s;
}

.question-card:hover {
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.12);
  transform: translateY(-2px);
}

.q-title {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 10px;
}

.question-card:hover .q-title {
  color: #409eff;
}

.q-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  color: #909399;
  font-size: 13px;
}

.q-stats {
  display: flex;
  gap: 20px;
  color: #909399;
  font-size: 13px;
}

.q-stats span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

/* 分页居中 */
.list :deep(.el-pagination) {
  justify-content: center;
  margin-top: 20px;
}
</style>
