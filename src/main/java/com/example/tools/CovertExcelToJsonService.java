package com.example.tools;


import org.apache.commons.text.StringEscapeUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;

@Service
public class CovertExcelToJsonService {
    public Map<String, Object> convertExcelFilesToJson(List<MultipartFile> files) throws IOException {
        Map<String, Object> result = new HashMap<>();
        Map<String, String> errorMap = new HashMap<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                errorMap.put(file.getOriginalFilename(), "The uploaded file is empty.");
                continue; // Bỏ qua file này và tiếp tục với file khác
            }

            try (InputStream inputStream = file.getInputStream();
                 Workbook workbook = new XSSFWorkbook(inputStream)) {

                for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                    Sheet sheet = workbook.getSheetAt(i);
                    HashMap<String, Object> sheetData = new HashMap<>();

                    try {
                        List<Object> curSheetData = processSheet(sheet);
                        sheetData.put("mc_questions", curSheetData);
                        sheetData.put("total_questions", curSheetData.size());

                        result.put(file.getOriginalFilename() + " - " + sheet.getSheetName(), sheetData);
                    } catch (Exception sheetException) {
                        errorMap.put(file.getOriginalFilename() + " - " + sheet.getSheetName(),
                                "Error processing sheet: " + sheetException.getMessage());
                    }
                }
            } catch (Exception e) {
                errorMap.put(file.getOriginalFilename(), "Error reading file: " + e.getMessage());
            }
        }

        if(!errorMap.isEmpty()) result.put("errors", errorMap);

        return result;
    }

    private List<Object> processSheet(Sheet sheet) {
        List<Object> mcQuestions = new ArrayList<>();
        int questionIndex = -1;
        int correctAnswerIndex = -1;
        int explanationIndex = -1;
        List<Integer> answerIndexes = new ArrayList<>();

        DataFormatter formatter = new DataFormatter();

        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            throw new RuntimeException("Header row is missing in sheet: " + sheet.getSheetName());
        }

        for( int rowIndex = 0; rowIndex < headerRow.getLastCellNum(); rowIndex++ ) {
            String headerContent = headerRow.getCell(rowIndex).getStringCellValue();

            // Check if it is the column index of question
            if( headerContent.trim().toLowerCase().contains("correct")) {
                correctAnswerIndex = rowIndex;
            }

            if( headerContent.trim().toLowerCase().contains("option")) {
                if(!answerIndexes.contains(rowIndex)) {
                    answerIndexes.add(rowIndex);
                }
            }

            if( headerContent.trim().toLowerCase().contains("question")) {
                questionIndex = rowIndex;
            }

            if( headerContent.trim().toLowerCase().contains("explanation")) {
                explanationIndex = rowIndex;
            }
        }

        if( questionIndex == -1) throw new RuntimeException("No question found in sheet: " + sheet.getSheetName());

        if(answerIndexes.isEmpty()) throw new RuntimeException("No answer options found in sheet: " + sheet.getSheetName());

        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) { // Skip header row (rowIndex 0)
            try {
                Row currentRow = sheet.getRow(rowIndex);
                if(currentRow == null) continue;

                HashMap<String, Object> questionData = new HashMap<String, Object>();

                String question = getAndFormateStringFromCell(currentRow.getCell(questionIndex));
                if(question.trim().isEmpty()) continue;

                String correct_answer_list = "";
                if( correctAnswerIndex != -1) {
                    correct_answer_list = getAndFormateStringFromCell(currentRow.getCell(correctAnswerIndex));
                }

                String explanation = "";
                if( explanationIndex != -1) {
                    explanation = getAndFormateStringFromCell(currentRow.getCell(explanationIndex));
                }

                List<String> answers = new ArrayList<>();
                for (int answerIndex : answerIndexes) {
                    String curAnswer = getAndFormateStringFromCell(currentRow.getCell(answerIndex));
                    answers.add(curAnswer);
                }

                // Parse correct answers based on letter positions (e.g., A, B, C, D)
                int cnt_correct = 0;
                int max_index_answer = -1;
                List<String> correctAnswers = new ArrayList<>();
                if (!correct_answer_list.isEmpty()) {
                    String[] correctSplit = correct_answer_list.split(",");
                    for(String correctAnswer : correctSplit) {
                        max_index_answer = Math.max(max_index_answer, correctAnswer.charAt(0) - 'A');
                    }

                    // Remove empty answer behind
                    for (int i = answers.size() - 1; i > max_index_answer; i--) {
                        if (answers.get(i).isEmpty()) {
                            answers.remove(i); // Xóa phần tử rỗng
                        }
                    }

                    for (String correctAnswer : correctSplit) {
                        correctAnswer = correctAnswer.trim(); // Trim spaces around answers
                        if (correctAnswer.isEmpty())
                            continue;
                        try {
                            int index = correctAnswer.charAt(0) - 'A'; // Convert A/B/C/D to 0/1/2/3
                            if (index >= 0 && index < answers.size()) {
                                correctAnswers.add(answers.get(index));
                                cnt_correct++;
                            }
                        } catch (Exception e) {
                            // Skip invalid entries
                        }
                    }
                } else {
                    for( int i = answers.size() - 1; i > max_index_answer; i--) {
                        if (answers.get(i).isEmpty()) {
                            answers.remove(i);
                        }
                    }
                }

                // Remove correct answers from the original list to avoid duplication
                answers.removeAll(correctAnswers);

                // Combine correct answers at the front
                correctAnswers.addAll(answers);

                questionData.put("question", question);
                questionData.put("answers", correctAnswers);
                questionData.put("total_correct", cnt_correct);
                questionData.put("explanation", explanation);

                mcQuestions.add(questionData);
            } catch (Exception e) {
                throw new RuntimeException("Can't processing row " + rowIndex);
            }
        }

        return mcQuestions;
    }

    private String getAndFormateStringFromCell(Cell cell) {
        String cellStrValue = getCellValueAsString(cell);
        String formatCellValue = StringEscapeUtils.escapeHtml4(cellStrValue);

        // Convert new line into <br>
        formatCellValue = formatCellValue.replace("\\r", "").replaceAll("\\r?\\n", "<br>");
        formatCellValue = formatCellValue.replace("&lt;br&gt;", "<br>");

        formatCellValue = formatCellValue.replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp");

        formatCellValue = formatCellValue.replace("    ", "&nbsp;&nbsp;&nbsp;&nbsp;");

        return formatCellValue;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        DataFormatter formatter = new DataFormatter();

        switch (cell.getCellType()) {
            case STRING:
                return formatter.formatCellValue(cell);
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf((int) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return "";
            default:
                return cell.toString().trim();
        }
    }

}
