<script setup>
import { ref, onMounted, onActivated, computed } from 'vue'
import axios from 'axios'

const users = ref([])
const dialogVisible = ref(false)
const batchDialogVisible = ref(false)
const batchAddDialogVisible = ref(false)
const currentUser = ref({
  id: null,
  name: '',
  groupId: 1,
  subGroupId: 1,
  isManager: false,
  isGroupLeader: false,
  isSubGroupLeader: false,
  status: 1,
  leaveDateRange: [],
  leaveStartDate: '',
  leaveEndDate: ''
})
const batchUser = ref({
  groupId: '',
  subGroupId: '',
  isManager: false,
  isGroupLeader: false,
  isSubGroupLeader: false,
  status: '',
  leaveDateRange: [],
  leaveStartDate: '',
  leaveEndDate: ''
})

// 用于批量添加的人员列表
const batchAddUsers = ref([
  {
    name: '',
    groupId: '',
    subGroupId: '',
    isManager: false,
    isGroupLeader: false,
    isSubGroupLeader: false,
    status: 1,
    leaveDateRange: [],
    leaveStartDate: '',
    leaveEndDate: ''
  }
])

// 用于批量操作的选中用户ID数组
const selectedUserIds = ref([])

// 大组和小组配置选项
const groups = ref([
  { label: '第1大组', value: '1' },
  { label: '第2大组', value: '2' },
  { label: '第3大组', value: '3' }
])

const subGroups = ref([
  { label: '第1小组', value: '1' },
  { label: '第2小组', value: '2' },
  { label: '第3小组', value: '3' },
  { label: '第4小组', value: '4' },
  { label: '第5小组', value: '5' }
])

// 根据groupId获取大组名称
const getGroupName = (groupId) => {
  const group = groups.value.find(item => item.value === String(groupId))
  return group ? group.label : `第${groupId}大组`
}

// 根据subGroupId获取小组名称
const getSubGroupName = (subGroupId) => {
  if (!subGroupId) return '无'
  const subGroup = subGroups.value.find(item => item.value === String(subGroupId))
  return subGroup ? subGroup.label : `第${subGroupId}小组`
}

// 获取角色名称列表
const getRoleNames = (user) => {
  const roles = []
  if (user.isManager) roles.push('项目经理')
  if (user.isGroupLeader) roles.push('大组长')
  if (user.isSubGroupLeader) roles.push('小组长')
  return roles
}

// 角色复选框组的计算属性，用于双向绑定
const roleCheckboxGroup = computed({
  get() {
    const roles = []
    if (currentUser.value.isManager) roles.push('isManager')
    if (currentUser.value.isGroupLeader) roles.push('isGroupLeader')
    if (currentUser.value.isSubGroupLeader) roles.push('isSubGroupLeader')
    return roles
  },
  set(val) {
    currentUser.value.isManager = val.includes('isManager')
    currentUser.value.isGroupLeader = val.includes('isGroupLeader')
    currentUser.value.isSubGroupLeader = val.includes('isSubGroupLeader')
  }
})

// 判断当前用户是否需要选择小组
const needSubGroup = computed(() => {
  // 项目经理和大组长不需要选择小组
  return !currentUser.value.isManager && !currentUser.value.isGroupLeader
})

// 批量编辑时判断是否需要显示小组选择
const batchNeedSubGroup = computed(() => {
  // 批量编辑时，如果勾选了项目经理或大组长，则不需要显示小组选择
  return !batchUser.value.isManager && !batchUser.value.isGroupLeader
})

// 批量角色复选框组的计算属性，用于双向绑定
const batchRoleCheckboxGroup = computed({
  get() {
    const roles = []
    if (batchUser.value.isManager) roles.push('isManager')
    if (batchUser.value.isGroupLeader) roles.push('isGroupLeader')
    if (batchUser.value.isSubGroupLeader) roles.push('isSubGroupLeader')
    return roles
  },
  set(val) {
    batchUser.value.isManager = val.includes('isManager')
    batchUser.value.isGroupLeader = val.includes('isGroupLeader')
    batchUser.value.isSubGroupLeader = val.includes('isSubGroupLeader')
  }
})

// 获取配置值
const getConfig = async (configKey) => {
  try {
    console.log(`开始获取${configKey}配置...`)
    // 直接获取所有配置，然后筛选出需要的配置项
    // 这样可以确保获取到最新的配置
    const response = await axios.get('http://localhost:8080/api/duty/configs')
    console.log(`获取到所有配置:`, response.data)
    const config = response.data.find(item => item.configKey === configKey)
    if (config) {
      console.log(`找到${configKey}配置:`, config.configValue)
      return config.configValue
    } else {
      console.error(`未找到${configKey}配置`)
      return null
    }
  } catch (error) {
    console.error(`获取${configKey}配置失败:`, error)
    return null
  }
}

