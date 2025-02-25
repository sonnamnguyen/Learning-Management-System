package com.example.code_judgement.AI;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AiReviewResponse {
    private String review;
    private String error;
}