package com.example.assessment.controller;

import com.example.assessment.model.*;
import com.example.assessment.repository.AssessmentRepository;
import com.example.assessment.repository.InvitedCandidateRepository;
import com.example.assessment.service.*;
import com.example.course.CourseService;
import com.example.email.EmailService;
import com.example.quiz.model.*;
import com.example.exercise.model.Exercise;
import com.example.assessment.model.Assessment;
import com.example.assessment.service.AssessmentService;
import com.example.assessment.service.AssessmentTypeService;
import com.example.assessment.service.StudentAssessmentAttemptService;
import com.example.assessment.service.InvitedCandidateService;
import com.example.exercise.model.ExerciseSession;
import com.example.exercise.model.StudentExerciseAttempt;
import com.example.exercise.service.ExerciseSessionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.hashids.Hashids;
import com.example.exercise.service.ExerciseService;
import com.example.quiz.model.AnswerOption;
import com.example.quiz.model.Question;
import com.example.quiz.model.Quiz;
import com.example.quiz.service.AnswerOptionService;
import com.example.quiz.service.QuestionService;
import com.example.quiz.service.QuizService;
import com.example.user.User;
import com.example.user.UserRepository;
import com.example.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.*;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Field;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.security.Principal;
import java.time.LocalDateTime;
import java.io.ByteArrayInputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
@RequestMapping("/assessments")
@SessionAttributes("exerciseSession")
public class AssessmentController {

    @ModelAttribute("exerciseSession")
    public ExerciseSession createExerciseSession() {
        return new ExerciseSession();
    }

    @ModelAttribute("exerciseSession")
    public ExerciseSession updateExerciseSession(ExerciseSession session) {
        return session;
    }

