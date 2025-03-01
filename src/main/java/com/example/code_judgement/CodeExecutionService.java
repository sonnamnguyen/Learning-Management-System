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
import java.util.concurrent.*;

@Service
public class CodeExecutionService {

    // Configurable timeout settings
    private static final int DEFAULT_TIMEOUT = 5;
    private static final int SQL_TIMEOUT = 10; // SQL queries might need more time
    private static final int COMPILATION_TIMEOUT = 30; // Compilation might take longer

    // Max memory allocation (in MB)
    private static final int MAX_MEMORY = 512;

    @Autowired
    private JavaJudgementService javaJudgementService;

    @Autowired
    private TestCaseService testCaseService;

    @Autowired
    private ExerciseRepository exerciseRepository;

    /**
     * Run code with a custom input for testing purposes
     */
    public String runWithCustomInput(String code, String input, ExecutionBasedLanguage executionBasedLanguage) {
        CompilationResult compilationResult = null;

        try {
            // Step 1: Detect language type and set appropriate timeout
            final int timeout = getTimeoutForLanguage(executionBasedLanguage);

            // Step 2: Compile the code with timeout for compilation
            compilationResult = compileWithTimeout(code, executionBasedLanguage);
            if (!compilationResult.isSuccess()) {
                return formatCompilationError(compilationResult.getErrorMessage(), executionBasedLanguage);
            }

            // Step 3: Run the code with execution timeout
            ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
                Thread thread = new Thread(r);
                thread.setDaemon(true);
                return thread;
            });

            // Create a final copy of compilationResult for use in lambda
            final CompilationResult finalCompilationResult = compilationResult;
            Future<String> future = executor.submit(() ->
                    executionBasedLanguage.runCode(finalCompilationResult.getRandomFileName(), input)
            );

