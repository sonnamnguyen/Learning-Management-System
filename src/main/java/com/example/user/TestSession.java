package com.example.user;

import com.example.quiz.model.Answer;
import com.example.quiz.model.AnswerOption;
import com.example.quiz.model.Quiz;
import com.example.quiz.model.Result;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Builder
@Table(name ="test_session")
public class TestSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    public TestSession(User user) {
        this.user = user;
        this.startTime = LocalDateTime.now();
    }

    @OneToMany(mappedBy = "testSession", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Result> results ;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "test_session_answer",
            joinColumns = @JoinColumn(name = "test_session_id"),
            inverseJoinColumns = @JoinColumn(name = "answer_id")
    )
    private Set<Answer> selectedAnswers = new HashSet<>();

}
