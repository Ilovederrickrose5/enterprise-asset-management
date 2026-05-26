<template>
  <el-container class="home-container">
    <el-header class="header">
      <div class="header-content">
        <div class="header-left">
          <h1 v-if="!isCollapse">企业固定资产管理系统</h1>
          <h1 v-else style="margin-left: 10px; font-size: 1.2rem;">资产管理</h1>
        </div>
        <div class="user-info">
          <el-tag type="success" effect="dark">欢迎，{{ getUserName() }}</el-tag>
          <el-dropdown trigger="click">
            <el-button circle>
              <el-icon><Setting /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="navigateTo('/settings')">系统设置</el-dropdown-item>
                <el-dropdown-item @click="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </el-header>
    
    <el-container class="content">
      <el-aside :width="isCollapse ? '80px' : '240px'" class="sidebar" :class="{ 'sidebar-collapsed': isCollapse }">
        <el-menu
          :default-active="currentMenu"
          class="el-menu-vertical"
          @select="handleMenuSelect"
          :collapse="isCollapse"
        >
          <template v-for="menu in accessibleMenus" :key="menu.id">
            <!-- 有子菜单的情况 -->
            <el-sub-menu v-if="menu.children && menu.children.length > 0" :index="String(menu.id)">
              <template #title>
                <el-icon><component :is="getMenuIcon(menu.id)" /></el-icon>
                <span v-if="!isCollapse">{{ menu.title }}</span>
              </template>
              <el-menu-item 
                v-for="subMenu in menu.children" 
                :key="subMenu.id"
                :index="String(subMenu.id)"
              >
                <el-icon><component :is="getMenuIcon(subMenu.id)" /></el-icon>
                <span v-if="!isCollapse">{{ getMenuTitle(subMenu) }}</span>
              </el-menu-item>
            </el-sub-menu>
            <!-- 没有子菜单的情况 -->
            <el-menu-item v-else :index="String(menu.id)">
              <el-icon><component :is="getMenuIcon(menu.id)" /></el-icon>
              <span v-if="!isCollapse">{{ menu.title }}</span>
            </el-menu-item>
          </template>
        </el-menu>
        
        <!-- 待处理任务 -->
        <el-card v-if="!isCollapse && pendingTasks.length > 0" class="pending-tasks-card" shadow="hover" style="margin-top: 20px;">
          <template #header>
            <div class="card-header">
              <span class="task-count">{{ pendingTasks.length }}</span>
              <span>待我处理</span>
            </div>
          </template>
          
          <el-scrollbar max-height="200px">
            <el-timeline>
              <el-timeline-item
                v-for="(task, index) in pendingTasks"
                :key="index"
                :timestamp="task.timeText"
                placement="top"
              >
                <div class="task-content">
                  <el-tag type="warning" size="small" class="task-type">{{ getTypeLabel(task.type) }}</el-tag>
                  <span>{{ task.content }}</span>
                </div>
              </el-timeline-item>
            </el-timeline>
          </el-scrollbar>
        </el-card>
        
        <!-- 最近动态 -->
        <el-card v-if="!isCollapse" class="recent-activities-card" shadow="hover" style="margin-top: 20px;">
          <template #header>
            <div class="card-header">
              <span>最近动态</span>
            </div>
          </template>
          
          <el-scrollbar max-height="250px">
            <el-timeline v-if="recentActivities.length > 0">
              <el-timeline-item
                v-for="(activity, index) in recentActivities"
                :key="index"
                :timestamp="activity.timeText"
                placement="top"
              >
                <div class="activity-content">
                  <el-tag :type="getActivityTagType(activity.type)" size="small" class="activity-type">{{ getTypeLabel(activity.type) }}</el-tag>
                  <span><strong>{{ activity.operator }}：</strong>{{ activity.content }}</span>
                </div>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else description="暂无操作记录" />
          </el-scrollbar>
        </el-card>
      </el-aside>
      
      <el-main class="main-content">
        <!-- 资产搜索 -->
        <div class="search-filter-container">
          <div class="search-filter-section">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索资产..."
              size="small"
              class="search-input"
              @keyup.enter="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
              <template #suffix>
                <el-button type="primary" plain size="small" @click="handleSearch">
                  搜索
                </el-button>
              </template>
            </el-input>
          </div>
        </div>

        //根据用户角色展示不同的资产概览卡片
        <el-card shadow="hover" style="margin-bottom: 20px;">
          <template #header>
            <div class="card-header">
              <el-icon><User /></el-icon>
              <span>{{ getUserRoleFromLocalStorage() === 'user' ? '个人资产概览' : (getUserRoleFromLocalStorage() === 'admin' ? '公司资产概览' : '部门资产概览') }}</span>
            </div>
          </template>
          <div class="stats-grid">
            <div class="stat-item">
              <div class="stat-value">{{ getUserRoleFromLocalStorage() === 'user' ? myAssetCount : totalAssetCount }}</div>
              <div class="stat-label">{{ getUserRoleFromLocalStorage() === 'user' ? '资产数量' : (getUserRoleFromLocalStorage() === 'admin' ? '公司资产总数' : '部门资产总数') }}</div>
            </div>
            <div v-if="getUserRoleFromLocalStorage() !== 'user'" class="stat-item">
              <div class="stat-value">{{ myApplicationCount }}</div>
              <div class="stat-label">待处理申请</div>
            </div>
            <div v-if="getUserRoleFromLocalStorage() === 'user'" class="stat-item">
              <div class="stat-value">{{ usingAssetCount }}</div>
              <div class="stat-label">使用中资产</div>
            </div>
            <div v-if="getUserRoleFromLocalStorage() === 'user'" class="stat-item">
              <div class="stat-value">{{ idleAssetCount }}</div>
              <div class="stat-label">闲置资产</div>
            </div>
            <div v-if="getUserRoleFromLocalStorage() === 'user'" class="stat-item">
              <div class="stat-value">{{ maintenanceAssetCount }}</div>
              <div class="stat-label">维修中资产</div>
            </div>
            <div v-if="getUserRoleFromLocalStorage() !== 'user'" class="stat-item">
              <div class="stat-value">{{ usingAssetCount }}</div>
              <div class="stat-label">使用中资产</div>
            </div>
            <div v-if="getUserRoleFromLocalStorage() !== 'user'" class="stat-item">
              <div class="stat-value">{{ idleAssetCount }}</div>
              <div class="stat-label">闲置资产</div>
            </div>
            <div v-if="getUserRoleFromLocalStorage() === 'admin'" class="stat-item">
              <div class="stat-value">{{ formatPrice(totalAssetValue) }}</div>
              <div class="stat-label">资产总价值</div>
            </div>
          </div>
        </el-card>

        <!-- 最近申请 (仅普通员工) -->
        <el-card v-if="getUserRoleFromLocalStorage() === 'user'" shadow="hover" style="margin-bottom: 20px;">
          <template #header>
            <div class="card-header">
              <el-icon><Clock /></el-icon>
              <span>最近申请</span>
              <el-button type="primary" plain size="small" @click="navigateTo('/my-applications')">
                <el-icon><ArrowRight /></el-icon>
                <span>查看全部</span>
              </el-button>
            </div>
          </template>
          <div v-if="recentApplications.length > 0" class="recent-applications">
            <el-timeline>
              <el-timeline-item
                v-for="(app, index) in recentApplications"
                :key="index"
                :timestamp="formatTime(app.applicationDate)"
                placement="top"
              >
                <el-card shadow="hover" class="application-card">
                  <div class="application-content">
                    <div class="application-title">
                      <span>{{ getApplicationTypeName(app.applicationType) }}</span>
                      <el-tag :type="getApplicationStatusType(app.status)">
                        <span>{{ getStatusIcon(app.status) }}</span> {{ getApplicationStatusName(app.status) }}
                      </el-tag>
                    </div>
                    <div class="application-details">
                      <div class="detail-item">
                        <el-icon><Box /></el-icon>
                        <span>{{ app.assetName || '未知资产' }}</span>
                        <span v-if="app.assetNo" class="asset-no">({{ app.assetNo }})</span>
                      </div>
                      <div v-if="app.approvalRemark" class="detail-item">
                        <el-icon><Message /></el-icon>
                        <span>审批意见: {{ app.approvalRemark }}</span>
                      </div>
                    </div>
                  </div>
                </el-card>
              </el-timeline-item>
            </el-timeline>
          </div>
          <el-empty v-else description="暂无申请记录" />
        </el-card>

        <!-- 图表区域 -->
        <el-row :gutter="20" style="margin-bottom: 20px;" v-if="checkChartPermission('status')">
          <el-col :span="12">
            <el-card class="chart-card" shadow="hover">
              <template #header>
                <div class="card-header">
                  <el-icon><PieChart /></el-icon>
                  <span>资产状态分布</span>
                </div>
              </template>
              <div class="chart-container">
                <div ref="statusChartRef" class="chart"></div>
              </div>
            </el-card>
          </el-col>
          
          <el-col :span="12">
            <el-card class="chart-card" shadow="hover">
              <template #header>
                <div class="card-header">
                  <el-icon><TrendCharts /></el-icon>
                  <span>资产价值分布</span>
                </div>
              </template>
              <div class="chart-container">
                <div ref="valueChartRef" class="chart"></div>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 部门资产统计和详细数据 - 左右布局 -->
        <el-row :gutter="20" v-if="checkChartPermission('department')" style="margin-bottom: 20px;">
          <el-col :span="12">
            <el-card class="department-card" shadow="hover" style="height: 100%;">
              <template #header>
                <div class="card-header">
                  <el-icon><OfficeBuilding /></el-icon>
                  <span>部门资产统计</span>
                  <el-button type="primary" plain size="small" @click="navigateTo('/departments')">
                    <el-icon><ArrowRight /></el-icon>
                    <span>查看详情</span>
                  </el-button>
                </div>
              </template>
              <div class="chart-container">
                <div ref="departmentChartRef" class="chart"></div>
              </div>
            </el-card>
          </el-col>
          
          <el-col :span="12">
            <el-card class="department-detail-card" shadow="hover" style="height: 100%;">
              <template #header>
                <div class="card-header">
                  <el-icon><Document /></el-icon>
                  <span>部门资产详细数据</span>
                  <el-button type="primary" plain size="small" @click="exportDepartmentStats">
                    <el-icon><Download /></el-icon>
                    <span>导出数据</span>
                  </el-button>
                </div>
              </template>
              <el-table :data="departmentStats" style="width: 100%" max-height="280">
                <el-table-column prop="departmentName" label="部门名称" min-width="100" />
                <el-table-column prop="assetCount" label="资产数量" width="80" />
                <el-table-column prop="totalValue" label="资产总价值" width="110">
                  <template #default="scope">
                    {{ formatPrice(scope.row.totalValue) }}
                  </template>
                </el-table-column>
                <el-table-column prop="averageValue" label="平均价值" width="100">
                  <template #default="scope">
                    {{ formatPrice(scope.row.averageValue) }}
                  </template>
                </el-table-column>
                <el-table-column prop="inUseCount" label="使用中" width="70" />
                <el-table-column prop="idleCount" label="闲置" width="70" />
                <el-table-column prop="maintenanceCount" label="维修中" width="70" />
                <el-table-column prop="scrappedCount" label="已报废" width="70" />
              </el-table>
            </el-card>
          </el-col>
        </el-row>
      </el-main>
    </el-container>
  </el-container>
