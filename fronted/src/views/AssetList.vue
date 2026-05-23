<template>
  <div class="asset-list-container">
    <!-- 导航栏 -->
    <div class="card-header">
      <el-button type="info" @click="goBack" :icon="ArrowLeft">返回首页</el-button>
      <span class="title">{{ pageTitle }}</span>
      <el-button v-if="hasAdminPermission()" type="primary" @click="handleAdd" :icon="Plus">新增资产</el-button>
    </div>

    <!-- 搜索栏 -->
    <div class="search-form">
      <el-form :model="searchForm" inline>
        <el-form-item label="资产名称">
          <el-input v-model="searchForm.assetName" placeholder="请输入资产名称" clearable />
        </el-form-item>
        <el-form-item label="资产状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable style="width: 150px">
            <el-option label="在库" value="in_stock" />
            <el-option label="使用中" value="using" />
            <el-option label="维修中" value="maintenance" />
            <el-option label="已报废" value="scrapped" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch" :icon="Search">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 资产列表 -->
    <div class="table-container">
      <el-table :data="filteredAssetList" v-loading="loading" stripe>
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="assetNo" label="资产编号" min-width="140" />
        <el-table-column prop="assetName" label="资产名称" min-width="150" />
        <el-table-column prop="categoryName" label="资产分类" min-width="120" />
        <el-table-column prop="model" label="型号" min-width="120" />
        <el-table-column prop="userName" label="使用人" min-width="100" />
        <el-table-column label="存放位置" min-width="180">
          <template #default="scope">
            <div class="location-cell">
              <!-- 原存放位置 -->
              <span>{{ scope.row.location || '-' }}</span>
              
              <!-- 已借出标识 -->
              <el-tag 
                v-if="scope.row.borrowStatus === 'borrowed'" 
                type="warning" 
                size="small"
                class="borrow-tag"
              >
                已借出
              </el-tag>
              
              <!-- 借出详情tooltip -->
              <el-tooltip 
                v-if="scope.row.borrowStatus === 'borrowed'"
                placement="top"
                effect="light"
              >
                <template #content>
                  <div class="borrow-tooltip">
                    <div><strong>当前位置：</strong>{{ scope.row.currentLocation || '未知' }}</div>
                    <div><strong>借用人：</strong>{{ scope.row.borrowerName || '未知' }}</div>
                    <div><strong>借出时间：</strong>{{ formatTime(scope.row.borrowTime) }}</div>
                    <div v-if="scope.row.expectedReturnTime">
                      <strong>预计归还：</strong>{{ formatTime(scope.row.expectedReturnTime) }}
                    </div>
                  </div>
                </template>
                <el-icon class="info-icon"><Info-Filled /></el-icon>
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="资产状态" min-width="150">
          <template #default="scope">
            <div class="status-with-action">
              <el-tag :type="getStatusType(scope.row.status)" style="margin-right: 8px;">
                {{ getStatusText(scope.row.status) }}
              </el-tag>
              <template v-if="!hasManagerPermission()">
                <el-button 
                  size="small" 
                  type="success" 
                  :icon="Plus" 
                  @click="handleApplyAsset(scope.row)"
                  :disabled="scope.row.status === 'using'"
                  style="padding: 4px 8px; font-size: 12px;"
                >
                  申请
                </el-button>
                <!-- 资产使用者或借用者的特定操作按钮 -->
                <template v-if="isAssetUserOrBorrower(scope.row)">
                  <!-- 维修完成按钮：仅在资产状态为维修中时显示 -->
                  <el-button 
                    v-if="scope.row.status === 'maintenance'" 
                    size="small" 
                    type="success" 
                    @click="handleRepairComplete(scope.row)"
                    style="padding: 4px 8px; font-size: 12px; margin-left: 4px;"
                  >
                    维修完成
                  </el-button>
                  <!-- 已批准的维修申请：显示"开始维修"按钮 -->
                  <el-button 
                    v-else-if="hasApprovedMaintenance(scope.row.id)" 
                    size="small" 
                    type="primary" 
                    @click="handleStartMaintenance(scope.row)"
                    style="padding: 4px 8px; font-size: 12px; margin-left: 4px;"
                  >
                    开始维修
                  </el-button>
                  <!-- 申请维修按钮：仅在资产状态为使用中或已借出时显示，且没有未完成的申请 -->
                  <el-button 
                    v-else-if="(scope.row.status === 'using' || scope.row.borrowStatus === 'borrowed') && !hasPendingMaintenance(scope.row.id) && !hasApprovedMaintenance(scope.row.id)" 
                    size="small" 
                    type="warning" 
                    @click="handleApplyMaintenance(scope.row)"
                    style="padding: 4px 8px; font-size: 12px; margin-left: 4px;"
                  >
                    申请维修
                  </el-button>
                  <!-- 已有待审批申请时显示提示 -->
                  <el-tag 
                    v-else-if="hasPendingMaintenance(scope.row.id)" 
                    type="info" 
                    size="small"
                    style="margin-left: 4px;"
                  >
                    申请已提交
                  </el-tag>
                </template>
              </template>            </div>
            </template>
          </el-table-column>
          <el-table-column v-if="hasManagerPermission()" prop="purchasePrice" label="采购价格" width="120">
            <template #default="scope">
              {{ formatPrice(scope.row.purchasePrice) }}
            </template>
          </el-table-column>
          <el-table-column v-if="hasManagerPermission()" prop="purchaseDate" label="采购日期" width="120" />
          <el-table-column v-if="hasManagerPermission()" label="供应商" min-width="120">
            <template #default="scope">
              {{ getSupplierName(scope.row.supplierId) }}
            </template>
          </el-table-column>
          <el-table-column v-if="hasManagerPermission()" prop="createTime" label="创建时间" width="180">
            <template #default="scope">
              {{ formatTime(scope.row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column v-if="hasManagerPermission()" label="操作" width="280" fixed="right">
            <template #default="scope">
              <el-button type="primary" size="small" @click="handleEdit(scope.row)" :icon="Edit">编辑</el-button>
              <el-button type="warning" size="small" @click="handleUpdateStatus(scope.row)" :icon="Refresh">状态</el-button>
              <el-button type="danger" size="small" @click="handleDelete(scope.row)" :icon="Delete">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="700px"
      :close-on-click-modal="false"
      class="asset-dialog"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <!-- 基本信息 -->
        <el-divider content-position="left">基本信息</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="资产编号" prop="assetNo" required>
              <el-input v-model="form.assetNo" placeholder="系统自动生成" :disabled="!isEdit" />
              <el-button type="primary" size="small" @click="generateAssetNo" v-if="!isEdit" style="margin-top: 8px">
                生成编号
              </el-button>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="资产名称" prop="assetName" required>
              <el-input v-model="form.assetName" placeholder="请输入资产名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="资产分类" prop="categoryId" required>
              <el-select v-model="form.categoryId" placeholder="请选择资产分类" style="width: 100%">
                <el-option
                  v-for="category in categoryOptions"
                  :key="category.id"
                  :label="category.categoryName"
                  :value="category.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="供应商">
              <el-select 
                v-model="form.supplierId" 
                placeholder="请选择或输入供应商" 
                clearable 
                filterable 
                allow-create 
                default-first-option
                style="width: 100%"
                @change="handleSupplierChange"
                @visible-change="handleSupplierVisibleChange"
              >
                <el-option
                  v-for="supplier in supplierOptions"
                  :key="supplier.id"
                  :label="supplier.supplierName"
                  :value="supplier.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="型号" prop="model" required>
              <el-input v-model="form.model" placeholder="请输入型号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单位" prop="unit" required>
              <el-input v-model="form.unit" placeholder="请输入单位" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 采购信息 -->
        <el-divider content-position="left">采购信息</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="采购价格" prop="purchasePrice" required>
              <el-input-number v-model="form.purchasePrice" :precision="2" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="采购日期" prop="purchaseDate" required>
              <el-date-picker v-model="form.purchaseDate" type="date" placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="保修期(月)" prop="warrantyPeriod" required>
              <el-input-number v-model="form.warrantyPeriod" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="使用年限" prop="usefulLife" required>
              <el-input-number v-model="form.usefulLife" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 状态信息 -->
        <el-divider content-position="left">状态信息</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="资产状态" prop="status" required>
              <el-select v-model="form.status" placeholder="请选择资产状态" style="width: 100%" :disabled="!isEdit">
                <el-option label="在库" value="in_stock" />
                <el-option label="使用中" value="using" />
                <el-option label="维修中" value="maintenance" />
                <el-option label="已报废" value="scrapped" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="使用状态" prop="useStatus" required>
              <el-select v-model="form.useStatus" placeholder="请选择使用状态" style="width: 100%" :disabled="!isEdit">
                <el-option label="闲置" value="idle" />
                <el-option label="使用中" value="using" />
                <el-option label="维修中" value="repairing" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="存放位置" prop="location" required>
              <el-autocomplete
                v-model="form.location"
                :fetch-suggestions="querySearch"
                placeholder="请输入或选择存放位置"
                clearable
                style="width: 100%"
                @select="handleLocationSelect"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 借出信息 -->
        <el-divider content-position="left">借出信息</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="借出状态" prop="borrowStatus" required>
              <el-select v-model="form.borrowStatus" placeholder="请选择借出状态" style="width: 100%" :disabled="!isEdit" @change="handleBorrowStatusChange">
                <el-option label="未借出" value="not_borrowed" />
                <el-option label="已借出" value="borrowed" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        
        <!-- 已借出时显示的额外字段 -->
        <template v-if="form.borrowStatus === 'borrowed'">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="当前位置" prop="currentLocation">
                <el-input v-model="form.currentLocation" placeholder="请输入当前实际位置" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="借用人" prop="borrowerId">
                <el-select v-model="form.borrowerId" placeholder="请选择借用人" clearable style="width: 100%" @change="handleBorrowerChange">
                  <el-option
                    v-for="user in userOptions"
                    :key="user.id"
                    :label="user.realName || user.username"
                    :value="user.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="借出时间" prop="borrowTime">
                <el-date-picker 
                  v-model="form.borrowTime" 
                  type="datetime" 
                  placeholder="选择借出时间" 
                  style="width: 100%" 
                  value-format="YYYY-MM-DD HH:mm:ss" 
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="预计归还" prop="expectedReturnTime">
                <el-date-picker 
                  v-model="form.expectedReturnTime" 
                  type="datetime" 
                  placeholder="选择预计归还时间" 
                  style="width: 100%" 
                  value-format="YYYY-MM-DD HH:mm:ss" 
                />
              </el-form-item>
            </el-col>
          </el-row>
        </template>
        
        <!-- 备注信息 -->
        <el-divider content-position="left">备注信息</el-divider>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 更新状态对话框 -->
    <el-dialog
      v-model="statusDialogVisible"
      title="更新资产状态"
      width="400px"
      :close-on-click-modal="false"
    >
      <el-form :model="statusForm" label-width="100px">
        <el-form-item label="资产状态">
          <el-select v-model="statusForm.status" placeholder="请选择资产状态" style="width: 100%">
            <el-option label="在库" value="in_stock" />
            <el-option label="使用中" value="using" />
            <el-option label="维修中" value="maintenance" />
            <el-option label="已报废" value="scrapped" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="statusDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleStatusSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted, computed } from 'vue'
import axios from '../utils/request'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Plus, 
  Edit, 
  Delete, 
  ArrowLeft,
  Search,
  Refresh
} from '@element-plus/icons-vue'

export default {
  name: 'AssetList',
  setup() {
    const router = useRouter()
    const route = useRoute()
    const loading = ref(false)
    const assetList = ref([])
    const categoryOptions = ref([])
    const departmentOptions = ref([])
    const userOptions = ref([])
    const supplierOptions = ref([])
    const locationOptions = ref([])
    
    const dialogVisible = ref(false)
    const dialogTitle = ref('')
    const formRef = ref(null)
    const isEdit = ref(false)
    const currentId = ref(null)

    const statusDialogVisible = ref(false)
    const statusForm = reactive({
      status: ''
    })
    const currentAsset = ref(null)

    const searchForm = reactive({
      assetName: '',
      status: ''
    })

    const form = reactive({
      assetNo: '',
      assetName: '',
      categoryId: null,
      supplierId: null,
      model: '',
      unit: '',
      purchasePrice: 0,
      originalValue: 0,
      netValue: 0,
      purchaseDate: '',
      warrantyPeriod: 12,
      usefulLife: 5,
      status: 'in_stock',
      useStatus: 'idle',
      location: '',
      userId: null,
      remark: '',
      borrowStatus: 'not_borrowed',
      currentLocation: '',
      borrowerId: null,
      borrowerName: '',
      borrowTime: '',
      expectedReturnTime: ''
    })

    const rules = reactive({
      assetNo: [{ required: true, message: '请输入资产编号', trigger: 'blur' }],
      assetName: [{ required: true, message: '请输入资产名称', trigger: 'blur' }],
      categoryId: [{ required: true, message: '请选择资产分类', trigger: 'change' }],
      model: [{ required: true, message: '请输入型号', trigger: 'blur' }],
      warrantyPeriod: [{ required: true, message: '请输入保修期', trigger: 'blur' }],
      unit: [{ required: true, message: '请输入单位', trigger: 'blur' }],
      purchasePrice: [{ required: true, message: '请输入采购价格', trigger: 'blur' }],
      purchaseDate: [{ required: true, message: '请选择采购日期', trigger: 'blur' }],
      usefulLife: [{ required: true, message: '请输入使用年限', trigger: 'blur' }],
      status: [{ required: true, message: '请选择资产状态', trigger: 'change' }],
      useStatus: [{ required: true, message: '请选择使用状态', trigger: 'change' }]
    })

    const getToken = () => localStorage.getItem('token')
    
    // 检查是否为系统管理员（用于新增资产）
    const hasAdminPermission = () => {
      const userStr = localStorage.getItem('user')
      
      if (userStr) {
        try {
          const user = JSON.parse(userStr)
          
          // 只有系统管理员可以新增资产
          if (user.roles && Array.isArray(user.roles) && user.roles.length > 0) {
            for (const role of user.roles) {
              let roleName = ''
              if (typeof role === 'string') {
                roleName = role.toLowerCase()
              } else if (role.name) {
                roleName = role.name.toLowerCase()
              }
              // 移除 ROLE_ 前缀并比较（不区分大小写）
              if (roleName.startsWith('role_')) {
                roleName = roleName.substring(5)
              }
              if (roleName === 'admin') {
                return true
              }
            }
          }
          
          // 兼容旧版用户数据（没有roles数组时，检查role字段）
          if (user.role) {
            const roleName = user.role.toLowerCase()
            if (roleName === 'admin') {
              return true
            }
          }
        } catch (e) {
          console.error('解析用户信息失败:', e)
        }
      }
      return false
    }
    
    // 检查是否为管理角色（管理员、领导、部门资产管理员，用于查看详细信息）
    const hasManagerPermission = () => {
      const userStr = localStorage.getItem('user')
      
      if (userStr) {
        try {
          const user = JSON.parse(userStr)
          
          // 管理员、领导、部门资产管理员可以查看详细信息
          if (user.roles && Array.isArray(user.roles) && user.roles.length > 0) {
            for (const role of user.roles) {
              let roleName = ''
              if (typeof role === 'string') {
                roleName = role.toLowerCase()
              } else if (role.name) {
                roleName = role.name.toLowerCase()
              }
              // 移除 ROLE_ 前缀并比较（不区分大小写）
              if (roleName.startsWith('role_')) {
                roleName = roleName.substring(5)
              }
              if (roleName === 'admin' || roleName === 'leader' || roleName === 'manager') {
                return true
              }
            }
          }
          
          // 兼容旧版用户数据（没有roles数组时，检查role字段）
          if (user.role) {
            const roleName = user.role.toLowerCase()
            if (roleName === 'admin' || roleName === 'leader' || roleName === 'manager') {
              return true
            }
          }
        } catch (e) {
          console.error('解析用户信息失败:', e)
        }
      }
      return false
    }
    
    // 获取用户角色
    const getUserRole = () => {
      const userStr = localStorage.getItem('user')
      if (userStr) {
        try {
          const user = JSON.parse(userStr)
          
          // 优先从roles数组获取角色
          if (user.roles && Array.isArray(user.roles)) {
            for (const role of user.roles) {
              let roleName = ''
              if (typeof role === 'string') {
                roleName = role.toLowerCase()
              } else if (role.name) {
                roleName = role.name.toLowerCase()
              }
              if (roleName.startsWith('role_')) {
                roleName = roleName.substring(5)
              }
              if (roleName === 'admin') return 'admin'
              if (roleName === 'leader') return 'leader'
              if (roleName === 'manager') return 'manager'
            }
          }
          
          // 兼容旧版用户数据（没有roles数组时，检查role字段）
          if (user.role) {
            const roleName = user.role.toLowerCase()
            if (roleName === 'admin') return 'admin'
            if (roleName === 'leader') return 'leader'
            if (roleName === 'manager') return 'manager'
          }
        } catch (e) {
          console.error('解析用户信息失败:', e)
        }
      }
      return 'user'
    }
    
    // 页面标题（根据角色动态显示）
    const pageTitle = computed(() => {
      const userRole = getUserRole()
      const titleMap = {
        'admin': '资产列表',
        'leader': '部门资产',
        'manager': '部门资产',
        'user': '我的资产'
      }
      return titleMap[userRole] || '我的资产'
    })

    // 获取资产列表
    const fetchAssets = async () => {
      loading.value = true
      try {
        const response = await axios.get('/assets')
        if (response.data.code === 200) {
          assetList.value = response.data.data
          // 更新存放位置列表
          fetchLocations()
        }
      } catch (error) {
        console.error('获取资产列表失败', error)
        ElMessage.error('获取资产列表失败')
      } finally {
        loading.value = false
      }
    }

    // 获取资产分类列表
    const fetchCategories = async () => {
      try {
        const response = await axios.get('/asset-categories')
        if (response.data.code === 200) {
          const allCategories = response.data.data
          // 过滤出最小级别的分类（没有子分类的分类）
          const leafCategories = allCategories.filter(category => {
            return !allCategories.some(c => c.parentId === category.id)
          })
          categoryOptions.value = leafCategories
        }
      } catch (error) {
        console.error('获取资产分类失败', error)
      }
    }

    // 获取部门列表
    const fetchDepartments = async () => {
      try {
        const response = await axios.get('/departments')
        if (response.data.code === 200) {
          departmentOptions.value = response.data.data
        }
      } catch (error) {
        console.error('获取部门列表失败', error)
      }
    }

    // 获取用户列表
    const fetchUsers = async () => {
      try {
        const response = await axios.get('/users')
        if (response.data.code === 200) {
          userOptions.value = response.data.data
        }
      } catch (error) {
        console.error('获取用户列表失败', error)
      }
    }

    // 获取供应商列表
    const fetchSuppliers = async () => {
      try {
        const response = await axios.get('/suppliers')
        if (response.data.code === 200) {
          supplierOptions.value = response.data.data
        }
      } catch (error) {
        console.error('获取供应商列表失败', error)
      }
    }

    // 获取存放位置列表
    const fetchLocations = () => {
      // 从资产列表中提取唯一的存放位置
      const locations = new Set()
      assetList.value.forEach(asset => {
        if (asset.location) {
          locations.add(asset.location)
        }
      })
      locationOptions.value = Array.from(locations)
    }

    // 过滤后的资产列表
    const filteredAssetList = computed(() => {
      // 获取当前用户信息
      const userStr = localStorage.getItem('user')
      let currentUserId = null
      let currentDepartmentId = null
      let hasPermission = false
      if (userStr) {
        try {
          const user = JSON.parse(userStr)
          currentUserId = user.id
          currentDepartmentId = user.departmentId || user.deptId
          hasPermission = hasManagerPermission()
        } catch (e) {
          console.error('解析用户信息失败:', e)
        }
      }
      
      // 检查是否只显示我的资产
      const onlyMyAssets = route.query.onlyMyAssets === 'true'
      
      // 获取资产的部门ID（兼容多种字段名格式）
      const getAssetDeptId = (asset) => {
        // 检查多种可能的字段名格式
        if (asset.deptId !== undefined && asset.deptId !== null) return asset.deptId
        if (asset.departmentId !== undefined && asset.departmentId !== null) return asset.departmentId
        if (asset.dept_id !== undefined && asset.dept_id !== null) return asset.dept_id
        return null
      }
      
      return assetList.value.map(asset => {
        const category = categoryOptions.value.find(c => c.id === asset.categoryId)
        const assetDeptId = getAssetDeptId(asset)
        const department = departmentOptions.value.find(d => d.id === assetDeptId)
        const user = userOptions.value.find(u => u.id === asset.userId)
        // 确保类型匹配，将supplierId转换为数字
        const supplierId = Number(asset.supplierId)
        const supplier = supplierOptions.value.find(s => Number(s.id) === supplierId)
        return {
          ...asset,
          deptId: assetDeptId,  // 统一字段名
          categoryName: category ? category.categoryName : '-',
          departmentName: department ? department.deptName : '-',
          userName: user ? (user.realName || user.username) : '-',
          supplierName: supplier ? supplier.supplierName : '-'
        }
      }).filter(asset => {
        // 普通员工只能查看自己的资产（类型转换确保比较正确）
        if (!hasPermission && Number(asset.userId) !== Number(currentUserId)) {
          return false
        }
        
        // 领导和部门资产管理员只能查看本部门的资产
        const assetDeptId = getAssetDeptId(asset)
        if (hasPermission && !hasAdminPermission() && currentDepartmentId && Number(assetDeptId) !== Number(currentDepartmentId)) {
          return false
        }
        
        // 只显示我的资产（类型转换确保比较正确）
        if (onlyMyAssets && Number(asset.userId) !== Number(currentUserId)) {
          return false
        }
        
        if (searchForm.assetName && !asset.assetName.includes(searchForm.assetName)) {
          return false
        }
        if (searchForm.status && asset.status !== searchForm.status) {
          return false
        }
        return true
      })
    })

    // 获取状态类型
    const getStatusType = (status) => {
      const typeMap = {
        'in_stock': 'info',
        'using': 'success',
        'maintenance': 'warning',
        'scrapped': 'danger'
      }
      return typeMap[status] || 'info'
    }

    // 获取状态文本
    const getStatusText = (status) => {
      const textMap = {
        'in_stock': '在库',
        'using': '使用中',
        'maintenance': '维修中',
        'scrapped': '已报废'
      }
      return textMap[status] || status
    }

    // 获取使用状态类型
    const getUseStatusType = (useStatus) => {
      const typeMap = {
        'idle': 'info',
        'using': 'success',
        'repairing': 'warning'
      }
      return typeMap[useStatus] || 'info'
    }

    // 获取使用状态文本
    const getUseStatusText = (useStatus) => {
      const textMap = {
        'idle': '闲置',
        'using': '使用中',
        'repairing': '维修中'
      }
      return textMap[useStatus] || useStatus
    }

    // 获取用户所属部门名称
    const getUserDepartmentName = (userId) => {
      if (!userId) return '-'
      const user = userOptions.value.find(u => u.id === userId)
      if (!user) return '-'
      // 兼容 deptId 和 departmentId 两种字段名
      const userDeptId = user.departmentId || user.deptId
      if (!userDeptId) return '-'
      const department = departmentOptions.value.find(d => d.id === userDeptId)
      return department ? department.deptName : '-'
    }

    // 获取供应商名称
    const getSupplierName = (supplierId) => {
      if (!supplierId) return '-'
      // 确保类型匹配，将supplierId转换为数字
      const id = Number(supplierId)
      const supplier = supplierOptions.value.find(s => Number(s.id) === id)
      return supplier ? supplier.supplierName : '-'
    }

    // 格式化价格
    const formatPrice = (price) => {
      if (price === null || price === undefined) return '-'
      return '¥' + parseFloat(price).toFixed(2)
    }

    // 检查当前用户是否是资产的使用者或借用者
    const isAssetUserOrBorrower = (asset) => {
      const userStr = localStorage.getItem('user')
      if (userStr) {
        try {
          const user = JSON.parse(userStr)
          const currentUserId = user.id
          
          // 检查是否是资产的使用者（类型转换确保比较正确）
          if (Number(asset.userId) === Number(currentUserId)) {
            return true
          }
          
          // 检查是否是资产的借用者（类型转换确保比较正确）
          if (asset.borrowStatus === 'borrowed' && Number(asset.borrowerId) === Number(currentUserId)) {
            return true
          }
        } catch (e) {
          console.error('解析用户信息失败:', e)
        }
      }
      return false
    }

    // 格式化时间
    const formatTime = (time) => {
      if (!time) return '-'
      return new Date(time).toLocaleString()
    }

    // 生成资产编号
    const generateAssetNo = () => {
      // 获取当前日期
      const now = new Date()
      const year = now.getFullYear()
      const month = String(now.getMonth() + 1).padStart(2, '0')
      
      // 生成随机数，确保唯一性
      const random = String(Math.floor(Math.random() * 10000)).padStart(4, '0')
      
      // 构建资产编号：AST-YYYY-MM-XXXX
      const assetNo = `AST-${year}-${month}-${random}`
      
      // 检查编号是否已存在
      const isExists = assetList.value.some(asset => asset.assetNo === assetNo)
      
      if (isExists) {
        // 如果存在，递归生成新编号
        generateAssetNo()
      } else {
        form.assetNo = assetNo
        ElMessage.success('资产编号生成成功')
      }
    }

    // 返回首页
    const goBack = () => {
      router.push('/home')
    }
    
    // 申请资产
    const handleApplyAsset = (row) => {
      router.push({ path: '/assets/apply', query: { assetId: row.id } })
    }

    // 搜索
    const handleSearch = () => {
      // 过滤逻辑在 computed 中处理
    }

    // 重置搜索
    const handleReset = () => {
      searchForm.assetName = ''
      searchForm.status = ''
    }

    // 新增资产
    const handleAdd = () => {
      isEdit.value = false
      currentId.value = null
      dialogTitle.value = '新增资产'
      Object.assign(form, {
        assetNo: '',
        assetName: '',
        categoryId: null,
        supplierId: null,
        model: '',
        unit: '',
        purchasePrice: 0,
        originalValue: 0,
        netValue: 0,
        purchaseDate: '',
        warrantyPeriod: 12,
        usefulLife: 5,
        status: 'in_stock', // 默认在库
        useStatus: 'idle', // 默认闲置
        location: '',
        userId: null,
        remark: '',
        // 借出信息默认值
        borrowStatus: 'not_borrowed', // 默认未借出
        currentLocation: '',
        borrowerId: null,
        borrowerName: '',
        borrowTime: '',
        expectedReturnTime: ''
      })
      dialogVisible.value = true
    }

    // 编辑资产
    const handleEdit = (row) => {
      isEdit.value = true
      currentId.value = row.id
      dialogTitle.value = '编辑资产'
      Object.assign(form, {
        assetNo: row.assetNo,
        assetName: row.assetName,
        categoryId: row.categoryId,
        supplierId: row.supplierId,
        model: row.model || '',
        unit: row.unit || '',
        purchasePrice: row.purchasePrice || 0,
        originalValue: row.originalValue || row.purchasePrice || 0,
        netValue: row.netValue || row.purchasePrice || 0,
        purchaseDate: row.purchaseDate || '',
        warrantyPeriod: row.warrantyPeriod || 12,
        usefulLife: row.usefulLife || 5,
        status: row.status || 'in_stock',
        useStatus: row.useStatus || 'idle',
        location: row.location || '',
        userId: row.userId,
        remark: row.remark || '',
        // 借出信息
        borrowStatus: row.borrowStatus || 'not_borrowed',
        currentLocation: row.currentLocation || '',
        borrowerId: row.borrowerId,
        borrowerName: row.borrowerName || '',
        borrowTime: row.borrowTime || '',
        expectedReturnTime: row.expectedReturnTime || ''
      })
      dialogVisible.value = true
    }

    // 借出状态改变处理
    const handleBorrowStatusChange = (value) => {
      if (value === 'not_borrowed') {
        // 清空借出相关信息
        form.currentLocation = ''
        form.borrowerId = null
        form.borrowerName = ''
        form.borrowTime = ''
        form.expectedReturnTime = ''
      } else if (value === 'borrowed') {
        // 设置默认借出时间为当前时间
        if (!form.borrowTime) {
          const now = new Date()
          form.borrowTime = now.toISOString().slice(0, 19).replace('T', ' ')
        }
      }
    }

    // 借用人改变处理
    const handleBorrowerChange = (userId) => {
      if (userId) {
        const user = userOptions.value.find(u => u.id === userId)
        if (user) {
          form.borrowerName = user.realName || user.username
        }
      } else {
        form.borrowerName = ''
      }
    }

    // 供应商改变处理
    const handleSupplierChange = async (value) => {
      // 如果是字符串，说明是手动输入的新供应商
      if (typeof value === 'string') {
        try {
          // 创建新供应商
          const response = await axios.post('/suppliers', {
            supplierName: value
          })
          if (response.data.code === 200) {
            const newSupplier = response.data.data
            // 更新供应商选项
            supplierOptions.value.push(newSupplier)
            // 更新表单中的供应商ID
            form.supplierId = newSupplier.id
            ElMessage.success('供应商创建成功')
          }
        } catch (error) {
          console.error('创建供应商失败', error)
          ElMessage.error('创建供应商失败，请重试')
        }
      }
    }

    // 供应商下拉框显示/隐藏处理
    const handleSupplierVisibleChange = (visible) => {
      if (visible) {
        // 下拉框显示时，确保供应商列表是最新的
        fetchSuppliers()
      }
    }

    // 存放位置变化处理
    const handleLocationChange = (value) => {
      // 确保值被正确设置到 form.location，处理可能的数组情况
      if (value) {
        // 如果 value 是数组，取第一个元素
        if (Array.isArray(value)) {
          form.location = value[0]
        } else {
          form.location = value
        }
      }
    }

    // 存放位置失去焦点处理
    const handleLocationBlur = () => {
      // 当失去焦点时，确保值被正确设置
      // 这里可以添加额外的验证逻辑
    }

    // 存放位置搜索处理
    const querySearch = (queryString, callback) => {
      // 过滤出匹配的存放位置
      const results = queryString ? locationOptions.value.filter(location => {
        return location.toLowerCase().includes(queryString.toLowerCase())
      }) : locationOptions.value
      // 转换为 el-autocomplete 需要的格式
      const formattedResults = results.map(location => ({
        value: location,
        label: location
      }))
      callback(formattedResults)
    }

    // 存放位置选择处理
    const handleLocationSelect = (item) => {
      // 确保值被正确设置
      form.location = item.value
    }

    // 更新状态
    const handleUpdateStatus = (row) => {
      currentAsset.value = row
      statusForm.status = row.status
      statusDialogVisible.value = true
    }

    // 提交状态更新
    const handleStatusSubmit = async () => {
      try {
        // 更新资产状态
        if (statusForm.status !== currentAsset.value.status) {
          const response = await axios.put(`/assets/${currentAsset.value.id}/status`, 
            { status: statusForm.status }
          )
          if (response.data.code !== 200) {
            ElMessage.error('更新资产状态失败')
            return
          }
        }
        
        ElMessage.success('状态更新成功')
        statusDialogVisible.value = false
        fetchAssets()
      } catch (error) {
        ElMessage.error('状态更新失败')
      }
    }

    // 删除资产
    const handleDelete = (row) => {
      ElMessageBox.confirm(`确定删除资产 "${row.assetName}" 吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const response = await axios.delete(`/assets/${row.id}`)
          if (response.data.code === 200) {
            ElMessage.success('删除成功')
            fetchAssets()
          } else {
            ElMessage.error(response.data.message || '删除失败')
          }
        } catch (error) {
          ElMessage.error('删除失败')
        }
      })
    }

    // 提交表单
    const handleSubmit = async () => {
      formRef.value.validate(async (valid) => {
        if (valid) {
          try {
            let response
            if (isEdit.value) {
              response = await axios.put(`/assets/${currentId.value}`, form)
            } else {
              response = await axios.post('/assets', form)
            }
            if (response.data.code === 200) {
              ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
              dialogVisible.value = false
              fetchAssets()
              // 更新存放位置列表
              fetchLocations()
            } else {
              ElMessage.error(response.data.message || '操作失败')
            }
          } catch (error) {
            ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
          }
        }
      })
    }

    // 维修完成处理函数
    const handleRepairComplete = async (row) => {
      try {
        const userStr = localStorage.getItem('user')
        if (!userStr) {
          ElMessage.error('请先登录')
          return
        }
        const currentUser = JSON.parse(userStr)

        // 找到对应的维修中申请
        const inProgressApp = maintenanceApplications.value.find(app => {
          const appAssetId = app.assetId || app.asset_id
          const appStatus = app.status || app.application_status
          return Number(appAssetId) === Number(row.id) && appStatus === 'in_progress'
        })

        if (!inProgressApp) {
          ElMessage.error('未找到维修中的申请记录')
          return
        }

        // 调用完成维修API
        const response = await axios.post(`/asset-applications/${inProgressApp.id}/complete-maintenance`, {
          userId: currentUser.id,
          userName: currentUser.realName || currentUser.username
        }, {
          headers: { Authorization: `Bearer ${getToken()}` }
        })

        if (response.data.code === 200) {
          ElMessage.success('维修完成，资产状态已更新为使用中')
          // 更新资产列表和维修申请列表
          fetchAssets()
          await fetchMaintenanceApplications()
        } else {
          ElMessage.error(response.data.message || '操作失败')
        }
      } catch (error) {
        console.error('维修完成失败:', error)
        ElMessage.error('操作失败，请重试')
      }
    }

    // 维修申请列表（包含所有状态）
    const maintenanceApplications = ref([])

    // 获取维修申请列表
    const fetchMaintenanceApplications = async () => {
      try {
        const userStr = localStorage.getItem('user')
        if (!userStr) {
          return
        }
        const currentUser = JSON.parse(userStr)
        const response = await axios.get(`/asset-applications/applicant/${currentUser.id}`)
        if (response.data.code === 200) {
          // 后端返回的是Page分页对象，实际数据在content属性中
          const data = response.data.data
          const applications = data.content || data
          maintenanceApplications.value = applications.filter(item => {
            const appType = item.applicationType || item.application_type
            const appAssetId = item.assetId || item.asset_id
            const appStatus = item.status || item.application_status
            return (appType === 'REPAIR' || appType === 'MAINTENANCE') && appAssetId && appStatus
          })
        }
      } catch (error) {
        console.error('获取维修申请失败', error)
      }
    }

    // 检查资产是否有待审批的维修申请（类型转换确保比较正确）
    const hasPendingMaintenance = (assetId) => {
      if (!assetId) {
        return false
      }
      return maintenanceApplications.value.some(app => {
        const appAssetId = app.assetId || app.asset_id
        const appStatus = (app.status || app.application_status || '').toLowerCase()
        return appAssetId && Number(appAssetId) === Number(assetId) && appStatus === 'pending'
      })
    }

    // 检查资产是否有已批准的维修申请（类型转换确保比较正确）
    const hasApprovedMaintenance = (assetId) => {
      if (!assetId) {
        return false
      }
      return maintenanceApplications.value.some(app => {
        const appAssetId = app.assetId || app.asset_id
        const appStatus = (app.status || app.application_status || '').toLowerCase()
        return appAssetId && Number(appAssetId) === Number(assetId) && appStatus === 'approved'
      })
    }

    // 申请维修处理函数
    const handleApplyMaintenance = async (row) => {
      // 显示确认对话框
      ElMessageBox.confirm(
        `确定要为资产「${row.assetName}」申请维修吗？`,
        '确认申请维修',
        {
          confirmButtonText: '确认',
          cancelButtonText: '取消',
          type: 'warning'
        }
      ).then(async () => {
        try {
          const userStr = localStorage.getItem('user')
          if (!userStr) {
            ElMessage.error('请先登录')
            return
          }
          const currentUser = JSON.parse(userStr)
          
          // 创建维修申请记录
          const maintenanceData = {
            assetId: row.id,
            applicationType: 'REPAIR',
            applicantId: currentUser.id,
            applicantName: currentUser.realName || currentUser.username,
            departmentId: currentUser.departmentId || currentUser.deptId,
            departmentName: currentUser.departmentName,
            applicationReason: '资产维修申请',
            maintenanceType: '日常维护',
            applicationDate: new Date().toISOString(),
            status: 'pending'
          }

          const response = await axios.post('/asset-applications', maintenanceData, {
            headers: { Authorization: `Bearer ${getToken()}` }
          })

          if (response.data.code === 200) {
            ElMessage.success('维修申请已提交，请等待资产管理员审批')
            // 更新维修申请列表
            await fetchMaintenanceApplications()
            fetchAssets()
          } else {
            ElMessage.error(response.data.message || '提交申请失败')
          }
        } catch (error) {
          ElMessage.error('操作失败，请重试')
        }
      }).catch(() => {
        // 用户点击取消
        ElMessage.info('已取消申请')
      })
    }

    // 开始维修处理函数
    const handleStartMaintenance = async (row) => {
      try {
        const userStr = localStorage.getItem('user')
        if (!userStr) {
          ElMessage.error('请先登录')
          return
        }
        const currentUser = JSON.parse(userStr)

        // 找到对应的已批准申请
        const approvedApp = maintenanceApplications.value.find(app => {
          const appAssetId = app.assetId || app.asset_id
          const appStatus = app.status || app.application_status
          return Number(appAssetId) === Number(row.id) && appStatus === 'approved'
        })

        if (!approvedApp) {
          ElMessage.error('未找到已批准的维修申请')
          return
        }

        // 发送包含 userId 和 userName 的请求体
        const response = await axios.put(`/asset-applications/${approvedApp.id}/start-maintenance`, {
          userId: currentUser.id,
          userName: currentUser.realName || currentUser.username
        }, {
          headers: { Authorization: `Bearer ${getToken()}` }
        })

        if (response.data.code === 200) {
          ElMessage.success('已开始维修，资产状态已更新为维修中')
          // 更新申请列表
          await fetchMaintenanceApplications()
          fetchAssets()
        } else {
          ElMessage.error(response.data.message || '开始维修失败')
        }
      } catch (error) {
        console.error('开始维修失败:', error)
        ElMessage.error('操作失败，请重试')
      }
    }



    onMounted(() => {
      // 读取 URL 参数中的搜索关键词
      if (route.query.keyword) {
        searchForm.assetName = route.query.keyword
      }
      
      fetchAssets()
      fetchCategories()
      fetchDepartments()
      fetchUsers()
      fetchSuppliers()
      fetchLocations()
      fetchMaintenanceApplications()
    })

    return {
      loading,
      assetList,
      filteredAssetList,
      categoryOptions,
      departmentOptions,
      userOptions,
      supplierOptions,
      locationOptions,
      dialogVisible,
      dialogTitle,
      formRef,
      form,
      rules,
      searchForm,
      statusDialogVisible,
      statusForm,
      pageTitle,
      hasAdminPermission,
      hasManagerPermission,
      isAssetUserOrBorrower,
      isEdit,
      goBack,
      handleSearch,
      handleReset,
      handleAdd,
      handleEdit,
      handleUpdateStatus,
      handleStatusSubmit,
      handleDelete,
      handleSubmit,
      handleApplyAsset,
      handleRepairComplete,
      handleApplyMaintenance,
      handleStartMaintenance,
      hasPendingMaintenance,
      hasApprovedMaintenance,
      generateAssetNo,
      handleSupplierChange,
      handleSupplierVisibleChange,
      handleLocationBlur,
      querySearch,
      handleLocationSelect,
      handleBorrowStatusChange,
      handleBorrowerChange,
      getStatusType,
      getStatusText,
      getUserDepartmentName,
      getSupplierName,
      formatPrice,
      formatTime,
      Plus,
      Edit,
      Delete,
      ArrowLeft,
      Search,
      Refresh
    }
  }
}
</script>

<style scoped>
.asset-list-container {
  padding: 20px;
  height: 100vh;
  overflow: hidden;
}

.asset-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 0;
  margin: 0;
  border: none;
  box-shadow: none;
}

.asset-card :deep(.el-card__body) {
  padding: 0;
  margin: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: sticky;
  top: 0;
  z-index: 10;
  background-color: white;
  padding: 15px 20px;
  margin: 0;
  border-bottom: 1px solid #e4e7ed;
  border-radius: 4px 4px 0 0;
}

.title {
  font-size: 18px;
  font-weight: bold;
}

.search-form {
  position: sticky;
  top: 70px;
  z-index: 5;
  background-color: #f5f7fa;
  margin: 0 0 20px 0;
  padding: 15px 20px;
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  border-top: 1px solid #e4e7ed;
}

.table-container {
  flex: 1;
  overflow-y: auto;
  margin-top: 10px;
  max-height: calc(100vh - 200px);
}

/* 优化表格行高和单元格间距 */
:deep(.el-table__row) {
  height: 48px;
}

:deep(.el-table__cell) {
  padding: 8px 12px;
}

/* 优化资产名称列的显示 */
:deep(.el-table__column--min-width) {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.status-with-action {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}

.el-tag {
  margin-right: 8px;
  margin-bottom: 4px;
}

.el-button {
  margin-bottom: 4px;
}

.location-cell {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}

.borrow-tag {
  margin-left: 4px;
}

.info-icon {
  margin-left: 4px;
  color: #909399;
  cursor: pointer;
  font-size: 14px;
}

.info-icon:hover {
  color: #409eff;
}

.borrow-tooltip {
  line-height: 1.8;
  font-size: 12px;
}

.borrow-tooltip div {
  margin-bottom: 4px;
}

.borrow-tooltip div:last-child {
  margin-bottom: 0;
}
</style>
