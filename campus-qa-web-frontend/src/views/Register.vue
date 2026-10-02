<template>
  <div class="register-container">
    <!-- 整图全屏铺满 -->
    <img class="bg-img" src="/images/login-bg.png" alt="校园问答平台" />

    <!-- 真实注册卡片：覆盖图内假表单区域 -->
    <div class="register-card">
      <h2>注册账号</h2>
      <p class="sub-title">加入我们，一起互助</p>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="0">
        <el-form-item prop="phone">
          <el-input v-model="form.phone" placeholder="手机号" maxlength="11" size="large" />
        </el-form-item>
        <el-form-item prop="nickname">
          <el-input v-model="form.nickname" placeholder="昵称" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" show-password size="large" />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" show-password size="large" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="btn-register" @click="handleRegister" style="width: 100%" size="large" :loading="loading">
            立即注册
          </el-button>
        </el-form-item>
      </el-form>
      <div class="card-footer">
        <el-button link type="primary" size="small" @click="router.push('/login')">
          已有账号？去登录
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormRules } from 'element-plus'
import { userApi } from '../api'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)

const form = reactive({
  phone: '',
  nickname: '',
  password: '',
  confirmPassword: ''
})

// 面试考点：自定义校验器确认密码一致；trigger blur 失焦即校验
const validateConfirmPassword = (_rule: any, value: string, callback: any) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 20, message: '昵称长度 2-20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度 6-20 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const formRef = ref()

// 注册即登录：后端 register 直接返回 LoginVO（token + nickname）
const handleRegister = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    const res = await userApi.register(form)
    userStore.setToken(res.data.token)
    const infoRes = await userApi.getInfo()
    userStore.setUser(infoRes.data)
    ElMessage.success('注册成功')
    router.push('/home')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-container {
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

.register-card {
  position: absolute;
  left: 63.8%;
  top: 48%;
  transform: translate(-50%, -50%);
  width: 27.5%;
  min-width: 380px;
  min-height: 56vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 32px 40px;
  background: rgba(255, 255, 255, 0.93);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border-radius: 20px;
  box-shadow: 0 12px 40px rgba(37, 99, 235, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.8);
}

.register-card h2 {
  text-align: center;
  margin: 0 0 6px;
  font-size: 24px;
  color: #111827;
  letter-spacing: 2px;
}

.sub-title {
  text-align: center;
  color: #6b7280;
  font-size: 14px;
  margin: 0 0 20px;
}

.btn-register {
  border: none;
  background: #3b82f6;
  color: #fff;
  font-weight: bold;
  font-size: 16px;
  letter-spacing: 4px;
  border-radius: 10px;
}

.btn-register:hover {
  background: #2563eb;
  color: #fff;
}

.card-footer {
  display: flex;
  justify-content: center;
  margin-top: -8px;
}

@media (max-width: 900px) {
  .register-card {
    left: 50%;
    transform: translate(-50%, -50%);
    width: 88%;
  }
}
</style>
