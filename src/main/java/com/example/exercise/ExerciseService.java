package com.example.exercise;

import com.example.assessment.model.ProgrammingLanguage;
import com.example.assessment.repository.ProgrammingLanguageRepository;
import com.example.testcase.TestCase;
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
import java.util.*;

import static com.example.utils.Helper.getCellValueAsString;

@Service
public class ExerciseService {

    @Autowired
    private ExerciseRepository exerciseRepository;

    @Autowired
    private ProgrammingLanguageRepository programmingLanguageRepository;

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

    public List<Exercise> getExerciseDataFromExcel(InputStream inputStream) {
        List<Exercise> exercises = new ArrayList<>();

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


            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();

                if (row.getCell(0) == null || row.getCell(0).getCellType() == CellType.BLANK) {
                    continue;  // Bỏ qua dòng trống
                }

                Exercise exercise = new Exercise();

                if (columnIndexMap.containsKey("ID")) {
                    Cell idCell = row.getCell(columnIndexMap.get("ID"));
                    if (idCell != null && idCell.getCellType() == CellType.NUMERIC) {
                        exercise.setId((long) idCell.getNumericCellValue());
                    }
                }

                if (columnIndexMap.containsKey("Title")) {
                    Cell cell = row.getCell(columnIndexMap.get("Title"));
                    exercise.setName((cell != null) ? cell.getStringCellValue().trim() : "");
                }

                if (columnIndexMap.containsKey("Language")) {
                    Cell cell = row.getCell(columnIndexMap.get("Language"));
                    String languageName = (cell != null) ? cell.getStringCellValue().trim() : "Unknown";
                    ProgrammingLanguage language = programmingLanguageRepository.findByLanguage(languageName).orElse(null);
                    if (language == null) {
                        language = new ProgrammingLanguage();
                        language.setLanguage(languageName);
                        programmingLanguageRepository.save(language);
                    }
                    exercise.setLanguage(language);
                }

                if (columnIndexMap.containsKey("Level")) {
                    Cell cell = row.getCell(columnIndexMap.get("Level"));
                    if (cell != null) {
                        try {
                            String levelText = cell.getStringCellValue().toUpperCase();
                            Exercise.Level level = Exercise.Level.valueOf(levelText);
                            exercise.setLevel(level);
                        } catch (IllegalArgumentException e) {
                            throw new RuntimeException("Invalid exercise level: " + cell.getStringCellValue());
                        }
                    }
                }

                if (columnIndexMap.containsKey("Description")) {
                    Cell cell = row.getCell(columnIndexMap.get("Description"));
                    exercise.setDescription((cell != null) ? cell.getStringCellValue().trim() : "");
                }

                if (columnIndexMap.containsKey("Set Up Code")) {
                    Cell cell = row.getCell(columnIndexMap.get("Set Up Code"));
                    exercise.setSetup((cell != null) ? cell.getStringCellValue().trim() : "");
                }

                if (columnIndexMap.containsKey("Test Case")) {
                    Cell cell = row.getCell(columnIndexMap.get("Test Case"));
                    String testCaseText = (cell != null) ? cell.getStringCellValue().trim() : "";
                    List<TestCase> testCases = new ArrayList<>();
                    if (!testCaseText.isEmpty()) {
                        String[] testCaseArray = testCaseText.split(";");
                        for (String testCaseStr : testCaseArray) {
                            testCaseStr = testCaseStr.trim();
                            if (!testCaseStr.isEmpty()) {
                                String[] parts = testCaseStr.split(",");
                                if (parts.length == 2) {
                                    String input = parts[0].replace("Input:", "").trim();
                                    String expected = parts[1].replace("Expected:", "").trim();
                                    TestCase testCase = new TestCase();
                                    testCase.setInput(input);
                                    testCase.setExpectedOutput(expected);
                                    testCases.add(testCase);
                                }
                            }
                        }
                    }
                    exercise.setTestCases(testCases);
                }

                exercises.add(exercise);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading Excel file: " + e.getMessage(), e);
        }

        return exercises;
    }



    public void saveExercisesToDatabase(MultipartFile file) {
        if (hasExcelFormat(file)) {
            try (InputStream inputStream = file.getInputStream()) {
                List<Exercise> exercises = getExerciseDataFromExcel(inputStream);
                exerciseRepository.saveAll(exercises);
            } catch (IOException e) {
                throw new IllegalArgumentException("The file is not a valid excel file");
            }
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

}