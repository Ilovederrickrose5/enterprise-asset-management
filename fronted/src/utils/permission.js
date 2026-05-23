// 权限控制工具函数
import { menuConfig } from '../config/menuConfig'

// 获取用户的真实角色（优先从后端返回的角色信息获取）
export function getCurrentUserRole() {
  const userStr = localStorage.getItem('user')
  if (!userStr) return 'user'

  try {
    const user = JSON.parse(userStr)

    // 优先使用后端返回的角色数组
    if (user.roles && user.roles.length > 0) {
      for (const r of user.roles) {
        let roleName = ''
        if (typeof r === 'string') {
          roleName = r.toLowerCase()
        } else if (r.name) {
          roleName = r.name.toLowerCase()
        }
        if (roleName.startsWith('role_')) {
          roleName = roleName.substring(5) // 移除ROLE_前缀
        }
        // 返回第一个有效的角色（admin > leader > manager > user）
        if (roleName === 'admin') return 'admin'
        if (roleName === 'leader') return 'leader'
        if (roleName === 'manager') return 'manager'
        if (roleName === 'user') return 'user'
      }
    }

    // 兼容旧版用户数据（使用role字段）
    if (user.role) {
      const roleName = user.role.toLowerCase()
      if (roleName === 'admin') return 'admin'
      if (roleName === 'leader') return 'leader'
      if (roleName === 'manager') return 'manager'
      if (roleName === 'user') return 'user'
    }
  } catch (e) {
    // 忽略解析错误
  }

  return 'user'
}

// 检查用户是否有权限访问路由
export function hasPermission(route, userRole) {
  if (!route.meta || !route.meta.roles) {
    return true // 没有配置角色权限的路由默认允许访问
  }

  return route.meta.roles.includes(userRole)
}

// 检查用户是否有权限执行操作
export function hasOperationPermission(requiredRole, userRole) {
  const roleHierarchy = {
    'admin': 4,
    'leader': 3,
    'manager': 2,
    'user': 1
  }

  return (roleHierarchy[userRole] || 0) >= (roleHierarchy[requiredRole] || 0)
}

// 检查用户是否为管理员
export function hasAdminPermission() {
  const userStr = localStorage.getItem('user')

  if (userStr) {
    try {
      const user = JSON.parse(userStr)
      // 优先使用后端返回的角色数组
      if (user.roles && user.roles.length > 0) {
        for (const r of user.roles) {
          let roleName = ''
          if (typeof r === 'string') {
            roleName = r.toLowerCase()
          } else if (r.name) {
            roleName = r.name.toLowerCase()
          }
          if (roleName.startsWith('role_')) {
            roleName = roleName.substring(5) // 移除ROLE_前缀
          }
          if (roleName === 'admin') {
            return true
          }
        }
      }

      // 兼容旧版用户数据（使用role字段）
      if (user.role) {
        const roleName = user.role.toLowerCase()
        if (roleName === 'admin') {
          return true
        }
      }
    } catch (e) {
      // 忽略解析错误
    }
  }

  return false
}

// 获取用户可访问的菜单
export function getAccessibleMenus() {
  // 获取当前用户角色
  let userRole = 'user'
  const userStr = localStorage.getItem('user')

  if (userStr) {
    try {
      const user = JSON.parse(userStr)
      // 优先使用后端返回的角色数组
      if (user.roles && user.roles.length > 0) {
        // 优先选择admin角色，其次是leader，然后是manager，最后是user
        let role = 'user'
        for (const r of user.roles) {
          let roleName = ''
          if (typeof r === 'string') {
            roleName = r.toLowerCase()
          } else if (r.name) {
            roleName = r.name.toLowerCase()
          }
          if (roleName.startsWith('role_')) {
            roleName = roleName.substring(5) // 移除ROLE_前缀
          }
          if (roleName === 'admin') {
            role = 'admin'
            break
          } else if (roleName === 'leader' && role !== 'admin') {
            role = 'leader'
          } else if (roleName === 'manager' && role !== 'admin' && role !== 'leader') {
            role = 'manager'
          }
        }
        userRole = role
      } else if (user.role) {
        // 兼容旧版用户数据（使用role字段）
        const roleName = user.role.toLowerCase()
        if (roleName === 'admin') userRole = 'admin'
        else if (roleName === 'leader') userRole = 'leader'
        else if (roleName === 'manager') userRole = 'manager'
        else if (roleName === 'user') userRole = 'user'
      }
    } catch (e) {
      // 忽略解析错误
    }
  }

  // 深拷贝菜单数组，避免修改原始数据
  const filteredMenus = JSON.parse(JSON.stringify(menuConfig))

  return filteredMenus.filter(menu => {
    // 检查父菜单权限
    if (menu.roles && !menu.roles.includes(userRole)) {
      return false
    }

    // 如果有子菜单，也需要过滤子菜单
    if (menu.children && menu.children.length > 0) {
      menu.children = menu.children.filter(child => {
        if (!child.roles) return true

        // 检查子菜单权限
        if (!child.roles.includes(userRole)) {
          return false
        }

        // 根据角色设置不同的菜单标题
        if (child.titleByRole && child.titleByRole[userRole]) {
          child.title = child.titleByRole[userRole]
        }

        return true
      })
      // 如果过滤后没有子菜单了，且父菜单本身没有权限配置，则不显示父菜单
      if (menu.children.length === 0 && !menu.roles) {
        return false
      }
    }

    return true
  })
}
