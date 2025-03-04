package com.example.quiz.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "answer_option")
public class AnswerOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_id", nullable = true)
    private Answer answer;

    private String optionLabel;
    @Column(name = "option_text", nullable = false, columnDefinition = "TEXT")
    String optionText;

    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;
    // Getters and Setters
    // Omitted for brevity
}

