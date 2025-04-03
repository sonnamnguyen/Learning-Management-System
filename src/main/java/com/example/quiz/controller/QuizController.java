package com.example.quiz.controller;

import com.example.assessment.model.StudentAssessmentAttempt;
import com.example.course.Course;
import com.example.course.CourseService;
import com.example.exception.NotFoundException;
import com.example.quiz.AI.AIRequestBody;
import com.example.quiz.AI.AIService;
import com.example.quiz.AI.response.AIResponse;
import com.example.quiz.Request.QuestionRequestDTO;
import com.example.quiz.model.*;
import com.example.quiz.repository.*;
import com.example.quiz.service.QuestionService;
import com.example.quiz.service.QuizParticipantService;
import com.example.quiz.service.QuizService;
import com.example.quiz.service.QuizTagService;
import com.example.user.User;
import com.example.user.UserRepository;
import com.example.user.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.quartz.ObjectAlreadyExistsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.Principal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Controller
@RequestMapping("/quizes")
public class QuizController {

    @Autowired
    private AIService aiService;
    @Autowired
    private AnswerRepository answerRepository;
    @Autowired
    private AnswerOptionRepository answerOptionRepository;
    @Autowired
    private QuizParticipantRepository quizParticipantRepository;
    @Autowired
    private QuizParticipantService quizParticipantService;
    @Autowired
    private QuizService quizService;
    @Autowired
    private QuizTagService quizTagService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TestSessionRepository testSessionRepository;
    @Autowired
    private QuestionRepository questionRepository;
    @Autowired
    private ResultRepository resultRepository;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private UserService userService;

    // Thêm model attribute chung cho tất cả các phương thức
    @ModelAttribute
    public void addCommonAttributes(Model model) {
        model.addAttribute("title", "Quizes");
        model.addAttribute("links", "/style.css");
        model.addAttribute("fileTypes", List.of("Word", "Excel", "Json"));
    }

//    @GetMapping
//    public String dashboard(Model model,Long studentId){
//
//        return "layout";
//    }



    @GetMapping
    public String list(Model model,
                       @RequestParam(value = "searchQuery", required = false) String searchQuery,
                       @RequestParam(value = "page", defaultValue = "0") int page,
                       @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
                       @RequestParam(value = "course", required = false) Long courseId,
                       @RequestParam(value = "tags", required = false) List<Long> tagIds,
                       Principal principal) {

        // Get current user and their roles
        User currentUser = userService.findByUsername(principal.getName());
        boolean isStudent = currentUser.getRoles().stream()
                .anyMatch(role -> "STUDENT".equals(role.getName()));

        model.addAttribute("currentUserId", currentUser.getId());
        Page<Quiz> quizes;
        Pageable pageable = PageRequest.of(page, pageSize);

        // Sử dụng service method mới để kết hợp tất cả điều kiện filter
        quizes = quizService.findQuizesWithFilters(courseId, searchQuery, tagIds, pageable);

        // Add all the necessary attributes to the model
        model.addAttribute("quizes", quizes.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", quizes.getTotalPages());
        model.addAttribute("totalItems", quizes.getTotalElements());
        model.addAttribute("searchQuery", searchQuery);
        model.addAttribute("courses", courseService.getAllCourses());
        model.addAttribute("tags", quizTagService.getAllQuizTag());
        model.addAttribute("isStudent", isStudent); // Add this for the view to use
        model.addAttribute("content", "quizes/list");

        return "layout";
    }




    @GetMapping("/create")
    public String showCreateForm(Model model) {
        // Get the authenticated user's details
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName(); // Get the username of the logged-in user
        User currentUser = userService.findByUsername(username);
        // Add a new Quiz object to the model
        Quiz quiz = new Quiz();
        quiz.setCreatedBy(userService.findByUsername(username)); // Assume you have a method to get the User object

//        model.addAttribute("models", aiService.getModelNames());
//        model.addAttribute("numOfCorrectAnswers", aiService.getNumOfCorrectAnswer());
        model.addAttribute("numOfAnswerOptions", aiService.getNumOfAnswerOptions());
        model.addAttribute("types", aiService.getTypes());
        model.addAttribute("AIRequestBody", new AIRequestBody());
        if (model.containsAttribute("response")) {
            model.addAttribute("response", model.getAttribute("response"));
        }

        model.addAttribute("warning", aiService.AIWarning());
        model.addAttribute("quiz", quiz);
        model.addAttribute("courses", courseService.getAllCourses());
        model.addAttribute("user", currentUser);
        model.addAttribute("users", userService.getAllUsers()); // Fetch all users for the dropdown
        model.addAttribute("tags", quizTagService.getAllQuizTag());

        model.addAttribute("content", "quizes/create");
        return "layout";
    }


    @PostMapping("/create")
    @Transactional
    public String createQuiz(@Valid @RequestBody String quiz,
                             @RequestParam(value = "tagIds", required = false) List<Long> tagIds,
                             @RequestParam(value = "newTagNames", required = false) List<String> newTagNames,
                             BindingResult result, Model model) {
        Quiz quizz = quizService.jsonToQuiz(quiz);

        quizz.validateTime(result);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User currentUser = userService.findByUsername(username);

        Duration duration = null;
        if (quizz.getQuizCategory() == Quiz.QuizCategory.PRACTICE) {
            duration = quizService.calculateQuizDuration(quizz.getNumberOfQuestions());
        }

        if (result.hasErrors()) {
            model.addAttribute("error", "Please fill the form.");
            model.addAttribute("courses", courseService.getAllCourses());
            model.addAttribute("users", userService.getAllUsers());
            model.addAttribute("user", currentUser);
            model.addAttribute("content", "quizes/create");
            return "layout";
        }

        // Pass tagIds and newTagNames to the service for association
        quizService.createQuiz(quizz, tagIds, newTagNames);

        if (quizz.getQuizCategory() == Quiz.QuizCategory.PRACTICE) {
            quizService.scheduleClearCacheJob(quizz.getId());
            System.out.println("Duration for practice quiz: " + duration.toMinutes() + " minutes");
        }

        model.addAttribute("success", "Quiz created successfully!");
        return "redirect:/quizes";
    }






    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model, Principal principal) {
        Quiz quiz = quizService.getQuizWithTags(id);
        List<Course> courses = courseService.getAllCourses();
        User user = userService.findByUsername(principal.getName());

        model.addAttribute("quiz", quiz);
        model.addAttribute("courses", courses);
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("user", user);
        model.addAttribute("content", "quizes/edit");
        return "layout";
    }

