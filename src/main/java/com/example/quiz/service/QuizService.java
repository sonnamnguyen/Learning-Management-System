package com.example.quiz.service;


import com.example.course.Course;
import com.example.course.CourseRepository;
import com.example.exception.DateException;
import com.example.exception.NotFoundException;
import com.example.quiz.Request.AnswerOptionRequestDTO;
import com.example.quiz.Request.QuestionRequestDTO;
import com.example.quiz.model.*;
import com.example.quiz.repository.*;
import com.example.user.User;
import com.example.user.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

@Service
public class QuizService {


    @Autowired
    private PracticeResultRepository practiceResultRepository;
    @Autowired
    private ResultRepository resultRepository;
    @Autowired
    private TestSessionRepository testSessionRepository;

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

    @Transactional
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

        int currentQuestionCount = questionRepository.countByQuiz(quiz);

        Question question = new Question();
        question.setQuestionText(request.getQuestionText());
        question.setQuestionType(request.getQuestionType() != null ? request.getQuestionType() : Question.QuestionType.MCQ);
        question.setQuizzes(quiz);
        question.setQuestionNo(currentQuestionCount + 1);


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

    @Transactional
    public void updateQuestionPosition(Long quizId, Long questionId, int newPosition) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        int oldPosition = question.getQuestionNo();

        if (oldPosition < newPosition) {
            // Di chuyển xuống => Dịch chuyển các câu ở giữa lên 1
            questionRepository.decrementQuestionNo(quiz, oldPosition, newPosition);
        } else if (oldPosition > newPosition) {
            // Di chuyển lên => Dịch chuyển các câu ở giữa xuống 1
            questionRepository.incrementQuestionNo(quiz, newPosition, oldPosition);
        }

