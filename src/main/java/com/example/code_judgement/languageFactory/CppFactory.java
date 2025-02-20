package com.example.code_judgement.languageFactory;

import com.example.code_judgement.cpp_judge.CppJudgementService;

public class CppFactory implements CodeRunnerFactory{
    @Override
    public ExecutionBasedLanguage createExecutionBasedLanguage() {
        return new CppJudgementService();
    }
}
