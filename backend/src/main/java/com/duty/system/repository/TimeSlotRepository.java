package com.duty.system.repository;

import com.duty.system.entity.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {
    List<TimeSlot> findByStatus(Integer status);
    List<TimeSlot> findByType(Integer type);
}