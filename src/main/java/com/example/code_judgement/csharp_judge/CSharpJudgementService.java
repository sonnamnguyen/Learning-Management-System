package com.example.code_judgement.csharp_judge;

import com.example.code_judgement.CompilationResult;
import com.example.code_judgement.languageFactory.ExecutionBasedLanguage;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;


@Service
public class CSharpJudgementService implements ExecutionBasedLanguage {
    private final Path baseDir;

    public CSharpJudgementService() {
        this.baseDir = Path.of("src/main/java/com/example/code_judgement/csharp_judge/csharp/").toAbsolutePath();
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create directory", e);
        }
    }

    @Override
    public CompilationResult compileCode(String userCode) {
        String randomId = generateRandomString(8);
        String projectName = "Program_" + randomId;
        Path projectDir = baseDir.resolve(projectName);

        try {
            // 1. Tạo thư mục project
            Files.createDirectories(projectDir);

            // 2. Tạo new project
            ProcessBuilder initBuilder = new ProcessBuilder(
                    "dotnet",
                    "new",
                    "console",
                    "--framework",
                    "net9.0",
                    "--output",
                    projectDir.toString()
            );
            initBuilder.redirectErrorStream(true);
            Process initProcess = initBuilder.start();
            String initOutput = readProcessOutput(initProcess.getInputStream());
            System.out.println("Init output: " + initOutput);
            initProcess.waitFor();

            // 3. Ghi code vào Program.cs
            Path programCs = projectDir.resolve("Program.cs");
            Files.writeString(programCs, userCode);

            Path csprojFile = projectDir.resolve(projectName + ".csproj");
            if (!Files.exists(csprojFile)) {
                throw new RuntimeException("Project file not found at: " + csprojFile);
            }
            // 4. Build project
            ProcessBuilder buildBuilder = new ProcessBuilder(
                    "dotnet",
                    "build",
                    csprojFile.toString(),  // Sử dụng đường dẫn đầy đủ đến file .csproj
                    "--configuration",
                    "Release"
            );
            buildBuilder.directory(projectDir.toFile());  // Set working directory
            buildBuilder.redirectErrorStream(true);
            Process buildProcess = buildBuilder.start();

            String buildOutput = readProcessOutput(buildProcess.getInputStream());
            System.out.println("Build output: " + buildOutput);
            int buildExitCode = buildProcess.waitFor();

            Path executablePath = projectDir.resolve("bin/Release/net9.0/" + projectName + ".dll");

            if (buildExitCode == 0 && Files.exists(executablePath)) {
                return new CompilationResult(true, null, executablePath.toString(), executablePath.toFile(), ".cs");
            } else {
                return new CompilationResult(false, "Error: " + buildOutput, projectName, null, null);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new CompilationResult(false, "Exception occurred: " + e.getMessage(), projectName, null, null);
        }
    }

    @Override
    public String runCode(File executableFile, String input) {
        ProcessBuilder runBuilder = new ProcessBuilder(
                "dotnet",
                executableFile.getAbsolutePath()
        );
        runBuilder.redirectErrorStream(true);
        Process runProcess = null;

        try {
            runProcess = runBuilder.start();

            // Xử lý input
            if (input != null && !input.isEmpty()) {
                try (BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(runProcess.getOutputStream()))) {
                    writer.write(input);
                    writer.flush();
                }
            }

            // Đọc output
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(runProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = runProcess.waitFor(2, TimeUnit.SECONDS);
            if (!finished) {
                runProcess.destroyForcibly();
                return "Error: Time Limit Exceeded (TLE)";
            }

            return output.toString().trim();

        } catch (Exception e) {
            return "Error executing code: " + e.getMessage();
        } finally {
            if (runProcess != null && runProcess.isAlive()) {
                runProcess.destroyForcibly();
            }
        }
    }

    // Các phương thức phụ trợ khác giữ nguyên



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

    private void deleteDirectoryRecursively(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
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
}