</template>

<script>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from '../utils/request'
import * as echarts from 'echarts'
import { ElMessageBox } from 'element-plus'
import { getAccessibleMenus } from '../utils/permission'
import { checkChartPermission, getAccessLevel } from '../utils/chartPermission'
import { 
  DataBoard, 
  Clock, 
  Operation, 
  SwitchButton,
  Menu,
  OfficeBuilding,
  User,
  Setting,
  TrendCharts,
  Search,
  Document,
  PieChart,
  ArrowRight,
  Download,
  Box,
  Message
} from '@element-plus/icons-vue'

export default {
  name: 'Home',
  setup() {
    const router = useRouter()
    const currentMenu = ref('1')
    const isCollapse = ref(false)
    const statsData = ref({})
    const pendingTasks = ref([])
    const recentActivities = ref([])
    const myApplicationCount = ref(0)
    const myAssetCount = ref(0)
    const usingAssetCount = ref(0)
    const idleAssetCount = ref(0)
    const maintenanceAssetCount = ref(0)
    const recentApplications = ref([])
    
    // 搜索关键词
    const searchKeyword = ref('')
    
    const username = localStorage.getItem('username')
    const userDepartment = localStorage.getItem('department') || '公司'
    // 从本地存储获取用户角色
    const getUserRoleFromLocalStorage = () => {
      const userStr = localStorage.getItem('user')
      if (userStr) {
        try {
          const user = JSON.parse(userStr)
          if (user.roles && Array.isArray(user.roles)) {
            for (const role of user.roles) {
              let roleName = ''
              if (typeof role === 'string') {
                roleName = role
              } else if (role.name) {
                roleName = role.name
              }
              roleName = roleName.toLowerCase()
              if (roleName.startsWith('role_')) {
                roleName = roleName.substring(5)
              }
              if (roleName === 'admin') return 'admin'
              if (roleName === 'leader') return 'leader'
              if (roleName === 'manager') return 'manager'
            }
          } else if (user.role) {
            return user.role.toLowerCase()
          } else if (user.username) {
            const username = user.username.toLowerCase()
            if (username === 'admin') return 'admin'
            if (username.startsWith('leader')) return 'leader'
            if (username.startsWith('admin_')) return 'manager'
          }
        } catch (e) {
          console.error('解析用户信息失败:', e)
        }
      }
      return 'user'
    }
    
    const accessibleMenus = computed(() => {
      return getAccessibleMenus()
    })
    
    const totalAssetCount = ref(0)
    const totalAssetValue = ref(0)
    const departmentStats = ref([])
    const statusDistribution = ref([])
    
    const statusChartRef = ref(null)
    const valueChartRef = ref(null)
    const departmentChartRef = ref(null)
    const statusChart = ref(null)
    const valueChart = ref(null)
    const departmentChart = ref(null)
    
    // 获取用户显示名称（优先显示真实姓名，否则显示用户名）
    const getUserName = () => {
      const userStr = localStorage.getItem('user')
      
      if (userStr) {
        try {
          const user = JSON.parse(userStr)
          
          // 优先显示真实姓名
          if (user.realName && user.realName.trim()) {
            return user.realName.trim()
          }
          
          // 否则显示用户名
          if (user.username && user.username.trim()) {
            return user.username.trim()
          }
        } catch (e) {
          console.error('解析用户信息失败:', e)
        }
      }
      
      return '用户'
    }

    const getRoleName = () => {
      const userStr = localStorage.getItem('user')
      
      if (userStr) {
        try {
          const user = JSON.parse(userStr)
          
          // 角色名称映射
          const roleMap = {
            'ROLE_ADMIN': '系统管理员',
            'ROLE_MANAGER': '部门资产管理员',
            'ROLE_LEADER': '领导',
            'ROLE_USER': '普通员工',
            'admin': '系统管理员',
            'manager': '部门资产管理员',
            'leader': '领导',
            'user': '普通员工'
          }
          
          // 优先检查后端返回的角色
          if (user.roles && Array.isArray(user.roles) && user.roles.length > 0) {
            // 首先检查是否有系统管理员角色
            for (const role of user.roles) {
              if (typeof role === 'string') {
                if (role === 'ROLE_ADMIN' || role === 'admin') {
                  return '系统管理员'
                }
              } else if (role.name) {
                if (role.name === 'ROLE_ADMIN' || role.name === 'admin') {
                  return '系统管理员'
                }
              }
            }
            
            // 然后检查是否有部门资产管理员角色
            for (const role of user.roles) {
              if (typeof role === 'string') {
                if (role === 'ROLE_MANAGER' || role === 'manager') {
                  return '部门资产管理员'
                }
              } else if (role.name) {
                if (role.name === 'ROLE_MANAGER' || role.name === 'manager') {
                  return '部门资产管理员'
                }
              }
            }
            
            // 然后检查是否有领导角色
            for (const role of user.roles) {
              if (typeof role === 'string') {
                if (role === 'ROLE_LEADER' || role === 'leader') {
                  return '领导'
                }
              } else if (role.name) {
                if (role.name === 'ROLE_LEADER' || role.name === 'leader') {
                  return '领导'
                }
              }
            }
            
            // 最后检查是否有普通员工角色
            for (const role of user.roles) {
              if (typeof role === 'string') {
                if (role === 'ROLE_USER' || role === 'user') {
                  return '普通员工'
                }
              } else if (role.name) {
                if (role.name === 'ROLE_USER' || role.name === 'user') {
                  return '普通员工'
                }
              }
            }
          } else if (user.role) {
            return roleMap[user.role] || user.role || '用户'
          }
          
          // 如果没有角色信息，通过用户名推断
          if (user.username) {
            const usernameLower = user.username.toLowerCase()
            if (usernameLower === 'admin') {
              return '系统管理员'
            } else if (usernameLower.startsWith('leader')) {
              return '领导'
            } else if (usernameLower.startsWith('admin_')) {
              return '部门资产管理员'
            } else if (usernameLower.startsWith('user')) {
              return '普通员工'
            }
          }
        } catch (e) {
          console.error('解析用户信息失败:', e)
        }
      }
      return '用户'
    }
    
    const hasAdminPermission = () => {
      const userStr = localStorage.getItem('user')
      
      if (userStr) {
        try {
          const user = JSON.parse(userStr)
          
          // 检查用户是否有管理员或领导权限
          if (user.roles && Array.isArray(user.roles) && user.roles.length > 0) {
            for (const role of user.roles) {
              if (typeof role === 'string') {
                if (role === 'ROLE_ADMIN' || role === 'admin' || role === 'ROLE_LEADER' || role === 'leader') {
                  return true
                }
              } else if (role.name) {
                if (role.name === 'ROLE_ADMIN' || role.name === 'admin' || role.name === 'ROLE_LEADER' || role.name === 'leader') {
                  return true
                }
              }
            }
          }
          
          // 如果没有角色信息，通过用户名推断
          if (user.username) {
            const usernameLower = user.username.toLowerCase()
            if (usernameLower === 'admin' || usernameLower.startsWith('leader')) {
              return true
            }
          }
        } catch (e) {
          console.error('解析用户信息失败:', e)
        }
      }
      return false
    }
    
    const getMenuIcon = (menuId) => {
      const iconMap = {
        1: DataBoard,
        2: OfficeBuilding,
        3: User,
        4: Menu,
        5: Setting,
        31: OfficeBuilding, // 部门管理
        32: Document, // 资产分类管理
        33: User, // 员工信息管理
        39: User // 供应商管理
      }
      return iconMap[menuId] || Menu
    }
    
    const getMenuTitle = (menu) => {
      if (menu.titleByRole) {
        const userRole = getUserRoleFromLocalStorage()
        if (menu.titleByRole[userRole]) {
          return menu.titleByRole[userRole]
        }
      }
      return menu.title
    }
    
    const findMenuById = (menus, id) => {
      for (const menu of menus) {
        if (String(menu.id) === id) {
          return menu
        }
        if (menu.children) {
          const found = findMenuById(menu.children, id)
          if (found) {
            return found
          }
        }
      }
      return null
    }

    const handleMenuSelect = (index) => {
      currentMenu.value = index
      const menu = findMenuById(accessibleMenus.value, index)
      if (menu && menu.path) {
        navigateTo(menu.path)
      }
    }
    
    const navigateTo = (path) => {
      router.push(path)
    }
    
    const logout = () => {
      ElMessageBox.confirm('确定要退出登录吗？', '退出登录', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        localStorage.removeItem('username')
        localStorage.removeItem('role')
        router.push('/')
      }).catch(() => {
        // 取消退出登录
      })
    }
    
    const exportDepartmentStats = () => {
      // 这里可以添加导出部门资产统计数据的逻辑
      // 例如调用后端API导出Excel文件
    }
    
    const handleSearch = () => {
      // 导航到资产列表页面并传递搜索参数
      router.push({
        path: '/assets',
        query: {
          keyword: searchKeyword.value
        }
      })
    }
    
    const goToMyApplications = () => {
      router.push({ path: '/assets/apply', query: { onlyRecords: 'true' } })
    }
    
    const goToMyAssets = () => {
      router.push({ path: '/assets/list', query: { onlyMyAssets: 'true' } })
    }
    
    const loadMyApplicationCount = async () => {
      try {
        const userStr = localStorage.getItem('user')
        if (userStr) {
          const user = JSON.parse(userStr)
          const userRole = getUserRoleFromLocalStorage()
          
          let totalCount = 0
          
          if (userRole === 'leader') {
            // 领导：查看需要自己审批的申请
            const departmentId = user.departmentId || user.deptId
            
            // 获取待审批的资产申请（终审）
            if (departmentId) {
              const assetApprovalResponse = await axios.get(`/asset-applications/status/pending_leader/department/${departmentId}`)
              if (assetApprovalResponse.data.code === 200) {
                totalCount += assetApprovalResponse.data.data.length
              }
            }
            // 获取待审批的采购申请
            const purchaseApprovalResponse = await axios.get(`/purchase-requests/pending/approver/${user.id}`)
            if (purchaseApprovalResponse.data.code === 200) {
              totalCount += purchaseApprovalResponse.data.data.length
            }
          } else if (userRole === 'admin') {
            // 管理员：查看所有待审批申请
            const purchaseResponse = await axios.get('/purchase-requests/status/pending')
            if (purchaseResponse.data.code === 200) {
              totalCount += purchaseResponse.data.data.length
            }
            
            // 统计待审批的资产申请（等待资产管理员审批）
            const assetPendingResponse = await axios.get('/asset-applications/status/pending')
            if (assetPendingResponse.data.code === 200) {
              totalCount += assetPendingResponse.data.data.length
            }
            
            // 统计待终审的资产申请（等待领导审批，如报废申请）
            const assetLeaderResponse = await axios.get('/asset-applications/status/pending_leader')
            if (assetLeaderResponse.data.code === 200) {
              totalCount += assetLeaderResponse.data.data.length
            }
          } else if (userRole === 'manager') {
            // 部门资产管理员：查看需要自己审批的申请（本部门员工的资产申请，排除自己提交的）
            const departmentId = user.departmentId || user.deptId
            if (departmentId) {
              const assetResponse = await axios.get(`/asset-applications/status/pending/department/${departmentId}`)
              if (assetResponse.data.code === 200) {
                // 过滤掉自己提交的申请
                const filteredData = assetResponse.data.data.filter(item => item.applicantId !== user.id)
                totalCount += filteredData.length
              }
            }
          } else {
            // 普通员工：查看自己发起的申请
            const assetResponse = await axios.get(`/asset-applications/applicant/${user.id}`)
            if (assetResponse.data.code === 200) {
              totalCount += assetResponse.data.data.length
            }
            const purchaseResponse = await axios.get(`/purchase-requests/applicant/${user.id}`)
            if (purchaseResponse.data.code === 200) {
              totalCount += purchaseResponse.data.data.length
            }
          }
          
          myApplicationCount.value = totalCount
        }
      } catch (error) {
        console.error('获取我的申请数量失败', error)
      }
    }
    
    const loadMyAssetCount = async () => {
      // 只有普通员工才需要加载我的资产数量
      if (!hasAdminPermission()) {
        try {
          const userStr = localStorage.getItem('user')
          if (userStr) {
            const user = JSON.parse(userStr)
            const token = localStorage.getItem('token')
            
            // 加载用户资产数量
            const response = await axios.get('/assets')
            
            if (response.data.code === 200) {
              // 过滤出当前用户的资产，包括使用中的、闲置的和维修中的
              const userAssets = response.data.data.filter(asset => 
                asset.userId === user.id && 
                (asset.status === 'using' || asset.status === 'idle' || asset.status === 'maintenance')
              )
              myAssetCount.value = userAssets.length
              
              // 计算不同状态的资产数量
              usingAssetCount.value = userAssets.filter(asset => asset.status === 'using').length
              idleAssetCount.value = userAssets.filter(asset => asset.status === 'idle').length
              maintenanceAssetCount.value = userAssets.filter(asset => asset.status === 'maintenance').length
            }
          }
        } catch (error) {
          console.error('获取我的资产数量失败', error)
        }
      }
    }
    
    const loadRecentApplications = async () => {
      // 只有普通员工才需要加载最近申请
      if (!hasAdminPermission()) {
        try {
          const userStr = localStorage.getItem('user')
          if (userStr) {
            const user = JSON.parse(userStr)
            
            // 加载用户最近的申请记录
            const response = await axios.get(`/asset-applications/applicant/${user.id}`)
            
            if (response.data.code === 200) {
              // 处理分页对象或数组
              const data = response.data.data
              const applications = data.content || data
              if (Array.isArray(applications)) {
                // 获取最近5条申请记录
                recentApplications.value = applications.slice(0, 5)
              } else {
                recentApplications.value = []
              }
            }
          }
        } catch (error) {
          console.error('获取最近申请失败', error)
        }
      }
    }
    
    const loadDashboardStats = async () => {
      try {
        // 所有角色都调用同一个接口
        // 后端会根据用户角色和部门ID返回相应的数据
        const response = await axios.get('/dashboard/stats')
        
        if (response.data.code === 200) {
          statsData.value = response.data.data
        } else {
          console.error('获取统计数据失败:', response.data.message)
        }
      } catch (error) {
        console.error('获取统计数据错误:', error)
      }
    }
    
    const loadRecentOperations = async () => {
      try {
        const response = await axios.get('/dashboard/operations', {
          params: {
            limit: 10
          }
        })
        
        if (response.data.code === 200) {
          pendingTasks.value = response.data.data.pendingTasks || []
          recentActivities.value = response.data.data.recentActivities || []
        } else {
          console.error('获取操作数据失败:', response.data.message)
        }
      } catch (error) {
        console.error('获取操作数据错误:', error)
        // 如果新版API失败，降级到旧版API
        try {
          const oldResponse = await axios.get('/dashboard/recent-operations', {
            params: {
              limit: 10
            }
          })
          if (oldResponse.data.code === 200) {
            recentActivities.value = oldResponse.data.data
          }
        } catch (e) {
          console.error('获取最近操作错误:', e)
        }
      }
    }
    
    const getTypeLabel = (type) => {
      const labels = {
        'purchase': '采购',
        'application': '资产',
        'inventory': '盘点',
        'asset': '资产',
        'system': '系统'
      }
      return labels[type] || type
    }
    
    const getActivityTagType = (type) => {
      const types = {
        'purchase': 'primary',
        'application': 'success',
        'inventory': 'info',
        'asset': 'success',
        'system': 'default'
      }
      return types[type] || 'default'
    }
    
    const formatTime = (timeStr) => {
      if (!timeStr) return ''
      const date = new Date(timeStr)
      return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      })
    }
    
    const formatPrice = (price) => {
      if (price === null || price === undefined) return '-'
      return '¥' + parseFloat(price).toFixed(2)
    }
    
    const getApplicationTypeName = (type) => {
      const typeMap = {
        '领用': '资产领用申请',
        'RECEIVE': '资产领用申请',
        'transfer': '资产转移申请',
        'TRANSFER': '资产转移申请',
        'maintenance': '资产维修申请',
        'MAINTENANCE': '资产维修申请',
        'REPAIR': '资产维修申请',
        'scrap': '资产报废申请',
        'DISPOSAL': '资产报废申请'
      }
      return typeMap[type] || type
    }
    
    const getStatusIcon = (status) => {
      const statusLower = status ? status.toLowerCase() : ''
      switch (statusLower) {
        case 'approved':
          return '✓'
        case 'rejected':
          return '✗'
        case 'pending':
          return '○'
        case 'completed':
          return '✓'
        default:
          return '○'
      }
    }
    
    const getApplicationStatusName = (status) => {
      if (!status) return '-'
      const statusLower = status.toLowerCase()
      const statusMap = {
        'pending': '待审批',
        'approved': '已批准',
        'rejected': '已拒绝',
        'completed': '已完成',
        'leader_approved': '领导已批准',
        'final_approval_created': '已创建终审'
      }
      return statusMap[statusLower] || status
    }
    
    const getApplicationStatusType = (status) => {
      if (!status) return 'info'
      const statusLower = status.toLowerCase()
      const typeMap = {
        'pending': 'warning',
        'approved': 'success',
        'rejected': 'danger',
        'completed': 'info',
        'leader_approved': 'success',
        'final_approval_created': 'primary'
      }
      return typeMap[statusLower] || 'info'
    }
    
    const getToken = () => localStorage.getItem('token')
    
    const fetchDepartmentStats = async () => {
      try {
        const userStr = localStorage.getItem('user')
        const user = userStr ? JSON.parse(userStr) : {}
        const userRole = getUserRole(user)
        
        let response
        if (userRole === 'admin' || userRole === 'leader' || userRole === 'manager') {
          // 系统管理员、领导和部门资产管理员都调用同一个接口
          // 后端会根据用户角色和部门ID返回相应的数据
          response = await axios.get('/reports/department-stats')
        } else {
          // 普通员工只能查看自己的资产数据
          departmentStats.value = []
          totalAssetCount.value = 0
          totalAssetValue.value = 0
          usingAssetCount.value = 0
          idleAssetCount.value = 0
          return
        }
        
        if (response.data.code === 200) {
          departmentStats.value = response.data.data || []
          
          // 计算总资产数量和价值
          totalAssetCount.value = departmentStats.value.reduce((sum, item) => sum + (item.assetCount || 0), 0)
          totalAssetValue.value = departmentStats.value.reduce((sum, item) => sum + (item.totalValue || 0), 0)
          usingAssetCount.value = departmentStats.value.reduce((sum, item) => sum + (item.inUseCount || 0), 0)
          idleAssetCount.value = departmentStats.value.reduce((sum, item) => sum + (item.idleCount || 0), 0)
          
          renderDepartmentChart()
        } else {
          console.error('获取部门资产统计失败:', response.data.message)
        }
      } catch (error) {
        console.error('获取部门资产统计失败', error)
      }
    }
    
    // 获取用户角色
    const getUserRole = (user) => {
      if (!user) return 'user';

      // 直接检查用户名是否为admin
      if (user.username && user.username.toLowerCase() === 'admin') {
        return 'admin';
      }

      // 优先使用后端返回的角色
      if (user.roles && user.roles.length > 0) {
        // 检查是否有admin角色
        for (const role of user.roles) {
          let roleName = '';
          try {
            roleName = typeof role === 'string' ? role : role.name || role.code;
            roleName = roleName.toLowerCase();
            if (roleName.startsWith('role_')) {
              roleName = roleName.substring(5); // 移除ROLE_前缀
            }
            if (roleName === 'admin') {
              return 'admin';
            }
          } catch (e) {
            console.error('处理角色时出错:', e);
          }
        }

        // 检查是否有leader角色
        for (const role of user.roles) {
          let roleName = '';
          try {
            roleName = typeof role === 'string' ? role : role.name || role.code;
            roleName = roleName.toLowerCase();
            if (roleName.startsWith('role_')) {
              roleName = roleName.substring(5); // 移除ROLE_前缀
            }
            if (roleName === 'leader') {
              return 'leader';
            }
          } catch (e) {
            console.error('处理角色时出错:', e);
          }
        }

        // 检查是否有manager角色
        for (const role of user.roles) {
          let roleName = '';
          try {
            roleName = typeof role === 'string' ? role : role.name || role.code;
            roleName = roleName.toLowerCase();
            if (roleName.startsWith('role_')) {
              roleName = roleName.substring(5); // 移除ROLE_前缀
            }
            if (roleName === 'manager') {
              return 'manager';
            }
          } catch (e) {
            console.error('处理角色时出错:', e);
          }
        }
      }

      // 如果没有角色信息，通过用户名推断
      if (user.username) {
        const usernameLower = user.username.toLowerCase();
        if (usernameLower === 'admin') {
          return 'admin';
        } else if (usernameLower.startsWith('leader')) {
          return 'leader';
        } else if (usernameLower.startsWith('admin_')) {
          return 'manager';
        } else if (usernameLower.startsWith('user')) {
          return 'user';
        }
      }

      return 'user';
    }

    const fetchStatusDistribution = async () => {
      try {
        const response = await axios.get('/reports/status-distribution')
        if (response.data.code === 200) {
          statusDistribution.value = response.data.data
          renderStatusCharts()
        }
      } catch (error) {
        console.error('获取资产状态分布失败', error)
      }
    }

    const renderDepartmentChart = () => {
      // 检查DOM元素是否存在
      if (!departmentChartRef.value) {
        return
      }
      
      // 检查数据是否存在
      if (!departmentStats.value || departmentStats.value.length === 0) {
        return
      }
      
      // 延迟渲染，确保DOM元素已经完全渲染
      setTimeout(() => {
        try {
          if (departmentChart.value) {
            departmentChart.value.dispose()
          }
          
          // 再次检查DOM元素
          if (!departmentChartRef.value) {
            return
          }
          
          departmentChart.value = echarts.init(departmentChartRef.value)
      
      // 获取部门数据
      const departments = departmentStats.value.map(item => item.departmentName)
      const assetCounts = departmentStats.value.map(item => item.assetCount)
      const totalValues = departmentStats.value.map(item => item.totalValue / 1000) // 转换为千单位
      
      // 根据数据量动态调整柱子宽度
      const getBarWidth = () => {
        if (departments.length === 1) {
          return 80; // 单个部门时，使用固定宽度80px
        } else if (departments.length <= 3) {
          return '40%'; // 少量部门时，使用较小百分比
        } else {
          return '50%'; // 正常情况
        }
      }
      
      const option = {
          tooltip: {
            trigger: 'axis',
            axisPointer: {
              type: 'cross',
              crossStyle: {
                color: '#999'
              }
            },
            formatter: function(params) {
              let result = params[0].name + '<br/>';
              for (let i = 0; i < params.length; i++) {
                result += params[i].marker + params[i].seriesName + ': ' + 
                  (params[i].seriesIndex === 0 ? params[i].value + ' 件' : '¥' + (params[i].value * 1000).toFixed(2)) + '<br/>';
              }
              return result;
            }
          },
          legend: {
            data: ['资产数量', '资产价值'],
            bottom: 0
          },
          grid: {
            left: departments.length === 1 ? '25%' : '3%',
            right: departments.length === 1 ? '25%' : '4%',
            bottom: '15%',
            containLabel: true
          },
          xAxis: {
            type: 'category',
            data: departments,
            axisLabel: {
              rotate: departments.length > 4 ? 45 : 0
            }
          },
          yAxis: [
            {
              type: 'value',
              name: '资产数量',
              min: 0,
              axisLabel: {
                formatter: '{value} 件'
              }
            },
            {
              type: 'value',
              name: '资产价值',
              min: 0,
              axisLabel: {
                formatter: '¥{value}K'
              }
            }
          ],
          series: [
            {
              name: '资产数量',
              type: 'bar',
              data: assetCounts,
              barWidth: getBarWidth(),
              itemStyle: {
                color: {
                  type: 'linear',
                  x: 0,
                  y: 0,
                  x2: 0,
                  y2: 1,
                  colorStops: [{
                    offset: 0, color: '#409EFF' // 开始颜色
                  }, {
                    offset: 1, color: '#69c0ff' // 结束颜色
                  }]
                },
                borderRadius: [4, 4, 0, 0] // 添加圆角
              },
              emphasis: {
                itemStyle: {
                  color: {
                    type: 'linear',
                    x: 0,
                    y: 0,
                    x2: 0,
                    y2: 1,
                    colorStops: [{
                      offset: 0, color: '#66b1ff' // 开始颜色
                    }, {
                      offset: 1, color: '#91d5ff' // 结束颜色
                    }]
                  }
                }
              }
            },
            {
              name: '资产价值',
              type: 'line',
              yAxisIndex: 1,
              data: totalValues,
              itemStyle: {
                color: '#67C23A'
              },
              lineStyle: {
                width: 3
              },
              symbol: 'circle',
              symbolSize: 8,
              emphasis: {
                itemStyle: {
                  color: '#85ce61',
                  borderColor: '#ffffff',
                  borderWidth: 2
                }
              }
            }
          ]
        }
      
          departmentChart.value.setOption(option)
        } catch (error) {
          console.error('渲染部门资产统计图表失败:', error)
        }
      }, 100)
    }

    const renderStatusCharts = () => {
      // 检查数据是否存在
      if (!statusDistribution.value || statusDistribution.value.length === 0) {
        return
      }
      
      // 状态数量分布
      if (statusChartRef.value) {
        // 添加延迟，确保DOM元素已经完全渲染
        setTimeout(() => {
          try {
            if (statusChart.value) {
              statusChart.value.dispose()
            }
            
            // 再次检查DOM元素
            if (!statusChartRef.value) {
              return
            }
            
            statusChart.value = echarts.init(statusChartRef.value)
            
            const statusNames = statusDistribution.value.map(item => item.statusName)
            const counts = statusDistribution.value.map(item => item.count)
            
            // 默认使用饼图
            const option = {
              tooltip: {
                trigger: 'item'
              },
              legend: {
                orient: 'vertical',
                left: 'left'
              },
              series: [
                {
                  name: '资产状态',
                  type: 'pie',
                  radius: '60%',
                  data: statusDistribution.value.map(item => ({
                    name: item.statusName,
                    value: item.count
                  })),
                  emphasis: {
                    itemStyle: {
                      shadowBlur: 10,
                      shadowOffsetX: 0,
                      shadowColor: 'rgba(0, 0, 0, 0.5)'
                    }
                  }
                }
              ]
            }
            
            statusChart.value.setOption(option)
          } catch (error) {
            console.error('渲染状态数量分布图表失败:', error)
          }
        }, 100)
      }
      
      // 状态价值分布
      if (valueChartRef.value) {
        // 添加延迟，确保DOM元素已经完全渲染
        setTimeout(() => {
          try {
            if (valueChart.value) {
              valueChart.value.dispose()
            }
            
            // 再次检查DOM元素
            if (!valueChartRef.value) {
              return
            }
            
            valueChart.value = echarts.init(valueChartRef.value)
            
            const statusNames = statusDistribution.value.map(item => item.statusName)
            const values = statusDistribution.value.map(item => item.totalValue)
            
            // 确保数据有效
            if (!statusNames || statusNames.length === 0 || !values || values.length === 0) {
              return
            }
            
            // 使用完整的ECharts配置，确保坐标系正确创建
            const option = {
              tooltip: {
                trigger: 'axis',
                axisPointer: {
                  type: 'shadow'
                }
              },
              grid: {
                left: '3%',
                right: '4%',
                bottom: '3%',
                containLabel: true
              },
              xAxis: {
                type: 'category',
                data: statusNames,
                axisLabel: {
                  rotate: 45
                }
              },
              yAxis: {
                type: 'log',
                min: 1,
                axisLabel: {
                  formatter: function(value) {
                    if (value >= 1000000) {
                      return (value / 1000000).toFixed(1) + 'M';
                    } else if (value >= 1000) {
                      return (value / 1000).toFixed(1) + 'K';
                    }
                    return value;
                  }
                }
              },
              series: [
                {
                  name: '资产价值',
                  type: 'bar',
                  data: values,
                  showBackground: true,
                  backgroundStyle: {
                    color: 'rgba(180, 180, 180, 0.2)'
                  },
                  itemStyle: {
                    color: '#67C23A'
                  }
                }
              ]
            }
            
            valueChart.value.setOption(option)
          } catch (error) {
            console.error('渲染状态价值分布图表失败:', error)
          }
        }, 100)
      }
    }

    const handleResize = () => {
      try {
        // 检查图表实例是否存在且未被销毁
        if (departmentChart.value && typeof departmentChart.value.resize === 'function' && !departmentChart.value.isDisposed()) {
          departmentChart.value.resize()
        }
        if (statusChart.value && typeof statusChart.value.resize === 'function' && !statusChart.value.isDisposed()) {
          statusChart.value.resize()
        }
        if (valueChart.value && typeof valueChart.value.resize === 'function' && !valueChart.value.isDisposed()) {
          valueChart.value.resize()
        }
      } catch (error) {
        // 忽略 resize 错误
      }
    }
    
    onMounted(() => {
      loadDashboardStats()
      loadRecentOperations()
      loadMyApplicationCount()
      loadMyAssetCount()
      loadRecentApplications()
      
      // 只有当用户有权限时才调用相关API
      if (checkChartPermission('department')) {
        fetchDepartmentStats()
      }
      
      if (checkChartPermission('status')) {
        fetchStatusDistribution()
      }
      
      window.addEventListener('resize', handleResize)
    })
    
    onUnmounted(() => {
      window.removeEventListener('resize', handleResize)
      departmentChart.value?.dispose()
      statusChart.value?.dispose()
      valueChart.value?.dispose()
    })
    
    return {
      currentMenu,
      isCollapse,
      accessibleMenus,
      statsData,
      pendingTasks,
      recentActivities,
      myApplicationCount,
      myAssetCount,
      usingAssetCount,
      idleAssetCount,
      maintenanceAssetCount,
      recentApplications,
      userDepartment,
      totalAssetCount,
      totalAssetValue,
      departmentStats,
      statusDistribution,
      statusChartRef,
      valueChartRef,
      departmentChartRef,
      searchKeyword,
      getRoleName,
      getUserName,
      hasAdminPermission,
      getMenuIcon,
      getMenuTitle,
      handleMenuSelect,
      navigateTo,
      goToMyApplications,
      goToMyAssets,
      logout,
      exportDepartmentStats,
      handleSearch,
      formatTime,
      formatPrice,
      getApplicationTypeName,
      getApplicationStatusName,
      getApplicationStatusType,
      getStatusIcon,
      checkChartPermission,
      getUserRoleFromLocalStorage,
      getTypeLabel,
      getActivityTagType,
      DataBoard,
      Clock,
      Operation,
      SwitchButton,
      PieChart,
      TrendCharts,
      OfficeBuilding,
      Document,
      ArrowRight,
      Download,
      Box,
      Message
    }
  }
}
</script>

