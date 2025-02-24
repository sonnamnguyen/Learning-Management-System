package com.example.quiz.model;

import com.example.course.Course;
import com.example.user.User;

import jakarta.persistence.*;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.validation.BindingResult;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "quiz")
@Getter
@Setter
@NoArgsConstructor
public class Quiz {

    public enum QuizType {
        OPEN, CLOSE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Quiz name is required.")
    @Column(name = "quiz_title", length = 255, nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @NotNull(message = "Course is required.")
    private Course course;

    @Lob
    @NotBlank(message = "Description is required.")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    // Many-to-Many with User
    @ManyToMany
    @JoinTable(
            name = "quiz_participants",
            joinColumns = @JoinColumn(name = "quiz_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> participants;

    @ManyToMany
    @JoinTable(
            name = "quiz_question",
            joinColumns = @JoinColumn(name = "quiz_id"),
            inverseJoinColumns = @JoinColumn(name = "question_id")
    )
    private Set<Question> questions = new HashSet<>();

    public void addQuestion(Question question) {
        this.questions.add(question);
        question.getQuizzes().add(this);
    }

    @NotNull(message = "Duration is required.")
    @Min(value = 1, message = "Duration must be at least 1 minute.")
    @Column(name = "duration", nullable = false, columnDefinition = "INTEGER DEFAULT 30")
    private Integer duration = 30;

    @NotNull(message = "Attempt limit is required.")
    @Min(value = 1, message = "Attempt limit must be at least 1.")
    @Column(name = "attempt_limit")
    private Integer attemptLimit;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "quiz_status", length = 50, nullable = false)
    private Quiz.QuizType quizType;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    public boolean isValidDuration() {
        return duration > 0; // Duration must be positive
    }

}