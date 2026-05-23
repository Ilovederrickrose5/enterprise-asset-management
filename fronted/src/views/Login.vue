<template>
  <div class="login-container">
    <div class="login-wrapper">
      <div class="login-left">
        <div class="login-left-content">
          <h1 class="login-title">企业固定资产管理系统</h1>
          <p class="login-subtitle">高效管理企业固定资产，提升运营效率</p>
          <div class="login-features">
            <div class="feature-item">
              <el-icon class="feature-icon"><CircleCheck /></el-icon>
              <span>全生命周期管理</span>
            </div>
            <div class="feature-item">
              <el-icon class="feature-icon"><DataAnalysis /></el-icon>
              <span>智能数据分析</span>
            </div>
            <div class="feature-item">
              <el-icon class="feature-icon"><Lock /></el-icon>
              <span>安全可靠</span>
            </div>
          </div>
        </div>
      </div>
      <div class="login-right">
        <el-card class="login-card">
          <template #header>
            <div class="card-header">
              <h3>用户登录</h3>
              <p class="card-subtitle">请输入您的账号和密码</p>
            </div>
          </template>
          
          <el-form :model="loginForm" :rules="rules" ref="loginFormRef" label-position="top">
            <el-form-item label="用户名" prop="username">
              <el-input 
                v-model="loginForm.username" 
                placeholder="请输入用户名"
                :prefix-icon="User"
                clearable
                class="login-input"
              />
            </el-form-item>
            
            <el-form-item label="密码" prop="password">
              <el-input 
                v-model="loginForm.password" 
                type="password" 
                placeholder="请输入密码"
                :prefix-icon="Lock"
                show-password
                @keyup.enter="handleLogin"
                class="login-input"
              />
            </el-form-item>
            
            <el-form-item>
              <el-button 
                type="primary" 
                :loading="isLoading" 
                @click="handleLogin"
                class="login-button"
              >
                {{ isLoading ? '登录中...' : '登录' }}
              </el-button>
            </el-form-item>
            
            <el-alert
              v-if="error"
              :title="error"
              type="error"
              :closable="false"
              show-icon
              class="login-error"
            />
            
            <el-alert
              title="如有登录问题请联系系统管理员"
              type="info"
              :closable="false"
              show-icon
              class="login-support"
            />
          </el-form>
        </el-card>
        <div class="login-footer">
          <p>© 2026 企业固定资产管理系统. 保留所有权利.</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from '../utils/request'
import { User, Lock, CircleCheck, DataAnalysis } from '@element-plus/icons-vue'
import { normalizeUser } from '../utils/common'

