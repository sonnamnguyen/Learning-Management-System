package com.example.quiz.repository;

import com.example.quiz.model.TestSession;
import com.example.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestSessionRepository extends JpaRepository<TestSession, Long> {
    TestSession findTopByUserOrderByStartTimeDesc(User user);

    // Đếm số lần làm bài theo từng tháng
    @Query("SELECT MONTH(r.completionTime), COUNT(r) FROM Result r GROUP BY MONTH(r.completionTime)")
    List<Object[]> countAttemptsByMonth();

}
