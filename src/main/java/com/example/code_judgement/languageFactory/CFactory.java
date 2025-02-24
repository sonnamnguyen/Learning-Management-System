package com.example.code_judgement.languageFactory;


import com.example.code_judgement.c_judge.CJudgementService;

public class CFactory implements CodeRunnerFactory{
    @Override
    public ExecutionBasedLanguage createExecutionBasedLanguage() {
        return new CJudgementService();
    }
}
