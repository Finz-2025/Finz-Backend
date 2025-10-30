package com.finz.domain.coach;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoachMessageRepository extends JpaRepository<CoachMessage, Long> {
    
    // 사용자별 최근 대화 조회 (최신순)
    List<CoachMessage> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);

    // 채팅방 입장 시 '전체' 대화 내역을 불러오기 위한 메서드 (시간 오름차순)
    List<CoachMessage> findByUserIdOrderByCreatedAtAsc(Long userId);
}
