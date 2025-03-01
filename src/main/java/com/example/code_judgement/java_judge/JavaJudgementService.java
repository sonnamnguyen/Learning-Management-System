package com.example.code_judgement.java_judge;

import com.example.code_judgement.CompilationResult;
import com.example.code_judgement.languageFactory.ExecutionBasedLanguage;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class JavaJudgementService implements ExecutionBasedLanguage {

    // Directory to store dynamically created source and class files
    private final String BASE_DIR = "src/main/java/com/example/code_judgement/java_judge/java/";


    @Override
    public CompilationResult compileCode(String userCode) {
        // Ensure the BASE_DIR exists
        File directory = new File(BASE_DIR);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        // Extract the public class name from the user's code
        String originalClassName = extractClassName(userCode);
        if (originalClassName == null) {
            return new CompilationResult(false, "Error: No public class found in Java code!", null, null, null );
        }
        // Generate a unique class name to avoid conflicts (e.g., MyClass_A1B2C3D4E5F6G7H8)
        String randomId = generateRandomString(16);
        String randomClassName = originalClassName + "_" + randomId;
        String fileName = BASE_DIR + randomClassName + ".java";
        File sourceFile = new File(fileName);
        File classFile = new File(BASE_DIR + randomClassName + ".class");
        try {
            // Replace the original class name with the unique class name in the user's code
            userCode = userCode.replaceFirst("public class\\s+" + originalClassName,
                    "public class " + randomClassName);

            // Write the user's code to the source file
            try (FileWriter writer = new FileWriter(sourceFile)) {
                writer.write(userCode);
            }

            // Dùng ProcessBuilder để gọi javac
            ProcessBuilder compileBuilder = new ProcessBuilder("javac", fileName);
            compileBuilder.redirectErrorStream(true); // Kết nối stdin và stderr vào cùng một nơi
            Process compileProcess = compileBuilder.start();

            // Đọc thông báo biên dịch từ javac
            String compileOutput = readProcessOutput(compileProcess.getInputStream());
            compileProcess.waitFor(); // Đợi quá trình biên dịch hoàn thành

            // Kiểm tra file .class đã được tạo (biên dịch thành công hay thất bại)
            if (classFile.exists()) {
                return new CompilationResult(true, null , originalClassName, classFile,".java" ); // Thành công
            } else {
                return new CompilationResult(false,"Runtime exception: "+compileOutput, originalClassName, null, null ); // Thất bại (kèm thông báo lỗi)
            }
        } catch (Exception e) {
            return new CompilationResult(false, "Exception occurred: " + e.getMessage(), originalClassName, null, null );
        } finally {
            // Xóa file mã nguồn sau khi biên dịch xong để đảm bảo không rác thải
            try {
                Files.deleteIfExists(sourceFile.toPath());
            } catch (IOException ignored) {
            }
        }
    }

    @Override
    public String runCode(File classFile, String input) {

        Process runProcess = null;
        try {
            String className = classFile.getName();
            className = className.substring(0, className.lastIndexOf('.'));
            ProcessBuilder runBuilder = new ProcessBuilder("java", "-cp", BASE_DIR, className);
            runBuilder.redirectErrorStream(true); // Kết hợp stdout và stderr
            runProcess = runBuilder.start();

            // Gửi input vào chương trình (nếu cần)
            if (input != null && !input.isEmpty()) {
                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(runProcess.getOutputStream()))) {
                    writer.write(input);
                    writer.flush();
                } catch (IOException e) {
                    // Xử lý lỗi khi ghi input
                    return "Error writing input: " + e.getMessage();
                }
            }

            // Đọc output trong một thread riêng để tránh deadlock
            StreamGobbler outputGobbler = new StreamGobbler(runProcess.getInputStream());
            Thread outputThread = new Thread(outputGobbler);
            outputThread.start();

            // Chờ tiến trình hoàn thành với thời gian giới hạn
            boolean finished = runProcess.waitFor(1, TimeUnit.SECONDS);
            if (!finished) {
                runProcess.destroyForcibly();
                return "Error: Time Limit Exceeded (TLE)";
            }

            outputThread.join(); // Đảm bảo đã đọc hết output
            String programOutput = outputGobbler.getOutput();

            return programOutput.trim();

        } catch (IOException e) {
            return "Error executing code: " + e.getMessage();
        } catch (InterruptedException e) {
            if (runProcess != null) {
                runProcess.destroyForcibly();
            }
            Thread.currentThread().interrupt();
            return "Error: Execution was interrupted.";
        }
        finally {
            if (runProcess != null && runProcess.isAlive()) {
                runProcess.destroyForcibly();
            }
        }
    }

    // Class để đọc output của tiến trình
    private static class StreamGobbler implements Runnable {
        private final InputStream inputStream;
        private final StringBuilder output = new StringBuilder();

        public StreamGobbler(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            } catch (IOException e) {
                // Xử lý lỗi khi đọc output
            }
        }

        public String getOutput() {
            return output.toString();
        }
    }

    /**
     * Read the output from the process's InputStream.
     *
     * @param inputStream The InputStream of the process.
     * @return The output as a String.
     * @throws IOException If an I/O error occurs.
     */
    private String readProcessOutput(InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        return output.toString();
    }

    /**
     * Extract the public class name from the Java code.
     *
     * @param code The Java code.
     * @return The public class name, or null if not found.
     */
    private String extractClassName(String code) {
        Pattern pattern = Pattern.compile("public class\\s+(\\w+)");
        Matcher matcher = pattern.matcher(code);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Generate a random string of uppercase letters and digits with the specified length.
     *
     * @param length The desired length of the random string.
     * @return The generated random string.
     */
    private String generateRandomString(int length) {
        SecureRandom random = new SecureRandom();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
