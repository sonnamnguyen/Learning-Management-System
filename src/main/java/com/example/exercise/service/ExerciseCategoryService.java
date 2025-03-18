package com.example.exercise.service;


import com.example.exercise.model.ExerciseCategory;
import com.example.exercise.repository.ExerciseCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ExerciseCategoryService {

    @Autowired
    private ExerciseCategoryRepository exerciseCategoryRepository;

    public ExerciseCategory savedExerciseCategory(ExerciseCategory exerciseCategory) {
        return exerciseCategoryRepository.save(exerciseCategory);
    }

    public void deleteByExerciseIdAndCategoryId(Long exerciseId, Long categoryId) {
        exerciseCategoryRepository.deleteByExerciseIdAndCategoryId(exerciseId, categoryId);
    }

    public boolean existsByExerciseIdAndCategoryId(Long exerciseId, Long categoryId) {
        return exerciseCategoryRepository.existsByExerciseIdAndCategoryId(exerciseId, categoryId);
    }
}
