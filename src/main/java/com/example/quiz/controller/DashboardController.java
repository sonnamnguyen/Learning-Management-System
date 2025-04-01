package com.example.quiz.controller;

import com.example.quiz.model.Quiz;
import com.example.quiz.repository.*;
import com.example.quiz.service.QuestionService;
import com.example.quiz.service.QuizService;
import com.example.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/quizes/admin") // 🔥 Corrected path
@PreAuthorize("hasAuthority('SUPERADMIN')")
public class DashboardController {

    @Autowired
    private QuizService quizService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private UserService userService;

    @Autowired
    private ResultRepository resultRepository;

    @Autowired
    private TestSessionRepository testSessionRepository;

    @Autowired
    private AnswerOptionRepository answerOptionRepository;

    @Autowired
    private QuizParticipantRepository quizParticipantRepository;

    @Autowired
    private AnswerRepository answerRepository;

    @GetMapping("/dashboard") // 🔥 Correct URL path
    public String showDashboard(Model model, RedirectAttributes redirectAttributes) {
        // 🔹 Get current user authentication
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 🔹 Check if user has "SUPERADMIN" role
        if (authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("SUPERADMIN"))) {
            redirectAttributes.addFlashAttribute("error", "You do not have permission to access the admin dashboard.");
            return "redirect:/quizes"; // 🔄 Redirect to quiz list
        }

        long totalQuizzes = quizService.count();
        long totalQuestions = questionService.count();
        long totalUsers = userService.count();
        long totalAttempts = testSessionRepository.count();
        List<Quiz> quizzes = quizService.findAll();

        List<Object[]> quizAttemptsByQuiz = quizParticipantRepository.countAttemptsByQuiz();
        List<String> quizNames = new ArrayList<>();
        List<Long> quizAttempts = new ArrayList<>();

        for (Object[] row : quizAttemptsByQuiz) {
            quizNames.add((String) row[0]);
            quizAttempts.add((Long) row[1]);
        }

        long correctAnswers = answerRepository.countTotalCorrectAnswers();
        long incorrectAnswers = answerRepository.countTotalIncorrectAnswers();

        Map<Long, Long> correctAnswersPerQuiz = new HashMap<>();
        Map<Long, Long> incorrectAnswersPerQuiz = new HashMap<>();
        Map<Long, String> quizTitles = new HashMap<>();

        List<Object[]> correctByQuiz = answerRepository.countCorrectAnswersByQuiz();
        List<Object[]> incorrectByQuiz = answerRepository.countIncorrectAnswersByQuiz();

        for (Object[] row : correctByQuiz) {
            Long quizId = (Long) row[0];
            String quizTitle = (String) row[1];
            Long count = (Long) row[2];

            quizTitles.put(quizId, quizTitle);
            correctAnswersPerQuiz.put(quizId, count);
        }

        for (Object[] row : incorrectByQuiz) {
            Long quizId = (Long) row[0];
            Long count = (Long) row[2];

            incorrectAnswersPerQuiz.put(quizId, count);
        }

        model.addAttribute("totalQuizzes", totalQuizzes);
        model.addAttribute("totalQuestions", totalQuestions);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalAttempts", totalAttempts);
        model.addAttribute("quizNames", quizNames);
        model.addAttribute("quizAttempts", quizAttempts);
        model.addAttribute("correctAnswers", correctAnswers);
        model.addAttribute("incorrectAnswers", incorrectAnswers);
        model.addAttribute("correctAnswersPerQuiz", correctAnswersPerQuiz);
        model.addAttribute("incorrectAnswersPerQuiz", incorrectAnswersPerQuiz);
        model.addAttribute("quizTitles", quizTitles);
        model.addAttribute("quizzes", quizzes);

        return "quizes/admin-dashboard"; // ✅ Correct path
    }
}
