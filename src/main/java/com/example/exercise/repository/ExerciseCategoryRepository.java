package com.example.exercise.repository;

import com.example.exercise.model.ExerciseCategory;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ExerciseCategoryRepository extends JpaRepository<ExerciseCategory, Long> {

    ExerciseCategory save(ExerciseCategory exerciseCategory);


    //    void deleteByExerciseId(Long exerciseId);
    @Modifying
    @Transactional
    @Query("DELETE FROM ExerciseCategory ec WHERE ec.exercise.id = :exerciseId AND ec.category.id = :categoryId")
    void deleteByExerciseIdAndCategoryId(@Param("exerciseId") Long exerciseId, @Param("categoryId") Long categoryId);

//    void deleteByExerciseIdAndCategoryId(Long exerciseId, Long categoryId);

    @Query("SELECT COUNT(ec) > 0 FROM ExerciseCategory ec WHERE ec.exercise.id = :exerciseId AND ec.category.id = :categoryId")
    boolean existsByExerciseIdAndCategoryId(@Param("exerciseId") Long exerciseId, @Param("categoryId") Long categoryId);

}
