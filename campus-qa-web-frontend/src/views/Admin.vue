<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { adminApi, rankApi } from '../api'
import { useUserStore } from '../stores/user'
import type { AdminUserVO, AdminCategoryVO, AdminTagVO } from '../types'

// 面试点：进入本页有两道防线——路由守卫（前端 localStorage 的 role）+
// 后端 /admin/** 类级 @RequireAdmin（把守卫当真、把接口当假都不行，两层都要有）
const router = useRouter()
const userStore = useUserStore()

const activeTab = ref<'users' | 'categories' | 'tags'>('users')
const fmt = (t: string) => dayjs(t).format('YYYY-MM-DD HH:mm')

// ---------- 榜单维护 ----------
// 重算是全表扫描 + 回写 Redis 的高成本操作，所以只给管理员手动触发，不放前台
const refreshingHot = ref(false)
const refreshingAnswerer = ref(false)

async function refreshRank(kind: 'hot' | 'answerer') {
  const label = kind === 'hot' ? '热门问题榜' : '优秀回答者榜'
  try {
    await ElMessageBox.confirm(`立即重算${label}并刷新缓存？数据量大时可能耗时数秒。`, '刷新榜单', {
      confirmButtonText: '重算',
      cancelButtonText: '取消',
      type: 'info'
    })
  } catch {
    return
  }
  if (kind === 'hot') refreshingHot.value = true
  else refreshingAnswerer.value = true
  try {
    const res =
      kind === 'hot' ? await rankApi.refreshHot() : await rankApi.refreshAnswerer()
    if (res.code === 200) ElMessage.success(`${label}已刷新`)
    else ElMessage.error(res.message)
  } finally {
    refreshingHot.value = false
    refreshingAnswerer.value = false
  }
}

// ---------- 用户管理 ----------
const users = ref<AdminUserVO[]>([])
const userTotal = ref(0)
const userPage = ref(1)
const userSize = ref(10)
const keyword = ref('')
const statusFilter = ref<number | undefined>(undefined)
const userLoading = ref(false)

async function loadUsers() {
  userLoading.value = true
  try {
    const res = await adminApi.users({
      page: userPage.value,
      size: userSize.value,
      keyword: keyword.value || undefined,
      status: statusFilter.value
    })
    if (res.code === 200) {
      users.value = res.data.records || []
      userTotal.value = res.data.total
    } else {
      ElMessage.error(res.message)
    }
  } finally {
    userLoading.value = false
  }
}

const searchUsers = () => {
  userPage.value = 1
  loadUsers()
}