// 加载大组和小组配置
const loadConfigs = async () => {
  console.log('开始加载配置...')
  
  const groupsConfig = await getConfig('groups')
  console.log('获取到的groups配置:', groupsConfig)
  if (groupsConfig) {
    try {
      const parsedGroups = JSON.parse(groupsConfig)
      console.log('解析后的groups配置:', parsedGroups)
      groups.value = parsedGroups
    } catch (e) {
      console.error('解析大组配置失败:', e)
    }
  } else {
    console.error('获取大组配置失败，使用默认值')
  }
  
  const subGroupsConfig = await getConfig('subGroups')
  console.log('获取到的subGroups配置:', subGroupsConfig)
  if (subGroupsConfig) {
    try {
      const parsedSubGroups = JSON.parse(subGroupsConfig)
      console.log('解析后的subGroups配置:', parsedSubGroups)
      subGroups.value = parsedSubGroups
    } catch (e) {
      console.error('解析小组配置失败:', e)
    }
  } else {
    console.error('获取小组配置失败，使用默认值')
  }
  
  console.log('配置加载完成，当前groups:', groups.value)
  console.log('配置加载完成，当前subGroups:', subGroups.value)
}

// 获取人员列表
const getUsers = async () => {
  try {
    const response = await axios.get('http://localhost:8080/api/duty/users')
    users.value = response.data
  } catch (error) {
    console.error('获取人员列表失败:', error)
  }
}

// 打开编辑对话框
const openDialog = (user = null) => {
  if (user) {
    currentUser.value = {
      ...user,
      leaveDateRange: user.leaveStartDate && user.leaveEndDate ? [user.leaveStartDate, user.leaveEndDate] : []
    }
  } else {
    currentUser.value = {
      id: null,
      name: '',
      groupId: groups.value.length > 0 ? groups.value[0].value : 1,
      subGroupId: subGroups.value.length > 0 ? subGroups.value[0].value : 1,
      isManager: false,
      isGroupLeader: false,
      isSubGroupLeader: false,
      status: 1,
      leaveDateRange: [],
      leaveStartDate: '',
      leaveEndDate: ''
    }
  }
  dialogVisible.value = true
}

// 保存人员信息
const saveUser = async () => {
  try {
    const userToSave = { ...currentUser.value }
    
    // 处理请假时间
    if (userToSave.status === 1 || userToSave.status === '1') {
      // 状态为正常，清空请假时间
      userToSave.leaveStartDate = ''
      userToSave.leaveEndDate = ''
    } else if (userToSave.leaveDateRange && userToSave.leaveDateRange.length === 2) {
      userToSave.leaveStartDate = userToSave.leaveDateRange[0]
      userToSave.leaveEndDate = userToSave.leaveDateRange[1]
    } else {
      userToSave.leaveStartDate = ''
      userToSave.leaveEndDate = ''
    }
    
    // 删除临时字段
    delete userToSave.leaveDateRange
    
    // 项目经理和大组长不需要小组
    if (userToSave.isManager || userToSave.isGroupLeader) {
      userToSave.subGroupId = null
    }
    
    if (userToSave.id) {
      await axios.post('http://localhost:8080/api/duty/users', userToSave)
    } else {
      await axios.post('http://localhost:8080/api/duty/users', userToSave)
    }
    dialogVisible.value = false
    getUsers()
  } catch (error) {
    console.error('保存人员信息失败:', error)
  }
}

// 批量删除人员
const batchDeleteUsers = async () => {
  if (selectedUserIds.value.length === 0) {
    return
  }
  
  try {
    // 这里应该调用批量删除的API，目前后端没有提供，所以暂时循环删除
    for (const userId of selectedUserIds.value) {
      await axios.delete(`http://localhost:8080/api/duty/users/${userId}`)
    }
    getUsers()
    selectedUserIds.value = []
  } catch (error) {
    console.error('批量删除人员失败:', error)
  }
}

// 打开批量编辑对话框
const openBatchDialog = () => {
  if (selectedUserIds.value.length === 0) {
    return
  }
  
  // 重置批量编辑表单
  batchUser.value = {
    groupId: '',
    subGroupId: '',
    isManager: false,
    isGroupLeader: false,
    isSubGroupLeader: false,
    status: '',
    leaveStartDate: '',
    leaveEndDate: ''
  }
  batchDialogVisible.value = true
}

