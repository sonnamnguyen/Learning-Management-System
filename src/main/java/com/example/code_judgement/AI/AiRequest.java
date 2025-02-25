package com.example.code_judgement.AI;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiRequest {
    private String model;
    private String prompt;
    private boolean stream;
}