export default {
  name: 'Login',
  setup() {
    const router = useRouter()
    const loginFormRef = ref(null)
    const loginForm = ref({
      username: '',
      password: ''
    })
    const isLoading = ref(false)
    const error = ref('')
    
    const rules = {
      username: [
        { required: true, message: '请输入用户名', trigger: 'blur' }
      ],
      password: [
        { required: true, message: '请输入密码', trigger: 'blur' }
      ]
    }
    

    
    const handleLogin = async () => {
      if (!loginFormRef.value) return
      
      await loginFormRef.value.validate(async (valid) => {
        if (!valid) return
        
        isLoading.value = true
        error.value = ''
        
        try {
          const response = await axios.post('/auth/login', loginForm.value)
          
          console.log('登录响应:', response.data)
          
          if (response.data.code === 200) {
            console.log('登录成功，原始用户数据:', response.data.data)
            
            // 使用字段映射函数统一用户数据字段名
            const normalizedUser = normalizeUser(response.data.data)
            console.log('标准化后的用户数据:', normalizedUser)
            
            localStorage.setItem('token', response.data.data.token)
            localStorage.setItem('user', JSON.stringify(normalizedUser))
            
            router.push('/home')
          } else {
            error.value = response.data.message
          }
        } catch (err) {
          console.error('登录错误:', err)
          if (err.response?.data?.message && err.response.data.message.includes('User is disabled')) {
            error.value = '您现在暂时无法登录，请联系系统管理员 13800138001'
          } else {
            error.value = err.response?.data?.message || '登录失败，请检查网络连接'
          }
        } finally {
          isLoading.value = false
        }
      })
    }
    
    return {
      loginFormRef,
      loginForm,
      isLoading,
      error,
      rules,
      User,
      Lock,
      CircleCheck,
      DataAnalysis,
      handleLogin
    }
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
  padding: 20px;
}

.login-wrapper {
  display: flex;
  width: 100%;
  max-width: 1200px;
  height: 700px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  animation: fadeIn 0.8s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.login-left {
  flex: 1;
  background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px;
}

.login-left-content {
  color: white;
  text-align: center;
  max-width: 400px;
}

.login-title {
  font-size: 2.5rem;
  font-weight: 700;
  margin-bottom: 20px;
  line-height: 1.2;
  animation: slideInLeft 1s ease-out;
}

.login-subtitle {
  font-size: 1.1rem;
  margin-bottom: 40px;
  opacity: 0.9;
  animation: slideInLeft 1s ease-out 0.2s both;
}

.login-features {
  display: flex;
  flex-direction: column;
  gap: 20px;
  align-items: flex-start;
  animation: slideInLeft 1s ease-out 0.4s both;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 1rem;
  opacity: 0.9;
  transition: transform 0.3s ease;
}

.feature-item:hover {
  transform: translateX(10px);
}

.feature-icon {
  font-size: 1.2rem;
  color: #fcd34d;
}

.login-right {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px;
  animation: slideInRight 1s ease-out;
}

@keyframes slideInLeft {
  from {
    opacity: 0;
    transform: translateX(-30px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes slideInRight {
  from {
    opacity: 0;
    transform: translateX(30px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.login-card {
  width: 100%;
  max-width: 400px;
  box-shadow: none;
  border: none;
}

.card-header {
  text-align: center;
  margin-bottom: 30px;
}

.card-header h3 {
  margin: 0 0 8px 0;
  color: #1f2937;
  font-size: 1.5rem;
  font-weight: 600;
}

.card-subtitle {
  margin: 0;
  color: #6b7280;
  font-size: 0.9rem;
}

:deep(.el-card__header) {
  background-color: transparent;
  border-bottom: none;
  padding: 0;
}

:deep(.el-form) {
  width: 100%;
}

:deep(.el-form-item) {
  margin-bottom: 20px;
}

:deep(.el-form-item__label) {
  font-weight: 500;
  color: #374151;
  margin-bottom: 8px;
}

.login-input {
  height: 48px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  transition: all 0.3s ease;
}

.login-input:focus {
  border-color: #4f46e5;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}

:deep(.el-input__wrapper) {
  border-radius: 8px;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}

.login-button {
  width: 100%;
  height: 48px;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 500;
  background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
  border: none;
  transition: all 0.3s ease;
  margin-top: 10px;
}

.login-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
}

.login-button:active {
  transform: translateY(0);
}

.login-error {
  margin-top: 20px;
  animation: shake 0.5s ease-in-out;
}

.login-support {
  margin-top: 16px;
  border-radius: 8px;
  border-left: 4px solid #4f46e5;
  background-color: rgba(79, 70, 229, 0.05);
  animation: fadeIn 0.8s ease-out 0.6s both;
}

:deep(.login-support .el-alert__content) {
  color: #4f46e5;
  font-size: 0.9rem;
  font-weight: 400;
}

@keyframes shake {
  0%, 100% {
    transform: translateX(0);
  }
  25% {
    transform: translateX(-5px);
  }
  75% {
    transform: translateX(5px);
  }
}

.login-footer {
  margin-top: 30px;
  text-align: center;
  color: #9ca3af;
  font-size: 0.875rem;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .login-wrapper {
    flex-direction: column;
    height: auto;
    max-width: 450px;
  }
  
  .login-left {
    padding: 40px;
  }
  
  .login-right {
    padding: 40px;
  }
  
  .login-title {
    font-size: 2rem;
  }
  
  .login-features {
    align-items: center;
  }
}
</style>
