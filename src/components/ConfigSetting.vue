<script setup>
import { ref, onMounted, computed } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

// 接收父组件传递的用户信息
const props = defineProps({
  userInfo: {
    type: Object,
    default: null
  }
})

const dialogVisible = ref(false)
const configs = ref([])
const currentConfig = ref({
  id: null,
  configKey: '',
  configValue: '',
  description: ''
})

// 检查当前用户是否为管理员
const isManager = computed(() => {
  return props.userInfo?.isManager || false
})

// 获取配置列表
const getConfigs = async () => {
  try {
    const response = await axios.get('http://localhost:8080/api/duty/configs')
    configs.value = response.data
    
    // 如果没有配置，初始化默认值
    if (configs.value.length === 0) {
      initDefaultConfigs()
    }
  } catch (error) {
    console.error('获取配置列表失败:', error)
  }
}

// 初始化默认配置
const initDefaultConfigs = async () => {
  const defaultConfigs = [
    {
      configKey: 'groups',
      configValue: '[{"label":"第1大组","value":"1"},{"label":"第2大组","value":"2"},{"label":"第3大组","value":"3"}]',
      description: '大组配置'
    },
    {
      configKey: 'subGroups',
      configValue: '[{"label":"第1小组","value":"1"},{"label":"第2小组","value":"2"},{"label":"第3小组","value":"3"},{"label":"第4小组","value":"4"},{"label":"第5小组","value":"5"}]',
      description: '小组配置'
    }
  ]
  
  // 直接调用后端API保存默认配置，不通过saveConfig函数
  for (const config of defaultConfigs) {
    try {
      await axios.post('http://localhost:8080/api/duty/configs', config)
    } catch (error) {
      console.error('保存默认配置失败:', error)
    }
  }
  
  // 重新获取配置列表
  await getConfigs()
}

// 打开编辑对话框
const openDialog = (config) => {
  if (config) {
    currentConfig.value = { ...config }
  }
  dialogVisible.value = true
}

// 保存配置
const saveConfig = async () => {
  try {
    const cfg = currentConfig.value
    
    // 打印详细调试信息
    console.log('完整的cfg对象:', cfg)
    
    // 构建正确的请求对象，确保所有必填字段都有值
    const requestConfig = {
      id: cfg.id,
      configKey: cfg.configKey || cfg.config_key || '',
      configValue: cfg.configValue || cfg.config_value || '[]',
      description: cfg.description || ''
    }
    
    // 验证必填字段
    if (!requestConfig.configKey) {
      ElMessage.error('配置项不能为空')
      return
    }
    
    if (!requestConfig.configValue) {
      ElMessage.error('配置值不能为空')
      return
    }
    
    console.log('发送的请求对象:', requestConfig)
    
    await axios.post('http://localhost:8080/api/duty/configs', requestConfig)
    ElMessage.success('配置保存成功')
    getConfigs()
    dialogVisible.value = false
  } catch (error) {
    console.error('保存配置失败:', error)
    ElMessage.error('保存配置失败: ' + (error.response?.data?.message || error.message))
  }
}

onMounted(() => {
  getConfigs()
})
</script>

<template>
  <div class="config-setting">
    <h3>配置管理</h3>
    
    <el-table :data="configs" style="width: 100%">
      <el-table-column prop="id" label="ID" width="80"></el-table-column>
      <el-table-column prop="configKey" label="配置项"></el-table-column>
      <el-table-column prop="configValue" label="配置值" width="500">
        <template #default="scope">
          <div class="config-value">{{ scope.row.configValue }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述"></el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="scope">
          <el-button 
            type="primary" 
            size="small" 
            @click="openDialog(scope.row)"
            :disabled="!isManager"
          >
            编辑
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <!-- 编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      title="编辑配置"
      width="600px"
    >
      <el-form :model="currentConfig" label-width="120px">
        <el-form-item label="配置项" prop="configKey">
          <el-input v-model="currentConfig.configKey" :disabled="true"></el-input>
        </el-form-item>
        <el-form-item label="配置值" prop="configValue">
          <el-input 
            v-model="currentConfig.configValue" 
            type="textarea" 
            :rows="6"
            placeholder='JSON格式，例如: [{"label":"第1大组","value":"1"},{"label":"第2大组","value":"2"}]'
          ></el-input>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="currentConfig.description"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveConfig">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.config-setting {
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

.config-value {
  max-height: 100px;
  overflow-y: auto;
  font-family: monospace;
  font-size: 12px;
  background-color: #f5f7fa;
  padding: 8px;
  border-radius: 4px;
}
</style>