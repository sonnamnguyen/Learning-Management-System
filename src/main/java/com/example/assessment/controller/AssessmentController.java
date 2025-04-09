package com.example.assessment.controller;

import com.example.assessment.model.*;
import com.example.assessment.repository.AssessmentRepository;
import com.example.assessment.repository.InvitedCandidateRepository;
import com.example.assessment.repository.ScoreEditHistoryRepository;
import com.example.assessment.service.*;
import com.example.config.AppConfig;
import com.example.course.CourseService;
import com.example.email.EmailService;
import com.example.exercise.model.Exercise;
import com.example.assessment.model.Assessment;
import com.example.assessment.service.AssessmentService;
import com.example.assessment.service.AssessmentTypeService;
import com.example.assessment.service.StudentAssessmentAttemptService;
import com.example.assessment.service.InvitedCandidateService;
import com.example.exercise.model.ExerciseSession;
import com.example.exercise.model.StudentExerciseAttempt;
import com.example.exercise.service.ExerciseSessionService;
import com.example.exercise.service.StudentExerciseAttemptService;
import com.example.quiz.model.*;
import com.example.quiz.repository.TestSessionRepository;
import com.example.utils.CalendarEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.hashids.Hashids;
import com.example.exercise.service.ExerciseService;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.io.IOException;
import java.lang.reflect.Field;

import java.time.Duration;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.Duration;
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
import weka.core.*;
import weka.core.stemmers.IteratedLovinsStemmer;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToWordVector;


