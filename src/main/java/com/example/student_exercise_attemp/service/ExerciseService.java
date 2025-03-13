package com.example.student_exercise_attemp.service;

import com.example.assessment.model.ProgrammingLanguage;
import com.example.assessment.repository.ProgrammingLanguageRepository;
import com.example.student_exercise_attemp.model.Exercise;
import com.example.student_exercise_attemp.repository.ExerciseRepository;
import com.example.testcase.TestCase;
import com.example.testcase.TestCaseRepository;
import jakarta.transaction.Transactional;
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

import static com.example.utils.Helper.getCellValueAsString;

@Service
public class ExerciseService {

    @Autowired
    private ExerciseRepository exerciseRepository;

    @Autowired
    private ProgrammingLanguageRepository programmingLanguageRepository;
    private TestCaseRepository testCaseRepository;

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
            String[] requiredHeaders = {"Title", "Language", "Level", "Description", "Set Up Code", "Test Case"};
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
                            warnings.add("Row " + rowIndex + " contains an unknown Language: " + languageName +". List available language: " + programmingLanguageRepository.findAllLanguages());

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


//              ************  Xử lý Test Case ***************
                if (columnIndexMap.containsKey("Test Case")) {
                    Cell cell = row.getCell(columnIndexMap.get("Test Case"));
                    String testCaseText = (cell != null) ? cell.getStringCellValue().trim() : "";
                    List<TestCase> testCases = new ArrayList<>();
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
                                boolean isHidden = false;

                                if (parts.length == 3) {
                                    if (!parts[2].trim().matches("^Hidden:\\s*(true|false)")) {
                                        warnings.add("Row " + rowIndex + " has an invalid 'Hidden:' value (must be 'true' or 'false') in test case: '" + testCaseStr + "'.");
                                        hasInvalidTestCase = true;
                                        continue;
                                    }
                                    isHidden = Boolean.parseBoolean(parts[2].replace("Hidden:", "").trim());
                                }

                                // Nếu test case hợp lệ, thêm vào danh sách tạm thời
                                TestCase testCase = new TestCase();
                                testCase.setInput(input);
                                testCase.setExpectedOutput(expected);
                                testCase.setHidden(isHidden);
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
        return Optional.ofNullable(exerciseRepository.countExercisesSubmittedBetweenHours(userId, start_lateTime, end_lateTime)).orElse(0);
    }

    public Integer countExercisesSubmittedEarly(Long userId) {
        return Optional.ofNullable(exerciseRepository.countExercisesSubmittedBetweenHours(userId, start_earlyTime, end_earlyTime)).orElse(0);
    }


    public Map<String, Integer> countPassedTestsPerMonth(Long userId, int year) {
        List<Object[]> results = exerciseRepository.countPassedTestsPerMonth(userId, year, pass_score);
        Map<String, Integer> passedTestsPerMonth = new LinkedHashMap<>(); // Keep order of months

        DateFormatSymbols dfs = new DateFormatSymbols(Locale.ENGLISH);
        String[] monthNames = dfs.getMonths(); // Array of month names: "January", "February", etc.

        for (Object[] row : results) {
            int monthIndex = (Integer) row[0] - 1; // Convert 1-based (SQL) to 0-based (Java)
            String monthName = monthNames[monthIndex]; // Get English month name
            Integer count = ((Number) row[1]).intValue();
            passedTestsPerMonth.put(monthName, count);
        }

        return passedTestsPerMonth;
    }
}