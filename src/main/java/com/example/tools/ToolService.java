package com.example.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ToolService {

    public String extractFileName(String content) {
        // Regular expression to find "CODE: " followed by the file name
        Pattern pattern = Pattern.compile("CODE:\\s*([\\w\\.\\-]+)");
        Matcher matcher = pattern.matcher(content);

        if (matcher.find()) {
            // Return the group containing the file name
            return matcher.group(1);
        }

        // Return null if no match is found
        return null;
    }

    public Map<String, Object> convertTxtFilesToJson(Map<String, String> files) throws IOException {
        Map<String, Object> result = new HashMap<>();

        for (Map.Entry<String, String> entry : files.entrySet()) {
            String fileName = entry.getKey();
            String fileContent = entry.getValue();

            HashMap<String, Object> fileData = new HashMap<String, Object>();

            List<Object> processedFile = processFileContent(fileContent);
            fileData.put("mc_questions", processedFile);
            fileData.put("total_questions", processedFile.size());

            result.put(fileName, fileData);
        }

        return result;
    }

//    private List<Object> processFileContent(String fileContent) throws IOException {
//        List<Object> mcQuestions = new ArrayList<>();
//
//
//        return mcQuestions;
//    }

    private List<Object> processFileContent(String fileContent) throws IOException {
        List<Object> mcQuestions = new ArrayList<>();
        String[] lines = fileContent.split("\\r?\\n"); // Chia fileContent thành danh sách các dòng
        String questionText = "";
        List<String> answers = new ArrayList<>();
        boolean started = false;
        int questionNumber = 0;

        for (String line : lines) {
            line = line.trim(); // Loại bỏ khoảng trắng dư thừa

            // Bắt đầu khi dòng đầu tiên là một câu hỏi bắt đầu bằng số '1.'
            if (!started) {
                if (line.matches("^1\\.\\s+.*")) {
                    started = true;
                } else {
                    continue;
                }
            }

            // Kiểm tra nếu dòng bắt đầu bằng số thứ tự của câu hỏi (e.g., '1.', '2.', etc.)
            if (line.matches("^\\d+\\.\\s+.*")) {
                int currentNumber = Integer.parseInt(line.split("\\.")[0]);

                if (currentNumber == questionNumber + 1) {
                    // Lưu câu hỏi và câu trả lời trước đó
                    if (!questionText.isEmpty()) {
                        mcQuestions.add(createQuestionObject(questionText, answers));
                    }
                    // Bắt đầu một câu hỏi mới
                    questionNumber = currentNumber;
                    questionText = line.substring(line.indexOf('.') + 1).trim();
                    answers.clear();
                } else {
                    // Nếu không phải câu hỏi mới, nối vào câu hỏi hiện tại
                    questionText += " " + line;
                }
            }
            // Kiểm tra nếu dòng là câu trả lời (e.g., 'A.', 'B.', etc.)
            else if (line.matches("^[A-Ma-m]\\.\\s+.*")) {
                String answer = line.substring(2).trim();
                answers.add(answer);
            } else {
                // Nếu không phải câu hỏi hay câu trả lời, nối vào câu hỏi hoặc câu trả lời cuối cùng
                if (!answers.isEmpty()) {
                    int lastIndex = answers.size() - 1;
                    answers.set(lastIndex, answers.get(lastIndex) + " " + line);
                } else {
                    questionText += " " + line;
                }
            }
        }

        // Thêm câu hỏi cuối cùng
        if (!questionText.isEmpty()) {
            mcQuestions.add(createQuestionObject(questionText, answers));
        }

        return mcQuestions;
    }

    // Hàm hỗ trợ tạo đối tượng câu hỏi
    private Map<String, Object> createQuestionObject(String questionText, List<String> answers) {
        Map<String, Object> questionObject = new HashMap<>();
        questionObject.put("question", questionText);
        questionObject.put("answers", answers);
        return questionObject;
    }

    public Map<String, Object> convertExcelFilesToJson(List<MultipartFile> files) throws IOException {
        Map<String, Object> result = new HashMap<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                throw new IllegalArgumentException("The uploaded file is empty.");
            }

            InputStream inputStream = file.getInputStream();
            try (Workbook workbook = new XSSFWorkbook(inputStream)) {
                for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                    Sheet sheet = workbook.getSheetAt(i);
                    HashMap<String, Object> sheetData = new HashMap<String, Object>();

                    List<Object> cur_sheetData = processSheet(sheet);
                    sheetData.put("mc_questions", cur_sheetData);
                    sheetData.put("total_questions", cur_sheetData.size());

                    result.put(file.getOriginalFilename() + " - " + sheet.getSheetName(), sheetData);
                }
            }
        }
        return result;
    }

    private List<Object> processSheet(Sheet sheet) {
        List<Object> mcQuestions = new ArrayList<>();

        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) { // Skip header row (rowIndex 0)
            Row row = sheet.getRow(rowIndex);
            if (row == null)
                continue;

            HashMap<String, Object> questionData = new HashMap<String, Object>();
            String question = getCellValueAsString(row.getCell(0)); // First column: Question
            String correct = getCellValueAsString(row.getCell(1)); // Second column: Correct answers
            List<String> answers = new ArrayList<>();

            if (question.isEmpty())
                continue;

            // Collect all answers from columns starting from the third
            for (int colIndex = 2; colIndex < row.getLastCellNum(); colIndex++) {
                String answer = getCellValueAsString(row.getCell(colIndex));
                if (!answer.isEmpty()) {
                    answers.add(answer);
                } else {
                    break;
                }
            }

            // Parse correct answers based on letter positions (e.g., A, B, C, D)
            int cnt_correct = 0;
            List<String> correctAnswers = new ArrayList<>();
            if (!correct.isEmpty()) {
                String[] correctSplit = correct.split(",");
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
            }

            // Remove correct answers from the original list to avoid duplication
            answers.removeAll(correctAnswers);

            // Combine correct answers at the front
            correctAnswers.addAll(answers);

            questionData.put("question", question);
            questionData.put("answers", correctAnswers);
            questionData.put("correct", cnt_correct);

            mcQuestions.add(questionData);
        }
        return mcQuestions;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
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