@Controller
@RequestMapping("/assessments")
@SessionAttributes("exerciseSession")
public class AssessmentController {
    @Value("${invite.url.header}")
    private String inviteUrlHeader;
    @Autowired
    private StudentExerciseAttemptService studentExerciseAttemptService;
    @Autowired
    private ScoreEditHistoryRepository scoreEditHistoryRepository;

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
        try {
            Long exerciseId = Long.parseLong(requestBody.get("exerciseId"));
            String code = requestBody.get("code");
            for (StudentExerciseAttempt attempt : exerciseSession.getStudentExerciseAttempts()) {
                if (Objects.equals(attempt.getSubmitted_exercise().getId(), exerciseId)) {
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
    private TestSessionRepository testSessionRepository;


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
    private AssessmentQuestionService assessmentQuestionService;
    @Autowired
    private ExerciseSessionService exerciseSessionService;

    @GetMapping("/create")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN')")
    public String createAssessment(Model model) {
        Assessment assessment = new Assessment();
        assessment.setTimeLimit(30);
        assessment.setQualifyScore(60);
        assessment.setExerciseScoreRatio(50);
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
        model.addAttribute("content", "assessments/create2");
        return "layout";//  return "assessments/create2";

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

    @GetMapping("/check-similarQuestions")
    public ResponseEntity<?> similarityQuiz(@RequestParam(value = "questions-ids", required = false) List<String> questionIdsStr) {
        Set<Question> selectedQuestionsSet = new LinkedHashSet<>();
        Map<Long, Integer> questionIdToPositionMap = new HashMap<>(); // Map to store questionId to its original position
        if (questionIdsStr != null && !questionIdsStr.isEmpty()) {
            int position = 1; // Start position from 1 (1-based indexing)
            for (String questionIdStr : questionIdsStr) {
                try {
                    Long questionId = Long.parseLong(questionIdStr);
                    Optional<Question> questionOptional = questionService.findById(questionId);
                    Question question = questionOptional.orElse(null);
                    if (question != null && question.getQuestionText() != null && !question.getQuestionText().trim().isEmpty()) {
                        selectedQuestionsSet.add(question);
                        questionIdToPositionMap.put(questionId, position); // Store position
                    }
                    position++; // Increment position for the next question
                } catch (NumberFormatException e) {
                    return ResponseEntity.badRequest().body("Invalid question ID: " + questionIdStr);
                }
            }
        }
        System.out.println("Checking similar questions...");
        // If no valid questions are provided, return bad request

        try {
            // Create Weka Instances (keep this part as is)
            FastVector attributes = new FastVector();
            attributes.addElement(new Attribute("content", (FastVector) null));
            Instances data = new Instances("questions", attributes, selectedQuestionsSet.size());
            data.setClassIndex(0);
            List<Question> selectedQuestionsList = new ArrayList<>(selectedQuestionsSet); // Convert Set to List for indexed access
            for (Question question : selectedQuestionsList) {
                Instance instance = new DenseInstance(1.0, new double[data.numAttributes()]);
                instance.setDataset(data);
                String processedContent = assessmentService.preprocessText(question.getQuestionText());
                instance.setValue((Attribute) attributes.elementAt(0), processedContent);
                data.add(instance);
            }
            StringToWordVector filter = new StringToWordVector();
            filter.setTFTransform(true);
            filter.setIDFTransform(true);
            filter.setLowerCaseTokens(true);
            filter.setStemmer(new IteratedLovinsStemmer()); // Set the stemmer
            filter.setInputFormat(data);
            Instances tfidfData = Filter.useFilter(data, filter);
            // Group similar question positions (using 1-based numbering from input order)
            List<List<Integer>> similarQuestionPositionsGroups = new ArrayList<>();
            Set<Integer> questionIndicesGrouped = new HashSet<>(); // Track indices already grouped (still using indices for loop)
            for (int i = 0; i < tfidfData.numInstances(); i++) {
                if (questionIndicesGrouped.contains(i)) { // Skip if index already grouped
                    continue;
                }
                List<Integer> currentGroupPositions = new ArrayList<>();
                Question question1 = selectedQuestionsList.get(i);
                currentGroupPositions.add(questionIdToPositionMap.get(question1.getId())); // Add 1-based position using the map

                questionIndicesGrouped.add(i);
                for (int j = i + 1; j < tfidfData.numInstances(); j++) {
                    double similarity = assessmentService.cosineSimilarity(tfidfData.instance(i).toDoubleArray(), tfidfData.instance(j).toDoubleArray());
                    System.out.println("similarity: Question " + questionIdToPositionMap.get(selectedQuestionsList.get(i).getId()) +
                            " vs Question " + questionIdToPositionMap.get(selectedQuestionsList.get(j).getId()) +
                            " = " + similarity);
                    if (similarity >= 0.80) { // % similarity threshold
                        if (!questionIndicesGrouped.contains(j)) { // Add if index not yet grouped
                            Question question2 = selectedQuestionsList.get(j);
                            currentGroupPositions.add(questionIdToPositionMap.get(question2.getId())); // Add 1-based position using the map
                            questionIndicesGrouped.add(j);
                        }
                    }
                }
                if (currentGroupPositions.size() > 1) { // Only add group if it has more than 1 position
                    similarQuestionPositionsGroups.add(currentGroupPositions);
                }
            }
            // Return response
            if (similarQuestionPositionsGroups.isEmpty()) {
                System.out.println("No duplicate questions found.");
                return ResponseEntity.ok("No duplicate questions found.");
            }
            System.out.println("Duplicate question groups found (by position): " + similarQuestionPositionsGroups);
            return ResponseEntity.ok(similarQuestionPositionsGroups); // Return list of position groups
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("An error occurred while processing similarity: " + e.getMessage());
        }
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
        Assessment assessment;
        try {
            assessment = assessmentService.findById(id)
                    .orElseThrow(() -> new Exception("Assessment not found with id: " + id));
        } catch (Exception e) {
            logger.error("Error retrieving assessment with id: {}", id, e);
            errorMessages.add("Error retrieving assessment: " + e.getMessage());
            // Fallback: create an empty Assessment
            assessment = new Assessment();
        }
        model.addAttribute("assessment", assessment);

        // 2. Retrieve REGISTERED ATTEMPTS with pagination (for the main table)
        Page<StudentAssessmentAttempt> registeredAttemptsPage;
        try {
            Pageable pageableReg = PageRequest.of(pageReg, 10, Sort.by(Sort.Direction.DESC, "lastModified"));
            registeredAttemptsPage = assessmentAttemptService.findByAssessment_Id(id, pageableReg);
        } catch (Exception e) {
            logger.error("Error retrieving paged attempts for assessment id: {}", id, e);
            errorMessages.add("Error retrieving paged attempts: " + e.getMessage());
            registeredAttemptsPage = Page.empty();
        }

        // 2a. Convert the paged content -> List<Map<...>> for the main table
        List<Map<String, Object>> attemptViewList = new ArrayList<>();
        try {
            List<Long> attemptIds = registeredAttemptsPage.getContent().stream()
                    .map(StudentAssessmentAttempt::getId)
                    .collect(Collectors.toList());

            // Fetch all latest comments in one query
            Map<Long, String> latestCommentsByAttemptId = new HashMap<>();
            if (!attemptIds.isEmpty()) {
                List<Object[]> commentsData = scoreEditHistoryRepository.findLatestCommentsForAttempts(attemptIds);
                for (Object[] data : commentsData) {
                    Long attemptId = (Long) data[0];
                    String comment = (String) data[1];
                    latestCommentsByAttemptId.put(attemptId, comment);
                }
            }
            for (StudentAssessmentAttempt attempt : registeredAttemptsPage.getContent()) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", attempt.getId());
                map.put("email", attempt.getEmail());
                map.put("last_modified", attempt.getLastModified());

                map.put("scoreAss", attempt.getScoreAss());
                map.put("scoreQuiz", attempt.getScoreQuiz());
                map.put("scoreEx", attempt.getScoreEx());
                map.put("latestEditComment", latestCommentsByAttemptId.get(attempt.getId()));

                attemptViewList.add(map);
            }
        } catch (Exception e) {
            logger.error("Error processing attempts for table", e);
            errorMessages.add("Error processing attempts (table): " + e.getMessage());
        }
        // Put the paged attempts (for table) in the model
        model.addAttribute("registeredAttempts", attemptViewList);
        model.addAttribute("registeredAttemptsPage", registeredAttemptsPage);

        // 2b. Retrieve ALL REGISTERED ATTEMPTS (no pagination) -> for the "View All" modal
        List<StudentAssessmentAttempt> allAttempts;
        try {
            allAttempts = assessmentAttemptService.findByAssessment_Id(id); // no pagination
        } catch (Exception e) {
            logger.error("Error retrieving all attempts for modal, assessment id: {}", id, e);
            errorMessages.add("Error retrieving all attempts for modal: " + e.getMessage());
            allAttempts = new ArrayList<>();
            errorMessages.add("Error retrieving all attempts for chart: " + e.getMessage());
        }

        // 2c. Convert allAttempts -> List<Map<...>> for the modal
        List<Map<String, Object>> allAttemptsView = new ArrayList<>();
        try {
            List<Long> attemptIds = registeredAttemptsPage.getContent().stream()
                    .map(StudentAssessmentAttempt::getId)
                    .collect(Collectors.toList());

            // Fetch all latest comments in one query
            Map<Long, String> latestCommentsByAttemptId = new HashMap<>();
            if (!attemptIds.isEmpty()) {
                List<Object[]> commentsData = scoreEditHistoryRepository.findLatestCommentsForAttempts(attemptIds);
                for (Object[] data : commentsData) {
                    Long attemptId = (Long) data[0];
                    String comment = (String) data[1];
                    latestCommentsByAttemptId.put(attemptId, comment);
                }
            }
            for (StudentAssessmentAttempt attempt : allAttempts) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", attempt.getId());
                map.put("email", attempt.getEmail());
                map.put("last_modified", attempt.getLastModified());
                map.put("scoreQuiz", attempt.getScoreQuiz());
                map.put("scoreEx", attempt.getScoreEx());
                map.put("latestEditComment", latestCommentsByAttemptId.get(attempt.getId()));

                allAttemptsView.add(map);
            }
        } catch (Exception e) {
            logger.error("Error processing all attempts for modal", e);
            errorMessages.add("Error processing all attempts for modal: " + e.getMessage());
        }
        model.addAttribute("registeredAttemptsAll", allAttemptsView);

        // 3. Build chart data from allAttempts
        List<Integer> quizScores = new ArrayList<>();
        List<Integer> exerciseScores = new ArrayList<>();
        List<String> dateLabels = new ArrayList<>();

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (StudentAssessmentAttempt att : allAttempts) {
            // Quiz
            quizScores.add(att.getScoreQuiz());
            // Exercise
            exerciseScores.add(att.getScoreEx());
            // Date
            if (att.getAttemptDate() != null) {
                dateLabels.add(att.getAttemptDate().format(dtf));
            } else {
                dateLabels.add("N/A");
            }
        }

        model.addAttribute("quizScores", quizScores);
        model.addAttribute("exerciseScores", exerciseScores);
        model.addAttribute("dateLabels", dateLabels);

        // 4. Invited candidates with pagination (table)
        Page<InvitedCandidate> invitedCandidatesPage;
        try {
            Pageable pageableInv = PageRequest.of(pageInv, 10);
            if (searchEmail != null && !searchEmail.isEmpty()) {
                invitedCandidatesPage = candidateService.findByAssessmentIdAndEmailContaining(id, searchEmail, pageableInv);
            } else {
                invitedCandidatesPage = candidateService.findByAssessmentId(id, pageableInv);
            }
        } catch (Exception e) {
            logger.error("Error retrieving invited candidates (paged) for assessment id: {}", id, e);
            errorMessages.add("Error retrieving invited candidates: " + e.getMessage());
            invitedCandidatesPage = Page.empty();
        }
        model.addAttribute("invitedCandidates", invitedCandidatesPage.getContent());
        model.addAttribute("invitedCandidatesPage", invitedCandidatesPage);

        // 4a. Retrieve ALL invited candidates (no pagination) for the "View All" modal
        List<InvitedCandidate> allInvited;
        try {
            allInvited = candidateService.findByAssessmentId(id); // no paging
        } catch (Exception e) {
            logger.error("Error retrieving all invited candidates for modal", e);
            errorMessages.add("Error retrieving all invited candidates: " + e.getMessage());
            allInvited = new ArrayList<>();
        }
        model.addAttribute("invitedCandidatesAll", allInvited);

        // 5. Retrieve & sort questions
        List<Question> orderedQuestions = new ArrayList<>();
        try {
            orderedQuestions = assessment.getAssessmentQuestions()
                    .stream()
                    .sorted(Comparator.comparingInt(AssessmentQuestion::getOrderIndex))
                    .map(AssessmentQuestion::getQuestion)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error retrieving questions for assessment id: {}", id, e);
            errorMessages.add("Error retrieving questions: " + e.getMessage());
        }
        model.addAttribute("questions", orderedQuestions);

        // 6. Map each question to answer options
        Map<Long, List<AnswerOption>> questionAnswerOptionsMap = new HashMap<>();
        for (Question q : orderedQuestions) {
            try {
                List<AnswerOption> answerOptions = answerOptionService.getAnswerOptionByid(q.getId());
                questionAnswerOptionsMap.put(q.getId(), answerOptions);
            } catch (Exception e) {
                logger.error("Error retrieving answer options for question id {}", q.getId(), e);
                errorMessages.add("Error retrieving answer options for question id " + q.getId() + ": " + e.getMessage());
                questionAnswerOptionsMap.put(q.getId(), new ArrayList<>());
            }
        }
        model.addAttribute("questionAnswerOptionsMap", questionAnswerOptionsMap);

        // 7. Retrieve exercises
        try {
            model.addAttribute("exercises", assessment.getExercises());
        } catch (Exception e) {
            logger.error("Error retrieving exercises for assessment id: {}", id, e);
            errorMessages.add("Error retrieving exercises: " + e.getMessage());
            model.addAttribute("exercises", new ArrayList<>());
        }

        // 8. Current user
        try {
            model.addAttribute("currentUser", userService.getCurrentUser());
        } catch (Exception e) {
            logger.error("Error retrieving current user", e);
            errorMessages.add("Error retrieving current user: " + e.getMessage());
        }

        // 9. Errors
        if (!errorMessages.isEmpty()) {
            model.addAttribute("errorMessage", String.join(" | ", errorMessages));
        }

        // 10. Return the view
        model.addAttribute("searchEmail", searchEmail);
        model.addAttribute("content", "assessments/detail");
        return "layout";
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

    /**
     * Displays the edit form for an assessment.
     * <p>
     * This method retrieves an assessment by its ID and prepares all necessary data
     * for rendering the edit page. It includes fetching related quizzes, questions,
     * answer options, exercises, and other assessment-related attributes.
     *
     * @param id    The ID of the assessment to edit.
     * @param model The model to hold attributes for the view.
     * @return The name of the Thymeleaf template to render.
     * @throws JsonProcessingException If there is an error converting objects to JSON.
     */
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
        List<Question> allQuestions = Optional.ofNullable(questionService.findAll()).orElse(new ArrayList<>());


        //Selected question list
        List<Map<String, Object>> selectedQuestionList = assessment.getAssessmentQuestions()
                .stream()
                .sorted(Comparator.comparing(AssessmentQuestion::getOrderIndex))
                .map(aq -> {
                    Map<String, Object> questionData = new HashMap<>();
                    questionData.put("questionId", aq.getQuestion().getId());
                    questionData.put("text", aq.getQuestion().getQuestionText());
                    questionData.put("orderIndex", aq.getOrderIndex());

                    Quiz quiz = aq.getQuestion().getQuizzes();
                    Long quizId = (quiz != null) ? quiz.getId() : null;

                    questionData.put("quizId", quizId);

                    return questionData;
                })
                .collect(Collectors.toList());

        //Answer list
        Map<Long, List<AnswerOption>> questionAnswerOptionsMap = new HashMap<>();

        for (Question question : allQuestions) {
            questionAnswerOptionsMap.put(question.getId(), answerOptionService.getAnswerOptionByid(question.getId()));
        }

        Map<Long, List<Map<String, Object>>> answersJsonMap = new HashMap<>();
        for (Question question : allQuestions) {
            List<Map<String, Object>> answerList = questionAnswerOptionsMap.getOrDefault(question.getId(), new ArrayList<>())
                    .stream()
                    .map(answer -> {
                        Map<String, Object> answerData = new HashMap<>();
                        answerData.put("questionId", question.getId());
                        answerData.put("answerId", answer.getId());
                        answerData.put("text", answer.getOptionText());
                        answerData.put("isCorrect", answer.getIsCorrect());
                        return answerData;
                    })
                    .collect(Collectors.toList());
            answersJsonMap.put(question.getId(), answerList);
        }

        ObjectMapper objectMapper = new ObjectMapper();
        String selectedQuestionsJson = objectMapper.writeValueAsString(selectedQuestionList);
        String answersJson = objectMapper.writeValueAsString(answersJsonMap);

//        DEBUG: log selected question list json
//        System.out.println("-------------------------------------------------------");
//        selectedQuestionList.forEach(item -> {
//            String json = item.entrySet()
//                    .stream()
//                    .map(entry -> String.format("\"%s\":%s", entry.getKey(),
//                            entry.getValue() instanceof String ? "\"" + entry.getValue() + "\"" : entry.getValue()))
//                    .collect(Collectors.joining(", "));
//
//            System.out.println("{" + json + "}");
//        });

//        DEBUG: log answer of the question json
//        System.out.println("-------------------------------------------------------");
//        answersJsonMap.forEach((questionId, answerList) -> {
//            System.out.println("Answers for Question ID " + questionId + ":");
//            answerList.forEach(answer -> {
//                String json = answer.entrySet()
//                        .stream()
//                        .map(entry -> String.format("\"%s\":%s", entry.getKey(),
//                                entry.getValue() instanceof String ? "\"" + entry.getValue() + "\"" : entry.getValue()))
//                        .collect(Collectors.joining(", "));
//
//                System.out.println("  {" + json + "}");
//            });
//        });

        model.addAttribute("selectedQuestionsJson", selectedQuestionsJson);
        model.addAttribute("quizQuestionsMap", quizQuestionsMap);
        model.addAttribute("answersJson", answersJson);
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
        model.addAttribute("updater", assessment.getUpdatedBy());
        model.addAttribute("currentUser", userService.getCurrentUser());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        User updater = assessment.getUpdatedBy();
        model.addAttribute("updater", updater);
        if (updater != null) {
            String formattedUpdatedAt = assessment.getUpdatedAt().format(formatter);
            model.addAttribute("formattedUpdatedAt", formattedUpdatedAt);
        }

        String formattedCreateAt = assessment.getCreatedAt().format(formatter);
        model.addAttribute("formattedCreateAt", formattedCreateAt);

        // Set the content for the edit view
        model.addAttribute("content", "assessments/edit");

        return "layout";
    }

    @GetMapping("/questionList/{assessmentId}")
    @ResponseBody
    public List<Map<String, Object>> getQuestionsByAssessment(@PathVariable Long assessmentId) {
        List<AssessmentQuestion> assessmentQuestions = assessmentQuestionService.findByAssessmentId(assessmentId);

        return assessmentQuestions.stream().map(aq -> {
            Map<String, Object> questionMap = new HashMap<>();
            questionMap.put("id", aq.getQuestion().getId());  // Lấy ID từ Question
            questionMap.put("orderIndex", aq.getOrderIndex()); // Lấy orderIndex từ AssessmentQuestion
            return questionMap;
        }).collect(Collectors.toList());
    }

    @PostMapping("/update/{id}")
    public String updateAssessment(
            @PathVariable Long id,
            @ModelAttribute Assessment assessment,
            @RequestParam(value = "exerciseIds", required = false) List<Long> exerciseIds,
            @RequestParam(value = "newAddedQuestions", required = false) String newAddedQuestionsJson,
            RedirectAttributes redirectAttributes) {

        System.out.println("[Controller] Updating Assessment ID: " + id);
        System.out.println("📥 JSON nhận được từ FE: " + newAddedQuestionsJson);

        Assessment existingAssessment = assessmentService.findById(id)
                .orElseThrow(() -> {
                    System.out.println("Assessment not found with ID: " + id);
                    return new RuntimeException("Assessment not found with ID: " + id);
                });

        existingAssessment.setTitle(assessment.getTitle());
        existingAssessment.setAssessmentType(assessment.getAssessmentType());
        existingAssessment.setCourse(assessment.getCourse());
        existingAssessment.setTimeLimit(assessment.getTimeLimit());
        existingAssessment.setQualifyScore(assessment.getQualifyScore());
        existingAssessment.setShuffled(assessment.isShuffled());
        existingAssessment.setQuizScoreRatio(assessment.getQuizScoreRatio());
        existingAssessment.setExerciseScoreRatio(assessment.getExerciseScoreRatio());
        existingAssessment.setUpdatedAt(ZonedDateTime.now().toLocalDateTime());

        User currentUser = userService.getCurrentUser();
        existingAssessment.setUpdatedBy(currentUser);
        existingAssessment.setUpdatedAt(
                ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDateTime()
        );

        if (exerciseIds != null && !exerciseIds.isEmpty()) {
            Set<Exercise> selectedExercises = new HashSet<>(exerciseService.findByIds(exerciseIds));
            existingAssessment.setExercises(selectedExercises);
            System.out.println("Updated " + selectedExercises.size() + " exercises in the assessment.");
        } else {
            existingAssessment.getExercises().clear();
            System.out.println("No exercises selected.");
        }

        List<AssessmentQuestionService.QuestionOrder> questionOrders = new ArrayList<>();
        if (newAddedQuestionsJson != null && !newAddedQuestionsJson.isBlank()) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                questionOrders = objectMapper.readValue(newAddedQuestionsJson, new TypeReference<>() {
                });
                System.out.println("Parsed " + questionOrders.size() + " questions from JSON.");
            } catch (Exception e) {
                System.out.println("❌ Lỗi khi parse JSON: " + e.getMessage());
                redirectAttributes.addFlashAttribute("error", "Invalid question data format.");
                return "redirect:/assessments";
            }
        } else {
            System.out.println("No new questions provided.");
        }

        // Update the list of questions in the assessment
        assessmentQuestionService.updateAssessmentQuestions(id, questionOrders);
        System.out.println("Updated question list in the assessment.");

        // Save the updated assessment in the database
        assessmentService.saveAssessment(existingAssessment);
        System.out.println("Assessment ID " + id + " has been successfully updated.");

        // Redirect with success message
        redirectAttributes.addFlashAttribute("message", "Assessment updated successfully!");
        return "redirect:/assessments";
    }

