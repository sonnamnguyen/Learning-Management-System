
package com.example.reports.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/studentReports")
public class StudentReport {


    @GetMapping("/attendance")
    public String assessmentDetails(Model model) {

        // add attribute for layout
        model.addAttribute("content","reports/student/attendance");
        return "layout";
    }


}



