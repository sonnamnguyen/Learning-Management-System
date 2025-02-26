package com.example.quiz.service;


import com.example.course.CourseRepository;
import com.example.exception.DateException;
import com.example.exception.NotFoundException;
import com.example.quiz.model.Question;
import com.example.quiz.model.Quiz;
import com.example.quiz.model.SchedulerUtil;
import com.example.quiz.repository.QuestionRepository;
import com.example.quiz.repository.QuizRepository;
import com.example.user.User;
import com.example.user.UserService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.checkerframework.checker.units.qual.A;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.xml.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.example.utils.Helper.getCellValueAsString;

@Service
public class QuizService {

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ScheduleJob scheduleJob;
    @Autowired
    private UserService userService;
    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private CourseRepository courseRepository;

    public Optional<Quiz> findById(Long id) {
        return quizRepository.findById(id);
    }

    public Quiz findByName(String name) {
        return quizRepository.findByName(name);
    }

    public Quiz save(Quiz quiz) {
        return quizRepository.save(quiz);
    }

    public void deleteById(Long id) {
        quizRepository.deleteById(id);
    }

    public Page<Quiz> findAll(Pageable pageable) {
        return quizRepository.findAll(pageable);  // Lấy tất cả quizs với phân trang
    }

    public List<Quiz> findAll() {
        return quizRepository.findAll();
    }

    public Page<Quiz> search(String searchQuery, Pageable pageable) {
        return quizRepository.searchQuizs(searchQuery, pageable);  // Tìm kiếm với phân trang
    }

    boolean existsByName(String quizName) {
        return quizRepository.existsByName(quizName);
    }

    public void createQuiz (Quiz quiz){
            User user = userService.getCurrentUser();
            if (user == null) {
               throw new RuntimeException("User not found");
            }
            if(quiz.getStartTime().isBefore(LocalDateTime.now())){
                throw new DateException("Start time can not in the past");
            }
            if(quiz.getEndTime().isBefore(quiz.getStartTime())){
                throw new DateException("End time can not before Start Time");
            }
            quiz.setCreatedAt(LocalDateTime.now());
            quiz.setCreatedBy(user);
            quiz.setQuizType(Quiz.QuizType.CLOSE);
            Quiz quiz1= quizRepository.save(quiz);

            try {
                SchedulerUtil.startScheduler();

                Scheduler scheduler = SchedulerUtil.getScheduler();
                scheduleJob.scheduleQuizOpenJob(quiz1.getId(), quiz1.getStartTime());
                scheduleJob.scheduleQuizCloseJob(quiz1.getId(), quiz1.getEndTime());
            } catch (SchedulerException e) {
                e.printStackTrace();
            }
    }