<style scoped>
.home-container {
  height: 100vh;
  background-color: #f5f7fa;
}

.header {
  background-color: #409EFF;
  color: white;
  padding: 0;
  display: flex;
  align-items: center;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.header-content {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 2rem;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.sidebar-toggle {
  color: white;
  border-color: white;
}

.sidebar-toggle:hover {
  background-color: rgba(255, 255, 255, 0.1);
  border-color: white;
}

.header h1 {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 600;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.content {
  height: calc(100vh - 60px);
}

.sidebar {
  background-color: white;
  border-right: 1px solid #e4e7ed;
  transition: width 0.3s ease;
}

.sidebar-collapsed {
  overflow: hidden;
}

.el-menu-vertical {
  border-right: none;
}

.main-content {
  padding: 0 20px 20px 20px !important;
  margin: 0 !important;
  overflow-y: auto;
}

.overview-card,
.recent-operations-card,
.quick-actions-card,
.chart-card,
.department-card,
.department-detail-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #303133;
  justify-content: space-between;
}

.date-picker {
  margin-left: auto;
}

.change-tag {
  margin-bottom: 8px;
}

.quick-action-btn {
  width: 100%;
  justify-content: center;
  gap: 8px;
}

.search-filter-container {
  position: sticky;
  top: 0;
  z-index: 100;
  background-color: #f5f7fa;
  padding: 0 0 10px 0;
  border-bottom: 1px solid #e4e7ed;
  margin: 0 0 20px 0;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}

.search-filter-section {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  max-width: 100%;
}

.status-filter {
  width: 180px;
}

.search-input {
  flex: 1;
  max-width: none;
  width: 100%;
}

.quick-search-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.chart-container {
  height: 350px;
  width: 100%;
}

/* 响应式设计 */
@media screen and (max-width: 1200px) {
  .main-content {
    padding: 16px;
  }
  
  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  
  .date-picker {
    margin-left: 0;
    width: 100%;
  }
  
  .search-filter-container {
    width: 100%;
    margin-top: 8px;
  }
  
  .status-filter {
    flex: 1;
    min-width: 100px;
  }
  
  .search-filter-section {
    max-width: 100%;
  }
  
  .status-filter {
    width: 150px;
  }
  
  .search-input {
    max-width: 250px;
  }
  
  .chart-container {
    height: 300px;
  }
}

@media screen and (max-width: 768px) {
  .home-container {
    flex-direction: column;
  }
  
  .header {
    padding: 0 16px;
  }
  
  .header-content {
    flex-direction: column;
    gap: 12px;
    padding: 12px 0;
  }
  
  .header-left {
    justify-content: center;
  }
  
  .user-info {
    width: 100%;
    justify-content: space-between;
  }
  
  .sidebar {
    width: 100% !important;
    max-height: 200px;
    overflow-y: auto;
  }
  
  .main-content {
    padding: 12px;
  }
  
  .search-filter-section {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  
  .status-filter {
    width: 100%;
  }
  
  .search-input {
    width: 100%;
    max-width: unset;
  }
  
  .overview-card .el-col {
    flex: 1;
    min-width: 150px;
  }
  
  .chart-card {
    margin-bottom: 16px;
  }
  
  .chart-container {
    height: 250px;
  }
  
  .department-detail-card .el-table {
    font-size: 12px;
  }
  
  .el-table-column {
    min-width: 80px !important;
  }
}

/* 待处理任务卡片样式 */
.pending-tasks-card .el-card__header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 15px;
  margin-bottom: 0;
  background-color: #fff7e6;
}

.pending-tasks-card .el-card__body {
  background-color: #fffaf0;
}

.task-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  background-color: #f59e0b;
  color: white;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 600;
}

.task-content {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  flex-wrap: wrap;
}

.task-type {
  flex-shrink: 0;
}

/* 最近动态卡片样式 */
.recent-activities-card .el-card__header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 15px;
  margin-bottom: 0;
}

