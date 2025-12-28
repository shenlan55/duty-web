<script setup>
import { ref, onMounted, computed } from 'vue'
import axios from 'axios'
import * as XLSX from 'xlsx'

const dutyPlans = ref([])
const dateRange = ref([])
const loading = ref(false)
const users = ref([])
const selectedPlanIds = ref([])
const currentView = ref('table') // 'table' 或 'calendar'
const calendarDate = ref(new Date()) // 日历当前显示的月份
const planByDate = ref({}) // 按日期分组的排班计划

// 日历详情弹窗相关
const detailDialogVisible = ref(false)
const detailDialogTitle = ref('')
const selectedDayPlans = ref([])

// 处理日历日期点击
const handleDayClick = (day) => {
  if (day.plans && day.plans.length > 0) {
    selectedDayPlans.value = day.plans
    // 确保日期格式正确，使用YYYY-MM-DD格式
    const formattedDate = day.date.split('T')[0]
    detailDialogTitle.value = `${formattedDate} 排班详情`
    detailDialogVisible.value = true
  }
}

// 计算日历显示的日期网格
const calendarDays = computed(() => {
  const year = calendarDate.value.getFullYear()
  const month = calendarDate.value.getMonth()
  
  // 获取当月第一天
  const firstDay = new Date(year, month, 1)
  // 获取当月最后一天
  const lastDay = new Date(year, month + 1, 0)
  // 获取当月第一天是星期几（0-6，0是周日）
  const startWeekDay = firstDay.getDay()
  
  // 生成日历网格数据
  const days = []
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  
  // 格式化日期为YYYY-MM-DD格式，避免时区问题
  const formatDate = (date) => {
    const y = date.getFullYear()
    const m = String(date.getMonth() + 1).padStart(2, '0')
    const d = String(date.getDate()).padStart(2, '0')
    return `${y}-${m}-${d}`
  }
  
  // 填充上月剩余日期
  const prevMonthLastDay = new Date(year, month, 0)
  for (let i = startWeekDay - 1; i >= 0; i--) {
    const day = prevMonthLastDay.getDate() - i
    const date = new Date(year, month - 1, day)
    const dateStr = formatDate(date)
    days.push({
      date: dateStr,
      day: day,
      plans: planByDate.value[dateStr] || [],
      isOtherMonth: true,
      isCurrentDay: false
    })
  }
  
  // 填充当月日期
  for (let day = 1; day <= lastDay.getDate(); day++) {
    const date = new Date(year, month, day)
    const dateStr = formatDate(date)
    const currentDay = new Date(date)
    currentDay.setHours(0, 0, 0, 0)
    
    days.push({
      date: dateStr,
      day: day,
      plans: planByDate.value[dateStr] || [],
      isOtherMonth: false,
      isCurrentDay: currentDay.getTime() === today.getTime()
    })
  }
  
  // 填充下月补充日期，使网格完整（6行7列=42天）
  const remainingDays = 42 - days.length
  for (let day = 1; day <= remainingDays; day++) {
    const date = new Date(year, month + 1, day)
    const dateStr = formatDate(date)
    days.push({
      date: dateStr,
      day: day,
      plans: planByDate.value[dateStr] || [],
      isOtherMonth: true,
      isCurrentDay: false
    })
  }
  
  return days
})

// 切换到上一月
const previousMonth = () => {
  calendarDate.value = new Date(calendarDate.value.getFullYear(), calendarDate.value.getMonth() - 1, 1)
}

// 切换到下一月
const nextMonth = () => {
  calendarDate.value = new Date(calendarDate.value.getFullYear(), calendarDate.value.getMonth() + 1, 1)
}

// 获取用户列表，用于将人员ID转换为姓名
const getUsers = async () => {
  try {
    const response = await axios.get('http://localhost:8080/api/duty/users')
    users.value = response.data
  } catch (error) {
    console.error('获取用户列表失败:', error)
  }
}

// 根据人员ID获取姓名
const getUserName = (userId) => {
  const user = users.value.find(user => user.id === userId)
  return user ? user.name : '未知'
}

// 根据人员ID获取大组名称
const getGroupName = (userId) => {
  const user = users.value.find(user => user.id === userId)
  if (!user) return '未知'
  
  switch (user.groupId) {
    case 1: return 'oncall-A组'; break
    case 2: return 'oncall-B组'; break
    case 3: return 'goc组'; break
    case 4: return 'pm组'; break
    default: return `组${user.groupId}`
  }
}