    @GetMapping("/edit/check-duplicate")
    public ResponseEntity<Map<String, Boolean>> checkDuplicateTitle(
            @RequestParam String title,
            @RequestParam Long assessmentTypeId,
            @RequestParam Long id) {

        boolean exists = assessmentService.existsByTitleAndAssessmentType(title, assessmentTypeId, id);
        Map<String, Boolean> response = new HashMap<>();
        response.put("isDuplicate", exists);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/edit/check-similar-new-question/add-batch")
    public ResponseEntity<?> checkSimilarQuestionsBatch(@RequestBody Map<String, List<Map<String, Object>>> requestData) {
        List<Map<String, Object>> questionsList = requestData.get("questions"); // ✅ Lấy danh sách câu hỏi mới

        System.out.println("📌 Received questionsList: " + questionsList);

        if (questionsList == null || questionsList.isEmpty()) {
            return ResponseEntity.badRequest().body("No questions provided.");
        }

        // Lấy danh sách văn bản câu hỏi và ID từ danh sách câu hỏi mới
        List<String> questionTexts = questionsList.stream()
                .map(q -> String.valueOf(q.get("text")))
                .filter(Objects::nonNull)
                .map(text -> text.replaceAll("\\<.*?\\>", "").trim())
                .collect(Collectors.toList());

        List<Integer> questionIds = questionsList.stream()
                .map(q -> (Integer) q.get("questionId")) // ✅ Lấy questionId kiểu Integer
                .collect(Collectors.toList());

        if (questionTexts.isEmpty()) {
            return ResponseEntity.badRequest().body("No valid questions found.");
        }

        try {
            // Chuẩn bị dữ liệu TF-IDF
            ArrayList<Attribute> attributes = new ArrayList<>();
            attributes.add(new Attribute("content", (List<String>) null));
            Instances data = new Instances("questions", attributes, questionTexts.size());
            data.setClassIndex(0);

            for (String text : questionTexts) {
                double[] instanceValue = new double[data.numAttributes()];
                instanceValue[0] = data.attribute(0).addStringValue(text);
                Instance instance = new DenseInstance(1.0, instanceValue);
                instance.setDataset(data);
                data.add(instance);
                System.out.println("  - " + text);
            }

            if (data.numInstances() == 0) {
                return ResponseEntity.badRequest().body("No valid questions found.");
            }

            // Áp dụng TF-IDF
            StringToWordVector filter = new StringToWordVector();
            filter.setOutputWordCounts(true);
            filter.setLowerCaseTokens(true);
            filter.setWordsToKeep(5000);
            filter.setDoNotOperateOnPerClassBasis(true);
            filter.setMinTermFreq(1);
            filter.setAttributeIndices("first-last");

            filter.setInputFormat(data);
            Instances tfidfData = Filter.useFilter(data, filter);

            // Bước 1: Xác định các câu hỏi trùng nhau
            Map<Integer, List<Integer>> duplicateQuestions = new HashMap<>();

            for (int i = 0; i < tfidfData.numInstances(); i++) {
                for (int j = i + 1; j < tfidfData.numInstances(); j++) {
                    double similarity = assessmentService.cosineSimilarityForEdit(
                            tfidfData.instance(i).toDoubleArray(),
                            tfidfData.instance(j).toDoubleArray()
                    );

                    if (similarity >= 0.80) {
                        duplicateQuestions.computeIfAbsent(i, k -> new ArrayList<>()).add(j);
                        duplicateQuestions.computeIfAbsent(j, k -> new ArrayList<>()).add(i);
                    }
                }
            }

            // Chuyển đổi kết quả về dạng mong muốn
            Map<Integer, List<Integer>> duplicateLists = new HashMap<>();
            for (Map.Entry<Integer, List<Integer>> entry : duplicateQuestions.entrySet()) {
                int questionId = questionIds.get(entry.getKey());
                List<Integer> similarQuestionIds = entry.getValue().stream()
                        .map(questionIds::get)
                        .collect(Collectors.toList());
                duplicateLists.put(questionId, similarQuestionIds);
            }

            System.out.println("📌 Final Duplicate Lists: " + duplicateLists);

            if (duplicateLists.isEmpty()) {
                return ResponseEntity.ok(Collections.singletonMap("message", "No duplicate questions found."));
            }

            return ResponseEntity.ok(Collections.singletonMap("duplicateLists", duplicateLists));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Collections.singletonMap("error", "An error occurred while processing similarity: " + e.getMessage()));
        }
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
        if (assessment.isShuffled()) {
            Collections.shuffle(questions);
            Collections.shuffle(exercises);
        }
        // Add to model
        model.addAttribute("assessment", assessment);
        model.addAttribute("questions", questions);
        model.addAttribute("exercises", exercises);
        return "assessments/AssessmentPreview";
    }