// 批量编辑人员
const batchEditUsers = async () => {
  if (selectedUserIds.value.length === 0) {
    return
  }
  
  try {
    // 处理请假时间
    const batchLeaveStartDate = batchUser.value.leaveDateRange && batchUser.value.leaveDateRange.length === 2 ? batchUser.value.leaveDateRange[0] : null
    const batchLeaveEndDate = batchUser.value.leaveDateRange && batchUser.value.leaveDateRange.length === 2 ? batchUser.value.leaveDateRange[1] : null
    
    // 这里应该调用批量编辑的API，目前后端没有提供，所以暂时循环更新
    for (const userId of selectedUserIds.value) {
      // 获取当前用户信息
      const user = users.value.find(u => u.id === userId)
      if (user) {
        // 只更新有修改的字段
        const updatedUser = {
          ...user,
          groupId: batchUser.value.groupId || user.groupId,
          subGroupId: batchUser.value.subGroupId || user.subGroupId,
          isManager: batchUser.value.isManager !== undefined ? batchUser.value.isManager : user.isManager,
          isGroupLeader: batchUser.value.isGroupLeader !== undefined ? batchUser.value.isGroupLeader : user.isGroupLeader,
          isSubGroupLeader: batchUser.value.isSubGroupLeader !== undefined ? batchUser.value.isSubGroupLeader : user.isSubGroupLeader,
          status: batchUser.value.status !== '' ? batchUser.value.status : user.status
        }
        
        // 处理请假时间
        // 如果状态为正常，清空请假时间
        if (updatedUser.status === 1 || updatedUser.status === '1') {
          updatedUser.leaveStartDate = ''
          updatedUser.leaveEndDate = ''
        } else if (batchLeaveStartDate && batchLeaveEndDate) {
          updatedUser.leaveStartDate = batchLeaveStartDate
          updatedUser.leaveEndDate = batchLeaveEndDate
        }
        
        // 项目经理和大组长不需要小组
        if (updatedUser.isManager || updatedUser.isGroupLeader) {
          updatedUser.subGroupId = null
        }
        await axios.post('http://localhost:8080/api/duty/users', updatedUser)
      }
    }
    batchDialogVisible.value = false
    getUsers()
    selectedUserIds.value = []
  } catch (error) {
    console.error('批量编辑人员失败:', error)
  }
}

// 处理复选框选择变化
const handleSelectionChange = (selection) => {
  selectedUserIds.value = selection.map(user => user.id)
}

// 打开批量添加对话框
const openBatchAddDialog = () => {
  // 重置批量添加列表
  batchAddUsers.value = [
    {
      name: '',
      groupId: '',
      subGroupId: '',
      isManager: false,
      isGroupLeader: false,
      isSubGroupLeader: false,
      status: 1,
      leaveStartDate: '',
      leaveEndDate: ''
    }
  ]
  batchAddDialogVisible.value = true
}

// 添加新的人员行
const addUserRow = () => {
  batchAddUsers.value.push({
    name: '',
    groupId: '',
    subGroupId: '',
    isManager: false,
    isGroupLeader: false,
    isSubGroupLeader: false,
    status: 1,
    leaveStartDate: '',
    leaveEndDate: ''
  })
}

// 删除人员行
const removeUserRow = (index) => {
  if (batchAddUsers.value.length > 1) {
    batchAddUsers.value.splice(index, 1)
  }
}

// 保存批量添加的人员
const saveBatchAddUsers = async () => {
  try {
    // 过滤掉姓名为空的人员
    const validUsers = batchAddUsers.value.filter(user => user.name.trim() !== '')
    
    if (validUsers.length === 0) {
      return
    }
    
    // 批量保存人员
    for (const user of validUsers) {
      const userToSave = { ...user }
      
      // 处理请假时间
      if (userToSave.leaveDateRange && userToSave.leaveDateRange.length === 2) {
        userToSave.leaveStartDate = userToSave.leaveDateRange[0]
        userToSave.leaveEndDate = userToSave.leaveDateRange[1]
      } else {
        userToSave.leaveStartDate = ''
        userToSave.leaveEndDate = ''
      }
      
      // 删除临时字段
      delete userToSave.leaveDateRange
      
      // 项目经理和大组长不需要小组
      if (userToSave.isManager || userToSave.isGroupLeader) {
        userToSave.subGroupId = null
      }
      
      await axios.post('http://localhost:8080/api/duty/users', userToSave)
    }
    
    batchAddDialogVisible.value = false
    getUsers()
  } catch (error) {
    console.error('批量添加人员失败:', error)
  }
}

