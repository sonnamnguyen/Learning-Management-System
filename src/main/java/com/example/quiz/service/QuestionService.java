package com.example.quiz.service;

import com.example.course.Course;
import com.example.course.CourseRepository;
import com.example.course.CourseService;
import com.example.exception.InputException;
import com.example.exception.NotFoundException;
import com.example.quiz.model.AnswerOption;
import com.example.quiz.model.Question;
import com.example.quiz.model.Question.QuestionType;
import com.example.quiz.model.Quiz;
import com.example.quiz.model.SchedulerUtil;
import com.example.quiz.repository.AnswerOptionRepository;
import com.example.quiz.repository.QuestionRepository;
import com.example.quiz.repository.QuizRepository;
import com.example.tools.GenerateRandomService;
import com.example.user.User;
import com.example.user.UserService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;


@Service
public class QuestionService {
    @Autowired
    private QuizRepository quizRepository;
    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ScheduleJob scheduleJob;
    @Autowired
    private UserService userService;

    @Autowired
    private GenerateRandomService generateRandomService;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseService courseService;

    @Autowired
    private AnswerOptionRepository answerOptionRepository;
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


//    public void importExcel(MultipartFile _file_) {
//        try (Workbook workbook = new XSSFWorkbook(_file_.getInputStream())) {
//            Sheet sheet = workbook.getSheetAt(0);
//            int rowCount = sheet.getPhysicalNumberOfRows();
//            List<Question> questions = new ArrayList<>();
//
//            for (int i = 1; i < rowCount; i++) { // Start from 1 to skip the header row
//                Row row = sheet.getRow(i);
//                if (row != null) {
//                    // Extract cells
//                    Cell quizNameCell = row.getCell(0);
//                    Cell questionTextCell = row.getCell(1);
//                    Cell questionTypeCell = row.getCell(2);
//                    Cell pointsCell = row.getCell(3);
//
//                    if (quizNameCell != null) {
//                        String quizName = getCellValueAsString(quizNameCell).trim();
//                        String questionText = questionTextCell != null ? getCellValueAsString(questionTextCell).trim() : null;
//                        String questionTypeString = questionTypeCell != null ? getCellValueAsString(questionTypeCell).trim() : null;
//                        Integer points = pointsCell != null ? Integer.parseInt(getCellValueAsString(pointsCell).trim()) : null;
//
//                        Quiz quiz = quizService.findByName(quizName);
//                        if (quiz != null && questionText != null && questionTypeString != null) {
//                            QuestionType questionType = QuestionType.valueOf(questionTypeString.toUpperCase());
//
//                            Question question = new Question();
//                            // Use addQuiz to maintain many-to-many relationship
//                            question.addQuiz(quiz);
//                            question.setQuestionText(questionText);
//                            question.setQuestionType(questionType);
//                            question.setPoints(points);
//                            questions.add(question);
//                        }
//                    }
//                }
//            }
//
//            for (Question question : questions) {
//                questionRepository.save(question); // Save each Question with its associated Quizzes
//            }
//
//        } catch (IOException _e_) {
//            throw new RuntimeException("Error importing questions from Excel", _e_);
//        }
//    }

    private String getCellValueAsString(Cell cell) {
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell);
    }

