package com.duty.system.repository;

import com.duty.system.entity.DutyPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface DutyPlanRepository extends JpaRepository<DutyPlan, Long> {
    List<DutyPlan> findByUserIdAndDateBetween(Long userId, Date startDate, Date endDate);
    List<DutyPlan> findByDateBetween(Date startDate, Date endDate);
    List<DutyPlan> findByDate(Date date);
}