package com.example.reports.course;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ReportController {

    @Autowired
    private ReportService dashboardService;

    @GetMapping("/reports")
    public String showDashboard(Model model) {
        model.addAttribute("totalDepartments", dashboardService.getTotalDepartments());
        model.addAttribute("totalCourses", dashboardService.getTotalCourses());
        // Add more attributes for other reports.
        return "report/dashboard";
    }
}

