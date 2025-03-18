package com.example.quiz.controller;

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
import jakarta.servlet.http.HttpSession;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.quartz.ObjectAlreadyExistsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.security.Principal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
@RequestMapping("/quizes")
public class QuizController {

    @Autowired
    private QuizTagService quizTagService;

    @Autowired
    private QuizTagRepository quizTagRepository;
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



    @GetMapping
    public String list(Model model,
                       @RequestParam(value = "searchQuery", required = false) String searchQuery,
                       @RequestParam(value = "page", defaultValue = "0") int page,
                       @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
                       @RequestParam(value = "course", required = false) Long courseId,
                       @RequestParam(value = "tags", required = false) List<Long> tagIds) {
        Page<Quiz> quizes;
        Pageable pageable = PageRequest.of(page, pageSize);

        if (courseId != null) {
            if (searchQuery != null && !searchQuery.isEmpty()) {
                // Tìm kiếm trong phạm vi course đã chọn
                quizes = quizService.searchByCourseAndName(courseId, searchQuery, pageable);
            } else {
                // Chỉ lọc theo course
                quizes = quizService.findByCourseId(courseId, pageable);
            }
        } else {
            if (searchQuery != null && !searchQuery.isEmpty()) {
                // Tìm kiếm tất cả
                quizes = quizService.search(searchQuery, pageable);
            } else {
                // Hiển thị tất cả
                quizes = quizService.findAll(pageable);
            }
        }

        // Filter by tags if tagIds are provided
        if (tagIds != null && !tagIds.isEmpty()) {
            quizes = quizService.filterByTags(tagIds, pageable);
        }

        model.addAttribute("quizes", quizes.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", quizes.getTotalPages());
        model.addAttribute("totalItems", quizes.getTotalElements());
        model.addAttribute("searchQuery", searchQuery);
        model.addAttribute("courses", courseService.getAllCourses());
        model.addAttribute("tags", quizTagService.getAllQuizTag());
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

        model.addAttribute("content", "quizes/create");
        return "layout";
    }


    @PostMapping("/create")
    @Transactional
    public String createQuiz(@Valid @ModelAttribute Quiz quizz,
                             @RequestParam(value = "tagIds", required = false) List<Long> tagIds,
                             @RequestParam(value = "newTagNames", required = false) List<String> newTagNames,
                             BindingResult result, Model model) {
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
        // Sử dụng fetch join để load tags cùng với quiz
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
        quizService.update(id, quiz);
        return "redirect:/quizes";
    }

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
        model.addAttribute("questions", sortedQuestions);
        model.addAttribute("content", "quizes/detail");

        return "layout";
    }


    // add question
    @PostMapping("/detail/{quizId}/questions/create")
    public String addQuestion(@PathVariable Long quizId, @ModelAttribute QuestionRequestDTO question) {
        quizService.createQuestion(quizId, question);
        return "redirect:/quizes/detail/" + quizId;
    }


    @PostMapping("/tags/create")
    @ResponseBody
    public ResponseEntity<String> createTag(@RequestParam String name) {
        try {
            QuizTag newTag = quizTagService.createTag(name);
            return ResponseEntity.ok("Tag created successfully with ID: " + newTag.getId());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creating tag: " + e.getMessage());
        }
    }



    @GetMapping("/participants/{quizId}")
    public String getQuizParticipants(@PathVariable Long quizId, Model model) {
        List<QuizParticipant> participants = quizParticipantService.getParticipantsByQuiz(quizId);
        model.addAttribute("participants", participants);
        model.addAttribute("content", "quizes/participants");
        return "layout";
    }

    @GetMapping("/{quizId}/participants/search")
    public String searchQuizParticipants(@PathVariable Long quizId, @RequestParam String searchTerm, Model model) {
        List<User> participants = quizService.searchParticipants(quizId, searchTerm);
        model.addAttribute("participants", participants);
        return "quizParticipants";
    }

