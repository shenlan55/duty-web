package com.duty.system.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import javax.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "duty_plan")
public class DutyPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "date", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date date;
    
    @Column(name = "time_slot_id", nullable = false)
    private Long timeSlotId;
    
    @Column(name = "type", nullable = false)
    private Integer type; // 1: 白班, 2: 夜班, 3: 24小时班, 4: 辅助
    
    @Column(name = "assigned_sub_group_id")
    private Integer assignedSubGroupId;
    
    @Column(name = "status", nullable = false)
    private Integer status = 1; // 1: 正常, 0: 取消
    
    @Column(name = "duty_group_id")
    private Integer dutyGroupId; // 兼大组ID
    
    @Column(name = "duty_sub_group_id")
    private Integer dutySubGroupId; // 兼小组ID
}