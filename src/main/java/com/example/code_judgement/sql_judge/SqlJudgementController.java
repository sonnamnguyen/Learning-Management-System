package com.example.code_judgement.sql_judge;

import com.example.code_judgement.CodeExecutionService;
import com.example.code_judgement.ExecutionResponse;
import com.example.student_exercise_attemp.model.Exercise;
import com.example.student_exercise_attemp.service.ExerciseService;
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

    private final SqlJudgementService sqlJudgementService;

    @PostMapping("/precheck-code")
    public String precheckCode(@RequestParam("exerciseId") Long exerciseId,
                               @RequestParam("code") String code,
                               Model model) {
        Exercise exercise = exerciseService.getExerciseById(exerciseId)
                .orElseThrow(()-> new IllegalArgumentException("Invalid exercise ID"));
        List<TestCase> testCases = exercise.getTestCases().stream().filter(testCase -> !testCase.isHidden()).toList();


        if(testCases.isEmpty()) {
            model.addAttribute("exercise", exercise);
            model.addAttribute("code", code);
            model.addAttribute("output", "No test case defined for this exercise");
            return "judgement/precheck_judge/precheck_code";
        }
        try {
            ExecutionResponse response;
            // Nếu ngôn ngữ là SQL, chuyển qua xử lý SQL tại tầng service
            if ("sql".equalsIgnoreCase(exercise.getLanguage().getLanguage())) {
                response = sqlJudgementService.executeSQLCode(false, exercise, code, testCases);
            } else {
                model.addAttribute("output", "No test cases defined for this exercise.");
                model.addAttribute("exercise", exercise);
                model.addAttribute("code", code);
                return "judgement/precheck_judge/precheck_code";
            }
            // Đưa kết quả vào model
            model.addAttribute("exercise", exercise);
            model.addAttribute("code", code);
            model.addAttribute("passed", response.getPassed());
            model.addAttribute("total", response.getTotal());
            model.addAttribute("testResults", response.getTestCasesResults());
            model.addAttribute("output", response.getPassed() + "/" + response.getTotal() + " test cases passed.");
            return "judgement/precheck_judge/precheck_code";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "judgement/precheck_judge/precheck_code";
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
        try {
            ExecutionResponse response;
            // Nếu ngôn ngữ là SQL, chuyển qua xử lý SQL tại tầng service
            if ("sql".equalsIgnoreCase(exercise.getLanguage().getLanguage())) {
                response = sqlJudgementService.executeSQLCode(true, exercise, code, testCases);
            } else {
                model.addAttribute("output", "No test cases defined for this exercise.");
                model.addAttribute("exercise", exercise);
                model.addAttribute("code", code);
                return "judgement/code_space";
            }
            // Đưa kết quả vào model
            model.addAttribute("exercise", exercise);
            model.addAttribute("code", code);
            model.addAttribute("testResults", response.getTestCasesResults());
            model.addAttribute("failed", response.getTotal() - response.getPassed());
            model.addAttribute("score", response.getScore());
            return "judgement/result_exercise";
        } catch (Exception e) {
            System.out.println(e.getMessage());
            model.addAttribute("output", e.getMessage());
            model.addAttribute("exercise", exercise);
            model.addAttribute("code", code);
            model.addAttribute("language", exercise.getLanguage().getLanguage());
            return "judgement/code_space";
        }
    }
}
