package com.example.code_judgement.sql_judge;

import com.example.code_judgement.CodeExecutionService;
import com.example.code_judgement.ExecutionResponse;
import com.example.exercise.Exercise;
import com.example.exercise.ExerciseService;
import com.example.testcase.TestCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/judgement/sql")
@RequiredArgsConstructor
public class SqlJudgementController {
    private final ExerciseService exerciseService;
    private final CodeExecutionService codeExecutionService;

    @PostMapping("/precheck-code")
    public String precheckCode(@RequestParam("exerciseId") Long exerciseId,
                               @RequestParam("code") String code,
                               Model model) {
        Exercise exercise = exerciseService.getExerciseById(exerciseId)
                .orElseThrow(()-> new IllegalArgumentException("Invalid exercise ID"));
        List<TestCase> testCases = exercise.getTestCases();
        if(testCases.isEmpty()) {
            model.addAttribute("exercise", exercise);
            model.addAttribute("code", code);
            model.addAttribute("output", "No test case defined for this exercise");
            return "judgement/code_space";
        }
        try {
            // type 2: "CREATE TABLE"
            if(exercise.getSetup().isEmpty()) {
                ExecutionResponse response = codeExecutionService.executeSQLCodeType2(code, testCases);
                // Đưa kết quả vào model để hiển thị trong view
                model.addAttribute("type_check", "<<<RUNNING TEST CASES>>>");
                model.addAttribute("exercise", exercise);
                model.addAttribute("code", code);
                model.addAttribute("passed", response.getPassed());
                model.addAttribute("total", response.getTotal());
                model.addAttribute("testResults", response.getTestCasesResults());
                model.addAttribute("output", response.getPassed() + "/" + response.getTotal() + " test cases passed.");

                return "judgement/code_space";

            } else {
                // type 1: "SELECT"
                ExecutionResponse response = codeExecutionService.executeSQLCodeType1(exercise.getSetup(), code, testCases);
                // Đưa kết quả vào model để hiển thị trong view
                model.addAttribute("type_check", "<<<RUNNING TEST CASES>>>");
                model.addAttribute("exercise", exercise);
                model.addAttribute("code", code);
                model.addAttribute("passed", response.getPassed());
                model.addAttribute("total", response.getTotal());
                model.addAttribute("testResults", response.getTestCasesResults());
                model.addAttribute("output", response.getPassed() + "/" + response.getTotal() + " test cases passed.");

                return "judgement/code_space";
            }
        } catch (Exception e) {
            model.addAttribute("output", e.getMessage());
            model.addAttribute("exercise", exercise);
            model.addAttribute("code", code);
            model.addAttribute("language", exercise.getLanguage().getLanguage());
            return "judgement/code_space";
        }
    }

    @PostMapping("/submit_exercise")
    public String submitExercise(@RequestParam("exerciseId") Long exerciseId,
                                 @RequestParam("code") String code,
                                 Model model) {
        Exercise exercise = exerciseService.getExerciseById(exerciseId)
                .orElseThrow(()-> new IllegalArgumentException("Invalid exercise ID"));
        List<TestCase> testCases = exercise.getTestCases();
        if(testCases.isEmpty()) {
            model.addAttribute("exercise", exercise);
            model.addAttribute("code", code);
            model.addAttribute("output", "No test case defined for this exercise");
            return "judgement/code_space";
        }
        if(code.isEmpty()) {
            model.addAttribute("exercise", exercise);
            model.addAttribute("code", code);
            model.addAttribute("output", "Your code is empty");
            return "judgement/code_space";
        }
        try {
            // type 2: "CREATE TABLE"
            if(exercise.getSetup().isEmpty()) {
                ExecutionResponse response = codeExecutionService.executeSQLCodeType2(code, testCases);
                model.addAttribute("exercise", exercise);
                model.addAttribute("code", code);
                model.addAttribute("passed", response.getPassed());
                model.addAttribute("total", response.getTotal());
                model.addAttribute("testResults", response.getTestCasesResults());
                model.addAttribute("output", response.getPassed() + "/" + response.getTotal() + " test cases passed.");
                return "judgement/result_exercise";

            } else {
                // type 1: "SELECT"
                ExecutionResponse response = codeExecutionService.executeSQLCodeType1(exercise.getSetup(), code, testCases);

//                model.addAttribute("exercise", exercise);
//                model.addAttribute("code", code);
//                model.addAttribute("passed", response.getPassed());
//                model.addAttribute("total", response.getTotal());
//                model.addAttribute("testResults", response.getTestCasesResults());
//                model.addAttribute("output", response.getPassed() + "/" + response.getTotal() + " test cases passed.");

                model.addAttribute("exercise", exercise);
                model.addAttribute("code", code);
                model.addAttribute("testResults", response.getTestCasesResults());
                model.addAttribute("failed", response.getTotal() - response.getPassed());
                return "judgement/result_exercise";
            }
        } catch (Exception e) {
            model.addAttribute("output", e.getMessage());
            model.addAttribute("exercise", exercise);
            model.addAttribute("code", code);
            model.addAttribute("language", exercise.getLanguage().getLanguage());
            return "judgement/code_space";
        }
    }
}