    @PostMapping("/save_data")
    public ResponseEntity<String> saveData(@ModelAttribute("exerciseSession") ExerciseSession exerciseSession,
                         Model model,
                         @RequestBody Map<String, String> requestBody) {
        try{
            Long exerciseId = Long.parseLong(requestBody.get("exerciseId"));
            String code = requestBody.get("code");
            for(StudentExerciseAttempt attempt: exerciseSession.getStudentExerciseAttempts()){
                if(Objects.equals(attempt.getSubmitted_exercise().getId(), exerciseId)){
                    attempt.setSubmitted_code(code);
                }
            }
            this.updateExerciseSession(exerciseSession);
        } catch (NumberFormatException e) {
            System.out.println("Invalid exercise ID: " + requestBody.get("exerciseId"));
            return new ResponseEntity<>("Invalid exercise ID", HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>("Save data successful", HttpStatus.OK);
    }


    @Autowired
    private AssessmentService assessmentService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private QuizService quizService;

    @Autowired
    private ExerciseService exerciseService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private AssessmentTypeService assessmentTypeService;

    @Autowired
    private StudentAssessmentAttemptService assessmentAttemptService;

    @Autowired
    private InvitedCandidateService candidateService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserService userService;

    @Autowired
    private AnswerOptionService answerOptionService;

    @Autowired
    private AnswerOptionService AnswerOptionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AssessmentFinalScoreService assessmentFinalScoreService;

    @Autowired
    private ProgrammingLanguageService programmingLanguageService;

    @Autowired
    private InvitedCandidateRepository invitedCandidateRepository;

    //Hashids to hash the assessment id
    private Hashids hashids = new Hashids("BaTramBaiCodeThieuNhi", 32);
    @Autowired
    private EmailService emailService;
    @Autowired
    private StudentAssessmentAttemptService studentAssessmentAttemptService;
    @Autowired
    private AssessmentRepository assessmentRepository;

    @Autowired
    private ExerciseSessionService exerciseSessionService;

    // code mới
    @GetMapping("/create")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN')")

    public String createAssessment(Model model) {
        Assessment assessment = new Assessment();
        assessment.setTimeLimit(30);
        assessment.setQualifyScore(60);
        assessment.setQuizScoreRatio(50);
        assessment.setExerciseScoreRatio(50);
        List<Quiz> allQuizzes = quizService.findAll();
        model.addAttribute("allQuizzes", allQuizzes);
        // Fetch questions for each quiz and store in a Map
        Map<Long, List<Question>> quizQuestionsMap = new LinkedHashMap<>();
        for (Quiz quiz : allQuizzes) {
            List<Question> questionsForQuiz = questionService.findQuestionsByQuizId(quiz.getId());
            quizQuestionsMap.put(quiz.getId(), questionsForQuiz);
        }
        // Fetch answer options for ALL questions
        Map<Long, List<AnswerOption>> questionAnswerOptionsMap = new LinkedHashMap<>();
        List<Question> allQuestions = questionService.findAll();
        for (Question question : allQuestions) {
            List<AnswerOption> answerOptions = answerOptionService.getAnswerOptionByid(question.getId());
            questionAnswerOptionsMap.put(question.getId(), answerOptions);
        }
        model.addAttribute("questionAnswerOptionsMap", questionAnswerOptionsMap); // Add to the model
        model.addAttribute("quizQuestionsMap", quizQuestionsMap); // Add the map to the model
        model.addAttribute("questions", questionService.findAll());
        // Add other attributes
        model.addAttribute("exercises", exerciseService.findAllExercises());
        model.addAttribute("languages", programmingLanguageService.getAllProgrammingLanguages());
        model.addAttribute("assessment", assessment);
        model.addAttribute("courses", courseService.getAllCourses());
        model.addAttribute("assessmentTypes", assessmentTypeService.getAllAssessmentTypes());
        model.addAttribute("currentUser", userService.getCurrentUser());
        return "assessments/create2";
    }

    @PostMapping("/create")
    public String createAssessment(@ModelAttribute Assessment assessment, @RequestParam(value = "exercises-ids", required = false) List<String> exerciseIdsStr, @RequestParam(value = "questions-ids", required = false) List<String> questionIdsStr, Model model) {


        Set<Exercise> selectedExercisesSet = new LinkedHashSet<>();
        Set<Question> selectedQuestionsSet = new LinkedHashSet<>();
        // Get the current user
        User currentUser = userService.getCurrentUser();
        assessment.setCreatedBy(currentUser);
        if (exerciseIdsStr != null && !exerciseIdsStr.isEmpty()) {
            for (String exerciseIdStr : exerciseIdsStr) {
                Long exerciseId = Long.parseLong(exerciseIdStr);
                Optional<Exercise> exerciseOptional = exerciseService.getExerciseById(exerciseId);
                Exercise exercise = exerciseOptional.orElse(null);
                if (exercise != null) {
                    selectedExercisesSet.add(exercise);
                }
            }
        }
        if (questionIdsStr != null && !questionIdsStr.isEmpty()) {
            for (String questionIdStr : questionIdsStr) {
                Long questionId = Long.parseLong(questionIdStr);
                Optional<Question> exerciseOptional = questionService.findById(questionId);
                Question question = exerciseOptional.orElse(null);
                if (question != null) {
                    selectedQuestionsSet.add(question);
                }
            }
        }
        assessment.setExercises(selectedExercisesSet);
        List<AssessmentQuestion> assessmentQuestions = new ArrayList<>();
        int orderIndex = 1; // Initialize order index

        for (Question question : selectedQuestionsSet) {
            AssessmentQuestion aq = new AssessmentQuestion();
            aq.setAssessment(assessment);
            aq.setQuestion(question);
            aq.setOrderIndex(orderIndex);
            assessmentQuestions.add(aq);
            orderIndex++;
        }
        assessmentService.alignSequenceForAssessmentQuestion();
        assessment.setAssessmentQuestions(assessmentQuestions);
//        assessment.setUpdatedBy(currentUser);
        assessmentService.createAssessment(assessment);
        model.addAttribute("message", "Assessment created successfully!");
        return "redirect:/assessments";
    }


    @GetMapping("/duplicate/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN')")
    public String duplicateAssessment(@PathVariable("id") Long id) {
        System.out.println("duplicateAssessment");
        assessmentService.duplicateAssessment(id);
            return "redirect:/assessments";
    }

    @ModelAttribute
    public void addCommonAttributes(Model model) {
        model.addAttribute("title", "Assessments");
        model.addAttribute("links", "/style.css");
    }

    @GetMapping
    public String list(Model model, @RequestParam(value = "searchQuery", required = false) String searchQuery, @RequestParam(value = "page", defaultValue = "0") int page, @RequestParam(value = "pageSize", defaultValue = "12") int pageSize) {
        Page<Assessment> assessments;
        Pageable pageable = PageRequest.of(page, pageSize);

        if (searchQuery != null && !searchQuery.isEmpty()) {
            assessments = assessmentService.search(searchQuery, pageable);
        } else {
            assessments = assessmentService.findAll(pageable);
        }

        model.addAttribute("assessments", assessments.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", assessments.getTotalPages());
        model.addAttribute("totalItems", assessments.getTotalElements());
        model.addAttribute("searchQuery", searchQuery);
        model.addAttribute("content", "assessments/list");

        return "layout";
    }

    @GetMapping("/get-exercise-content/{exerciseId}")
    public ResponseEntity<?> getExerciseContent(@PathVariable Long exerciseId) {
        Optional<Exercise> exercise = exerciseService.getExerciseById(exerciseId);

        if (exercise.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("title", exercise.get().getName());
        response.put("content", exercise.get().getDescription());
        response.put("language", exercise.get().getLanguage());

        return ResponseEntity.ok(response); // Returns the data as JSON
    }




    @GetMapping("/create/check-duplicate")
    public ResponseEntity<Map<String, Boolean>> checkDuplicateTitle(
            @RequestParam String title,
            @RequestParam Long assessmentTypeId
    ) { // Make id optional
        System.out.println("Received checkDuplicateTitleForEdit request:"); // Basic console log

        boolean exists = assessmentService.existsByTitleAndAssessmentTypeId(title, assessmentTypeId);
        Map<String, Boolean> response = new HashMap<>();
        response.put("isDuplicate", exists);
        System.out.println(exists);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/invite/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN')")
    public String inviteCandidate(@PathVariable int id, Model model) {
        List<User> usersWithRole5 = userRepository.findByRoles_Id(2L);
        System.out.println("Users found: " + usersWithRole5.size()); // Debugging print

        model.addAttribute("assessmentId", id);
        model.addAttribute("candidate", usersWithRole5);
        return "assessments/invite";
    }

    @GetMapping("/detail/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN')")
    @Transactional(readOnly = true)
    public String showDetail(
            @PathVariable("id") Long id,
            @RequestParam(value = "pageReg", defaultValue = "0") int pageReg,
            @RequestParam(value = "pageInv", defaultValue = "0") int pageInv,
            @RequestParam(value = "searchEmail", required = false) String searchEmail,
            Model model) {

        Logger logger = LoggerFactory.getLogger(this.getClass());
        List<String> errorMessages = new ArrayList<>();

        // 1. Retrieve the assessment
        Assessment assessment = null;
        try {
            assessment = assessmentService.findById(id)
                    .orElseThrow(() -> new Exception("Assessment not found with id: " + id));
        } catch (Exception e) {
            logger.error("Error retrieving assessment with id: " + id, e);
            errorMessages.add("Error retrieving assessment: " + e.getMessage());
            // Fallback: create an empty Assessment
            assessment = new Assessment();
        }
        model.addAttribute("assessment", assessment);

        // 2. Retrieve registered attempts with pagination
        Page<StudentAssessmentAttempt> registeredAttemptsPage;
        try {
            Pageable pageableReg = PageRequest.of(pageReg, 10);
            registeredAttemptsPage = assessmentAttemptService.findByAssessment_Id(id, pageableReg);
        } catch (Exception e) {
            logger.error("Error retrieving registered attempts for assessment id: " + id, e);
            errorMessages.add("Error retrieving registered attempts: " + e.getMessage());
            registeredAttemptsPage = Page.empty();
        }

        // 3. Process registered attempts into a list of maps, including candidate username via reflection
        List<Map<String, Object>> attemptViewList = new ArrayList<>();
        try {
            attemptViewList = registeredAttemptsPage.getContent().stream().map(attempt -> {
                Map<String, Object> map = new HashMap<>();
                try {
                    map.put("id", attempt.getId());
                    map.put("email", attempt.getEmail());
                    map.put("attemptDate", attempt.getAttemptDate());
                    map.put("scoreQuiz", attempt.getScoreQuiz());
                    map.put("scoreAss", attempt.getScoreAss());
                    String candidateUsername = "N/A";
                    try {
                        Field field = attempt.getClass().getDeclaredField("user");
                        field.setAccessible(true);
                        Object userObj = field.get(attempt);
                        if (userObj != null) {
                            candidateUsername = (String) userObj.getClass().getMethod("getUsername").invoke(userObj);
                        }
                    } catch (Exception ex) {
                        logger.error("Error retrieving candidate username for attempt id " + attempt.getId(), ex);
                        errorMessages.add("Error retrieving candidate username for attempt id " + attempt.getId() + ": " + ex.getMessage());
                    }
                    map.put("candidateUsername", candidateUsername);
                } catch (Exception ex) {
                    logger.error("Error processing attempt id " + attempt.getId(), ex);
                    errorMessages.add("Error processing attempt id " + attempt.getId() + ": " + ex.getMessage());
                }
                return map;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error processing registered attempts", e);
            errorMessages.add("Error processing registered attempts: " + e.getMessage());
        }
        model.addAttribute("registeredAttempts", attemptViewList);
        model.addAttribute("registeredAttemptsPage", registeredAttemptsPage);

        // 4. Retrieve invited candidates with pagination and search
        Page<?> invitedCandidatesPage;
        try {
            Pageable pageableInv = PageRequest.of(pageInv, 10);
            if (searchEmail != null && !searchEmail.isEmpty()) {
                invitedCandidatesPage = candidateService.findByAssessmentIdAndEmailContaining(id, searchEmail, pageableInv);
            } else {
                invitedCandidatesPage = candidateService.findByAssessmentId(id, pageableInv);
            }
        } catch (Exception e) {
            logger.error("Error retrieving invited candidates for assessment id: " + id, e);
            errorMessages.add("Error retrieving invited candidates: " + e.getMessage());
            invitedCandidatesPage = Page.empty();
        }
        model.addAttribute("invitedCandidates", invitedCandidatesPage.getContent());
        model.addAttribute("invitedCandidatesPage", invitedCandidatesPage);
        model.addAttribute("searchEmail", searchEmail);

        // 5. Retrieve and sort questions from the assessment
        List<Question> orderedQuestions = new ArrayList<>();
        try {
            orderedQuestions = assessment.getAssessmentQuestions()
                    .stream()
                    .sorted(Comparator.comparingInt(AssessmentQuestion::getOrderIndex))
                    .map(AssessmentQuestion::getQuestion)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error retrieving questions for assessment id: " + id, e);
            errorMessages.add("Error retrieving questions: " + e.getMessage());
        }
        model.addAttribute("questions", orderedQuestions);

        // 6. Map each question to its answer options (to support modal view)
        Map<Long, List<AnswerOption>> questionAnswerOptionsMap = new HashMap<>();
        for (Question q : orderedQuestions) {
            try {
                List<AnswerOption> answerOptions = answerOptionService.getAnswerOptionByid(q.getId());
                questionAnswerOptionsMap.put(q.getId(), answerOptions);
            } catch (Exception e) {
                logger.error("Error retrieving answer options for question id " + q.getId(), e);
                errorMessages.add("Error retrieving answer options for question id " + q.getId() + ": " + e.getMessage());
                questionAnswerOptionsMap.put(q.getId(), new ArrayList<>());
            }
        }
        model.addAttribute("questionAnswerOptionsMap", questionAnswerOptionsMap);

        // 7. Retrieve exercises from the assessment
        try {
            model.addAttribute("exercises", assessment.getExercises());
        } catch (Exception e) {
            logger.error("Error retrieving exercises for assessment id: " + id, e);
            errorMessages.add("Error retrieving exercises: " + e.getMessage());
            model.addAttribute("exercises", new ArrayList<>());
        }

        // 7. Retrieve current user information for display
        try {
            model.addAttribute("currentUser", userService.getCurrentUser());
        } catch (Exception e) {
            logger.error("Error retrieving current user", e);
            errorMessages.add("Error retrieving current user: " + e.getMessage());
        }

        // 8. Add collected error messages to model, if any
        if (!errorMessages.isEmpty()) {
            model.addAttribute("errorMessage", String.join(" | ", errorMessages));
        }

        // 9. Set the content template for the common layout and return the view
        model.addAttribute("content", "assessments/detail");
        return "layout";
    }




//    @GetMapping("/invite/{id}")
//    public String inviteCandidate(@PathVariable int id, Model model) {
//        List<User> usersWithRole5 = userRepository.findByRoles_Id(2L);
//        System.out.println("Users found: " + usersWithRole5.size()); // Debugging print
//
//        model.addAttribute("assessmentId", id);
//        model.addAttribute("candidate", usersWithRole5);
//        return "assessments/invite";
//    }

    @PostMapping("/edit/{id}")
    public String update(@PathVariable("id") Long id, @ModelAttribute Assessment assessment) {
        assessment.setId(id);
        assessmentService.save(assessment);
        return "redirect:/assessments";
    }

    @GetMapping("/delete/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public String delete(@PathVariable("id") Long id) {
        assessmentService.deleteById(id);
        return "redirect:/assessments";
    }

    @GetMapping("/print")
    public String print(Model model) {
        List<Assessment> assessments = assessmentService.findAll();
        model.addAttribute("assessments", assessments);
        return "assessments/print";
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN')")
    public ResponseEntity<InputStreamResource> exportExcel() {
        List<Assessment> assessments = assessmentService.findAll();
        ByteArrayInputStream excelFile = assessmentService.exportToExcel(assessments);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=assessments.xlsx");

        return ResponseEntity.ok().headers(headers).body(new InputStreamResource(excelFile));
    }


    @PostMapping("/import")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN')")
    public String importExcel(@RequestParam("file") MultipartFile file) {
        assessmentService.importExcel(file);
        return "redirect:/assessments";
    }

    @GetMapping("/edit/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN')")

    public String showEditForm(@PathVariable("id") Long id, Model model) throws JsonProcessingException {
        Assessment assessment = assessmentService.findById(id).orElse(null);

        if (assessment == null) {
            return "redirect:/assessments";
        }

        // Add assessment to model
        model.addAttribute("assessment", assessment);

        // Fetch all quizzes
        List<Quiz> allQuizzes = quizService.findAll();
        model.addAttribute("allQuizzes", allQuizzes);


        Map<Long, List<Question>> quizQuestionsMap = new HashMap<>();
        for (Quiz quiz : allQuizzes) {
            quizQuestionsMap.put(quiz.getId(), questionService.findQuestionsByQuizId(quiz.getId()));
        }

        // Fetch answer options for all questions
        Map<Long, List<AnswerOption>> questionAnswerOptionsMap = new HashMap<>();
        List<Question> allQuestions = Optional.ofNullable(questionService.findAll()).orElse(new ArrayList<>());

        for (Question question : allQuestions) {
            questionAnswerOptionsMap.put(question.getId(), answerOptionService.getAnswerOptionByid(question.getId()));
        }

        List<Question> orderedQuestions = assessment.getAssessmentQuestions()
                .stream()
                .map(AssessmentQuestion::getQuestion)
                .collect(Collectors.toList());
        List<Map<String, Object>> selectedQuestionList = new ArrayList<>();
        for (Question q : orderedQuestions) {
            Map<String, Object> questionData = new HashMap<>();
            questionData.put("id", q.getId());
            questionData.put("text", q.getQuestionText());
            selectedQuestionList.add(questionData);
        }

        ObjectMapper objectMapper = new ObjectMapper();
        String selectedQuestionsJson = objectMapper.writeValueAsString(selectedQuestionList);
        System.out.println("Selected Questions JSON: " + selectedQuestionsJson);
        model.addAttribute("selectedQuestionsJson", selectedQuestionsJson);
        model.addAttribute("quizQuestionsMap", quizQuestionsMap);
        model.addAttribute("questionAnswerOptionsMap", questionAnswerOptionsMap);
        model.addAttribute("questions", allQuestions);

        // Fetch selected exercises & ensure it's not null
        List<Long> selectedExerciseIds = assessment.getExercises()
                .stream()
                .map(Exercise::getId)
                .collect(Collectors.toList());

        model.addAttribute("selectedExercises", selectedExerciseIds);

        // Add other attributes
        model.addAttribute("exercises", exerciseService.findAllExercises());
        model.addAttribute("languages", programmingLanguageService.getAllProgrammingLanguages());
        model.addAttribute("courses", courseService.getAllCourses());
        model.addAttribute("assessmentTypes", assessmentTypeService.getAllAssessmentTypes());


        //Add user models
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("creator", assessment.getCreatedBy());
        model.addAttribute("currentUser", userService.getCurrentUser());

        // Set the content for the edit view
        model.addAttribute("content", "assessments/edit");

        return "layout";
    }

    //Preview assessment
    @GetMapping("/{id}/preview")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN')")
    public String showAssessmentPreview(@PathVariable("id") Long id, Model model) {
        // Get Assessment
        Assessment assessment = assessmentService.getAssessmentByIdForPreview(id);
        if (assessment == null) {
            throw new RuntimeException("Assessment not found!");
        }
        // Get list question and exercise
        List<Question> questions = questionService.findQuestionsByAssessmentId(id);
        List<Exercise> exercises = exerciseService.getExercisesByAssessmentId(id);
        // Add to model
        model.addAttribute("assessment", assessment);
        model.addAttribute("questions", questions);
        model.addAttribute("exercises", exercises);
        return "assessments/AssessmentPreview";
    }

    @GetMapping("/invite/{id}/verify-email")
    public String showEmailSubmissionPage(@PathVariable("id") String id, Model model) {
        // Decode the ID
        System.out.println("Stored assessmentId in model: " + id);

        model.addAttribute("assessmentId", id);
        System.out.println("Return to take exam with id: " + id);
        return "assessments/verifyEmail"; // Show email input page
    }

    @PostMapping("/invite/{id}/take-exam")
    public String verifyEmail(@PathVariable("id") String rawId, @RequestParam("email") String email, Model model) {
        email = email.toLowerCase();
        // Decode the ID (but don't overwrite rawId)
        System.out.println("");
        System.out.println("");
        System.out.println("Take exam get id: " + rawId);
        long id;
        try {
            long[] temp = assessmentService.decodeId(rawId);
            if (temp.length == 0) {
                throw new IllegalArgumentException("Invalid assessment ID!");
            }
            id = temp[0];
        } catch (Exception e) {
            throw new RuntimeException("Failed to decode assessment ID: " + rawId, e);
        }

        // Store the hashed ID (rawId) in the model instead of the decoded id
        model.addAttribute("assessmentId", rawId);

        // Check expiration date for the specific email
        Optional<LocalDateTime> expireDateOpt = invitedCandidateRepository.findExpireDateByAssessmentIdAndEmail(id, email);
        if (expireDateOpt.isEmpty()) {
            return "redirect:/invalid-link";
        }

        LocalDateTime nowUtc = LocalDateTime.now();
        LocalDateTime expireDateGmt7 = expireDateOpt.get();

        // Convert nowUtc to GMT+7
        ZoneId gmt7 = ZoneId.of("Asia/Bangkok");
        ZonedDateTime nowGmt7 = nowUtc.atZone(ZoneId.of("UTC")).withZoneSameInstant(gmt7);

        if (expireDateGmt7.isBefore(nowGmt7.toLocalDateTime())) {
            String formattedExpireTime = expireDateGmt7.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            System.out.println("Formatted Expire Date: " + formattedExpireTime);
            return "redirect:/assessments/expired-link?time=" + formattedExpireTime;
        }

        // Fetch the current invited count
        Integer invitedCount = jdbcTemplate.queryForObject(
                "SELECT assessed_count FROM assessment WHERE id = ?",
                Integer.class, id
        );

        if (invitedCount == null) {
            System.out.println("❌ Error: Could not retrieve invited_count for assessment ID: " + id);
        } else {
            System.out.println("✅ Current invited_count: " + invitedCount);

            int updatedRows = jdbcTemplate.update(
                    "UPDATE assessment SET assessed_count = ? WHERE id = ?",
                    invitedCount + 1, id
            );

            if (updatedRows > 0) {
                System.out.println("✅ Updated invited_count to " + (invitedCount + 1) + " for assessment ID: " + id);
            } else {
                System.out.println("❌ Update failed! No rows affected.");
            }
        }

        // Fetch Assessment again to avoid Hibernate caching issues
        Assessment assessment = assessmentService.getAssessmentByIdForPreview(id);
        if (assessment == null) {
            throw new RuntimeException("Assessment not found!");
        }

        // Force Hibernate to refresh from DB
        assessmentRepository.refresh(assessment.getId());
        System.out.println("✅ Reloaded assessment from database.");

        // Create an assessment attempt
//        assessmentService.createAssessmentAttempt(id, email);

        // Fetch questions
        List<Question> questions = questionService.findQuestionsByAssessmentId(id);
        List<Exercise> exercises = exerciseService.getExercisesByAssessmentId(id);
        //Create attempt
        StudentAssessmentAttempt attempt = studentAssessmentAttemptService.createAssessmentAttempt(id, email);

        // create exercise session for participant
        model.addAttribute("exerciseSession", exerciseSessionService.assessmentExerciseSession(assessment, nowUtc, exercises, attempt));
        // Add to model
        model.addAttribute("assessment", assessment);
        model.addAttribute("questions", questions);
        model.addAttribute("exercises", exercises);
        model.addAttribute("email", email);
        model.addAttribute("attempt", attempt);
        return "assessments/TakeAssessment";
    }


    @GetMapping("/expired-link")
    public String showExpiredPage(@RequestParam("time") String expireTime, Model model) {
        model.addAttribute("expireTime", expireTime);
        return "assessments/expiredLink"; // Return the correct view instead of redirecting
    }

    @GetMapping("/view_score/{assessmentId}")
    public String viewGrade(@PathVariable Long assessmentId, Model model) {
        try {
            // 1. Get Assessment with error catching
            Assessment assessment = null;
            try {
                assessment = assessmentService.findById(assessmentId)
                        .orElseThrow(() -> new RuntimeException("Assessment not found with id: " + assessmentId));
            } catch (Exception e) {
                model.addAttribute("errorMessage", "Error retrieving assessment: " + e.getMessage());
                assessment = new Assessment(); // Fallback to an empty Assessment
            }
            model.addAttribute("assessment", assessment);

            // 2. Get current user
            User currentUser = null;
            try {
                currentUser = userService.getCurrentUser();
                model.addAttribute("currentUser", currentUser);
            } catch (Exception e) {
                model.addAttribute("errorMessage", "Error retrieving current user: " + e.getMessage());
                // Depending on your needs, you might choose to stop processing here.
            }

            // 3. Retrieve student's attempt to obtain scoreQuiz and duration
            int duration = 0;
            int scoreQuiz = 0;
            try {
                List<StudentAssessmentAttempt> userAttempts = studentAssessmentAttemptService.findByUserId(currentUser.getId());
                Optional<StudentAssessmentAttempt> currentAttempt = userAttempts.stream()
                        .filter(a -> a.getAssessment().getId().equals(assessmentId))
                        .findFirst();
                if (currentAttempt.isPresent()) {
                    duration = currentAttempt.get().getDuration();
                    scoreQuiz = currentAttempt.get().getScoreQuiz();
                }
            } catch (Exception e) {
                model.addAttribute("errorMessage", "Error retrieving student attempts: " + e.getMessage());
            }
            model.addAttribute("duration", duration);
            model.addAttribute("scoreQuiz", scoreQuiz);

            // 4. Get list of Questions for the assessment
            List<Question> questions = new ArrayList<>();
            try {
                questions = questionService.findQuestionsByAssessmentId(assessmentId);
            } catch (Exception e) {
                model.addAttribute("errorMessage", "Error retrieving questions: " + e.getMessage());
            }
            model.addAttribute("questions", questions);

            // 5. Map each question to its answer options
            Map<Long, List<AnswerOption>> questionAnswerOptionsMap = new HashMap<>();
            for (Question q : questions) {
                try {
                    List<AnswerOption> answerOptions = answerOptionService.getAnswerOptionByid(q.getId());
                    questionAnswerOptionsMap.put(q.getId(), answerOptions);
                } catch (Exception e) {
                    model.addAttribute("errorMessage", "Error retrieving answer options for question id "
                            + q.getId() + ": " + e.getMessage());
                    questionAnswerOptionsMap.put(q.getId(), new ArrayList<>());
                }
            }
            model.addAttribute("questionAnswerOptionsMap", questionAnswerOptionsMap);

//            // 6. Retrieve TestSession for the user (from the attempt) and assessment to get user's selected answers
//            Map<Long, Long> userAnswersMap = new HashMap<>();
//            try {
//                Optional<TestSession> testSessionOpt = quizService.findTestSessionByAssessmentIdAndUserId(assessmentId, userFromAttempt.getId());
//                if (testSessionOpt.isPresent() && testSessionOpt.get().getAnswers() != null) {
//                    for (Answer ans : testSessionOpt.get().getAnswers()) {
//                        if (ans.getQuestion() != null && ans.getSelectedOption() != null) {
//                            userAnswersMap.put(ans.getQuestion().getId(), ans.getSelectedOption().getId());
//                        }
//                    }
//                }
//            } catch (Exception e) {
//                model.addAttribute("errorMessage", "Error retrieving test session or user answers: " + e.getMessage());
//            }
//            model.addAttribute("userAnswersMap", userAnswersMap);

            // 6. Return the view; errors (if any) will be displayed in the page via errorMessage
            return "assessments/view_score";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Unexpected error occurred: " + e.getMessage());
            return "error"; // Fallback error page if something catastrophic happens
        }
    }



    @GetMapping("/viewReport/{assessmentId}")
    public String viewReport(@PathVariable Long assessmentId, @RequestParam("attempt-id") Long attemptId, Model model) {

        Optional<StudentAssessmentAttempt> attempt = studentAssessmentAttemptService.findById(attemptId);

        if (attempt != null && attempt.isPresent()) {
            JsonNode proctoringData = attempt.get().getProctoringData();
            int tabLeaveCount = proctoringData.has("tabLeaveCount") ? proctoringData.get("tabLeaveCount").asInt() : 0;

            model.addAttribute("tabLeaveCount", tabLeaveCount);
            model.addAttribute("attemptInfo", attempt.get());
        }
        model.addAttribute("content", "assessments/view_report");
        return "layout";
    }

    //Update attempt after user submit their assessment
    @PostMapping("/{id}/submit")
    public String submitAssessment(@PathVariable("id") Long assessmentId,
                                   @RequestParam("attemptId") Long attemptId,
                                   @RequestParam("elapsedTime") int elapsedTime,
                                   @RequestParam(value = "questionId", required = false) List<String> questionIds,
                                   @RequestParam("tabLeaveCount") int tabLeaveCount,
                                   @RequestParam Map<String, String> responses,
                                   Principal principal,
                                   SessionStatus sessionStatus,
                                   Model model) {
        // Lấy thông tin user
        User user = userService.findByUsername(principal.getName());
        int quizScore = 0;
        if (questionIds != null && !questionIds.isEmpty()) {
            double rawScore = quizService.calculateScore(questionIds, assessmentId, responses, user);
            quizScore = (int) Math.round(rawScore);
        }

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode proctoringData = objectMapper.createObjectNode()
                .put("tabLeaveCount", tabLeaveCount);
        // Tính điểm phần Exercise
        ExerciseSession exerciseSession = (ExerciseSession) model.getAttribute("exerciseSession");
        assert exerciseSession != null;
        this.updateExerciseSession(null);
        double rawScoreExercises = exerciseSessionService.calculateAverageExerciseScoreInAssessment(exerciseSession);
        int scoreExercise = (int) Math.round(rawScoreExercises);
        // Lưu kết quả attempt
        StudentAssessmentAttempt attempt = studentAssessmentAttemptService.saveTestAttempt(attemptId, elapsedTime, quizScore, scoreExercise, proctoringData);
        model.addAttribute("timeTaken", elapsedTime);
        return "assessments/submitAssessment";
    }

    @GetMapping("/calendar")
    public String showAssessmentCalendar() {
        return "assessments/Calendar";  // This will load the calendar.html template
    }

    @GetMapping("/already-assessed")
    public String showAlreadyAssessedError() {
        return "assessments/already-assessed";  // This will load the calendar.html template
    }
}

