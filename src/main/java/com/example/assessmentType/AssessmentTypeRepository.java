package com.example.assessmentType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AssessmentTypeRepository extends JpaRepository<AssessmentType, Integer> {
    Page<AssessmentType> findAll(Pageable pageable);
    Page<AssessmentType> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Optional<AssessmentType> findByName(String name);
}


