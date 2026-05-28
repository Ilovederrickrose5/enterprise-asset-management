<template>
  <el-table :data="filteredRecords" style="width: 100%" v-loading="loading">
    <slot name="columns"></slot>
    <el-table-column label="操作" width="300" fixed="right">
      <template #default="scope">
        <template v-if="scope.row.status === 'pending' || scope.row.status === 'leader_approved' || scope.row.status === 'pending_leader' || scope.row.status === 'final_approval_created'">
          <el-button 
            type="success" 
            size="small" 
            @click="$emit('approve', scope.row)"
            :disabled="(scope.row.status !== 'leader_approved' && scope.row.status !== 'pending_leader') && scope.row.applicationType === 'DISPOSAL'"
          >批准</el-button>
          <el-button type="danger" size="small" @click="$emit('reject', scope.row)">拒绝</el-button>
          <slot name="actionButtons" :row="scope.row"></slot>
        </template>
        <template v-else>
          <el-button type="primary" size="small" @click="$emit('view', scope.row)">查看</el-button>
        </template>
      </template>
    </el-table-column>
  </el-table>
</template>

<script>
export default {
  name: 'ApprovalTable',
  props: {
    records: {
      type: Array,
      default: () => []
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    filteredRecords() {
      return this.records
    }
  },
  methods: {
  },
  emits: ['approve', 'reject', 'view']
}
</script>