    @GetMapping("/invite/{encodedId}/verify-email")
    public String showEmailSubmissionPage(@PathVariable("encodedId") String encodedId, Model model) {
        // Decode the ID
        System.out.println("Stored assessmentId in model: " + encodedId);

        long[] decoded = hashids.decode(encodedId);

        if (decoded.length == 0) {
            throw new IllegalArgumentException("Invalid HashID: " + encodedId);
        }

        long assessmentId = decoded[0]; // Extract the first decoded long

        model.addAttribute("assessmentId", assessmentId);
        System.out.println("Return to take exam with id: " + assessmentId);

        return "assessments/verifyEmail"; // Show email input page
    }

    @PostMapping("/invite/take-exam/{id}")
    //rawId is actually true Id
    public String verifyEmail(@PathVariable("id") String rawId, @RequestParam("email") String email, Model model) {
        //Check if invited candidate -> has_assessed = true or not
        Optional<InvitedCandidate> invitedCandidateOpt = invitedCandidateRepository.findByAssessmentIdAndEmail(Long.parseLong(rawId), email);
        if (invitedCandidateOpt.isEmpty()) {
            model.addAttribute("message", "You have already taken this exam.");
            return "redirect:/assessments/invalid-link"; // Redirect to an "Already Taken" page
        }
        InvitedCandidate invitedCandidate = invitedCandidateOpt.get();
        if (invitedCandidate.isHasAssessed()) {
            model.addAttribute("message", "You have already taken this exam.");
            return "redirect:/assessments/already-assessed"; // Redirect to an "Already Taken" page
        }
        //Change has_assessed -> Invited Candidate to true
        invitedCandidateRepository.updateHasAssessedByEmailAndAssessmentId(email, Long.parseLong(rawId));
        email = email.toLowerCase();
        long id = Long.parseLong(rawId);

        // Store the hashed ID (rawId) in the model instead of the decoded id
        model.addAttribute("assessmentId", rawId);

        // Check expiration date for the specific email
        Optional<LocalDateTime> expireDateOpt = invitedCandidateRepository.findExpireDateByAssessmentIdAndEmail(id, email);
        if (expireDateOpt.isEmpty()) {
            return "assessments/invalid-link";
        }

        LocalDateTime nowUtc = LocalDateTime.now();
        LocalDateTime expireDateGmt7 = expireDateOpt.get();

        // Convert nowUtc to GMT+7
        ZoneId gmt7 = ZoneId.of("Asia/Bangkok");
        ZonedDateTime nowGmt7 = nowUtc.atZone(ZoneId.of("UTC"));

        if (expireDateGmt7.isBefore(nowGmt7.toLocalDateTime())) {
            String formattedExpireTime = expireDateGmt7.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            System.out.println("Formatted Expire Date: " + formattedExpireTime);
            return "redirect:/assessments/expired-link?time=" + formattedExpireTime;
        }

        // Fetch the current assessed_count count
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
        if (assessment.isShuffled()) {
            Collections.shuffle(questions);
            Collections.shuffle(exercises);
        }
        //Create attempt
        StudentAssessmentAttempt attempt = studentAssessmentAttemptService.createAssessmentAttempt(id, email);

        // create exercise session for participant
        model.addAttribute("exerciseSession", exerciseSessionService.assessmentExerciseSession(assessment, nowUtc, exercises, attempt));
        assessmentService.incrementAssessedCount(Long.parseLong(rawId));
        // Add to model
        model.addAttribute("assessment", assessment);
        model.addAttribute("questions", questions);
        model.addAttribute("exercises", exercises);
        model.addAttribute("email", email);
        model.addAttribute("attempt", attempt);
        return "assessments/TakeAssessment";
    }

