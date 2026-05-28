<template>
  <el-form
    :model="model"
    :rules="rules"
    ref="formRef"
    :label-width="labelWidth"
    :validate-on-rule-change="false"
  >
    <slot></slot>
    
    <el-form-item>
      <el-button type="primary" @click="submit" :loading="submitting">提交</el-button>
      <el-button @click="reset">重置</el-button>
    </el-form-item>
  </el-form>
</template>

<script>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

export default {
  name: 'BaseForm',
  props: {
    model: {
      type: Object,
      required: true
    },
    rules: {
      type: Object,
      default: () => {}
    },
    labelWidth: {
      type: String,
      default: '100px'
    }
  },
  emits: ['submit', 'reset'],
  setup(props, { emit }) {
    const formRef = ref(null)
    const submitting = ref(false)
    
    const submit = async () => {
      if (!formRef.value) return
      
      formRef.value.validate((valid) => {
        if (valid) {
          submitting.value = true
          Promise.resolve(emit('submit')).then(() => {
            submitting.value = false
          }).catch((error) => {
            ElMessage.error('提交失败')
            submitting.value = false
          })
        }
      })
    }
    
    const reset = () => {
      if (formRef.value) {
        formRef.value.resetFields()
        emit('reset')
      }
    }
    
    return {
      formRef,
      submitting,
      submit,
      reset
    }
  }
}
</script>