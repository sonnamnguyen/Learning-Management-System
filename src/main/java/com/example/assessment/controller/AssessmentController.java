package com.example.assessment.controller;


import com.example.assessment.model.InvitedCandidate;
import com.example.assessment.model.StudentAssessmentAttempt;
import com.example.assessment.repository.InvitedCandidateRepository;
import com.example.assessment.service.*;
import com.example.course.Course;
import com.example.course.CourseService;
import com.example.email.EmailService;
import com.example.exercise.Exercise;
import com.example.assessment.model.Assessment;
import com.example.assessment.service.AssessmentService;
import com.example.assessment.service.AssessmentTypeService;
import com.example.assessment.service.StudentAssessmentAttemptService;
import com.example.assessment.service.InvitedCandidateService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.hashids.Hashids;
import com.example.exercise.ExerciseService;
import com.example.quiz.model.AnswerOption;
import com.example.quiz.model.Question;
import com.example.quiz.model.Quiz;
import com.example.quiz.service.AnswerOptionService;
import com.example.quiz.model.Question;
import com.example.quiz.service.QuestionService;
import com.example.quiz.service.QuizService;
import com.example.user.User;
import com.example.user.UserRepository;
import com.example.user.UserService;
import com.example.exercise.Exercise;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
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
    private UserService userService;
    @Autowired
    private AnswerOptionService answerOptionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProgrammingLanguageService programmingLanguageService;

    @Autowired
    private InvitedCandidateRepository invitedCandidateRepository;

    //Hashids to hash the assessment id
    private Hashids hashids = new Hashids("BaTramBaiCodeThieuNhi", 32);
    @Autowired
    private EmailService emailService;

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
        Map<Long, List<Question>> quizQuestionsMap = new HashMap<>();
        for (Quiz quiz : allQuizzes) {
            List<Question> questionsForQuiz = questionService.findQuestionsByQuizId(quiz.getId());
            quizQuestionsMap.put(quiz.getId(), questionsForQuiz);
        }
        // Fetch answer options for ALL questions
        Map<Long, List<AnswerOption>> questionAnswerOptionsMap = new HashMap<>();
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
        List<Course> courses = courseService.getAllCourses();
        model.addAttribute("courses", courses);
        model.addAttribute("assessmentTypes", assessmentTypeService.getAllAssessmentTypes());
        model.addAttribute("currentUser", userService.getCurrentUser());
        return "assessments/create2";
    }

    @PostMapping("/create")
    public String createAssessment(@ModelAttribute Assessment assessment,
                                   @RequestParam(value = "exercises-ids", required = false) List<String> exerciseIdsStr,
                                   @RequestParam(value = "questions-ids", required = false) List<String> questionIdsStr,
                                   Model model) {
        if (assessmentService.duplicateAss(assessment.getTitle())) {
            List<Course> courses = courseService.getAllCourses();
            model.addAttribute("courses", courses);
            model.addAttribute("assessmentTypes", assessmentTypeService.getAllAssessmentTypes());
            model.addAttribute("languages", programmingLanguageService.getAllProgrammingLanguages());
            model.addAttribute("duplicateAss", "this assessment name has exited!!");
            model.addAttribute("currentUser", userService.getCurrentUser());
            model.addAttribute("assessment", assessment);
            model.addAttribute("currentUser", userService.getCurrentUser());
            return "assessments/create2";
        }

        Set<Exercise> selectedExercisesSet = new HashSet<>();
        Set<Question> selectedQuestionsSet = new HashSet<>();
        // Get the current user
        User currentUser = userService.getCurrentUser();
        assessment.setCreatedBy(currentUser);
        if (exerciseIdsStr != null && !exerciseIdsStr.isEmpty()) {
            for (String exerciseIdStr : exerciseIdsStr) { // <--- LOOPING THROUGH THE LIST
                Long exerciseId = Long.parseLong(exerciseIdStr); // <--- **CORRECTED LINE: CONVERT STRING TO LONG**
                Optional<Exercise> exerciseOptional = exerciseService.getExerciseById(exerciseId);
                Exercise exercise = exerciseOptional.orElse(null); // Handle Optional and get Exercise or null
                if (exercise != null) {
                    selectedExercisesSet.add(exercise);
                }
            }
        }
        if (questionIdsStr != null && !questionIdsStr.isEmpty()) {
            for (String questionIdStr : questionIdsStr) { // <--- LOOPING THROUGH THE LIST
                Long questionId = Long.parseLong(questionIdStr); // <--- **CORRECTED LINE: CONVERT STRING TO LONG**
                Optional<Question> exerciseOptional = questionService.findById(questionId);
                Question question = exerciseOptional.orElse(null); // Handle Optional and get Exercise or null
                if (question != null) {
                    selectedQuestionsSet.add(question);
                }
            }
        }
        assessment.setExercises(selectedExercisesSet);
        assessment.setQuestions(selectedQuestionsSet);// <--- SETTING THE ENTIRE SET AFTER THE LOOP
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
    /// / code cũ
    @ModelAttribute
    public void addCommonAttributes(Model model) {
        model.addAttribute("title", "Assessments");
        model.addAttribute("links", "/style.css");
    }

    @GetMapping
    public String list(Model model,
                       @RequestParam(value = "searchQuery", required = false) String searchQuery,
                       @RequestParam(value = "page", defaultValue = "0") int page,
                       @RequestParam(value = "pageSize", defaultValue = "12") int pageSize) {
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
//
//    @GetMapping("/create")
//    public String showCreateForm(Model model) {
//        model.addAttribute("assessment", new Assessment());
//        model.addAttribute("courses", courseService.getAllCourses());
//        model.addAttribute("exercises", exerciseService.findAllExercises());
//        model.addAttribute("questions", questionService.findAll());
//        model.addAttribute("assessmentTypes", assessmentTypeService.getAllAssessmentTypes());
//        // Add all users to the model for selection in the form
//        model.addAttribute("users", userService.getAllUsers());
//        model.addAttribute("currentUser", userService.getCurrentUser());
//        model.addAttribute("content", "assessments/create");
//        return "layout";
//    }
//
//
//    @PostMapping("/create")
//    public String create(@ModelAttribute Assessment assessment) {
//        assessmentService.save(assessment);
//        return "redirect:/assessments";
//    }
//
//
//    @GetMapping("/edit/{id}")
//    public String showEditForm(@PathVariable("id") Long id, Model model) {
//        // Retrieve the assessment by its ID
//        Assessment assessment = assessmentService.findById(id).orElse(null);
//
//        // If the assessment is not found, redirect to assessments or handle the error
//        if (assessment == null) {
//            return "redirect:/assessments";
//        }
//
//        // Add the assessment to the model
//        model.addAttribute("assessment", assessment);
//
//        // Add quizzes, questions, and related data
//        model.addAttribute("allQuizzes", quizService.findAll());
//        model.addAttribute("questions", questionService.findAll());
//        model.addAttribute("selectedQuiz", quizService.findById(id));
//        model.addAttribute("totalQuestionsSelectedQuiz", assessment.getQuestions().size());
//
//        // Add selected exercises
//        List<Long> selectedExerciseIds = assessment.getExercises()
//                .stream()
//                .map(Exercise::getId)
//                .collect(Collectors.toList());
//        model.addAttribute("selectedExercises", selectedExerciseIds);
//
//        // Add other attributes
//        model.addAttribute("exercises", exerciseService.findAllExercises());
//
//        // Add other necessary attributes
//        model.addAttribute("courses", courseService.getAllCourses());
//        model.addAttribute("assessmentTypes", assessmentTypeService.getAllAssessmentTypes());
//        model.addAttribute("users", userService.getAllUsers());
//        model.addAttribute("currentUser", userService.getCurrentUser());
//
//        // Set the content for the edit view
//        model.addAttribute("content", "assessments/edit");
//
//        return "layout";
//    }

    @GetMapping("/invite/{id}")
    public String inviteCandidate(@PathVariable int id, Model model) {
        List<User> usersWithRole5 = userRepository.findByRoles_Id(2L);
        System.out.println("Users found: " + usersWithRole5.size()); // Debugging print

        model.addAttribute("assessmentId", id);
        model.addAttribute("candidate", usersWithRole5);
        return "assessments/invite";
    }

    @GetMapping("/detail/{id}")
    @Transactional(readOnly = true)
    public String showDetail(@PathVariable("id") Long id,
                             @RequestParam(value = "pageReg", defaultValue = "0") int pageReg,
                             @RequestParam(value = "pageInv", defaultValue = "0") int pageInv,
                             @RequestParam(value = "searchEmail", required = false) String searchEmail,
                             Model model) {
        try {
            // Lấy assessment; nếu không tìm thấy, ném ngoại lệ
            Assessment assessment = assessmentService.findById(id)
                    .orElseThrow(() -> new Exception("Assessment not found with id: " + id));
            model.addAttribute("assessment", assessment);

            // PHÂN TRANG REGISTERED ATTEMPTS
            Pageable pageableReg = PageRequest.of(pageReg, 10);
            Page<StudentAssessmentAttempt> registeredAttemptsPage =
                    assessmentAttemptService.findByAssessment_Id(id, pageableReg);

            // Chuyển danh sách attempt sang danh sách DTO (Map) với trường candidateUsername
            List<java.util.Map<String, Object>> attemptViewList = registeredAttemptsPage.getContent().stream()
                    .map(attempt -> {
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
                    })
                    .collect(Collectors.toList());
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

            // Thêm Exercises & Questions
            model.addAttribute("exercises", assessment.getExercises());
            model.addAttribute("questions", assessment.getQuestions());

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
    public ResponseEntity<InputStreamResource> exportExcel() {
        List<Assessment> assessments = assessmentService.findAll();
        ByteArrayInputStream excelFile = assessmentService.exportToExcel(assessments);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=assessments.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .body(new InputStreamResource(excelFile));
    }


    @PostMapping("/import")
    public String importExcel(@RequestParam("file") MultipartFile file) {
        assessmentService.importExcel(file);
        return "redirect:/assessments";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        // Retrieve the assessment by its ID
        Assessment assessment = assessmentService.findById(id).orElse(null);

        // If the assessment is not found, redirect to assessments or handle the error
        if (assessment == null) {
            return "redirect:/assessments";
        }

        // Add the assessment to the model
        model.addAttribute("assessment", assessment);

        // Fetch all quizzes
        List<Quiz> allQuizzes = quizService.findAll();
        model.addAttribute("allQuizzes", allQuizzes);

        // Fetch questions for each quiz and store in a Map
        Map<Long, List<Question>> quizQuestionsMap = new HashMap<>();
        for (Quiz quiz : allQuizzes) {
            List<Question> questionsForQuiz = questionService.findQuestionsByQuizId(quiz.getId());
            quizQuestionsMap.put(quiz.getId(), questionsForQuiz);
        }

        // Fetch answer options for ALL questions
        Map<Long, List<AnswerOption>> questionAnswerOptionsMap = new HashMap<>();
        List<Question> allQuestions = questionService.findAll();
        for (Question question : allQuestions) {
            List<AnswerOption> answerOptions = answerOptionService.getAnswerOptionByid(question.getId());
            questionAnswerOptionsMap.put(question.getId(), answerOptions);
        }

        // Fetch Selected Questions
        Set<Question> selectedQuestions = assessment.getQuestions();
        List<Long> selectedQuestionIds = selectedQuestions.stream()
                .map(Question::getId)
                .collect(Collectors.toList());

        model.addAttribute("selectedQuestions", selectedQuestionIds);
        model.addAttribute("quizQuestionsMap", quizQuestionsMap);
        model.addAttribute("questionAnswerOptionsMap", questionAnswerOptionsMap);
        model.addAttribute("questions", allQuestions);

        // **Fetch Selected Exercises**
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
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("currentUser", userService.getCurrentUser());

        // Set the content for the edit view
        model.addAttribute("content", "assessments/edit");

        return "layout";
    }

    //Preview assessment
    @GetMapping("/{id}/preview")
    public String showAssessmentPreview(@PathVariable("id") Long id, Model model) {
        System.out.println("Controller method called for assessment ID: " + id);
        // Get Assessment
        Assessment assessment = assessmentService.getAssessmentByIdForPreview(id);
        if (assessment == null) {
            throw new RuntimeException("Assessment not found!");
        }
        // Get list question and exercise
        List<Question> questions = questionService.getQuestionsByAssessmentId(id);
//        List<Exercise> exercises = exerciseService.getExercisesByAssessmentId(id);
        //Shuffle list
        Collections.shuffle(questions);
//        Collections.shuffle(exercises);
        // Add to model
        model.addAttribute("assessment", assessment);
        model.addAttribute("questions", questions);
//        model.addAttribute("exercises", exercises);
        return "assessments/AssessmentPreview";
    }

    @GetMapping("/invite/{id}/verify-email")
    public String showEmailSubmissionPage(@PathVariable("id") String id, Model model) {
        // Decode the ID
        System.out.println("Stored assessmentId in model: " + id);

        model.addAttribute("assessmentId", id);
        System.out.println("Return to take exam with id: "+id);
        return "assessments/verifyEmail"; // Show email input page
    }
    @PostMapping("/invite/{id}/take-exam")
    public String verifyEmail(@PathVariable("id") String rawId, @RequestParam("email") String email, Model model) {
        email = email.toLowerCase();

        System.out.println("Take exam get id: "+ rawId);
        long id;

        try {
            // Decode the ID
            long[] temp = assessmentService.decodeId(rawId);
            if (temp.length == 0) {
                throw new IllegalArgumentException("Invalid assessment ID!");
            }
            id = temp[0];
            System.out.println("Dit con me may: "+ rawId +" dasdasd: "+id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decode assessment ID: " + rawId, e);
        }

        // Store the hashed ID (rawId) in the model instead of the decoded id
        model.addAttribute("assessmentId", rawId);

        // Check expiration date for the specific email

        Optional<LocalDateTime> expireDateOpt = invitedCandidateRepository.findExpireDateByAssessmentIdAndEmail(id, email);
        if (expireDateOpt.isEmpty()) {
            return "redirect:/invalid-link"; // Redirect if no record found
        }

// Get current time in UTC
        LocalDateTime nowUtc = LocalDateTime.now();
        ZoneId gmt7 = ZoneId.of("Asia/Bangkok");

// Treat stored time as GMT+7 (avoid incorrect conversion)
        ZonedDateTime expireDateGmt7 = expireDateOpt.get().atZone(gmt7);
        ZonedDateTime nowGmt7 = nowUtc.atZone(ZoneId.of("UTC")).withZoneSameInstant(gmt7);

        System.out.println("Expire: " + expireDateGmt7.toLocalDateTime() + " Current: "+ nowGmt7.toLocalDateTime());

        if (expireDateGmt7.toLocalDateTime().isBefore(nowGmt7.toLocalDateTime())) {
            String formattedExpireTime = expireDateGmt7.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return "redirect:/assessments/expired-link?time=" + formattedExpireTime;
        }

        // Get Assessment
        Assessment assessment = assessmentService.getAssessmentByIdForPreview(id);
        if (assessment == null) {
            throw new RuntimeException("Assessment not found!");
        }

        // Get list of questions and exercises
        List<Question> questions = questionService.getQuestionsByAssessmentId(id);
//        List<Exercise> exercises = exerciseService.getExercisesByAssessmentId(id);
        //Shuffle list
        Collections.shuffle(questions);
//        Collections.shuffle(exercises);
        // Add to model
        model.addAttribute("assessment", assessment);
        model.addAttribute("questions", questions);
//        model.addAttribute("exercises", exercises);
        model.addAttribute("email", email);

        return "assessments/TakeAssessment";
    }

    @GetMapping("/expired-link")
    public String showExpiredPage(@RequestParam("time") String expireTime, Model model) {
        model.addAttribute("expireTime", expireTime);
        return "assessments/expiredLink"; // Return the correct view instead of redirecting
    }

}