        // Cập nhật vị trí mới cho câu hỏi
        question.setQuestionNo(newPosition);
        questionRepository.save(question);
    }

    @Transactional
    public void moveQuestion(Long quizId, Long questionId, int newPosition) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new EntityNotFoundException("Quiz not found"));

        Question questionToMove = questionRepository.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException("Question not found"));

        // Lấy danh sách câu hỏi theo thứ tự
        List<Question> questions = questionRepository.findByQuizzesOrderByQuestionNo(quiz);

        int oldPosition = questionToMove.getQuestionNo();

        if (newPosition < 1 || newPosition > questions.size()) {
            throw new IllegalArgumentException("Invalid position");
        }

        // Nếu vị trí không thay đổi thì không làm gì
        if (oldPosition == newPosition) return;

        // Cập nhật vị trí của các câu hỏi khác
        for (Question q : questions) {
            if (oldPosition < newPosition) { // Di chuyển xuống
                if (q.getQuestionNo() > oldPosition && q.getQuestionNo() <= newPosition) {
                    q.setQuestionNo(q.getQuestionNo() - 1);
                }
            } else { // Di chuyển lên
                if (q.getQuestionNo() >= newPosition && q.getQuestionNo() < oldPosition) {
                    q.setQuestionNo(q.getQuestionNo() + 1);
                }
            }
        }

        // Cập nhật vị trí mới cho câu hỏi
        questionToMove.setQuestionNo(newPosition);
        questionRepository.saveAll(questions);
        questionRepository.save(questionToMove);
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



    @Autowired
    private Scheduler scheduler; // Inject từ Spring, không dùng SchedulerUtil nữa

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
            // 🔹 Chỉ lên lịch job nếu startTime và endTime không null
            if (savedQuiz.getStartTime() != null) {
                JobKey openJobKey = JobKey.jobKey("quizOpenJob-" + savedQuiz.getId(), "quizJobs");
                if (!scheduler.checkExists(openJobKey)) {
                    scheduleJob.scheduleQuizOpenJob(savedQuiz.getId(), savedQuiz.getStartTime());
                }
            }

            if (savedQuiz.getEndTime() != null) {
                JobKey closeJobKey = JobKey.jobKey("quizCloseJob-" + savedQuiz.getId(), "quizJobs");
                if (!scheduler.checkExists(closeJobKey)) {
                    scheduleJob.scheduleQuizCloseJob(savedQuiz.getId(), savedQuiz.getEndTime());
                }
            }

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
            throw new DateException("Start time cannot be in the past");
        }
        if (quiz.getEndTime().isBefore(quiz.getStartTime())) {
            throw new DateException("End time cannot be before Start Time");
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
            JobKey openJobKey = JobKey.jobKey("quizOpenJob-" + quiz1.getId(), "quizJobs");
            JobKey closeJobKey = JobKey.jobKey("quizCloseJob-" + quiz1.getId(), "quizJobs");

            if (scheduler.checkExists(openJobKey)) {
                scheduler.deleteJob(openJobKey);
            }
            if (scheduler.checkExists(closeJobKey)) {
                scheduler.deleteJob(closeJobKey);
            }

            scheduleJob.scheduleQuizOpenJob(quiz1.getId(), quiz1.getStartTime());
            scheduleJob.scheduleQuizCloseJob(quiz1.getId(), quiz1.getEndTime());

            scheduleJob.scheduleClearCacheJob(quiz1.getId(), LocalDateTime.now());

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

//    public double calculateScore(Long quizId, Map<String, String> responses) {
//        Optional<Quiz> quizOptional = quizRepository.findById(quizId);
//        if (quizOptional.isEmpty()) {
//            throw new IllegalArgumentException("Quiz not found");
//        }
//
//        Quiz quiz = quizOptional.get();
//        List<Question> questions = questionRepository.findByQuizzes_Id(quizId);
//
//        if (questions.isEmpty()) return 0.0;
//
//        double totalPoints = 100.0;
//        double pointsPerQuestion = totalPoints / questions.size();
//        double score = 0;
//
//        for (Question question : questions) {
//            String userAnswer = responses.get("question_" + question.getId());
//            AnswerOption correctAnswer = answerOptionRepository.findCorrectAnswerByQuestionId(question.getId());
//
//            if (correctAnswer != null && correctAnswer.getOptionText().equals(userAnswer)) {
//                score += pointsPerQuestion;
//            }
//        }
//        return score;
//    }
public double calculateScore(List<String> questionId, Long assessmentId, Map<String, String> responses, User user) {
    // Kiểm tra danh sách questionId
    if (questionId == null || questionId.isEmpty()) {
        throw new IllegalArgumentException("Question ID list cannot be null or empty");
    }

    // Truy vấn danh sách câu hỏi từ questionId
    List<Question> questions = questionRepository.findAllById(
            questionId.stream().map(Long::parseLong).collect(Collectors.toList())
    );
    if (questions.isEmpty()) {
        return 0.0;
    }

    // Xác định checkPractice dựa trên assessmentId
    // Nếu assessmentId là null, giả định là PRACTICE
    boolean isPractice = (assessmentId == null);

    // Tạo TestSession
    TestSession session = new TestSession();
    session.setUser(user);
    session.setStartTime(LocalDateTime.now());
    session.setAssessmentId(assessmentId);
    session.setCheckPractice(isPractice);
    testSessionRepository.save(session);

    double totalPoints = 100.0;
    double pointsPerQuestion = totalPoints / questions.size();
    double score = 0;

    for (Question question : questions) {
        String userAnswerId = responses.get("answers[" + question.getId() + "]");
        AnswerOption correctAnswer = answerOptionRepository.findCorrectAnswerByQuestionId(question.getId());

        if (userAnswerId != null && correctAnswer != null) {
            Long userAnswerIdLong = Long.parseLong(userAnswerId);
            AnswerOption selectedOption = answerOptionRepository.findById(userAnswerIdLong).orElse(null);

            if (selectedOption != null && selectedOption.getId().equals(correctAnswer.getId())) {
                score += pointsPerQuestion;

                if (session.isCheckPractice()) {
                    // Lưu vào practice_result cho PRACTICE
                    PracticeResult practiceResult = new PracticeResult(session, question, true, pointsPerQuestion);
                    practiceResultRepository.save(practiceResult);
                } else {
                    // Lưu vào result cho EXAM
                    Result result = new Result(session, question, true, pointsPerQuestion);
                    resultRepository.save(result);
                }
            }
        }
    }

    return score;
}
    public Duration calculateQuizDuration(int numberOfQuestions) {
        long totalSeconds = numberOfQuestions * 90;
        return Duration.of(totalSeconds, ChronoUnit.SECONDS);
    }

    @Transactional
    public Set<Question> getQuestionsOfQuiz(String quizName){
        return findByName(quizName).getQuestions();
    }

    public List<Question> totalQuestions(Long quizID)
    {
        return questionRepository.findQuestionsByQuizId(quizID);
    }

    public void submitQuiz(Long quizId, String username, Map<String, String> responses) {
        User user = userService.findByUsername(username);
        List<Question> questions = totalQuestions(quizId);
        int correctCount = 0;

        Map<Long, Long> selectedAnswers = new HashMap<>();
        Map<Long, Long> correctAnswers = new HashMap<>();

        System.out.println("Dữ liệu nhận được từ form: " + responses); // Kiểm tra log

        for (Question question : questions) {
            String selectedOptionId = responses.get("answers[" + question.getId() + "]");
            if (selectedOptionId != null) {
                Long selectedOptionLong = Long.parseLong(selectedOptionId);
                selectedAnswers.put(question.getId(), selectedOptionLong);

                AnswerOption selectedOption = answerOptionRepository.findById(selectedOptionLong).orElse(null);
                AnswerOption correctOption = answerOptionRepository.findCorrectAnswerByQuestionId(question.getId());

                if (correctOption != null) {
                    correctAnswers.put(question.getId(), correctOption.getId());
                }

                if (selectedOption != null) {
                    if (selectedOption.getIsCorrect()) {
                        correctCount++;
                    }
                    Answer answer = new Answer();
                    answer.setSelectedOption(selectedOption);
                    answer.setQuestion(question);
                    answer.setAnswerText(selectedOption.getOptionText());
                    answer.setIsCorrect(selectedOption.getIsCorrect());
                    answerRepository.save(answer);
                }
            }
        }

        double score = (double) correctCount / questions.size() * 10; // Thang điểm 10

    }

    public int countCorrectAnswers(Long quizId, Map<String, String> responses) {
        List<Question> questions = questionRepository.findByQuizzes_Id(quizId);
        int correctCount = 0;

        for (Question question : questions) {
            String userAnswerId = responses.get("question_" + question.getId());

            if (userAnswerId != null) {
                Long selectedOptionId = Long.parseLong(userAnswerId);
                AnswerOption selectedOption = answerOptionRepository.findById(selectedOptionId).orElse(null);

                if (selectedOption != null && selectedOption.getIsCorrect()) {
                    correctCount++;
                }
            }
        }
        return correctCount;
    }

    /**
     * find TestSession by assessmentId và userId.
     *
     * @param assessmentId ID of assessment
     * @param userId ID of user
     * Contact gr 2 if this function error
     */
    public Optional<TestSession> findTestSessionByAssessmentIdAndUserId(Long assessmentId, Long userId) {
        return testSessionRepository.findByAssessmentIdAndUserId(assessmentId, userId);
    }


}
