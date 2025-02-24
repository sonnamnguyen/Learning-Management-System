package com.example.code_judgement;


import com.example.code_judgement.java_judge.JavaJudgementService;
import com.example.code_judgement.languageFactory.ExecutionBasedLanguage;
import com.example.exercise.ExerciseRepository;
import com.example.testcase.TestCase;
import com.example.testcase.TestCaseResult;
import com.example.testcase.TestCaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;



@Service
public class CodeExecutionService {

//    @Autowired
//    private CSharpJudgementService cSharpJudgementService;

    @Autowired
    private JavaJudgementService javaJudgementService;

    @Autowired
    private TestCaseService testCaseService;

    @Autowired
    private ExerciseRepository exerciseRepository;

    public String runWithCusTomInput(String code, String input, ExecutionBasedLanguage executionBasedLanguage) {
//        ExecutionBasedLanguage executionBasedLanguage = initialLanguage(language);
        CompilationResult compilationResult = executionBasedLanguage.compileCode(code);
        if (!compilationResult.isSuccess()) {
            throw new RuntimeException(compilationResult.getErrorMessage());
        }
        String userOutput = executionBasedLanguage.runCode(compilationResult.getRandomFileName(), input);
        if(!compilationResult.getExtensionFileName().equalsIgnoreCase(".cs")){
            try {
                Path filePath = Path.of(compilationResult.getRandomFileName().getAbsolutePath());
                if (Files.exists(filePath)) {
                    System.out.println("File exists, attempting to delete...");
                    Files.delete(filePath);
                } else {
                    System.out.println("File does not exist!");
                }

            } catch (IOException ignored) {
            }
        }
        else{
            try {
                Path executablePath = Path.of(compilationResult.getRandomFileName().getAbsolutePath());

                Path projectDir = executablePath.getParent().getParent().getParent().getParent();

                if (Files.exists(projectDir)) {
                    System.out.println("Deleting project directory: " + projectDir);
                    // Sử dụng phương thức deleteDirectoryRecursively để xóa toàn bộ thư mục
                    deleteDirectoryRecursively(projectDir);
                } else {
                    System.out.println("Project directory not found: " + projectDir);
                }
            } catch (IOException e) {
                System.err.println("Error deleting project: " + e.getMessage());
            }
        }
        return userOutput;
    }

    public ExecutionResponse executeCodeOptimized(String code, List<TestCase> testCases, ExecutionBasedLanguage executionBasedLanguage) {
        // Biên dịch mã nguồn một lần
//        ExecutionBasedLanguage executionBasedLanguage = initialLanguage(language);
        CompilationResult compilationResult = executionBasedLanguage.compileCode(code);
        if (!compilationResult.isSuccess()) {
            throw new RuntimeException(compilationResult.getErrorMessage());
        }

        // Sử dụng ExecutorService để chạy các test case song song
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<TestCaseResult>> futures = new ArrayList<>();

        for (TestCase testCase : testCases) {
            futures.add(executor.submit(() -> {
                String userOutput = executionBasedLanguage.runCode(compilationResult.getRandomFileName(), testCase.getInput());
                boolean isCorrect = userOutput.trim().equals(testCase.getExpectedOutput().trim());
                return new TestCaseResult(testCase, userOutput, isCorrect);
            }));
        }

        // Thu thập kết quả
        List<TestCaseResult> testResults = new ArrayList<>();
        int passed = 0;
        int count = 0;
        for (Future<TestCaseResult> future : futures) {
            try {
                TestCaseResult result = future.get();
                if (result.isCorrect()) {
                    passed++;
                }
                count++;
                if(count == 2) break;
                testResults.add(result);
            } catch (Exception e) {
                testResults.add(new TestCaseResult(null, "Error: " + e.getMessage(), false));
            }
        }

        executor.shutdown();
        if(!compilationResult.getExtensionFileName().equalsIgnoreCase(".cs")){
            try {
                Path filePath = Path.of(compilationResult.getRandomFileName().getAbsolutePath());
                if (Files.exists(filePath)) {
                    System.out.println("File exists, attempting to delete...");
                    Files.delete(filePath);
                } else {
                    System.out.println("File does not exist!");
                }

            } catch (IOException ignored) {
            }
        }
        else{
            try {
                Path executablePath = Path.of(compilationResult.getRandomFileName().getAbsolutePath());

                Path projectDir = executablePath.getParent().getParent().getParent().getParent();

                if (Files.exists(projectDir)) {
                    System.out.println("Deleting project directory: " + projectDir);
                    // Sử dụng phương thức deleteDirectoryRecursively để xóa toàn bộ thư mục
                    deleteDirectoryRecursively(projectDir);
                } else {
                    System.out.println("Project directory not found: " + projectDir);
                }
            } catch (IOException e) {
                System.err.println("Error deleting project: " + e.getMessage());
            }
        }


        // Tính toán kết quả tổng quát
        return new ExecutionResponse(code,passed,testCases.size(),testResults);
    }
    private void deleteDirectoryRecursively(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder()) // Sắp xếp ngược để xóa files trước, directories sau
                    .forEach(file -> {
                        try {
                            Files.delete(file);
                            System.out.println("Deleted: " + file);
                        } catch (IOException e) {
                            System.err.println("Failed to delete: " + file + " - " + e.getMessage());
                        }
                    });
        }
    }

}
