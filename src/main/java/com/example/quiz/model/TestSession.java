package com.example.quiz.model;

import com.example.assessment.model.StudentAssessmentAttempt;
import com.example.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Builder
@Table(name = "test_session")
public class TestSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "assessment_id")
    private Long assessmentId;

    @Column(name = "check_practice", nullable = false)
    private boolean checkPractice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @OneToMany(mappedBy = "testSession", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Result> results;

    @OneToMany(mappedBy = "testSession", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Answer> answers;
    @OneToMany(mappedBy = "testSession", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PracticeResult> practiceResults;

    public TestSession(User user, Quiz quiz, Long assessmentId) {
        this.user = user;
        this.startTime = LocalDateTime.now();
        this.assessmentId = assessmentId;
        setCheckPracticeBasedOnQuiz(quiz);
    }

    private void setCheckPracticeBasedOnQuiz(Quiz quiz) {
        this.checkPractice = (quiz.getQuizCategory() == Quiz.QuizCategory.PRACTICE);
    }

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "student_assessment_attempt_id", unique = true, nullable = true)
    private StudentAssessmentAttempt studentAssessmentAttempt;
}