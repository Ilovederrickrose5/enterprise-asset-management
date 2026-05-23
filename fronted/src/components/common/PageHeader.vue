<template>
  <el-row class="page-header" :gutter="20">
    <el-col :span="12">
      <el-button type="primary" size="small" @click="goBack" :icon="ArrowLeft">返回首页</el-button>
    </el-col>
    <el-col :span="12" class="text-right">
      <el-tag type="info" size="large" class="title-tag">{{ title }}</el-tag>
    </el-col>
  </el-row>
</template>

<script>
import { ArrowLeft } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { ElRow, ElCol, ElTag } from 'element-plus'

export default {
  name: 'PageHeader',
  components: {
    ElRow,
    ElCol,
    ElTag
  },
  props: {
    title: {
      type: String,
      required: true
    }
  },
  setup() {
    const router = useRouter()
    
    const goBack = () => {
      router.push('/dashboard')
    }
    
    return {
      ArrowLeft,
      goBack
    }
  }
}
</script>

<style scoped>
.page-header {
  align-items: center;
}

.title-tag {
  font-weight: bold;
  font-size: 16px;
}
</style>