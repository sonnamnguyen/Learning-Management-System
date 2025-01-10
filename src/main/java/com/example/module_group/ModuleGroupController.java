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

    // Thêm model attribute chung cho tất cả các phương thức
    @ModelAttribute
    public void addCommonAttributes(Model model) {
        model.addAttribute("title", "Module Group");
        model.addAttribute("links", "/style.css");
    }

    @GetMapping()
    public String getAllModuleGroups(Model model,
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

        // add attribute for layout
        model.addAttribute("content","module_group/list");
        return "layout";
    }


    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("moduleGroup", new ModuleGroup());

        model.addAttribute("content", "module_group/create");
        return "layout";
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

	// Show edit form for a specific assessment_type
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("moduleGroup", moduleGroupService.getModuleGroupById(id));

        model.addAttribute("content", "module_group/edit");
        return "layout";
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
