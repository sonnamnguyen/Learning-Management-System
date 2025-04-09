package com.example.assessment.service;

import com.example.assessment.model.Assessment;
import com.example.assessment.model.ScoreEditHistory;
import com.example.assessment.model.StudentAssessmentAttempt;
import com.example.assessment.repository.AssessmentRepository;
import com.example.assessment.repository.ScoreEditHistoryRepository;
import com.example.assessment.repository.StudentAssessmentAttemptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;


@Service
public class AssessmentScoreService {

    @Autowired
    private StudentAssessmentAttemptRepository attemptRepository;

    @Autowired
    private ScoreEditHistoryRepository scoreEditHistoryRepository;

    @Autowired
    private AssessmentRepository assessmentRepository;

    public Map<String, Object> editScore(Long attemptId, Map<String, Object> scoreData) {
        // Kiểm tra xem attemptId có tồn tại không
        StudentAssessmentAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found with id: " + attemptId));

        // Lấy assessment để tính toán điểm mới
        Assessment assessment = assessmentRepository.findById(attempt.getAssessment().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));

        // Lưu điểm cũ
        Integer oldScoreEx = attempt.getScoreEx();
        Integer oldScoreQuiz = attempt.getScoreQuiz();
        Integer oldScoreAss = attempt.getScoreAss();

        // Lấy dữ liệu từ request
        Integer newScoreEx = scoreData.get("newScoreEx") != null ?
                Integer.valueOf(scoreData.get("newScoreEx").toString()) : null;
        Integer newScoreQuiz = scoreData.get("newScoreQuiz") != null ?
                Integer.valueOf(scoreData.get("newScoreQuiz").toString()) : null;
        String comment = (String) scoreData.get("comment");

        // Kiểm tra điểm mới phải >= 0
        if ((newScoreEx != null && newScoreEx < 0) ||
                (newScoreQuiz != null && newScoreQuiz < 0)) {
            throw new IllegalArgumentException("Điểm không được nhỏ hơn 0");
        }

        // Kiểm tra comment bắt buộc phải có
        if (comment == null || comment.trim().isEmpty()) {
            throw new IllegalArgumentException("Comment không được để trống");
        }

        // Cập nhật điểm mới
        boolean scoreChanged = false;
        if (newScoreEx != null) {
            attempt.setScoreEx(newScoreEx);
            scoreChanged = true;
        }

        if (newScoreQuiz != null) {
            attempt.setScoreQuiz(newScoreQuiz);
            scoreChanged = true;
        }

        // Nếu không có điểm nào thay đổi, trả về lỗi
        if (!scoreChanged) {
            throw new IllegalArgumentException("Không có điểm nào được thay đổi");
        }

        // Tính toán lại điểm tổng
        Integer newScoreAss = calculateTotalScore(
                attempt.getScoreEx(),
                attempt.getScoreQuiz(),
                assessment.getExerciseScoreRatio(),
                assessment.getQuizScoreRatio()
        );

        attempt.setScoreAss(newScoreAss);
        attempt.setLastModified(LocalDateTime.now());
        // Lưu lịch sử chỉnh sửa điểm
        ScoreEditHistory history = new ScoreEditHistory();
        history.setAttemptId(attemptId);
        history.setOldScoreEx(oldScoreEx);
        history.setNewScoreEx(newScoreEx != null ? newScoreEx : oldScoreEx);
        history.setOldScoreQuiz(oldScoreQuiz);
        history.setNewScoreQuiz(newScoreQuiz != null ? newScoreQuiz : oldScoreQuiz);
        history.setOldScoreAss(oldScoreAss);
        history.setNewScoreAss(newScoreAss);
        history.setComment(comment);
        history.setEditedAt(LocalDateTime.now());
        // Lưu thay đổi
        attemptRepository.save(attempt);
        ScoreEditHistory savedHistory = scoreEditHistoryRepository.save(history);

        // Tạo response
        Map<String, Object> response = new HashMap<>();
        response.put("attemptId", attempt.getId());
        response.put("oldScoreEx", savedHistory.getOldScoreEx());
        response.put("newScoreEx", savedHistory.getNewScoreEx());
        response.put("oldScoreQuiz", savedHistory.getOldScoreQuiz());
        response.put("newScoreQuiz", savedHistory.getNewScoreQuiz());
        response.put("oldScoreAss", savedHistory.getOldScoreAss());
        response.put("newScoreAss", savedHistory.getNewScoreAss());
        response.put("comment", savedHistory.getComment());
        response.put("editedAt", savedHistory.getEditedAt());
        response.put("lastModified", attempt.getLastModified());

        return response;
    }

    // Hàm tính toán điểm tổng dựa trên điểm bài tập và điểm trắc nghiệm
    private Integer calculateTotalScore(Integer scoreEx, Integer scoreQuiz, Integer exerciseRatio, Integer quizRatio) {
        if (exerciseRatio + quizRatio == 0) {
            return 0;
        }

        double exPortion = (double) scoreEx * exerciseRatio / 100;
        double quizPortion = (double) scoreQuiz * quizRatio / 100;

        return (int) Math.round(exPortion + quizPortion);
    }

    public Map<String, Object> getCurrentScores(Long attemptId) {
        // Find the attempt by ID
        StudentAssessmentAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found with id: " + attemptId));

        // Create response with current scores
        Map<String, Object> response = new HashMap<>();
        response.put("attemptId", attempt.getId());
        response.put("scoreEx", attempt.getScoreEx());
        response.put("scoreQuiz", attempt.getScoreQuiz());
        response.put("scoreAss", attempt.getScoreAss());

        return response;
    }

}
