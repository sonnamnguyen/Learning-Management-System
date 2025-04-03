package com.example.quiz.controller;

import com.example.quiz.model.Quiz;
import com.example.quiz.repository.*;
import com.example.quiz.service.QuestionService;
import com.example.quiz.service.QuizService;
import com.example.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/quizes/admin")
@PreAuthorize("hasAuthority('SUPERADMIN')")
public class DashboardController {

    @Autowired
    private QuizService quizService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private UserService userService;

    @Autowired
    private TestSessionRepository testSessionRepository;

    @Autowired
    private QuizParticipantRepository quizParticipantRepository;

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private QuizRepository quizRepository;

    @GetMapping("/dashboard")
    public String showDashboard(Model model, RedirectAttributes redirectAttributes) {
        // Kiểm tra quyền SUPERADMIN
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("SUPERADMIN"))) {
            redirectAttributes.addFlashAttribute("error", "You do not have permission to access the admin dashboard.");
            return "redirect:/quizes";
        }

        // Lấy thống kê chính
        model.addAttribute("totalQuizzes", quizService.count());
        model.addAttribute("totalQuestions", questionService.count());
        model.addAttribute("totalUsers", userService.count());
        model.addAttribute("totalAttempts", quizParticipantRepository.countTotalAttempts());

//        // Thống kê số lần tham gia quiz
        List<Object[]> quizAttemptsByQuiz = quizParticipantRepository.countAttemptsByQuiz();
//        List<String> quizNames = new ArrayList<>();
//        List<Long> quizAttempts = new ArrayList<>();
        List<Quiz> quizzes = quizService.findAll();
        List<Object[]> results = quizParticipantRepository.countAttemptsByQuiz();
        for (Object[] row : results) {
            System.out.println("Quiz ID: " + row[0] + ", Quiz Name: " + row[1] + ", Attempts: " + row[2]);
        }


//
//

//        model.addAttribute("quizNames", quizNames);
//        model.addAttribute("quizAttempts", quizAttempts);

        // Lấy số lượng câu trả lời đúng và sai
        model.addAttribute("correctAnswers", answerRepository.countTotalCorrectAnswers());
        model.addAttribute("incorrectAnswers", answerRepository.countTotalIncorrectAnswers());

        // Thống kê câu trả lời đúng/sai theo quiz
        Map<Long, String> quizTitles = new LinkedHashMap<>();
        Map<Long, Long> correctAnswersPerQuiz = new LinkedHashMap<>();
        Map<Long, Long> incorrectAnswersPerQuiz = new LinkedHashMap<>();

        List<Object[]> correctByQuiz = answerRepository.countCorrectAnswersByQuiz();
        List<Object[]> incorrectByQuiz = answerRepository.countIncorrectAnswersByQuiz();

        for (Object[] row : correctByQuiz) {
            Long quizId = (Long) row[0];
            String quizTitle = (String) row[1];
            quizTitles.putIfAbsent(quizId, quizTitle);
            correctAnswersPerQuiz.put(quizId, (Long) row[2]);
        }

        for (Object[] row : incorrectByQuiz) {
            Long quizId = (Long) row[0];
            String quizTitle = (String) row[1];
            quizTitles.putIfAbsent(quizId, quizTitle);
            incorrectAnswersPerQuiz.put(quizId, (Long) row[2]);
        }

        model.addAttribute("quizTitles", quizTitles);
        model.addAttribute("correctAnswersPerQuiz", correctAnswersPerQuiz);
        model.addAttribute("incorrectAnswersPerQuiz", incorrectAnswersPerQuiz);
        model.addAttribute("quizzes", quizzes);

        // Lấy danh sách quiz theo ID giảm dần (mới nhất)
        List<Quiz> quizzecreate = Optional.ofNullable(quizRepository.findAllByOrderByIdDesc()).orElse(new ArrayList<>());


        List<Long> quizIds = new ArrayList<>();
        List<String> quizNames = new ArrayList<>();
        List<Long> quizAttempts = new ArrayList<>();

        if (!quizAttemptsByQuiz.isEmpty()) {
            for (Object[] row : quizAttemptsByQuiz) {
                quizNames.add((String) row[1]);
                quizAttempts.add((Long) row[2]);
            }
        }

//        for (Quiz quiz : quizzecreate) {
//            quizIds.add(quiz.getId());
//            quizNames.add(quiz.getName()); // Đảm bảo phương thức này tồn tại
//            Optional<Long> attempts = quizAttemptsByQuiz.stream()
//                    .filter(q -> q[0].equals(quiz.getId()))
//                    .map(q -> (Long) q[1])
//                    .findFirst();
//            quizAttempts.add(attempts.orElse(0L));
//        }

//        model.addAttribute("quizIds", quizIds);
        model.addAttribute("quizNames", quizNames);
        model.addAttribute("quizAttempts", quizAttempts);

        System.out.println("=== Debug Quiz Data ===");
        System.out.println("Quiz IDs: " + quizIds);
        System.out.println("Quiz Names: " + quizNames);
        System.out.println("Quiz Attempts: " + quizAttempts);
        System.out.println("=======================");
        return "quizes/admin-dashboard";
    }
}