    @PostMapping("/edit/{id}")
    public String update(@PathVariable("id") Long id, @ModelAttribute Quiz quiz) {
        quizService.update(id,quiz);
        return "redirect:/quizes";
    }

    @Transactional
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable("id") Long id) {
        quizService.deleteById(id);
        return "redirect:/quizes";
    }


    @PostMapping("/attempt/{quizId}")
    public String attemptQuiz(@PathVariable("quizId") Long quizId, Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        String resultMessage = quizService.attemptQuiz(quizId, username);

        if (resultMessage != null) {
            model.addAttribute("message", resultMessage);
            return "redirect:/quizes";
        }

        Quiz quiz = quizService.findById(quizId).orElse(null);
        model.addAttribute("quiz", quiz);
        model.addAttribute("content", "quizes/start");
        return "layout";
    }


    // Print roles page
    @GetMapping("/print")
    public String print(Model model) {
        List<Quiz> quizes = quizService.findAll();
        model.addAttribute("quizes", quizes);
        return "quizes/print";
    }

    @GetMapping("/export")
    public ResponseEntity<InputStreamResource> export() {
        // Fetch all roles (page size set to max to get all records)
        List<Quiz> quizes = quizService.findAll();

        // Convert roles to Excel (assumes you have a service for that)
        ByteArrayInputStream excelFile = quizService.exportToExcel(quizes);

        // Create headers for the response (Content-Disposition to trigger file download)
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=quizes.xlsx");

        // Return the file in the response
        return ResponseEntity.ok()
                .headers(headers)
                .body(new InputStreamResource(excelFile));
    }

    @GetMapping("/detail/{id}")
    public String showDetailForm(@PathVariable("id") Long id, Model model,
                                 @RequestParam(name = "course", required = false) Long courseId) {
        Quiz quiz = quizService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));

        List<Quiz> quizes = quizService.findQuizzesIgnoreId(courseId, quiz.getId());
        List<Course> courses = courseService.getAllCourses();

        List<Question> sortedQuestions = new ArrayList<>(quiz.getQuestions());
        sortedQuestions.sort(Comparator.comparingInt(Question::getQuestionNo));
        List<Question> questions = questionRepository.findByQuizzesOrderByQuestionNo(quiz);

        for (Question question : sortedQuestions) {
            List<AnswerOption> sortedAnswerOptions = new ArrayList<>(question.getAnswerOptions());
            sortedAnswerOptions.sort(Comparator.comparing(AnswerOption::getOptionLabel));
            question.setAnswerOptions(sortedAnswerOptions);
        }

        model.addAttribute("questions", sortedQuestions);
        model.addAttribute("totalQuestions", sortedQuestions.size());
        model.addAttribute("types", Question.QuestionType.values());
        model.addAttribute("question", new Question());
        model.addAttribute("quiz", quiz);
        model.addAttribute("quizes", quizes);
        model.addAttribute("courses", courses);
        model.addAttribute("content", "quizes/detail");
        model.addAttribute("currentQuiz",quiz);

        return "layout";
    }

    // add question
    @PostMapping("/detail/{quizId}/questions/create")
    public String addQuestion(@PathVariable Long quizId, @ModelAttribute QuestionRequestDTO question) {
        quizService.createQuestion(quizId, question);
        return "redirect:/quizes/detail/" + quizId;
    }

    @GetMapping("/participants/{quizId}/result/{testSessionId}")
    public String getResultByTestSession(
            @PathVariable Long quizId,
            @PathVariable Long testSessionId,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            Quiz quiz = quizService.findById(quizId)
                    .orElseThrow(() -> new NotFoundException("Quiz not found"));

            QuizParticipant participant = quizParticipantRepository.findByQuizIdAndTestSessionId(quizId, testSessionId)
                    .orElseThrow(() -> new NotFoundException("Participant not found for this quiz and test session"));

            TestSession testSession = testSessionRepository.findByIdWithAnswers(testSessionId)
                    .orElseThrow(() -> new NotFoundException("Test session not found"));

            if (testSession.getEndTime() == null) {
                throw new IllegalStateException("Test session has not been completed");
            }

            List<PracticeResult> practiceResults = testSession.getPracticeResults();
            if (practiceResults == null || practiceResults.isEmpty()) {
                throw new IllegalStateException("No result found for this test session");
            }

            List<Question> sortedQuestions = new ArrayList<>(quiz.getQuestions());
            sortedQuestions.sort(Comparator.comparingInt(Question::getQuestionNo));
            quiz.setQuestions(new HashSet<>(sortedQuestions));

            long durationInMinutes = Duration.between(testSession.getStartTime(), testSession.getEndTime()).toMinutes();

            model.addAttribute("quiz", quiz);
            model.addAttribute("testSession", testSession);
            model.addAttribute("participant", participant);
            model.addAttribute("practiceResults", practiceResults);
            model.addAttribute("durationInMinutes", durationInMinutes);
            model.addAttribute("content", "quizes/session-result");

            return "layout";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/quizes/participants/" + quizId;
        }
    }


    @GetMapping("/participants/{quizId}")
    public String getQuizParticipants(@PathVariable Long quizId, Model model, Principal principal) {
        try {
            // Lấy thông tin người dùng hiện tại và kiểm tra role
            User currentUser = userService.findByUsername(principal.getName());
            boolean isStudent = currentUser.getRoles().stream()
                    .anyMatch(role -> "STUDENT".equals(role.getName()));

            // Lấy thông tin quiz
            Quiz quiz = quizService.getQuizById(quizId);
            if (quiz == null) {
                throw new NotFoundException("Quiz not found");
            }

            List<QuizParticipant> participants;

            if (isStudent) {
                // Nếu là student, chỉ hiển thị thông tin của chính họ
                participants = quizParticipantRepository.findByQuizIdAndUserId(quizId, currentUser.getId())
                        .map(Collections::singletonList)
                        .orElse(Collections.emptyList());
            } else {
                // Nếu không phải student (là admin hoặc giảng viên), hiển thị tất cả
                participants = quizParticipantService.getParticipantsByQuiz(quizId);
            }

            // Tính toán thống kê
            long totalParticipants = participants.size();
            long completedCount = participants.stream()
                    .filter(p -> p.getTestSession() != null && p.getTestSession().getEndTime() != null)
                    .count();
            long inProgressCount = participants.stream()
                    .filter(p -> p.getTestSession() != null && p.getTestSession().getEndTime() == null)
                    .count();
            long notStartedCount = participants.stream()
                    .filter(p -> p.getTestSession() == null)
                    .count();

            // Add attributes to model
            model.addAttribute("participants", participants);
            model.addAttribute("quiz", quiz);
            model.addAttribute("quizId", quizId);
            model.addAttribute("isStudent", isStudent);  // Thêm thuộc tính isStudent vào model

            // Add statistics
            model.addAttribute("totalParticipants", totalParticipants);
            model.addAttribute("completedCount", completedCount);
            model.addAttribute("inProgressCount", inProgressCount);
            model.addAttribute("notStartedCount", notStartedCount);

            model.addAttribute("content", "quizes/participants");
            return "layout";
        } catch (Exception e) {
            model.addAttribute("error", "Could not load participants: " + e.getMessage());
            return "redirect:/quizes";
        }
    }

    @GetMapping("/{quizId}/participants/search")
    public String searchQuizParticipants(
            @PathVariable Long quizId,
            @RequestParam(required = false) String searchTerm,
            Model model) {
        try {
            Quiz quiz = quizService.getQuizById(quizId);
            List<QuizParticipant> participants;

            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                participants = quizParticipantService.searchParticipants(quizId, searchTerm);
            } else {
                participants = quizParticipantService.getParticipantsByQuiz(quizId);
            }

            model.addAttribute("participants", participants);
            model.addAttribute("quiz", quiz);
            model.addAttribute("quizId", quizId);
            model.addAttribute("searchTerm", searchTerm);
            model.addAttribute("content", "quizes/participants");

            return "layout";
        } catch (Exception e) {
            model.addAttribute("error", "Error searching participants: " + e.getMessage());
            return "redirect:/quizes/participants/" + quizId;
        }
    }

    @GetMapping("/{quizId}/participants/export")
    public ResponseEntity<InputStreamResource> exportQuizParticipants(@PathVariable Long quizId) {
        List<User> participants = quizService.getParticipants(quizId);

        ByteArrayInputStream excelFile = quizService.exportParticipantsToExcel(participants);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=participants.xlsx");
        return ResponseEntity.ok()
                .headers(headers)
                .body(new InputStreamResource(excelFile));
    }

    @GetMapping("/{quizId}/participants/print")
    public String printAllParticipants(@PathVariable Long quizId, Model model) {
        List<QuizParticipant> participants = quizParticipantService.getParticipantsByQuiz(quizId);
        model.addAttribute("participants", participants);
        return "quizes/print-participant";
    }

    @PostMapping("/apply/{quizId}")
    public String applyToQuiz(@PathVariable Long quizId, Principal principal, RedirectAttributes redirectAttributes, Model model) {
        Quiz quiz = quizService.getQuizById(quizId);
        User user = userService.findByUsername(principal.getName());

        Optional<QuizParticipant> optionalQuizParticipant = quizParticipantRepository.findByQuizAndUser(quiz, user);
        QuizParticipant quizParticipant;

        if (optionalQuizParticipant.isPresent()) {
            quizParticipant = optionalQuizParticipant.get();

            if (quizParticipant.getAttemptUsed() >= quiz.getAttemptLimit()) {
                redirectAttributes.addFlashAttribute("errorMessage", "You have reached the maximum number of attempts!");
                return "redirect:/quizes";
            }

            quizParticipant.setAttemptUsed(quizParticipant.getAttemptUsed() + 1);
        } else {
            // Nếu chưa có, tạo mới
            quizParticipant = new QuizParticipant();
            quizParticipant.setQuiz(quiz);
            quizParticipant.setUser(user);
            quizParticipant.setAttemptUsed(1);
            quizParticipant.setTimeStart(LocalDateTime.now());
        }

        quizParticipantRepository.saveAndFlush(quizParticipant);

        redirectAttributes.addFlashAttribute("successMessage", "Join Quiz Complete! Attempt: " + quizParticipant.getAttemptUsed());
        redirectAttributes.addFlashAttribute("attemptUsed", quizParticipant.getAttemptUsed());
        redirectAttributes.addFlashAttribute("attemptLimit", quiz.getAttemptLimit());

        Set<Question> questions = quiz.getQuestions();
        model.addAttribute("quiz", quiz);
        model.addAttribute("questions", questions);
        if (Quiz.QuizCategory.PRACTICE==(quiz.getQuizCategory())) {
            Duration calculatedDuration = quizService.calculateQuizDuration(quiz.getQuestions().size());
            model.addAttribute("timeLimit", calculatedDuration.toMinutes());
        } else {
            model.addAttribute("timeLimit", quiz.getDuration());
        }

        model.addAttribute("content", "quizes/do-quiz");
        return "layout";
    }



    @GetMapping("/detail/{quizId}/view-all")
    public String viewQuiz(@PathVariable("quizId") Long quizId, Model model) {
        Quiz quiz = quizService.getQuizById(quizId);
        if (quiz == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found");
        }

        List<Question> questions = new ArrayList<>(quiz.getQuestions());
        questions.sort(Comparator.comparingInt(Question::getQuestionNo));

        model.addAttribute("quiz", quiz);
        model.addAttribute("questions", questions);
        model.addAttribute("quizId", quizId);
        model.addAttribute("content", "quizes/view-quiz");
        return "layout";
    }

    @PostMapping("/apply/{quizId}/submit")
    public String submitQuiz(
            @PathVariable("quizId") Long quizId,
            @RequestParam(required = false) Long assessmentId,
            @RequestParam MultiValueMap<String, String> responses,
            @RequestParam("elapsedTime") int elapsedTime,
            @RequestParam("questionIds") List<String> questionIds,
            @RequestParam(required = false) StudentAssessmentAttempt studentAssessmentAttemptId,
            Model model) {

        System.out.println("Dữ liệu nhận được từ form: " + questionIds);

        responses.forEach((key, values) -> {
            System.out.println("Key: " + key + " | Values: " + values);
        });

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userService.findByUsername(username);

        Quiz quiz = quizService.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found!"));

        if (quiz.getQuizCategory() != Quiz.QuizCategory.PRACTICE && assessmentId == null) {
            throw new IllegalArgumentException("Assessment ID is required for EXAM quiz type");
        }


        // ✅ Lấy danh sách câu hỏi trước khi dùng
        List<Question> questions1 = questionRepository.findAllById(
                questionIds.stream().map(Long::parseLong).collect(Collectors.toList())
        );

// ✅ Lọc số câu hỏi không phải dạng TEXT
        long nonTextQuestionCount = questions1.stream()
                .filter(q -> q.getQuestionType() != Question.QuestionType.TEXT)
                .count();

// ✅ Chia đều 100 điểm cho các câu hỏi không phải TEXT
        double scorePerQuestion = nonTextQuestionCount > 0 ? 100.0 / nonTextQuestionCount : 0.0;



        int totalTime = quiz.getDuration();
        int remainingTime = (totalTime * 60) - elapsedTime;

        // ✅ Tính điểm sử dụng MultiValueMap
        double score = quizService.calculateScore(questionIds, assessmentId, responses, user,studentAssessmentAttemptId);

        List<Question> questions = questionRepository.findAllById(
                questionIds.stream().map(Long::parseLong).collect(Collectors.toList())
        );

        int correctCount = 0;
        Map<Long, List<Long>> selectedAnswers = new HashMap<>();
        Map<Long, String> textAnswers = new HashMap<>();
        Map<Long, Map<Long, Double>> selectedAnswerScores = new HashMap<>(); // ✅ QuizID -> (OptionID -> Score)
        Map<Long, Double> questionScores = new HashMap<>(); // ✅ Lưu điểm tổng của từng câu hỏi

        int totalCorrectAnswers = 0;
        int userCorrectAnswers = 0;



        System.out.println("📌 Dữ liệu nhận được từ form: " + responses);

        for (Question question : questions) {

            double questionScore = 0.0; // ✅ Điểm của câu hỏi hiện tại
            List<String> selectedOptionIds = responses.get("answers[" + question.getId() + "]");

            if (selectedOptionIds != null && !selectedOptionIds.isEmpty()) {
                if (question.getQuestionType().toString().equals("TEXT")) {
                    // ✅ Lưu câu trả lời dạng TEXT
                    String textAnswer = selectedOptionIds.get(0);
                    textAnswers.put(question.getId(), textAnswer);
                } else {
                    // ✅ Xử lý câu hỏi trắc nghiệm (MCQ & SCQ)
                    List<Long> selectedOptionLongs = selectedOptionIds.stream()
                            .filter(id -> id.matches("\\d+")) // Chỉ giữ lại số hợp lệ
                            .map(Long::parseLong)
                            .collect(Collectors.toList());

                    selectedAnswers.put(question.getId(), selectedOptionLongs);

                    // ✅ Lấy danh sách đáp án đúng
                    List<AnswerOption> correctOptions = answerOptionRepository.findCorrectAnswersByQuestionId(question.getId());
                    List<Long> correctOptionIds = correctOptions.stream().map(AnswerOption::getId).toList();

                    // ✅ Đếm tổng số đáp án đúng trong bài quiz
                    totalCorrectAnswers += correctOptionIds.size();

                    // ✅ Kiểm tra số đáp án đúng mà user đã chọn
                    for (Long selectedOptionId : selectedOptionLongs) {
                        if (correctOptionIds.contains(selectedOptionId)) {
                            userCorrectAnswers++; // ✅ Nếu user chọn đáp án đúng, tăng biến đếm
                        }
                    }

                    // ✅ Lưu điểm từng đáp án
                    for (Long selectedOptionId : selectedOptionLongs) {
                        AnswerOption selectedOption = answerOptionRepository.findById(selectedOptionId).orElse(null);
                        if (selectedOption != null) {
                            List<Answer> answers = answerRepository.findAllByQuestionIdAndSelectedOptionId(question.getId(), selectedOptionId);

                            if (question.getQuestionType() == Question.QuestionType.TEXT) {
                                continue;
                            }

                            // ✅ Map lưu điểm của tất cả đáp án đúng trong câu hỏi này
                            Map<Long, Double> optionScores = selectedAnswerScores.getOrDefault(question.getId(), new HashMap<>());

                            for (Answer answer : answers) {
                                if (answer.getIsCorrect() != null && answer.getIsCorrect()) {
                                    optionScores.put(answer.getSelectedOption().getId(), answer.getScore());

                                }
                            }

                            selectedAnswerScores.put(question.getId(), optionScores);
                        }
                    }
                }
            }
        }
        TestSession testSession = testSessionRepository.findTopByUserOrderByStartTimeDesc(user);
        if (testSession == null) {
            throw new NotFoundException("TestSession not found");
        }

        System.out.println("📌 selectedAnswers lưu lại:");
        selectedAnswers.forEach((key, value) -> System.out.println("Câu " + key + ": " + value));

        testSession.setEndTime(LocalDateTime.now());
        testSessionRepository.save(testSession);

        QuizParticipant participant = quizParticipantRepository.findByQuizIdAndUser_Id(quizId, user.getId());
        if (participant == null) {
            participant = new QuizParticipant();
            participant.setQuiz(quiz);
            participant.setUser(user);
        }
        participant.setTestSession(testSession);
        participant.setTimeStart(testSession.getStartTime());
        participant.setTimeEnd(testSession.getEndTime());
        quizParticipantRepository.save(participant);

        System.out.println("📌 Selected Answers: " + selectedAnswers);

        model.addAttribute("correctAnswers", correctCount);
        model.addAttribute("selectedAnswers", selectedAnswers);
        model.addAttribute("textAnswers", textAnswers);
        model.addAttribute("selectedAnswerScores", selectedAnswerScores); // ✅ Thêm dữ liệu điểm số
        model.addAttribute("correctAnswers", userCorrectAnswers); // ✅ Số đáp án đúng của user
        model.addAttribute("totalCorrectAnswers", totalCorrectAnswers); // ✅ Tổng số đáp án đúng của quiz
        model.addAttribute("scorePerQuestion", scorePerQuestion); // ✅ Truyền điểm mỗi câu hỏi vào model

        model.addAttribute("questions", questions);
        model.addAttribute("quizId", quizId);
        model.addAttribute("score", score);
        model.addAttribute("user", user);
        model.addAttribute("totalQuestions", questions.size());
        model.addAttribute("remainingTime", remainingTime);
        model.addAttribute("content", "quizes/result");

        return "layout";
    }



    @GetMapping("/do-quiz/{quizId}")
    public String startQuiz(@PathVariable Long quizId, Model model, Principal principal) {
        Quiz quiz = quizService.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz "));

        Set<Question> questions = quiz.getQuestions();

        User user = userService.findByUsername(principal.getName());

        model.addAttribute("quiz", quiz);
        model.addAttribute("questions", questions);
        model.addAttribute("timeLimit",quiz.getDuration());
        model.addAttribute("user", user);
        model.addAttribute("content", "quizes/do-quiz");

        return "layout";
    }

    @GetMapping("/detail/review/{name}")
    public String viewDetailQuiz(@PathVariable String name, Model model){
        try{
            List<Question> questionList = new ArrayList<>(quizService.getQuestionsOfQuiz(name));
            questionList.sort(Comparator.comparingInt(Question::getQuestionNo));
            model.addAttribute("Questions", questionList);
            model.addAttribute("content", "quizes/review-no-import");
            return "layout";
        } catch (Exception e){
            model.addAttribute("Errors", e.getMessage());
            return "tools/generate_exams";
        }
    }

    @GetMapping("/detail/questions/{questionId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getQuestion(@PathVariable Long questionId) {
        try {
            Question question = questionService.findById(questionId)
                    .orElseThrow(() -> new NotFoundException("Question not found"));

            Map<String, Object> response = new HashMap<>();
            response.put("id", question.getId());
            response.put("questionText", question.getQuestionText());
            response.put("questionType", question.getQuestionType());

            List<Map<String, Object>> answerOptions = question.getAnswerOptions().stream()
                    .map(option -> {
                        Map<String, Object> optionMap = new HashMap<>();
                        optionMap.put("id", option.getId());
                        optionMap.put("optionLabel", option.getOptionLabel());
                        optionMap.put("optionText", option.getOptionText());
                        optionMap.put("isCorrect", option.getIsCorrect());
                        return optionMap;
                    })
                    .collect(Collectors.toList());

            response.put("answerOptions", answerOptions);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    @PutMapping("/detail/{quizId}/questions/{questionId}/update")
    @ResponseBody
    public ResponseEntity<?> updateQuestion(
            @PathVariable Long quizId,
            @PathVariable Long questionId,
            @RequestBody QuestionRequestDTO request) {
        try {
            Question updatedQuestion = quizService.updateQuestion(questionId, request);
            return ResponseEntity.ok(updatedQuestion);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating question: " + e.getMessage());
        }
    }

    @PutMapping("/detail/{quizId}/questions/{questionId}/move")
    public ResponseEntity<String> moveQuestion(
            @PathVariable Long quizId,
            @PathVariable Long questionId,
            @RequestParam int newPosition) {

        try {
            quizService.moveQuestion(quizId, questionId, newPosition);
            return ResponseEntity.ok("Question position updated successfully");
        } catch (IllegalArgumentException | EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while updating question position.");
        }

    }
    @GetMapping("/dashboard/{studentId}")
    public String getStudentDashboard(@PathVariable Long studentId, Model model) {
        try {
            User student = userRepository.findById(studentId)
                    .orElseThrow(() -> new NotFoundException("User not found"));
            model.addAttribute("student", student);

            Map<Long, String> quizCourses = quizService.getQuizCourses(studentId);
            Map<String, Object> scoresData = quizService.getScoreByQuiz(studentId);
            model.addAttribute("quizScores", scoresData.get("quizScores"));
            model.addAttribute("scoreDifferences", scoresData.get("scoreDifferences"));
            model.addAttribute("quizDurations", scoresData.get("quizDurations"));
            model.addAttribute("quizCourses", quizCourses);



            Map<String, Integer> courseQuizData = quizService.getQuizFromCourse(studentId);
            model.addAttribute("courseQuizCount", courseQuizData);

            model.addAttribute("content", "quizes/dashboard");
            System.out.println("Quiz Scores: " + scoresData.get("quizScores"));
            System.out.println("Quiz Courses: " + quizCourses);
            System.out.println("Quiz Durations: " + scoresData.get("quizDurations"));

            return "layout";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    @DeleteMapping("/detail/{quizId}/questions")
    public ResponseEntity<String> deleteAllQuestions(@PathVariable Long quizId) {
        try {
            questionService.deleteAllQuestions(quizId);
            return ResponseEntity.ok("All questions deleted successfully.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to delete all questions: " + e.getMessage());
        }
    }
    @DeleteMapping("/detail/{quizId}/questions/{questionId}")
    public ResponseEntity<String> deleteQuestion(@PathVariable Long quizId, @PathVariable Long questionId) {
        try {
            questionService.deleteQuestion(questionId);
            return ResponseEntity.ok("Question deleted successfully.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to delete question: " + e.getMessage());
        }
    }
    @PostMapping("/modal")
    public String processModal(@RequestParam("file") MultipartFile file,
                               @RequestParam("course") String courseName,
                               @RequestParam("fileType") String fileType,
                               @RequestParam String action,
                               RedirectAttributes redirectAttributes,
                               HttpSession session,
                               Model model){
        if(action.equals("import")){
            try{
                if(fileType.equals("Excel")){
                    questionService.importExcelTEST(file, courseName);
                    redirectAttributes.addFlashAttribute("successMessage", "Quizzes imported successfully from Excel");
                } else if (fileType.equals("Json")) {
                    questionService.importJson(file, courseName);
                    redirectAttributes.addFlashAttribute("successMessage", "Quizzes imported successfully from Json");
                } else {
                    questionService.importWord(file, courseName);
                    redirectAttributes.addFlashAttribute("successMessage", "Quizzes imported successfully from Word");
                }

                return "redirect:/quizes";
            }catch (Exception e){
                redirectAttributes.addFlashAttribute("error", e.getMessage());
                redirectAttributes.addFlashAttribute("chosenCourse", courseName);
                redirectAttributes.addFlashAttribute("chosenType", fileType);
                redirectAttributes.addFlashAttribute("chosenFile", file.getOriginalFilename());
                //redirectAttributes.addFlashAttribute("modalOpen", true);
                return "redirect:/quizes?openModal=importModal";
            }
        } else {
            try{
                Map<String, Object> reviewData = new HashMap<>();
                if(fileType.equals("Excel")){
                    reviewData = questionService.reviewQuizTEST(file, courseName);
                } else if (fileType.equals("Json")) {
                    reviewData = questionService.reviewFileJson(file, courseName);
                } else {
                    reviewData = questionService.reviewImportWord(file, courseName);
                }


                for (Map.Entry<String, Object> entry : reviewData.entrySet()) {
                    //model.addAttribute(entry.getKey(), entry.getValue());
                    session.setAttribute(entry.getKey(), entry.getValue());
                }
                session.setAttribute("fileType", fileType);
                model.addAttribute("content", "quizes/review");
                return "layout";
                //return "quizes/review";
            }catch (Exception e){
                redirectAttributes.addFlashAttribute("error", e.getMessage());
                redirectAttributes.addFlashAttribute("chosenCourse", courseName);
                redirectAttributes.addFlashAttribute("chosenType", fileType);
                redirectAttributes.addFlashAttribute("chosenFile", file.getOriginalFilename());
                return "redirect:/quizes?openModal=importModal";
            }
        }
    }

    @GetMapping("/tags")
    @ResponseBody
    public ResponseEntity<List<QuizTag>> getAllTags() {
        try {
            List<QuizTag> tags = quizTagService.getAllQuizTag();
            return ResponseEntity.ok(tags);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("review/delete/{questionNo}")
    public String deleteQuestionInReview(@PathVariable int questionNo, HttpSession session, Model model){
        List<Question> questionList = (List<Question>) session.getAttribute("Questions");
        if (questionList != null){
            for (Question question : questionList){
                if (question.getQuestionNo() == questionNo){
                    questionList.remove(question);
                    break;
                }
            }
            for (int i = 0; i < questionList.size(); i++){
                questionList.get(i).setQuestionNo(i + 1);
            }
            session.setAttribute("Questions", questionList);
        }
        model.addAttribute("content", "quizes/review");
        return "layout";
    }

    @PostMapping("/importFromReview")
    public String importFromReview(@RequestParam("questionsJson") String questionsJson,
                                   @RequestParam("fileName") String fileName,
                                   @RequestParam("courseName") String courseName,
                                   RedirectAttributes redirectAttributes) throws JsonProcessingException {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            List<Question> questions = objectMapper.readValue(questionsJson, new TypeReference<List<Question>>() {
            });
            questionService.importFileFromReview(questions, fileName, courseName);
            return "redirect:/quizes";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/quizes?openModal=importModal";
        }
    }
    @PostMapping("/create/AI")
    @ResponseBody
    public Set<Question> createQuizByAI(@ModelAttribute AIRequestBody aiRequest) {

        AIResponse response = aiService.getResponseAIGenerate(aiRequest.getType(), aiRequest.getNumOfQuestions(),
                aiRequest.getNumOfAnswerOptions(),
                aiRequest.getQuestionDescription());
        try {
            String json = response.getChoices().getLast().getMessage().getContent();
            System.out.println(json);
            return questionService.jsonToQuestionSet(json);
        } catch (Exception e) {
            return null;
        }
    }






    @PostMapping("/import")
    public String importFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("course") String courseName,
            @RequestParam(value = "importType", defaultValue = "excel") String importType,
            RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a file to upload");
            redirectAttributes.addFlashAttribute("importType", importType); // Giữ giá trị importType
            return "redirect:/quizes";
        }

        try {
            if ("word".equals(importType)) {
                questionService.importWord(file, courseName);
                redirectAttributes.addFlashAttribute("successMessage", "Quizzes imported successfully from Word");
            } else if ("excel".equals(importType)) {
                questionService.importExcel(file, courseName);
                redirectAttributes.addFlashAttribute("successMessage", "Quizzes imported successfully from Excel");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Invalid import type selected");
            }
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to import quizzes: " + e.getMessage());
            redirectAttributes.addFlashAttribute("importType", importType); // Giữ giá trị importType nếu lỗi
        } catch (ObjectAlreadyExistsException e) {
            throw new RuntimeException(e);
        }

        return "redirect:/quizes";

    }

    @GetMapping("/tags/check/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkTag(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            QuizTag tag = quizTagService.getQuizTagById(id);
            if (tag == null) {
                response.put("success", false);
                response.put("message", "Tag not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            boolean isUsed = quizTagService.isTagUsedInQuiz(id);
            response.put("success", true);
            response.put("isUsed", isUsed);
            if (isUsed) {
                response.put("message", "Tag '" + tag.getName() + "' is being used in quizzes");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error checking tag: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/{quizId}/tags")
    @ResponseBody
    public ResponseEntity<List<QuizTag>> getQuizTags(@PathVariable Long quizId) {
        try {
            List<QuizTag> tags = quizService.getQuizTags(quizId);
            return ResponseEntity.ok(tags);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{quizId}/tags/update")
    @ResponseBody
    public ResponseEntity<String> updateQuizTags(
            @PathVariable Long quizId,
            @RequestBody Map<String, List<Long>> request) {
        try {
            List<Long> tagIds = request.get("tagIds");
            quizService.updateQuizTags(quizId, tagIds);
            return ResponseEntity.ok("Tags updated successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{quizId}/tags/{tagId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> removeTagFromQuiz(
            @PathVariable Long quizId,
            @PathVariable Long tagId) {
        Map<String, Object> response = new HashMap<>();
        try {
            quizService.removeTagFromQuiz(quizId, tagId);
            response.put("success", true);
            response.put("message", "Tag removed successfully from quiz");
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to remove tag: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/tags/create")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createTag(@RequestParam String name) {
        try {
            QuizTag tag = quizTagService.createTag(name);
            Map<String, Object> response = new HashMap<>();
            response.put("id", tag.getId());
            response.put("name", tag.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


    @PostMapping("/{quizId}/tags")
    @ResponseBody
    public ResponseEntity<String> addTagsToQuiz(
            @PathVariable Long quizId,
            @RequestParam(required = false) List<Long> tagIds,
            @RequestParam(required = false) List<String> newTagNames) {
        try {
            quizService.addTagsToQuiz(quizId, tagIds, newTagNames);
            return ResponseEntity.ok("Tags added successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to add tags: " + e.getMessage());
        }
    }

    @DeleteMapping("/tags/delete/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteTag(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Kiểm tra xem tag có được sử dụng không
            QuizTag tag = quizTagService.getQuizTagById(id);
            if (tag == null) {
                response.put("success", false);
                response.put("message", "Tag not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            if (quizTagService.isTagUsedInQuiz(id)) {
                response.put("success", false);
                response.put("message", "Cannot delete tag '" + tag.getName() + "' as it is being used in one or more quizzes");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }

            quizTagService.deleteTagById(id);
            response.put("success", true);
            response.put("message", "Tag deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to delete tag: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    @GetMapping("/tags/search")
    @ResponseBody
    public ResponseEntity<List<QuizTag>> searchTags(@RequestParam String name) {
        try {
            List<QuizTag> tags = quizTagService.searchTagsByName(name);
            return ResponseEntity.ok(tags);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/participants/{quizId}/testsession")
    public String getParticipantTestSession(
            @PathVariable Long quizId,
            @RequestParam Long participantId,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            // Get quiz participant
            QuizParticipant participant = quizParticipantRepository.findById(participantId)
                    .orElseThrow(() -> new NotFoundException("Participant not found"));

            // Get quiz information
            Quiz quiz = quizService.findById(quizId)
                    .orElseThrow(() -> new NotFoundException("Quiz not found"));

            // Get all test sessions for this participant and quiz
            List<TestSession> quizTestSessions = testSessionRepository.findByUserAndQuizOrderByStartTimeDesc(participant.getUser(), quizId);

            // Create a map to store data for each test session
            Map<TestSession, Map<String, Object>> testSessionData = new LinkedHashMap<>();

            for (TestSession testSession : quizTestSessions) {
                Map<String, Object> sessionInfo = new HashMap<>();
                List<Question> questions = new ArrayList<>(quiz.getQuestions());
                questions.sort(Comparator.comparingInt(Question::getQuestionNo));

                Map<Long, List<AnswerOption>> questionOptions = new HashMap<>();
                Map<Long, List<Answer>> participantAnswers = new HashMap<>();

                // Get all answer options for each question
                for (Question question : questions) {
                    List<AnswerOption> options = answerOptionRepository.findByQuestionIdOrderByOptionLabel(question.getId());
                    questionOptions.put(question.getId(), options);
                }

                // Get answers for this specific test session
                List<Answer> answers = answerRepository.findByTestSessionId(testSession.getId());

                // Organize answers by question
                for (Answer answer : answers) {
                    participantAnswers.computeIfAbsent(answer.getQuestion().getId(), k -> new ArrayList<>())
                            .add(answer);
                }

                // Create selected options map for this session
                Map<Long, Set<Long>> selectedOptionsMap = new HashMap<>();
                for (Map.Entry<Long, List<Answer>> entry : participantAnswers.entrySet()) {
                    Set<Long> selectedOptionIds = entry.getValue().stream()
                            .filter(a -> a.getSelectedOption() != null)
                            .map(a -> a.getSelectedOption().getId())
                            .collect(Collectors.toSet());
                    selectedOptionsMap.put(entry.getKey(), selectedOptionIds);
                }

                // Create correctness map for this session
                Map<Long, Boolean> questionCorrectnessMap = new HashMap<>();
                int correctCount = 0;
                int totalAnswerableQuestions = 0;

                for (Question question : questions) {
                    // Only count questions that have answers and are not TEXT type
                    if (question.getQuestionType() != Question.QuestionType.TEXT) {
                        totalAnswerableQuestions++;

                        List<Answer> questionAnswers = participantAnswers.get(question.getId());
                        boolean isCorrect = false;

                        if (questionAnswers != null && !questionAnswers.isEmpty()) {
                            isCorrect = questionAnswers.stream()
                                    .allMatch(a -> a.getIsCorrect() != null && a.getIsCorrect());

                            if (isCorrect) {
                                correctCount++;
                            }
                        }

                        questionCorrectnessMap.put(question.getId(), isCorrect);
                    } else {
                        // For TEXT questions, we can't automatically determine correctness
                        questionCorrectnessMap.put(question.getId(), false);
                    }
                }

                // Calculate score similar to submitQuiz method
                double calculatedScore = 0.0;
                if (totalAnswerableQuestions > 0) {
                    calculatedScore = ((double) correctCount / totalAnswerableQuestions) * 100;
                }

                // Also get score from practice results if available
                double practiceResultScore = 0.0;
                if (testSession.getPracticeResults() != null && !testSession.getPracticeResults().isEmpty()) {
                    practiceResultScore = testSession.getPracticeResults().stream()
                            .mapToDouble(PracticeResult::getScore)
                            .sum();
                }

                // Use calculated score if practice result score is 0
                double finalScore = practiceResultScore > 0 ? practiceResultScore : calculatedScore;
                double roundedScore = Math.round(finalScore * 100.0) / 100.0;

                // Calculate duration in minutes
                long durationMinutes = 0;
                if (testSession.getEndTime() != null) {
                    durationMinutes = Duration.between(testSession.getStartTime(), testSession.getEndTime()).toMinutes();
                }

                // Store all information for this session
                sessionInfo.put("questions", questions);
                sessionInfo.put("questionOptions", questionOptions);
                sessionInfo.put("participantAnswers", participantAnswers);
                sessionInfo.put("selectedOptionsMap", selectedOptionsMap);
                sessionInfo.put("questionCorrectnessMap", questionCorrectnessMap);
                sessionInfo.put("totalScore", roundedScore);
                sessionInfo.put("correctCount", correctCount);
                sessionInfo.put("totalQuestions", totalAnswerableQuestions);
                sessionInfo.put("durationMinutes", durationMinutes);

                testSessionData.put(testSession, sessionInfo);
            }

            // Add all data to model
            model.addAttribute("participant", participant);
            model.addAttribute("quiz", quiz);
            model.addAttribute("testSessionData", testSessionData);
            model.addAttribute("content", "quizes/quiz-participant-do");

            // Update the participant.attemptUsed if it doesn't match the actual count
            if (participant.getAttemptUsed() != quizTestSessions.size()) {
                participant.setAttemptUsed(quizTestSessions.size());
                quizParticipantRepository.save(participant);
            }

            return "layout";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/quizes/participants/" + quizId;
        }
    }

    @GetMapping("/download-template")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=quiz_template.xlsx");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Quiz Template");
            Row headerRow = sheet.createRow(0);

            // Tiêu đề cột (định dạng cần nhập)
            String[] headers = {"Question", "Type", "Correct", "Answer Option A", "Answer Option B", "Answer Option C", "Answer Option D"};
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            // Ghi file Excel ra response
            workbook.write(response.getOutputStream());
            response.flushBuffer();  // Đảm bảo dữ liệu được gửi đi
        }
    }

}