package com.example.assessment.service;

import com.example.assessment.model.Assessment;
import com.example.assessment.model.AssessmentQuestion;
import com.example.assessment.repository.AssessmentQuestionRepository;
import com.example.quiz.model.Question;
import com.example.quiz.service.QuestionService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AssessmentQuestionService {
    @Autowired
    private AssessmentQuestionRepository assessmentQuestionRepository;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private AssessmentService assessmentService;

    /**
     * Updates the questions associated with a specific assessment.
     *
     * Steps:
     * 1. Validate the input list of questions.
     * 2. Retrieve the assessment by its ID; throw an error if not found.
     * 3. Remove all existing questions linked to this assessment.
     * 4. Iterate through the provided question list:
     *    - Validate each question ID.
     *    - Create new AssessmentQuestion entries.
     * 5. Save the updated list of questions to the database.
     *
     * @param assessmentId   The ID of the assessment to update.
     * @param questionOrders List of questions with their order indices.
     */
    @Transactional
    public void updateAssessmentQuestions(Long assessmentId, List<QuestionOrder> questionOrders) {
        System.out.println("Updating questions for assessment ID: " + assessmentId);

        // Retrieve assessment
        Assessment assessment = assessmentService.findById(assessmentId)
                .orElseThrow(() -> new RuntimeException("Assessment not found with ID: " + assessmentId));

        // Delete existing assessment questions
        System.out.println("Removing existing questions for assessment ID: " + assessmentId);
        int deletedCount = assessmentQuestionRepository.deleteByAssessmentId(assessmentId);
        assessmentQuestionRepository.flush();
        System.out.println("Removed " + deletedCount + " old questions.");

        if (questionOrders == null || questionOrders.isEmpty()) {
            System.out.println("No new questions provided.");
            return;
        }

        // Prepare new assessment questions
        List<AssessmentQuestion> newAssessmentQuestions = new ArrayList<>();
        for (QuestionOrder qo : questionOrders) {
            try {
                Question question = questionService.findById(qo.questionId())
                        .orElseThrow(() -> new RuntimeException("Question not found with ID: " + qo.questionId()));

                newAssessmentQuestions.add(new AssessmentQuestion(null, assessment, question, qo.orderIndex()));
                System.out.println("Added Question ID: " + qo.questionId() + ", Order: " + qo.orderIndex());
            } catch (Exception e) {
                System.out.println("Error creating AssessmentQuestion: " + e.getMessage());
            }
        }

        // Save to database
        if (!newAssessmentQuestions.isEmpty()) {
            assessmentQuestionRepository.saveAll(newAssessmentQuestions);
            assessmentQuestionRepository.flush();  // Ensure immediate save to DB
            System.out.println("Saved " + newAssessmentQuestions.size() + " questions to assessment.");
        } else {
            System.out.println("No valid questions to save.");
        }
    }

    /**
     * Retrieves all questions linked to a given assessment.
     *
     * @param assessmentId The ID of the assessment.
     * @return A list of AssessmentQuestion entities associated with the assessment.
     */
    public List<AssessmentQuestion> findByAssessmentId(Long assessmentId) {
        return assessmentQuestionRepository.findByAssessmentId(assessmentId);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record QuestionOrder(Long questionId, Integer orderIndex) {}
}
