package com.example.code_judgement.c_judge;

import com.example.code_judgement.CodeExecutionService;
import com.example.code_judgement.ExecutionResponse;
import com.example.code_judgement.csharp_judge.CSharpJudgementService;
import com.example.code_judgement.java_judge.JavaJudgementService;
import com.example.exercise.Exercise;
import com.example.exercise.ExerciseService;
import com.example.testcase.TestCase;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

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
            ExecutionResponse response = codeExecutionService.executeCodeOptimized(code,testCases,new CSharpJudgementService());
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

    @PostMapping("/run_custom_code")
    public ResponseEntity<String> runCustomCode(@RequestParam("code") String code,
                                                @RequestParam("customInput") String customInput){
        String userOutput = codeExecutionService.runWithCusTomInput(code, customInput,new JavaJudgementService());
        return ResponseEntity.ok(userOutput);
    }
}
