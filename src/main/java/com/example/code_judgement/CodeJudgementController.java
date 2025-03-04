package com.example.code_judgement;

import com.example.exercise.Exercise;
import com.example.exercise.ExerciseService;
import com.example.testcase.TestCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/judgement")
@RequiredArgsConstructor
public class CodeJudgementController {
    private final ExerciseService exerciseService;

    // Hiển thị giao diện code space cho một bài tập
    @GetMapping("/code_space/{id}")
    public String showExercisePlayground(@PathVariable Long id,
                                         Model model) {
        Exercise exercise = exerciseService.getExerciseById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid exercise ID"));
        model.addAttribute("exercise", exercise);
        model.addAttribute("code", exercise.getSetup() );
        model.addAttribute("output", "");
        return "judgement/code_space";
    }

    // Chạy code khi user nhập custom input và trả lại output
    @PostMapping("/run-custom-code")
    public String runCustomCode(@RequestParam("exerciseId") Long exerciseId,
                                @RequestParam("code") String code,
                                @RequestParam("customInput") String customInput,
                                Model model) {
        // Lấy bài tập
        Exercise exercise = exerciseService.getExerciseById(exerciseId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid exercise ID"));

        // Thực thi mã nguồn với custom input dựa trên ngôn ngữ
        if(customInput == null) {
            model.addAttribute("customOutput", "No custom input defined for this exercise.");
            model.addAttribute("exercise", exercise);
            model.addAttribute("code", code);
            return "judgement/code_space";
        }

        // Đưa kết quả vào model để hiển thị
        model.addAttribute("exercise", exercise);
        model.addAttribute("code", code);
        model.addAttribute("customInput", customInput);
        model.addAttribute("customOutput", "");

        String targetPath = "/judgement/" +  (exercise.getLanguage().getLanguage().equalsIgnoreCase("c#")?"csharp":exercise.getLanguage().getLanguage().toLowerCase()) + "/run-custom-code";
        return "forward:" + targetPath;
    }

    // Nhận request chạy code, sau đó chuyển tiếp đến controller xử lý code tương ứng với ngôn ngữ
    @PostMapping("/precheck-code")
    public String runCode(@RequestParam("exerciseId") Long exerciseId,
                          @RequestParam("code") String code,
                          Model model) {
        // Lấy bài tập và test cases tương ứng
        Exercise exercise = exerciseService.getExerciseById(exerciseId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid exercise ID"));
        List<TestCase> testCases = exercise.getTestCases().stream().filter(testCase -> !testCase.isHidden()).toList();

        if (testCases == null || testCases.isEmpty()) {
            model.addAttribute("output", "No test cases defined for this exercise.");
            model.addAttribute("exercise", exercise);
            model.addAttribute("code", code);
            return "judgement/code_space";
        }

        // Xây dựng đường dẫn chuyển tiếp dựa trên ngôn ngữ (ví dụ: "java" -> "/judgement/java_judge/precheck-code")
        String targetPath = "/judgement/" + (exercise.getLanguage().getLanguage().equalsIgnoreCase("c#")?"csharp":exercise.getLanguage().getLanguage().toLowerCase()) + "/precheck-code";
        System.out.println(targetPath);
        return "forward:" + targetPath;
    }

    @PostMapping("/run_test_case")
    public String runTestCase(@RequestParam("exerciseId") Long exerciseId,
                              @RequestParam("code") String code,
                              Model model) {
        model.addAttribute("exerciseId", exerciseId);
        model.addAttribute("code", code);
        return "judgement/run_test_case";
    }

    @PostMapping("/submit_exercise")
    public String submitExercise(@RequestParam("exerciseId") Long exerciseId,
                                 @RequestParam("code") String code,
                                 Model model){
        Exercise exercise = exerciseService.getExerciseById(exerciseId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid exercise ID"));
        List<TestCase> testCases = exercise.getTestCases();

        if (testCases == null || testCases.isEmpty()) {
            model.addAttribute("output", "No test cases defined for this exercise.");
            model.addAttribute("exercise", exercise);
            model.addAttribute("code", code);
            return "judgement/code_space";
        }
        String targetPath = "/judgement/" +  (exercise.getLanguage().getLanguage().equalsIgnoreCase("c#")?"csharp":exercise.getLanguage().getLanguage().toLowerCase()) + "/submit_exercise";
        System.out.println(targetPath);
        return "forward:" + targetPath;
    }
}
