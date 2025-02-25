package com.example.code_judgement.java_judge;

import com.example.code_judgement.CodeExecutionService;
import com.example.code_judgement.ExecutionResponse;
import com.example.exercise.Exercise;
import com.example.exercise.ExerciseService;
import com.example.testcase.TestCase;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@Controller
@RequestMapping("/judgement/java")
@RequiredArgsConstructor
public class JavaJudgementController {
    private final JavaJudgementService javaJudgementService;
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
            ExecutionResponse response = codeExecutionService.executeCodeOptimized(code,testCases,new JavaJudgementService());
            // Đưa kết quả vào model để hiển thị trong view
            model.addAttribute("exercise", exercise);
            model.addAttribute("code", code);
            model.addAttribute("passed", response.getPassed());
            model.addAttribute("total", response.getTotal());
            model.addAttribute("testResults", response.getTestCasesResults());
            model.addAttribute("output", response.getPassed() + "/" + response.getTotal() + " test cases passed.");

            return "judgement/code_space";
//            StringBuilder sb = new StringBuilder();
//            sb.append("You passed ").append(response.getPassed()).append(" of ").append(response.getTotal()).append(" test cases.");
//            return sb.toString();
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
            ExecutionResponse response = codeExecutionService.executeCodeOptimized(code,testCases,new JavaJudgementService());
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
