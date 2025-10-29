package com.finz.domain.goal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoalRepository extends JpaRepository<Goal, Long> {
    
    // 사용자별 특정 상태의 목표 조회
    List<Goal> findByUserIdAndStatus(Long userId, GoalStatus status);

}
