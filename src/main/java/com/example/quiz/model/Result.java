package com.example.quiz.model;

import com.example.user.TestSession;
import lombok.*;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "result")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Result {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_session_id", nullable = false)
    private TestSession testSession;

    @Column(name = "score")
    private Integer score;

    @Column(name = "completion_time")
    private LocalDateTime completionTime;

    public Result(TestSession testSession, Integer score) {
        this.testSession = testSession;
        this.score = score;
        this.completionTime = LocalDateTime.now();
    }
}