//    public ByteArrayInputStream exportToExcel(List<Question> questions) {
//        XSSFWorkbook workbook = new XSSFWorkbook();
//        Sheet sheet = workbook.createSheet("Questions");
//
//        // Header row
//        String[] headers = {"ID", "Quiz(s)", "Question Text", "Question Type", "Points"};
//        Row headerRow = sheet.createRow(0);
//        for (int i = 0; i < headers.length; i++) {
//            headerRow.createCell(i).setCellValue(headers[i]);
//        }
//
//        // Data rows
//        int rowNum = 1;
//        for (Question question : questions) {
//            Row row = sheet.createRow(rowNum++);
//
//            // ID
//            row.createCell(0).setCellValue(question.getId());
//
//            // Quiz(s)
//            String quizNames = question.getQuiz().stream()
//                    .map(Quiz::getName)
//                    .reduce((name1, name2) -> name1 + ", " + name2)
//                    .orElse("");
//            row.createCell(1).setCellValue(quizNames);
//
//            // Question Text
//            row.createCell(2).setCellValue(question.getQuestionText());
//
//            // Question Type
//            row.createCell(3).setCellValue(question.getQuestionType().name());
//
//            // Points
//            row.createCell(4).setCellValue(question.getPoints());
//        }
//
//        // Auto-size columns to fit content
//        for (int i = 0; i < headers.length; i++) {
//            sheet.autoSizeColumn(i);
//        }
//
//        // Write the output to a byte array
//        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//        try {
//            workbook.write(outputStream);
//        } catch (IOException e) {
//            e.printStackTrace(); // Handle exception as needed
//        }
//
//        // Close the workbook to free resources
//        try {
//            workbook.close();
//        } catch (IOException e) {
//            e.printStackTrace(); // Handle exception as needed
//        }
//
//        return new ByteArrayInputStream(outputStream.toByteArray());
//    }

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
//    @Transactional
//    public void create(Long quizId,Question question) throws NotFoundException{
//        Quiz quiz = quizRepository.findById(quizId)
//                .orElseThrow(() -> new NotFoundException("Quiz not found!"));
//        question.addQuiz(quiz);
//        questionRepository.save(question);
//        quizRepository.save(quiz);
//    }

    public List<Question> findQuestionsByQuizId(Long id) {
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Quiz not found!"));
        return new ArrayList<>(quiz.getQuestions());
    }

    @Transactional
    public void importExcel(MultipartFile _file_, String courseName) {
        try (Workbook workbook = new XSSFWorkbook(_file_.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int rowCount = sheet.getPhysicalNumberOfRows();
            List<Question> questions = new ArrayList<>();
            List<String> optionLabelList = List.of("A", "B", "C", "D");
            //List<AnswerOption> answerOptionList = new ArrayList<>();

            for (int i = 1; i < rowCount; i++) { // Start from 1 to skip the header row
                // QUESTION ------------------------------------------
                Row row = sheet.getRow(i);
                if (row != null) {
                    // Extract cells
                    Cell questionTextCell = row.getCell(3);
                    Cell questionTypeCell = row.getCell(4);
                    Cell answerCell = row.getCell(5);
                    Cell optionA = row.getCell(6);
                    Cell optionB = row.getCell(7);
                    Cell optionC = row.getCell(8);
                    Cell optionD = row.getCell(9);

                    String questionText = questionTextCell != null ? getCellValueAsString(questionTextCell).trim() : null;
                    String questionTypeString = questionTypeCell != null ? getCellValueAsString(questionTypeCell).trim() : null;
                    List<Cell> optionCellList = List.of(optionA, optionB, optionC, optionD);

                    List<AnswerOption> answerOptionListTemp = new ArrayList<>();

                    // Tạo các obj answer option
                    for(int y = 0; y < 4; y++){
                        AnswerOption answerOption = new AnswerOption();
                        answerOption.setOptionLabel(optionLabelList.get(y));
                        answerOption.setOptionText(getCellValueAsString(optionCellList.get(y)));

                        if(Arrays.stream(getCellValueAsString(answerCell).split(", "))
                                .toList()
                                .contains(optionLabelList.get(y))){
                            answerOption.setIsCorrect(true);
                        } else {
                            answerOption.setIsCorrect(false);
                        }

                        answerOptionListTemp.add(answerOption);
                    }

                    // Tạo question
                    if (questionText != null && questionTypeString != null) {
                        QuestionType questionType = setTypeBaseOnStringInput(questionTypeString);

                        Question question = new Question();

                        question.setQuestionText(questionText);
                        question.setQuestionType(questionType);
                        //question.setQuizzes(new HashSet<>());
                        //question.setAnswerOptions(answerOptionListTemp);

                        for(AnswerOption answerOption : answerOptionListTemp){
                            question.addAnswerOption(answerOption);
                            //answerOption.setQuestion(question);
                            //answerOptionList.add(answerOption);
                            //answerOptionRepository.save(answerOption);
                        }

                        questions.add(question);
                    }
                }
                //----------------------------------------------------------
            }
            // Tạo quiz

            Quiz quiz = new Quiz();

            try{
                Set<Quiz> quizzes = showAllQuizzesWithQuestions(courseName);
                quiz.setName(generateRandomService.generateRandomName(quizzes));
            } catch (Exception e) {
                quiz.setName(generateRandomService.generateRandomName(null));
            }

            quiz.setDescription("This is an auto-created quiz!");
            quiz.setUpdatedAt(LocalDateTime.now());
            quiz.setStartTime(LocalDateTime.now().plusSeconds(1));
            quiz.setEndTime(LocalDateTime.now().plusSeconds(2));
            User user = userService.getCurrentUser();
            quiz.setCreatedAt(LocalDateTime.now());
            quiz.setCreatedBy(user);
            quiz.setQuizType(Quiz.QuizType.CLOSE);

            /*Quiz savedQuiz = quizRepository.save(quiz);

            try {
                SchedulerUtil.startScheduler();

                Scheduler scheduler = SchedulerUtil.getScheduler();
                scheduleJob.scheduleQuizOpenJob(savedQuiz.getId(), savedQuiz.getStartTime());
                scheduleJob.scheduleQuizCloseJob(savedQuiz.getId(), savedQuiz.getEndTime());
            } catch (SchedulerException e) {
                e.printStackTrace();
            }*/

            for(Question question : questions){
                quiz.addQuestion(question);
            }

            //quizService.createQuiz(quiz);

            /*for(AnswerOption answerOption : answerOptionList){
                answerOptionRepository.save(answerOption);
            }

            quizService.save(quiz);
            courseService.save(course);*/

            Course course = courseService.findByName(courseName);
            course.addQuiz(quiz);

            Course savedCourse = courseService.save(course);
            List<Quiz> newestQuizzes = savedCourse.getQuizzes();
            Quiz savedQuiz = newestQuizzes.get(newestQuizzes.size() - 1);

            try {
                SchedulerUtil.startScheduler();

                Scheduler scheduler = SchedulerUtil.getScheduler();
                scheduleJob.scheduleQuizOpenJob(savedQuiz.getId(), savedQuiz.getStartTime());
                scheduleJob.scheduleQuizCloseJob(savedQuiz.getId(), savedQuiz.getEndTime());
            } catch (SchedulerException e) {
                e.printStackTrace();
            }

        } catch (IOException _e_) {
            throw new RuntimeException("Error importing questions from Excel", _e_);
        }
    }

    public QuestionType setTypeBaseOnStringInput(String questionTypeString) {
        if (questionTypeString.equalsIgnoreCase("Multiple Choice")) {
            return QuestionType.MCQ;
        } else if (questionTypeString.equalsIgnoreCase("Single Choice")) { // Tùy vào type trong excel
            return QuestionType.SCQ;
        } else {
            return QuestionType.TEXT;
        }
    }


    // GENERATE EXAM
    @Transactional
    public List<Quiz> generateQuizzes(String courseName, int numOfQuizzes, int questionsEachQuiz) {
        if (numOfQuizzes <= 0) {
            throw new InputException("Number of quizs must not be <= 0");
        }

        if (questionsEachQuiz <= 0) {
            throw new InputException("Number of questions must not be <= 0");
        }

        List<Quiz> createdQuizzes = new ArrayList<>();
        Set<Quiz> quizzes = showAllQuizzesWithQuestions(courseName);

        List<Question> questions = new ArrayList<>();
        for (Quiz quiz : quizzes) {
            questions.addAll(quiz.getQuestions()); // Thêm tất cả câu hỏi từ quiz vào danh sách
        }

        if(questions.isEmpty()){
            throw new NotFoundException("Course has no question!");
        }

        // Tạo quiz
        for (int i = 0; i < numOfQuizzes; i++) {
            List<Question> shuffledQuestions = generateRandomService.collectRandomQuestions(questions, questionsEachQuiz);
            Quiz quiz = new Quiz();
            quiz.setName(generateRandomService.generateRandomName(quizzes));
            //quiz.setCourse(courseService.findByName(courseName));
            quiz.setDescription("This is an auto-created quiz!");
            quiz.setCreatedBy(userService.getCurrentUser());
            //quiz.setQuestions(new HashSet<>(shuffledQuestions));
            quiz.setCreatedAt(LocalDateTime.now());
            quiz.setUpdatedAt(LocalDateTime.now());
            quiz.setStartTime(LocalDateTime.now().plusSeconds(1));
            quiz.setEndTime(LocalDateTime.now().plusSeconds(2));
            quiz.setQuizType(Quiz.QuizType.CLOSE);

            for(Question question : shuffledQuestions){
                quiz.addQuestion(question);
            }

            Course course = courseService.findByName(courseName);
            course.addQuiz(quiz);

            Course savedCourse = courseService.save(course);
            List<Quiz> newestQuizzes = savedCourse.getQuizzes();
            Quiz savedQuiz = newestQuizzes.get(newestQuizzes.size() - 1);

            try {
                SchedulerUtil.startScheduler();

                Scheduler scheduler = SchedulerUtil.getScheduler();
                scheduleJob.scheduleQuizOpenJob(savedQuiz.getId(), savedQuiz.getStartTime());
                scheduleJob.scheduleQuizCloseJob(savedQuiz.getId(), savedQuiz.getEndTime());
            } catch (SchedulerException e) {
                e.printStackTrace();
            }

            createdQuizzes.add(savedQuiz);
        }

        return createdQuizzes;
    }

    public Question cloneQuestion(Long questionId, Quiz targetQuiz) {
        Question originalQuestion = questionRepository.findById(questionId)
                .orElseThrow(() -> new NotFoundException("Question not found"));

        // Tạo bản sao của câu hỏi
        Question clonedQuestion = new Question();
        clonedQuestion.setQuestionNo(originalQuestion.getQuestionNo());
        clonedQuestion.setQuestionText(originalQuestion.getQuestionText());
        clonedQuestion.setQuestionType(originalQuestion.getQuestionType());
        clonedQuestion.setPoints(originalQuestion.getPoints());
        clonedQuestion.setQuizzes(targetQuiz); // Gán vào quiz mới

        // Tạo bản sao của AnswerOption (nếu có)
        List<AnswerOption> clonedOptions = new ArrayList<>();
        for (AnswerOption option : originalQuestion.getAnswerOptions()) {
            AnswerOption clonedOption = new AnswerOption();
            clonedOption.setOptionText(option.getOptionText()); // Đổi từ setText() -> setOptionText()
            clonedOption.setIsCorrect(option.getIsCorrect());
            clonedOption.setQuestion(clonedQuestion);
            clonedOption.setOptionLabel(option.getOptionLabel());

            clonedOptions.add(clonedOption);
        }
        clonedQuestion.setAnswerOptions(clonedOptions);

        return questionRepository.save(clonedQuestion);
    }
    public List<Question> getQuestionsByAssessmentId(Long assessmentId) {
        return questionRepository.findQuestionsWithAnswersByAssessmentId(assessmentId);
    }
    public List<Question> findQuestionsByAssessmentId(Long assessmentId) {
        return questionRepository.findQuestionsWithAnswersByAssessmentId(assessmentId);
    }
}

