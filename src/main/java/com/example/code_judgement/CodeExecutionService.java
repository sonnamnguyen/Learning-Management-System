package com.example.code_judgement;


import com.example.code_judgement.java_judge.JavaJudgementService;
import com.example.code_judgement.languageFactory.ExecutionBasedLanguage;
import com.example.exercise.model.Exercise;
import com.example.exercise.model.ExerciseSession;
import com.example.exercise.repository.ExerciseRepository;
import com.example.exercise.model.StudentExerciseAttempt;
import com.example.exercise.service.StudentExerciseAttemptService;
import com.example.testcase.TestCase;
import com.example.testcase.TestCaseResult;
import com.example.testcase.TestCaseService;
import com.example.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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

    @Autowired
    private StudentExerciseAttemptService studentExerciseAttemptService;

    @Autowired
    private UserService userService;

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

    // calculate score for exercise
    public double exerciseScore(int passed, int total) {
        try {
            if (total == 0) {
                throw new ArithmeticException("TestCase is null");
            }
            double score = (double) passed / total * 100;
            return Math.round(score * 10.0) / 10.0;
        } catch (ArithmeticException e) {
            System.err.println("Error: " + e.getMessage());
            return 0.0;
        }
    }

    public ExecutionResponse executeCodeOptimized(String type, String code, List<TestCase> testCases, ExecutionBasedLanguage executionBasedLanguage, Exercise exercise, ExerciseSession exerciseSession) {
        // Biên dịch mã nguồn một lần
//        ExecutionBasedLanguage executionBasedLanguage = initialLanguage(language);
        long startCompileTime = System.nanoTime();
        CompilationResult compilationResult = executionBasedLanguage.compileCode(code);
        long endCompileTime = System.nanoTime();

        long compileTime = (endCompileTime - startCompileTime)/1_000_000;

        StudentExerciseAttempt studentExerciseAttempt = new StudentExerciseAttempt();
        if(type.equalsIgnoreCase("practice")){
            studentExerciseAttempt.setAttemptDate(LocalDateTime.now());
            studentExerciseAttempt.setSubmitted_code(code);
            studentExerciseAttempt.setSubmitted_exercise(exercise);
            studentExerciseAttempt.setAttendant_user(userService.getCurrentUser());
            studentExerciseAttempt.setSubmitted(true);
            studentExerciseAttemptService.save(studentExerciseAttempt);
        } else if(type.equalsIgnoreCase("assessment")){
            Optional<StudentExerciseAttempt> optStudentExerciseAttempt = studentExerciseAttemptService.getStudentExerciseAttemptBySessionAndExercise(exerciseSession, exercise);
            if(optStudentExerciseAttempt.isPresent()){
                studentExerciseAttempt = optStudentExerciseAttempt.get();
                studentExerciseAttempt.setSubmitted_code(code);
                studentExerciseAttempt.setSubmitted(true);
                studentExerciseAttemptService.save(studentExerciseAttempt);
            } else {
                String error = "Can not take Student Exercise Attempt!";
                System.out.println(error);
                throw new RuntimeException(error);
            }
        }


        if (!compilationResult.isSuccess()) {
            return new ExecutionResponse(code, 0, testCases.size(), 0, null, compilationResult.getErrorMessage(), compileTime, 0);
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
        long runTime =0;
        for (Future<TestCaseResult> future : futures) {
            long startRunTime = System.nanoTime();
            try {
                TestCaseResult result = future.get();
                long endRunTime = System.nanoTime();
                runTime += endRunTime - startRunTime;
                if (result.isCorrect()) {
                    passed++;
                }
                testResults.add(result);
            } catch (Exception e) {
                testResults.add(new TestCaseResult(null, "Error: " + e.getMessage(), false));
            }
        }

        executor.shutdown();
        long avgRunTime = (runTime/testCases.size())/1_000_000;
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

        double score = 0;
        if(type.equalsIgnoreCase("practice") || type.equalsIgnoreCase("assessment")){
            score = exerciseScore(passed, testResults.size());
            studentExerciseAttempt.setScore_exercise(score);
            studentExerciseAttemptService.save(studentExerciseAttempt);
        }

        // Tính toán kết quả tổng quát
        return new ExecutionResponse(code,passed,testCases.size(),score,testResults, null, compileTime, avgRunTime);
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
