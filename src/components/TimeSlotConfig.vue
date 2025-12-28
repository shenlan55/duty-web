<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const timeSlots = ref([])
const dialogVisible = ref(false)
const currentSlot = ref({
  id: null,
  name: '',
  startTime: '',
  endTime: '',
  type: 1,
  status: 1
})

// 获取时间段列表
const getTimeSlots = async () => {
  try {
    const response = await axios.get('http://localhost:8080/api/duty/time-slots')
    timeSlots.value = response.data
  } catch (error) {
    console.error('获取时间段失败:', error)
  }
}

// 打开编辑对话框
const openDialog = (slot = null) => {
  if (slot) {
    currentSlot.value = { ...slot }
  } else {
    currentSlot.value = {
      id: null,
      name: '',
      startTime: '',
      endTime: '',
      type: 1,
      status: 1
    }
  }
  dialogVisible.value = true
}

// 保存时间段
const saveTimeSlot = async () => {
  try {
    await axios.post('http://localhost:8080/api/duty/time-slots', currentSlot.value)
    dialogVisible.value = false
    getTimeSlots()
  } catch (error) {
    console.error('保存时间段失败:', error)
  }
}

// 处理状态开关变化
const handleStatusChange = async (slot) => {
  try {
    await axios.post('http://localhost:8080/api/duty/time-slots', slot)
    // 不需要重新获取整个列表，状态已经通过v-model更新
  } catch (error) {
    console.error('更新状态失败:', error)
    // 恢复原状态
    slot.status = slot.status === 1 ? 0 : 1
  }
}

onMounted(() => {
  getTimeSlots()
})
</script>

<template>
  <div class="time-slot-config">
    <h3>时间段配置</h3>
    <el-button type="primary" @click="openDialog()" style="margin-bottom: 20px;">
      添加时间段
    </el-button>
    
    <el-table :data="timeSlots" style="width: 100%">
      <el-table-column prop="id" label="ID" width="80"></el-table-column>
      <el-table-column prop="name" label="名称"></el-table-column>
      <el-table-column prop="startTime" label="开始时间"></el-table-column>
      <el-table-column prop="endTime" label="结束时间"></el-table-column>
      <el-table-column prop="type" label="类型">
        <template #default="scope">
          <el-tag v-if="scope.row.type === 1" type="success">白班</el-tag>
          <el-tag v-else-if="scope.row.type === 2" type="warning">夜班</el-tag>
          <el-tag v-else type="danger">24小时班</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态">
        <template #default="scope">
          <el-switch 
            v-model="scope.row.status" 
            :active-value="1" 
            :inactive-value="0"
            @change="handleStatusChange(scope.row)"
          ></el-switch>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="scope">
          <el-button type="primary" size="small" @click="openDialog(scope.row)">
            编辑
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <!-- 编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      title="时间段配置"
      width="500px"
    >
      <el-form :model="currentSlot" label-width="100px">
        <el-form-item label="名称">
          <el-input v-model="currentSlot.name"></el-input>
        </el-form-item>
        <el-form-item label="开始时间">
          <el-time-picker
            v-model="currentSlot.startTime"
            format="HH:mm"
            placeholder="选择开始时间"
          ></el-time-picker>
        </el-form-item>
        <el-form-item label="结束时间">
          <el-time-picker
            v-model="currentSlot.endTime"
            format="HH:mm"
            placeholder="选择结束时间"
          ></el-time-picker>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="currentSlot.type">
            <el-option label="白班" value="1"></el-option>
            <el-option label="夜班" value="2"></el-option>
            <el-option label="24小时班" value="3"></el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveTimeSlot">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.time-slot-config {
  background-color: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  width: 100% !important;
  min-width: 1000px !important;
  margin: 0 auto !important;
  box-sizing: border-box !important;
}

h3 {
  margin-bottom: 20px;
  color: #303133;
}
</style>