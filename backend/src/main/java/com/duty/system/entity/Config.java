package com.duty.system.entity;

import javax.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "config")
public class Config {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "config_key", nullable = false, unique = true)
    private String configKey;
    
    @Column(name = "config_value", nullable = false)
    private String configValue;
    
    @Column(name = "description")
    private String description;
}