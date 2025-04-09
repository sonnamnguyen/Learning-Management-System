package com.example.assessment.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "score_edit_history")
@Data
@Getter
@Setter
@NoArgsConstructor
public class ScoreEditHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "attempt_id", nullable = false)
    private Long attemptId;

    @Column(name = "old_score_ex")
    private Integer oldScoreEx;

    @Column(name = "new_score_ex")
    private Integer newScoreEx;

    @Column(name = "old_score_quiz")
    private Integer oldScoreQuiz;

    @Column(name = "new_score_quiz")
    private Integer newScoreQuiz;

    @Column(name = "old_score_ass")
    private Integer oldScoreAss;

    @Column(name = "new_score_ass")
    private Integer newScoreAss;

    @Column(nullable = false)
    private String comment;


    @Column(name = "edited_at", nullable = false)
    private LocalDateTime editedAt = LocalDateTime.now();

}
