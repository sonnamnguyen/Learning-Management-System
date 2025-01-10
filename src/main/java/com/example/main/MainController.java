package com.example.main;

import com.example.module.Module;
import com.example.module.ModuleService;
import com.example.module_group.ModuleGroup;
import com.example.module_group.ModuleGroupRepository;
import com.example.module_group.ModuleGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.SessionAttributes;

import java.util.Comparator;
import java.util.List;

@Controller
@SessionAttributes("moduleGroups") // Specify attributes to store in session
public class MainController {

    @Autowired
    private ModuleService moduleService; // Service to fetch modules

    @Autowired
    private ModuleGroupService moduleGroupService;

    @GetMapping("/")
    public String dashboard(Model model) {
        List<ModuleGroup> moduleGroups = moduleGroupService.getAllModuleGroups(); // Fetch all module groups and their modules
        moduleGroups.sort(Comparator.comparing(ModuleGroup::getName));
        model.addAttribute("moduleGroups", moduleGroups);

        List<Module> modules = moduleService.findAllModules(); // Fetch modules from the service
        modules.sort(Comparator.comparing(Module::getName));
        model.addAttribute("modules", modules); // Add modules to the model

        return "dashboard"; // Corresponds to dashboard.html in templates folder
    }

    @GetMapping("/logout")
    public String logout() {
        return "logout";
    }
}
