package com.example.code_judgement.cpp_judge;

import com.example.code_judgement.CompilationResult;
import com.example.code_judgement.languageFactory.ExecutionBasedLanguage;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Service
public class CppJudgementService implements ExecutionBasedLanguage {
    private final String BASE_DIR = "src/main/java/com/example/code_judgement/cpp_judge/cpp/";

    @Override
    public CompilationResult compileCode(String userCode) {
        File directory = new File(BASE_DIR);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        String randomId = generateRandomString(8);
        String fileName = "program_" + randomId + ".cpp";
        File sourceFile = new File(BASE_DIR + fileName);
        File executableFile = new File(BASE_DIR + "program_" + randomId+".exe");

        try {
            try (FileWriter writer = new FileWriter(sourceFile)) {
                writer.write(userCode);
            }

            ProcessBuilder compileBuilder = new ProcessBuilder("g++", sourceFile.getAbsolutePath(), "-o", executableFile.getAbsolutePath());
            compileBuilder.redirectErrorStream(true);
            Process compileProcess = compileBuilder.start();

            String compileOutput = readProcessOutput(compileProcess.getInputStream());
            compileProcess.waitFor();

            if (executableFile.exists()) {
                return new CompilationResult(true, null, fileName, executableFile, ".exe");
            } else {
                return new CompilationResult(false, "Error: "+ compileOutput, fileName, null, null);
            }
        } catch (Exception e) {
            return new CompilationResult(false, "Exception occurred: " + e.getMessage(), fileName, null, null);
        } finally {
            try {
                Files.deleteIfExists(sourceFile.toPath());
            } catch (IOException ignored) {
            }
        }
    }

    @Override
    public String runCode(File executablePath, String input) {
        ProcessBuilder runBuilder = new ProcessBuilder(executablePath.getAbsolutePath());
        runBuilder.redirectErrorStream(true);
        Process runProcess = null;
        try {
            runProcess = runBuilder.start();
            if (input != null && !input.isEmpty()) {
                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(runProcess.getOutputStream()))) {
                    writer.write(input);
                    writer.flush();
                }
            }

            StreamGobbler outputGobbler = new StreamGobbler(runProcess.getInputStream());
            Thread outputThread = new Thread(outputGobbler);
            outputThread.start();

            boolean finished = runProcess.waitFor(2, TimeUnit.SECONDS);
            if (!finished) {
                runProcess.destroyForcibly();
                return "Error: Time Limit Exceeded (TLE)";
            }
            outputThread.join();
            return outputGobbler.getOutput().trim();

        } catch (IOException | InterruptedException e) {
            return "Error executing code: " + e.getMessage();
        } finally {
            if (runProcess != null && runProcess.isAlive()) {
                runProcess.destroyForcibly();
            }
//            try {
//                Path filePath = Path.of(executablePath);
//                if (Files.exists(filePath)) {
//                    System.out.println("File exists, attempting to delete...");
//                    Files.delete(filePath);
//                } else {
//                    System.out.println("File does not exist!");
//                }
//            } catch (IOException ignored) {
//            }
        }
    }

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
            } catch (IOException ignored) {
            }
        }

        public String getOutput() {
            return output.toString();
        }
    }

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