    @GetMapping("/{quizId}/participants/export")
    public ResponseEntity<InputStreamResource> exportQuizParticipants(@PathVariable Long quizId) {
        // Fetch participants for the quiz
        List<User> participants = quizService.getParticipants(quizId);

        // Convert participants list to Excel file
        ByteArrayInputStream excelFile = quizService.exportParticipantsToExcel(participants);

        // Set headers to prompt file download
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=participants.xlsx");

        // Return the file in the response
        return ResponseEntity.ok()
                .headers(headers)
                .body(new InputStreamResource(excelFile));
    }

    @GetMapping("/{quizId}/participants/print")
    public String printAllParticipants(@PathVariable Long quizId, Model model) {
        List<QuizParticipant> participants = quizParticipantService.getParticipantsByQuiz(quizId);

        model.addAttribute("participants", participants);
        return "printParticipants";
    }

    @PostMapping("/apply/{quizId}")
    public String applyToQuiz(@PathVariable Long quizId, Principal principal, RedirectAttributes redirectAttributes, Model model) {
        Quiz quiz = quizService.getQuizById(quizId);
        User user = userService.findByUsername(principal.getName());

        Optional<QuizParticipant> optionalQuizParticipant = quizParticipantRepository.findByQuizAndUser(quiz, user);
        QuizParticipant quizParticipant;

        if (optionalQuizParticipant.isPresent()) {
            quizParticipant = optionalQuizParticipant.get();

            // Kiểm tra nếu đã đạt maxAttempt
            if (quizParticipant.getAttemptUsed() >= quiz.getAttemptLimit()) {
                redirectAttributes.addFlashAttribute("errorMessage", "You have reached the maximum number of attempts!");
                return "redirect:/quizes";
            }

            // Tăng số lần attempt
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

//        return "redirect:/quizes";
        Set<Question> questions = quiz.getQuestions(); // Lấy danh sách câu hỏi của Quiz
        model.addAttribute("quiz", quiz);
        model.addAttribute("questions", questions);
        if (Quiz.QuizCategory.PRACTICE == (quiz.getQuizCategory())) {
            Duration calculatedDuration = quizService.calculateQuizDuration(quiz.getQuestions().size());
            model.addAttribute("timeLimit", calculatedDuration.toMinutes());
        } else {
            model.addAttribute("timeLimit", quiz.getDuration());
        }

        model.addAttribute("content", "quizes/do-quiz");
        return "layout";
    }


    @GetMapping("/detail/{quizId}/view")
    public String viewQuiz(@PathVariable("quizId") Long quizId, Principal principal, RedirectAttributes redirectAttributes, Model model) {
        Quiz quiz = quizService.getQuizById(quizId);
        User user = userService.findByUsername(principal.getName());
//        return "redirect:/quizes";
        Set<Question> questions = quiz.getQuestions(); // Lấy danh sách câu hỏi của Quiz
        model.addAttribute("quizes", quiz);
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
            @RequestParam("questionIds") List<String> questionId,
            Model model) {

        System.out.println("Dữ liệu nhận được từ form: " + questionId);

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

        int totalTime = quiz.getDuration();
        int remainingTime = (totalTime * 60) - elapsedTime;

        // ✅ Tính điểm sử dụng MultiValueMap
        double score = quizService.calculateScore(questionId, assessmentId, responses, user);

        List<Question> questions = questionRepository.findAllById(
                questionId.stream().map(Long::parseLong).collect(Collectors.toList())
        );
        int correctCount = 0;
        Map<Long, List<Long>> selectedAnswers = new HashMap<>();
        Map<Long, Long> correctAnswers = new HashMap<>();
        List<Answer> answerList = new ArrayList<>();

        System.out.println("Dữ liệu nhận được từ form: " + responses);

        for (Question question : questions) {
            List<String> selectedOptionIds = responses.get("answers[" + question.getId() + "]");

            if (selectedOptionIds != null && !selectedOptionIds.isEmpty()) {
                List<Long> selectedOptionLongs = selectedOptionIds.stream()
                        .map(Long::parseLong)
                        .collect(Collectors.toList());
                selectedAnswers.put(question.getId(), selectedOptionLongs);

                List<AnswerOption> correctOptions = answerOptionRepository.findCorrectAnswersByQuestionId(question.getId());

                if (!correctOptions.isEmpty()) {
                    correctAnswers.put(question.getId(), correctOptions.get(0).getId()); // Chỉ lấy một đáp án đúng đầu tiên
                }

                boolean isCorrect = correctOptions.stream().allMatch(opt -> selectedOptionLongs.contains(opt.getId()))
                        && selectedOptionLongs.containsAll(correctOptions.stream().map(AnswerOption::getId).toList());

                if (isCorrect) {
                    correctCount++;
                }

                for (Long selectedOptionId : selectedOptionLongs) {
                    AnswerOption selectedOption = answerOptionRepository.findById(selectedOptionId).orElse(null);
                    if (selectedOption != null) {
                        Answer answer = new Answer();
                        answer.setSelectedOption(selectedOption);
                        answer.setQuestion(question);
                        answer.setAnswerText(selectedOption.getOptionText());
                        answer.setIsCorrect(correctOptions.stream().anyMatch(opt -> opt.getId().equals(selectedOptionId)));
                        answerList.add(answer);
                        answerRepository.save(answer);
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

        testSession.setAnswers(answerList);
        testSession.setEndTime(LocalDateTime.now());
        testSessionRepository.save(testSession);

        QuizParticipant participant = quizParticipantRepository.findByQuizIdAndUserId(quizId, user.getId());
        if (participant == null) {
            participant = new QuizParticipant();
            participant.setQuiz(quiz);
            participant.setUser(user);
        }
        participant.setTestSession(testSession);
        participant.setAttemptUsed(participant.getAttemptUsed() + 1);
        participant.setTimeStart(testSession.getStartTime());
        participant.setTimeEnd(testSession.getEndTime());
        quizParticipantRepository.save(participant);

        model.addAttribute("correctAnswers", correctCount);
        model.addAttribute("selectedAnswers", selectedAnswers);
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
        model.addAttribute("timeLimit", quiz.getDuration());
        model.addAttribute("user", user);
        model.addAttribute("content", "quizes/do-quiz");

        return "layout";
    }

    @GetMapping("/detail/test/{name}")
    public String viewDetailQuiz(@PathVariable String name, Model model) {
        try {
            model.addAttribute("Questions", quizService.getQuestionsOfQuiz(name));
        } catch (Exception e) {
            model.addAttribute("Errors", e.getMessage());
        }
        return "test";
    }

    @GetMapping("/detail/questions/{questionId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getQuestion(@PathVariable Long questionId) {
        try {
            Question question = questionService.findById(questionId)
                    .orElseThrow(() -> new NotFoundException("Question not found"));

            // Tạo map đơn giản chỉ chứa thông tin cần thiết
            Map<String, Object> response = new HashMap<>();
            response.put("id", question.getId());
            response.put("questionText", question.getQuestionText());
            response.put("questionType", question.getQuestionType());

            // Đơn giản hóa answerOptions
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
    /*@GetMapping("review")
    public*/

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
            // Lấy thông tin sinh viên
            User student = userRepository.findById(studentId)
                    .orElseThrow(() -> new NotFoundException("User not found"));
            model.addAttribute("student", student);

            // Lấy dữ liệu điểm số quiz
            Map<Long, String> quizCourses = quizService.getQuizCourses(studentId);
            Map<String, Object> scoresData = quizService.getScoreByQuiz(studentId);
            model.addAttribute("quizScores", scoresData.get("quizScores"));
            model.addAttribute("scoreDifferences", scoresData.get("scoreDifferences"));
            model.addAttribute("quizDurations", scoresData.get("quizDurations"));
            model.addAttribute("quizCourses", quizCourses);


            // Lấy dữ liệu số lượng quiz theo khóa học
            Map<String, Integer> courseQuizData = quizService.getQuizFromCourse(studentId);
            model.addAttribute("courseQuizCount", courseQuizData);

            model.addAttribute("content", "quizes/dashboard");
            System.out.println("Quiz Scores: " + scoresData.get("quizScores"));
            System.out.println("Quiz Courses: " + quizCourses);
            System.out.println("Quiz Durations: " + scoresData.get("quizDurations"));

            return "layout"; // Trả về layout chính
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    @DeleteMapping("/detail/{quizId}/questions")
    public ResponseEntity<String> deleteAllQuestions(@PathVariable Long quizId) {
        try {
            // Kiểm tra quyền truy cập (nếu cần)
            questionService.deleteAllQuestions(quizId);
            return ResponseEntity.ok("All questions deleted successfully.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to delete all questions: " + e.getMessage());
        }
    }

    @DeleteMapping("/detail/{quizId}/questions/{questionId}")
    public ResponseEntity<String> deleteQuestion(@PathVariable Long quizId, @PathVariable Long questionId) {
        try {
            // Kiểm tra quyền truy cập (nếu cần)
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
                               Model model) {
        if (action.equals("import")) {
            try {
                if (fileType.equals("Excel")) {
                    questionService.importExcel(file, courseName);
                } else if (fileType.equals("Json")) {
                    questionService.importJson(file, courseName);
                } else if (fileType.equals("Word")) {
                    questionService.importWord(file, courseName);
                }

                return "redirect:/quizes";
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", e.getMessage());
                return "redirect:/quizes?openModal=importModal";
            }
        } else {
            try {
                Map<String, Object> reviewData = new HashMap<>();
                if (fileType.equals("Excel")) {
                    reviewData = questionService.reviewQuiz(file, courseName);
                } else if (fileType.equals("Json")) {
                    reviewData = questionService.reviewFileJson(file, courseName);
                }

                for (Map.Entry<String, Object> entry : reviewData.entrySet()) {
                    //model.addAttribute(entry.getKey(), entry.getValue());
                    session.setAttribute(entry.getKey(), entry.getValue());
                }
                session.setAttribute("fileType", fileType);
                return "quizes/review";
            } catch (Exception e) {
                //model.addAttribute("Error", e.getMessage());
                redirectAttributes.addFlashAttribute("error", e.getMessage());
                return "redirect:/quizes?openModal=importModal";
            }
        }
    }

    @GetMapping("review/delete/{questionNo}")
    public String deleteQuestionInReview(@PathVariable int questionNo, HttpSession session) {
        List<Question> questionList = (List<Question>) session.getAttribute("Questions");
        if (questionList != null) {
            for (Question question : questionList) {
                if (question.getQuestionNo() == questionNo) {
                    questionList.remove(question);
                    break;
                }
            }
            session.setAttribute("Questions", questionList);
        }
        return "quizes/review";
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
    public String createQuizByAI(@ModelAttribute("AIRequestBody") AIRequestBody aiRequest, RedirectAttributes redirectAttributes) {

        AIResponse response = aiService.getResponseAIGenerate(aiRequest.getType(), aiRequest.getNumOfQuestions(),
                aiRequest.getNumOfAnswerOptions(),
                aiRequest.getQuestionDescription());
        System.out.println("content: " + response.getChoices().getLast().getMessage().getContent());
        return response.getChoices().getLast().getMessage().getContent();
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



    @DeleteMapping("/tags/delete/{id}")
    @ResponseBody
    public ResponseEntity<String> deleteTag(@PathVariable Long id) {
        try {
            quizTagService.deleteTagById(id);
            return ResponseEntity.ok("Tag deleted successfully.");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Cannot delete this tag as it is being used by one or more quizzes.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete tag: " + e.getMessage());
        }
    }
}