package com.duty.system.entity;

import javax.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "time_slot")
public class TimeSlot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "start_time", nullable = false)
    private String startTime;
    
    @Column(name = "end_time", nullable = false)
    private String endTime;
    
    @Column(name = "type", nullable = false)
    private Integer type; // 1: 白班, 2: 夜班, 3: 24小时班
    
    @Column(name = "status", nullable = false)
    private Integer status = 1; // 1: 启用, 0: 禁用
}