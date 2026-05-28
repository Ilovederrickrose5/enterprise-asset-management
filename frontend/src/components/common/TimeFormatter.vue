<template>
  <el-tag size="small" type="info" effect="plain">
    {{ formattedTime }}
  </el-tag>
</template>

<script>
import { ElTag } from 'element-plus'

export default {
  name: 'TimeFormatter',
  components: {
    ElTag
  },
  props: {
    time: {
      type: [String, Date],
      default: null
    },
    fallback: {
      type: String,
      default: '-'
    },
    type: {
      type: String,
      default: 'info',
      validator: (value) => ['info', 'success', 'warning', 'danger'].includes(value)
    }
  },
  computed: {
    formattedTime() {
      if (!this.time) return this.fallback
      const date = new Date(this.time)
      return date.toLocaleString('zh-CN')
    }
  }
}
</script>