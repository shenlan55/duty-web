package com.duty.system.repository;

import com.duty.system.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByGroupId(Integer groupId);
    List<User> findBySubGroupId(Integer subGroupId);
    List<User> findByGroupIdAndStatus(Integer groupId, Integer status);
}