onMounted(async () => {
  await loadConfigs()
  getUsers()
})

// 当组件激活时重新加载配置，确保下拉选项反映最新配置
const refreshConfigs = async () => {
  await loadConfigs()
}

// 监听组件激活事件（如果使用了keep-alive）
onActivated(async () => {
  await loadConfigs()
})
</script>

<template>
  <div class="user-config">
    <h3>人员配置</h3>
    <div style="margin-bottom: 20px;">
      <el-button type="primary" @click="openDialog()">
        添加人员
      </el-button>
      <el-button type="success" @click="openBatchAddDialog()">
        批量添加
      </el-button>
      <el-button type="danger" @click="batchDeleteUsers()" :disabled="selectedUserIds.length === 0">
        批量删除
      </el-button>
      <el-button type="warning" @click="openBatchDialog()" :disabled="selectedUserIds.length === 0">
        批量编辑
      </el-button>
    </div>
    
    <el-table 
      :data="users" 
      style="width: 100%"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55"></el-table-column>
      <el-table-column prop="id" label="ID" width="80"></el-table-column>
      <el-table-column prop="name" label="姓名"></el-table-column>
      <el-table-column prop="groupId" label="大组">
        <template #default="scope">
          {{ getGroupName(scope.row.groupId) }}
        </template>
      </el-table-column>
      <el-table-column prop="subGroupId" label="小组">
        <template #default="scope">
          {{ getSubGroupName(scope.row.subGroupId) }}
        </template>
      </el-table-column>
      <el-table-column label="角色" width="200">
        <template #default="scope">
          {{ getRoleNames(scope.row).join('、') || '无' }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态">
        <template #default="scope">
          <el-tag v-if="scope.row.status === 1" type="success">正常</el-tag>
          <el-tag v-else type="danger">请假</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="请假时间" width="200">
        <template #default="scope">
          {{ scope.row.status === 0 ? (scope.row.leaveStartDate && scope.row.leaveEndDate ? `${scope.row.leaveStartDate} 至 ${scope.row.leaveEndDate}` : '未设置') : '-' }}
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
      title="人员配置"
      width="600px"
    >
      <el-form :model="currentUser" label-width="100px">
        <el-form-item label="姓名">
          <el-input v-model="currentUser.name"></el-input>
        </el-form-item>
        <el-form-item label="大组">
          <el-select v-model="currentUser.groupId">
            <el-option 
              v-for="group in groups" 
              :key="group.value" 
              :label="group.label" 
              :value="group.value">
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="小组" v-if="needSubGroup">
          <el-select v-model="currentUser.subGroupId" placeholder="请选择小组">
            <el-option 
              v-for="subGroup in subGroups" 
              :key="subGroup.value" 
              :label="subGroup.label" 
              :value="subGroup.value">
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-checkbox-group v-model="roleCheckboxGroup">
            <el-checkbox label="isManager">项目经理</el-checkbox>
            <el-checkbox label="isGroupLeader">大组长</el-checkbox>
            <el-checkbox label="isSubGroupLeader">小组长</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="currentUser.status">
            <el-option label="正常" value="1"></el-option>
            <el-option label="请假" value="0"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="请假时间" v-if="currentUser.status === '0' || currentUser.status === 0">
          <el-date-picker
            v-model="currentUser.leaveDateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 100%;"
          ></el-date-picker>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveUser">保存</el-button>
        </span>
      </template>
    </el-dialog>
    
    <!-- 批量编辑对话框 -->
    <el-dialog
      v-model="batchDialogVisible"
      title="批量编辑人员"
      width="600px"
    >
      <el-form :model="batchUser" label-width="100px">
        <el-form-item label="大组">
          <el-select v-model="batchUser.groupId" placeholder="保持不变">
            <el-option 
              v-for="group in groups" 
              :key="group.value" 
              :label="group.label" 
              :value="group.value">
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="小组" v-if="batchNeedSubGroup">
          <el-select v-model="batchUser.subGroupId" placeholder="保持不变">
            <el-option 
              v-for="subGroup in subGroups" 
              :key="subGroup.value" 
              :label="subGroup.label" 
              :value="subGroup.value">
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-checkbox-group v-model="batchRoleCheckboxGroup">
            <el-checkbox label="isManager">项目经理</el-checkbox>
            <el-checkbox label="isGroupLeader">大组长</el-checkbox>
            <el-checkbox label="isSubGroupLeader">小组长</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="batchUser.status" placeholder="保持不变">
            <el-option label="正常" value="1"></el-option>
            <el-option label="请假" value="0"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="请假时间" v-if="batchUser.status === '0' || batchUser.status === 0">
          <el-date-picker
            v-model="batchUser.leaveDateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 100%;"
          ></el-date-picker>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="batchDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="batchEditUsers">保存</el-button>
        </span>
      </template>
    </el-dialog>
    
    <!-- 批量添加对话框 -->
    <el-dialog
      v-model="batchAddDialogVisible"
      title="批量添加人员"
      width="800px"
      :before-close="() => batchAddDialogVisible = false"
    >
      <div class="batch-add-container">
        <div class="batch-add-header">
          <div class="header-item header-item-wide">姓名</div>
          <div class="header-item header-item-wide">大组</div>
          <div class="header-item header-item-wide">小组</div>
          <div class="header-item">项目经理</div>
          <div class="header-item">大组长</div>
          <div class="header-item">小组长</div>
          <div class="header-item">状态</div>
          <div class="header-item header-item-wide">请假时间</div>
          <div class="header-item">操作</div>
        </div>
        
        <div class="batch-add-content">
          <div 
            v-for="(user, index) in batchAddUsers" 
            :key="index" 
            class="batch-add-row"
          >
            <div class="row-item row-item-wide">
              <el-input v-model="user.name" placeholder="请输入姓名"></el-input>
            </div>
            <div class="row-item row-item-wide">
              <el-select v-model="user.groupId" placeholder="选择大组">
                <el-option 
                  v-for="group in groups" 
                  :key="group.value" 
                  :label="group.label" 
                  :value="group.value">
                </el-option>
              </el-select>
            </div>
            <div class="row-item row-item-wide" v-if="!user.isManager && !user.isGroupLeader">
              <el-select v-model="user.subGroupId" placeholder="选择小组">
                <el-option 
                  v-for="subGroup in subGroups" 
                  :key="subGroup.value" 
                  :label="subGroup.label" 
                  :value="subGroup.value">
                </el-option>
              </el-select>
            </div>
            <div class="row-item">
              <el-switch v-model="user.isManager"></el-switch>
            </div>
            <div class="row-item">
              <el-switch v-model="user.isGroupLeader"></el-switch>
            </div>
            <div class="row-item">
              <el-switch v-model="user.isSubGroupLeader"></el-switch>
            </div>
            <div class="row-item">
              <el-select v-model="user.status">
                <el-option label="正常" value="1"></el-option>
                <el-option label="请假" value="0"></el-option>
              </el-select>
            </div>
            <div class="row-item row-item-wide" v-if="user.status === '0' || user.status === 0">
              <el-date-picker
                v-model="user.leaveDateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                style="width: 100%;"
              ></el-date-picker>
            </div>
            <div class="row-item row-item-wide" v-else>
              <span style="color: #c0c4cc;">--</span>
            </div>
            <div class="row-item">
              <el-button 
                type="danger" 
                size="small" 
                @click="removeUserRow(index)"
                :disabled="batchAddUsers.length <= 1"
              >
                删除
              </el-button>
            </div>
          </div>
        </div>
        
        <div class="batch-add-footer">
          <el-button type="primary" @click="addUserRow">
            添加一行
          </el-button>
        </div>
      </div>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="batchAddDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveBatchAddUsers">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.user-config {
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



/* 批量添加样式 */
.batch-add-container {
  width: 100%;
}

.batch-add-header {
  display: flex;
  background-color: #f5f7fa;
  padding: 10px;
  border-radius: 4px;
  margin-bottom: 10px;
  font-weight: bold;
}

.header-item {
  flex: 1;
  text-align: center;
  font-size: 14px;
}

/* 加宽姓名、大组、小组列 */
.header-item-wide {
  flex: 2;
}

.batch-add-content {
  max-height: 400px;
  overflow-y: auto;
  margin-bottom: 10px;
}

.batch-add-row {
  display: flex;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #ebeef5;
}

.row-item {
  flex: 1;
  text-align: center;
}

/* 加宽姓名、大组、小组列 */
.row-item-wide {
  flex: 2;
}

.batch-add-footer {
  margin-top: 10px;
  text-align: right;
}

/* 调整输入框大小 */
.row-item .el-input,
.row-item .el-select {
  width: 90%;
}

/* 加宽列的输入框 */
.row-item-wide .el-input,
.row-item-wide .el-select {
  width: 95%;
}

/* 最后一列操作按钮调整 */
.row-item:last-child {
  display: flex;
  justify-content: center;
}
</style>