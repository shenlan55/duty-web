package com.duty.system.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import javax.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "group_id", nullable = false)
    private Integer groupId;
    
    @Column(name = "sub_group_id")
    private Integer subGroupId;
    
    @Column(name = "is_manager", nullable = false)
    private Boolean isManager = false;
    
    @Column(name = "is_group_leader", nullable = false)
    private Boolean isGroupLeader = false;
    
    @Column(name = "is_sub_group_leader", nullable = false)
    private Boolean isSubGroupLeader = false;
    
    @Column(name = "status", nullable = false)
    private Integer status = 1; // 1: 正常, 0: 请假
    
    @Column(name = "leave_start_date")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    private Date leaveStartDate; // 请假开始时间
    
    @Column(name = "leave_end_date")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    private Date leaveEndDate; // 请假结束时间
}