// 根据人员ID获取小组名称
const getSubGroupName = (userId) => {
  const user = users.value.find(user => user.id === userId)
  if (!user || !user.subGroupId) return ''
  return `小组${user.subGroupId}`
}

// 根据兼大组ID获取兼大组名称
const getDutyGroupName = (dutyGroupId) => {
  if (!dutyGroupId) return '无'
  
  switch (dutyGroupId) {
    case 1: return 'oncall-A组'; break
    case 2: return 'oncall-B组'; break
    case 3: return 'goc组'; break
    case 4: return 'pm组'; break
    default: return `组${dutyGroupId}`
  }
}

// 根据兼小组ID获取兼小组名称
const getDutySubGroupName = (dutySubGroupId) => {
  if (!dutySubGroupId) return '无'
  return `小组${dutySubGroupId}`
}

// 获取时间段名称
const getTimeSlotName = (plan) => {
  // 这里应该根据plan.timeSlotId从时间段配置中获取名称
  // 暂时根据类型返回默认名称
  switch (plan.type) {
    case 1: return '白班'; break
    case 2: return '夜班'; break
    case 3: return '24小时班'; break
    case 4: return '辅助'; break
    default: return '未知'
  }
}

// 根据人员ID获取角色名称
const getRoleName = (userId) => {
  const user = users.value.find(user => user.id === userId)
  if (!user) return ''
  
  if (user.isManager) return '项目经理'
  if (user.isGroupLeader) return '大组长'
  if (user.isSubGroupLeader) return '小组长'
  return ''
}

// 根据人员ID获取请假时间
const getLeaveTime = (userId) => {
  const user = users.value.find(user => user.id === userId)
  if (!user || !user.leaveStartDate || !user.leaveEndDate) return ''
  
  return `${user.leaveStartDate} 至 ${user.leaveEndDate}`
}

// 格式化排班信息显示文本
const getPlanDisplayText = (plan) => {
  const userName = getUserName(plan.userId)
  const groupName = getGroupName(plan.userId)
  const subGroupName = getSubGroupName(plan.userId)
  const roleName = getRoleName(plan.userId)
  const timeSlotName = getTimeSlotName(plan)
  
  return `${timeSlotName}: ${userName} (${groupName}${subGroupName ? '-' + subGroupName : ''}${roleName ? ', ' + roleName : ''})`
}

// 获取排班计划
const getDutyPlans = async () => {
  if (dateRange.value.length !== 2) {
    return
  }
  
  loading.value = true
  
  try {
    const startDate = dateRange.value[0]
    const endDate = dateRange.value[1]
    
    const response = await axios.get('http://localhost:8080/api/duty/plans', {
      params: {
        startDate: startDate,
        endDate: endDate
      }
    })
    
    dutyPlans.value = response.data
    selectedPlanIds.value = [] // 清空选择
    
    // 按日期分组，用于日历视图
    planByDate.value = dutyPlans.value.reduce((acc, plan) => {
      // 直接使用plan.date字符串作为日期键，避免时区转换问题
      // 如果plan.date是字符串，直接取前10位；如果是Date对象，则转换为ISO字符串
      const dateKey = typeof plan.date === 'string' ? 
        plan.date.substring(0, 10) : 
        plan.date.toISOString().split('T')[0];
      if (!acc[dateKey]) {
        acc[dateKey] = [];
      }
      acc[dateKey].push(plan);
      return acc;
    }, {});
  } catch (error) {
    console.error('获取排班计划失败:', error)
  } finally {
    loading.value = false
  }
}

// 处理选择变化
const handleSelectionChange = (selection) => {
  selectedPlanIds.value = selection.map(plan => plan.id)
}

// 批量删除排班计划
const batchDeletePlans = async () => {
  if (selectedPlanIds.value.length === 0) {
    return
  }
  
  loading.value = true
  
  try {
    await axios.delete('http://localhost:8080/api/duty/plans', {
      data: selectedPlanIds.value
    })
    
    // 重新获取排班计划
    await getDutyPlans()
  } catch (error) {
    console.error('批量删除排班计划失败:', error)
  } finally {
    loading.value = false
  }
}

