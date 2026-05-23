<template>
  <el-dialog
    v-model="dialogVisible"
    :title="approvalType === 'approve' ? '批准申请' : '拒绝申请'"
    width="500px"
  >
    <el-form :model="approvalForm" ref="approvalFormRef" label-width="80px">
      <slot name="details"></slot>
      <el-form-item label="审批备注" prop="approvalRemark">
        <el-input
          v-model="approvalForm.approvalRemark"
          type="textarea"
          :rows="3"
          :placeholder="approvalType === 'approve' ? '请输入批准备注（可选）' : '请输入拒绝原因'"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="close">取消</el-button>
        <el-button 
          :type="approvalType === 'approve' ? 'success' : 'danger'" 
          @click="submit"
          :loading="loading"
        >
          {{ approvalType === 'approve' ? '确认批准' : '确认拒绝' }}
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script>
import { ref, reactive, watch } from 'vue'

export default {
  name: 'ApprovalDialog',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    approvalType: {
      type: String,
      default: 'approve',
      validator: (value) => ['approve', 'reject'].includes(value)
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:visible', 'submit', 'close'],
  setup(props, { emit }) {
    const dialogVisible = ref(props.visible)
    const approvalFormRef = ref(null)
    const approvalForm = reactive({
      approvalRemark: ''
    })

    watch(() => props.visible, (newVal) => {
      dialogVisible.value = newVal
    })

    watch(dialogVisible, (newVal) => {
      emit('update:visible', newVal)
    })

    const close = () => {
      dialogVisible.value = false
      emit('close')
    }

    const submit = () => {
      emit('submit', approvalForm)
    }

    return {
      dialogVisible,
      approvalFormRef,
      approvalForm,
      close,
      submit
    }
  }
}
</script>