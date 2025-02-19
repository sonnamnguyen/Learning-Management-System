package com.example.quiz.controller;

import com.example.course.Course;
import com.example.course.CourseService;
import com.example.quiz.model.Quiz;
import com.example.quiz.service.QuizService;
import com.example.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ModelAttribute;


@Controller
@RequestMapping("/quizes")
public class QuizController {

    @Autowired
    private QuizService quizService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private UserService userService;

    // Thêm model attribute chung cho tất cả các phương thức
    @ModelAttribute
    public void addCommonAttributes(Model model) {
        model.addAttribute("title", "Quizes");
        model.addAttribute("links", "/style.css");
    }

    @GetMapping
//    @PreAuthorize("hasAuthority('ADMIN')")
    public String list(Model model,
                              @RequestParam(value = "searchQuery", required = false) String searchQuery,
                              @RequestParam(value = "page", defaultValue = "0") int page,
                              @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        Page<Quiz> quizes;
        Pageable pageable = PageRequest.of(page, pageSize);
        if (searchQuery != null && !searchQuery.isEmpty()) {
            quizes = quizService.search(searchQuery, pageable);
        } else {
            quizes = quizService.findAll(pageable);
        }

        model.addAttribute("quizes", quizes.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", quizes.getTotalPages());
        model.addAttribute("totalItems", quizes.getTotalElements());
        model.addAttribute("searchQuery", searchQuery);
        // add attribute for layout
        model.addAttribute("content","quizes/list");

        return "layout";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        // Get the authenticated user's details
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName(); // Get the username of the logged-in user

        // Add a new Quiz object to the model
        Quiz quiz = new Quiz();
        quiz.setCreatedBy(userService.findByUsername(username)); // Assume you have a method to get the User object

        model.addAttribute("quizes", quiz);
        model.addAttribute("courses", courseService.getAllCourses());
        model.addAttribute("users", userService.getAllUsers()); // Fetch all users for the dropdown

        model.addAttribute("content", "quizes/create");
        return "layout";
    }

    @PostMapping("/create")
    public String create(@ModelAttribute("quizes") Quiz quiz) {
        quizService.save(quiz);
        return "redirect:/quizes";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        Quiz quiz = quizService.findById(id).orElse(null);
        List<Course> courses = courseService.getAllCourses();
        model.addAttribute("quiz", quiz);
        model.addAttribute("courses", courses);
        model.addAttribute("users", userService.getAllUsers()); // Fetch all users for the dropdown

        model.addAttribute("content", "quizes/edit");
        return "layout";
    }

    @PostMapping("/edit/{id}")
    public String update(@PathVariable("id") Long id, @ModelAttribute Quiz quiz) {
        quiz.setId(id);
        quizService.save(quiz);
        return "redirect:/quizes";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable("id") Long id) {
        quizService.deleteById(id);
        return "redirect:/quizes";
    }

    // Print roles page
    @GetMapping("/print")
    public String print(Model model) {
        List<Quiz> quizes = quizService.findAll();
        model.addAttribute("quizes", quizes);
        return "quizes/print";
    }
    @GetMapping("/export")
    public ResponseEntity<InputStreamResource> export() {
        // Fetch all roles (page size set to max to get all records)
       List<Quiz> quizes = quizService.findAll();

        // Convert roles to Excel (assumes you have a service for that)
        ByteArrayInputStream excelFile = quizService.exportToExcel(quizes);

        // Create headers for the response (Content-Disposition to trigger file download)
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=quizes.xlsx");

        // Return the file in the response
        return ResponseEntity.ok()
                .headers(headers)
                .body(new InputStreamResource(excelFile));
    }

    @PostMapping("/import")
    public String importExcel(@RequestParam("file") MultipartFile file) {
        quizService.importExcel(file);
        return "redirect:/quizes";
    }
}
