package com.example.student_exercise_attemp;


import com.example.exercise.Exercise;
import com.example.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "student_exercise_attemp")
public class StudentExerciseAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User attendant_user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", referencedColumnName = "id",nullable = false)
    private Exercise submitted_exercise;

    @Column(columnDefinition = "TEXT")
    private String submitted_code;

    @Min(0)
    private double score_exercise;

    private boolean isSubmitted = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime attemptDate = LocalDateTime.now();
}
