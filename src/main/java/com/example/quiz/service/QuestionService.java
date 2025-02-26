package com.example.quiz.service;

import com.example.course.Course;
import com.example.exception.NotFoundException;
import com.example.quiz.model.Question;
import com.example.quiz.model.Question.QuestionType;
import com.example.quiz.model.Quiz;
import com.example.quiz.repository.QuestionRepository;
import com.example.quiz.repository.QuizRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

import static com.example.utils.Helper.getCellValueAsString;

@Service
public class QuestionService {

    @Autowired
    private QuizRepository quizRepository;
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
        return questionRepository.findByQuizzes_Id(quiz.getId());
    }
    public List<Question> findByQuizName(String name) {
        return questionRepository.findByQuizzes_Name(name);
    }


    public void importExcel(MultipartFile _file_) {
        try (Workbook workbook = new XSSFWorkbook(_file_.getInputStream())) {
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
                            // Use addQuiz to maintain many-to-many relationship
                            question.addQuiz(quiz);
                            question.setQuestionText(questionText);
                            question.setQuestionType(questionType);
                            question.setPoints(points);
                            questions.add(question);
                        }
                    }
                }
            }

            for (Question question : questions) {
                questionRepository.save(question); // Save each Question with its associated Quizzes
            }

        } catch (IOException _e_) {
            throw new RuntimeException("Error importing questions from Excel", _e_);
        }
    }

    private String getCellValueAsString(Cell cell) {
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell);
    }

    public ByteArrayInputStream exportToExcel(List<Question> questions) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Questions");

        // Header row
        String[] headers = {"ID", "Quiz(s)", "Question Text", "Question Type", "Points"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        // Data rows
        int rowNum = 1;
        for (Question question : questions) {
            Row row = sheet.createRow(rowNum++);

            // ID
            row.createCell(0).setCellValue(question.getId());

            // Quiz(s)
            String quizNames = question.getQuizzes().stream()
                    .map(Quiz::getName)
                    .reduce((name1, name2) -> name1 + ", " + name2)
                    .orElse("");
            row.createCell(1).setCellValue(quizNames);

            // Question Text
            row.createCell(2).setCellValue(question.getQuestionText());

            // Question Type
            row.createCell(3).setCellValue(question.getQuestionType().name());

            // Points
            row.createCell(4).setCellValue(question.getPoints());
        }

        // Auto-size columns to fit content
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Write the output to a byte array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            workbook.write(outputStream);
        } catch (IOException e) {
            e.printStackTrace(); // Handle exception as needed
        }

        // Close the workbook to free resources
        try {
            workbook.close();
        } catch (IOException e) {
            e.printStackTrace(); // Handle exception as needed
        }

        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    //Find list quiz and question form course
    public Set<Quiz> showAllQuizzesWithQuestions(String courseName) {
        Set<Quiz> quizzes = quizRepository.findQuizByCourseName(courseName);

        if (quizzes == null || quizzes.isEmpty()) {
            throw new NotFoundException("Quiz not found");
        }

        for (Quiz quiz : quizzes) {
            List<Question> questions = questionRepository.findByQuizzes_Name(quiz.getName());

            quiz.setQuestions((questions != null && !questions.isEmpty())
                    ? new HashSet<>(questions)
                    : Collections.emptySet());
        }

        return quizzes;
    }
    @Transactional
    public void create(Long quizId,Question question) throws NotFoundException{
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new NotFoundException("Quiz not found!"));
        question.addQuiz(quiz);
        questionRepository.save(question);
        quizRepository.save(quiz);
    }

    public List<Question> findQuestionsByQuizId(Long id) {
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Quiz not found!"));
        return new ArrayList<>(quiz.getQuestions());
    }
    @Autowired
    public QuestionService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    public List<Question> getQuestionsByAssessmentId(Long assessmentId) {
        return questionRepository.findQuestionsWithAnswersByAssessmentId(assessmentId);
    }
}

