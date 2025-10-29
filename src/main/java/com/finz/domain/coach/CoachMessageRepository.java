package com.finz.domain.coach;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoachMessageRepository extends JpaRepository<CoachMessage, Long> {
    
    // 사용자별 최근 대화 조회 (최신순)
    List<CoachMessage> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);

}
