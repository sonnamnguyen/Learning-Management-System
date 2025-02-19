package com.example.code_judgement.sql_judge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteUserCodeResponse {
    private boolean isSuccess;
    private String output;
    private List<String> randomTableNames;
    private String randomId;
}
