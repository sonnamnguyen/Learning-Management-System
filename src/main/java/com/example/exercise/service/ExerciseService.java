package com.example.exercise.service;

import com.example.assessment.model.ProgrammingLanguage;
import com.example.assessment.repository.ProgrammingLanguageRepository;
import com.example.exercise.model.Category;
import com.example.exercise.model.Exercise;
import com.example.exercise.model.ExerciseCategory;
import com.example.exercise.repository.CategoryRepository;
import com.example.exercise.repository.ExerciseRepository;
import com.example.testcase.TestCase;
import com.example.testcase.TestCaseRepository;
import jakarta.transaction.Transactional;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormatSymbols;
import java.util.*;
import java.util.stream.Collectors;

import static com.example.utils.Helper.getCellValueAsString;

@Service
public class ExerciseService {

    @Autowired
    private ExerciseRepository exerciseRepository;

    @Autowired
    private ProgrammingLanguageRepository programmingLanguageRepository;
    private TestCaseRepository testCaseRepository;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private CategoryRepository categoryRepository;

    public Map<String, Map<String, Integer>> getExercisesByLanguageWithAssessment() {
        List<Object[]> results = exerciseRepository.countExercisesByLanguageWithAssessment();
        Map<String, Map<String, Integer>> data = new HashMap<>();

        for (Object[] row : results) {
            String languageName = row[0].toString();
            int assessed = ((Number) row[1]).intValue();
            int notAssessed = ((Number) row[2]).intValue();
            int total = assessed + notAssessed;

            Map<String, Integer> exerciseData = new HashMap<>();
            exerciseData.put("assessed", assessed);
            exerciseData.put("notAssessed", notAssessed);
            exerciseData.put("total", total);

            data.put(languageName, exerciseData);
        }

        return data;
    }
    // Trả về dữ liệu theo từng ngôn ngữ (nếu có languageId)
    // ✅ Lấy dữ liệu theo languageId
    public Map<String, Map<String, Integer>> getExercisesByLanguageWithAssessment(Long languageId) {
        List<Object[]> results;
        if (languageId == null) {
            results = exerciseRepository.countExercisesByLanguageWithAssessment();
        } else {
            results = exerciseRepository.countExercisesByLanguageWithAssessment(languageId);
        }

        Map<String, Map<String, Integer>> data = new HashMap<>();
        for (Object[] row : results) {
            String languageName = (String) row[0];
            int assessedExercises = ((Number) row[1]).intValue();    // Bài tập có trong Assessment
            int notAssessed = ((Number) row[2]).intValue();  // Bài tập chưa có trong Assessment
            int totalExercises = assessedExercises + notAssessed;

            Map<String, Integer> values = new HashMap<>();
            values.put("assessed", assessedExercises);
            values.put("notAssessed", notAssessed);
            values.put("total", totalExercises);

            data.put(languageName, values);
        }
        return data;
    }
    public ExerciseService(ExerciseRepository exerciseRepository) {
        this.exerciseRepository = exerciseRepository;
    }


    public List<Exercise> searchByTitle(String title) {
        System.out.println("Searching for: " + title);
        List<Exercise> exercises = exerciseRepository.searchByTitle(title);
        System.out.println("Results found: " + exercises.size());
        return exercises;
    }

    public Optional<Exercise> getExerciseById(Long id) {
        return exerciseRepository.findById(id);
    }

    public List<Exercise> findAllExercises() {
        return exerciseRepository.findAll();
    }

    public void deleteExercise(Long id) {
        exerciseRepository.deleteById(id);
    }

    public Page<Exercise> getAllExercises(Pageable pageable) {
        return exerciseRepository.findAll(pageable); // Fetch all exercises with pagination
    }

    public List<TestCase> getTestCases(Long exerciseId) {
        return testCaseRepository.findByExerciseId(exerciseId);
    }

    public boolean existsByTitleExcludingId(String title, Long id) {
        return exerciseRepository.existsByNameExcludingId(title, id);
    }

    public void saveExercise(Exercise exercise) {
        if (!"SQL".equalsIgnoreCase(exercise.getLanguage().getLanguage())) {
            exercise.setSetupsql(null);
        }
        System.out.println("📩 Saving Exercise: " + exercise.getName());
        exerciseRepository.save(exercise);
        System.out.println("✔ Exercise Saved Successfully!");


    }


    public Page<Exercise> searchExercises(String searchQuery, Pageable pageable) {
        return exerciseRepository.searchExercises(searchQuery, pageable); // Search exercises with pagination
    }

