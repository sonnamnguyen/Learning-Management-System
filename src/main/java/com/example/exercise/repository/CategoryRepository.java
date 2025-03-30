package com.example.exercise.repository;

import com.example.exercise.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByTag(String tag);

    boolean existsByTag(String tag);
}
