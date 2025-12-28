package com.duty.system.service;

import com.duty.system.entity.Config;
import com.duty.system.entity.DutyPlan;
import com.duty.system.entity.TimeSlot;
import com.duty.system.entity.User;

import java.util.Date;
import java.util.List;

public interface DutyService {
    // 生成排班计划
    List<DutyPlan> generateDutyPlan(Date startDate, Date endDate);
    
    // 获取时间段配置
    List<TimeSlot> getTimeSlots();
    
    // 保存时间段配置
    TimeSlot saveTimeSlot(TimeSlot timeSlot);
    
    // 获取用户列表
    List<User> getUsers();
    
    // 保存用户
    User saveUser(User user);
    
    // 获取排班计划
    List<DutyPlan> getDutyPlans(Date startDate, Date endDate);
    
    // 删除日期范围内的排班计划
    void deleteDutyPlans(Date startDate, Date endDate);
    
    // 批量删除排班计划
    void deleteDutyPlansByIds(List<Long> ids);
    
    // 获取所有配置
    List<Config> getConfigs();
    
    // 保存配置
    Config saveConfig(Config config);
    
    // 根据key获取配置
    String getConfigValueByKey(String configKey);
    
    // 删除用户
    void deleteUser(Long userId);
}