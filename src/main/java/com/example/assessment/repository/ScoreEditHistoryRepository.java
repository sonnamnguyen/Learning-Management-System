package com.example.assessment.repository;

import com.example.assessment.model.ScoreEditHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ScoreEditHistoryRepository extends JpaRepository<ScoreEditHistory, Long> {

    // Lấy lịch sử chỉnh sửa điểm của một attempt, sắp xếp theo thời gian giảm dần
    List<ScoreEditHistory> findByAttemptIdOrderByEditedAtDesc(Long attemptId);

    @Query("SELECT seh.comment FROM ScoreEditHistory seh WHERE seh.attemptId = :attemptId ORDER BY seh.editedAt DESC LIMIT 1")
    String findLatestCommentByAttemptId(@Param("attemptId") Long attemptId);

    // Lấy batch các comment mới nhất cho nhiều attempt cùng lúc
    @Query("SELECT seh.attemptId, seh.comment FROM ScoreEditHistory seh " +
            "WHERE seh.attemptId IN :attemptIds AND seh.editedAt = " +
            "(SELECT MAX(seh2.editedAt) FROM ScoreEditHistory seh2 WHERE seh2.attemptId = seh.attemptId)")
    List<Object[]> findLatestCommentsForAttempts(@Param("attemptIds") List<Long> attemptIds);

}
