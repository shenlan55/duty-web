<script setup>
import { ref } from 'vue'
import axios from 'axios'

const dateRange = ref([])
const loading = ref(false)
const successMessage = ref('')

// 生成排班计划
const generatePlan = async () => {
  if (dateRange.value.length !== 2) {
    return
  }
  
  loading.value = true
  successMessage.value = ''
  
  try {
    const startDate = dateRange.value[0]
    const endDate = dateRange.value[1]
    
    const response = await axios.post('http://localhost:8080/api/duty/generate', null, {
      params: {
        startDate: startDate,
        endDate: endDate
      }
    })
    
    successMessage.value = `成功生成 ${response.data.length} 条排班记录`
    dateRange.value = []
  } catch (error) {
    console.error('生成排班失败:', error)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="duty-generate">
    <h3>生成排班</h3>
    
    <div class="generate-form">
      <el-form label-width="100px">
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 400px;"
          ></el-date-picker>
        </el-form-item>
        
        <el-form-item>
          <el-button 
            type="primary" 
            @click="generatePlan" 
            :loading="loading"
            :disabled="dateRange.length !== 2"
          >
            生成排班
          </el-button>
        </el-form-item>
      </el-form>
      
      <el-alert
        v-if="successMessage"
        title="成功"
        :message="successMessage"
        type="success"
        show-icon
        style="margin-top: 20px;"
      ></el-alert>
    </div>
    
    <div class="rule-info">
      <h4>排班规则说明：</h4>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="oncall-A组（第一大组）">
          7人，大组长1人，组员6人。组员两人一组，分三组排班。大组的groups.value为1。
        </el-descriptions-item>
        <el-descriptions-item label="oncall-B组（第二大组）">
          7人，大组长1人上白班，组员6人。组员两人一组，分三组排班。大组的groups.value为2。
        </el-descriptions-item>
        <el-descriptions-item label="goc组（第三大组）">
          8人，大组长1人，组员7人。组员两人一组，分四组负责各系统。大组的groups.value为3。
        </el-descriptions-item>
        <el-descriptions-item label="pm组（第四大组）">
          2人，1主1备，上白班。大组的groups.value为4。
        </el-descriptions-item>
        <el-descriptions-item label="请假处理">
          请假的员工不参与排班。
        </el-descriptions-item>
        <el-descriptions-item label="oncall组轮动规则">
          oncall两组按周期轮动，在oncall岗上的组，大组长上白班，组员排班为24小时班 休 休，两人一组，组合不乱变动。只有oncall在岗的大组上24小时班 休 休，其他都是白班。
        </el-descriptions-item>
        <el-descriptions-item label="oncall人员请假处理">
          如果在oncall岗上的人请假，从另一组oncall的组员调人补上。如果oncall在岗大组长请假，不在岗的大组长顶岗。只有补充到oncall在岗大组时才按照24小时班 休 休排班。
        </el-descriptions-item>
        <el-descriptions-item label="goc组排班规则">
          goc组，大组长和组员都上白班，组员两人组的有三组，还有一组为1人常驻。不在oncall岗上的其他组员优先往oncall组补充，剩余组员往goc组补充上白班，优先往单人小组补充到3人，剩余人补充去其他小组。还有多余的补充到pm组。
        </el-descriptions-item>
        <el-descriptions-item label="大组长请假处理">
          如果goc组的大组长请假，不在oncall岗上的大组长兼岗。如果不在oncall岗上的大组长请假，goc组的大组长兼岗。oncall在岗的大组长请假时，不在oncall岗的大组长要兼岗。
        </el-descriptions-item>
        <el-descriptions-item label="小组分配规则">
          项目经理和大组长不要挂小组，不参与小组分配。
        </el-descriptions-item>
      </el-descriptions>
    </div>
  </div>
</template>

<style scoped>
.duty-generate {
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

h4 {
  margin: 20px 0 10px 0;
  color: #606266;
}

.generate-form {
  margin-bottom: 30px;
}

.rule-info {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}
</style>