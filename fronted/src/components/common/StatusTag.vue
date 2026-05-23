<template>
  <el-tag :type="statusType">
    {{ statusText }}
  </el-tag>
</template>

<script>
export default {
  name: 'StatusTag',
  props: {
    status: {
      type: String,
      default: 'pending'
    },
    statusMap: {
      type: Object,
      default: () => ({
        pending: { text: '待审批', type: 'warning' },
        approved: { text: '已批准', type: 'success' },
        rejected: { text: '已拒绝', type: 'danger' },
        in_progress: { text: '维修中', type: 'info' },
        completed: { text: '已完成', type: 'success' },
        final_approval_created: { text: '已提交终审', type: 'info' },
        pending_leader: { text: '待领导审批', type: 'warning' },
        leader_approved: { text: '领导已批准', type: 'primary' }
      })
    }
  },
  computed: {
    statusText() {
      return this.statusMap[this.status]?.text || this.status
    },
    statusType() {
      return this.statusMap[this.status]?.type || 'info'
    }
  }
}
</script>