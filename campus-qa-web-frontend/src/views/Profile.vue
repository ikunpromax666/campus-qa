<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadRawFile, UploadRequestOptions } from 'element-plus'
import { userApi } from '../api'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()

// ---------- 基本资料 ----------
const profileForm = reactive({
  nickname: '',
  avatar: '',
  bio: ''
})
const profileSaving = ref(false)

onMounted(() => {
  // 未登录时路由守卫已拦截，这里正常回显 store 里的用户信息
  const u = userStore.user
  if (u) {
    profileForm.nickname = u.nickname || ''
    profileForm.avatar = u.avatar || ''
    profileForm.bio = u.bio || ''
  } else {
    // 刷新页面后 store 为空：先拉一次 /user/info 回显
    userApi.getInfo().then((res) => {
      if (res.code === 200 && res.data) {
        userStore.setUser(res.data)
        profileForm.nickname = res.data.nickname || ''
        profileForm.avatar = res.data.avatar || ''
        profileForm.bio = res.data.bio || ''
      }
    })
  }
})

async function saveProfile() {
  const nickname = profileForm.nickname.trim()
  if (!nickname) {
    ElMessage.warning('昵称不能为空')
    return
  }
  if (nickname.length > 20) {
    ElMessage.warning('昵称长度 1-20 位')
    return
  }
  profileSaving.value = true
  try {
    const res = await userApi.updateInfo({
      nickname,
      avatar: profileForm.avatar.trim(),
      bio: profileForm.bio.trim()
    })
    if (res.code === 200) {
      ElMessage.success('资料已保存')
      // 同步 store，导航栏昵称即时更新（重新拉取保证与后端一致）
      const info = await userApi.getInfo()
      if (info.code === 200 && info.data) userStore.setUser(info.data)
    } else {
      ElMessage.error(res.message || '保存失败')
    }
  } finally {
    profileSaving.value = false
  }
}

// ---------- 头像上传 ----------
const avatarUploading = ref(false)

// 上传前校验：类型白名单 + 大小限制（前端拦一次给即时提示，后端再拦一次防绕过 —— 双端校验）
function beforeAvatarUpload(file: UploadRawFile): boolean {
  const okTypes = ['image/jpeg', 'image/png', 'image/webp']
  if (!okTypes.includes(file.type)) {
    ElMessage.warning('仅支持 jpg/png/webp 格式')
    return false
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning('头像不能超过 2MB')
    return false
  }
  return true
}

// 自定义上传：必须走 axios（统一带 token、统一错误弹窗），
// 别用 el-upload 的 action 直传 —— 那是浏览器原生请求，会绕过请求/响应拦截器，
// 401 跳转和统一错误提示全部失效
async function handleAvatarUpload(options: UploadRequestOptions) {
  avatarUploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', options.file)
    const res = await userApi.uploadAvatar(fd)
    if (res.code === 200 && res.data) {
      // 后端上传成功时已同步落库，这里回显 + 刷新 store，导航栏头像即时更新
      profileForm.avatar = res.data
      ElMessage.success('头像已更新')
      const info = await userApi.getInfo()
      if (info.code === 200 && info.data) userStore.setUser(info.data)
    }
  } finally {
    avatarUploading.value = false
  }
}

// ---------- 修改密码 ----------
const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const pwdSaving = ref(false)

async function savePassword() {
  if (!pwdForm.oldPassword) {
    ElMessage.warning('请输入旧密码')
    return
  }
  if (pwdForm.newPassword.length < 6 || pwdForm.newPassword.length > 20) {
    ElMessage.warning('新密码长度 6-20 位')
    return
  }
  if (pwdForm.newPassword === pwdForm.oldPassword) {
    ElMessage.warning('新密码不能与旧密码相同')
    return
  }
  if (pwdForm.newPassword !== pwdForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  pwdSaving.value = true
  try {
    const res = await userApi.updatePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword,
      confirmPassword: pwdForm.confirmPassword
    })
    if (res.code === 200) {
      ElMessage.success('密码修改成功，请重新登录')
      // 密码变更后强制重新登录（JWT 无状态，旧 token 仍有效但强制登出更安全）
      userStore.logout()
      router.push('/login')
    } else {
      ElMessage.error(res.message || '修改失败')
    }
  } finally {
    pwdSaving.value = false
  }
}

