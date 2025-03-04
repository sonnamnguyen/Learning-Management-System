package com.example.quiz.service;


import com.example.course.Course;
import com.example.course.CourseRepository;
import com.example.exception.DateException;
import com.example.exception.NotFoundException;
import com.example.quiz.Request.AnswerOptionRequestDTO;
import com.example.quiz.Request.QuestionRequestDTO;
import com.example.quiz.model.*;
import com.example.quiz.repository.AnswerOptionRepository;
import com.example.quiz.repository.AnswerRepository;
import com.example.quiz.repository.QuestionRepository;
import com.example.quiz.repository.QuizRepository;
import com.example.user.User;
import com.example.user.UserService;
import jakarta.annotation.PostConstruct;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static com.example.utils.Helper.getCellValueAsString;

@Service
public class QuizService {

    @Autowired
    private QuestionRepository questionRepository;
    @Autowired
    private AnswerOptionRepository answerOptionRepository;
    @Autowired
    private AnswerRepository answerRepository;
    @Autowired
    private ScheduleJob scheduleJob;
    @Autowired
    private UserService userService;
    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Transactional
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

    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    public List<Quiz> getAllQuizzes() {
        return quizRepository.findAll();
    }

    public Quiz getQuizById(Long id) {
        return quizRepository.findById(id).orElse(null);
    }

    // create question
    @Transactional
    public Question createQuestion(Long quizId, QuestionRequestDTO request) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found with id: " + quizId));

        Question question = new Question();
        question.setQuestionText(request.getQuestionText());
        question.setQuestionType(request.getQuestionType() != null ? request.getQuestionType() : Question.QuestionType.MCQ);
        question.setQuizzes(quiz);

//        int currentQuestionCount = questionRepository.countByQuiz(quiz);
//        question.setQuestionNo(currentQuestionCount + 1);

        question = questionRepository.saveAndFlush(question);
        System.out.println("Question saved with ID: " + question.getId());

        if (request.getAnswerOptions() != null && !request.getAnswerOptions().isEmpty()) {
            Set<AnswerOption> answerOptions = new HashSet<>();

            for (int i = 0; i < request.getAnswerOptions().size(); i++) {
                AnswerOptionRequestDTO optionDTO = request.getAnswerOptions().get(i);
                AnswerOption option = new AnswerOption();

                option.setOptionLabel(generateOptionLabel(i));
                option.setOptionText(optionDTO.getOptionText());
                option.setIsCorrect(optionDTO.isCorrect());
                option.setQuestion(question);

                System.out.println("Creating AnswerOption: " + option.getOptionLabel() + " | Question ID: " + question.getId());

                answerOptions.add(option);
            }

            answerOptionRepository.saveAll(answerOptions);
            question.getAnswerOptions().addAll(answerOptions);
        }

