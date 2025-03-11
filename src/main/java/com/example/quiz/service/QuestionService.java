package com.example.quiz.service;

import com.example.quiz.model.Question;
import com.example.quiz.model.Question.QuestionType;
import com.example.quiz.model.Quiz;
import com.example.quiz.repository.QuestionRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.example.utils.Helper.getCellValueAsString;

@Service
public class QuestionService {

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private QuizService quizService;

    public Optional<Question> findById(Long id) {
        return questionRepository.findById(id);
    }

    public Question save(Question question) {
        return questionRepository.save(question);
    }

    public void deleteById(Long id) {
        questionRepository.deleteById(id);
    }

    public Page<Question> findAll(Pageable pageable) {
        return questionRepository.findAll(pageable);
    }

    public List<Question> findAll() {
        return questionRepository.findAll();
    }

    public Page<Question> search(String searchQuery, Pageable pageable) {
        return questionRepository.search(searchQuery, pageable);
    }

    public List<Question> findByQuiz(Quiz quiz) {
        return questionRepository.findByQuiz(quiz);
    }

    public void importExcel(MultipartFile file) {
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int rowCount = sheet.getPhysicalNumberOfRows();
            List<Question> questions = new ArrayList<>();

            for (int i = 1; i < rowCount; i++) { // Start from 1 to skip the header row
                Row row = sheet.getRow(i);
                if (row != null) {
                    // Extract cells
                    Cell quizNameCell = row.getCell(0);
                    Cell questionTextCell = row.getCell(1);
                    Cell questionTypeCell = row.getCell(2);
                    Cell pointsCell = row.getCell(3);

                    if (quizNameCell != null) {
                        String quizName = getCellValueAsString(quizNameCell).trim();
                        String questionText = questionTextCell != null ? getCellValueAsString(questionTextCell).trim() : null;
                        String questionTypeString = questionTypeCell != null ? getCellValueAsString(questionTypeCell).trim() : null;
                        Integer points = pointsCell != null ? Integer.parseInt(getCellValueAsString(pointsCell).trim()) : null;

                        Quiz quiz = quizService.findByName(quizName);
                        if (quiz != null && questionText != null && questionTypeString != null) {
                            QuestionType questionType = QuestionType.valueOf(questionTypeString.toUpperCase());

                            Question question = new Question();
                            question.setQuiz(quiz);
                            question.setQuestionText(questionText);
                            question.setQuestionType(questionType);
                            question.setPoints(points);
                            questions.add(question);
                        }
                    }
                }
            }

            for (Question question : questions) {
                questionRepository.save(question);
            }

        } catch (IOException e) {
            throw new RuntimeException("Error importing questions from Excel", e);
        }
    }

    public ByteArrayInputStream exportToExcel(List<Question> questions) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Questions");

        // Header row
        String[] headers = {"ID", "Quiz", "Question Text", "Question Type", "Points"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        // Data rows
        int rowNum = 1;
        for (Question question : questions) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(question.getId());
            row.createCell(1).setCellValue(question.getQuiz() != null ? question.getQuiz().getName() : "");
            row.createCell(2).setCellValue(question.getQuestionText());
            row.createCell(3).setCellValue(question.getQuestionType().name());
            row.createCell(4).setCellValue(question.getPoints() != null ? question.getPoints() : 0);
        }

        // Write to output stream
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            workbook.write(out);
            workbook.close();
        } catch (IOException e) {
            throw new RuntimeException("Error exporting questions to Excel", e);
        }

        return new ByteArrayInputStream(out.toByteArray());
    }
}

