package com.example.code_judgement.languageFactory;

import com.example.code_judgement.csharp_judge.CSharpJudgementService;

public class CSharpFactory implements CodeRunnerFactory{
    @Override
    public ExecutionBasedLanguage createExecutionBasedLanguage() {
        return new CSharpJudgementService();
    }
}
