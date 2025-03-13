package com.example.exercise.model;


import com.example.exercise.model.Exercise;
import com.example.user.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
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

<<<<<<< HEAD:src/main/java/com/example/exercise/model/StudentExerciseAttempt.java
    // có thể null nếu như participant truy cập assessment bằng email
=======
    @JsonIgnore
>>>>>>> 6c22cd6 (feat: add Dashboard display feature for users):src/main/java/com/example/student_exercise_attemp/model/StudentExerciseAttempt.java
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User attendant_user;

<<<<<<< HEAD:src/main/java/com/example/exercise/model/StudentExerciseAttempt.java
    // participant truy cập assessment bằng email
    @Column(nullable = true)
    private String attendant_email;

=======
    @JsonIgnore
>>>>>>> 6c22cd6 (feat: add Dashboard display feature for users):src/main/java/com/example/student_exercise_attemp/model/StudentExerciseAttempt.java
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", referencedColumnName = "id",nullable = false)
    private Exercise submitted_exercise;

    @JsonProperty("name") // Expose only exercise name
    public String getExerciseName() {
        return submitted_exercise != null ? submitted_exercise.getName() : null;
    }

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
