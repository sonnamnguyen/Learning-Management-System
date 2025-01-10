package com.example;

import com.example.module_group.ModuleGroup;
import com.example.module_group.ModuleGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
public class GlobalAttributesController {

    @Autowired
    private ModuleGroupService moduleGroupService; // Service hoặc repository để lấy dữ liệu

    @ModelAttribute("moduleGroups")
    public List<ModuleGroup> getModuleGroups() {
        return moduleGroupService.getAllModuleGroups(); // Trả về danh sách `ModuleGroup`
    }
}
