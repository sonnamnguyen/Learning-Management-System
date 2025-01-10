package com.example.module_group;

import com.example.module_group.ModuleGroup;
import com.example.module_group.ModuleGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/module-groups")
public class ModuleGroupController {

    @Autowired
    private ModuleGroupService moduleGroupService;

    @GetMapping()
    public String getAssessmentType(Model model,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "10") int size,
                                    @RequestParam(value = "searchQuery", required = false) String searchQuery) {
        Page<ModuleGroup> moduleGroupPage;

        if (searchQuery != null && !searchQuery.isEmpty()) {
            // If there is a search query, use it to filter the assessmentTypes
            moduleGroupPage = moduleGroupService.searchModuleGroups(searchQuery, page, size);
        } else {
            // If no search query, just get all assessmentTypes with pagination
            moduleGroupPage = moduleGroupService.getModuleGroups(page, size);
        }

        model.addAttribute("moduleGroupPage", moduleGroupPage);
        model.addAttribute("searchQuery", searchQuery); // Pass search query back to the view
        return "module_group/list";  // your view template for displaying assessmentTypes
    }

//    @GetMapping
//    public String getAllModuleGroups(Model model) {
//        List<ModuleGroup> moduleGroups = moduleGroupService.getAllModuleGroups();
//        model.addAttribute("moduleGroups", moduleGroups);
//        return "module_group/list";
//    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("moduleGroup", new ModuleGroup());
        return "module_group/create"; // Ensure this is the correct view name
    }

    @PostMapping("/create")
    public String createModuleGroup(@ModelAttribute ModuleGroup moduleGroup, Model model) {
        if (moduleGroupService.isModuleGroupNameExists(moduleGroup.getName())) {
            model.addAttribute("error", "ModuleGroup name already exists!");
            return "module_group/create"; // Ensure this is the correct view name
        }
        moduleGroupService.createModuleGroup(moduleGroup.getName());
        return "redirect:/module-groups"; // Ensure this is the correct view name
    }

    //    @GetMapping("/edit/{id}")
//    public String showEditForm(@PathVariable Long id, Model model) {
//        moduleGroupService.getModuleGroupById(Math.toIntExact(id)).ifPresent(moduleGroup -> model.addAttribute("moduleGroup", moduleGroup));
//        return "module_group/edit";
//    }
// Show edit form for a specific assessment_type
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("moduleGroup", moduleGroupService.getModuleGroupById(id));
        return "module_group/edit";
    }

    // Update existing assessment_type
    @PostMapping("/edit/{id}")
    public String updateModuleGroup(@PathVariable Long id, @ModelAttribute ModuleGroup moduleGroup, Model model) {
        if (moduleGroupService.isModuleGroupNameExists(moduleGroup.getName())) {
            model.addAttribute("error", "ModuleGroup name already exists!");
            return "module_group/edit"; // Ensure this is the correct view name
        }
        moduleGroupService.updateModuleGroup(id, moduleGroup.getName());
        return "redirect:/module-groups";
    }

    @GetMapping("/delete/{id}")
    public String deleteModuleGroup(@PathVariable Long id) {
        moduleGroupService.deleteModuleGroup((long) Math.toIntExact(id));
        return "redirect:/module-groups";
    }

    // print module-group
    @GetMapping("/print")
    public String printModuleGroup(Model model){
        List<ModuleGroup> moduleGroupList = moduleGroupService.getAllModuleGroups();
        model.addAttribute("moduleGroupList", moduleGroupList);
        return "module_group/print";
    }
}
