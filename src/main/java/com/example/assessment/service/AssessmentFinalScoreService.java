package com.example.assessment.service;

import com.example.assessment.model.AssessmentFinalScore;
import com.example.assessment.repository.AssessmentFinalScoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AssessmentFinalScoreService {

    private final AssessmentFinalScoreRepository assessmentFinalScoreRepository;

    @Autowired
    public AssessmentFinalScoreService(AssessmentFinalScoreRepository assessmentFinalScoreRepository) {
        this.assessmentFinalScoreRepository = assessmentFinalScoreRepository;
    }

    public AssessmentFinalScore save(AssessmentFinalScore finalScore) {
        return assessmentFinalScoreRepository.save(finalScore);
    }

    public Optional<AssessmentFinalScore> findById(Long id) {
        return assessmentFinalScoreRepository.findById(id);
    }

    public void deleteById(Long id) {
        assessmentFinalScoreRepository.deleteById(id);
    }

    public List<AssessmentFinalScore> findByAssessmentId(Long assessmentId) {
        return assessmentFinalScoreRepository.findByAssessmentId(assessmentId);
    }

    public List<AssessmentFinalScore> findByUserId(Long userId) {
        return assessmentFinalScoreRepository.findByUserId(userId);
    }

    public Optional<AssessmentFinalScore> findByAssessmentIdAndUserId(Long assessmentId, Long userId) {
        // Gọi đúng method trong repository (phải có method này)
        return assessmentFinalScoreRepository.findByAssessmentIdAndUserId(assessmentId, userId);
    }

    public void updateFinalScore(Long id, float newScoreAss, float newScoreQuiz, float weightAss, float weightQuiz) {
        Optional<AssessmentFinalScore> optional = assessmentFinalScoreRepository.findById(id);
        if (optional.isPresent()) {
            AssessmentFinalScore finalScore = optional.get();
            finalScore.setFinalScoreAss(newScoreAss);
            finalScore.setFinalScoreQuiz(newScoreQuiz);
            float total = newScoreAss * weightAss + newScoreQuiz * weightQuiz;
            finalScore.setFinalScore(total);
            assessmentFinalScoreRepository.save(finalScore);
        }
    }
}

