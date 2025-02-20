package com.example.code_judgement.languageFactory;

import com.example.code_judgement.CompilationResult;

import java.io.File;

public interface ExecutionBasedLanguage {
    CompilationResult compileCode(String userCode);
    String runCode(File className, String input);
}
