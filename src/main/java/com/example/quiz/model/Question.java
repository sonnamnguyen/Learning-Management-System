package com.example.quiz.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "question")
public class Question {


    public enum QuestionType {
        MCQ, SCQ, TEXT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    public void addAnswerOption(AnswerOption answerOption) {
        answerOptions.add(answerOption);
        answerOption.setQuestion(this);
    }

    @ManyToOne
    @JoinColumn(name = "quiz_id")
    @JsonIgnore
    private Quiz quizzes;

    @Column(name = "question_no", nullable = false)
    private Integer questionNo;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", length = 50, nullable = false)
    private QuestionType questionType;

    @Column(name = "points", nullable = false)
    private Integer points = 0;

    @OneToMany(mappedBy = "question", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Answer> answers = new ArrayList<>();

    @OneToMany(mappedBy = "question", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AnswerOption> answerOptions = new ArrayList<>();

    // Getters and Setters
    // Omitted for brevity
}