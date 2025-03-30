package com.example.exercise.service;

import com.example.exercise.model.Category;
import com.example.exercise.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    public List<Category> getAllCategory() {
        return categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "tag"));
    }

    public Page<Category> getAllCategoryPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("tag").ascending());
        return categoryRepository.findAll(pageable);
    }

    public Optional<Category> getCategoryById(long id) {
        return categoryRepository.findById(id);
    }

    public Category saveCategory(Category category) throws Exception {
        if(categoryRepository.existsByTag(category.getTag())) {
            throw new Exception("Tag already exists");
        }
        return categoryRepository.save(category);
    }

    public Category updateCategory(Long id, String tag) throws Exception {
        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new Exception("Tag not found!"));

        // Check if new tag name exists for other categories
        if (!existingCategory.getTag().equals(tag) && existsByTagName(tag)) {
            throw new Exception("Tag name already exists!");
        }

        existingCategory.setTag(tag);
        return categoryRepository.save(existingCategory);
    }

    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }

    public boolean existsByTagName(String tag) {
        return categoryRepository.findAll().stream()
                .anyMatch(category -> category.getTag().equalsIgnoreCase(tag));
    }

    public Category existsByName(String name) {
        return categoryRepository.findByTag(name).orElse(null);
    }


}
