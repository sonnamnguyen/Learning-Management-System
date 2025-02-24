package com.example.quiz.controller;

import com.example.exception.NotFoundException;
import com.example.quiz.model.Question;
import com.example.quiz.model.Quiz;
import com.example.quiz.service.QuestionService;
import com.example.quiz.service.QuizService;
import com.example.user.UserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/questions")
public class QuestionController {

    @Autowired
    private UserService userService;
    @Autowired
    private QuizService quizService;
    @Autowired
    private QuestionService questionService;

    @ModelAttribute
    public void addCommonAttributes(Model model) {
        model.addAttribute("title", "Question");
        model.addAttribute("links", "/style.css");
    }

    @GetMapping()
    public String getQuestions(Model model,
                               @RequestParam(value = "searchQuery", required = false) String searchQuery,
                               @RequestParam(value = "page", defaultValue = "0") int page,
                               @RequestParam(value = "pageSize", defaultValue = "10") int pageSize)  {
        Page<Question> questionsPage;
        Pageable pageable = PageRequest.of(page, pageSize);
        if (searchQuery != null && !searchQuery.isEmpty()) {
            questionsPage = questionService.search(searchQuery, pageable);
        } else {
            questionsPage = questionService.findAll(pageable);
        }

        model.addAttribute("questionsPage", questionsPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", questionsPage.getTotalPages());
        model.addAttribute("totalItems", questionsPage.getTotalElements());
        model.addAttribute("searchQuery", searchQuery);

        model.addAttribute("content","questions/list");
        return "layout";
    }

    @GetMapping("/question-quiz/{name}")
    public String questionQuiz(Model model , @PathVariable String name){
        try {
            Question question = (Question) questionService.findByQuizName(name);
            model.addAttribute("quiz", quizService.findAll());
            model.addAttribute("question", questionService.findAll());
            model.addAttribute("selectedQuestion", question); // Fetch all users for the dropdown
            model.addAttribute("content", "quizes/detail");
        }catch(Exception e){
            return null;
        }
        return "layout";

    }

    @GetMapping("/create")
    public String showCreateForm(Model model,
                                 @RequestParam(name = "questionType", required = false) Question.QuestionType questionType,
                                 @PathVariable Long quizId) { // Lấy quizId từ URL nếu có
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        Question question = new Question();
        if (questionType != null) {
            question.setQuestionType(questionType);
        }

        List<Question> questionsForQuiz = questionService.findQuestionsByQuizId(quizId);

        model.addAttribute("questions", questionsForQuiz);
        model.addAttribute("newQuestion", question);
        model.addAttribute("content", "quizes/create");

        return "layout";
    }
    @PostMapping("/create")
    public String createQuestion(@PathVariable Long quizId, @ModelAttribute Question question) {
        questionService.create(quizId, question);
        return "redirect:/questions";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        Question question = questionService.findById(id).orElse(null);
        List<Quiz> quiz = quizService.findAll();
        model.addAttribute("quiz", quiz);
        model.addAttribute("users", userService.getAllUsers()); // Fetch all users for the dropdown

        model.addAttribute("content", "question/edit");
        return "layout";
    }
    @GetMapping("/delete/{id}")
    public String deleteQuestion(@PathVariable Long id) {
        questionService.deleteById(id);
        return "redirect:/questions";
    }

    @GetMapping("/showQuestion/{courseName}")
    public String showQuestion(Model _model_, @PathVariable String courseName) {
        try {
            Set<Quiz> quizzes = questionService.showAllQuizzesWithQuestions(courseName);

            if (quizzes == null || quizzes.isEmpty()) {
                return "errorPage";
            }
            _model_.addAttribute("quizzes", quizzes);
            _model_.addAttribute("content", "quizes/detail");
        } catch (NotFoundException e) {
            _model_.addAttribute("error", e.getMessage());
            return "errorPage";
        } catch (Exception e) {
            return "errorPage";
        }
        return "layout";
    }
}

