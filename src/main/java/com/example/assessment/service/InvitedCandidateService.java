package com.example.assessment.service;


import com.example.assessment.model.InvitedCandidate;
import com.example.assessment.repository.InvitedCandidateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class InvitedCandidateService  {

    private final InvitedCandidateRepository repository;

    @Autowired
    public InvitedCandidateService(InvitedCandidateRepository repository) {
        this.repository = repository;
    }

    public InvitedCandidate save(InvitedCandidate invitedCandidate) {
        return repository.save(invitedCandidate);
    }

    public Optional<InvitedCandidate> findById(Long id) {
        return repository.findById(id);
    }

    public List<InvitedCandidate> findByAssessmentId(Long assessmentId) {
        return repository.findByAssessmentId(assessmentId);
    }

    public List<InvitedCandidate> findByEmail(String email) {
        return repository.findByEmail(email);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    public List<InvitedCandidate> findAll() {
        return repository.findAll();
    }

    public Page<InvitedCandidate> findByAssessmentIdAndEmailContaining(Long assessmentId, String email, Pageable pageable) {
        return repository.findByAssessmentIdAndEmailContaining(assessmentId, email, pageable);
    }

    public Page<InvitedCandidate> findByAssessmentId(Long assessmentId, Pageable pageable) {
        return repository.findByAssessmentId(assessmentId, pageable);
    }
}