// 启用/禁用：后端拒绝操作自己（50001），前端也把"自己"那行按钮禁掉（双保险）
async function toggleUserStatus(u: AdminUserVO) {
  const target = u.status === 0 ? 1 : 0
  const action = target === 1 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定${action}用户「${u.nickname}」吗？`, `${action}用户`, {
      confirmButtonText: action,
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  const res = await adminApi.updateUserStatus(u.id, target)
  if (res.code === 200) {
    ElMessage.success(`已${action}`)
    loadUsers()
  } else {
    ElMessage.error(res.message)
  }
}

// ---------- 分类管理 ----------
const categories = ref<AdminCategoryVO[]>([])
const catLoading = ref(false)
const catDialogVisible = ref(false)
const catEditingId = ref<number | null>(null) // null=新增
const catForm = ref({ name: '', description: '', sortOrder: 0 })

async function loadCategories() {
  catLoading.value = true
  try {
    const res = await adminApi.categories()
    if (res.code === 200) {
      categories.value = res.data || []
    } else {
      ElMessage.error(res.message)
    }
  } finally {
    catLoading.value = false
  }
}

function openCreateCategory() {
  catEditingId.value = null
  catForm.value = { name: '', description: '', sortOrder: 0 }
  catDialogVisible.value = true
}

function openEditCategory(c: AdminCategoryVO) {
  catEditingId.value = c.id
  catForm.value = { name: c.name, description: c.description, sortOrder: c.sortOrder }
  catDialogVisible.value = true
}

async function submitCategory() {
  if (!catForm.value.name.trim()) {
    ElMessage.warning('分类名称不能为空')
    return
  }
  const payload = {
    name: catForm.value.name.trim(),
    description: catForm.value.description.trim() || undefined,
    sortOrder: catForm.value.sortOrder
  }
  const res =
    catEditingId.value === null
      ? await adminApi.createCategory(payload)
      : await adminApi.updateCategory(catEditingId.value, payload)
  if (res.code === 200) {
    ElMessage.success(catEditingId.value === null ? '新增成功' : '修改成功')
    catDialogVisible.value = false
    loadCategories()
  } else {
    ElMessage.error(res.message) // 重名 20010 等业务提示
  }
}

async function deleteCategory(c: AdminCategoryVO) {
  try {
    await ElMessageBox.confirm(
      `确定删除分类「${c.name}」吗？分类下有正常状态的问题时会被拒绝。`,
      '删除分类',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  const res = await adminApi.deleteCategory(c.id)
  if (res.code === 200) {
    ElMessage.success('已删除')
    loadCategories()
  } else {
    ElMessage.error(res.message) // 20008：分类使用中
  }
}

// ---------- 标签管理 ----------
const tags = ref<AdminTagVO[]>([])
const tagLoading = ref(false)
const tagDialogVisible = ref(false)
const tagName = ref('')

async function loadTags() {
  tagLoading.value = true
  try {
    const res = await adminApi.tags()
    if (res.code === 200) {
      tags.value = res.data || []
    } else {
      ElMessage.error(res.message)
    }
  } finally {
    tagLoading.value = false
  }
}

async function submitTag() {
  if (!tagName.value.trim()) {
    ElMessage.warning('标签名称不能为空')
    return
  }
  const res = await adminApi.createTag(tagName.value.trim())
  if (res.code === 200) {
    ElMessage.success('新增成功')
    tagDialogVisible.value = false
    tagName.value = ''
    loadTags()
  } else {
    ElMessage.error(res.message) // 重名 20011
  }
}

async function deleteTag(t: AdminTagVO) {
  try {
    await ElMessageBox.confirm(`确定删除标签「${t.name}」吗？`, '删除标签', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  const res = await adminApi.deleteTag(t.id)
  if (res.code === 200) {
    ElMessage.success('已删除')
    loadTags()
  } else {
    ElMessage.error(res.message) // 20009：标签使用中
  }
}

const switchTab = (name: 'users' | 'categories' | 'tags') => {
  activeTab.value = name
  if (name === 'users') loadUsers()
  if (name === 'categories') loadCategories()
  if (name === 'tags') loadTags()
}

onMounted(() => {
  // 二次校验：守卫读的是 localStorage，可能被手动篡改——以服务端为准
  if (userStore.user && userStore.user.role !== 1) {
    ElMessage.error('无管理员权限')
    router.replace('/')
    return
  }
  loadUsers()
})
</script>

<template>
  <div class="admin-page">
    <!-- 顶部导航 -->
    <header class="navbar">
      <div class="nav-inner">
        <div class="logo" @click="router.push('/')">
          <span class="logo-icon">🛡️</span>
          <span class="logo-text">管理后台</span>
        </div>
        <el-button round size="small" @click="router.push('/')">← 返回首页</el-button>
      </div>
    </header>

    <div class="container">
      <!-- 榜单维护：高成本重算操作，仅管理员手动触发 -->
      <div class="rank-ops">
        <span class="rank-ops-label">榜单维护：</span>
        <el-button size="small" :loading="refreshingHot" @click="refreshRank('hot')">
          🔥 刷新热门问题榜
        </el-button>
        <el-button size="small" :loading="refreshingAnswerer" @click="refreshRank('answerer')">
          🏆 刷新优秀回答者榜
        </el-button>
      </div>

      <el-tabs v-model="activeTab" class="admin-tabs" @tab-change="switchTab">
        <!-- ===== 用户管理 ===== -->
        <el-tab-pane label="用户管理" name="users">
          <div class="toolbar">
            <el-input
              v-model="keyword"
              placeholder="按昵称搜索"
              clearable
              class="search-input"
              @keyup.enter="searchUsers"
              @clear="searchUsers"
            />
            <el-select
              v-model="statusFilter"
              placeholder="全部状态"
              clearable
              class="status-select"
              @change="searchUsers"
            >
              <el-option label="正常" :value="0" />
              <el-option label="禁用" :value="1" />
            </el-select>
            <el-button type="primary" @click="searchUsers">搜索</el-button>
          </div>

          <el-table v-loading="userLoading" :data="users" stripe>
            <el-table-column label="用户" min-width="200">
              <template #default="{ row }">
                <div class="user-cell">
                  <el-avatar :size="32" :src="row.avatar">{{ row.nickname?.charAt(0) }}</el-avatar>
                  <span>{{ row.nickname }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="phone" label="手机号" width="140" />
            <el-table-column label="角色" width="100">
              <template #default="{ row }">
                <el-tag :type="row.role === 1 ? 'danger' : 'info'" size="small">
                  {{ row.role === 1 ? '管理员' : '用户' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 0 ? 'success' : 'warning'" size="small">
                  {{ row.status === 0 ? '正常' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="注册时间" width="170">
              <template #default="{ row }">{{ fmt(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button
                  link
                  :type="row.status === 0 ? 'danger' : 'success'"
                  :disabled="row.id === userStore.user?.id"
                  @click="toggleUserStatus(row)"
                >
                  {{ row.status === 0 ? '禁用' : '启用' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="userPage"
            :page-size="userSize"
            :total="userTotal"
            layout="prev, pager, next, total"
            class="pager"
            @current-change="loadUsers"
          />
        </el-tab-pane>

        <!-- ===== 分类管理 ===== -->
        <el-tab-pane label="分类管理" name="categories">
          <div class="toolbar">
            <el-button type="primary" @click="openCreateCategory">+ 新增分类</el-button>
          </div>
          <el-table v-loading="catLoading" :data="categories" stripe>
            <el-table-column prop="name" label="名称" min-width="140" />
            <el-table-column prop="description" label="描述" min-width="240" show-overflow-tooltip />
            <el-table-column prop="sortOrder" label="排序" width="90" />
            <el-table-column label="创建时间" width="170">
              <template #default="{ row }">{{ fmt(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openEditCategory(row)">编辑</el-button>
                <el-button link type="danger" @click="deleteCategory(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- ===== 标签管理 ===== -->
        <el-tab-pane label="标签管理" name="tags">
          <div class="toolbar">
            <el-button type="primary" @click="tagDialogVisible = true">+ 新增标签</el-button>
          </div>
          <el-table v-loading="tagLoading" :data="tags" stripe>
            <el-table-column prop="name" label="名称" min-width="160" />
            <el-table-column label="创建时间" width="170">
              <template #default="{ row }">{{ fmt(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button link type="danger" @click="deleteTag(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 分类新增/编辑对话框 -->
    <el-dialog
      v-model="catDialogVisible"
      :title="catEditingId === null ? '新增分类' : '编辑分类'"
      width="440px"
    >
      <el-form label-width="70px">
        <el-form-item label="名称" required>
          <el-input v-model="catForm.name" maxlength="20" placeholder="分类名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="catForm.description" maxlength="100" placeholder="一句话描述（可选）" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="catForm.sortOrder" :min="0" :max="999" />
          <span class="sort-hint">数值越大越靠前</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="catDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCategory">保存</el-button>
      </template>
    </el-dialog>

    <!-- 标签新增对话框 -->
    <el-dialog v-model="tagDialogVisible" title="新增标签" width="380px">
      <el-input v-model="tagName" maxlength="20" placeholder="标签名称" @keyup.enter="submitTag" />
      <template #footer>
        <el-button @click="tagDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitTag">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.admin-page {
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
  max-width: 1080px;
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
  background: linear-gradient(135deg, #ef4444, #f59e0b);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.container {
  max-width: 1080px;
  margin: 0 auto;
  padding: 20px;
}

.rank-ops {
  background: #fff;
  border-radius: 10px;
  padding: 12px 16px;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 10px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.rank-ops-label {
  font-size: 13px;
  color: #6b7280;
  font-weight: 500;
}

.admin-tabs {
  background: #fff;
  border-radius: 12px;
  padding: 8px 20px 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
  align-items: center;
}

.search-input {
  width: 220px;
}

.status-select {
  width: 130px;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

.sort-hint {
  margin-left: 10px;
  color: #9ca3af;
  font-size: 12px;
}
</style>
