package com.example.assessment.repository;

import com.example.assessment.model.ScoreEditHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScoreEditHistoryRepository extends JpaRepository<ScoreEditHistory, Long> {

    // Lấy lịch sử chỉnh sửa điểm của một attempt, sắp xếp theo thời gian giảm dần
    List<ScoreEditHistory> findByAttemptIdOrderByEditedAtDesc(Long attemptId);


}