    public void importExcel(MultipartFile file) {
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int rowCount = sheet.getPhysicalNumberOfRows();
            List<Quiz> quizs = new ArrayList<>();

            for (int i = 1; i < rowCount; i++) { // Start from 1 to skip the header row
                Row row = sheet.getRow(i);
                if (row != null) {
                    Cell idCell = row.getCell(0);
                    Cell nameCell = row.getCell(1);
                    Cell descriptionCell = row.getCell(2);
//                    Cell attemptsAllowedCell = row.getCell(3); //attemptsAllowed
                    Cell groupCell = row.getCell(4);

                    if (nameCell != null && descriptionCell != null) {
                        String quizName = getCellValueAsString(nameCell).trim();
                        String quizDescription = getCellValueAsString(descriptionCell).trim();
//                        Integer quizAttemptsAllowed = attemptsAllowedCell != null ? attemptsAllowedCell : null; // Get quiz attemptsAllowed, if present
                        String courseName = groupCell != null ? getCellValueAsString(groupCell).trim() : null; // Get quiz group, if present

                        // Check if quiz already exists based on some criteria (e.g., quiz name)
                        if (!existsByName(quizName)) {
                            Quiz quiz = new Quiz();
                            quiz.setName(quizName);
                            quiz.setDescription(quizDescription);
//                            quiz.setAttemptsAllowed(quizAttemptsAllowed);

//                            if (courseName != null) {
//                                Course course = findCourseByName(courseName); // Implement this method based on your logic
//                                quiz.setCourse(course);
//                            }

                            quizs.add(quiz);
                        }
                    }
                }
            }

            // Save quizs to the database
           for (Quiz quiz : quizs) {
               quizRepository.save(quiz);
           }
        } catch (IOException e) {
            throw new RuntimeException("Error importing quizs from Excel", e);
        }
    }
    

    // Method to export roles to Excel
    public ByteArrayInputStream exportToExcel(List<Quiz> quizs) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Quizs");

        // Create the header row
        String[] headers = { "ID", "Name", "Description", "AttemptsAllowed", "Quiz Group" };
        sheet.createRow(0).createCell(0).setCellValue(headers[0]);
        sheet.getRow(0).createCell(1).setCellValue(headers[1]);
        sheet.getRow(0).createCell(2).setCellValue(headers[2]);
        sheet.getRow(0).createCell(3).setCellValue(headers[3]);
        sheet.getRow(0).createCell(4).setCellValue(headers[4]);

        // Populate data rows
        int rowNum = 1;
        for (Quiz quiz : quizs) {
            sheet.createRow(rowNum).createCell(0).setCellValue(quiz.getId());
            sheet.getRow(rowNum).createCell(1).setCellValue(quiz.getName());
            sheet.getRow(rowNum).createCell(2).setCellValue(quiz.getDescription());
//            sheet.getRow(rowNum).createCell(3).setCellValue(quiz.getAttemptsAllowed());
            sheet.getRow(rowNum).createCell(4).setCellValue(quiz.getCourse().getName());
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

    public int getAttemptCountForUser(Quiz quiz, User user) {
        Integer count = quizRepository.getAttemptCountForUser(quiz.getId(), user.getId());
        return (count != null) ? count : 0;
    }

    public void incrementAttemptCount(Quiz quiz, User user) {
        quizRepository.incrementAttemptCount(quiz.getId(), user.getId());
    }


    public String attemptQuiz(Long quizId, String username) {
        Quiz quiz = findById(quizId).orElse(null);
        if (quiz == null) {
            return "Quiz not found!";
        }

        User user = userService.findByUsername(username);
        int attemptCount = getAttemptCountForUser(quiz, user);

        if (attemptCount >= quiz.getAttemptLimit()) {
            return "You have reached the maximum number of attempts for this quiz!";
        }

        incrementAttemptCount(quiz, user);
        return null;
    }

    public void addQuestion(Long quizId,Long questionId){
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new NotFoundException("Quiz not found!"));
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new NotFoundException("Question not found!"));
        quiz.getQuestions().add(question);
        quizRepository.save(quiz);
    }
    public void update(Long id, Quiz quiz){
        Quiz quiz1 = quizRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Quiz not found!"));
        if(quiz.getStartTime().isBefore(LocalDateTime.now())){
            throw new DateException("Start time can not in the past");
        }
        if(quiz.getEndTime().isBefore(quiz.getStartTime())){
            throw new DateException("End time can not before Start Time");
        }
        quiz1.setName(quiz.getName());
        quiz1.setAttemptLimit(quiz.getAttemptLimit());
        quiz1.setDescription(quiz.getDescription());
        quiz1.setUpdatedAt(LocalDateTime.now());
        quiz1.setStartTime(quiz.getStartTime());
        quiz1.setEndTime(quiz.getEndTime());
        quiz1.setId(id);
        quiz1.setQuizType(Quiz.QuizType.CLOSE);
        try {
            SchedulerUtil.startScheduler();

            Scheduler scheduler = SchedulerUtil.getScheduler();
            scheduleJob.scheduleQuizOpenJob(quiz1.getId(), quiz1.getStartTime());
            scheduleJob.scheduleQuizCloseJob(quiz1.getId(), quiz1.getEndTime());
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
        quizRepository.save(quiz1);
    }

}
