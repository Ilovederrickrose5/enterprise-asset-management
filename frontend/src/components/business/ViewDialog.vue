<template>
  <el-dialog
    v-model="dialogVisible"
    :title="title"
    width="600px"
  >
    <el-descriptions :column="2" border>
      <slot></slot>
    </el-descriptions>
  </el-dialog>
</template>

<script>
import { ref, watch } from 'vue'

export default {
  name: 'ViewDialog',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    title: {
      type: String,
      default: '详情'
    }
  },
  emits: ['update:visible'],
  setup(props, { emit }) {
    const dialogVisible = ref(props.visible)

    watch(() => props.visible, (newVal) => {
      dialogVisible.value = newVal
    })

    watch(dialogVisible, (newVal) => {
      emit('update:visible', newVal)
    })

    return {
      dialogVisible
    }
  }
}
</script>