package com.example.assessment.model;

import com.example.course.Course;
import com.example.exercise.model.Exercise;
import com.example.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Data
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "assessment")
public class Assessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private String title;

    @ManyToMany
    @JoinTable(
            name = "assessment_exercise",
            joinColumns = @JoinColumn(name = "assessment_id"),
            inverseJoinColumns = @JoinColumn(name = "exercise_id")
    )
    private Set<Exercise> exercises = new HashSet<>();

    @OneToMany(mappedBy = "assessment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<AssessmentQuestion> assessmentQuestions = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_type_id", nullable = false)
    private AssessmentType assessmentType;

    @Min(0)
    private int invitedCount;

    @Min(0)
    private int assessedCount;

    @Min(0)
    private int qualifiedCount;

    @Min(0)
    private int qualifyScore;

    private boolean shuffled ;

    @Min(0)
    private int quizScoreRatio;

    @Min(0)
    private int exerciseScoreRatio;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Column(nullable = false)
    private int timeLimit;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String invitedEmails;

    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToOne
    @JoinColumn(name = "updated_by", nullable = true)
    private User updatedBy;

    public int getExerciseCount() {
        return exercises.size();
    }

    public int getQuestionCount() {
        return assessmentQuestions.size();
    }

//    @PostPersist
//    public void createProgressNotificationOnAssessmentAttempt() {
//        try {
//            String message = "You completed the assessment with a Quiz Score: " + this.scoreQuiz + " and an Assignment Score: " + this.scoreAss + "!";
//
//            ProgressNotification progressNotification = new ProgressNotification();
//            progressNotification.setUser(this.user);  // Assuming `this.user` is the User (Student)
//            progressNotification.setCourse(this.assessment.getCourse());  // Assuming `this.assessment` has a Course field
//            progressNotification.setNotificationMessage(message);
//            progressNotification.setNotificationDate(LocalDateTime.now());
//
//            // Save the progress notification
//            progressNotificationRepository.save(progressNotification);
//        } catch (Exception e) {
//            // Log error or handle as needed
//        }
//    }

    // Getters and Setters
    // Omitted for brevity
}