            try {
                String userOutput = future.get(timeout, TimeUnit.SECONDS);
                return userOutput;
            } catch (TimeoutException e) {
                future.cancel(true);
                return "Execution Timeout: Your code took too long to execute (>" + timeout + " seconds)";
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                return formatRuntimeError(cause != null ? cause : e, executionBasedLanguage);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "Execution Interrupted: " + e.getMessage();
            } finally {
                executor.shutdownNow();
                try {
                    // Give the executor time to terminate gracefully
                    if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                        System.err.println("Executor did not terminate in the specified time.");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                cleanupFiles(compilationResult);
            }
        } catch (Exception e) {
            if (compilationResult != null) {
                cleanupFiles(compilationResult);
            }
            return "System Error: " + e.getMessage();
        }
    }

    /**
     * Execute code against multiple test cases with optimized parallel execution
     */
    public ExecutionResponse executeCodeOptimized(String code, List<TestCase> testCases, ExecutionBasedLanguage executionBasedLanguage) {
        CompilationResult compilationResult = null;
        ExecutorService executor = null;

        try {
            // Step 1: Detect language and set timeout
            final int timeout = getTimeoutForLanguage(executionBasedLanguage);

            // Step 2: Compile with timeout
            compilationResult = compileWithTimeout(code, executionBasedLanguage);
            if (!compilationResult.isSuccess()) {
                String errorMsg = formatCompilationError(compilationResult.getErrorMessage(), executionBasedLanguage);
                List<TestCaseResult> results = testCases.stream()
                        .map(testCase -> new TestCaseResult(testCase, errorMsg, false))
                        .toList();
                return new ExecutionResponse(code, 0, testCases.size(), results);
            }

            // Step 3: Run test cases with controlled parallelism and resource limits
            int processors = Math.min(Runtime.getRuntime().availableProcessors(), 4); // Limit max threads
            executor = Executors.newFixedThreadPool(processors, r -> {
                Thread thread = new Thread(r);
                thread.setDaemon(true);
                return thread;
            });

            List<Future<TestCaseResult>> futures = new ArrayList<>();

            // Create a final copy for lambda use
            final CompilationResult finalCompilationResult = compilationResult;

            for (TestCase testCase : testCases) {
                futures.add(executor.submit(() -> executeTestCase(
                        testCase,
                        finalCompilationResult,
                        executionBasedLanguage,
                        timeout
                )));
            }

            // Step 4: Collect and process results
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
                    testResults.add(new TestCaseResult(null, "System Error: " + e.getMessage(), false));
                }
            }

            // Step 5: Return results
            return new ExecutionResponse(code, passed, testCases.size(), testResults);

        } catch (Exception e) {
            List<TestCaseResult> errorResults = testCases.stream()
                    .map(testCase -> new TestCaseResult(testCase, "System Error: " + e.getMessage(), false))
                    .toList();
            return new ExecutionResponse(code, 0, testCases.size(), errorResults);
        } finally {
            if (executor != null) {
                executor.shutdownNow();
                try {
                    if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                        System.err.println("Executor did not terminate in the specified time.");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            if (compilationResult != null) {
                cleanupFiles(compilationResult);
            }
        }
    }

    /**
     * Execute a single test case with proper resource constraints and error handling
     */
    private TestCaseResult executeTestCase(
            TestCase testCase,
            CompilationResult compilationResult,
            ExecutionBasedLanguage executionBasedLanguage,
            int timeout) {

        ExecutorService singleTaskExecutor = null;
        try {
            singleTaskExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread thread = new Thread(r);
                thread.setDaemon(true);
                return thread;
            });

            Future<String> executionFuture = singleTaskExecutor.submit(() ->
                    executionBasedLanguage.runCode(compilationResult.getRandomFileName(), testCase.getInput())
            );

            try {
                String userOutput = executionFuture.get(timeout, TimeUnit.SECONDS);
                boolean isCorrect = compareOutputs(
                        userOutput.trim(),
                        testCase.getExpectedOutput().trim(),
                        executionBasedLanguage
                );
                return new TestCaseResult(testCase, userOutput, isCorrect);
            } catch (TimeoutException e) {
                executionFuture.cancel(true);
                return new TestCaseResult(testCase, "Execution Timeout (>" + timeout + " seconds)", false);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                String errorMsg = formatRuntimeError(cause != null ? cause : e, executionBasedLanguage);
                return new TestCaseResult(testCase, errorMsg, false);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new TestCaseResult(testCase, "Execution Interrupted: " + e.getMessage(), false);
            }
        } catch (Exception e) {
            return new TestCaseResult(testCase, "Error: " + e.getMessage(), false);
        } finally {
            if (singleTaskExecutor != null) {
                singleTaskExecutor.shutdownNow();
                try {
                    if (!singleTaskExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                        System.err.println("Task executor did not terminate in the specified time.");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * Compile code with a timeout to prevent hanging on problematic code
     */
    private CompilationResult compileWithTimeout(String code, ExecutionBasedLanguage executionBasedLanguage) throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<CompilationResult> compilationFuture = executor.submit(() ->
                executionBasedLanguage.compileCode(code)
        );

        try {
            return compilationFuture.get(COMPILATION_TIMEOUT, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            compilationFuture.cancel(true);
            CompilationResult result = new CompilationResult();
            result.setSuccess(false);
            result.setErrorMessage("Compilation timed out after " + COMPILATION_TIMEOUT + " seconds");
            return result;
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Get the appropriate timeout based on language type
     */
    private int getTimeoutForLanguage(ExecutionBasedLanguage executionBasedLanguage) {
        String languageType = executionBasedLanguage.getClass().getSimpleName().toLowerCase();

        if (languageType.contains("sql")) {
            return SQL_TIMEOUT;
        }

        // Can add more language-specific timeouts here
        return DEFAULT_TIMEOUT;
    }

    /**
     * Format compilation errors with language-specific details
     */
    private String formatCompilationError(String errorMessage, ExecutionBasedLanguage executionBasedLanguage) {
        String languageType = executionBasedLanguage.getClass().getSimpleName().toLowerCase();

        // Extract useful info from error messages based on language
        try {
            if (languageType.contains("java")) {
                if (errorMessage.contains("public class") && errorMessage.contains("should be declared in a file named")) {
                    return "Compilation Error: Class name doesn't match expected name. Make sure your public class name is correct.";
                }
                if (errorMessage.contains("cannot find symbol") && errorMessage.contains("main")) {
                    return "Compilation Error: No main method found. Your program must have a 'public static void main(String[] args)' method.";
                }
            } else if (languageType.contains("c++") ) {
                if (errorMessage.contains("undefined reference to `main'")) {
                    return "Compilation Error: No main function found. Your program must have a 'int main()' function.";
                }
            } else if (languageType.contains("c")) {
                if (errorMessage.contains("undefined reference to `main'")) {
                    return "Compilation Error: No main function found. Your program must have an 'int main()' function.";
                }
                if (errorMessage.contains("expected ‘;’ before")) {
                    return "Compilation Error: Syntax error, missing semicolon.";
                }
            } else if (languageType.contains("c#") || languageType.contains("csharp") ) {
                if (errorMessage.contains("CS5001") && errorMessage.contains("Main")) {
                    return "Compilation Error: No Main method found. Your C# program must have a 'static void Main()' method inside a class.";
                }
                if (errorMessage.contains("CS1002")) {
                    return "Compilation Error: Missing semicolon.";
                }
            } else if (languageType.contains("sql")) {
                if (errorMessage.toLowerCase().contains("syntax error")) {
                    return "Compilation Error: SQL syntax error. Check your query structure.";
                }
                if (errorMessage.toLowerCase().contains("unknown column")) {
                    return "Compilation Error: Invalid column name. Verify column names in your query.";
                }
                if (errorMessage.toLowerCase().contains("table doesn't exist")) {
                    return "Compilation Error: Table not found. Ensure the table name is correct.";
                }
            }
        } catch (Exception e) {
            return "Error" + e.getClass().getSimpleName();
        }

        // Default error formatting
        return "Compilation Error: " + errorMessage;
    }


    /**
     * Format runtime errors with language-specific details
     */
    private String formatRuntimeError(Throwable error, ExecutionBasedLanguage executionBasedLanguage) {
        String message = error.getMessage();
        String languageType = executionBasedLanguage.getClass().getSimpleName().toLowerCase();

        if (message == null) {
            message = error.getClass().getName();
        }

        // Detect common runtime errors by language
        if (languageType.contains("java")) {
            if (message.contains("java.lang.OutOfMemoryError")) {
                return "Runtime Error: Memory limit exceeded";
            }
            if (message.contains("java.lang.StackOverflowError")) {
                return "Runtime Error: Stack overflow - check for infinite recursion";
            }
            if (message.contains("java.lang.ArrayIndexOutOfBoundsException")) {
                return "Runtime Error: Array index out of bounds";
            }
        } else if (languageType.contains("c") || languageType.contains("c++") || languageType.contains("cpp")) {
            if (message.contains("segmentation fault")) {
                return "Runtime Error: Segmentation fault - check memory access";
            }
        }

        return "Runtime Error: " + message;
    }

    /**
     * Compare expected and actual outputs with smart comparison (accounting for whitespace/formatting)
     */
    private boolean compareOutputs(String actual, String expected, ExecutionBasedLanguage executionBasedLanguage) {
        // Basic exact comparison
        if (actual.equals(expected)) {
            return true;
        }

        String languageType = executionBasedLanguage.getClass().getSimpleName().toLowerCase();

        // For SQL, normalize whitespace and case for comparison
        if (languageType.contains("sql")) {
            String normalizedActual = normalizeSQL(actual);
            String normalizedExpected = normalizeSQL(expected);
            return normalizedActual.equals(normalizedExpected);
        }

        // For other languages, just trim and compare
        return actual.trim().equals(expected.trim());
    }

    /**
     * Normalize SQL output for comparison
     */
    private String normalizeSQL(String sql) {
        return sql.replaceAll("\\s+", " ")
                .replaceAll("\\s*,\\s*", ",")
                .replaceAll("\\s*;\\s*", ";")
                .toLowerCase()
                .trim();
    }

    /**
     * Clean up all temporary files and resources
     */
    private void cleanupFiles(CompilationResult compilationResult) {
        if (compilationResult == null || compilationResult.getRandomFileName() == null) {
            return;
        }

        try {
            String extension = compilationResult.getExtensionFileName().toLowerCase();
            Path filePath = Path.of(compilationResult.getRandomFileName().getAbsolutePath());

            if (extension.equals(".cs")) {
                // C# cleanup requires deleting the whole project structure
                Path projectDir = filePath.getParent().getParent().getParent().getParent();
                if (Files.exists(projectDir)) {
                    System.out.println("Deleting C# project directory: " + projectDir);
                    deleteDirectoryRecursively(projectDir);
                }
            } else if (extension.equals(".c") || extension.equals(".cpp")) {
                // Clean up C/C++ executables and object files
                deleteFileAndRelated(filePath);
            } else {
                // Default cleanup for Java and other languages
                if (Files.exists(filePath)) {
                    System.out.println("Deleting file: " + filePath);
                    Files.delete(filePath);

                    // For Java, also delete class files
                    if (extension.equals(".java")) {
                        String classFilename = filePath.toString().replace(".java", ".class");
                        Path classPath = Path.of(classFilename);
                        if (Files.exists(classPath)) {
                            System.out.println("Deleting class file: " + classPath);
                            Files.delete(classPath);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
    }

    /**
     * Delete a file and its related files (like object files)
     */
    private void deleteFileAndRelated(Path filePath) throws IOException {
        String baseName = filePath.toString();
        baseName = baseName.substring(0, baseName.lastIndexOf('.'));

        // Delete source file
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }

        // Delete executable (no extension on Unix, .exe on Windows)
        Path exePath = Path.of(baseName);
        if (Files.exists(exePath)) {
            Files.delete(exePath);
        }

        Path exeWinPath = Path.of(baseName + ".exe");
        if (Files.exists(exeWinPath)) {
            Files.delete(exeWinPath);
        }

        // Delete object file
        Path objPath = Path.of(baseName + ".o");
        if (Files.exists(objPath)) {
            Files.delete(objPath);
        }
    }

    /**
     * Recursively delete a directory and all its contents
     */
    private void deleteDirectoryRecursively(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
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