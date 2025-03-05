package com.example.exercise;

import com.example.assessment.model.ProgrammingLanguage;
import com.example.assessment.service.ProgrammingLanguageService;
import com.example.testcase.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.*;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.*;

@Controller
@RequestMapping("/exercises")
@RequiredArgsConstructor
public class ExerciseController {
    private final ProgrammingLanguageService programmingLanguageService;
    private final ExerciseRepository exerciseRepository;
    private final ExerciseService exerciseService;
    private final TestCaseService testCaseService;


    // Common attributes for all views
    @ModelAttribute
    public void addCommonAttributes(Model model) {
        model.addAttribute("title", "Exercises");
        model.addAttribute("links", "/style.css");
        model.addAttribute("links", "/print.css");
    }


    //    @GetMapping
//    public String getList(
//            @RequestParam(value = "title", required = false) String title,
//            @RequestParam(value = "language", required = false) Long languageId,
//            @RequestParam(value = "level", required = false) String level,
//            @RequestParam(value = "page", defaultValue = "0") int page,
//            Model model) {
//
//        Pageable pageable = PageRequest.of(page, 10);
//        Page<Exercise> exercisesPage = exerciseService.getExercisesByLanguageAndLevel(languageId, level, pageable);
//
//        // Nếu có từ khóa tìm kiếm, thì gọi searchByTitle()
//        if (title != null && !title.isEmpty()) {
//            List<Exercise> exercises = exerciseService.searchByTitle(title);
//            model.addAttribute("exercises", exercises);
//            model.addAttribute("totalPages", 1); // Vì kết quả search không có phân trang
//        } else {
//            exercisesPage = exerciseService.getExercisesByLanguageAndLevel(languageId, level, pageable);
//            model.addAttribute("exercises", exercisesPage.getContent());
//            model.addAttribute("totalPages", exercisesPage.getTotalPages());
//        }
//        model.addAttribute("currentPage", page);
//        model.addAttribute("languages", programmingLanguageService.findAll());
//        model.addAttribute("currentLanguage", languageId);
//        model.addAttribute("currentLevel", level);
//
//        return "exercises/list";
//    }
    @GetMapping
    public String getList(
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "language", required = false) Long languageId,
            @RequestParam(value = "level", required = false) String level,
            @RequestParam(value = "page", defaultValue = "0") int page,
            Model model) {

        Pageable pageable = PageRequest.of(page, 10);

        List<Exercise> exercises;
        int totalPages;

        // Search by title if provided
        if (title != null && !title.isEmpty()) {
            exercises = exerciseService.searchByTitle(title);
            totalPages = 1; // No pagination for search results
        } else {
            // Filter by language and level
            Page<Exercise> exercisesPage = exerciseService.getExercisesByLanguageAndLevel(languageId, level, pageable);
            exercises = exercisesPage.getContent();
            totalPages = exercisesPage.getTotalPages();
        }

        // Add attributes to model
        model.addAttribute("exercises", exercises);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("languages", programmingLanguageService.findAll());
        model.addAttribute("currentLanguage", languageId);
        model.addAttribute("currentLevel", level);
        model.addAttribute("paramTitle", title);

        System.out.println("Language ID: " + languageId + ", Level: " + level + ", Title: " + title);

        return "exercises/list";
    }

    // Show create exercise form (existing method)
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("exercise", new Exercise());
        TestCaseFormList testCaseFormList = new TestCaseFormList();
        testCaseFormList.setTestCasesList(List.of(new TestCaseForm())); // Ít nhất 1 test case
        model.addAttribute("testCaseFormList", testCaseFormList); // Initialize the wrapper
        List<ProgrammingLanguage> programmingLanguages = programmingLanguageService.getAllProgrammingLanguages();
        model.addAttribute("programmingLanguages", programmingLanguages);
        model.addAttribute("content", "exercises/create");
        return "layout";
    }

    @PostMapping("/create")
    @ResponseBody
    public ResponseEntity<?> createExercise(
            @Valid @ModelAttribute Exercise exercise, BindingResult result,
            @RequestParam("name") String name,
            @RequestParam("language") Integer languageId,
            @RequestParam(value = "setup_for_sql", required = false) String setupsql,
            @RequestParam("description") String description,
            @RequestParam("setup") String setup,
            @RequestParam("level") String level,
            @RequestParam("testCaseMethod") String testCaseMethod,
            @RequestParam(value = "testCasesJson", required = false) String testCasesJson,
            @RequestParam Map<String, String> allParams,
            HttpServletRequest httpServletRequest) {

        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body("Validation error: " + result.getAllErrors());
        }

        Optional<ProgrammingLanguage> languageOpt = programmingLanguageService.getProgrammingLanguageById(languageId);
        if (languageOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid programming language");
        }

        ProgrammingLanguage language = languageOpt.get();
        exercise.setLanguage(language);
        exercise.setName(name);
        exercise.setDescription(description);
        exercise.setSetup(setup);
        exercise.setLevel(Exercise.Level.valueOf(level));

        if ("SQL".equalsIgnoreCase(language.getLanguage())) {
            exercise.setSetupsql(setupsql);
        } else {
            exercise.setSetupsql(null);
        }

        List<TestCase> testCasesFinal = new ArrayList<>();

        if ("json".equalsIgnoreCase(testCaseMethod)) {
            if (testCasesJson == null || testCasesJson.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("TestCases JSON is EMPTY!");
            }

            try {
                ObjectMapper objectMapper = new ObjectMapper();
                List<Map<String, Object>> testCaseJsonList = objectMapper.readValue(testCasesJson, new TypeReference<>() {});

                for (Map<String, Object> tcMap : testCaseJsonList) {
                    TestCase tc = new TestCase();
                    tc.setInput((String) tcMap.get("input"));
                    tc.setExpectedOutput((String) tcMap.get("expectedOutput"));
                    tc.setHidden(Boolean.parseBoolean(String.valueOf(tcMap.getOrDefault("hidden", "false")))); // Dùng "hidden" thay vì "isHidden"
                    tc.setExercise(exercise);

                    if ("SQL".equalsIgnoreCase(language.getLanguage())) {
                        tc.setSqlTagNumber((String) tcMap.get("sqlTagNumber"));
                    } else {
                        tc.setSqlTagNumber(null);
                    }

                    testCasesFinal.add(tc);
                }
            } catch (JsonProcessingException e) {
                return ResponseEntity.badRequest().body("Invalid JSON format! Check console for details.");
            }
        } else if ("ui".equalsIgnoreCase(testCaseMethod)) {
            int normalIndex = 0;
            while (allParams.containsKey("testCaseFormList.testCasesList[" + normalIndex + "].input")) {
                String input = allParams.get("testCaseFormList.testCasesList[" + normalIndex + "].input");
                String expectedOutput = allParams.get("testCaseFormList.testCasesList[" + normalIndex + "].expectedOutput");
                String sqlTagNumber = allParams.get("testCaseFormList.testCasesList[" + normalIndex + "].sqlTagNumber");

                if (input != null && !input.trim().isEmpty() && expectedOutput != null && !expectedOutput.trim().isEmpty()) {
                    TestCase tc = new TestCase();
                    tc.setInput(input);
                    tc.setExpectedOutput(expectedOutput);
                    tc.setHidden(false);
                    tc.setExercise(exercise);
                    if ("SQL".equalsIgnoreCase(language.getLanguage())) {
                        tc.setSqlTagNumber(sqlTagNumber);
                    }
                    testCasesFinal.add(tc);
                }
                normalIndex++;
            }

            int hiddenIndex = 0;
            while (allParams.containsKey("hiddenTestCases[" + hiddenIndex + "].input")) {
                String input = allParams.get("hiddenTestCases[" + hiddenIndex + "].input");
                String expectedOutput = allParams.get("hiddenTestCases[" + hiddenIndex + "].expectedOutput");
                String sqlTagNumber = allParams.get("hiddenTestCases[" + hiddenIndex + "].sqlTagNumber");

                if (input != null && !input.trim().isEmpty() && expectedOutput != null && !expectedOutput.trim().isEmpty()) {
                    TestCase tc = new TestCase();
                    tc.setInput(input);
                    tc.setExpectedOutput(expectedOutput);
                    tc.setHidden(true);
                    tc.setExercise(exercise);
                    if ("SQL".equalsIgnoreCase(language.getLanguage())) {
                        tc.setSqlTagNumber(sqlTagNumber);
                    }
                    testCasesFinal.add(tc);
                }
                hiddenIndex++;
            }

            if (testCasesFinal.isEmpty()) {
                return ResponseEntity.badRequest().body("At least one valid test case is required!");
            }
        } else {
            return ResponseEntity.badRequest().body("Invalid Test Case Method!");
        }

        exercise.setTestCases(testCasesFinal);

        try {
            exerciseService.saveExercise(exercise);
            return ResponseEntity.ok("Exercise & Test Cases Saved!");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error saving exercise: " + e.getMessage());
        }
    }
    // Show the edit form for a specific exercise
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Exercise exercise = exerciseService.getExerciseById(id).orElse(null);

        if (exercise == null) {
            return "redirect:/exercises"; // Redirect nếu không tìm thấy
        }

        List<ProgrammingLanguage> programmingLanguages = programmingLanguageService.getAllProgrammingLanguages();
        ObjectMapper objectMapper = new ObjectMapper();
        String testCasesJson = "[]";
        try {
            testCasesJson = objectMapper.writeValueAsString(exercise.getTestCases());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        List<TestCase> normalTestCases = testCaseService.findVisibleTestCases(id);
        List<TestCase> hiddenTestCases = testCaseService.findHiddenTestCases(id);

        model.addAttribute("exercise", exercise);
        model.addAttribute("programmingLanguages", programmingLanguages);
        model.addAttribute("testCasesJson", testCasesJson);
        model.addAttribute("testCases", exercise.getTestCases());
        model.addAttribute("normalTestCases", normalTestCases);
        model.addAttribute("hiddenTestCases", hiddenTestCases);
        model.addAttribute("content", "exercises/edit");

        return "layout";
    }

    @PostMapping("/edit/{id}")
    public String updateExercise(
            @PathVariable Long id,
            @ModelAttribute Exercise exercise,
            @RequestParam(name = "testCaseMethod") String testCaseMethod, // Thêm để xác định phương thức
            @RequestParam(name = "testCasesJson", required = false) String testCasesJson,
            @RequestParam Map<String, String> allParams, // Lấy dữ liệu từ UI
            Model model) {

        Exercise existingExercise = exerciseService.getExerciseById(id).orElse(null);
        if (existingExercise == null) {
            return "redirect:/exercises"; // Nếu không tìm thấy thì redirect
        }

        // Kiểm tra và cập nhật tên bài tập
        if (!exercise.getName().equals(existingExercise.getName())) {
            if (exerciseService.existsByTitleExcludingId(exercise.getName(), id)) {
                model.addAttribute("error", "Exercise name already exists!");
                model.addAttribute("programmingLanguages", programmingLanguageService.getAllProgrammingLanguages());
                model.addAttribute("testCasesJson", testCasesJson);
                model.addAttribute("content", "exercises/edit");
                return "layout";
            }
            existingExercise.setName(exercise.getName());
        }

        // Cập nhật các thông tin khác
        existingExercise.setDescription(exercise.getDescription());
        existingExercise.setSetup(exercise.getSetup());
        existingExercise.setLevel(exercise.getLevel());
        // existingExercise.setLanguage(exercise.getLanguage()); // Đảm bảo ánh xạ language từ form

        if (exercise.getLanguage().getLanguage().equals("SQL")) {
            existingExercise.setSetupsql(exercise.getSetupsql());
        } else {
            existingExercise.setSetupsql(null);
        }

        List<TestCase> normalTestCases = new ArrayList<>();
        List<TestCase> hiddenTestCases = new ArrayList<>();

        // Xử lý test cases dựa trên phương thức
        if ("json".equalsIgnoreCase(testCaseMethod)) {
            // Xử lý khi chọn "Enter as JSON"
            if (testCasesJson != null && !testCasesJson.trim().isEmpty()) {
                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    List<TestCase> testCases = objectMapper.readValue(testCasesJson, new TypeReference<List<TestCase>>() {
                    });
                    for (TestCase testCase : testCases) {
                        testCase.setExercise(existingExercise);
                        if(testCase.isHidden()){
                            hiddenTestCases.add(testCase);
                        }
                        else{
                            normalTestCases.add(testCase);
                        }
                    }
                    existingExercise.setTestCases(testCases);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                    model.addAttribute("error", "Invalid JSON format for test cases!");
                    model.addAttribute("programmingLanguages", programmingLanguageService.getAllProgrammingLanguages());
                    model.addAttribute("testCasesJson", testCasesJson);
                    model.addAttribute("content", "exercises/edit");
                    return "layout";
                }
            }
        } else if ("ui".equalsIgnoreCase(testCaseMethod)) {
            // Xử lý khi nhập thủ công
            int normalIndex = 0;
            while (allParams.containsKey("normalTestCases[" + normalIndex + "].input")) {
                String input = allParams.get("normalTestCases[" + normalIndex + "].input");
                String expectedOutput = allParams.get("normalTestCases[" + normalIndex + "].expectedOutput");
                String sqlTagNumber = allParams.get("normalTestCases[" + normalIndex + "].sqlTagNumber");

                if (input != null && !input.trim().isEmpty() && expectedOutput != null && !expectedOutput.trim().isEmpty()) {
                    TestCase testCase = new TestCase();
                    testCase.setInput(input);
                    testCase.setExpectedOutput(expectedOutput);
                    testCase.setExercise(existingExercise);
                    testCase.setSqlTagNumber(sqlTagNumber);
                    testCase.setHidden(false);

                    normalTestCases.add(testCase);
                }
                normalIndex++;
            }

// For hidden test cases
            int hiddenIndex = 0;
            while (allParams.containsKey("hiddenTestCases[" + hiddenIndex + "].input")) {
                String input = allParams.get("hiddenTestCases[" + hiddenIndex + "].input");
                String expectedOutput = allParams.get("hiddenTestCases[" + hiddenIndex + "].expectedOutput");
                String sqlTagNumber = allParams.get("hiddenTestCases[" + hiddenIndex + "].sqlTagNumber");

                if (input != null && !input.trim().isEmpty() && expectedOutput != null && !expectedOutput.trim().isEmpty()) {
                    TestCase testCase = new TestCase();
                    testCase.setInput(input);
                    testCase.setExpectedOutput(expectedOutput);
                    testCase.setExercise(existingExercise);
                    testCase.setSqlTagNumber(sqlTagNumber);
                    testCase.setHidden(true);

                    hiddenTestCases.add(testCase);
                }
                hiddenIndex++;
            }
        }

        // Lưu bài tập đã cập nhật
        List<TestCase> allTestCases = new ArrayList<>();
        allTestCases.addAll(normalTestCases);
        allTestCases.addAll(hiddenTestCases);
        existingExercise.setTestCases(allTestCases);
        exercise.setLanguage(existingExercise.getLanguage());
        exerciseService.saveExercise(existingExercise);

        return "redirect:/exercises";
    }


    // Delete a specific exercise
    @GetMapping("/delete/{id}")
    public String deleteExercise(@PathVariable Long id) {
        exerciseService.deleteExercise(id);
        return "redirect:/exercises";
    }

    @PostMapping("/delete-batch")
    public ResponseEntity<?> deleteExercises(@RequestBody Map<String, List<Long>> request) {
        List<Long> ids = request.get("ids");

        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().body("No IDs provided for deletion.");
        }

        exerciseService.deleteExercisesByIds(ids);
        return ResponseEntity.ok().body("Deleted successfully.");
    }

    @PostMapping("/import")
    public String uploadExercisesData(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        try {
            exerciseService.saveExercisesToDatabase(file);
            redirectAttributes.addFlashAttribute("success", "Import successful!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Import failed: " + e.getMessage());
        }

        return "redirect:/exercises";
    }

    // Print roles page
    @GetMapping("/print")
    public String print(Model model) {
        List<Exercise> exercises = exerciseService.findAllExercises();
        model.addAttribute("exercises", exercises);
        return "exercises/print";
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExercisesToExcel() throws IOException {
        List<Exercise> exercises = exerciseService.findAllExercises();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Exercises");

        // Tạo tiêu đề
        Row headerRow = sheet.createRow(0);
        String[] columns = {"ID", "Title", "Language", "Level", "Description", "Set Up Code", "Test Case", "Hidden Test Case"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            cell.setCellStyle(headerStyle);
        }

        // Ghi dữ liệu từ danh sách
        int rowIdx = 1;
        for (Exercise exercise : exercises) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(exercise.getId());
            row.createCell(1).setCellValue(exercise.getName());
            row.createCell(2).setCellValue(exercise.getLanguage().getLanguage());
            row.createCell(3).setCellValue(String.valueOf(exercise.getLevel()));
            row.createCell(4).setCellValue(exercise.getDescription());
            row.createCell(5).setCellValue(exercise.getSetup());
// Concatenate test cases
//            StringBuilder testCaseText = new StringBuilder();
              StringBuilder visibleTestCases = new StringBuilder();
              StringBuilder hiddenTestCases = new StringBuilder();

            for (TestCase testCase : exercise.getTestCases()) {
//                testCaseText.append("Input: ").append(testCase.getInput())
//                        .append(", Expected: ").append(testCase.getExpectedOutput())
//                        .append(";")
//                        .append("\n");
                String testCaseEntry = "Input: " + testCase.getInput() +
                        ", Expected: " + testCase.getExpectedOutput() + ";\n";

                if (testCase.isHidden()) {
                    hiddenTestCases.append(testCaseEntry);
                } else {
                    visibleTestCases.append(testCaseEntry);
                }
            }

            // Set the concatenated string in the cell
            row.createCell(6).setCellValue(visibleTestCases.toString().trim());
            row.createCell(7).setCellValue(hiddenTestCases.toString().trim());
        }

        // Xuất dữ liệu ra mảng byte
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        byte[] excelBytes = outputStream.toByteArray();
        String timeStamp = new SimpleDateFormat("dd_MM_yyyy hh-mm a").format(new Date());
        String fileName = "exercises_" + timeStamp + ".xlsx";

        // Trả về file Excel
        return ResponseEntity.ok()
//                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=exercises.xlsx")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelBytes);

    }


    // Export exercises to an Excel file
//    @GetMapping("/export")
//    public ResponseEntity<InputStreamResource> exportExercises() {
//        // Fetch all exercises (page size set to max to get all records)
//        List<Exercise> exercises = exerciseService.findAllExercises();
//
//        // Convert exercises to an Excel file
//        ByteArrayInputStream excelFile = exerciseService.exportExercisesToExcel(exercises);
//
//        // Create headers for the response (Content-Disposition to trigger file download)
//        HttpHeaders headers = new HttpHeaders();
//        headers.add("Content-Disposition", "attachment; filename=exercises.xlsx");
//
//        // Return the file in the response
//        return ResponseEntity.ok()
//                .headers(headers)
//                .body(new InputStreamResource(excelFile));
//    }
//
//    // Import exercises from an Excel file
//    @PostMapping("/import")
//    public String importModules(@RequestParam("file") MultipartFile file) {
//        exerciseService.importExcel(file);
//        return "redirect:/modules";
//    }

}
