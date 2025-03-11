package com.example.testcase;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, Long> {
    List<TestCase> findByExerciseId(Long exerciseId);

    //Query to find hidden test case
    @Query("SELECT t FROM TestCase t " +
            "WHERE t.isHidden = TRUE " +
            "AND t.exercise.id = :id ")
    List<TestCase> findHiddenTestCase(@Param("id") Long id);

    //Query to find visible test case
    @Query("SELECT t FROM TestCase t " +
            "WHERE t.isHidden = FALSE " +
            "AND t.exercise.id = :id ")
    List<TestCase> findVisibleTestCase(@Param("id") Long id);

    @Modifying
    @Query("DELETE FROM TestCase t WHERE t.exercise.id = :exerciseId")
    void deleteByExerciseId(@Param("exerciseId") Long exerciseId);
}