.recent-activities-card .el-card__header .el-icon {
  font-size: 16px;
}

.recent-activities-card .el-card__header span {
  font-size: 16px;
  font-weight: 500;
}

.activity-content {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  flex-wrap: wrap;
}

.activity-type {
  flex-shrink: 0;
}

.el-timeline {
  padding-left: 0;
}

.el-timeline-item {
  padding-bottom: 16px;
}

.el-timeline-item__tail {
  display: none;
}

.el-timeline-item__node {
  display: none;
}

.el-timeline-item__content {
  margin-left: 0;
  padding: 0;
}

.chart {
  width: 100%;
  height: 100%;
}

/* 快捷操作 */
.quick-actions {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.quick-action-card {
  flex: 1;
  min-width: 180px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.quick-action-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.quick-action-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
}

.action-icon {
  font-size: 24px;
  color: #409EFF;
}

/* 个人资产概览 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 20px;
  margin-top: 16px;
}

.stat-item {
  text-align: center;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #409EFF;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #606266;
}

:deep(.el-statistic__head) {
  font-size: 14px;
  color: #606266;
}

:deep(.el-statistic__content) {
  font-size: 28px;
  font-weight: bold;
  color: #409EFF;
}

:deep(.el-timeline-item__timestamp) {
  color: #909399;
  font-size: 12px;
}

@media (max-width: 768px) {
  .header-content {
    padding: 0 1rem;
  }
  
  .header h1 {
    font-size: 1.2rem;
  }
  
  .main-content {
    padding: 10px;
  }
  
  .chart-container {
    height: 250px;
  }
  
  .application-card {
    width: 100%;
    margin-bottom: 10px;
  }
  
  .application-content {
    padding: 10px;
  }
  
  .application-title {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
  }
  
  .application-details {
    font-size: 14px;
    color: #606266;
    line-height: 1.5;
  }
  
  .application-details span {
    display: block;
    margin-bottom: 4px;
  }
  
  .recent-applications {
    max-height: 400px;
    overflow-y: auto;
  }
}
</style>