    boolean exerciseExists(String exerciseName) {
        return exerciseRepository.existsByName(exerciseName);
    }

    // Method to export exercises to Excel
    public ByteArrayInputStream exportExercisesToExcel(List<Exercise> exercises) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Exercises");

        // Create the header row
        String[] headers = {"ID", "Name", "Description", "Level", "Module"};
        sheet.createRow(0).createCell(0).setCellValue(headers[0]);
        sheet.getRow(0).createCell(1).setCellValue(headers[1]);
        sheet.getRow(0).createCell(2).setCellValue(headers[2]);
        sheet.getRow(0).createCell(3).setCellValue(headers[3]);
        sheet.getRow(0).createCell(4).setCellValue(headers[4]);

        // Populate data rows
        int rowNum = 1;
        for (Exercise exercise : exercises) {
            sheet.createRow(rowNum).createCell(0).setCellValue(exercise.getId());
            sheet.getRow(rowNum).createCell(1).setCellValue(exercise.getName());
            sheet.getRow(rowNum).createCell(2).setCellValue(exercise.getDescription());
            sheet.getRow(rowNum).createCell(3).setCellValue(exercise.getLevel().ordinal());

            rowNum++;
        }

        // Write to ByteArrayOutputStream
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            workbook.write(out);
            workbook.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    public static boolean hasExcelFormat(MultipartFile file) {
        return file.getContentType().equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    public Map<String, Object> getExerciseDataFromExcel(InputStream inputStream) {
        List<Exercise> exercises = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Set<String> existingTitles = new HashSet<>();
        Set<String> existingTitlesInDB = new HashSet<>(exerciseRepository.findAllName());

        try {
            Workbook workbook = new XSSFWorkbook(inputStream);
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            if (!rowIterator.hasNext()) {
                throw new RuntimeException("Excel file is empty!");
            }

            Row headerRow = rowIterator.next();
            Map<String, Integer> columnIndexMap = new HashMap<>();

            Iterator<Cell> headerCells = headerRow.iterator();
            int colIndex = 0;
            while (headerCells.hasNext()) {
                Cell headerCell = headerCells.next();
                columnIndexMap.put(headerCell.getStringCellValue().trim(), colIndex++);
            }

            // Kiểm tra xem các header quan trọng có tồn tại không
            String[] requiredHeaders = {"Title", "Language", "Level", "Description", "Set Up Code", "Test Case", "Hidden Test Case", "SQL Tag", "Tag"};
            for (String header : requiredHeaders) {
                if (!columnIndexMap.containsKey(header)) {
                    throw new RuntimeException("Missing required column: " + header);
                }
            }

            int rowIndex = 1; // Bắt đầu từ dòng thứ 2 (dòng 1 là header)
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                rowIndex++;

//                if (row.getCell(0) == null || row.getCell(0).getCellType() == CellType.BLANK) {
//                    warnings.add("Row " + rowIndex + " is empty and was skipped.");
//                    continue;
//                }

                Exercise exercise = new Exercise();
                boolean isValidRow = true;

                if (columnIndexMap.containsKey("ID")) {
                    Cell idCell = row.getCell(columnIndexMap.get("ID"));
                    if (idCell != null && idCell.getCellType() == CellType.NUMERIC) {
                        exercise.setId((long) idCell.getNumericCellValue());
                    }
                }


//              ************  Xử lý Title ***************
                if (columnIndexMap.containsKey("Title")) {
                    Cell cell = row.getCell(columnIndexMap.get("Title"));
                    if (cell != null) {
                        String title = cell.getStringCellValue().trim();


                        if (existingTitlesInDB.contains(title)) {
                            warnings.add("Row " + rowIndex + " has a title that already exists: " + title);
                            isValidRow = false;
                        }

                        if (existingTitles.contains(title)) {
                            warnings.add("Row " + rowIndex + " has a duplicate Title in the file: " + title);
                            isValidRow = false;
                        } else {
                            existingTitles.add(title);
                            exercise.setName(title);
                        }


                    } else {
                        warnings.add("Row " + rowIndex + " is missing a Title.");
                        isValidRow = false;
                    }
                }

//              ************  Xử lý Language ***************
                if (columnIndexMap.containsKey("Language")) {
                    Cell cell = row.getCell(columnIndexMap.get("Language"));
                    if (cell != null) {
                        String languageName = cell.getStringCellValue().trim();

                        Optional<ProgrammingLanguage> optionalLanguage = programmingLanguageRepository.findByLanguage(languageName);
                        if (optionalLanguage.isPresent()) {
                            exercise.setLanguage(optionalLanguage.get());
                        } else {
                            warnings.add("Row " + rowIndex + " contains an unknown Language: " + languageName + ". List available language: " + programmingLanguageRepository.findAllLanguages());

                            isValidRow = false;
                        }
                    } else {
                        warnings.add("Row " + rowIndex + " is missing Language.");
                        isValidRow = false;
                    }

                }

//              ************  Xử lý Level ***************
                if (columnIndexMap.containsKey("Level")) {
                    Cell cell = row.getCell(columnIndexMap.get("Level"));
                    if (cell != null) {
                        try {
                            String levelText = cell.getStringCellValue().toUpperCase();
                            Exercise.Level level = Exercise.Level.valueOf(levelText);
                            exercise.setLevel(level);
                        } catch (IllegalArgumentException e) {
                            warnings.add("Row " + rowIndex + " has an invalid Level: " + cell.getStringCellValue());
                            isValidRow = false;
                        }
                    } else {
                        warnings.add("Row " + rowIndex + " is missing Level.");
                        isValidRow = false;
                    }
                }

//              ************  Xử lý Description ***************
                if (columnIndexMap.containsKey("Description")) {
                    Cell cell = row.getCell(columnIndexMap.get("Description"));
                    exercise.setDescription((cell != null) ? cell.getStringCellValue().trim() : "");
                }

//              ************  Xử lý Set Up Code ***************
                if (columnIndexMap.containsKey("Set Up Code")) {
                    Cell cell = row.getCell(columnIndexMap.get("Set Up Code"));
                    exercise.setSetup((cell != null) ? cell.getStringCellValue().trim() : "");
                }

                List<TestCase> testCases = new ArrayList<>();

//              ************  Xử lý Test Case ***************
                if (columnIndexMap.containsKey("Test Case")) {
                    Cell cell = row.getCell(columnIndexMap.get("Test Case"));
                    String testCaseText = (cell != null) ? cell.getStringCellValue().trim() : "";
                    boolean hasInvalidTestCase = false; // Biến để kiểm tra nếu có test case sai

                    if (!testCaseText.isEmpty()) {
                        String[] testCaseArray = testCaseText.split(";"); // Mỗi test case phải kết thúc bằng dấu ";"
                        for (String testCaseStr : testCaseArray) {
                            testCaseStr = testCaseStr.trim();
                            if (!testCaseStr.isEmpty()) {
                                // Kiểm tra dấu phẩy (,)
                                if (!testCaseStr.contains(",")) {
                                    warnings.add("Row " + rowIndex + " is missing a comma (',') in test case: '" + testCaseStr + "'.");
                                    hasInvalidTestCase = true;
                                    continue;
                                }

                                String[] parts = testCaseStr.split(",");
                                if (parts.length < 2 || parts.length > 3) {
                                    warnings.add("Row " + rowIndex + " has an invalid test case format: '" + testCaseStr + "'. Expected format: 'Input:<value>, Expected:<value>[, Hidden:true/false]'.");
                                    hasInvalidTestCase = true;
                                    continue;
                                }

                                // Kiểm tra từng phần tử có đúng tiền tố không
                                if (!parts[0].trim().matches("^Input:\\s*.+")) {
                                    warnings.add("Row " + rowIndex + " is missing or has incorrect 'Input:' format in test case: '" + testCaseStr + "'.");
                                    hasInvalidTestCase = true;
                                    continue;
                                }
                                if (!parts[1].trim().matches("^Expected:\\s*.+")) {
                                    warnings.add("Row " + rowIndex + " is missing or has incorrect 'Expected:' format in test case: '" + testCaseStr + "'.");
                                    hasInvalidTestCase = true;
                                    continue;
                                }

                                String input = parts[0].replace("Input:", "").trim();
                                String expected = parts[1].replace("Expected:", "").trim();


                                // Nếu test case hợp lệ, thêm vào danh sách tạm thời
                                TestCase testCase = new TestCase();
                                testCase.setInput(input);
                                testCase.setExpectedOutput(expected);
                                testCase.setHidden(false);
                                testCases.add(testCase);
                            } else {
                                warnings.add("Row " + rowIndex + " is empty and was skipped.");
                                hasInvalidTestCase = true;
                            }
                        }
                    }


                    if (hasInvalidTestCase) {
                        warnings.add("Row " + rowIndex + " has invalid test cases and was not saved.");
                        isValidRow = false;
                    } else {
                        exercise.setTestCases(testCases);
                    }
                }


//                ******* Xử lý Hidden Test Case *********
                if (columnIndexMap.containsKey("Hidden Test Case")) {
                    Cell cell = row.getCell(columnIndexMap.get("Hidden Test Case"));
                    String testCaseText = (cell != null) ? cell.getStringCellValue().trim() : "";
                    boolean hasInvalidTestCase = false; // Biến để kiểm tra nếu có test case sai

                    if (!testCaseText.isEmpty()) {
                        String[] testCaseArray = testCaseText.split(";"); // Mỗi test case phải kết thúc bằng dấu ";"
                        for (String testCaseStr : testCaseArray) {
                            testCaseStr = testCaseStr.trim();
                            if (!testCaseStr.isEmpty()) {
                                // Kiểm tra dấu phẩy (,)
                                if (!testCaseStr.contains(",")) {
                                    warnings.add("Row " + rowIndex + " is missing a comma (',') in test case: '" + testCaseStr + "'.");
                                    hasInvalidTestCase = true;
                                    continue;
                                }

                                String[] parts = testCaseStr.split(",");
                                if (parts.length < 2 || parts.length > 3) {
                                    warnings.add("Row " + rowIndex + " has an invalid test case format: '" + testCaseStr + "'. Expected format: 'Input:<value>, Expected:<value>[, Hidden:true/false]'.");
                                    hasInvalidTestCase = true;
                                    continue;
                                }

                                // Kiểm tra từng phần tử có đúng tiền tố không
                                if (!parts[0].trim().matches("^Input:\\s*.+")) {
                                    warnings.add("Row " + rowIndex + " is missing or has incorrect 'Input:' format in test case: '" + testCaseStr + "'.");
                                    hasInvalidTestCase = true;
                                    continue;
                                }
                                if (!parts[1].trim().matches("^Expected:\\s*.+")) {
                                    warnings.add("Row " + rowIndex + " is missing or has incorrect 'Expected:' format in test case: '" + testCaseStr + "'.");
                                    hasInvalidTestCase = true;
                                    continue;
                                }

                                String input = parts[0].replace("Input:", "").trim();
                                String expected = parts[1].replace("Expected:", "").trim();

                                // Nếu test case hợp lệ, thêm vào danh sách tạm thời
                                TestCase hiddenTestCase = new TestCase();
                                hiddenTestCase.setInput(input);
                                hiddenTestCase.setExpectedOutput(expected);
                                hiddenTestCase.setHidden(true);
                                testCases.add(hiddenTestCase);
                            } else {
                                warnings.add("Row " + rowIndex + " is empty and was skipped.");
                                hasInvalidTestCase = true;
                            }
                        }
                    }


                    if (hasInvalidTestCase) {
                        warnings.add("Row " + rowIndex + " has invalid test cases and was not saved.");
                        isValidRow = false;
                    } else {
                        exercise.setTestCases(testCases);
                    }
                }


//                ******** Xử lý SQL Tag ******

                if (columnIndexMap.containsKey("SQL Tag")) {
                    Cell cell = row.getCell(columnIndexMap.get("SQL Tag"));
                    exercise.setSetupsql((cell != null) ? cell.getStringCellValue().trim() : "");
                }

//                ********* Xử lý Tag ********
                if (columnIndexMap.containsKey("Tag")) {
                    Cell cell = row.getCell(columnIndexMap.get("Tag"));
                    String tagText = (cell != null) ? cell.getStringCellValue().trim() : "";
                    List<ExerciseCategory> exerciseCategories = new ArrayList<>();
                    boolean hadInvalidTag = false;
                    if (!tagText.isEmpty()) {
                        String[] tagArray = tagText.split(",");
                        for (String tagStr : tagArray) {
                            tagStr = tagStr.trim();
                            if (!tagStr.isEmpty()) {
//                                if (!tagStr.contains(",")) {
//                                    warnings.add("Row " + rowIndex + " has an invalid tag format: '" + tagStr + "'.");
//                                    hadInvalidTag = false;
//                                    continue;
//                                }

                                Category category = categoryRepository.findByTag(tagStr).orElse(null);
                                if(category == null) {
                                    warnings.add("Row " + rowIndex + " has a tag that does not exist: '" + tagStr + "'");
                                    hadInvalidTag = true;
                                }

                                ExerciseCategory exerciseCategory = new ExerciseCategory();
                                exerciseCategory.setCategory(category);
                                exerciseCategory.setExercise(exercise);
                                exerciseCategories.add(exerciseCategory);
                            }


                        }
                    }
                    if (hadInvalidTag) {
                        warnings.add("Row " + rowIndex + " has invalid tag and was not saved.");
                        isValidRow = false;
                    }else {
                        exercise.setExerciseCategories(exerciseCategories);
                    }

                }


                if (isValidRow) {
                    exercises.add(exercise);
                }

            }


            workbook.close();

            // Nếu tất cả dữ liệu đều bị trùng, không thêm file vào hệ thống
            if (exercises.isEmpty()) {
                throw new RuntimeException("All data in the file already exists. Import is canceled.");
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading Excel file: " + e.getMessage(), e);
        }

        // Trả về cả danh sách bài tập hợp lệ và danh sách cảnh báo
        Map<String, Object> result = new HashMap<>();
        result.put("exercises", exercises);
        result.put("warnings", warnings);
        return result;
    }


    public void saveExercisesToDatabase(MultipartFile file, List<String> warnings) {
        if (hasExcelFormat(file)) {
            try (InputStream inputStream = file.getInputStream()) {
                // Lấy dữ liệu từ file Excel
                Map<String, Object> result = getExerciseDataFromExcel(inputStream);

                // Lấy danh sách bài tập hợp lệ
                List<Exercise> exercises = (List<Exercise>) result.get("exercises");

                // Lưu vào database nếu danh sách không rỗng
                if (!exercises.isEmpty()) {
                    exerciseRepository.saveAll(exercises);
                }


                // Log cảnh báo nếu có
//                List<String> warnings = (List<String>) result.get("warnings");
//                if (!warnings.isEmpty()) {
//                    warnings.forEach(System.out::println); // Hoặc log ra file, hệ thống logging
//                }
                List<String> extractedWarnings = (List<String>) result.get("warnings");
                if (extractedWarnings != null && !extractedWarnings.isEmpty()) {
                    warnings.addAll(extractedWarnings); // Thêm cảnh báo vào danh sách
                }
            } catch (IOException e) {
                throw new IllegalArgumentException("The file is not a valid Excel file: " + e.getMessage(), e);
            }
        } else {
            throw new IllegalArgumentException("Invalid file format. Please upload an Excel file.");
        }
    }

    public Page<Exercise> getExercisesByLanguageAndLevel(Long languageId, String level, Pageable pageable) {
        Exercise.Level exerciseLevel = (level == null || level.trim().isEmpty())
                ? null
                : Exercise.Level.valueOf(level.toUpperCase());

        return exerciseRepository.findByFilters(languageId, exerciseLevel, pageable);
    }


    @Transactional
    public void deleteExercisesByIds(List<Long> ids) {
        exerciseRepository.deleteAllByIdIn(ids);
    }

    public List<Exercise> getExercisesByAssessmentId(Long assessmentId) {
        return exerciseRepository.findExercisesByAssessmentId(assessmentId);
    }

    //                      COOKING DASHBOARD
    double min_score = 0.0;
    double pass_score = 70.0;
    double max_score = 100.0;
    int start_lateTime = 0;
    int end_lateTime = 1;
    int start_earlyTime = 5;
    int end_earlyTime = 6;

    public Integer countTotalExercisesByLanguage(String language) {
        return Optional.ofNullable(exerciseRepository.countTotalExercisesByLanguage(language)).orElse(0);
    }

    public Integer countUserExercisesByLanguage(Long userId, String language) {
        return Optional.ofNullable(exerciseRepository.countUserExercisesByLanguage(userId, language, pass_score)).orElse(0);
    }

    public Integer countEasyExercises(String language) {
        return Optional.ofNullable(exerciseRepository.countNumberEasyExercises(language)).orElse(0);
    }

    public Integer countHardExercises(String language) {
        return Optional.ofNullable(exerciseRepository.countNumberHardExercises(language)).orElse(0);
    }

    public Integer countMediumExercises(String language) {
        return Optional.ofNullable(exerciseRepository.countNumberMediumExercises(language)).orElse(0);
    }

    public Integer countUserExercises(Long userId) {
        return Optional.ofNullable(exerciseRepository.countUserNumberExercises(userId, min_score)).orElse(0);
    }

    public Integer countUserPassedExercises(Long userId) {
        return Optional.ofNullable(exerciseRepository.countUserNumberExercises(userId, pass_score)).orElse(0);
    }

    public Integer countPerfectScoreUserExercises(Long userId) {
        return Optional.ofNullable(exerciseRepository.countUserNumberPerfectExercises(userId, max_score)).orElse(0);
    }

    public Integer countUserEasyExercises(Long userId, String language) {
        return Optional.ofNullable(exerciseRepository.countUserNumberEasyExercises(userId, language, pass_score)).orElse(0);
    }

    public Integer countUserHardExercises(Long userId, String language) {
        return Optional.ofNullable(exerciseRepository.countUserNumberHardExercises(userId, language, pass_score)).orElse(0);
    }

    public Integer countUserMediumExercises(Long userId, String language) {
        return Optional.ofNullable(exerciseRepository.countUserNumberMediumExercises(userId, language, pass_score)).orElse(0);
    }

    public Integer countExercisesWithMoreThanFiveAttempts(Long userId) {
        return Optional.ofNullable(exerciseRepository.countExercisesWithMoreThanFiveAttemptsAndAtLeastOneAbove70(userId)).orElse(0);
    }

    public Integer countExercisesSubmittedMidnight(Long userId) {
        return Optional.ofNullable(exerciseRepository.countExercisesSubmittedBetweenHours(userId, start_lateTime, end_lateTime,pass_score)).orElse(0);
    }

    public Integer countExercisesSubmittedEarly(Long userId) {
        return Optional.ofNullable(exerciseRepository.countExercisesSubmittedBetweenHours(userId, start_earlyTime, end_earlyTime,pass_score)).orElse(0);
    }


    public Map<String, Integer> countPassedTestsPerMonth(Long userId, int year) {
        List<Object[]> results = exerciseRepository.countPassedTestsPerMonth(userId, year);
        Map<String, Integer> passedTestsPerMonth = new LinkedHashMap<>();

        DateFormatSymbols dfs = new DateFormatSymbols(Locale.ENGLISH);
        String[] monthNames = dfs.getMonths();

        for (int i = 0; i < 12; i++) {
            passedTestsPerMonth.put(monthNames[i], 0);
        }

        // Populate with actual results from the database
        for (Object[] row : results) {
            int monthIndex = (Integer) row[0] - 1; // Convert 1-based (SQL) to 0-based (Java)
            String monthName = monthNames[monthIndex]; // Get English month name
            Integer count = ((Number) row[1]).intValue();
            passedTestsPerMonth.put(monthName, count);
        }

        return passedTestsPerMonth;
    }

    //Cai method nay cua nhom 2 dung de lay list exercise theo id
    public List<Exercise> findByIds(List<Long> ids) {
        return exerciseRepository.findAllById(ids);
    }

    public List<Exercise> findAll() {
        return exerciseRepository.findAll();
    }

    public Page<Exercise> getExercisesByLanguage(Long languageId, Pageable pageable) {
        return exerciseRepository.findByLanguageId(languageId, pageable);
    }

    public Page<Exercise> searchByDescriptionPaginated(String keyword, Pageable pageable) {
        return exerciseRepository.findByDescriptionContainingIgnoreCase(keyword, pageable);
    }

    public Page<Exercise> searchByDescriptionAndLanguage(String keyword, Long languageId, Pageable pageable) {
        return exerciseRepository.findByDescriptionContainingIgnoreCaseAndLanguageId(keyword, languageId, pageable);
    }

    public Page<Exercise> searchByDescriptionAndLevel(String keyword, String level, Pageable pageable) {
        Exercise.Level exerciseLevel = (level == null || level.trim().isEmpty()) ? null : Exercise.Level.valueOf(level.toUpperCase());
        return exerciseRepository.findByDescriptionContainingIgnoreCaseAndLevel(keyword, exerciseLevel, pageable);
    }

    public Page<Exercise> searchByDescriptionAndLanguageAndLevel(String keyword, Long languageId, String level, Pageable pageable) {
        Exercise.Level exerciseLevel = (level == null || level.trim().isEmpty()) ? null : Exercise.Level.valueOf(level.toUpperCase());
        return exerciseRepository.findByDescriptionContainingIgnoreCaseAndLanguageIdAndLevel(keyword, languageId, exerciseLevel, pageable);
    }

    // Các phương thức mới được bổ sung
    public List<Exercise> searchExercisesAll(String title) {
        return exerciseRepository.findByNameContainingIgnoreCase(title);
    }

    public List<Exercise> searchByDescriptionAll(String description) {
        return exerciseRepository.findByDescriptionContainingIgnoreCase(description);
    }

    public List<Exercise> searchByDescriptionAndLanguageAll(String description, Long languageId) {
        return exerciseRepository.findByDescriptionContainingIgnoreCaseAndLanguageId(description, languageId);
    }

    public List<Exercise> searchByDescriptionAndLevelAll(String description, String level) {
        Exercise.Level exerciseLevel = (level == null || level.trim().isEmpty())
                ? null
                : Exercise.Level.valueOf(level.toUpperCase());
        return exerciseRepository.findByDescriptionContainingIgnoreCaseAndLevel(description, exerciseLevel);
    }

    public List<Exercise> searchByDescriptionAndLanguageAndLevelAll(String description, Long languageId, String level) {
        Exercise.Level exerciseLevel = (level == null || level.trim().isEmpty())
                ? null
                : Exercise.Level.valueOf(level.toUpperCase());
        return exerciseRepository.findByDescriptionContainingIgnoreCaseAndLanguageIdAndLevel(description, languageId, exerciseLevel);
    }

    public List<Exercise> getExercisesByLanguageAll(Long languageId) {
        return exerciseRepository.findByLanguageId(languageId);
    }

    public List<Exercise> getExercisesByLanguageAndLevelAll(Long languageId, String level) {
        Exercise.Level exerciseLevel = (level == null || level.trim().isEmpty())
                ? null
                : Exercise.Level.valueOf(level.toUpperCase());
        return exerciseRepository.findByLanguageIdAndLevel(languageId, exerciseLevel);
    }

    public List<Exercise> getAllExercisesAll() {
        return exerciseRepository.findAll();
    }

//    public Map<String, List<Exercise>> findDuplicates() {
//        // Lấy tất cả exercises và nhóm theo ngôn ngữ ngay từ đầu
//        List<Exercise> allExercises = exerciseRepository.findAll();
//        Map<String, List<Exercise>> exercisesByLanguage = allExercises.stream()
//                .collect(Collectors.groupingBy(ex -> ex.getLanguage().getLanguage()));
//
//        Map<String, List<Exercise>> duplicates = new HashMap<>();
//        LevenshteinDistance levenshtein = new LevenshteinDistance();
//
//        // Xử lý từng ngôn ngữ riêng biệt
//        for (List<Exercise> languageGroup : exercisesByLanguage.values()) {
//            int size = languageGroup.size();
//            // Nếu chỉ có 1 bài thì không cần kiểm tra
//            if (size <= 1) continue;
//
//            // Sử dụng Map tạm thời để nhóm các bài tương tự
//            Map<String, List<Exercise>> tempGroups = new HashMap<>();
//
//            for (int i = 0; i < size; i++) {
//                Exercise ex1 = languageGroup.get(i);
//                String desc1 = ex1.getDescription();
//                int maxLength1 = desc1.length();
//
//                // Tìm nhóm phù hợp cho ex1
//                String matchedKey = null;
//
//                for (String key : tempGroups.keySet()) {
//                    int distance = levenshtein.apply(desc1, key);
//                    double similarity = 1.0 - ((double) distance / Math.max(maxLength1, key.length()));
//
//                    if (similarity >= 0.7) {
//                        matchedKey = key;
//                        break;
//                    }
//                }
//
//                // Nếu tìm thấy nhóm tương tự
//                if (matchedKey != null) {
//                    tempGroups.get(matchedKey).add(ex1);
//                } else {
//                    // Tạo nhóm mới với desc1 làm key
//                    List<Exercise> newGroup = new ArrayList<>();
//                    newGroup.add(ex1);
//                    tempGroups.put(desc1, newGroup);
//                }
//            }
//
//            // Chỉ giữ lại các nhóm có duplicates
//            tempGroups.entrySet().stream()
//                    .filter(entry -> entry.getValue().size() > 1)
//                    .forEach(entry -> {
//                        String finalKey = entry.getValue().get(0).getDescription() + "-" +
//                                entry.getValue().get(0).getLanguage().getLanguage();
//                        duplicates.put(finalKey, entry.getValue());
//                    });
//        }
//
//        return duplicates;
//    }

    public boolean existsByTitleAndLanguageExcludingId(String title, Integer languageId, Long id) {
        return exerciseRepository.existsByNameAndLanguageExcludingId(title, languageId, id);
    }

    public boolean existsByTitleAndLanguage(String title, Integer languageId) {
        return exerciseRepository.existsByNameAndLanguage(title, languageId);
    }

    //---------------------Dashboard
    public Map<String, Integer> getExerciseStatistics(Long languageId) {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("total", exerciseRepository.countTotalExercises(languageId));
        stats.put("new", exerciseRepository.countNewExercises(languageId));
        stats.put("completed", exerciseRepository.countCompletedExercises(languageId));
        stats.put("missed", exerciseRepository.countMissedWorkouts(languageId));
        return stats;
    }


    public Map<String, Integer> getLevelDistribution(Long languageId) {
        Map<String, Integer> levelDistribution = new HashMap<>(Map.of("EASY", 0, "MEDIUM", 0, "HARD", 0));
        List<Object[]> levelCounts = exerciseRepository.countExercisesByLevel(languageId);

        if (levelCounts != null) {
            for (Object[] row : levelCounts) {
                if (row.length >= 2 && row[0] != null && row[1] != null) {
                    String level = row[0].toString();
                    int count = ((Number) row[1]).intValue();
                    levelDistribution.put(level, count);
                }
            }
        }
        return levelDistribution;
    }


    public Map<String, Integer> getStatusDistribution(Long languageId) {
        Map<String, Integer> statusDistribution = new HashMap<>(Map.of("COMPLETED", 0, "PENDING", 0));
        List<Object[]> statusCounts = exerciseRepository.countExercisesByStatus(languageId);
        if (statusCounts != null) {
            for (Object[] row : statusCounts) {
                if (row.length >= 2 && row[0] != null && row[1] != null) {
                    statusDistribution.put(row[0].toString(), ((Number) row[1]).intValue());
                }
            }
        }
        return statusDistribution;
    }


    public int countNewExercises(Long languageId) {
        return exerciseRepository.countNewExercises(languageId);
    }

    public double getCompletionRate(Long languageId) {
        return exerciseRepository.calculateCompletionRate(languageId);
    }

    public int countUpcomingWorkouts(Long languageId) {
        return exerciseRepository.countUpcomingWorkouts(languageId);
    }

    public int countMissedWorkouts(Long languageId) {
        return exerciseRepository.countMissedWorkouts(languageId);
    }

    public Map<String, Integer> getExercisesByLanguage() {
        Map<String, Integer> data = new HashMap<>();
        List<Object[]> results = exerciseRepository.countExercisesByLanguage();

        for (Object[] row : results) {
            data.put(row[0].toString(), ((Number) row[1]).intValue());
        }

        return data;
    }

    public int countTotalExercises(Long languageId) {
        return exerciseRepository.countTotalExercises(languageId);
    }

    public Map<String, Integer> getExercisesByLanguage(Long languageId) {
        Map<String, Integer> data = new HashMap<>();
        List<Object[]> results;

        if (languageId == null) {
            results = exerciseRepository.countExercisesByLanguage();
        } else {
            results = exerciseRepository.countExercisesByLanguage(languageId);
        }

        for (Object[] row : results) {
            data.put(row[0].toString(), ((Number) row[1]).intValue());
        }

        return data;
    }
    public Page<Exercise> getExercisesByFilters(Long languageId, String level, List<Long> tagIds, Pageable pageable) {
//        Exercise.Level levelEnum = null;
//        if (level != null && !level.isEmpty()) {
//            levelEnum = Exercise.Level.valueOf(level);
//        }
//        return exerciseRepository.findByFiltersAndTags(languageId, levelEnum, tagIds, pageable);
        Exercise.Level exerciseLevel = (level == null || level.trim().isEmpty())
                ? null
                : Exercise.Level.valueOf(level.toUpperCase());

        if (tagIds != null && tagIds.isEmpty()) {
            tagIds = null;
        }

        return exerciseRepository.findByFiltersAndTags(languageId, exerciseLevel, tagIds, pageable);
    }

    public Page<Exercise> getExercisesByAllFilters(String title, Long languageId, String level, List<Long> tagIds, Pageable pageable) {
        Exercise.Level exerciseLevel = (level == null || level.trim().isEmpty())
                ? null
                : Exercise.Level.valueOf(level.toUpperCase());

        if (tagIds != null && tagIds.isEmpty()) {
            tagIds = null;
        }

        // Nếu không có title, dùng phương thức findByFiltersAndTags hiện có
        if (title == null || title.trim().isEmpty()) {
            return exerciseRepository.findByFiltersAndTags(languageId, exerciseLevel, tagIds, pageable);
        }

        // Nếu có title, gọi một truy vấn mới kết hợp tất cả các bộ lọc
        return exerciseRepository.findByTitleAndFiltersAndTags(title, languageId, exerciseLevel, tagIds, pageable);
    }

}