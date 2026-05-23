<template>
  <div :class="`${pageClass}-approval`">
    <el-card class="box-card">
      <template #header>
        <PageHeader :title="pageTitle" />
      </template>

      <el-tabs v-model="activeTab" type="card" class="mt-2">
        <el-tab-pane :label="pendingTabLabel" name="pending">
          <ApprovalTable
            :records="pendingItems"
            :loading="loading"
            @approve="handleApprove"
            @reject="handleReject"
            @view="handleView"
          >
            <template #columns>
              <slot name="pendingColumns" />
            </template>
            <template #actionButtons="{ row }">
              <slot name="actionButtons" :row="row" />
            </template>
          </ApprovalTable>

          <div v-if="pendingItems.length === 0" class="empty-state">
            <el-empty :description="pendingEmptyText" />
          </div>
        </el-tab-pane>

        <el-tab-pane :label="approvedTabLabel" name="approved">
          <el-table :data="approvedItems" style="width: 100%">
            <slot name="approvedColumns" />
          </el-table>

          <div v-if="approvedItems.length === 0" class="empty-state">
            <el-empty :description="approvedEmptyText" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 审批对话框 -->
    <ApprovalDialog
      v-model:visible="approvalDialogVisible"
      :approvalType="approvalType"
      :loading="approvalLoading"
      @submit="submitApproval"
    >
      <template #details>
        <slot name="approvalDetails" :item="currentItem" />
      </template>
    </ApprovalDialog>

    <!-- 查看详情对话框 -->
    <ViewDialog
      v-model:visible="viewDialogVisible"
      :title="viewDialogTitle"
    >
      <slot name="viewDetails" :item="currentItem" />
    </ViewDialog>

    <!-- 额外对话框（如完成报废） -->
    <slot name="extraDialogs" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '../common/PageHeader.vue'
import ApprovalTable from './ApprovalTable.vue'
import ApprovalDialog from './ApprovalDialog.vue'
import ViewDialog from './ViewDialog.vue'

const props = defineProps({
  pageTitle: {
    type: String,
    required: true
  },
  pageClass: {
    type: String,
    required: true
  },
  pendingTabLabel: {
    type: String,
    default: '待审批'
  },
  approvedTabLabel: {
    type: String,
    default: '已审批'
  },
  pendingEmptyText: {
    type: String,
    default: '暂无待审批的申请'
  },
  approvedEmptyText: {
    type: String,
    default: '暂无已审批的申请'
  },
  viewDialogTitle: {
    type: String,
    default: '申请详情'
  }
})

const emit = defineEmits(['approve', 'reject', 'view', 'submitApproval'])

// 状态管理
const loading = ref(false)
const approvalLoading = ref(false)
const approvalDialogVisible = ref(false)
const viewDialogVisible = ref(false)
const approvalType = ref('approve')
const activeTab = ref('pending')
const currentItem = ref({})
const pendingItems = ref([])
const approvedItems = ref([])

// 方法
const handleApprove = (item) => {
  currentItem.value = item
  approvalType.value = 'approve'
  approvalDialogVisible.value = true
}

const handleReject = (item) => {
  currentItem.value = item
  approvalType.value = 'reject'
  approvalDialogVisible.value = true
}

const handleView = (item) => {
  currentItem.value = item
  viewDialogVisible.value = true
}

const submitApproval = async (formData) => {
  if (!currentItem.value) return
  emit('submitApproval', { formData, item: currentItem.value, type: approvalType.value })
}

// 设置加载状态
const setLoading = (value) => {
  loading.value = value
}

// 设置待审批项目
const setPendingItems = (items) => {
  pendingItems.value = items
}

// 设置已审批项目
const setApprovedItems = (items) => {
  approvedItems.value = items
}

// 暴露给父组件的方法
defineExpose({
  setLoading,
  setPendingItems,
  setApprovedItems,
  currentItem,
  approvalDialogVisible,
  viewDialogVisible,
  handleApprove,
  handleReject,
  handleView,
  submitApproval
})
</script>

<style scoped>
.asset-approval {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  margin-bottom: 20px;
}

.section-title {
  font-size: 16px;
  font-weight: bold;
  margin: 20px 0 10px 0;
  color: #303133;
}

.empty-state {
  text-align: center;
  padding: 40px 0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>