const goBack = () => router.push('/')
</script>

<template>
  <div class="profile-page">
    <header class="navbar">
      <div class="nav-inner">
        <div class="logo" @click="goBack">
          <span class="logo-icon">🎓</span>
          <span class="logo-text">校园问答</span>
        </div>
        <el-button round size="small" @click="goBack">← 返回首页</el-button>
      </div>
    </header>

    <div class="container">
      <h2 class="page-title">👤 个人中心</h2>

      <!-- 基本资料 -->
      <section class="card">
        <h3 class="section-title">基本资料</h3>
        <el-form label-width="80px" size="large" class="profile-form">
          <el-form-item label="昵称">
            <el-input
              v-model="profileForm.nickname"
              maxlength="20"
              show-word-limit
              placeholder="1-20 位昵称"
            />
          </el-form-item>
          <el-form-item label="头像">
            <div class="avatar-row">
              <el-avatar :size="44" :src="profileForm.avatar || undefined">
                {{ profileForm.nickname?.charAt(0) || 'U' }}
              </el-avatar>
              <el-upload
                action="#"
                accept="image/jpeg,image/png,image/webp"
                :show-file-list="false"
                :before-upload="beforeAvatarUpload"
                :http-request="handleAvatarUpload"
              >
                <el-button size="small" round :loading="avatarUploading">本地上传</el-button>
              </el-upload>
              <span class="avatar-tip">jpg/png/webp，不超过 2MB</span>
            </div>
          </el-form-item>
          <el-form-item label="个人简介">
            <el-input
              v-model="profileForm.bio"
              type="textarea"
              :rows="3"
              maxlength="200"
              show-word-limit
              placeholder="介绍一下自己（200 字以内）"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" round :loading="profileSaving" @click="saveProfile">
              保存资料
            </el-button>
          </el-form-item>
        </el-form>
      </section>

      <!-- 修改密码 -->
      <section class="card">
        <h3 class="section-title">修改密码</h3>
        <el-form label-width="80px" size="large" class="profile-form">
          <el-form-item label="旧密码">
            <el-input
              v-model="pwdForm.oldPassword"
              type="password"
              show-password
              placeholder="输入当前密码"
            />
          </el-form-item>
          <el-form-item label="新密码">
            <el-input
              v-model="pwdForm.newPassword"
              type="password"
              show-password
              placeholder="6-20 位，不能与旧密码相同"
            />
          </el-form-item>
          <el-form-item label="确认密码">
            <el-input
              v-model="pwdForm.confirmPassword"
              type="password"
              show-password
              placeholder="再次输入新密码"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="warning" round :loading="pwdSaving" @click="savePassword">
              修改密码
            </el-button>
            <span class="pwd-tip">修改成功后需重新登录</span>
          </el-form-item>
        </el-form>
      </section>
    </div>
  </div>
</template>

<style scoped>
.profile-page {
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
  max-width: 760px;
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
  max-width: 760px;
  margin: 0 auto;
  padding: 24px 20px 60px;
}

.page-title {
  margin: 0 0 16px;
  color: #111827;
}

.card {
  background: #fff;
  border-radius: 12px;
  padding: 24px 28px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  margin-bottom: 16px;
}

.section-title {
  margin: 0 0 18px;
  font-size: 16px;
  color: #111827;
}

.profile-form {
  max-width: 520px;
}

.avatar-row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.avatar-tip {
  color: #9ca3af;
  font-size: 12px;
}

.pwd-tip {
  margin-left: 12px;
  color: #9ca3af;
  font-size: 12px;
}

@media (max-width: 640px) {
  .card {
    padding: 16px;
  }
}
</style>
