package com.example.quiz.controller;

import com.example.course.Course;
import com.example.course.CourseService;
import com.example.exception.NotFoundException;
import com.example.quiz.Request.QuestionRequestDTO;
import com.example.quiz.model.*;
import com.example.quiz.repository.*;
import com.example.quiz.service.QuestionService;
import com.example.quiz.service.QuizParticipantService;
import com.example.quiz.service.QuizService;
import com.example.user.User;
import com.example.user.UserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
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
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.ByteArrayInputStream;
import java.security.Principal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


@Controller
@RequestMapping("/quizes")
public class QuizController {

    @Autowired
    private QuizParticipantRepository quizParticipantRepository;
    @Autowired
    private QuizParticipantService quizParticipantService;
    @Autowired
    private QuizService quizService;

    @Autowired
    private TestSessionRepository testSessionRepository;

    @Autowired
    private ResultRepository resultRepository;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private AnswerOptionRepository answerOptionRepository;

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private QuestionRepository questionRepository;

    // Thêm model attribute chung cho tất cả các phương thức
    @ModelAttribute
    public void addCommonAttributes(Model model) {
        model.addAttribute("title", "Quizes");
        model.addAttribute("links", "/style.css");
    }

    @GetMapping
//    @PreAuthorize("hasAuthority('ADMIN')")
    public String list(Model model,
                       @RequestParam(value = "searchQuery", required = false) String searchQuery,
                       @RequestParam(value = "page", defaultValue = "0") int page,
                       @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        Page<Quiz> quizes;
        Pageable pageable = PageRequest.of(page, pageSize);
        if (searchQuery != null && !searchQuery.isEmpty()) {
            quizes = quizService.search(searchQuery, pageable);
        } else {
            quizes = quizService.findAll(pageable);
        }

        model.addAttribute("quizes", quizes.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", quizes.getTotalPages());
        model.addAttribute("totalItems", quizes.getTotalElements());
        model.addAttribute("searchQuery", searchQuery);
        model.addAttribute("courses", courseService.getAllCourses());
        // add attribute for layout
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

        model.addAttribute("quiz", quiz);
        model.addAttribute("courses", courseService.getAllCourses());
        model.addAttribute("user", currentUser);
        model.addAttribute("users", userService.getAllUsers()); // Fetch all users for the dropdown

        model.addAttribute("content", "quizes/create");
        return "layout";
    }


    @PostMapping("/create")
    public String createQuiz(@Valid @ModelAttribute Quiz quizz, BindingResult result, Model model) {
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

        quizService.createQuiz(quizz);

        if (quizz.getQuizCategory() == Quiz.QuizCategory.PRACTICE) {
            quizService.scheduleClearCacheJob(quizz.getId());
            System.out.println("Duration for practice quiz: " + duration.toMinutes() + " minutes"); // Log thời gian nếu cần
        }

        model.addAttribute("success", "Quiz created successfully!");
        return "redirect:/quizes";
    }



    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model, Principal principal) {
        Quiz quiz = quizService.findById(id).orElse(null);
        List<Course> courses = courseService.getAllCourses();
        User user = userService.findByUsername(principal.getName());

        model.addAttribute("quiz", quiz);
        model.addAttribute("courses", courses);
        model.addAttribute("users", userService.getAllUsers()); // Fetch all users for the dropdown
        model.addAttribute("user", user);
        model.addAttribute("content", "quizes/edit");
        return "layout";
    }

    @PostMapping("/edit/{id}")
    public String update(@PathVariable("id") Long id, @ModelAttribute Quiz quiz) {
        quizService.update(id,quiz);
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

        model.addAttribute("questions", questions);
        model.addAttribute("totalQuestions", questions.size());
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


    @PostMapping("/import")
    public String importExcel(@RequestParam("file") MultipartFile file, @RequestParam("course") String courseName) {
        questionService.importExcel(file, courseName);
        return "redirect:/quizes";
    }


    @GetMapping("/participants/{quizId}")
    public String getQuizParticipants(@PathVariable Long quizId, Model model) {
        List<QuizParticipant> participants = quizParticipantService.getParticipantsByQuiz(quizId);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");


        model.addAttribute("participants", participants);
        model.addAttribute("formatter", formatter);
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
        // Fetch all participants for the given quiz ID
//        List<Quiz> participants = quizService.getParticipants(quizId);
        List<QuizParticipant> participants = quizParticipantService.getParticipantsByQuiz(quizId);
        // Log each participant's details (for debugging or printing purposes)
//        participants.forEach(participant -> {
//            System.out.println("Participant Name: " + participant.getUsername());
//            System.out.println("Participant Email: " + participant.getEmail());
//        });

        // Add participants to the model for rendering in Thymeleaf
        model.addAttribute("participants", participants);
        return "printParticipants";  // The name of the Thymeleaf template
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
        model.addAttribute("timeLimit",quiz.getDuration());
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
            @RequestParam Map<String, String> responses,
            @RequestParam("elapsedTime") int elapsedTime,
            @RequestParam("questionIds") List<String> questionId, // Thêm danh sách questionId từ form
            Model model) {

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

        // Tính điểm với danh sách questionId
        double score = quizService.calculateScore(questionId, assessmentId, responses, user);

        List<Question> questions = questionRepository.findAllById(
                questionId.stream().map(Long::parseLong).collect(Collectors.toList())
        );
        int correctCount = 0;
        Map<Long, Long> selectedAnswers = new HashMap<>();
        Map<Long, Long> correctAnswers = new HashMap<>();
        List<Answer> answerList = new ArrayList<>();

        System.out.println("Dữ liệu nhận được từ form: " + responses);

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
                    answerList.add(answer);
                    answerRepository.save(answer);
                }
            }
        }

        TestSession testSession = testSessionRepository.findTopByUserOrderByStartTimeDesc(user);
        if(testSession==null){
            throw new NotFoundException("TestSession not found");
        }

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
        model.addAttribute("timeLimit",quiz.getDuration());
        model.addAttribute("user", user);
        model.addAttribute("content", "quizes/do-quiz");

        return "layout";
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

}