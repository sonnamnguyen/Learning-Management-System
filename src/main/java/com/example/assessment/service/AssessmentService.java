package com.example.assessment.service;

import com.example.assessment.model.Assessment;
import com.example.assessment.model.AssessmentQuestion;
import com.example.assessment.model.StudentAssessmentAttempt;
import com.example.assessment.repository.AssessmentRepository;
import com.example.assessment.repository.StudentAssessmentAttemptRepository;
import com.example.course.Course;
import com.example.course.CourseService;
import com.example.student_exercise_attemp.model.Exercise;
import com.example.student_exercise_attemp.repository.ExerciseRepository;
import com.example.student_exercise_attemp.service.ExerciseService;
import com.example.user.User;
import com.example.user.UserRepository;
import com.example.user.UserService;
import com.google.gson.Gson;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hashids.Hashids;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.example.utils.Helper.getCellValueAsString;

@Service
public class AssessmentService {

    @Autowired
    private AssessmentRepository assessmentRepository;
    @Autowired
    private ExerciseRepository exerciseRepository;
    @Autowired
    private UserService userService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StudentAssessmentAttemptRepository attemptRepository;

    @Autowired
    private CourseService courseService;

    @Autowired
    private final Gson gson = new Gson();

    //Hashids to hash the assessment id
    private Hashids hashids = new Hashids("BaTramBaiCodeThieuNhi", 32);
    @Autowired
    private UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void alignSequence() {
        Object maxId = entityManager.createQuery("SELECT MAX(i.id) FROM InvitedCandidate i").getSingleResult();
        if (maxId != null) {
            entityManager.createNativeQuery("SELECT setval('assessment_id_seq', :newValue, true)")
                    .setParameter("newValue", ((Number) maxId).longValue())
                    .getSingleResult();
        }
    }
    @Transactional
    public void alignSequenceForAssessmentQuestion() {
        Object maxId = entityManager.createQuery("SELECT MAX(aq.id) FROM AssessmentQuestion aq").getSingleResult();
        if (maxId != null) {
            entityManager.createNativeQuery("SELECT setval('assessment_question_id_seq', :newValue, true)")
                    .setParameter("newValue", ((Number) maxId).longValue())
                    .getSingleResult();
        }
    }
    public Assessment createAssessment(Assessment assessment) {

        return assessmentRepository.save(assessment);
    }

    public boolean existsByTitleAndAssessmentTypeId(String title, Long assessmentTypeId) {
        return assessmentRepository.existsByTitleAndAssessmentTypeId(title, assessmentTypeId);
    }

    public boolean duplicateAss(String name) {
        return assessmentRepository.existsByName(name);
    }

    private final String SECRET_KEY = "BaTramBaiCodeThieuNhi";

    // private final String inviteUrlHeader = "https://group-02.cookie-candy.id.vn/assessments/invite/";
    private final String inviteUrlHeader = "http://localhost:9091/assessments/invite/";

    //  private final String inviteUrlHeader = "https://java02.fsa.io.vn/assessments/invite/";
    public Optional<Assessment> findById(Long id) {
        return assessmentRepository.findById(id);
    }

    @Transactional
    public Assessment save(Assessment assessment) {
        return assessmentRepository.save(assessment);
    }

    public void deleteById(Long id) {
        assessmentRepository.deleteById(id);
    }

    public Page<Assessment> findAll(Pageable pageable) {
        return assessmentRepository.findAll(pageable);
    }

    public List<Assessment> findAll() {
        return assessmentRepository.findAll();
    }

    public Page<Assessment> search(String searchQuery, Pageable pageable) {
        return assessmentRepository.search(searchQuery, pageable);
    }

    public boolean existsByTitle(String title) {
        return assessmentRepository.existsByTitle(title);
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userService.findByUsername(username);
    }

