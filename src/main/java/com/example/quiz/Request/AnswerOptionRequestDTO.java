package com.example.quiz.Request;

import lombok.Data;

@Data
public class AnswerOptionRequestDTO {
    private String optionLabel;
    private String optionText;
    private boolean correct;
}
