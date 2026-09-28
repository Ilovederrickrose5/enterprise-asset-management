import { defineStore } from 'pinia'

const STORAGE_KEYS = ['user', 'token', 'username', 'role', 'department']

// 角色优先级：admin > leader > manager > user
const ROLE_PRIORITY = { admin: 4, leader: 3, manager: 2, user: 1 }

// 统一的角色解析：兼容 roles 数组 / role 字段 / 用户名推断
function parseRole(user) {
  if (!user) return 'user'

  // 1. 优先使用后端返回的 roles 数组
  if (Array.isArray(user.roles) && user.roles.length > 0) {
    let best = 'user'
    let bestScore = 1
    for (const r of user.roles) {
      let name = typeof r === 'string' ? r : (r.name || r.code || '')
      name = String(name).toLowerCase()
      if (name.startsWith('role_')) name = name.substring(5)
      const score = ROLE_PRIORITY[name]
      if (score && score > bestScore) {
        best = name
        bestScore = score
      }
    }
    if (bestScore > 1) return best
  }

  // 2. 兼容旧版 role 字段
  if (user.role) {
    const r = String(user.role).toLowerCase()
    if (ROLE_PRIORITY[r]) return r
  }

  // 3. 用户名推断（兼容 chartPermission 中旧逻辑）
  if (user.username) {
    const u = String(user.username).toLowerCase()
    if (u === 'admin') return 'admin'
    if (u.startsWith('leader')) return 'leader'
    if (u.startsWith('admin_')) return 'manager'
    if (u.startsWith('user')) return 'user'
  }

  return 'user'
}

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    user: JSON.parse(localStorage.getItem('user') || 'null'),
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
    currentUserRole: (state) => parseRole(state.user),
    // 用法：userStore.hasRole('admin') 或 userStore.hasRole(['admin','manager'])
    hasRole: (state) => (roles) => {
      if (!roles) return true
      const arr = Array.isArray(roles) ? roles : [roles]
      return arr.includes(parseRole(state.user))
    },
    userId: (state) => (state.user ? state.user.id : null),
    departmentId: (state) => (state.user ? state.user.departmentId : null),
  },
  actions: {
    // 登录：写入 store + localStorage
    login(userData, token) {
      this.user = userData
      this.token = token
      localStorage.setItem('user', JSON.stringify(userData))
      localStorage.setItem('token', token)
    },
    // 登出：清空 store + localStorage
    logout() {
      this.user = null
      this.token = ''
      STORAGE_KEYS.forEach((k) => localStorage.removeItem(k))
    },
    // 启动时从 localStorage 恢复（state 已在创建时读取，此方法用于外部变更后同步）
    initFromStorage() {
      this.token = localStorage.getItem('token') || ''
      try {
        this.user = JSON.parse(localStorage.getItem('user') || 'null')
      } catch (e) {
        this.user = null
      }
    },
  },
})
