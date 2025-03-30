package com.example.assessment.repository;

import com.example.assessment.model.InvitedCandidate;
import org.apache.poi.sl.draw.geom.GuideIf;
import org.springframework.cglib.core.Local;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InvitedCandidateRepository extends JpaRepository<InvitedCandidate, Long> {

    @Query("SELECT ic FROM InvitedCandidate ic WHERE ic.email LIKE %:searchQuery%")
    Page<InvitedCandidate> searchByEmail(@Param("searchQuery") String searchQuery, Pageable pageable);

    // Find all invited candidates for a specific assessment
    @Query("SELECT ic FROM InvitedCandidate ic WHERE ic.assessment.id = :assessmentId")
    Page<InvitedCandidate> findByAssessmentId(@Param("assessmentId") Long assessmentId, Pageable pageable);

    // Find all invited candidates (paged)
    Page<InvitedCandidate> findAll(Pageable pageable);

    // Find an invited candidate by email
    List<InvitedCandidate> findByEmail(String email);

    // Find all invited candidates for a specific assessment (non-paged)
    List<InvitedCandidate> findByAssessmentId(Long assessmentId);

    // Save or update an invited candidate (provided by JpaRepository)
    InvitedCandidate save(InvitedCandidate invitedCandidate);

    /**
     * Tìm kiếm tất cả InvitedCandidate của một Assessment
     * theo từ khóa email (có chứa 'email').
     * Sử dụng phân trang (Pageable).
     */
    @Query("SELECT ic FROM InvitedCandidate ic " +
            "WHERE ic.assessment.id = :assessmentId " +
            "AND ic.email LIKE %:email%")
    Page<InvitedCandidate> findByAssessmentIdAndEmailContaining(@Param("assessmentId") Long assessmentId,
                                                                @Param("email") String email,
                                                                Pageable pageable);

    @Query("SELECT ic.expirationDate FROM InvitedCandidate ic WHERE ic.assessment.id = :assessmentId ORDER BY ic.expirationDate DESC")
    Optional<LocalDateTime> findLatestExpireDateByAssessmentId(@Param("assessmentId") Long assessmentId);

    @Query("SELECT ic.expirationDate FROM InvitedCandidate ic " +
            "WHERE ic.assessment.id = :assessmentId AND ic.email = :email")
    Optional<LocalDateTime> findExpireDateByAssessmentIdAndEmail(@Param("assessmentId") Long assessmentId,
                                                                 @Param("email") String email);

    @Query("SELECT ic FROM InvitedCandidate ic WHERE ic.expirationDate = :targetTime")
    List<InvitedCandidate> findCandidatesExpiringAt(LocalDateTime targetTime);

    @Query("SELECT ic FROM InvitedCandidate ic " +
            "WHERE ic.assessment.id = :assessmentId " +
            "AND ic.email = :email")
    Optional<InvitedCandidate> findByAssessmentIdAndEmail(@Param("assessmentId") Long assessmentId,
                                                          @Param("email") String email);

    @Query("SELECT i FROM InvitedCandidate i WHERE i.email = :email")
    List<InvitedCandidate> findByUserEmail(@Param("email") String email);

    @Query("SELECT ic FROM InvitedCandidate ic WHERE ic.assessment.id = :userId")
    List<InvitedCandidate> findByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE InvitedCandidate ic SET ic.hasAssessed = true WHERE ic.email = :email AND ic.assessment.id = :id")
    void updateHasAssessedByEmailAndAssessmentId(@Param("email") String email, @Param("id") long id);

}
