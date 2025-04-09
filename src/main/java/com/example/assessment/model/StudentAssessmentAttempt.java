package com.example.assessment.model;

import com.example.quiz.model.TestSession;
import com.example.user.User;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;


@Getter
@Setter
@Entity
@Table(name = "student_assessment_attempt")
public class StudentAssessmentAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_id", nullable = false)
    private Assessment assessment;

    @Min(0)
    private int duration;

    @Min(0)
    private int scoreQuiz;

    @Min(0)
    private int scoreEx;

    @Min(0)
    private int scoreAss;

    private String note;

    private boolean isSubmitted;

    private boolean isProctored;

    @JdbcTypeCode(SqlTypes.JSON) // Correct way for Hibernate 6+
    @Column(columnDefinition = "jsonb") // Change to "json" if needed
    private JsonNode proctoringData;

    @Column(nullable = false, updatable = false)
    private LocalDateTime attemptDate = LocalDateTime.now();

    @Column(name = "last_modified", nullable = false)
    private LocalDateTime lastModified = LocalDateTime.now();


    @PrePersist
    public void onPrePersist() {
        this.lastModified = this.attemptDate.plusSeconds(this.duration);
    }


    @OneToOne(mappedBy = "studentAssessmentAttempt", fetch = FetchType.LAZY)
    private TestSession testSession;


    @PreUpdate
    public void onPreUpdate() {
    }
    // Getters and Setters
    // Omitted for brevity
}