    public void importExcel(MultipartFile file) {
        User currentUser = getCurrentUser();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int rowCount = sheet.getPhysicalNumberOfRows();
            List<Assessment> assessments = new ArrayList<>();

            for (int i = 1; i < rowCount; i++) { // Start from 1 to skip the header row
                Row row = sheet.getRow(i);
                if (row != null) {
                    // Extract cells
                    Cell titleCell = row.getCell(0);
                    Cell descriptionCell = row.getCell(1);
                    Cell courseCell = row.getCell(2);
                    Cell createdAtCell = row.getCell(3);

                    if (titleCell != null) {
                        String title = getCellValueAsString(titleCell).trim();
                        String description = descriptionCell != null ? getCellValueAsString(descriptionCell).trim() : null;

                        Course course = null;
                        if (courseCell != null) {
                            String courseName = getCellValueAsString(courseCell).trim();
                            course = courseService.findByName(courseName);
                        }

                        LocalDateTime createdAt = createdAtCell != null
                                ? LocalDateTime.parse(getCellValueAsString(createdAtCell).trim())
                                : LocalDateTime.now();

                        if (!assessmentRepository.existsByTitle(title)) {
                            Assessment assessment = new Assessment();
                            assessment.setTitle(title);
                            assessment.setCourse(course);
                            assessment.setCreatedAt(createdAt);
                            assessment.setCreatedBy(currentUser);
                            assessments.add(assessment);
                        }
                    }
                }
            }

            for (Assessment assessment : assessments) {
                assessmentRepository.save(assessment);
            }

        } catch (IOException e) {
            throw new RuntimeException("Error importing assessments from Excel", e);
        }
    }

    public ByteArrayInputStream exportToExcel(List<Assessment> assessments) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Assessments");

        // Header row
        String[] headers = {"ID", "Title", "Description", "Course", "Created At", "Created By"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        // Data rows
        int rowNum = 1;
        for (Assessment assessment : assessments) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(assessment.getId());
            row.createCell(1).setCellValue(assessment.getTitle());
            row.createCell(3).setCellValue(assessment.getCourse() != null ? assessment.getCourse().getName() : "");
            row.createCell(4).setCellValue(assessment.getCreatedAt().toString());
            row.createCell(5).setCellValue(assessment.getCreatedBy() != null ? assessment.getCreatedBy().getUsername() : "");
        }

        // Write to output stream
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            workbook.write(out);
            workbook.close();
        } catch (IOException e) {
            throw new RuntimeException("Error exporting assessments to Excel", e);
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    public void increaseInvitedCount(long assessmentId, int count) {
        Optional<Assessment> assessmentOpt = assessmentRepository.findById(assessmentId);
        if (assessmentOpt.isPresent()) {
            Assessment assessment = assessmentOpt.get();
            assessment.setInvitedCount(assessment.getInvitedCount() + count); // Increase by batch size
            assessmentRepository.save(assessment);
        }
    }

    /**
     * Stores or updates the invited emails in the PostgreSQL LOB column and inserts into the invited_candidate table.
     * This method appends new emails to the existing ones instead of overwriting.
     *
     * @param assessmentId The ID of the assessment.
     * @param newEmails    List of new emails to be added.
     */
    public void storeInvitedEmail(long assessmentId, List<String> newEmails, LocalDateTime invitationDate, LocalDateTime expirationDate) {
        List<String> existingEmails = getInvitedEmails(assessmentId);
        Set<String> updatedEmails = new HashSet<>(existingEmails);
        updatedEmails.addAll(newEmails);

        ZoneId vietnamZone = ZoneId.of("Asia/Ho_Chi_Minh");

        // Convert LocalDateTime to ZonedDateTime (GMT+7)
        ZonedDateTime invitationZoned = invitationDate.atZone(ZoneId.systemDefault()).withZoneSameInstant(vietnamZone);
        ZonedDateTime expirationZoned = expirationDate.atZone(ZoneId.systemDefault());

        // Convert ZonedDateTime to Timestamp
        Timestamp invitationTimestamp = Timestamp.valueOf(invitationZoned.toLocalDateTime());
        Timestamp expirationTimestamp = Timestamp.valueOf(expirationZoned.toLocalDateTime());

        for (String email : newEmails) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM invited_candidate WHERE email = ? AND assessment_id = ?",
                    Integer.class, email, assessmentId
            );

            if (count != null && count > 0) {
                jdbcTemplate.update(
                        "UPDATE invited_candidate SET invitation_date = ?, expiration_date = ? WHERE email = ? AND assessment_id = ?",
                        invitationTimestamp, expirationTimestamp, email, assessmentId
                );
            } else {
                jdbcTemplate.update(
                        "INSERT INTO invited_candidate (email, invitation_date, expiration_date, assessment_id) VALUES (?, ?, ?, ?)",
                        email, invitationTimestamp, expirationTimestamp, assessmentId
                );
            }
        }
    }


    /**
     * Retrieves the list of invited emails stored in PostgreSQL LOB column.
     *
     * @param assessmentId The ID of the assessment.
     * @return List of invited email addresses.
     */
    public List<String> getInvitedEmails(long assessmentId) {
        // Step 1: Retrieve text data from the database
        String emailsText = jdbcTemplate.queryForObject(
                "SELECT invited_emails FROM assessment WHERE id = ?",
                String.class, assessmentId
        );

        // Step 2: Convert comma-separated string back to a list
        if (emailsText == null || emailsText.isBlank()) {
            return List.of();
        }
        return List.of(emailsText.split(","));
    }

    //Encode the assessment id into some hashed code to send email for invitation

    public String encodeId(long id) {
        return hashids.encode(id);
    }

    public long[] decodeId(String hash) {
        return hashids.decode(hash);
    }

    public String generateInviteLink(long id) {
        return inviteUrlHeader + encodeId(id) + "/take";
    }

    public Assessment getAssessmentByIdForPreview(Long id) {
        return assessmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Assessment not found!"));
    }

    public List<Exercise> getExercisesByAssessmentId(Long assessmentId) {
        return exerciseRepository.findExercisesByAssessmentId(assessmentId);
    }

    public void createAssessmentAttempt(Long assessmentId, String email) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assessment not found"));

        StudentAssessmentAttempt attempt = new StudentAssessmentAttempt();
        attempt.setAttemptDate(LocalDateTime.now());
        attempt.setDuration(0);
        attempt.setEmail(email);
        attempt.setProctored(false);
        attempt.setSubmitted(false);
        attempt.setNote(null);
        attempt.setProctoringData(null);
        attempt.setScoreAss(0);
        attempt.setScoreQuiz(0);
        attempt.setAssessment(assessment);
        userRepository.findByEmail(email).ifPresent(attempt::setUser);

        attemptRepository.save(attempt);
    }

    public long countAttemptsByAssessmentId(Long assessmentId) {
        return attemptRepository.countByAssessmentId(assessmentId);
    }


    @Transactional // Add transactional annotation
    public Assessment duplicateAssessment(Long assessmentId) {
        Assessment originalAssessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new RuntimeException("Assessment not found with id: " + assessmentId));

        Assessment duplicatedAssessment = new Assessment();

        // Copy basic properties
        duplicatedAssessment.setCourse(originalAssessment.getCourse());
        String originalTitle = originalAssessment.getTitle();
        String baseTitle = originalTitle;
        int copyNumber = 1;

        // Regex to extract base title and existing copy number (if any)
        Pattern pattern = Pattern.compile("\\(copy (\\d+)\\) (.+)");
        Matcher matcher = pattern.matcher(originalTitle);

        if (matcher.matches()) {
            baseTitle = matcher.group(2).trim(); // Extract base title (group 2)
        } else {
            baseTitle = originalTitle; // Original title is the base title
        }


        // Find existing copies based on the BASE title
        List<Assessment> existingCopies = assessmentRepository.findByTitleContaining(baseTitle);
        int maxCopyNumber = 0;


        for (Assessment existingCopy : existingCopies) {
            Matcher existingMatcher = pattern.matcher(existingCopy.getTitle());
            if (existingMatcher.matches()) {
                String group1 = existingMatcher.group(1);
                String existingBaseTitle = existingMatcher.group(2).trim(); // Extract base title from existing copy

                try {
                    int existingCopyNum = Integer.parseInt(group1);
                    if (existingBaseTitle.equals(baseTitle)) { // Check if base titles match
                        maxCopyNumber = Math.max(maxCopyNumber, existingCopyNum);
                    }
                } catch (NumberFormatException e) {
                    // Ignore if the copy number is not a valid integer
                }
            } else if (existingCopy.getTitle().equals(baseTitle)) { // Check for original base title
                maxCopyNumber = Math.max(maxCopyNumber, 0);
            }
        }
        copyNumber = maxCopyNumber + 1;
        duplicatedAssessment.setTitle("(copy " + copyNumber + ") " + baseTitle); // Set title with new copy number and BASE title


        duplicatedAssessment.setAssessmentType(originalAssessment.getAssessmentType());
        duplicatedAssessment.setTimeLimit(originalAssessment.getTimeLimit());
        duplicatedAssessment.setQualifyScore(originalAssessment.getQualifyScore());
        duplicatedAssessment.setQuizScoreRatio(originalAssessment.getQuizScoreRatio());
        duplicatedAssessment.setExerciseScoreRatio(originalAssessment.getExerciseScoreRatio());
        duplicatedAssessment.setShuffled(originalAssessment.isShuffled());
        duplicatedAssessment.setInvitedEmails(originalAssessment.getInvitedEmails());
        duplicatedAssessment.setCreatedBy(userService.getCurrentUser());
        duplicatedAssessment.setCreatedAt(LocalDateTime.now());
        duplicatedAssessment.setInvitedCount(0);
        duplicatedAssessment.setAssessedCount(0);
        duplicatedAssessment.setQualifiedCount(0);


        // Copy Exercises and AssessmentQuestions (same as before)
        Set<Exercise> duplicatedExercises = new HashSet<>(originalAssessment.getExercises());
        duplicatedAssessment.setExercises(duplicatedExercises);
        List<AssessmentQuestion> duplicatedAssessmentQuestions = new ArrayList<>();
        for (AssessmentQuestion originalAq : originalAssessment.getAssessmentQuestions()) {
            AssessmentQuestion duplicatedAq = new AssessmentQuestion();
            duplicatedAq.setAssessment(duplicatedAssessment);
            duplicatedAq.setQuestion(originalAq.getQuestion());
            duplicatedAq.setOrderIndex(originalAq.getOrderIndex());
            duplicatedAssessmentQuestions.add(duplicatedAq);
        }
        duplicatedAssessment.setAssessmentQuestions(duplicatedAssessmentQuestions);

        return assessmentRepository.save(duplicatedAssessment);
    }
}
