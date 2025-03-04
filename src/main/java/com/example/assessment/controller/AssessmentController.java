package com.example.assessment.controller;


import com.example.assessment.model.*;
import com.example.assessment.repository.AssessmentRepository;
import com.example.assessment.repository.InvitedCandidateRepository;
import com.example.assessment.service.*;
import com.example.course.CourseService;
import com.example.student_exercise_attemp.model.Exercise;
import com.example.assessment.service.AssessmentService;
import com.example.assessment.service.AssessmentTypeService;
import com.example.assessment.service.StudentAssessmentAttemptService;
import com.example.assessment.service.InvitedCandidateService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.example.student_exercise_attemp.service.ExerciseService;
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
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

import java.io.ByteArrayInputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/assessments")
public class AssessmentController {
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
    private UserRepository userRepository;

    @Autowired
    private AssessmentFinalScoreService assessmentFinalScoreService;

    @Autowired
    private ProgrammingLanguageService programmingLanguageService;

    @Autowired
    private InvitedCandidateRepository invitedCandidateRepository;

    @Autowired
    private StudentAssessmentAttemptService studentAssessmentAttemptService;
    @Autowired
    private AssessmentRepository assessmentRepository;


    // code mới
    @GetMapping("/create")
    public String createAssessment(Model model) {
        Assessment assessment = new Assessment();
        assessment.setTotalScore(100);
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
        List<Question> allQuestions = questionService.findAll(); // Or get questions from quizzes you are displaying
        for (Question question : allQuestions) {
            List<AnswerOption> answerOptions = answerOptionService.getAnswerOptionByid(question.getId()); // **Potential Issue Here**
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
        if (assessmentService.duplicateAss(assessment.getTitle())) {
            List<Quiz> allQuizzes = quizService.findAll();
            model.addAttribute("allQuizzes", allQuizzes);
            // Fetch questions for each quiz and store in a Map
            Map<Long, List<Question>> quizQuestionsMap = new LinkedHashMap<>();
            for (Quiz quiz : allQuizzes) {
                List<Question> questionsForQuiz = questionService.findQuestionsByQuizId(quiz.getId());
                quizQuestionsMap.put(quiz.getId(), questionsForQuiz); // CORRECTED LINE
            }
            // Fetch answer options for ALL questions
            Map<Long, List<AnswerOption>> questionAnswerOptionsMap = new LinkedHashMap<>();
            List<Question> allQuestions = questionService.findAll();
            for (Question question : allQuestions) {
                List<AnswerOption> answerOptions = answerOptionService.getAnswerOptionByid(question.getId()); // **Potential Issue Here**
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
            model.addAttribute("duplicateTitleError", "Assessment title already exists."); // **Updated line with English message**
//        model.addAttribute("content", "assessments/create");
//        return "layout";
            return "assessments/create2";
        }
        Set<Exercise> selectedExercisesSet = new LinkedHashSet<>();
        Set<Question> selectedQuestionsSet = new LinkedHashSet<>();
        // Get the current user
        User currentUser = userService.getCurrentUser();
        assessment.setCreatedBy(currentUser);
        if (exerciseIdsStr != null && !exerciseIdsStr.isEmpty()) {
            for (String exerciseIdStr : exerciseIdsStr) { // <--- LOOPING THROUGH THE LIST
                Long exerciseId = Long.parseLong(exerciseIdStr);
                Optional<Exercise> exerciseOptional = exerciseService.getExerciseById(exerciseId);
                Exercise exercise = exerciseOptional.orElse(null); // Handle Optional and get Exercise or null
                if (exercise != null) {
                    selectedExercisesSet.add(exercise);
                }
            }
        }
        if (questionIdsStr != null && !questionIdsStr.isEmpty()) {
            for (String questionIdStr : questionIdsStr) { // <--- LOOPING THROUGH THE LIST
                Long questionId = Long.parseLong(questionIdStr);
                Optional<Question> exerciseOptional = questionService.findById(questionId);
                Question question = exerciseOptional.orElse(null); // Handle Optional and get Exercise or null
                if (question != null) {
                    selectedQuestionsSet.add(question);
                }
            }
        }
        assessment.setExercises(selectedExercisesSet);
        //SUA CHO NAY NE HIHI
        List<AssessmentQuestion> assessmentQuestions = new ArrayList<>(); // Use ArrayList to maintain order
        int orderIndex = 1; // Initialize order index

        for (Question question : selectedQuestionsSet) { // Iterate through the selected questions
            AssessmentQuestion aq = new AssessmentQuestion();
            aq.setAssessment(assessment);
            aq.setQuestion(question);
            aq.setOrderIndex(orderIndex);
            assessmentQuestions.add(aq);
            orderIndex++; // Increment for the next question
        }
        assessmentService.alignSequenceForAssessmentQuestion(); // align the sequence of the increment id
         assessment.setAssessmentQuestions(assessmentQuestions);

        assessmentService.createAssessment(assessment);
        model.addAttribute("message", "Assessment created successfully!");

        return "redirect:/assessments";
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

    //Cai nay de copy link invite
    @GetMapping("/{id}/copy_invite_link/")
    public ResponseEntity<Map<String, String>> copyInviteLink(@PathVariable("id") Long assessmentId) {
        String inviteLink = assessmentService.generateInviteLink(assessmentId); // Generate the invite link

        if (inviteLink != null) {
            Map<String, String> response = new HashMap<>();
            response.put("invite_link", inviteLink);  // Send back the invite link as JSON
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);  // Return 404 if link is not found
        }
    }


    /// / code cũ
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

    @GetMapping("/invite/{id}")
    public String inviteCandidate(@PathVariable int id, Model model) {
        List<User> usersWithRole5 = userRepository.findByRoles_Id(5L);
        System.out.println("Users found: " + usersWithRole5.size()); // Debugging print

        model.addAttribute("assessmentId", id);
        model.addAttribute("candidate", usersWithRole5);
        return "assessments/invite";
    }

    @GetMapping("/detail/{id}")
    @Transactional(readOnly = true)
    public String showDetail(@PathVariable("id") Long id, @RequestParam(value = "pageReg", defaultValue = "0") int pageReg, @RequestParam(value = "pageInv", defaultValue = "0") int pageInv, @RequestParam(value = "searchEmail", required = false) String searchEmail, Model model) {
        try {
            // Lấy assessment; nếu không tìm thấy, ném ngoại lệ
            Assessment assessment = assessmentService.findById(id).orElseThrow(() -> new Exception("Assessment not found with id: " + id));
            model.addAttribute("assessment", assessment);

            // PHÂN TRANG REGISTERED ATTEMPTS
            Pageable pageableReg = PageRequest.of(pageReg, 10);
            Page<StudentAssessmentAttempt> registeredAttemptsPage = assessmentAttemptService.findByAssessment_Id(id, pageableReg);

            // Chuyển danh sách attempt sang danh sách DTO (Map) với trường candidateUsername
            List<java.util.Map<String, Object>> attemptViewList = registeredAttemptsPage.getContent().stream().map(attempt -> {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", attempt.getId());
                map.put("email", attempt.getEmail());
                map.put("attemptDate", attempt.getAttemptDate());
                map.put("scoreQuiz", attempt.getScoreQuiz());
                map.put("scoreAss", attempt.getScoreAss());
                // Lấy username từ trường user qua reflection (do không có getter)
                String candidateUsername = "N/A";
                try {
                    java.lang.reflect.Field field = attempt.getClass().getDeclaredField("user");
                    field.setAccessible(true);
                    Object userObj = field.get(attempt);
                    if (userObj != null) {
                        // Gọi phương thức getUsername() của đối tượng user
                        candidateUsername = (String) userObj.getClass().getMethod("getUsername").invoke(userObj);
                    }
                } catch (Exception ex) {
                    // Nếu có lỗi, giữ giá trị mặc định "N/A"
                }
                map.put("candidateUsername", candidateUsername);
                return map;
            }).collect(Collectors.toList());
            model.addAttribute("registeredAttempts", attemptViewList);
            model.addAttribute("registeredAttemptsPage", registeredAttemptsPage);

            // PHÂN TRANG & TÌM KIẾM INVITED CANDIDATES
            Pageable pageableInv = PageRequest.of(pageInv, 10);
            Page<?> invitedCandidatesPage;
            if (searchEmail != null && !searchEmail.isEmpty()) {
                invitedCandidatesPage = candidateService.findByAssessmentIdAndEmailContaining(id, searchEmail, pageableInv);
            } else {
                invitedCandidatesPage = candidateService.findByAssessmentId(id, pageableInv);
            }
            model.addAttribute("invitedCandidates", invitedCandidatesPage.getContent());
            model.addAttribute("invitedCandidatesPage", invitedCandidatesPage);
            model.addAttribute("searchEmail", searchEmail);

            //SUA CHO NAY NE HIHI
            List<Question> orderedQuestions = assessment.getAssessmentQuestions()
                    .stream()
                    .map(AssessmentQuestion::getQuestion)
                    .collect(Collectors.toList());

            // Thêm Exercises & Questions
            model.addAttribute("exercises", assessment.getExercises());
            model.addAttribute("questions", orderedQuestions);
            //HET SUA ROI NHEN

            // Thêm thông tin current user
            model.addAttribute("currentUser", userService.getCurrentUser());

            // Gán template con cho layout
            model.addAttribute("content", "assessments/detail");
            return "layout";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "error"; // Phải có file error.html để hiển thị thông báo lỗi
        }
    }
//
//    @GetMapping("/invite/{id}")
//    public String inviteCandidate(@PathVariable int id, Model model) {
//        List<User> usersWithRole5 = userRepository.findByRoles_Id(5L);
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
    public ResponseEntity<InputStreamResource> exportExcel() {
        List<Assessment> assessments = assessmentService.findAll();
        ByteArrayInputStream excelFile = assessmentService.exportToExcel(assessments);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=assessments.xlsx");

        return ResponseEntity.ok().headers(headers).body(new InputStreamResource(excelFile));
    }


    @PostMapping("/import")
    public String importExcel(@RequestParam("file") MultipartFile file) {
        assessmentService.importExcel(file);
        return "redirect:/assessments";
    }

    @GetMapping("/edit/{id}")
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
    public String showAssessmentPreview(@PathVariable("id") Long id, Model model) {
        // Get Assessment
        Assessment assessment = assessmentService.getAssessmentByIdForPreview(id);
        if (assessment == null) {
            throw new RuntimeException("Assessment not found!");
        }
        // Get list question and exercise
        List<Question> questions = questionService.getQuestionsByAssessmentId(id);
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
        assessmentService.createAssessmentAttempt(id, email);

        // Fetch questions
        List<Question> questions = questionService.getQuestionsByAssessmentId(id);
        List<Exercise> exercises = exerciseService.getExercisesByAssessmentId(id);
        //Create attempt
        StudentAssessmentAttempt attempt = studentAssessmentAttemptService.createTestAttempt(id, email);
        // Add to model
        model.addAttribute("assessment", assessment);
        model.addAttribute("questions", questions);
        model.addAttribute("email", email);
        model.addAttribute("attempt", attempt);
        return "assessments/TakeAssessment";
    }

    @GetMapping("/expired-link")
    public String showExpiredPage(@RequestParam("time") String expireTime, Model model) {
        model.addAttribute("expireTime", expireTime);
        return "assessments/expiredLink"; // Return the correct view instead of redirecting
    }

    @GetMapping("/viewGrade/{assessmentId}")
    public String viewGrade(@PathVariable Long assessmentId, Model model) {

        // 1. Get Assessment
        Assessment assessment = assessmentService.findById(assessmentId).orElseThrow(() -> new RuntimeException("Assessment not found with id: " + assessmentId));

        // 2. Get current user
        User currentUser = userService.getCurrentUser();

        // 3. Get finalScore
        Optional<AssessmentFinalScore> optionalFinalScore = assessmentFinalScoreService.findByAssessmentIdAndUserId(assessmentId, currentUser.getId());

        float finalScore = 0f;
        if (optionalFinalScore.isPresent()) {
            finalScore = optionalFinalScore.get().getFinalScore();
        }

        // 4. Get list of Questions
        List<Question> questions = questionService.findQuestionsByAssessmentId(assessmentId);

        // 5. Map question -> answer options
        Map<Long, List<AnswerOption>> questionAnswerOptionsMap = new HashMap<>();
        for (Question q : questions) {
            List<AnswerOption> answerOptions = answerOptionService.getAnswerOptionByid(q.getId());
            questionAnswerOptionsMap.put(q.getId(), answerOptions);
        }

        // 6. Add data to model
        model.addAttribute("assessment", assessment);
        model.addAttribute("finalScore", finalScore);
        model.addAttribute("questions", questions);
        model.addAttribute("questionAnswerOptionsMap", questionAnswerOptionsMap);
        model.addAttribute("exercises", assessment.getExercises());

        // 7. Return view
        return "assessments/view_grade";
    }

    @GetMapping("/viewReport/{assessmentId}")
    public String viewReport(@PathVariable Long assessmentId, @RequestParam("attempt-id") Long attemptId, Model model) {

        Optional<StudentAssessmentAttempt> attempt = studentAssessmentAttemptService.findById(attemptId);

        if (attempt != null && attempt.isPresent()) {
            model.addAttribute("attemptInfo", attempt.get());
        }
        model.addAttribute("content", "assessments/view_report");
        return "layout";
    }

    //Update attempt after user submit their assessment
    @PostMapping("/{id}/submit")
    public String submitAssessment(@RequestParam("attemptId") Long attemptId,
                                   @RequestParam("elapsedTime") int elapsedTime,
                                   Model model) {
        StudentAssessmentAttempt attempt = studentAssessmentAttemptService.saveTestAttempt(attemptId, elapsedTime);
        model.addAttribute("timeTaken", elapsedTime);
        return "assessments/submitAssessment";
    }
}
