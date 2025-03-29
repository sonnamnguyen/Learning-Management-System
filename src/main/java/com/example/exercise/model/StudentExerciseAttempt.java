package com.example.exercise.model;


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
@Table(name = "student_exercise_attempt")
public class StudentExerciseAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // có thể null nếu như participant truy cập assessment bằng email
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User attendant_user;

    // participant truy cập assessment bằng email
    @Column(nullable = true)
    private String attendant_email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", referencedColumnName = "id",nullable = false)
    private Exercise submitted_exercise;

    // có thể null
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_session_id", referencedColumnName = "id", nullable = true)
    private ExerciseSession exercise_session;

    @Column(columnDefinition = "TEXT")
    private String submitted_code;

    @Min(0)
    private double score_exercise;

    private boolean isSubmitted = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime attemptDate = LocalDateTime.now();
}
