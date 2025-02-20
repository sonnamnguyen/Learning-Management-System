package com.example.code_judgement.languageFactory;

import com.example.code_judgement.java_judge.JavaJudgementService;

public class JavaFactory implements CodeRunnerFactory{

    @Override
    public ExecutionBasedLanguage createExecutionBasedLanguage() {
        return new JavaJudgementService();
    }
}
