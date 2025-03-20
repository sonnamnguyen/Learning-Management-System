package com.example.exercise.controller;

import com.example.assessment.model.ProgrammingLanguage;
import com.example.assessment.service.ProgrammingLanguageService;
import com.example.exercise.model.*;
import com.example.exercise.repository.ExerciseRepository;
import com.example.exercise.service.CategoryService;
import com.example.exercise.service.ExerciseCategoryService;
import com.example.exercise.service.ExerciseService;
import com.example.exercise.service.StudentExerciseAttemptService;
import com.example.testcase.*;
import com.example.user.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
import java.util.stream.Collectors;

@Controller
@RequestMapping("/exercises")
@RequiredArgsConstructor
public class ExerciseController {
    private final ProgrammingLanguageService programmingLanguageService;
    private final ExerciseRepository exerciseRepository;
    private final ExerciseService exerciseService;
    private final TestCaseService testCaseService;
    private final StudentExerciseAttemptService studentExerciseAttemptService;
    private final UserService userService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ExerciseCategoryService exerciseCategoryService;


    // Common attributes for all views

    @ModelAttribute
    public void addCommonAttributes(Model model) {
        model.addAttribute("title", "Exercises");
        model.addAttribute("links", "/style.css");
        model.addAttribute("links", "/print.css");
    }


    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN', 'STUDENT')")
    public String getList(
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "language", required = false) Long languageId,
            @RequestParam(value = "level", required = false) String level,
            @RequestParam(value = "description", required = false) String descriptionKeyword,
            @RequestParam(value = "tags", required = false) List<Long> tagIds,
            @RequestParam(value = "page", defaultValue = "0") int page,
            Model model, Authentication authentication) {

        Pageable pageable = PageRequest.of(page, 12); // 12 items per page
        Page<Exercise> exercisesPage;
        List<Exercise> exercises;
        int totalPages;

        // Logic filtering
        if (title != null && !title.isEmpty()) {
            exercisesPage = exerciseService.searchExercises(title, pageable);
        } else {
            if (tagIds != null && !tagIds.isEmpty()) {
                exercisesPage = exerciseService.getExercisesByFilters(languageId, level, tagIds, pageable);
            } else {
                exercisesPage = exerciseService.getExercisesByLanguageAndLevel(languageId, level, pageable);
            }
            exercises = exercisesPage.getContent();
            totalPages = exercisesPage.getTotalPages();
        }

        exercises = exercisesPage.getContent();
        totalPages = exercisesPage.getTotalPages();

        // Add attributes to model
        model.addAttribute("exercises", exercises);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("languages", programmingLanguageService.findAll());
        model.addAttribute("currentLanguage", languageId);
        model.addAttribute("currentLevel", level);
        model.addAttribute("paramTitle", title);
        model.addAttribute("tags", categoryService.getAllCategory());
        model.addAttribute("paramDescription", descriptionKeyword);

        // Calculate common words if languageId is provided
        if (languageId != null) {
            Page<Exercise> allExercises;
            if (level != null && !level.isEmpty()) {
                allExercises = exerciseService.getExercisesByLanguageAndLevel(languageId, level, Pageable.unpaged());
            } else {
                allExercises = exerciseService.getExercisesByLanguage(languageId, Pageable.unpaged());
            }
            model.addAttribute("commonWords", getTfIdfScores(allExercises.getContent()));
        }
        Map<String, List<Exercise>> duplicates = exerciseService.findDuplicates();
        if (!duplicates.isEmpty()) {
            int totalDuplicates = duplicates.values().stream().mapToInt(List::size).sum();
            model.addAttribute("duplicateMessage",
                    "There are " + totalDuplicates + " duplicate exercises found. " +
                            "<a href=\"/exercises/check-duplicates\" class=\"alert-link\">View duplicates</a>");
        }
        System.out.println("Language ID: " + languageId + ", Level: " + level +
                ", Title: " + title + ", Description: " + descriptionKeyword);
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ADMIN") || auth.getAuthority().equals("SUPERADMIN"));
        return isAdmin ? "exercises/list" : "exercises/student-list";
    }

    @GetMapping("/check-duplicates")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN', 'STUDENT')")
    public String checkDuplicates(
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "language", required = false) Long languageId,
            @RequestParam(value = "level", required = false) String level,
            @RequestParam(value = "description", required = false) String descriptionKeyword,
            Model model, Authentication authentication, RedirectAttributes redirectAttributes) {

        try {
            System.out.println("Received parameters - title: " + title + ", languageId: " + languageId +
                    ", level: " + level + ", description: " + descriptionKeyword);

            // Kiểm tra dữ liệu đầu vào
            if (level != null && !level.isEmpty()) {
                if (!isValidLevel(level)) {
                    redirectAttributes.addFlashAttribute("error", "Invalid level value: " + level + ". Valid values are: EASY, MEDIUM, HARD.");
                    return "redirect:/exercises";
                }
            }

            // Lấy toàn bộ dữ liệu từ database mà không phân trang
            List<Exercise> allExercises;
            if (title != null && !title.trim().isEmpty()) {
                allExercises = exerciseService.searchExercisesAll(title.trim());
            } else if (descriptionKeyword != null && !descriptionKeyword.trim().isEmpty()) {
                if (languageId != null && level != null && !level.isEmpty()) {
                    allExercises = exerciseService.searchByDescriptionAndLanguageAndLevelAll(descriptionKeyword.trim(), languageId, level);
                } else if (languageId != null) {
                    allExercises = exerciseService.searchByDescriptionAndLanguageAll(descriptionKeyword.trim(), languageId);
                } else if (level != null && !level.isEmpty()) {
                    allExercises = exerciseService.searchByDescriptionAndLevelAll(descriptionKeyword.trim(), level);
                } else {
                    allExercises = exerciseService.searchByDescriptionAll(descriptionKeyword.trim());
                }
            } else {
                if (languageId != null && level != null && !level.isEmpty()) {
                    allExercises = exerciseService.getExercisesByLanguageAndLevelAll(languageId, level);
                } else if (languageId != null) {
                    allExercises = exerciseService.getExercisesByLanguageAll(languageId);
                } else {
                    allExercises = exerciseService.getAllExercisesAll();
                }
            }

            // Kiểm tra nếu không có dữ liệu
            if (allExercises == null || allExercises.isEmpty()) {
                model.addAttribute("error", "No exercises found with the given filters.");
                model.addAttribute("languages", programmingLanguageService.findAll());
                model.addAttribute("currentLanguage", languageId);
                model.addAttribute("currentLevel", level);
                model.addAttribute("paramTitle", title != null ? title.trim() : null);
                model.addAttribute("paramDescription", descriptionKeyword != null ? descriptionKeyword.trim() : null);
                boolean isAdmin = authentication.getAuthorities().stream()
                        .anyMatch(auth -> auth.getAuthority().equals("ADMIN") || auth.getAuthority().equals("SUPERADMIN"));
                return isAdmin ? "exercises/check-duplicates" : "exercises/student-check-duplicates";
            }

            // Nhóm bài tập theo ngôn ngữ trước
            Map<String, List<Exercise>> exercisesByLanguage = allExercises.stream()
                    .collect(Collectors.groupingBy(exercise -> exercise.getLanguage().getLanguage()));

            // Nhóm bài tập trùng lặp theo mô tả trong từng ngôn ngữ
            Map<String, Map<String, List<Exercise>>> groupedByLanguageAndDescription = new HashMap<>();
            exercisesByLanguage.forEach((language, exercises) -> {
                Map<String, List<Exercise>> duplicates = findDuplicates(exercises);
                groupedByLanguageAndDescription.put(language, duplicates);
            });


            // Thêm các thuộc tính vào model
            model.addAttribute("groupedByLanguageAndDescription", groupedByLanguageAndDescription);
            model.addAttribute("exercises", allExercises);
            model.addAttribute("languages", programmingLanguageService.findAll());
            model.addAttribute("currentLanguage", languageId);
            model.addAttribute("currentLevel", level);
            model.addAttribute("paramTitle", title != null ? title.trim() : null);
            model.addAttribute("paramDescription", descriptionKeyword != null ? descriptionKeyword.trim() : null);

            // Calculate common words if languageId is provided
            if (languageId != null) {
                List<Exercise> filteredExercises;
                if (level != null && !level.isEmpty()) {
                    filteredExercises = exerciseService.getExercisesByLanguageAndLevelAll(languageId, level);
                } else {
                    filteredExercises = exerciseService.getExercisesByLanguageAll(languageId);
                }
                model.addAttribute("commonWords", getTfIdfScores(filteredExercises));
            }

            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ADMIN") || auth.getAuthority().equals("SUPERADMIN"));
            return isAdmin ? "exercises/check-duplicates" : "exercises/student-check-duplicates";

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error occurred while checking duplicates: " + e.getMessage());
            return "redirect:/exercises";
        }
    }

    // Phương thức phụ để kiểm tra giá trị level hợp lệ
    private boolean isValidLevel(String level) {
        try {
            Exercise.Level.valueOf(level.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // Method phụ để tìm các bài tập trùng lặp
    private Map<String, List<Exercise>> findDuplicates(List<Exercise> exercises) {
        Map<String, List<Exercise>> duplicateGroups = new HashMap<>();
        List<Exercise> processedExercises = new ArrayList<>();

        // Chuẩn hóa description và lưu vào processedExercises
        for (Exercise exercise : exercises) {
            if (exercise.getDescription() != null && !exercise.getDescription().trim().isEmpty()) {
                String normalizedDesc = exercise.getDescription()
                        .toLowerCase()
                        .replaceAll("[^a-zA-Z\\s]", "") // Bỏ ký tự đặc biệt
                        .replaceAll("\\s+", " ") // Chuẩn hóa khoảng trắng
                        .trim();
                exercise.setDescription(normalizedDesc); // Tạm thời lưu description chuẩn hóa
                processedExercises.add(exercise);
            }
        }

        // So sánh từng cặp Exercise để tìm độ tương đồng
        for (int i = 0; i < processedExercises.size(); i++) {
            Exercise e1 = processedExercises.get(i);
            String desc1 = e1.getDescription();
            Set<String> words1 = new HashSet<>(Arrays.asList(desc1.split("\\s+")));
            String groupKey = null;

            // Kiểm tra xem e1 đã thuộc nhóm nào chưa
            for (Map.Entry<String, List<Exercise>> entry : duplicateGroups.entrySet()) {
                List<Exercise> group = entry.getValue();
                Exercise representative = group.get(0); // Lấy bài tập đầu tiên trong nhóm làm đại diện
                Set<String> words2 = new HashSet<>(Arrays.asList(representative.getDescription().split("\\s+")));

                double similarity = calculateJaccardSimilarity(words1, words2);
                if (similarity >= 0.7) { // Ngưỡng 70%
                    groupKey = entry.getKey();
                    break;
                }
            }

            // Nếu không tìm thấy nhóm nào tương đồng, tạo nhóm mới
            if (groupKey == null) {
                groupKey = desc1; // Dùng description của bài tập đầu tiên làm key
                duplicateGroups.put(groupKey, new ArrayList<>());
            }

            // Thêm bài tập vào nhóm
            duplicateGroups.get(groupKey).add(e1);
        }

        // Lọc chỉ giữ lại các nhóm có từ 2 bài tập trở lên
        return duplicateGroups.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    // Phương thức tính Jaccard Similarity
    private double calculateJaccardSimilarity(Set<String> set1, Set<String> set2) {
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2); // Giao của 2 tập hợp

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2); // Hợp của 2 tập hợp

        if (union.isEmpty()) return 0.0; // Tránh chia cho 0
        return (double) intersection.size() / union.size();
    }

    private Map<String, Double> getTfIdfScores(List<Exercise> exercises) {
        // Bước 1: Tạo danh sách các chuỗi (documents) và tính document frequency
        List<String> documents = new ArrayList<>();
        Map<String, Integer> documentFrequency = new HashMap<>(); // Đếm số tài liệu chứa chuỗi

        // Xử lý từng Exercise
        for (Exercise exercise : exercises) {
            if (exercise.getDescription() != null) {
                String description = exercise.getDescription()
                        .toLowerCase()
                        .replaceAll("[^a-zA-Z\\s]", "")
                        .trim();
                String[] words = description.split("\\s+");
                int originalLength = words.length;

                // Giữ nguyên chuỗi nếu độ dài > 0
                if (originalLength > 0) {
                    String fullSequence = String.join(" ", words);
                    documents.add(fullSequence);

                    // Tính document frequency (IDF)
                    documentFrequency.put(fullSequence, documentFrequency.getOrDefault(fullSequence, 0) + 1);
                }
            }
        }

        int totalDocuments = documents.size();
        Map<String, Double> tfIdfScores = new HashMap<>();

        // Bước 2: Tính TF-IDF cho từng chuỗi, chỉ lấy chuỗi xuất hiện từ 2 lần trở lên
        for (String document : documents) {
            int df = documentFrequency.getOrDefault(document, 1); // Số tài liệu chứa chuỗi
            if (df < 2) { // Bỏ qua nếu chuỗi chỉ xuất hiện 1 lần
                continue;
            }

            Map<String, Integer> termFrequency = new HashMap<>();
            termFrequency.put(document, 1); // TF = 1 vì mỗi chuỗi chỉ xuất hiện 1 lần trong tài liệu của nó

            // Tính TF-IDF cho chuỗi này
            for (Map.Entry<String, Integer> entry : termFrequency.entrySet()) {
                String sequence = entry.getKey();
                int tf = entry.getValue(); // TF = 1
                double idf = Math.log((double) totalDocuments / df); // IDF = log(N / df)
                double tfIdf = tf * idf;

                // Lưu giá trị TF-IDF cao nhất cho chuỗi này
                tfIdfScores.put(sequence, Math.max(tfIdfScores.getOrDefault(sequence, 0.0), tfIdf));
            }
        }

        // Bước 3: Sắp xếp và lấy top 10 chuỗi có TF-IDF cao nhất
        return tfIdfScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    private Map<String, Integer> getWordFrequency(List<Exercise> exercises) {
        Map<String, Integer> wordCount = new HashMap<>();

        for (Exercise exercise : exercises) {
            if (exercise.getDescription() != null) {
                String[] words = exercise.getDescription()
                        .toLowerCase()
                        .replaceAll("[^a-zA-Z\\s]", "")
                        .split("\\s+");

                for (String word : words) {
                    if (!word.isEmpty() && word.length() > 2) {
                        wordCount.put(word, wordCount.getOrDefault(word, 0) + 1);
                    }
                }
            }
        }

        return wordCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('SUPERADMIN')")
    @GetMapping("/new-dashboard")
    public String showDashboard(@RequestParam(value = "languageId", required = false) Long languageId,Model model, Authentication authentication, RedirectAttributes redirectAttributes) {
        // Lấy dữ liệu thống kê từ ExerciseService
        int newExercises = exerciseService.countNewExercises(languageId);
        double completionRate = exerciseService.getCompletionRate(languageId);
        int totalExercises = exerciseService.countTotalExercises(languageId);
        int totalLanguages = programmingLanguageService.countTotalLanguages();
        int upcomingWorkouts = exerciseService.countUpcomingWorkouts(languageId);
        int missedWorkouts = exerciseService.countMissedWorkouts(languageId);

        List<ProgrammingLanguage> languages = programmingLanguageService.getAllProgrammingLanguages();

        // Thêm dữ liệu vào model
        model.addAttribute("newExercises", newExercises);
        model.addAttribute("completionRate", completionRate);
        model.addAttribute("totalExercises", totalExercises);
        model.addAttribute("totalLanguages", totalLanguages);
        model.addAttribute("upcomingWorkouts", upcomingWorkouts);
        model.addAttribute("missedWorkouts", missedWorkouts);
        model.addAttribute("languages", programmingLanguageService.findAll());
        model.addAttribute("languages", languages);
        model.addAttribute("selectedLanguage", languageId);
        model.addAttribute("content", "exercises/new-dashboard");

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ADMIN") || auth.getAuthority().equals("SUPERADMIN"));
//        if (!isAdmin) {
//            Long id = userService.getCurrentUser().getId();
//            return "redirect:/exercises/profile/" + id;
//        }
        return "exercises/dashboard-admin";
    }

    @PreAuthorize("hasAuthority('STUDENT')")
    @GetMapping("/student-dashboard")
    public String showStudentDashboard(Model model) {
        Long id = userService.getCurrentUser().getId();
        return "redirect:/exercises/profile/" + id;
    }

    @GetMapping("/exercises/stats")
    public ResponseEntity<Map<String, Integer>> getExerciseStats(@RequestParam(required = false) Long languageId) {
        return ResponseEntity.ok(exerciseService.getExerciseStatistics(languageId));
    }


    @GetMapping("/dashboard-data")
    @ResponseBody
    public Map<String, Object> getDashboardData(@RequestParam(value = "languageId", required = false) Long languageId) {
        Map<String, Object> response = new HashMap<>();

        //response.put("newExercises", exerciseService.countNewExercises(languageId));
        //response.put("completionRate", exerciseService.getCompletionRate(languageId));
        response.put("totalExercises", exerciseService.countTotalExercises(languageId));
        response.put("totalLanguages", programmingLanguageService.countTotalLanguages());

        // Lấy dữ liệu biểu đồ
//        Map<String, Integer> topicChartData = exerciseService.getExercisesByLanguage();
        // Xử lý dữ liệu cho biểu đồ
        Map<String, Integer> topicChartData = (languageId == null)
                ? exerciseService.getExercisesByLanguage()  // Lấy dữ liệu của tất cả ngôn ngữ
                : exerciseService.getExercisesByLanguage(languageId); // Lọc theo ngôn ngữ được chọn
        Map<String, Integer> difficultyChartData = exerciseService.getLevelDistribution(languageId);

        response.put("topicChartData", topicChartData != null ? topicChartData : new HashMap<>());
        response.put("difficultyChartData", difficultyChartData != null ? difficultyChartData : new HashMap<>());

        return response;
    }

    @GetMapping("/api/exercise-data")
    @ResponseBody
    public List<Map<String, Object>> getExerciseData() {
        List<Map<String, Object>> response = new ArrayList<>();

        List<Object[]> results = exerciseRepository.countExercisesByLevel(null); // Lấy dữ liệu tất cả ngôn ngữ
        for (Object[] row : results) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("month", row[0].toString());
            entry.put("completedExercises", ((Number) row[1]).intValue());
            response.add(entry);
        }
        return response;
    }

    @GetMapping("/api/difficulty-data")
    @ResponseBody
    public Map<String, Integer> getDifficultyData() {
        return exerciseService.getLevelDistribution(null);
    }

    @GetMapping("/api/language-data")
    @ResponseBody
    public Map<String, Integer> getLanguageData() {
        return exerciseService.getExercisesByLanguage();
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
        List<Category> categories = categoryService.getAllCategory();
        model.addAttribute("tags", categories);
        model.addAttribute("content", "exercises/create");
        return "layout";
    }



    @PostMapping("/create")
    @ResponseBody
    public ResponseEntity<?> createExercise(
            @Valid @ModelAttribute Exercise exercise, BindingResult result,
            @RequestParam("name") String name,
            @RequestParam("language") Integer languageId,
            @RequestParam("tag") List<Integer> tagIds, // Chấp nhận danh sách tag
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

        // Xử lý nhiều tag
        List<ExerciseCategory> exerciseCategories = new ArrayList<>();
        for (Integer tagId : tagIds) {
            Optional<Category> categoryOpt = categoryService.getCategoryById(tagId);
            if (categoryOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Invalid tag ID: " + tagId);
            }
            Category category = categoryOpt.get();
            ExerciseCategory exerciseCategory = new ExerciseCategory();
            exerciseCategory.setExercise(exercise);
            exerciseCategory.setCategory(category);
            exerciseCategories.add(exerciseCategory);
        }
        exercise.setExerciseCategories(exerciseCategories);

        List<TestCase> testCasesFinal = new ArrayList<>();

        if ("json".equalsIgnoreCase(testCaseMethod)) {
            if (testCasesJson == null || testCasesJson.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("TestCases JSON is EMPTY!");
            }
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                List<Map<String, Object>> testCaseJsonList = objectMapper.readValue(testCasesJson, new TypeReference<>() {
                });
                for (Map<String, Object> tcMap : testCaseJsonList) {
                    //moi
                    String input = (String) tcMap.get("input");
                    String expectedOutput = (String) tcMap.get("expectedOutput");

                    if (input == null || input.trim().isEmpty() || expectedOutput == null || expectedOutput.trim().isEmpty()) {
//                        return ResponseEntity.badRequest().body("Test case input/output cannot be empty!");
                        Map<String, String> errorResponse = new HashMap<>();
                        errorResponse.put("error", "Test case input/output cannot be empty!");
                        return ResponseEntity.badRequest().body(errorResponse);
                    }
                    //moi

                    TestCase tc = new TestCase();
                    tc.setInput((String) tcMap.get("input"));
                    tc.setExpectedOutput((String) tcMap.get("expectedOutput"));
                    tc.setHidden(Boolean.parseBoolean(String.valueOf(tcMap.getOrDefault("hidden", "false"))));
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
            //moi
            if ("ui".equalsIgnoreCase(testCaseMethod) && testCasesFinal.isEmpty()) {
                return ResponseEntity.badRequest().body("At least one valid test case is required!");
            }
        } else {
            return ResponseEntity.badRequest().body("Invalid Test Case Method!");
        }

        exercise.setTestCases(testCasesFinal);

        try {
            exerciseService.saveExercise(exercise);
            for (ExerciseCategory exerciseCategory : exerciseCategories) {
                exerciseCategory.setExercise(exercise); // Đảm bảo có ID của exercise
                exerciseCategoryService.savedExerciseCategory(exerciseCategory); // Lưu vào DB
            }
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
        List<Category> categories = categoryService.getAllCategory();
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
        model.addAttribute("tags", categories);
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
            @RequestParam(name = "tag", required = false) List<Integer> categoryIds,
            @RequestParam(name = "removedTagIds", required = false) List<Integer> removedTagIds,
            @RequestParam Map<String, String> allParams, // Lấy dữ liệu từ UI
            Model model,
            RedirectAttributes redirectAttributes) {

        Exercise existingExercise = exerciseService.getExerciseById(id).orElse(null);
        if (existingExercise == null) {
            return "redirect:/exercises"; // Nếu không tìm thấy thì redirect
        }

        // Kiểm tra và cập nhật tên bài tập
        List<Category> categories = categoryService.getAllCategory();
        if (!exercise.getName().equals(existingExercise.getName())) {
            if (exerciseService.existsByTitleExcludingId(exercise.getName(), id)) {
                model.addAttribute("error", "Exercise name already exists!");
                model.addAttribute("programmingLanguages", programmingLanguageService.getAllProgrammingLanguages());
                model.addAttribute("testCasesJson", testCasesJson);
                model.addAttribute("tags", categories);
                model.addAttribute("content", "exercises/edit");
                model.addAttribute("exercise", existingExercise);

                return "layout";
            }
            existingExercise.setName(exercise.getName());
        }

        // Cập nhật các thông tin khác
        existingExercise.setDescription(exercise.getDescription());
        existingExercise.setSetup(exercise.getSetup());
        existingExercise.setLevel(exercise.getLevel());

        // xóa các tag đã removed trên UI
        if (removedTagIds != null && !removedTagIds.isEmpty()) {
            for (Integer tagId : removedTagIds) {
                exerciseCategoryService.deleteByExerciseIdAndCategoryId(id, Long.valueOf(tagId));
            }
        }

        if (categoryIds != null && !categoryIds.isEmpty()) {
            List<ExerciseCategory> newCategories = new ArrayList<>();
            for (Integer categoryId : categoryIds) {
                if (!exerciseCategoryService.existsByExerciseIdAndCategoryId(id, categoryId.longValue())) {
                    Category category = categoryService.getCategoryById(categoryId.longValue())
                            .orElseThrow(() -> new RuntimeException("Category not found with id: " + categoryId));

                    ExerciseCategory exerciseCategory = new ExerciseCategory();
                    exerciseCategory.setExercise(existingExercise);
                    exerciseCategory.setCategory(category);
                    exerciseCategoryService.savedExerciseCategory(exerciseCategory);
                }
            }
//            // Lưu các ExerciseCategory mới
//            for (ExerciseCategory ec : newCategories) {
//                exerciseCategoryService.savedExerciseCategory(ec);
//            }
        }


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
                        if (testCase.isHidden()) {
                            hiddenTestCases.add(testCase);
                        } else {
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

        try {
            redirectAttributes.addFlashAttribute("successMessage", "Edit successful!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Edit failed: " + e.getMessage());
        }

        return "redirect:/exercises";
    }


    // Delete a specific exercise
    @GetMapping("/delete/{id}")
    public String deleteExercise(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Exercise exercise = exerciseService.getExerciseById(id).get();
            if (!exercise.getAssessments().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Exercise has been in an assessment!");
            } else {
                exerciseService.deleteExercise(id);
                redirectAttributes.addFlashAttribute("successMessage", "Delete successful!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Delete failed: " + e.getMessage());
        }
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
            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Please upload a valid Excel file.");
                return "redirect:/exercises";
            }

            List<String> warnings = new ArrayList<>();
            exerciseService.saveExercisesToDatabase(file, warnings); // Cập nhật phương thức để truyền warnings

            redirectAttributes.addFlashAttribute("success", "Import successful!");

            if (!warnings.isEmpty()) {
                redirectAttributes.addFlashAttribute("warningList", warnings);
            }
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
        String[] columns = {"ID", "Title", "Language", "Level", "Description", "Set Up Code", "Test Case", "Hidden Test Case", "Tag"};
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

            StringBuilder tags = new StringBuilder();
            if (exercise.getExerciseCategories() != null && !exercise.getExerciseCategories().isEmpty()) {
                exercise.getExerciseCategories().forEach(ec -> {
                    if (tags.length() > 0) {
                        tags.append(", ");
                    }
                    tags.append(ec.getCategory().getTag());
                });
            }
            row.createCell(8).setCellValue(tags.toString());
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


    @GetMapping("/profile/{id}")
    @PreAuthorize("#id == @userService.getCurrentUser().id and hasAuthority('STUDENT')")
    public String showChart(@PathVariable Long id,
                            @RequestParam(value = "language", required = false) String language,
                            @RequestParam(value = "year", required = false) Integer year,
                            Model model) {

        if (year == null) {
            year = 2025; // Default value
        }
        if(language == null){
            language = "";
        }
        Integer easyExercisesNoLanguage = exerciseService.countEasyExercises("");
        Integer hardExercisesNoLanguage = exerciseService.countHardExercises("");
        Integer mediumExercisesNoLanguage = exerciseService.countMediumExercises("");
        Integer userEasyExercisesNoLanguage = exerciseService.countUserEasyExercises(id, "");
        Integer userHardExercisesNoLanguage = exerciseService.countUserHardExercises(id, "");
        Integer userMediumExercisesNoLanguage = exerciseService.countUserMediumExercises(id, "");
        Integer easyExercises = exerciseService.countEasyExercises(language);
        Integer hardExercises = exerciseService.countHardExercises(language);
        Integer mediumExercises = exerciseService.countMediumExercises(language);
        Integer userExercises = exerciseService.countUserExercises(id);
        Integer perfectScoreUserExercises = exerciseService.countPerfectScoreUserExercises(id);
        Integer userPassExercises = exerciseService.countUserPassedExercises(id);
        Integer userEasyExercises = exerciseService.countUserEasyExercises(id, language);
        Integer userHardExercises = exerciseService.countUserHardExercises(id, language);
        Integer userMediumExercises = exerciseService.countUserMediumExercises(id, language);
        Map<String, Integer> passedTestsPerMonth = exerciseService.countPassedTestsPerMonth(id, year);
        Integer exercisesWithMoreThanFiveAttempts = exerciseService.countExercisesWithMoreThanFiveAttempts(id);
        Integer exercisesSubmittedMidnight = exerciseService.countExercisesSubmittedMidnight(id);
        Integer exercisesSubmittedEarly = exerciseService.countExercisesSubmittedEarly(id);
        List<StudentExerciseAttemptResponse> studentAttempts = studentExerciseAttemptService.getStudentAttemptsByUser(id);

        StudentExerciseResponse chartResponse = new StudentExerciseResponse(
                easyExercises, hardExercises, mediumExercises, userExercises,userPassExercises,
                perfectScoreUserExercises, userEasyExercises, userHardExercises,
                userMediumExercises, passedTestsPerMonth, exercisesWithMoreThanFiveAttempts,
                exercisesSubmittedMidnight, exercisesSubmittedEarly,easyExercisesNoLanguage,hardExercisesNoLanguage,
                mediumExercisesNoLanguage,userEasyExercisesNoLanguage,userHardExercisesNoLanguage,userMediumExercisesNoLanguage
        );
        model.addAttribute("languages", programmingLanguageService.findAll());
        model.addAttribute("currentLanguage", language);
        model.addAttribute("chartData", chartResponse);
        model.addAttribute("studentAttempts", studentAttempts);
        return "exercises/profile"; // Ensure this view exists
    }


    //Access denied
    @GetMapping("/access-denied")
    public String accessDenied() {
        return "exercises/access-denied";
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
