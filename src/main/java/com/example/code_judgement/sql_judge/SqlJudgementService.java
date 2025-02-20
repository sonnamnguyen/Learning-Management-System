package com.example.code_judgement.sql_judge;

import com.example.code_judgement.ExecutionResponse;
import com.example.testcase.TestCase;
import com.example.testcase.TestCaseResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SqlJudgementService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${SQL_DIR}")
    private String SQL_DIR;
    @Value("${USER_SCHEMAS}")
    private String USER_SCHEMAS;

    public ExecuteUserCodeResponse hashAndRunSQL(String scriptSQL) { // return hash token
        // hash code for tables
        List<String> originalTableName = extractTableName(scriptSQL);
        List<String> randomTableNames = new ArrayList<>();
        String randomId = generateRandomString(16);
        originalTableName.forEach(tableName -> {
            randomTableNames.add( USER_SCHEMAS + "." + tableName + "_" + randomId); // tạo bảng cho user trong schema khác
        });
        for(int i = 0; i < randomTableNames.size(); i++){
            scriptSQL = scriptSQL.replaceFirst("CREATE TABLE\\s+" + originalTableName.get(i), "CREATE TABLE " + randomTableNames.get(i))
                    .replaceFirst("\\s+" + originalTableName.get(i) +"\\s*[(]", " "+randomTableNames.get(i)+"(")
                    .replaceFirst("INSERT INTO\\s+" + originalTableName.get(i) +"\\s*[(]", "INSERT INTO " + randomTableNames.get(i)+"("); // if sql exercise is type 1 (include insert data)
        }
        scriptSQL = deleteBreakLine(scriptSQL);
        try{
            jdbcTemplate.execute(scriptSQL);
            return new ExecuteUserCodeResponse(true,"Create tables successfully", randomTableNames, randomId);
        } catch (Exception e){
            return new ExecuteUserCodeResponse(false, e.getMessage(),null, null);
        }
    }

    // ***************************************************************************
    // Type 1: FOR SELECT TABLE
    public List<Map<String, Object>> select(String code){
        try {
            return jdbcTemplate.queryForList(code);
        } catch(Exception e) {
            return null;
        }
    }

    public ExecutionResponse runTestCases1(String scriptSetup, String code, List<TestCase> testCases, List<String> randomTableNames) {
        // split user's code
        List<String> userScriptCode = new ArrayList<>();
        String[] splits = code.split("\\s*[;]\\s*");
        for (String s : splits) {
            if (!s.isEmpty()) {
                userScriptCode.add(deleteBreakLine(s) + ";");
            }
        }
        int passed = 0;
        List<String> originalTableName = extractTableName(scriptSetup);
        List<TestCaseResult> testResults = new ArrayList<>();
        for(int i = 0; i < userScriptCode.size(); i++){
            String userCode = userScriptCode.get(i), testcase = testCases.get(i).getInput();
            for(int j = 0; j < randomTableNames.size(); j++){
                // change table name for user's code
                userCode = userCode.replace(originalTableName.get(j), randomTableNames.get(j));
                // change table name for test cases
                testcase = testcase.replace(originalTableName.get(j), randomTableNames.get(j));
            }
            List<Map<String, Object>> resultUserCode = select(userCode);
            List<Map<String, Object>> resultTestCase = select(testcase);
            if (resultUserCode.equals(resultTestCase)) {
                passed++;
                testResults.add(new TestCaseResult(testCases.get(i), "Passed", true));
            } else {
                testResults.add(new TestCaseResult(testCases.get(i), "Failed", false));
            }
        }
        // delete table
        try{
            deleteUserTable(randomTableNames);
            System.out.println( "Delete tables successfully");
        } catch (Exception e){
            System.out.println( "Error: " + e.getMessage()+", failed to delete tables");
        }
        return new ExecutionResponse(code, passed, testCases.size(), testResults);
    }


    // ***************************************************************************
    // Type 2: FOR CREATING TABLE - 1 bài có thể tạo ra nhiều bảng
    public String runTestCases(TestCase testCase, String randomId) {
        List<String> originalTableName = extractTableNameFromInsert(testCase.getInput());
        String query = testCase.getInput().replace(originalTableName.get(0), USER_SCHEMAS + "." + originalTableName.get(0) + "_" + randomId);
        try{
            int success = jdbcTemplate.update(query);
            if(success==1){
                return "Passed";
            }
        } catch (Exception e){
            return "Failed";
        }
        return "Failed";
    }

    private static List<String> extractTableName(String userSQL) {
        Pattern pattern = Pattern.compile("CREATE TABLE\\s+(\\w+)");
        Matcher matcher = pattern.matcher(userSQL.toUpperCase());
        List<String> list = new ArrayList<>();
        while (matcher.find()) {
            list.add(matcher.group(1).toLowerCase());
        }
        return list;
    }

    private static List<String> extractTableNameFromInsert(String userSQL) {
        Pattern pattern = Pattern.compile("INSERT INTO\\s+(\\w+)");
        Matcher matcher = pattern.matcher(userSQL.toUpperCase());
        List<String> list = new ArrayList<>();
        while (matcher.find()) {
            list.add(matcher.group(1).toLowerCase());
        }
        return list;
    }

    private static String generateRandomString(int length) {
        SecureRandom random = new SecureRandom();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

        // delete user's table already created
        public String deleteUserTable(List<String> randomTableNames) {
            try {
                for(int i = randomTableNames.size()-1; i >= 0; i--){
                    jdbcTemplate.execute("DROP TABLE IF EXISTS " + randomTableNames.get(i));
                }
                return "Delete tables successfully";
            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
        }


    private String deleteBreakLine(String userSQL) {
        return userSQL.replaceAll("[\r\n]+", " ").replaceAll("[\n]+", " ");
    }

}
