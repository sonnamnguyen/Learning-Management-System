package com.example.assessment.service;

import com.example.email.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class NotificationService {
    private final EmailService emailService;

    @Autowired
    public NotificationService(EmailService emailService) {
        this.emailService = emailService;
    }

    public void sendReminderEmail(String email, Long assessmentId, LocalDateTime expireTime) {
        emailService.sendReminderEmail(email, assessmentId, expireTime);
    }
}
