package com.example.exercise.controller;

import com.example.exercise.model.Category;
import com.example.exercise.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/category")
@RequiredArgsConstructor
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping
    public String showCategoryForm(Model model, @RequestParam(defaultValue = "0") int page, @RequestParam(value = "redirect", required = false) String redirectUrl) {
        Page<Category> categoryPage = categoryService.getAllCategoryPaginated(page, 10);
        model.addAttribute("redirectUrl", redirectUrl);
        model.addAttribute("categories", categoryPage.getContent());
        model.addAttribute("category", new Category());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", categoryPage.getTotalPages());
        model.addAttribute("totalItems", categoryPage.getTotalElements());


        return "exercises/tag";
    }

    @PostMapping("/create")
    public String createCategory(@ModelAttribute Category category, RedirectAttributes redirectAttributes) {
        try {
            categoryService.saveCategory(category);
            redirectAttributes.addFlashAttribute("successMessage", "Tag added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/category";
    }

    @PostMapping("/edit")
    public String editCategory(@RequestParam Long id, @RequestParam String tag, RedirectAttributes redirectAttributes) {
        try {
            categoryService.updateCategory(id, tag);
            redirectAttributes.addFlashAttribute("successMessage", "Tag updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/category";
    }

    @PostMapping("/delete/{id}")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.deleteCategory(id);
            redirectAttributes.addFlashAttribute("successMessage", "Tag deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete tag. Please try again.");
        }
        return "redirect:/category";
    }
}
