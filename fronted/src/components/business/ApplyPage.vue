<template>
  <div :class="`${pageClass}-apply`">
    <!-- 表单卡片 -->
    <el-card v-if="showForm" class="box-card">
      <template #header>
        <PageHeader :title="formTitle" />
      </template>

      <slot name="form" :submit="handleFormSubmit" :reset="handleFormReset">
        <component
          :is="formComponent"
          v-if="formComponent"
          v-bind="formProps"
          @submit="handleFormSubmit"
          @reset="handleFormReset"
        />
      </slot>
    </el-card>

    <!-- 记录卡片 -->
    <el-card class="box-card" :style="showForm ? 'margin-top: 20px;' : ''">
      <template #header>
        <div class="card-header">
          <span>{{ recordTitle }}</span>
          <el-button 
            v-if="showBackButton && !showForm" 
            type="primary" 
            size="small" 
            @click="goBack"
          >
            返回首页
          </el-button>
        </div>
      </template>

      <DataTable
        :data="records"
        :columns="recordColumns"
        :loading="loading"
        :show-index="showIndex"
        :show-pagination="showPagination"
        :total="total"
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :show-edit-button="false"
        :show-delete-button="false"
        @page-change="handlePageChange"
      >
        <template v-for="column in customColumns" :key="column" #[`column-${column}`]="{ row, value }">
          <slot :name="`column-${column}`" :row="row" :value="value" />
        </template>
        
        <template #actions="{ row }" v-if="showRecordActions">
          <slot name="record-actions" :row="row" />
        </template>
      </DataTable>
    </el-card>

    <!-- 额外对话框 -->
    <slot name="dialogs" />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import PageHeader from '../common/PageHeader.vue'
import DataTable from '../common/DataTable.vue'

const props = defineProps({
  // 页面基础配置
  pageClass: {
    type: String,
    default: 'asset'
  },
  
  // 表单配置
  showForm: {
    type: Boolean,
    default: true
  },
  formTitle: {
    type: String,
    default: ''
  },
  formComponent: {
    type: Object,
    default: null
  },
  formProps: {
    type: Object,
    default: () => ({})
  },
  
  // 记录配置
  recordTitle: {
    type: String,
    default: '我的申请记录'
  },
  records: {
    type: Array,
    default: () => []
  },
  recordColumns: {
    type: Array,
    required: true
  },
  loading: {
    type: Boolean,
    default: false
  },
  showIndex: {
    type: Boolean,
    default: true
  },
  showRecordActions: {
    type: Boolean,
    default: false
  },
  
  // 分页配置
  showPagination: {
    type: Boolean,
    default: false
  },
  total: {
    type: Number,
    default: 0
  },
  pageSize: {
    type: Number,
    default: 10
  },
  
  // 其他配置
  showBackButton: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits([
  'form-submit',
  'form-reset',
  'page-change'
])

const router = useRouter()

// 分页相关
const currentPage = ref(1)
const pageSize = ref(props.pageSize)

// 计算自定义列
const customColumns = computed(() => {
  return props.recordColumns
    .filter(col => col.type === 'custom' || col.slot)
    .map(col => col.prop)
})

// 方法
const goBack = () => {
  router.push('/dashboard')
}

const handleFormSubmit = (formData) => {
  emit('form-submit', formData)
}

const handleFormReset = () => {
  emit('form-reset')
}

const handlePageChange = ({ pageSize: size, currentPage: page }) => {
  emit('page-change', { pageSize: size, currentPage: page })
}

// 暴露方法给父组件
defineExpose({
  currentPage,
  pageSize
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