// 导出查询数据
const exportPlans = () => {
  if (dutyPlans.value.length === 0) {
    return
  }
  
  // 准备导出数据 - 与页面展示列一致
  const exportData = dutyPlans.value.map(plan => {
    return {
      '日期': new Date(plan.date).toLocaleDateString(),
      '状态': getTimeSlotName(plan),
      '人员姓名': getUserName(plan.userId),
      '大组': getGroupName(plan.userId),
      '小组': getSubGroupName(plan.userId) || '-',
      '角色': getRoleName(plan.userId) || '-',
      '兼大组': getDutyGroupName(plan.dutyGroupId),
      '兼小组': getDutySubGroupName(plan.dutySubGroupId),
      '请假时间': getLeaveTime(plan.userId)
    }
  })
  
  // 创建工作簿和工作表
  const worksheet = XLSX.utils.json_to_sheet(exportData)
  const workbook = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(workbook, worksheet, '排班计划')
  
  // 导出文件
  XLSX.writeFile(workbook, `排班计划_${new Date().toISOString().split('T')[0]}.xlsx`)
}

onMounted(async () => {
  // 获取用户列表
  await getUsers()
  
  // 默认查询本周的排班
  const now = new Date()
  const startOfWeek = new Date(now.setDate(now.getDate() - now.getDay()))
  const endOfWeek = new Date(now.setDate(now.getDate() + 6))
  
  // 格式化日期为YYYY-MM-DD格式的字符串
  const formatDate = (date) => {
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  }
  
  dateRange.value = [formatDate(startOfWeek), formatDate(endOfWeek)]
  getDutyPlans()
})
</script>

