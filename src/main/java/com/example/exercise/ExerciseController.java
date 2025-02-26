package com.example.exercise;

import com.example.assessment.model.ProgrammingLanguage;
import com.example.assessment.service.ProgrammingLanguageService;
import com.example.testcase.*;
import com.fasterxml.jackson.core.JsonProcessingException;
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
            @ModelAttribute TestCaseRequest request,
            @RequestParam("name") String name,
            @RequestParam("language") Long language,
            @RequestParam("description") String description,
            @RequestParam("setup") String setup,
            @RequestParam("level") String level, HttpServletRequest httpServletRequest) {

        System.out.println("📩 New Request received: " + exercise.getName() + " | IP: " + httpServletRequest.getRemoteAddr());

        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body("Validation error: " + result.getAllErrors());
        }

        if (exercise == null) {
            return ResponseEntity.badRequest().body("Exercise cannot be null");
        }

        List<TestCase> testCasesFinal = new ArrayList<>();

        if ("json".equalsIgnoreCase(request.getTestCaseMethod())) {
            if (request.getTestCasesJson() == null || request.getTestCasesJson().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("TestCases JSON is EMPTY!");
            }

            ObjectMapper objectMapper = new ObjectMapper();
            try {
                List<TestCaseForm> testCaseForms = objectMapper.readValue(request.getTestCasesJson(), new TypeReference<List<TestCaseForm>>() {});
                if (testCaseForms.isEmpty()) {
                    return ResponseEntity.badRequest().body("No Test Cases in JSON!");
                }
                for (TestCaseForm tcForm : testCaseForms) {
                    TestCase tc = new TestCase();
                    tc.setInput(tcForm.getInput());
                    tc.setExpectedOutput(tcForm.getExpectedOutput());
                    tc.setExercise(exercise);
                    testCasesFinal.add(tc);
                }
            } catch (Exception e) {
                return ResponseEntity.badRequest().body("Invalid JSON format!");
            }
        } else if ("ui".equalsIgnoreCase(request.getTestCaseMethod())) {
            if (request.getTestCaseFormList() == null || request.getTestCaseFormList().getTestCasesList() == null || request.getTestCaseFormList().getTestCasesList().isEmpty()) {
                return ResponseEntity.badRequest().body("TestCasesList is NULL or EMPTY!");
            }

            for (TestCaseForm tcForm : request.getTestCaseFormList().getTestCasesList()) {
                if (tcForm.getInput() == null || tcForm.getInput().trim().isEmpty()
                        || tcForm.getExpectedOutput() == null || tcForm.getExpectedOutput().trim().isEmpty()) {
                    return ResponseEntity.badRequest().body("One or more Test Cases are invalid!");
                }
                TestCase tc = new TestCase();
                tc.setInput(tcForm.getInput());
                tc.setExpectedOutput(tcForm.getExpectedOutput());
                tc.setExercise(exercise);
                testCasesFinal.add(tc);
            }
        } else {
            return ResponseEntity.badRequest().body("Invalid Test Case Method!");
        }

        exercise.setTestCases(new ArrayList<>());
        exercise.setTestCases(testCasesFinal);
        exercise.setName(name);
        exercise.setDescription(description);
        exercise.setSetup(setup);
        exercise.setLevel(Exercise.Level.valueOf(level));

        exerciseService.saveExercise(exercise);

        return ResponseEntity.ok("Exercise & Test Cases Saved!");
    }



    // Show the edit form for a specific exercise
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Exercise exercise = exerciseService.getExerciseById(id).orElse(null);

        if (exercise == null) {
            return "redirect:/exercises"; // Redirect if the exercise doesn't exist
        }

        // Fetch all programming languages for the dropdown
        List<ProgrammingLanguage> programmingLanguages = programmingLanguageService.getAllProgrammingLanguages();
//        System.out.println("test case test"+exercise.getTestCases());
        // Chuyển danh sách testCases thành chuỗi JSON
        ObjectMapper objectMapper = new ObjectMapper();
        String testCasesJson = "[]"; // Mặc định nếu không có test case nào

        try {
            testCasesJson = objectMapper.writeValueAsString(exercise.getTestCases());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        model.addAttribute("exercise", exercise); // Add the current exercise to the model
        model.addAttribute("programmingLanguages", programmingLanguages); // Add the languages to the model
        model.addAttribute("testCasesJson", testCasesJson); // Thêm test case JSON vào model
        model.addAttribute("testCases", exercise.getTestCases()); // Gửi danh sách test case dạng List
        model.addAttribute("content", "exercises/edit");

        return "layout";
    }

    @PostMapping("/edit/{id}")
    public String updateExercise(
            @PathVariable Long id,
            @ModelAttribute Exercise exercise,
            @RequestParam(name = "testCasesJson", required = false) String testCasesJson,
            Model model) {

        Exercise existingExercise = exerciseService.getExerciseById(id).orElse(null);
        if (existingExercise == null) {
            return "redirect:/exercises"; // Nếu không tìm thấy thì redirect
        }

        // 🟢 Kiểm tra nếu user không sửa title
        if (!exercise.getName().equals(existingExercise.getName())) {
            // 🟠 Chỉ kiểm tra trùng lặp nếu title thay đổi
            if (exerciseService.existsByTitleExcludingId(exercise.getName(), id)) {
                model.addAttribute("error", "Exercise name already exists!");
                model.addAttribute("content", "exercises/edit");
                return "layout";
            }
            // Cập nhật title nếu hợp lệ
            existingExercise.setName(exercise.getName());
        }

        // Cập nhật các thông tin khác
        existingExercise.setDescription(exercise.getDescription());
        existingExercise.setSetup(exercise.getSetup());
        existingExercise.setLevel(exercise.getLevel());

        // Xóa tất cả test cases cũ trước khi thêm mới
        existingExercise.getTestCases().clear();

        // Xử lý test cases từ JSON
        if (testCasesJson != null && !testCasesJson.isEmpty()) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                List<TestCase> testCases = objectMapper.readValue(testCasesJson, new TypeReference<List<TestCase>>() {});
                for (TestCase testCase : testCases) {
                    testCase.setExercise(existingExercise);
                }
                existingExercise.setTestCases(testCases);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        // Lưu bài tập đã cập nhật
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
        String[] columns = {"ID", "Title", "Language", "Level", "Description", "Set Up Code", "Test Case"};
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
            StringBuilder testCaseText = new StringBuilder();
            for (TestCase testCase : exercise.getTestCases()) {
                testCaseText.append("Input: ").append(testCase.getInput())
                        .append(", Expected: ").append(testCase.getExpectedOutput())
                        .append(";")
                        .append("\n");
            }

            // Set the concatenated string in the cell
            row.createCell(6).setCellValue(testCaseText.toString().trim());
        }

        // Xuất dữ liệu ra mảng byte
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        byte[] excelBytes = outputStream.toByteArray();
        String timeStamp = new SimpleDateFormat("dd_MM_yyyy").format(new Date());
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
