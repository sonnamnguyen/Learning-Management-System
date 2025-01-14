package com.example.reports.course;

import com.example.department.Department;
import com.example.department.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class CourseReport {

    @Autowired
    private DepartmentService departmentService;


    @GetMapping("/report/total-courses")
    public String getTotalCourses(Model model) {
        List<Department> departments = departmentService.getAllDepartments(); // Fetch all departments
        Map<String, Long> departmentCourseCounts = new HashMap<>();

        // Iterate through each department to calculate the total courses
        for (Department department : departments) {
            long totalCourses = department.getCourses() != null ? department.getCourses().size() : 0;
            departmentCourseCounts.put(department.getName(), totalCourses);
        }

        model.addAttribute("departmentCourseCounts", departmentCourseCounts);
        return "report/department/total-courses";
    }



}



