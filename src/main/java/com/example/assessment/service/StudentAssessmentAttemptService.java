package com.example.assessment.service;

import com.example.assessment.model.Assessment;
import com.example.assessment.model.StudentAssessmentAttempt;
import com.example.assessment.repository.AssessmentRepository;
import com.example.assessment.repository.StudentAssessmentAttemptRepository;
import com.example.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class StudentAssessmentAttemptService {

    @Autowired
    private StudentAssessmentAttemptRepository repository;
    @Autowired
    private AssessmentRepository assessmentRepository;

    @Autowired
    private UserRepository userRepository;

    public StudentAssessmentAttempt save(StudentAssessmentAttempt attempt) {
        return repository.save(attempt);
    }

    public Optional<StudentAssessmentAttempt> findById(Long id) {
        return repository.findById(id);
    }

    public List<StudentAssessmentAttempt> findByAssessment_Id(Long assessmentId) {
        return repository.findByAssessment_Id(assessmentId);
    }

    public Page<StudentAssessmentAttempt> findByAssessment_Id(Long assessmentId, Pageable pageable) {
        return repository.findByAssessment_Id(assessmentId, pageable);
    }

    public List<StudentAssessmentAttempt> findByUserId(Long userId) {
        return repository.findByUserId(userId);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    public List<StudentAssessmentAttempt> findAll() {
        return repository.findAll();
    }

    public StudentAssessmentAttempt createAssessmentAttempt(Long assessmentId, String mail) {
        StudentAssessmentAttempt testAttempt = new StudentAssessmentAttempt();
        Assessment assessment = assessmentRepository.findById(assessmentId).orElseThrow(()
                -> new RuntimeException("Assessment not found!"));
        // sample
        testAttempt.setAssessment(assessment);
        testAttempt.setEmail(mail);
        testAttempt.setDuration(0);
        testAttempt.setScoreQuiz(0);
        testAttempt.setScoreEx(0);
        testAttempt.setScoreAss(0);
        testAttempt.setNote("");
        testAttempt.setSubmitted(false);
        testAttempt.setProctored(true);
        testAttempt.setProctoringData(null);
        testAttempt.setAttemptDate(LocalDateTime.now());
        //Update number of attend in the assessment
        StudentAssessmentAttempt savedAttempt = repository.save(testAttempt);
        userRepository.findByEmail(mail).ifPresent(testAttempt::setUser);
        assessmentRepository.save(assessment);
        return savedAttempt;
    }


    public StudentAssessmentAttempt saveTestAttempt(Long attemptId, int timeTaken, int score, int scoreEx) {
        StudentAssessmentAttempt testAttempt = repository.findById(attemptId).orElseThrow(()
                -> new RuntimeException("Attempt not found!"));
        // sample; check if the assessment has reached the qualify or not here later
        testAttempt.setDuration(timeTaken);
        testAttempt.setScoreQuiz(score);
        testAttempt.setScoreEx(scoreEx);
        testAttempt.setSubmitted(true);
        testAttempt.setProctoringData(null);
        return repository.save(testAttempt);
    }
}
