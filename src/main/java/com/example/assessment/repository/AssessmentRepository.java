package com.example.assessment.repository;

import com.example.assessment.model.Assessment;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssessmentRepository extends PagingAndSortingRepository<Assessment, Long> {

    @Query("SELECT a FROM Assessment a WHERE a.title LIKE %:searchQuery% ORDER BY a.updatedAt DESC")
    Page<Assessment> search(@Param("searchQuery") String searchQuery, Pageable pageable);

    @Query("SELECT COUNT(a) > 0 FROM Assessment a WHERE LOWER(a.title) = LOWER(:name)")
    boolean existsByName(@Param("name") String name);

    @Query("SELECT a FROM Assessment a LEFT JOIN FETCH a.assessmentQuestions WHERE a.id = :id")
    Assessment findByAssessmentIdWithQuestions(@Param("id") Long id);

    @Query("SELECT a FROM Assessment a ORDER BY a.updatedAt DESC")
    Page<Assessment> findAll(Pageable pageable);

    List<Assessment> findAll();

    Optional<Assessment> findById(Long id);

    Assessment save(Assessment assessment);

    boolean existsByTitle(String title);

    void deleteById(Long id);

    // ✅ New method: Update invited_count using JPQL
    @Modifying
    @Transactional
    @Query("UPDATE Assessment a SET a.invitedCount = a.invitedCount + 1 WHERE a.id = :id")
    int incrementInvitedCount(@Param("id") Long id);

    // ✅ New method: Refresh entity to avoid Hibernate caching issues
    @Query("SELECT a FROM Assessment a WHERE a.id = :id")
    Optional<Assessment> refresh(@Param("id") Long id);

    boolean existsByTitleAndAssessmentTypeIdAndIdNot(String title, Long assessmentTypeId, Long id);


    boolean existsByTitleAndAssessmentTypeId(String title, Long assessmentTypeId);

    List<Assessment> findByTitleContaining(String title); // Removed Course parameter

}

