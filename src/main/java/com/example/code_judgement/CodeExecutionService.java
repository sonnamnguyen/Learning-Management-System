package com.example.code_judgement;

import com.example.code_judgement.c_judge.CJudgementService;
import com.example.code_judgement.java_judge.JavaJudgementService;
import com.example.code_judgement.sql_judge.ExecuteUserCodeResponse;
import com.example.code_judgement.sql_judge.SqlJudgementService;
import com.example.exercise.ExerciseRepository;
import com.example.testcase.TestCase;
import com.example.testcase.TestCaseResult;
import com.example.testcase.TestCaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;



@Service
public class CodeExecutionService {

    // ***************************************************************************
    // Execute Java Code
    @Value("${BASE_DIR}")
    private String BASE_DIR;
    @Autowired
    private JavaJudgementService javaJudgementService;

    @Autowired
    private TestCaseService testCaseService;

    @Autowired
    private ExerciseRepository exerciseRepository;

    public String runWithCusTomInput(String code, String input) {
        CompilationResult compilationResult = javaJudgementService.compileCode(code);
        if (!compilationResult.isSuccess()) {
            throw new RuntimeException(compilationResult.getErrorMessage());
        }
        String userOutput = javaJudgementService.runCode(compilationResult.getRandomClassName(), input);
        try {
            Files.deleteIfExists(Path.of(BASE_DIR + compilationResult.getRandomClassName()+".java"));
        } catch (IOException ignored) {
        }
        return userOutput;
    }

    public ExecutionResponse executeCodeOptimized(String code, List<TestCase> testCases) {
        // Biên dịch mã nguồn một lần
        CompilationResult compilationResult = javaJudgementService.compileCode(code);
        if (!compilationResult.isSuccess()) {
            throw new RuntimeException(compilationResult.getErrorMessage());
        }

        // Sử dụng ExecutorService để chạy các test case song song
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<TestCaseResult>> futures = new ArrayList<>();

        for (TestCase testCase : testCases) {
            futures.add(executor.submit(() -> {
                String userOutput = javaJudgementService.runCode(compilationResult.getRandomClassName(), testCase.getInput());
                boolean isCorrect = userOutput.trim().equals(testCase.getExpectedOutput().trim());
                return new TestCaseResult(testCase, userOutput, isCorrect);
            }));
        }

        // Thu thập kết quả
        List<TestCaseResult> testResults = new ArrayList<>();
        int passed = 0;
        for (Future<TestCaseResult> future : futures) {
            try {
                TestCaseResult result = future.get();
                if (result.isCorrect()) {
                    passed++;
                }
                testResults.add(result);
            } catch (Exception e) {
                testResults.add(new TestCaseResult(null, "Error: " + e.getMessage(), false));
            }
        }

        executor.shutdown();

        try {
            Path filePath = Path.of(BASE_DIR + compilationResult.getRandomClassName() + ".class");
            if (Files.exists(filePath)) {
                System.out.println("File exists, attempting to delete...");
                Files.delete(filePath);
            } else {
                System.out.println("File does not exist!");
            }

        } catch (IOException ignored) {
        }

        // Tính toán kết quả tổng quát
        return new ExecutionResponse(code,passed,testCases.size(),testResults);
    }

    // ***************************************************************************
    // Execute SQL-MySQL Code
    @Autowired
    private SqlJudgementService sqlJudgementService;

    public ExecutionResponse executeSQLCodeType2(String code, List<TestCase> testCases) {
        ExecuteUserCodeResponse executeUserCodeResponse = sqlJudgementService.hashAndRunSQL(code);
        if(executeUserCodeResponse.isSuccess()){
            List<TestCaseResult> testResults = new ArrayList<>();
            int passed = 0;
            for(TestCase testCase : testCases){
                String result = sqlJudgementService.runTestCases(testCase, executeUserCodeResponse.getRandomId());
                if(result.equals(testCase.getExpectedOutput())){
                    testResults.add(new TestCaseResult(testCase, result, true));
                    passed++;
                } else{
                    testResults.add(new TestCaseResult(testCase, result, false));
                }
            }
            // delete users' table
            try{
                sqlJudgementService.deleteUserTable(executeUserCodeResponse.getRandomTableNames());
            } catch (Exception e){
            }
            return new ExecutionResponse(code, passed, testCases.size(), testResults);
        }
        return new ExecutionResponse(code, 0, testCases.size(), null);
    }

    public ExecutionResponse executeSQLCodeType1(String scriptSetup, String code, List<TestCase> testCases) {
        ExecuteUserCodeResponse executeUserCodeResponse = sqlJudgementService.hashAndRunSQL(scriptSetup);
        if(executeUserCodeResponse.isSuccess()){
            return sqlJudgementService.runTestCases1(scriptSetup, code, testCases, executeUserCodeResponse.getRandomTableNames());
        }
        return new ExecutionResponse(code, 0, testCases.size(), null);
    }

    // ***************************************************************************
    // Execute C Code
    @Value("${C_DIR}")
    private String C_DIR;

    @Autowired
    private CJudgementService cJudgementService;

    public ExecutionResponse executeCodeCOptimized (String code, List<TestCase> testCases ) {
        CompilationResult compilationResult = javaJudgementService.compileCode(code);
        if (!compilationResult.isSuccess()) {
            throw new RuntimeException(compilationResult.getErrorMessage());
        }

        // Sử dụng ExecutorService để chạy các test case song song
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<TestCaseResult>> futures = new ArrayList<>();

        for (TestCase testCase : testCases) {
            futures.add(executor.submit(() -> {
                String userOutput = javaJudgementService.runCode(compilationResult.getRandomClassName(), testCase.getInput());
                boolean isCorrect = userOutput.trim().equals(testCase.getExpectedOutput().trim());
                return new TestCaseResult(testCase, userOutput, isCorrect);
            }));
        }

        // Thu thập kết quả
        List<TestCaseResult> testResults = new ArrayList<>();
        int passed = 0;
        for (Future<TestCaseResult> future : futures) {
            try {
                TestCaseResult result = future.get();
                if (result.isCorrect()) {
                    passed++;
                }
                testResults.add(result);
            } catch (Exception e) {
                testResults.add(new TestCaseResult(null, "Error: " + e.getMessage(), false));
            }
        }

        executor.shutdown();

        try {
            Path filePath = Path.of(BASE_DIR + compilationResult.getRandomClassName() + ".class");
            if (Files.exists(filePath)) {
                System.out.println("File exists, attempting to delete...");
                Files.delete(filePath);
            } else {
                System.out.println("File does not exist!");
            }

        } catch (IOException ignored) {
        }

        // Tính toán kết quả tổng quát
        return new ExecutionResponse(code,passed,testCases.size(),testResults);
//        return new ExecutionResponse(code, 0, 0, null);
    }

}