package com.example.code_judgement.sql_judge;

import com.example.code_judgement.ExecutionResponse;
import com.example.exercise.model.Exercise;
import com.example.exercise.model.ExerciseSession;
import com.example.exercise.model.StudentExerciseAttempt;
import com.example.exercise.service.StudentExerciseAttemptService;
import com.example.testcase.TestCase;
import com.example.testcase.TestCaseResult;
import com.example.user.UserService;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SqlJudgementService {

    @Autowired
    private UserService userService;

    @Autowired
    private StudentExerciseAttemptService studentExerciseAttemptService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    public ExecutionResponse executeSQLCode(String type, Exercise exercise, String userCode, List<TestCase> testCases, ExerciseSession exerciseSession) {
        if(type.equalsIgnoreCase("precheck") && userCode.trim().equals(exercise.getSetup().trim())){
            throw new RuntimeException("You have not done this exercise yet!");
        }

        StudentExerciseAttempt studentExerciseAttempt = new StudentExerciseAttempt();
        if(type.equalsIgnoreCase("practice")){
            studentExerciseAttempt.setAttemptDate(LocalDateTime.now());
            studentExerciseAttempt.setSubmitted_code(userCode);
            studentExerciseAttempt.setSubmitted_exercise(exercise);
            studentExerciseAttempt.setAttendant_user(userService.getCurrentUser());
            studentExerciseAttempt.setSubmitted(true);
            studentExerciseAttemptService.save(studentExerciseAttempt);
        } else if(type.equalsIgnoreCase("assessment")){
            Optional<StudentExerciseAttempt> optStudentExerciseAttempt = studentExerciseAttemptService.getStudentExerciseAttemptBySessionAndExercise(exerciseSession, exercise);
            if(optStudentExerciseAttempt.isPresent()){
                studentExerciseAttempt = optStudentExerciseAttempt.get();
                studentExerciseAttempt.setSubmitted_code(userCode);
                studentExerciseAttempt.setSubmitted(true);
                studentExerciseAttemptService.save(studentExerciseAttempt);
            } else {
                String error = "Can not take Student Exercise Attempt";
                System.out.println(error);
                throw new RuntimeException(error);
            }
        }

        userCode = removeCommentsFromSQL(userCode);

        // Sinh suffix duy nhất (8 ký tự)
        String uniqueSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        // Kiểm tra schema 'sql_judge', nếu không tồn tại thì tạo
        String checkSchemaQuery = "SELECT 1 FROM information_schema.schemata WHERE schema_name = 'sql_judge'";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(checkSchemaQuery);
        if (result.isEmpty()) {
            jdbcTemplate.execute("CREATE SCHEMA sql_judge");
        }
        jdbcTemplate.execute("SET SCHEMA 'sql_judge'");

        // Tách code người dùng thành các segment dựa theo marker (sql_1:, sql_2:, ...)
        Map<String, String> codeSegments = parseUserCode(userCode);
        if (codeSegments.isEmpty()) {
            codeSegments.put("sql_1", userCode);
        }
        // Kiểm tra định dạng và thứ tự của segment tags
        for (String tag : codeSegments.keySet()) {
            if (!tag.matches("(?i)sql_\\d+")) {
                throw new IllegalArgumentException("Invalid segment tag format: " + tag);
            }
        }
        validateConsecutiveSegmentTags(codeSegments);

        // Xác định exercise có liên quan đến tạo bảng hay không
        boolean isTableCreationExercise = false;
        for (String segment : codeSegments.values()) {
            if (segment.trim().toUpperCase().startsWith("CREATE TABLE")) {
                isTableCreationExercise = true;
                break;
            }
        }
        String globalSuffix = isTableCreationExercise ? uniqueSuffix + "_user" : uniqueSuffix;

        // Xây dựng mapping đổi tên bảng toàn cục
        Set<String> allTableNames = new HashSet<>();
        String setupSQL = exercise.getSetupsql();
        if (setupSQL != null && !setupSQL.trim().isEmpty()) {
            allTableNames.addAll(extractTableNames(setupSQL));
        }
        for (String segment : codeSegments.values()) {
            allTableNames.addAll(extractTableNames(segment));
        }
        for (TestCase tc : testCases) {
            String testSQL = tc.getInput();
            if (testSQL != null && !testSQL.trim().isEmpty()) {
                allTableNames.addAll(extractTableNames(testSQL));
            }
        }
        Map<String, String> tableMapping = new HashMap<>();
        for (String tableName : allTableNames) {
            tableMapping.put(tableName, tableName + "_" + globalSuffix);
        }

        // Thực thi setup SQL (nếu có) bằng cách tách và thực thi từng câu lệnh
        if (setupSQL != null && !setupSQL.trim().isEmpty()) {
            String modifiedSetupSQL = renameTables(setupSQL, tableMapping);
            try {
                // Thực thi setup với giao dịch riêng biệt
                getThis().executeScriptNew(modifiedSetupSQL);
            } catch (Exception e) {
                // Dù lỗi setup xảy ra, cố gắng cleanup các bảng (nếu có)
                cleanupTables(tableMapping.values());
                throw new RuntimeException("Error during setup execution: " + e.getMessage(), e);
            }
        }

        int totalTestCases = 0;
        int passedTestCases = 0;
        List<TestCaseResult> resultsList = new ArrayList<>();

        // Sắp xếp các segment theo thứ tự (sql_1, sql_2, ...)
        List<String> sortedTags = new ArrayList<>(codeSegments.keySet());
        sortedTags.sort(Comparator.comparingInt(this::extractNumber));

        try {
            // Xử lý từng segment
            for (String tag : sortedTags) {
                String segmentCode = codeSegments.get(tag);
                String modifiedSegmentCode = renameTables(segmentCode, tableMapping);
                List<TestCase> segmentTestCases = filterTestCasesByTag(testCases, tag);

                // Nếu segment chỉ có 1 câu lệnh SELECT thì dùng executeTestQueryNew
                if (isSingleSelectQuery(modifiedSegmentCode)) {
                    List<Map<String, Object>> userSegmentResult;
                    try {
                        userSegmentResult = getThis().executeTestQueryNew(modifiedSegmentCode);
                    } catch (Exception e) {
                        String errorMessage = e.getMessage();
                        for (Map.Entry<String, String> entry : tableMapping.entrySet()) {
                            errorMessage = errorMessage.replace(entry.getValue(), entry.getKey());
                        }

                        for (TestCase tc : segmentTestCases) {
                            totalTestCases++;
                            resultsList.add(new TestCaseResult(tc, "Execution error: " + errorMessage, false));
                        }
                        markRemainingSegmentsAsNotExecuted(sortedTags, tag, testCases, resultsList);
                        break;
                    }
                    // Với mỗi test case của segment, thực thi test query và so sánh kết quả
                    for (TestCase tc : segmentTestCases) {
                        totalTestCases++;
                        String output;
                        boolean testPassed = false;
                        try {
                            String modifiedTestQuery = renameTables(tc.getInput(), tableMapping);
                            List<Map<String, Object>> testQueryResult = getThis().executeTestQueryNew(modifiedTestQuery);
                            output = testQueryResult.toString();
                            testPassed = userSegmentResult.toString().equals(output);
                        } catch (Exception e) {
                            output = extractErrorMessage(e.getMessage());
                            for (Map.Entry<String, String> entry : tableMapping.entrySet()) {
                                output = output.replace(entry.getValue(), entry.getKey());
                            }
//                            System.out.println("Error during test case execution: " + e.getMessage());
                            testPassed = output.equals(tc.getExpectedOutput());
                        }
                        if (testPassed) {
                            passedTestCases++;
                            resultsList.add(new TestCaseResult(tc, "Query successfully!", testPassed));
                        } else {
                            resultsList.add(new TestCaseResult(tc, "The expected output and your output are not similar!", testPassed));
                        }
                    }
                } else {
                    // Nếu segment chứa nhiều câu lệnh (không chỉ SELECT)
                    try {
                        getThis().executeScriptNew(modifiedSegmentCode);
                    } catch (Exception e) {
                        String errorMessage = e.getMessage();

                        for (Map.Entry<String, String> entry : tableMapping.entrySet()) {
                            errorMessage = errorMessage.replace(entry.getValue(), entry.getKey());
                        }

                        for (TestCase tc : segmentTestCases) {
                            totalTestCases++;
                            resultsList.add(new TestCaseResult(tc, "Execution error: " + errorMessage, false));
                        }
                        markRemainingSegmentsAsNotExecuted(sortedTags, tag, testCases, resultsList);
                        break;
                    }
                    // Thực thi và chấm điểm test case cho segment này
                    for (TestCase tc : segmentTestCases) {
                        totalTestCases++;
                        String output;
                        boolean testPassed = false;
                        try {
                            String modifiedTestQuery = renameTables(tc.getInput(), tableMapping);
                            List<Map<String, Object>> userResult = getThis().executeScriptAndReturnLastSelectNew(modifiedTestQuery);
                            output = userResult != null ? userResult.toString() : "";
                            testPassed = output.equals(tc.getExpectedOutput());
                        } catch (Exception e) {
                            output = extractErrorMessage(e.getMessage());
                            for (Map.Entry<String, String> entry : tableMapping.entrySet()) {
                                output = output.replace(entry.getValue(), entry.getKey());
                            }
//                            System.out.println("Error during test case execution: " + e.getMessage());
                            testPassed = output.equals(tc.getExpectedOutput());
                        }
                        if (testPassed) {
                            passedTestCases++;
                            resultsList.add(new TestCaseResult(tc, "Query successfully!", testPassed));
                        } else {
                            if(output.contains("Execution error: ")) {
                                resultsList.add(new TestCaseResult(tc, output, testPassed));
                            } else {
                                resultsList.add(new TestCaseResult(tc, "The expected output and your output are not similar!", testPassed));
                            }
                        }
                    }
                }
            }
        } finally {
            // Dọn dẹp các bảng được tạo
            cleanupTables(tableMapping.values());
            jdbcTemplate.execute("SET SCHEMA 'public'");
        }

        double score = 0;
        if(type.equalsIgnoreCase("practice") || type.equalsIgnoreCase("assessment")){
            score = exerciseScore(passedTestCases, testCases.size());
            studentExerciseAttempt.setScore_exercise(score);
            studentExerciseAttemptService.save(studentExerciseAttempt);
        }

        ExecutionResponse response = new ExecutionResponse();
        response.setPassed(passedTestCases);
        response.setTotal(testCases.size());
        response.setTestCasesResults(resultsList);
        response.setScore(score);
        return response;
    }

    /**
     * Phương thức chạy với custom input. Tương tự, các bước thực thi (script và custom query)
     * sẽ được cách ly trong giao dịch riêng.
     */
    public String runWithCusTomInput(String code, String customInput) {
        code = removeCommentsFromSQL(code);
        String uniqueSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        String checkSchemaQuery = "SELECT 1 FROM information_schema.schemata WHERE schema_name = 'sql_judge'";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(checkSchemaQuery);
        if (result.isEmpty()) {
            jdbcTemplate.execute("CREATE SCHEMA sql_judge");
        }
        jdbcTemplate.execute("SET SCHEMA 'sql_judge'");

        Map<String, String> codeSegments = parseUserCode(code);
        if (codeSegments.isEmpty()) {
            codeSegments.put("sql_1", code);
        }

        Set<String> allTableNames = new HashSet<>();
        for (String segment : codeSegments.values()) {
            allTableNames.addAll(extractTableNames(segment));
        }
        if (customInput != null && !customInput.trim().isEmpty()) {
            allTableNames.addAll(extractTableNames(customInput));
        }
        Map<String, String> tableMapping = new HashMap<>();
        for (String tableName : allTableNames) {
            tableMapping.put(tableName, tableName + "_" + uniqueSuffix);
        }

        List<String> sortedTags = new ArrayList<>(codeSegments.keySet());
        sortedTags.sort(Comparator.comparingInt(this::extractNumber));

        try {
            for (String tag : sortedTags) {
                String segmentCode = codeSegments.get(tag);
                String modifiedSegmentCode = renameTables(segmentCode, tableMapping);
                getThis().executeScriptNew(modifiedSegmentCode);
            }
            String modifiedCustomInput = renameTables(customInput, tableMapping);
            List<Map<String, Object>> resultList = getThis().executeScriptAndReturnLastSelectNew(modifiedCustomInput);
            return resultList != null ? resultList.toString() : "";
        } catch (Exception e) {
            String output = e.getMessage();

            for (Map.Entry<String, String> entry : tableMapping.entrySet()) {
                output = output.replace(entry.getValue(), entry.getKey());
            }
            return output;
        } finally {
            cleanupTables(tableMapping.values());
        }
    }

    /* ==================== Các hàm helper ==================== */

    private String removeCommentsFromSQL(String sql) {
        String[] lines = sql.split("\n");
        StringBuilder processedSQL = new StringBuilder();
        for (String line : lines) {
            String processedLine = line.replaceAll("^\\s*--.*", "").trim();
            if (!processedLine.isEmpty()) {
                processedSQL.append(processedLine).append("\n");
            }
        }
        return processedSQL.toString().replaceAll("(?s)/\\*.*?\\*/", "");
    }

    private Map<String, String> parseUserCode(String userCode) {
        Map<String, String> segments = new LinkedHashMap<>();
        Pattern pattern = Pattern.compile("(?i)(sql_\\d+:)");
        Matcher matcher = pattern.matcher(userCode);
        List<Integer> startIndexes = new ArrayList<>();
        List<String> tags = new ArrayList<>();
        while (matcher.find()) {
            startIndexes.add(matcher.start());
            tags.add(matcher.group().toLowerCase().replace(":", ""));
        }
        if (startIndexes.isEmpty()) {
            return segments;
        }
        for (int i = 0; i < startIndexes.size(); i++) {
            int start = startIndexes.get(i);
            int end = (i < startIndexes.size() - 1) ? startIndexes.get(i + 1) : userCode.length();
            String segment = userCode.substring(start, end);
            String currentTag = tags.get(i);
            String segmentCode = segment.replaceFirst("(?i)" + currentTag + ":", "").trim();
            segments.put(currentTag, segmentCode);
        }
        return segments;
    }

    private void validateConsecutiveSegmentTags(Map<String, String> segments) {
        List<Integer> numbers = new ArrayList<>();
        for (String tag : segments.keySet()) {
            numbers.add(extractNumber(tag));
        }
        Collections.sort(numbers);
        for (int i = 0; i < numbers.size(); i++) {
            if (numbers.get(i) != i + 1) {
                throw new IllegalArgumentException(
                        "SQL segment tags must be consecutive starting from sql_1. Expected sql_" + (i + 1) +
                                " but found sql_" + numbers.get(i));
            }
        }
    }

    private int extractNumber(String tag) {
        try {
            return Integer.parseInt(tag.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    private List<TestCase> filterTestCasesByTag(List<TestCase> testCases, String tag) {
        List<TestCase> filtered = new ArrayList<>();
        for (TestCase tc : testCases) {
            String testCaseTag = tc.getSqlTagNumber();
            if (testCaseTag == null || !testCaseTag.matches("(?i)sql_\\d+")) {
                throw new IllegalArgumentException("Test case SQL tag is not in the correct format: " + testCaseTag);
            }
            if (tag.equalsIgnoreCase(testCaseTag)) {
                filtered.add(tc);
            }
        }
        return filtered;
    }

    private String renameTables(String sql, Map<String, String> tableMapping) {
        for (Map.Entry<String, String> entry : tableMapping.entrySet()) {
            String oldTable = entry.getKey();
            String newTable = entry.getValue();
            sql = sql.replaceAll("(?i)\\b" + Pattern.quote(oldTable) + "\\b", newTable);
        }
        return sql;
    }

    private Set<String> extractTableNames(String sql) {
        Set<String> tableNames = new HashSet<>();
        Pattern pattern = Pattern.compile("(?i)\\b(CREATE\\s+TABLE|FROM|JOIN|INTO|UPDATE)\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\b");
        Matcher matcher = pattern.matcher(sql);
        while (matcher.find()) {
            String tableName = matcher.group(2);
            tableNames.add(tableName);
        }
        return tableNames;
    }

    private boolean isSingleSelectQuery(String sql) {
        List<String> statements = splitSqlStatements(sql);
        return statements.size() == 1 && statements.get(0).trim().toLowerCase().startsWith("select");
    }

    private List<String> splitSqlStatements(String script) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        for (int i = 0; i < script.length(); i++) {
            char c = script.charAt(i);
            if (c == '\'' && !inDoubleQuote) {
                if (inSingleQuote && i + 1 < script.length() && script.charAt(i + 1) == '\'') {
                    current.append("''");
                    i++;
                    continue;
                }
                inSingleQuote = !inSingleQuote;
            } else if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            }
            if (c == ';' && !inSingleQuote && !inDoubleQuote) {
                String stmt = current.toString().trim();
                if (!stmt.isEmpty()) {
                    statements.add(stmt);
                }
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        String lastStmt = current.toString().trim();
        if (!lastStmt.isEmpty()) {
            statements.add(lastStmt);
        }
        return statements;
    }

    private void cleanupTables(Collection<String> tableNames) {
        String checkSchemaQuery = "SELECT 1 FROM information_schema.schemata WHERE schema_name = 'sql_judge'";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(checkSchemaQuery);
        if (result.isEmpty()) {
            jdbcTemplate.execute("CREATE SCHEMA sql_judge");
        }
        jdbcTemplate.execute("SET SCHEMA 'sql_judge'");

        for (String tableName : tableNames) {
            String dropQuery = "DROP TABLE IF EXISTS " + tableName + " CASCADE";
            try {
                jdbcTemplate.execute(dropQuery);
            } catch (Exception ex) {
                // Log nếu cần
                System.err.println("SQL Dropping table " + tableName + " failed: " + ex.getMessage());
            }
        }
    }

    private void markRemainingSegmentsAsNotExecuted(List<String> sortedTags, String currentTag,
                                                    List<TestCase> allTestCases, List<TestCaseResult> results) {
        int currentIndex = sortedTags.indexOf(currentTag);
        for (int i = currentIndex + 1; i < sortedTags.size(); i++) {
            String remainingTag = sortedTags.get(i);
            List<TestCase> remainingTCs = filterTestCasesByTag(allTestCases, remainingTag);
            for (TestCase tc : remainingTCs) {
                results.add(new TestCaseResult(tc, "Not executed due to previous error", false));
            }
        }
    }

    private String extractErrorMessage(String fullError) {
        int index = fullError.toUpperCase().indexOf("ERROR:");
        if (index >= 0) {
            int endIndex = fullError.indexOf("\n", index);
            if (endIndex > 0) {
                return fullError.substring(index, endIndex).trim();
            } else {
                return fullError.substring(index).trim();
            }
        }
        return fullError;
    }

    /* ==================== Các phương thức thực thi với REQUIRES_NEW ==================== */

    /**
     * Thực thi câu lệnh SELECT (hoặc script có SELECT) trong giao dịch mới.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Map<String, Object>> executeTestQueryNew(String query) {
        String checkSchemaQuery = "SELECT 1 FROM information_schema.schemata WHERE schema_name = 'sql_judge'";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(checkSchemaQuery);
        if (result.isEmpty()) {
            jdbcTemplate.execute("CREATE SCHEMA sql_judge");
        }
        jdbcTemplate.execute("SET SCHEMA 'sql_judge'");

        return jdbcTemplate.queryForList(query);
    }

    /**
     * Thực thi một SQL script (chia câu lệnh theo dấu ;) trong giao dịch mới.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeScriptNew(String script) {
        String checkSchemaQuery = "SELECT 1 FROM information_schema.schemata WHERE schema_name = 'sql_judge'";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(checkSchemaQuery);
        if (result.isEmpty()) {
            jdbcTemplate.execute("CREATE SCHEMA sql_judge");
        }
        jdbcTemplate.execute("SET SCHEMA 'sql_judge'");

        List<String> statements = splitSqlStatements(script);
        for (String stmt : statements) {
            String trimmed = stmt.trim();
            if (trimmed.toLowerCase().startsWith("select")) {
                jdbcTemplate.queryForList(trimmed);
            } else {
                jdbcTemplate.execute(trimmed);
            }
        }
    }

    /**
     * Thực thi SQL script và trả về kết quả của câu lệnh SELECT cuối cùng trong giao dịch mới.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Map<String, Object>> executeScriptAndReturnLastSelectNew(String script) {
        List<String> statements = splitSqlStatements(script);
        List<Map<String, Object>> lastSelectResult = new ArrayList<>();
        for (String stmt : statements) {
            String trimmed = stmt.trim();
            if (trimmed.toLowerCase().startsWith("select")) {
                lastSelectResult = jdbcTemplate.queryForList(trimmed);
            } else {
                jdbcTemplate.execute(trimmed);
            }
        }
        return lastSelectResult;
    }

    /**
     * Trả về "proxy" của bean hiện tại để gọi các phương thức có @Transactional với propagation = REQUIRES_NEW.
     * Yêu cầu cấu hình Spring phải cho phép exposeProxy (ví dụ: <tx:annotation-driven proxy-target-class="true" expose-proxy="true" />).
     */
    private SqlJudgementService getThis() {
        return (SqlJudgementService) AopContext.currentProxy();
    }

}