        return question;
    }

    /**
     * Hàm tạo optionLabel theo thứ tự (A, B, C, ..., Z, AA, AB, ...)
     */
    private String generateOptionLabel(int index) {
        StringBuilder label = new StringBuilder();
        do {
            label.insert(0, (char) ('A' + (index % 26)));
            index = index / 26 - 1;
        } while (index >= 0);
        return label.toString();
    }



    public void createQuiz(Quiz quiz) {
        User user = userService.getCurrentUser();
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        // Kiểm tra startTime không nằm trong quá khứ
        if (quiz.getStartTime() != null && quiz.getStartTime().isBefore(LocalDateTime.now())) {
            throw new DateException("Start time cannot be in the past.");
        }

        // Kiểm tra endTime không nhỏ hơn startTime
        if (quiz.getEndTime() != null && quiz.getEndTime().isBefore(quiz.getStartTime())) {
            throw new DateException("End time cannot be before start time.");
        }

        quiz.setCreatedAt(LocalDateTime.now());
        quiz.setCreatedBy(user);
        quiz.setQuizType(Quiz.QuizType.CLOSE);

        Quiz savedQuiz = quizRepository.save(quiz);

        try {
            SchedulerUtil.startScheduler();

            Scheduler scheduler = SchedulerUtil.getScheduler();
            scheduleJob.scheduleQuizOpenJob(savedQuiz.getId(), savedQuiz.getStartTime());
            scheduleJob.scheduleQuizCloseJob(savedQuiz.getId(), savedQuiz.getEndTime());
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    public void scheduleClearCacheJob(Long quizId) {
        try {
            scheduleJob.scheduleClearCacheJob(quizId, LocalDateTime.now().plusMinutes(10));
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    @Transactional
    public void importExcel(MultipartFile file) {
        if (file.isEmpty() || (!file.getOriginalFilename().endsWith(".xls") && !file.getOriginalFilename().endsWith(".xlsx"))) {
            throw new IllegalArgumentException("File không hợp lệ! Hãy chọn một file Excel.");
        }

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int rowCount = sheet.getPhysicalNumberOfRows();
            List<Quiz> quizs = new ArrayList<>();

            for (int i = 1; i < rowCount; i++) { // Bỏ qua hàng tiêu đề
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Cell nameCell = row.getCell(1);
                Cell descriptionCell = row.getCell(2);
                Cell attemptsAllowedCell = row.getCell(3); // attemptsAllowed
                Cell groupCell = row.getCell(4); // course group

                String quizName = (nameCell != null) ? getCellValueAsString(nameCell).trim() : null;
                Integer quizAttemptsAllowed = (attemptsAllowedCell != null) ? (int) attemptsAllowedCell.getNumericCellValue() : null;
                String courseName = (groupCell != null) ? getCellValueAsString(groupCell).trim() : null;

                if (quizName == null || quizName.isEmpty()) {
                    quizName = generateQuizName(); // Tạo tên tự động nếu trống
                }

                if (!existsByName(quizName)) {
                    Quiz quiz = new Quiz();
                    quiz.setName(quizName);
                    quiz.setDescription(null);
                    quiz.setAttemptLimit(1);
                    quiz.setCourse(null);

                    quizs.add(quiz);
                }
            }

            if (!quizs.isEmpty()) {
                quizRepository.save((Quiz) quizs);
            }
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi nhập dữ liệu từ Excel", e);
        }
    }

    /**
     * Tạo tên Quiz tự động theo format "Quiz_YYYYMMdd_HHmmss"
     */
    private String generateQuizName() {
        return "Quiz_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }

    /**
     * Chuyển đổi dữ liệu từ ô Excel thành chuỗi.
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf((long) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case BLANK:
                return "";
            default:
                return "";
        }
    }



    // Method to export roles to Excel
    @Transactional
    public ByteArrayInputStream exportToExcel(List<Quiz> quizs) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Quizs");

        // Create the header row
        String[] headers = {"ID", "Name", "Description", "AttemptsAllowed", "Quiz Group"};
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
        Quiz quiz = quizRepository.findById(quizId).orElse(null);
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


    public void update(Long id, Quiz quiz) {
        Quiz quiz1 = quizRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Quiz not found!"));
        if (quiz.getStartTime().isBefore(LocalDateTime.now())) {
            throw new DateException("Start time can not in the past");
        }
        if (quiz.getEndTime().isBefore(quiz.getStartTime())) {
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
            scheduleJob.scheduleClearCacheJob(quiz1.getId(), LocalDateTime.now());
            scheduleJob.scheduleQuizOpenJob(quiz1.getId(), quiz1.getStartTime());
            scheduleJob.scheduleQuizCloseJob(quiz1.getId(), quiz1.getEndTime());
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
        quizRepository.save(quiz1);
    }


    @Transactional
    public List<Quiz> findQuizzesIgnoreId(Long courseId, Long quizId) {
        return courseId == null ? quizRepository.findByIdNot(quizId) : quizRepository.findByCourseIdAndIdNot(courseId, quizId) ;
    }

    public List<User> getParticipants(Long quizId) {
        // Fetch the quiz by its ID from the repository
        Optional<Quiz> quizOptional = quizRepository.findById(quizId);

        // If quiz is found, return the participants, else return an empty list
        return quizOptional.map(Quiz::getParticipants).orElseGet(List::of);
    }

    public List<User> searchParticipants(Long quizId, String searchTerm) {
        // Fetch the quiz and its participants
        List<User> participants = getParticipants(quizId);

        // Filter participants based on the search term (name or email)
        return participants.stream()
                .filter(user -> user.getUsername().toLowerCase().contains(searchTerm.toLowerCase()) ||
                        user.getEmail().toLowerCase().contains(searchTerm.toLowerCase()))
                .collect(Collectors.toList());
    }

    public ByteArrayInputStream exportParticipantsToExcel(List<User> participants) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Participants");

            // Create header row
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Name");
            headerRow.createCell(1).setCellValue("Email");

            // Fill data rows
            int rowNum = 1;
            for (User participant : participants) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(participant.getUsername());
                row.createCell(1).setCellValue(participant.getEmail());
            }

            // Convert the workbook to ByteArrayInputStream
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                workbook.write(out);
                return new ByteArrayInputStream(out.toByteArray());
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public double calculateScore(Long quizId, Map<String, String> responses) {
        Optional<Quiz> quizOptional = quizRepository.findById(quizId);
        if (quizOptional.isEmpty()) {
            throw new IllegalArgumentException("Quiz not found");
        }

        Quiz quiz = quizOptional.get();
        List<Question> questions = questionRepository.findByQuizzes_Id(quizId);

        if (questions.isEmpty()) return 0.0;

        double totalPoints = 100.0;
        double pointsPerQuestion = totalPoints / questions.size();
        double score = 0;

        for (Question question : questions) {
            String userAnswer = responses.get("question_" + question.getId());
            AnswerOption correctAnswer = answerOptionRepository.findCorrectAnswerByQuestionId(question.getId());

            if (correctAnswer != null && correctAnswer.getOptionText().equals(userAnswer)) {
                score += pointsPerQuestion;
            }
        }
        return score;
    }
    public Duration calculateQuizDuration(int numberOfQuestions) {
        long totalSeconds = numberOfQuestions * 90;
        return Duration.of(totalSeconds, ChronoUnit.SECONDS);
    }
    @PostConstruct
    public void updateQuizStatusOnStartup(){
        updateQuizStatuses();
    }
    @Scheduled(fixedRate = 60000)
    public void updateQuizStatuses() {
        List<Quiz> quizzes = quizRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (Quiz quiz : quizzes) {
            if (now.isAfter(quiz.getStartTime()) && now.isBefore(quiz.getEndTime())) {
                quiz.setQuizType(Quiz.QuizType.OPEN);
            } else {
                quiz.setQuizType(Quiz.QuizType.CLOSE);
            }
            quizRepository.save(quiz);
        }
    }

    @Transactional
    public Set<Question> getQuestionsOfQuiz(String quizName){
        return findByName(quizName).getQuestions();
    }

    public List<Question> totalQuestions(Long quizID)
    {
        return questionRepository.findQuestionsByQuizId(quizID);
    }
}
