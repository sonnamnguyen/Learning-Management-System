package com.example.code_judgement.c_judge;

import com.example.code_judgement.CodeExecutionService;
import com.example.code_judgement.ExecutionResponse;
import com.example.code_judgement.java_judge.JavaJudgementService;
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
@RequestMapping("/judgement/c")
@RequiredArgsConstructor
public class CJudgementController {
    private final CJudgementService cJudgementService;
    private final ExerciseService exerciseService;
    private final CodeExecutionService codeExecutionService;
    // Nhận request chạy code (đã được forward từ CodeJudgementController)
    @PostMapping("/precheck-code")
    public String runCode(@RequestParam("exerciseId") Long exerciseId,
                          @RequestParam("code") String code,
                          Model model) {
        // Lấy bài tập và test cases
        Exercise exercise = exerciseService.getExerciseById(exerciseId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid exercise ID"));
        List<TestCase> testCases = exercise.getTwoTestCases();

        if (testCases == null || testCases.isEmpty()) {
            model.addAttribute("output", "No test cases defined for this exercise.");
            model.addAttribute("exercise", exercise);
            model.addAttribute("code", code);
            return "judgement/code_space";
        }
        try{
            ExecutionResponse response = codeExecutionService.executeCodeOptimized(code,testCases,new CJudgementService());
            // Đưa kết quả vào model để hiển thị trong view
            model.addAttribute("exercise", exercise);
            model.addAttribute("code", code);
            model.addAttribute("passed", response.getPassed());
            model.addAttribute("total", response.getTotal());
            model.addAttribute("testResults", response.getTestCasesResults());
            model.addAttribute("output", response.getPassed() + "/" + response.getTotal() + " test cases passed.");

            return "judgement/code_space";
        }
        catch (Exception e){
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
        // Lấy bài tập và test cases
        Exercise exercise = exerciseService.getExerciseById(exerciseId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid exercise ID"));
        List<TestCase> testCases = exercise.getTestCases();

        if (testCases == null || testCases.isEmpty()) {
            model.addAttribute("output", "No test cases defined for this exercise.");
            model.addAttribute("exercise", exercise);
            model.addAttribute("code", code);
            return "judgement/code_space";
        }
        try{
            ExecutionResponse response = codeExecutionService.executeCodeOptimized(code,testCases,new CJudgementService());
            // Đưa kết quả vào model để hiển thị trong view
            model.addAttribute("exercise", exercise);
            model.addAttribute("code", code);
            model.addAttribute("testResults", response.getTestCasesResults());
            model.addAttribute("failed", response.getTotal() - response.getPassed());

            return "judgement/result_exercise";
        }
        catch (Exception e){
            model.addAttribute("output", e.getMessage());
            model.addAttribute("exercise", exercise);
            model.addAttribute("code", code);
            model.addAttribute("language", exercise.getLanguage().getLanguage());
            return "judgement/code_space";
        }
    }

    @PostMapping("/run-custom-code")
    public String runCustomCode(@RequestParam("exerciseId") Long exerciseId,
                                @RequestParam("code") String code,
                                @RequestParam("customInput") String customInput,
                                Model model) {
        // Lấy bài tập
        Exercise exercise = exerciseService.getExerciseById(exerciseId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid exercise ID"));

        // Thực thi mã nguồn với custom input
        String userOutput = codeExecutionService.runWithCusTomInput(code, customInput,new CJudgementService());


        // Đưa kết quả vào model để hiển thị
        model.addAttribute("exercise", exercise);
        model.addAttribute("code", code);
        model.addAttribute("customOutput", userOutput);
        model.addAttribute("output", "");

        return "judgement/code_space";
    }
}