<template>
  <div class="duty-plan">
    <h3>排班计划</h3>
    
    <div class="plan-header">
      <el-form label-width="100px" inline>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 300px;"
          ></el-date-picker>
        </el-form-item>
        
        <el-form-item>
          <el-button 
            type="primary" 
            @click="getDutyPlans" 
            :loading="loading"
          >
            查询
          </el-button>
        </el-form-item>
        
        <el-form-item>
          <el-button 
            type="success" 
            @click="exportPlans" 
            :loading="loading"
            :disabled="dutyPlans.length === 0"
          >
            导出查询数据
          </el-button>
        </el-form-item>
      </el-form>
    </div>
    
    <div class="view-tabs" style="margin-bottom: 20px;">
      <el-tabs v-model="currentView" type="card">
        <el-tab-pane label="表格视图" name="table"></el-tab-pane>
        <el-tab-pane label="日历视图" name="calendar"></el-tab-pane>
      </el-tabs>
    </div>
    
    <div v-if="currentView === 'table'" class="plan-action" style="margin-bottom: 10px;">
      <el-button 
        type="danger" 
        @click="batchDeletePlans" 
        :loading="loading"
        :disabled="selectedPlanIds.length === 0"
      >
        批量删除选中计划
      </el-button>
    </div>
    
    <!-- 表格视图 -->
    <el-table 
      v-if="currentView === 'table'"
      :data="dutyPlans" 
      style="width: 100%" 
      :loading="loading"
      @selection-change="handleSelectionChange"
      fit
      stripe
      border
    >
      <el-table-column type="selection" width="55" class-name="el-table-column--selection"></el-table-column>
      <el-table-column prop="date" label="日期" width="120" class-name="el-table-column--date">
        <template #default="scope">
          <!-- 直接使用日期字符串的前10位，避免时区转换问题 -->
          {{ typeof scope.row.date === 'string' ? scope.row.date.substring(0, 10) : new Date(scope.row.date).toLocaleDateString() }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="120" class-name="el-table-column--status">
        <template #default="scope">
          <el-tag 
            :type="scope.row.type === 1 ? 'success' : scope.row.type === 2 ? 'warning' : scope.row.type === 3 ? 'danger' : 'info'"
          >
            {{ getTimeSlotName(scope.row) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="人员姓名" width="120" class-name="el-table-column--name">
        <template #default="scope">
          {{ getUserName(scope.row.userId) }}
        </template>
      </el-table-column>
      <el-table-column label="大组" width="120" class-name="el-table-column--group">
        <template #default="scope">
          {{ getGroupName(scope.row.userId) }}
        </template>
      </el-table-column>
      <el-table-column label="小组" width="100" class-name="el-table-column--subgroup">
        <template #default="scope">
          {{ getSubGroupName(scope.row.userId) || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="角色" width="120" class-name="el-table-column--role">
        <template #default="scope">
          {{ getRoleName(scope.row.userId) || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="兼大组" width="120" class-name="el-table-column--duty-group">
        <template #default="scope">
          {{ getDutyGroupName(scope.row.dutyGroupId) }}
        </template>
      </el-table-column>
      <el-table-column label="兼小组" width="100" class-name="el-table-column--duty-subgroup">
        <template #default="scope">
          {{ getDutySubGroupName(scope.row.dutySubGroupId) }}
        </template>
      </el-table-column>
    </el-table>
    
    <div v-if="currentView === 'table' && dutyPlans.length === 0 && !loading" class="empty-tip">
      <el-empty description="暂无排班记录"></el-empty>
    </div>
    
    <!-- 日历视图 -->
    <div v-else-if="currentView === 'calendar'" class="calendar-container">
      <div class="calendar-header">
        <el-button type="text" @click="previousMonth" size="small">上一月</el-button>
        <span class="calendar-title">{{ calendarDate.getFullYear() }}年{{ calendarDate.getMonth() + 1 }}月</span>
        <el-button type="text" @click="nextMonth" size="small">下一月</el-button>
      </div>
      <div class="calendar-grid">
        <!-- 星期标题 -->
        <div class="calendar-weekdays">
          <div class="weekday">日</div>
          <div class="weekday">一</div>
          <div class="weekday">二</div>
          <div class="weekday">三</div>
          <div class="weekday">四</div>
          <div class="weekday">五</div>
          <div class="weekday">六</div>
        </div>
        <!-- 日期网格 -->
        <div class="calendar-days">
          <!-- 生成日历网格 -->
          <div 
            v-for="day in calendarDays" 
            :key="day.date"
            :class="['calendar-day', { 'other-month': day.isOtherMonth, 'current-day': day.isCurrentDay }]"
            @click="handleDayClick(day)"
          >
            <div class="day-number">{{ day.day }}</div>
            <div class="day-plans" v-if="day.plans && day.plans.length > 0">
              <div 
                v-for="plan in day.plans" 
                :key="plan.id"
                class="plan-item"
              >
                <el-tag 
                  :type="plan.type === 1 ? 'success' : plan.type === 2 ? 'warning' : plan.type === 3 ? 'danger' : 'info'"
                  size="small"
                  class="plan-tag"
                >
                  {{ getPlanDisplayText(plan) }}
                </el-tag>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 日历详情弹窗 -->
    <el-dialog
      v-model="detailDialogVisible"
      :title="detailDialogTitle"
      width="80%"
      :max-width="'90%'"
      class="detail-dialog"
    >
      <el-table 
        :data="selectedDayPlans" 
        style="width: 100%" 
        stripe
        border
        :max-height="'60vh'"
      >
        <el-table-column type="index" label="序号" width="60"></el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="scope">
            <el-tag 
              :type="scope.row.type === 1 ? 'success' : scope.row.type === 2 ? 'warning' : scope.row.type === 3 ? 'danger' : 'info'"
            >
              {{ getTimeSlotName(scope.row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="人员姓名" width="120">
          <template #default="scope">
            {{ getUserName(scope.row.userId) }}
          </template>
        </el-table-column>
        <el-table-column label="大组" width="120">
          <template #default="scope">
            {{ getGroupName(scope.row.userId) }}
          </template>
        </el-table-column>
        <el-table-column label="小组" width="100">
          <template #default="scope">
            {{ getSubGroupName(scope.row.userId) || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="角色" width="120">
          <template #default="scope">
            {{ getRoleName(scope.row.userId) || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="兼大组" width="120">
          <template #default="scope">
            {{ getDutyGroupName(scope.row.dutyGroupId) }}
          </template>
        </el-table-column>
        <el-table-column label="兼小组" width="100">
          <template #default="scope">
            {{ getDutySubGroupName(scope.row.dutySubGroupId) }}
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="detailDialogVisible = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.duty-plan {
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

.plan-header {
  margin-bottom: 20px;
}

.view-tabs {
  margin-bottom: 20px;
}

.empty-tip {
  margin-top: 50px;
  text-align: center;
}

/* 表格样式调整 */
:deep(.el-table) {
  width: 100%;
  border-collapse: collapse;
}

/* 确保表头和表体宽度一致 */
:deep(.el-table__header-wrapper) {
  width: 100%;
}

:deep(.el-table__body-wrapper) {
  width: 100%;
  overflow-x: auto;
}

/* 强制表格列宽一致 */
:deep(.el-table__header) {
  table-layout: fixed;
  width: 100% !important;
}

:deep(.el-table__body) {
  table-layout: fixed;
  width: 100% !important;
}

/* 表头样式 */
:deep(.el-table th) {
  text-align: center;
  font-weight: bold;
  background-color: #f5f7fa;
  padding: 12px 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 表体样式 */
:deep(.el-table td) {
  text-align: center;
  vertical-align: middle;
  padding: 12px 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 选择列宽度 */
:deep(.el-table-column--selection) {
  width: 55px;
}

/* 日期列宽度 */
:deep(.el-table-column--date) {
  width: 120px;
}

/* 状态列宽度 */
:deep(.el-table-column--status) {
  width: 120px;
}

/* 姓名列宽度 */
:deep(.el-table-column--name) {
  width: 120px;
}

/* 大组列宽度 */
:deep(.el-table-column--group) {
  width: 120px;
}

/* 小组列宽度 */
:deep(.el-table-column--subgroup) {
  width: 100px;
}

/* 角色列宽度 */
:deep(.el-table-column--role) {
  width: 120px;
}

/* 兼大组列宽度 */
:deep(.el-table-column--duty-group) {
  width: 120px;
}

/* 兼小组列宽度 */
:deep(.el-table-column--duty-subgroup) {
  width: 100px;
}

/* 自定义日历样式 */
.calendar-container {
  margin-top: 20px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 16px;
}

.calendar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e4e7ed;
}

.calendar-title {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.calendar-grid {
  width: 100%;
}

.calendar-weekdays {
  display: flex;
  margin-bottom: 8px;
  background-color: #f5f7fa;
  border-radius: 4px;
  overflow: hidden;
}

.weekday {
  flex: 1;
  padding: 8px;
  text-align: center;
  font-weight: bold;
  font-size: 14px;
  color: #606266;
  border-right: 1px solid #e4e7ed;
}

.weekday:last-child {
  border-right: none;
}

.calendar-days {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 2px;
}

.calendar-day {
  background-color: #ffffff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 8px;
  min-height: 100px;
  display: flex;
  flex-direction: column;
  transition: all 0.2s ease;
  cursor: pointer;
}

.calendar-day:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  background-color: #f0f9ff;
  border-color: #91d5ff;
}

.calendar-day.other-month {
  background-color: #fafafa;
  opacity: 0.6;
}

.calendar-day.current-day {
  background-color: #ecf5ff;
  border-color: #d9ecff;
}

.day-number {
  font-weight: bold;
  font-size: 14px;
  margin-bottom: 6px;
  color: #303133;
  width: 100%;
  text-align: left;
}

.day-plans {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
  overflow-y: auto;
  max-height: 80px;
}

.day-plans::-webkit-scrollbar {
  width: 2px;
}

.day-plans::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.day-plans::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 1px;
}

.plan-item {
  width: 100%;
  margin-bottom: 2px;
}

.plan-tag {
  padding: 2px 4px;
  font-size: 11px;
  line-height: 14px;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  border-radius: 2px;
  display: inline-block;
  width: 100%;
  box-sizing: border-box;
}

/* 日历详情弹窗样式 */
.detail-dialog :deep(.el-dialog__body) {
  max-height: 70vh;
  overflow-y: auto;
  padding: 16px;
}

.detail-dialog :deep(.el-dialog__header) {
  border-bottom: 1px solid #e4e7ed;
  padding: 16px;
}

.detail-dialog :deep(.el-dialog__footer) {
  border-top: 1px solid #e4e7ed;
  padding: 12px 16px;
  text-align: right;
}

.detail-dialog :deep(.el-table) {
  width: 100%;
  border-collapse: collapse;
}

.detail-dialog :deep(.el-table__header-wrapper) {
  width: 100%;
}

.detail-dialog :deep(.el-table__body-wrapper) {
  width: 100%;
  overflow-x: auto;
}

/* 强制表格列宽一致 */
.detail-dialog :deep(.el-table__header) {
  table-layout: fixed;
  width: 100% !important;
}

.detail-dialog :deep(.el-table__body) {
  table-layout: fixed;
  width: 100% !important;
}

/* 响应式调整 */
@media (max-width: 1200px) {
  .calendar-day {
    min-height: 80px;
  }
  
  .day-plans {
    max-height: 60px;
  }
  
  .plan-tag {
    font-size: 10px;
    padding: 1px 3px;
  }
  
  .detail-dialog {
    width: 90% !important;
  }
}

@media (max-width: 900px) {
  .calendar-day {
    min-height: 70px;
    padding: 6px;
  }
  
  .day-plans {
    max-height: 50px;
  }
  
  .plan-tag {
    font-size: 9px;
  }
  
  .detail-dialog {
    width: 95% !important;
  }
}
</style>