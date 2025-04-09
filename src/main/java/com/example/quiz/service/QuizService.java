package com.example.quiz.service;


import com.example.assessment.model.StudentAssessmentAttempt;
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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class QuizService {

    @Autowired
    private QuizTagRepository quizTagRepository;
    @Autowired
    private PracticeResultRepository practiceResultRepository;
    @Autowired
    private ResultRepository resultRepository;
    @Autowired
    private TestSessionRepository testSessionRepository;
    @Autowired
    private QuizTagService quizTagService;
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
    @Autowired
    private QuizParticipantRepository quizParticipantRepository;

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
    public void moveQuestion(Long quizId, Long questionId, int newPosition) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new EntityNotFoundException("Quiz not found"));

        Question questionToMove = questionRepository.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException("Question not found"));

        List<Question> questions = questionRepository.findByQuizzesOrderByQuestionNo(quiz);

        int oldPosition = questionToMove.getQuestionNo();

        if (newPosition < 1 || newPosition > questions.size()) {
            throw new IllegalArgumentException("Invalid position");
        }

        if (oldPosition == newPosition) return;

        for (Question q : questions) {
            if (oldPosition < newPosition) {
                if (q.getQuestionNo() > oldPosition && q.getQuestionNo() <= newPosition) {
                    q.setQuestionNo(q.getQuestionNo() - 1);
                }
            } else { // Di chuyển lên
                if (q.getQuestionNo() >= newPosition && q.getQuestionNo() < oldPosition) {
                    q.setQuestionNo(q.getQuestionNo() + 1);
                }
            }
        }

        questionToMove.setQuestionNo(newPosition);
        questionRepository.saveAll(questions);
        questionRepository.save(questionToMove);
    }



    private String generateOptionLabel(int index) {
        StringBuilder label = new StringBuilder();
        do {
            label.insert(0, (char) ('A' + (index % 26)));
            index = index / 26 - 1;
        } while (index >= 0);
        return label.toString();
    }



    @Autowired
    private Scheduler scheduler;
    public void createQuiz(Quiz quiz, List<Long> tagIds, List<String> newTagNames) {
        User user = userService.getCurrentUser();
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        if (quiz.getStartTime() != null && quiz.getStartTime().isBefore(LocalDateTime.now())) {
            throw new DateException("Start time cannot be in the past.");
        }

        if (quiz.getEndTime() != null && quiz.getEndTime().isBefore(quiz.getStartTime())) {
            throw new DateException("End time cannot be before start time.");
        }

        quiz.setCreatedAt(LocalDateTime.now());
        quiz.setCreatedBy(user);
        quiz.setQuizType(Quiz.QuizType.CLOSE);

        if (newTagNames != null) {
            for (String tagName : newTagNames) {
                QuizTag newTag = quizTagService.createTag(tagName);
                quiz.getTags().add(newTag);
            }
        }

        if (tagIds != null) {
            Set<QuizTag> selectedTags = new HashSet<>(quizTagRepository.findAllById(tagIds));
            quiz.getTags().addAll(selectedTags);
        }

        Quiz savedQuiz = quizRepository.save(quiz);

        try {
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

    public long count() {
        return quizRepository.count();
    }



    @Transactional
    public void addTagsToQuiz(Long quizId, List<Long> tagIds, List<String> newTagNames) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new NotFoundException("Quiz not found"));

        // Khởi tạo Set tags nếu chưa có
        if (quiz.getTags() == null) {
            quiz.setTags(new HashSet<>());
        }

        // Thêm tags mới nếu có
        if (newTagNames != null && !newTagNames.isEmpty()) {
            for (String tagName : newTagNames) {
                if (tagName != null && !tagName.trim().isEmpty()) {
                    QuizTag newTag = quizTagService.createTag(tagName.trim());
                    quiz.getTags().add(newTag);
                }
            }
        }

        // Thêm tags hiện có
        if (tagIds != null && !tagIds.isEmpty()) {
            Set<QuizTag> selectedTags = new HashSet<>(quizTagRepository.findAllById(tagIds));
            quiz.getTags().addAll(selectedTags);
        }

        quizRepository.save(quiz);
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

            for (int i = 1; i < rowCount; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Cell nameCell = row.getCell(1);
                Cell descriptionCell = row.getCell(2);
                Cell attemptsAllowedCell = row.getCell(3);
                Cell groupCell = row.getCell(4);

                String quizName = (nameCell != null) ? getCellValueAsString(nameCell).trim() : null;
                Integer quizAttemptsAllowed = (attemptsAllowedCell != null) ? (int) attemptsAllowedCell.getNumericCellValue() : null;
                String courseName = (groupCell != null) ? getCellValueAsString(groupCell).trim() : null;

                if (quizName == null || quizName.isEmpty()) {
                    quizName = generateQuizName();
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

        String[] headers = {"ID", "Name", "Description", "AttemptsAllowed", "Quiz Group"};
        sheet.createRow(0).createCell(0).setCellValue(headers[0]);
        sheet.getRow(0).createCell(1).setCellValue(headers[1]);
        sheet.getRow(0).createCell(2).setCellValue(headers[2]);
        sheet.getRow(0).createCell(3).setCellValue(headers[3]);
        sheet.getRow(0).createCell(4).setCellValue(headers[4]);

        int rowNum = 1;
        for (Quiz quiz : quizs) {
            sheet.createRow(rowNum).createCell(0).setCellValue(quiz.getId());
            sheet.getRow(rowNum).createCell(1).setCellValue(quiz.getName());
            sheet.getRow(rowNum).createCell(2).setCellValue(quiz.getDescription());
            sheet.getRow(rowNum).createCell(4).setCellValue(quiz.getCourse().getName());
            rowNum++;
        }

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
        return courseId == null ? quizRepository.findByIdNot(quizId) : quizRepository.findByCourseIdAndIdNot(courseId, quizId);
    }

    public List<User> getParticipants(Long quizId) {
        Optional<Quiz> quizOptional = quizRepository.findById(quizId);

        return quizOptional.map(Quiz::getParticipants).orElseGet(List::of);
    }

    public List<User> searchParticipants(Long quizId, String searchTerm) {
        List<User> participants = getParticipants(quizId);

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


    public double calculateScore(List<String> questionId, Long assessmentId, MultiValueMap<String, String> responses, User user, StudentAssessmentAttempt studentAssessmentAttemptId) {
        if (questionId == null || questionId.isEmpty()) {
            throw new IllegalArgumentException("Danh sách câu hỏi không được rỗng");
        }

        List<Long> questionIds = questionId.stream().map(Long::parseLong).collect(Collectors.toList());
        List<Question> questions = questionRepository.findAllById(questionIds);
        if (questions.isEmpty()) {
            return 0.0;
        }

        boolean isPractice = studentAssessmentAttemptId==null;

        if (isPractice){
            assessmentId = null ;
        }


        TestSession session = new TestSession();
        session.setUser(user);
        session.setStartTime(LocalDateTime.now());
        if(!isPractice) {
            session.setAssessmentId(assessmentId);
            session.setStudentAssessmentAttempt(studentAssessmentAttemptId);
        }

        session.setCheckPractice(isPractice);
        testSessionRepository.save(session);

        long totalScoredQuestions = questions.stream()
                .filter(q -> !q.getQuestionType().toString().equals("TEXT"))
                .count();

        double totalPoints = 100.0;
        double pointsPerQuestion = totalScoredQuestions > 0 ? totalPoints / totalScoredQuestions : 0;
        double score = 0;

        List<AnswerOption> correctOptionsList = answerOptionRepository.findCorrectAnswersByQuestionIds(questionIds);
        Map<Long, List<Long>> correctAnswersMap = correctOptionsList.stream()
                .collect(Collectors.groupingBy(
                        ao -> ao.getQuestion().getId(),
                        Collectors.mapping(AnswerOption::getId, Collectors.toList())
                ));

        for (Question question : questions) {
            List<String> userAnswerIdsStr = responses.get("answers[" + question.getId() + "]");
            List<Long> correctAnswerIds = correctAnswersMap.getOrDefault(question.getId(), Collections.emptyList());

            if ("TEXT".equalsIgnoreCase(question.getQuestionType().toString())) {
                if (userAnswerIdsStr != null && !userAnswerIdsStr.isEmpty()) {
                    String textAnswer = userAnswerIdsStr.get(0);
                    Answer textResponse = new Answer();
                    textResponse.setQuestion(question);
                    textResponse.setAnswerText(textAnswer);
                    textResponse.setIsCorrect(null);
                    textResponse.setSelectedOption(null);
                    textResponse.setTestSession(session);
                    answerRepository.save(textResponse);
                }
                continue;
            }

            double questionScore = 0;

            if (userAnswerIdsStr != null && !userAnswerIdsStr.isEmpty()) {
                List<Long> userAnswerIds = userAnswerIdsStr.stream()
                        .filter(id -> id.matches("\\d+"))
                        .map(Long::parseLong)
                        .collect(Collectors.toList());

                int totalCorrect = correctAnswerIds.size();
                int userCorrectCount = (int) userAnswerIds.stream().filter(correctAnswerIds::contains).count();
                int totalIncorrect = (int) userAnswerIds.stream().filter(id -> !correctAnswerIds.contains(id)).count();
                int excessSelections = userAnswerIds.size() - totalCorrect;

                if (excessSelections > 0) {
                    questionScore = 0; // Nếu chọn quá số đáp án đúng -> 0 điểm
                } else {
                    if (totalCorrect > 0) {
                        double pointsPerCorrectAnswer = pointsPerQuestion / totalCorrect;
                        questionScore = pointsPerCorrectAnswer * userCorrectCount;
                    }
                }

                for (Long selectedOptionId : userAnswerIds) {
                    AnswerOption selectedOption = answerOptionRepository.findById(selectedOptionId).orElse(null);
                    if (selectedOption != null) {
                        Answer answer = new Answer();
                        answer.setSelectedOption(selectedOption);
                        answer.setQuestion(question);
                        answer.setAnswerText(selectedOption.getOptionText());
                        answer.setIsCorrect(correctAnswerIds.contains(selectedOptionId));
                        answer.setScore((excessSelections > 0) ? 0.0 : (correctAnswerIds.contains(selectedOptionId) ? (pointsPerQuestion / correctAnswerIds.size()) : 0.0));
                        answer.setTestSession(session);
                        answerRepository.save(answer);
                    }
                }
            }

            score += questionScore;

            if (isPractice) {
                practiceResultRepository.save(new PracticeResult(session, question, questionScore > 0, questionScore));
            } else {
                resultRepository.save(new Result(session, question, questionScore > 0, questionScore));
            }

            List<AnswerOption> allOptions = answerOptionRepository.findByQuestionId(question.getId());
            for (AnswerOption option : allOptions) {
                if (!responses.containsKey("answers[" + question.getId() + "]") || !responses.get("answers[" + question.getId() + "]").contains(String.valueOf(option.getId()))) {
                    Answer answer = new Answer();
                    answer.setSelectedOption(option);
                    answer.setQuestion(question);
                    answer.setAnswerText(option.getOptionText());
                    answer.setIsCorrect(null);
                    answer.setScore(0.0);
                    answerRepository.save(answer);
                }
            }
        }

        return BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }




    public Duration calculateQuizDuration(int numberOfQuestions) {
        long totalSeconds = numberOfQuestions * 90;
        return Duration.of(totalSeconds, ChronoUnit.SECONDS);
    }

    @Transactional
    public Set<Question> getQuestionsOfQuiz(String quizName) {
        return findByName(quizName).getQuestions();
    }

    public List<Question> totalQuestions(Long quizID) {
        return questionRepository.findQuestionsByQuizId(quizID);
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

    public Map<String, Object> getScoreByQuiz(Long studentId) {
        List<Quiz> quizzes = quizRepository.findByParticipants_Id(studentId);
        if (quizzes.isEmpty()) {
            throw new NotFoundException("Quiz not found!");
        }
        System.out.println("Số lượng quiz tìm thấy: " + quizzes.size());

        List<PracticeResult> results = practiceResultRepository.findResultByTestSession_User_Id(studentId);
        if (results.isEmpty()) {
            throw new NotFoundException("No results found for this student!");
        }
        System.out.println("✅ Số lượng kết quả bài làm: " + results.size());

        Map<Long, String> quizMap = quizzes.stream()
                .collect(Collectors.toMap(Quiz::getId, Quiz::getName));
        System.out.println("Quiz Map: " + quizMap);

        Map<String, Integer> quizScores = new LinkedHashMap<>();
        Map<String, String> quizTimestamps = new LinkedHashMap<>();
        Map<String, Long> quizDurations = new LinkedHashMap<>(); // Đơn vị: phút
        Map<String, String> quizCourses = new LinkedHashMap<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm:ss");

        for (PracticeResult result : results) {
            Question question = result.getQuestion();
            if (question != null) {
                Quiz quiz = question.getQuizzes();
                if (quiz != null) {
                    String quizTitle = quizMap.get(quiz.getId());
                    String courseName = quiz.getCourse() != null ? quiz.getCourse().getName() : "Unknown Course";
                    if (quizTitle != null) {
                        TestSession session = result.getTestSession();
                        if (session != null && session.getEndTime() != null && session.getStartTime() != null) {
                            String timestamp = session.getEndTime().format(formatter);
                            long durationMinutes = Duration.between(session.getStartTime(), session.getEndTime()).toMinutes();

                            String key = quizTitle + " | " + timestamp;

                            quizScores.put(key, quizScores.getOrDefault(key, 0) + (int) result.getScore());
                            quizTimestamps.put(key, timestamp);
                            quizDurations.put(key, durationMinutes);
                            quizCourses.put(key, courseName);
                            System.out.println("Added to quizCourses -> Key: " + key + ", Course: " + courseName);
                        }
                    }
                }
            }
        }

        System.out.println("Final quizCourses: " + quizCourses);


        List<Integer> scoreDifferences = new ArrayList<>();
        List<Integer> scores = new ArrayList<>(quizScores.values());

        if (!scores.isEmpty()) {
            scoreDifferences.add(0);
            for (int i = 1; i < scores.size(); i++) {
                scoreDifferences.add(scores.get(i) - scores.get(i - 1));
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("quizScores", quizScores);
        response.put("scoreDifferences", scoreDifferences);
        response.put("quizTimestamps", quizTimestamps);
        response.put("quizDurations", quizDurations);
        response.put("quizCourse",quizCourses);

        System.out.println("Dữ liệu trả về: " + response);
        return response;
    }

    public Map<Long, String> getQuizCourses(Long studentId) {
        List<Quiz> quizzes = quizRepository.findByParticipants_Id(studentId);

        Map<Long, String> quizCourses = new HashMap<>();

        for (Quiz quiz : quizzes) {
            if (quiz.getCourse() != null) {
                quizCourses.put(quiz.getId(), quiz.getCourse().getName());
            }
        }

        return quizCourses;
    }

    public Map<String, Integer> getQuizFromCourse(Long studentId) {
        User user = userService.getUserById(studentId);
        if (user == null) {
            throw new NotFoundException("User not found !");
        }
        System.out.println("User tìm thấy: " + user.getId());

        List<Quiz> quizzes = quizRepository.findByParticipants_Id(user.getId());
        if (quizzes.isEmpty()) {
            throw new NotFoundException("Quiz not found !");
        }
        System.out.println("Số lượng quiz tìm thấy: " + quizzes.size());

        Map<String, Integer> courseQuizCount = new HashMap<>();
        for (Quiz quiz : quizzes) {
            Course course = quiz.getCourse();
            if (course != null) {
                courseQuizCount.put(course.getName(), courseQuizCount.getOrDefault(course.getName(), 0) + 1);
            }
        }

        System.out.println("Số lượng quiz theo khóa học: " + courseQuizCount);
        return courseQuizCount;
    }


    @Transactional
    public Page<Quiz> findByCourseId(Long courseId, Pageable pageable) {
        return quizRepository.findByCourseId(courseId, pageable);
    }

    @Transactional
    public Page<Quiz> searchByCourseAndName(Long courseId, String searchQuery, Pageable pageable) {
        return quizRepository.findByCourseIdAndNameContainingIgnoreCase(courseId, searchQuery, pageable);
    }
    @Transactional
    public Question updateQuestion(Long questionId, QuestionRequestDTO request) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found with id: " + questionId));

        question.setQuestionText(request.getQuestionText());
        question.setQuestionType(request.getQuestionType());

        answerOptionRepository.deleteAll(question.getAnswerOptions());
        question.getAnswerOptions().clear();

        if (request.getAnswerOptions() != null && !request.getAnswerOptions().isEmpty()) {
            for (int i = 0; i < request.getAnswerOptions().size(); i++) {
                AnswerOptionRequestDTO optionDTO = request.getAnswerOptions().get(i);
                AnswerOption option = new AnswerOption();

                option.setOptionLabel(generateOptionLabel(i));
                option.setOptionText(optionDTO.getOptionText());
                option.setIsCorrect(optionDTO.isCorrect());
                option.setQuestion(question);

                question.getAnswerOptions().add(option);
            }
        }

        return questionRepository.save(question);
    }

    @Transactional
    public Page<Quiz> filterByTags(List<Long> tagIds, Pageable pageable) {
        return quizRepository.findDistinctByTags_IdIn(tagIds, pageable);
    }


    @Transactional
    public void removeTagFromQuiz(Long quizId, Long tagId) {
        Quiz quiz = findById(quizId)
                .orElseThrow(() -> new EntityNotFoundException("Quiz not found"));

        QuizTag tag = quizTagService.getQuizTagById(tagId);
        if (tag == null) {
            throw new EntityNotFoundException("Tag not found");
        }

        if (quiz.getTags().remove(tag)) {
            quizRepository.save(quiz);
        } else {
            throw new EntityNotFoundException("Tag is not associated with this quiz");
        }
    }

    @Transactional
    public void updateQuizTags(Long quizId, List<Long> tagIds) {
        Quiz quiz = findById(quizId)
                .orElseThrow(() -> new EntityNotFoundException("Quiz not found"));

        if (tagIds == null) {
            quiz.getTags().clear();
        } else {
            Set<QuizTag> tags = tagIds.stream()
                    .map(tagId -> quizTagService.getQuizTagById(tagId))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            quiz.setTags(tags);
        }

        quizRepository.save(quiz);
    }
    @Transactional(readOnly = true)
    public Quiz getQuizWithTags(Long id) {
        return quizRepository.findQuizWithTags(id)
                .orElseThrow(() -> new NotFoundException("Quiz not found"));
    }
    @Transactional
    public List<QuizTag> getQuizTags(Long quizId) {
        Quiz quiz = findById(quizId)
                .orElseThrow(() -> new EntityNotFoundException("Quiz not found"));
        return new ArrayList<>(quiz.getTags());
    }
    public Page<Quiz> findQuizesWithFilters(Long courseId, String searchQuery, List<Long> tagIds, Pageable pageable) {
        if (tagIds != null && !tagIds.isEmpty()) {
            if (courseId != null) {
                if (searchQuery != null && !searchQuery.isEmpty()) {
                    return quizRepository.findByCourseIdAndNameContainingIgnoreCaseAndTagsIn(courseId, searchQuery, tagIds, pageable);
                } else {
                    return quizRepository.findByCourseIdAndTagsIn(courseId, tagIds, pageable);
                }
            } else if (searchQuery != null && !searchQuery.isEmpty()) {
                return quizRepository.findByNameContainingIgnoreCaseAndTagsIn(searchQuery, tagIds, pageable);
            } else {
                return filterByTags(tagIds, pageable);
            }
        } else {
            if (courseId != null) {
                if (searchQuery != null && !searchQuery.isEmpty()) {
                    return searchByCourseAndName(courseId, searchQuery, pageable);
                } else {
                    return findByCourseId(courseId, pageable);
                }
            } else if (searchQuery != null && !searchQuery.isEmpty()) {
                // Chỉ search
                return search(searchQuery, pageable);
            } else {
                return findAll(pageable);
            }
        }
    }

    public Quiz jsonToQuiz(String json) {
        ObjectMapper objectMapper = new ObjectMapper();

        Quiz quiz = new Quiz();
        // Sử dụng jsonToQuestionSet để lấy danh sách câu hỏi

        try {
            JsonNode rootNode = objectMapper.readTree(json);

            String questionsJson = rootNode.get("questions").toString();
            // Deserialize "questions" thành danh sách Question
            Set<Question> questions = objectMapper.readValue(questionsJson, new TypeReference<Set<Question>>() {
            });

            quiz.setName(rootNode.get("name").asText());
            quiz.setDescription(rootNode.get("description").asText());

            quiz.setStartTime(LocalDateTime.parse(rootNode.get("startTime").asText()));
            quiz.setEndTime(LocalDateTime.parse(rootNode.get("endTime").asText()));

            quiz.setAttemptLimit(rootNode.get("attemptLimit").asInt());
            quiz.setCourse(courseRepository.findById(rootNode.get("course").asLong()));

            quiz.setQuizType(Quiz.QuizType.CLOSE);

            quiz.setQuizCategory(rootNode.get("quizCategory").asText().equals("EXAM")
                    ? Quiz.QuizCategory.EXAM : Quiz.QuizCategory.PRACTICE);

            quiz.setCreatedBy(userService.getUserById(rootNode.get("createdBy").asLong()));

            if (questions != null && !questions.isEmpty()) {
                int questionNo = 1;
                for (Question question : questions) {
                    question.setQuestionNo(questionNo);
                    questionNo++;
                    int answerNo = 0;
                    for (AnswerOption answerOption : question.getAnswerOptions()) {
                        answerOption.setOptionLabel(generateOptionLabel(answerNo));
                        answerNo++;
                    }
                    questionRepository.save(question);
                    quiz.addQuestion(question);
                }
            }

            quizRepository.save(quiz);

            Course course = courseRepository.findById(rootNode.get("course").asLong());
            course.addQuiz(quiz);
            courseRepository.save(course);

            SchedulerUtil.startScheduler();

            Scheduler scheduler = SchedulerUtil.getScheduler();
            scheduleJob.scheduleQuizOpenJob(quiz.getId(), quiz.getStartTime());
            scheduleJob.scheduleQuizCloseJob(quiz.getId(), quiz.getEndTime());

            return quiz;
        } catch (Exception e) {
            return null;
        }

    }




}
