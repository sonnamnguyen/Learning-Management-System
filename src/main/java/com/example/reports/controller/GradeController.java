package com.example.reports.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/gradereports")
public class GradeController {


    @GetMapping("/assessment")
    public String assessmentDetails(Model model) {

        // add attribute for layout
        model.addAttribute("content","reports/grade/assessment");
        return "layout";
    }

    @GetMapping("/weightedGrade")
    public String weightedGrade(Model model) {

        // add attribute for layout
        model.addAttribute("content","reports/grade/weightedGrade");
        return "layout";
    }

    @GetMapping("/individualStudent")
    public String individualStudent(Model model) {

        // add attribute for layout
        model.addAttribute("content","reports/grade/individualStudent");
        return "layout";
    }

    @GetMapping("/classComparison")
    public String classComparison(Model model) {

        // add attribute for layout
        model.addAttribute("content","reports/grade/classComparison");
        return "layout";
    }

    @GetMapping("/gradeDistribution")
    public String gradeDistribution(Model model) {

        // add attribute for layout
        model.addAttribute("content","reports/grade/gradeDistribution");
        return "layout";
    }

    @GetMapping("/assessmentAnalysis")
    public String assessmentAnalysis(Model model) {

        // add attribute for layout
        model.addAttribute("content","reports/grade/assessmentAnalysis");
        return "layout";
    }

    @GetMapping("/classPerformance")
    public String classPerformance(Model model) {

        // add attribute for layout
        model.addAttribute("content","reports/grade/classPerformance");
        return "layout";
    }

    @GetMapping("/studentProgress")
    public String studentProgress(Model model) {
        // add attribute for layout
        model.addAttribute("content","reports/grade/studentProgress");
        return "layout";
    }

    @GetMapping("/studentSelfService")
    public String studentSelfService(Model model) {
        // add attribute for layout
        model.addAttribute("content","reports/grade/studentSelfService");
        return "layout";
    }

    @GetMapping("/notificationsAlerts")
    public String notificationsAlerts(Model model) {
        // add attribute for layout
        model.addAttribute("content","reports/grade/notificationsAlerts");
        return "layout";
    }
    @GetMapping("/inputScore")
    public String inputScore(Model model) {

        // add attribute for layout
        model.addAttribute("content","reports/grade/inputScore");
        return "layout";
    }

    @GetMapping("/overview")
    public String overview(Model model) {

        // add attribute for layout
        model.addAttribute("content","reports/grade/overview");
        return "layout";
    }


    @GetMapping("/grade")
    public String gradeStudent(Model model) {

        // add attribute for layout
        model.addAttribute("content","reports/grade/grades");
        return "layout";
    }


    @GetMapping("/gradeDetails")
    public String gradeDetails(Model model) {

        // add attribute for layout
        model.addAttribute("content","reports/grade/gradeDetail");
        return "layout";
    }
}



