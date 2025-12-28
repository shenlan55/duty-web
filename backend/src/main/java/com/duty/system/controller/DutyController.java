package com.duty.system.controller;

import com.duty.system.entity.Config;
import com.duty.system.entity.DutyPlan;
import com.duty.system.entity.TimeSlot;
import com.duty.system.entity.User;
import com.duty.system.service.DutyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/duty")
public class DutyController {

    @Autowired
    private DutyService dutyService;
    
    // 生成排班计划
    @PostMapping("/generate")
    public ResponseEntity<List<DutyPlan>> generateDutyPlan(
            @RequestParam("startDate") @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam("endDate") @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        List<DutyPlan> plans = dutyService.generateDutyPlan(startDate, endDate);
        return ResponseEntity.ok(plans);
    }
    
    // 获取时间段配置
    @GetMapping("/time-slots")
    public ResponseEntity<List<TimeSlot>> getTimeSlots() {
        return ResponseEntity.ok(dutyService.getTimeSlots());
    }
    
    // 保存时间段配置
    @PostMapping("/time-slots")
    public ResponseEntity<TimeSlot> saveTimeSlot(@RequestBody TimeSlot timeSlot) {
        return ResponseEntity.ok(dutyService.saveTimeSlot(timeSlot));
    }
    
    // 获取用户列表
    @GetMapping("/users")
    public ResponseEntity<List<User>> getUsers() {
        return ResponseEntity.ok(dutyService.getUsers());
    }
    
    // 保存用户
    @PostMapping("/users")
    public ResponseEntity<User> saveUser(@RequestBody User user) {
        return ResponseEntity.ok(dutyService.saveUser(user));
    }
    
    // 删除用户
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable("userId") Long userId) {
        dutyService.deleteUser(userId);
        return ResponseEntity.ok().build();
    }
    
    // 获取排班计划
    @GetMapping("/plans")
    public ResponseEntity<List<DutyPlan>> getDutyPlans(
            @RequestParam("startDate") @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam("endDate") @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        return ResponseEntity.ok(dutyService.getDutyPlans(startDate, endDate));
    }
    
    // 获取所有配置
    @GetMapping("/configs")
    public ResponseEntity<List<Config>> getConfigs() {
        return ResponseEntity.ok(dutyService.getConfigs());
    }
    
    // 保存配置
    @PostMapping("/configs")
    public ResponseEntity<Config> saveConfig(@RequestBody Config config) {
        return ResponseEntity.ok(dutyService.saveConfig(config));
    }
    
    // 根据key获取配置
    @GetMapping("/configs/{key}")
    public ResponseEntity<String> getConfigByKey(@PathVariable("key") String key) {
        String value = dutyService.getConfigValueByKey(key);
        return ResponseEntity.ok(value);
    }
    
    // 批量删除排班计划
    @DeleteMapping("/plans")
    public ResponseEntity<Void> deleteDutyPlansByIds(@RequestBody List<Long> ids) {
        dutyService.deleteDutyPlansByIds(ids);
        return ResponseEntity.ok().build();
    }
    
    // 根据日期范围删除排班计划
    @DeleteMapping("/plans/range")
    public ResponseEntity<Void> deleteDutyPlansByRange(
            @RequestParam("startDate") @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam("endDate") @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        dutyService.deleteDutyPlans(startDate, endDate);
        return ResponseEntity.ok().build();
    }
}