    @GetMapping("/invalid-link")
    public String showInvalidLinkPage() {
        return "assessments/invalid-link"; // Directly returns the invalid-link.html page
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
            return "error";
        }
    }


    @GetMapping("/viewReport/{assessmentId}")
    public String viewReport(@PathVariable Long assessmentId,
                             @RequestParam("attempt-id") Long attemptId,
                             Model model) {
        Optional<StudentAssessmentAttempt> attemptOpt = studentAssessmentAttemptService.findById(attemptId);

        if (attemptOpt.isPresent()) {
            StudentAssessmentAttempt attempt = attemptOpt.get();
            JsonNode proctoringData = attempt.getProctoringData();
            int tabLeaveCount = proctoringData.has("tabLeaveCount") ? proctoringData.get("tabLeaveCount").asInt() : 0;
            int violationFaceCount = proctoringData.has("violationFaceCount") ? proctoringData.get("violationFaceCount").asInt() : 0;

            int copyCount = 0;
            int pasteCount = 0;

            if (proctoringData.has("exerciseCopyPasteData")) {
                String copyPasteDataStr = proctoringData.get("exerciseCopyPasteData").asText();
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode copyPasteData = objectMapper.readTree(copyPasteDataStr);
                    Iterator<Map.Entry<String, JsonNode>> fields = copyPasteData.fields();
                    while (fields.hasNext()) {
                        Map.Entry<String, JsonNode> entry = fields.next();
                        JsonNode exerciseData = entry.getValue();
                        copyCount += exerciseData.has("copy") ? exerciseData.get("copy").asInt() : 0;
                        pasteCount += exerciseData.has("paste") ? exerciseData.get("paste").asInt() : 0;
                    }
                } catch (Exception e) {
                    System.out.println("Error parsing copyPasteData JSON: " + e.getMessage());
                }
            }

            model.addAttribute("tabLeaveCount", tabLeaveCount);
            model.addAttribute("violationFaceCount", violationFaceCount);
            model.addAttribute("copyCount", copyCount);
            model.addAttribute("pasteCount", pasteCount);
            model.addAttribute("attemptInfo", attempt);

            Assessment assessment = attempt.getAssessment();
            if (assessment != null) {
                model.addAttribute("assessment", assessment);

                // Lấy danh sách câu hỏi dựa trên attemptId
                List<Question> questions = new ArrayList<>();
                try {
                    questions = questionService.findQuestionsByAttemptId(attemptId);
                } catch (Exception e) {
                    model.addAttribute("errorMessage",
                            "Error retrieving questions for attempt " + attemptId + ": " + e.getMessage());
                }
                model.addAttribute("questions", questions);

                Map<Long, List<AnswerOption>> questionAnswerOptionsMap = new HashMap<>();
                for (Question q : questions) {
                    try {
                        List<AnswerOption> ansList = answerOptionService.getAnswerOptionByid(q.getId());
                        questionAnswerOptionsMap.put(q.getId(), ansList);
                    } catch (Exception e) {
                        model.addAttribute("errorMessage",
                                "Error retrieving answer options for question id " + q.getId() + ": " + e.getMessage());
                        questionAnswerOptionsMap.put(q.getId(), new ArrayList<>());
                    }
                }
                model.addAttribute("questionAnswerOptionsMap", questionAnswerOptionsMap);

                // Lấy đáp án của người dùng từ TestSession liên kết với attempt
                Optional<TestSession> tsOpt = testSessionRepository.findByStudentAssessmentAttemptId(attemptId);
                Map<Long, List<Long>> userAnswersMap = new HashMap<>();
                Map<Long, String> userAnswerTexts = new HashMap<>();
                if (tsOpt.isPresent() && tsOpt.get().getAnswers() != null) {
                    for (Answer ans : tsOpt.get().getAnswers()) {
                        if (ans.getQuestion() != null) {
                            Long questionId = ans.getQuestion().getId();
                            if (ans.getSelectedOption() != null) {
                                Long answerOptionId = ans.getSelectedOption().getId();
                                userAnswersMap.computeIfAbsent(questionId, k -> new ArrayList<>()).add(answerOptionId);
                            } else if (ans.getAnswerText() != null && !ans.getAnswerText().trim().isEmpty()) {
                                userAnswerTexts.put(questionId, ans.getAnswerText());
                            }
                        }
                    }
                }
                model.addAttribute("userAnswersMap", userAnswersMap);
                model.addAttribute("userAnswerTexts", userAnswerTexts);

                LocalDateTime attemptDate = attempt.getAttemptDate();
                List<StudentExerciseAttempt> studentExerciseAttempts = exerciseSessionService.findStudentExerciseAttemptsByAttemptDate(attemptDate);
                model.addAttribute("studentExerciseAttempts", studentExerciseAttempts);

            }
        }
        model.addAttribute("content", "assessments/view_report");
        return "layout";
    }






    @PostMapping("/capture-image")
    public ResponseEntity<Integer> captureImage(@RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(assessmentService.detectFace(file));
    }

    //Update attempt after user submit their assessment
    @PostMapping("/submit/{id}")
    public String submitAssessment(@PathVariable("id") Long assessmentId,
                                   @RequestParam("attemptId") Long attemptId,
                                   @RequestParam("elapsedTime") int elapsedTime,
                                   @RequestParam(value = "questionId", required = false) List<String> questionIds,
                                   @RequestParam("tabLeaveCount") int tabLeaveCount,
                                   @RequestParam("violationFaceCount") int violationFaceCount,
                                   @RequestParam("exerciseCopyPasteData") String exerciseCopyPasteData,
                                   @RequestParam MultiValueMap<String, String> responses,
                                   @RequestParam("hasExercise") boolean hasExercise,
                                   SessionStatus sessionStatus,
                                   Model model) {
        User user = null;
        int quizScore = 0;
        int scoreExercise = 0;
        StudentAssessmentAttempt studentAssessmentAttempt = studentAssessmentAttemptService.findStudentAssessmentAttemptById(attemptId);
        if (questionIds != null && !questionIds.isEmpty()) {
            double rawScore = quizService.calculateScore(questionIds, assessmentId, responses, user, studentAssessmentAttempt);
            quizScore = (int) Math.round(rawScore);
        }
        //Save cheating count
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode proctoringData = objectMapper.createObjectNode()
                .put("tabLeaveCount", tabLeaveCount)
                .put("violationFaceCount", violationFaceCount)
                .put("exerciseCopyPasteData", exerciseCopyPasteData);
        if (hasExercise) {
            // Tính điểm phần Exercise
            ExerciseSession exerciseSession = (ExerciseSession) model.getAttribute("exerciseSession");
            assert exerciseSession != null;
            double rawScoreExercises = exerciseSessionService.calculateAverageExerciseScoreInAssessment(exerciseSession);
            scoreExercise = (int) Math.round(rawScoreExercises);
            this.updateExerciseSession(null);
        }
        // Lưu kết quả attempt
        StudentAssessmentAttempt attempt = studentAssessmentAttemptService.saveTestAttempt(attemptId, elapsedTime, quizScore, scoreExercise, proctoringData);
        Optional<Assessment> assessment = assessmentService.findById(assessmentId);
        if (attempt.getScoreAss() >= assessment.get().getQualifyScore()) {
            assessmentService.updateQualifiedCount(assessmentId);
        }
        model.addAttribute("timeTaken", elapsedTime);
        return "assessments/submitAssessment";
    }

    @GetMapping("/calendar")
    public String showAssessmentCalendar(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        List<InvitedCandidate> userAssessments = invitedCandidateRepository.findByEmail(userEmail);

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        // Convert to JSON-friendly format
        List<CalendarEvent> events = userAssessments.stream()
                .map(ic -> {
                    String encodedId = hashids.encode(ic.getAssessment().getId());
                    String inviteLink = inviteUrlHeader + encodedId + "/verify-email"; // ✅ Pass invite link

                    return new CalendarEvent(
                            ic.getAssessment().getTitle(),
                            ic.getInvitationDate().format(formatter),
                            ic.getExpirationDate().format(formatter),
                            inviteLink // ✅ Include inviteLink
                    );
                })
                .toList();

        // 🔍 Debugging: Log data before passing it to the view
        System.out.println("📝 Sending events to Thymeleaf: " + events);

        model.addAttribute("events", events);
        model.addAttribute("assessments", userAssessments);

        return "assessments/Calendar";


    }

    @GetMapping("/calendar/events")
    @ResponseBody
    public List<CalendarEvent> getCalendarEvents() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        Optional<User> user = userRepository.findByUsername(username);
        if (user.isEmpty()) {
            System.out.println("❌ User not found: " + username);
            return Collections.emptyList();
        }

        String userEmail = user.get().getEmail();
        System.out.println("🔍 Fetching events for email: " + userEmail);

        String inviteUrlHeader = "https://java02.fsa.io.vn/assessments/invite/";
        List<InvitedCandidate> invitedAssessments = invitedCandidateRepository.findByEmail(userEmail);

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime now = LocalDateTime.now();

        List<CalendarEvent> events = invitedAssessments.stream()
                .map(a -> {
                    String encodedId = hashids.encode(a.getAssessment().getId());
                    String inviteLink = inviteUrlHeader + encodedId + "/verify-email";

                    boolean hasAssessed = a.isHasAssessed();
                    LocalDateTime expirationDate = a.getExpirationDate();
                    long hoursLeft = Duration.between(now, expirationDate).toHours();

                    // 📝 New logic to fix expired events
                    String title = hasAssessed
                            ? "<del>" + a.getAssessment().getTitle() + "</del>" // ✅ Completed = strikethrough
                            : a.getAssessment().getTitle();

                    CalendarEvent event = new CalendarEvent(
                            title,
                            a.getInvitationDate().format(formatter),
                            expirationDate.format(formatter),
                            inviteLink
                    );

                    // ✅ If event is expired, override the color to gray
                    if (hoursLeft <= 0) {
                        event.setColor("#6c757d"); // Gray for expired events
                    } else if (hoursLeft < 24) {
                        event.setColor("#dc3545"); // Red for soon-to-expire events
                    }

                    return event;
                })
                .toList();

        System.out.println("📅 Events to return: " + events);
        return events;
    }


    @GetMapping("/already-assessed")
    public String showAlreadyAssessedError() {
        return "assessments/already-assessed";  // This will load the calendar.html template
    }

    @Controller
    @RequestMapping("/api/score")
    public class AssessmentScoreController {
        @Autowired
        private AssessmentScoreService assessmentScoreService;

        @PutMapping("/edit/{attemptId}")
        public ResponseEntity<?> editScore(@PathVariable Long attemptId,
                                           @RequestBody Map<String, Object> requestMap) {
            try {
                Map<String, Object> result = assessmentScoreService.editScore(attemptId, requestMap);
                return ResponseEntity.ok(result);
            } catch (ResourceNotFoundException e) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", e.getMessage()));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", e.getMessage()));
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Lỗi khi chỉnh sửa điểm: " + e.getMessage()));
            }
        }

        @GetMapping("/edit/{attemptId}")
        public String editScorePage(@PathVariable Long attemptId, Model model) {
            model.addAttribute("attemptId", attemptId);
            return "assessments/editscore";
        }

        @GetMapping("/current/{attemptId}")
        public ResponseEntity<Map<String, Object>> getCurrentScores(@PathVariable Long attemptId) {
            Map<String, Object> currentScores = assessmentScoreService.getCurrentScores(attemptId);
            return ResponseEntity.ok(currentScores);
        }
    }
}
