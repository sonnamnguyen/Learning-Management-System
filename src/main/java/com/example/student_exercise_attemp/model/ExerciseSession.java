package com.example.student_exercise_attemp.model;


import com.example.assessment.model.Assessment;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "exercise_session")
public class ExerciseSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private double averageScore;

    @OneToMany(mappedBy = "exercise_session", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StudentExerciseAttempt> studentExerciseAttempts;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_id", referencedColumnName = "id")
    private Assessment assessment;

}
