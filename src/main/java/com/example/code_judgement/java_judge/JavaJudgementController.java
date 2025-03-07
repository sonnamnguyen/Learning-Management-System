package com.example.code_judgement.java_judge;

import com.example.code_judgement.CodeExecutionService;
import com.example.code_judgement.ExecutionResponse;
import com.example.student_exercise_attemp.model.Exercise;
import com.example.student_exercise_attemp.service.ExerciseService;
import com.example.testcase.TestCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
        List<TestCase> testCases = exercise.getTestCases().stream().filter(testCase -> !testCase.isHidden()).toList();

        if (testCases == null || testCases.isEmpty()) {
            model.addAttribute("output", "<strong>No test case defined for this exercise</strong>");
            model.addAttribute("exercise", exercise);
            model.addAttribute("code", code);
            return "judgement/precheck_judge/precheck_code";
        }
        try{
            ExecutionResponse response = codeExecutionService.executeCodeOptimized(false, code,testCases,new JavaJudgementService(), exercise);
            // Nếu compile code bị thất bại trả về error message
            if(response.getErrorMessage()!=null){
                model.addAttribute("error", response.getErrorMessage());
                return "judgement/precheck_judge/precheck_code";
            }
            // Đưa kết quả vào model để hiển thị trong view
            model.addAttribute("exercise", exercise);
            model.addAttribute("code", code);
            model.addAttribute("passed", response.getPassed());
            model.addAttribute("total", response.getTotal());
            model.addAttribute("testResults", response.getTestCasesResults());
            model.addAttribute("output", "<p>You passed <strong th:text=\"${passed}\">0</strong> out of <strong th:text=\"${total}\">0</strong>\n" +
                    "test cases.</p>");
            return "judgement/precheck_judge/precheck_code";
        }
        catch (RuntimeException e){
            model.addAttribute("output", e.getMessage());
            model.addAttribute("exercise", exercise);
            model.addAttribute("code", code);
            model.addAttribute("language", exercise.getLanguage().getLanguage());
            return "judgement/precheck_judge/precheck_code";
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
            ExecutionResponse response = codeExecutionService.executeCodeOptimized(true, code,testCases,new JavaJudgementService(), exercise);
            // Đưa kết quả vào model để hiển thị trong view
            model.addAttribute("exercise", exercise);
            model.addAttribute("code", code);
            model.addAttribute("failed", response.getTotal() - response.getPassed());
            model.addAttribute("score", response.getScore());

            if(response.getErrorMessage()!=null){
                model.addAttribute("error", response.getErrorMessage());
                return "judgement/result_exercise";
            }
            model.addAttribute("testResults", response.getTestCasesResults());
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
    public ResponseEntity<String> runCustomCode(@RequestParam("exerciseId") Long exerciseId,
                                                @RequestParam("code") String code,
                                                @RequestParam("customInput") String customInput) {
        try {
            // Lấy bài tập
            Exercise exercise = exerciseService.getExerciseById(exerciseId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid exercise ID"));

            // Thực thi mã nguồn với custom input
            String userOutput = codeExecutionService.runWithCusTomInput(code, customInput, new JavaJudgementService());

            // Trả về output dưới dạng JSON
            return ResponseEntity.ok(userOutput);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

}
