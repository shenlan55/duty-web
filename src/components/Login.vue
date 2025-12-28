<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

// 定义登录表单数据
const loginForm = ref({
  username: '',
  password: ''
})

// 定义登录成功回调
const emit = defineEmits(['login-success'])

// 登录方法
const handleLogin = () => {
  // 简单的硬编码验证，实际项目中应该调用后端API
  if (loginForm.value.username === 'admin' && loginForm.value.password === 'admin') {
    // 登录成功，保存用户信息
    const userInfo = {
      username: 'admin',
      isManager: true,
      roles: ['admin']
    }
    // 保存到localStorage，模拟登录状态持久化
    localStorage.setItem('userInfo', JSON.stringify(userInfo))
    // 触发登录成功事件
    emit('login-success', userInfo)
    // 显示登录成功消息
    ElMessage.success('登录成功')
  } else {
    // 登录失败，显示错误消息
    ElMessage.error('用户名或密码错误')
  }
}
</script>

<template>
  <div class="login-container">
    <div class="login-box">
      <h2>值班系统登录</h2>
      <el-form :model="loginForm" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="loginForm.username" placeholder="请输入用户名"></el-input>
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="loginForm.password" type="password" placeholder="请输入密码"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleLogin" style="width: 100%">登录</el-button>
        </el-form-item>
        <div class="login-tip">
          <p>提示：管理员用户名和密码均为 admin</p>
        </div>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f5f7fa;
}

.login-box {
  width: 400px;
  padding: 30px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

h2 {
  text-align: center;
  margin-bottom: 20px;
  color: #303133;
}

.login-tip {
  margin-top: 15px;
  text-align: center;
  font-size: 12px;
  color: #909399;
}
</style>