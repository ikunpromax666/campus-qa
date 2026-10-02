<template>
  <div class="login-container">
    <!-- 整图全屏铺满：图内自带登录卡片设计，真表单覆盖其上 -->
    <img class="bg-img" src="/images/login-bg.png" alt="校园问答平台" />

    <!-- 真实登录卡片：覆盖图内假表单区域（右侧），样式仿图设计 -->
    <div class="login-card">
      <h2>校园问答平台</h2>
      <p class="sub-title">欢迎回来</p>
      <el-form :model="form" :rules="rules" ref="formRef">
        <el-form-item prop="phone">
          <el-input v-model="form.phone" placeholder="账号" maxlength="11" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" show-password size="large" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="btn-login" @click="handleLogin" style="width: 100%" size="large" :loading="loading">
            立即登录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="card-footer">
        <el-button link size="small" @click="ElMessage.info('请联系管理员重置密码')">忘记密码</el-button>
        <el-button link type="primary" size="small" @click="router.push('/register')">注册账号</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { useUserStore } from '../stores/user'
import type { Result, LoginVO, UserInfoVO } from '../types'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)

const form = reactive({
  phone: '',
  password: ''
})

const rules = {
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const formRef = ref()

const handleLogin = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    const res = await request.post<Result<LoginVO>>('/user/login', form)
    userStore.setToken(res.data.token)
    // 登录只返回 token + nickname，用户详情再拉 /user/info
    const infoRes = await request.get<Result<UserInfoVO>>('/user/info')
    userStore.setUser(infoRes.data)
    ElMessage.success('登录成功')
    router.push('/home')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  position: relative;
  height: 100vh;
  overflow: hidden;
  background: linear-gradient(135deg, #fdf2f8 0%, #e0f2fe 100%);
}

.bg-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

/*
 * 真实登录卡片：覆盖图片自带的假表单（图内假卡约在 x 50%-77%, y 25%-75%）。
 * 不透明白底 + 强模糊：即使有轻微错位，透出的假表单也被 blur 抹成色块不可读。
 */
.login-card {
  position: absolute;
  left: 63.8%;
  top: 48%;
  transform: translate(-50%, -50%);
  width: 27.5%;
  min-width: 380px;
  min-height: 52vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 36px 40px;
  background: rgba(255, 255, 255, 0.93);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border-radius: 20px;
  box-shadow: 0 12px 40px rgba(37, 99, 235, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.8);
}

.login-card h2 {
  text-align: center;
  margin: 0 0 6px;
  font-size: 26px;
  color: #111827;
  letter-spacing: 2px;
}

.sub-title {
  text-align: center;
  color: #6b7280;
  font-size: 14px;
  margin: 0 0 30px;
}

/* 仿图设计：纯蓝圆角大按钮 */
.btn-login {
  border: none;
  background: #3b82f6;
  font-weight: bold;
  font-size: 16px;
  letter-spacing: 4px;
  border-radius: 10px;
}

.btn-login:hover {
  background: #2563eb;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  margin-top: -8px;
}

/* 窄屏：卡片居中 */
@media (max-width: 900px) {
  .login-card {
    left: 50%;
    transform: translate(-50%, -50%);
    width: 88%;
  }
}
</style>
