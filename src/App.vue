<script setup>
import { ref, onMounted } from 'vue'
import TimeSlotConfig from './components/TimeSlotConfig.vue'
import UserConfig from './components/UserConfig.vue'
import DutyGenerate from './components/DutyGenerate.vue'
import DutyPlan from './components/DutyPlan.vue'
import ConfigSetting from './components/ConfigSetting.vue'
import Login from './components/Login.vue'

// 登录状态管理
const isLoggedIn = ref(false)
const userInfo = ref(null)

// 菜单和组件配置
const activeMenu = ref('timeSlot')

const components = {
  timeSlot: TimeSlotConfig,
  user: UserConfig,
  generate: DutyGenerate,
  plan: DutyPlan,
  config: ConfigSetting
}

// 从localStorage加载用户信息
const loadUserInfo = () => {
  const savedUserInfo = localStorage.getItem('userInfo')
  if (savedUserInfo) {
    userInfo.value = JSON.parse(savedUserInfo)
    isLoggedIn.value = true
  }
}

// 登录成功处理
const handleLoginSuccess = (user) => {
  userInfo.value = user
  isLoggedIn.value = true
}

// 退出登录
const handleLogout = () => {
  localStorage.removeItem('userInfo')
  userInfo.value = null
  isLoggedIn.value = false
}

// 页面加载时检查登录状态
onMounted(() => {
  loadUserInfo()
})
</script>

<template>
  <!-- 登录页面 -->
  <Login v-if="!isLoggedIn" @login-success="handleLoginSuccess" />
  
  <!-- 主应用界面 -->
  <div v-else class="app-container">
    <!-- 侧边栏 -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <h2>值班系统</h2>
      </div>
      <nav class="sidebar-nav">
        <el-menu
          default-active="timeSlot"
          class="el-menu-vertical-demo"
          @select="(index) => activeMenu = index"
        >
          <el-menu-item index="timeSlot">
            <template #title>
              <span>时间段配置</span>
            </template>
          </el-menu-item>
          <el-menu-item index="user">
            <template #title>
              <span>人员配置</span>
            </template>
          </el-menu-item>
          <el-menu-item index="generate">
            <template #title>
              <span>生成排班</span>
            </template>
          </el-menu-item>
          <el-menu-item index="plan">
            <template #title>
              <span>排班计划</span>
            </template>
          </el-menu-item>
          <el-menu-item index="config">
            <template #title>
              <span>配置管理</span>
            </template>
          </el-menu-item>
        </el-menu>
      </nav>
      <!-- 用户信息和退出登录 -->
      <div class="user-info">
        <div class="user-name">{{ userInfo.username }}</div>
        <el-button type="text" @click="handleLogout" class="logout-btn">退出登录</el-button>
      </div>
    </aside>
    
    <!-- 主内容区 -->
    <main class="main-content">
      <component :is="components[activeMenu]" :user-info="userInfo" />
    </main>
  </div>
</template>

<style>
/* 基础样式重置，只针对布局元素 */
html, body {
  font-family: Arial, sans-serif;
  background-color: #f5f7fa;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  margin: 0;
  padding: 0;
}

/* App容器样式 */
.app-container {
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  display: flex;
  margin: 0;
  padding: 0;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #f5f7fa;
}

/* 侧边栏样式 */
.sidebar {
  width: 200px;
  height: 100vh;
  margin: 0;
  padding: 0;
  flex-shrink: 0;
  background-color: #304156;
  color: #fff;
  display: flex;
  flex-direction: column;
}

/* 侧边栏头部样式 */
.sidebar-header {
  padding: 20px;
  background-color: #263445;
  text-align: center;
}

/* 侧边栏导航样式 */
.sidebar-nav {
  flex: 1;
  padding: 10px 0;
}

/* 主内容区样式 */
.main-content {
  flex: 1;
  width: calc(100vw - 200px);
  height: 100vh;
  min-width: 0;
  box-sizing: border-box;
  overflow-x: hidden;
  overflow-y: auto;
  margin: 0;
  padding: 20px;
  background-color: #f5f7fa;
}

/* 组件卡片样式 */
.time-slot-config,
.user-config,
.duty-generate,
.duty-plan {
  width: 100%;
  min-width: 800px;
  margin: 0;
  padding: 20px;
  box-sizing: border-box;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

/* 菜单样式 */
.el-menu-vertical-demo {
  background-color: #304156;
  border-right: none;
}

.el-menu-item {
  color: #bfcbd9;
  text-decoration: none;
}

.el-menu-item:hover {
  color: #409eff !important;
  background-color: rgba(64, 158, 255, 0.1) !important;
}

.el-menu-item.is-active {
  background-color: #409eff !important;
  color: #fff !important;
}

/* 确保链接不显示蓝色 */
.el-menu-item a,
.el-menu-item a:hover {
  color: inherit !important;
  text-decoration: none !important;
}

/* 确保菜单文本不会变成蓝色 */
.el-menu-item span {
  color: inherit !important;
  text-decoration: none !important;
}

.el-menu-item span:hover {
  color: inherit !important;
  text-decoration: none !important;
}

/* 用户信息和退出登录样式 */
.user-info {
  padding: 20px;
  margin-top: auto;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  color: #bfcbd9;
}

.user-name {
  margin-bottom: 10px;
  font-size: 14px;
  font-weight: 500;
}

.logout-btn {
  width: 100%;
  color: #bfcbd9 !important;
  padding: 5px 0;
}

.logout-btn:hover {
  color: #409eff !important;
  background-color: rgba(64, 158, 255, 0.1) !important;
}

/* 表格样式 */
.el-table {
  width: 100%;
  min-width: 0;
  box-sizing: border-box;
}

/* 表单样式 */
.el-form {
  width: 100%;
  min-width: 0;
  box-sizing: border-box;
}

/* 确保对话框能正常显示 */
.el-dialog__wrapper {
  position: fixed !important;
  top: 0 !important;
  left: 0 !important;
  right: 0 !important;
  bottom: 0 !important;
  z-index: 2000 !important;
}

.el-dialog {
  position: relative !important;
  margin: 0 auto !important;
  top: 50% !important;
  transform: translateY(-50%) !important;
  z-index: 2001 !important;
}
</style>
