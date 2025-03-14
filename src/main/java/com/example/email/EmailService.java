package com.example.email;

import com.example.assessment.model.Assessment;
import com.example.assessment.repository.AssessmentRepository;
import com.example.assessment.service.AssessmentService;
import com.example.config.AppConfig;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.hashids.Hashids;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    @Autowired
    AssessmentRepository assessmentRepository;

    // Declaration of email hasher
    private final String EMAIL_SECRET_KEY = "SuGiaBienGioi123";
    private final SecretKey secretKey = new SecretKeySpec(EMAIL_SECRET_KEY.getBytes(), "AES");

    //    Changing this to the deploy server url later or change this according to the port of ur docker-compose
    //    private final String inviteUrlHeader = "https://group-02.cookie-candy.id.vn/assessment/invite/";
    @Value("${invite.url.header}")
    private String inviteUrlHeader;//  private final String inviteUrlHeader = "https://java02.fsa.io.vn/assessments/invite/";
    private Hashids hashids = new Hashids("BaTramBaiCodeThieuNhi", 32);

    public void increaseInvitedCount(long assessmentId, int count) {
        Optional<Assessment> assessmentOpt = assessmentRepository.findById(assessmentId);
        if (assessmentOpt.isPresent()) {
            Assessment assessment = assessmentOpt.get();
            assessment.setInvitedCount(assessment.getInvitedCount() + count); // Increase by batch size
            assessmentRepository.save(assessment);
        }
    }
    public String encodeId(long id) {
        return hashids.encode(id);
    }
    /**
     * Sends a single invitation email to multiple recipients.
     */
    @Async
    public void sendAssessmentInvite(List<String> emailList, long assessmentId, LocalDateTime expirationDate) {
        if (emailList.isEmpty()) return;

        String encodedId = encodeId(assessmentId);
        increaseInvitedCount(assessmentId, emailList.size());

        // Convert expiration date to GMT+7
        ZoneId gmtPlus7 = ZoneId.of("Asia/Ho_Chi_Minh");
        ZonedDateTime nowGmt7 = ZonedDateTime.now(gmtPlus7);
        ZonedDateTime expirationGmt7 = expirationDate.atZone(gmtPlus7);

        // Generate assessment link
        String assessmentLink = inviteUrlHeader + encodedId + "/verify-email";

        // Convert list to array for sending
        String[] recipients = emailList.toArray(new String[0]);
        List<String> recipientList = Arrays.asList(recipients);

        // Check if the expiration is in less than 24 hours
        long hoursUntilExpiration = Duration.between(nowGmt7, expirationGmt7).toHours();

        if (hoursUntilExpiration < 24) {
            // Send reminder email
            sendHtmlEmails(recipientList, "Assessment Reminder", generateUrgentAssessmentEmailBody(assessmentLink, expirationDate));
        } else {
            // Send normal invitation email
            sendHtmlEmails(recipientList, "Assessment Invitation", generateAssessmentEmailBody(assessmentLink, expirationDate));
        }
    }

    @Async
    public void sendReminderEmail(String email, long assessmentId, LocalDateTime expirationDate) {
        if (email == null || email.isEmpty()) return;

        String encodedId = encodeId(assessmentId);
        String assessmentLink = inviteUrlHeader + encodedId + "/verify-email";

        sendHtmlEmails(Collections.singletonList(email), "Assessment Reminder",
                generateReminderEmailBody(assessmentLink, expirationDate));
    }
    @Async
    public void sendUrgentAssessmentInvite(List<String> emailList, long assessmentId, LocalDateTime expirationDate) {
        if (emailList.isEmpty()) return;

        String encodedId = encodeId(assessmentId);
        increaseInvitedCount(assessmentId, emailList.size());

        // Convert expiration date to GMT+7
        ZoneId gmtPlus7 = ZoneId.of("Asia/Ho_Chi_Minh");
        ZonedDateTime expirationGmt7 = expirationDate.atZone(gmtPlus7);

        // Generate assessment link
        String assessmentLink = inviteUrlHeader + encodedId + "/verify-email";

        // Convert list to array for sending
        List<String> recipientList = new ArrayList<>(emailList);

        // Send urgent assessment invitation
        sendHtmlEmails(recipientList, "⚠️ Urgent: Complete Your Assessment Now",
                generateUrgentAssessmentEmailBody(assessmentLink, expirationDate));
    }

    /**
     * Generates an urgent assessment email when the expiration is under 24 hours.
     */
    private String generateUrgentAssessmentEmailBody(String assessmentLink, LocalDateTime expirationGmt7) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedExpirationDate = expirationGmt7.format(formatter);

        return "<html>"
                + "<head>"
                + "<style>"
                + "body { font-family: Arial, sans-serif; background-color: #fff3cd; color: #856404; padding: 20px; }"
                + ".container { width: 100%; max-width: 600px; margin: auto; background-color: #ffffff; border-radius: 8px; padding: 20px; box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1); }"
                + ".header { text-align: center; color: #d9534f; font-size: 22px; font-weight: bold; }"
                + ".content { margin-top: 20px; font-size: 16px; color: #000000; text-align: center; }"
                + ".button-container { margin-top: 20px; text-align: center; }"
                + ".invite-button { background-color: #d9534f !important; color: white !important; padding: 12px 20px; text-decoration: none !important; font-size: 16px; font-weight: bold; border-radius: 6px; display: inline-block; }"
                + ".invite-button:hover { background-color: #c9302c !important; }"
                + ".footer { margin-top: 30px; text-align: left; font-size: 14px; color: #888; }"
                + "</style>"
                + "</head>"
                + "<body>"
                + "<div class='container'>"
                + "<div class='header'>⚠️ Urgent: Assessment Closing Soon!</div>"
                + "<div class='content'>"
                + "<p>Hello,</p>"
                + "<p>You have less than 24 hours left to complete your assessment.</p>"
                + "<p><strong>Expiration Time: " + formattedExpirationDate + " (GMT+7)</strong></p>"
                + "<p>Click below to take the assessment before it's too late!</p>"
                + "<div class='button-container'>"
                + "<a href='" + assessmentLink + "' class='invite-button' "
                + "style='background-color: #d9534f !important; color: white !important; text-decoration: none !important; padding: 12px 20px; font-size: 16px; font-weight: bold; border-radius: 6px; display: inline-block;'>"
                + "Complete Now</a>"
                + "</div>"
                + "<p>If you have already completed this assessment, you may ignore this message.</p>"
                + "</div>"
                + "<div class='footer'>"
                + "<p>If you have any questions, feel free to contact our support team.</p>"
                + "<p>Thank you!</p>"
                + "</div>"
                + "</div>"
                + "</body>"
                + "</html>";
    }

    /**
     * Generates a reminder email if the assessment expires in less than 24 hours.
     */
    private String generateReminderEmailBody(String assessmentLink, LocalDateTime expirationGmt7) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedExpirationDate = expirationGmt7.format(formatter);

        return "<html>"
                + "<head>"
                + "<style>"
                + "body { font-family: Arial, sans-serif; background-color: #fff3cd; color: #856404; padding: 20px; }"
                + ".container { width: 100%; max-width: 600px; margin: auto; background-color: #ffffff; border-radius: 8px; padding: 20px; box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1); }"
                + ".header { text-align: center; color: #d9534f; font-size: 22px; font-weight: bold; }"
                + ".content { margin-top: 20px; font-size: 16px; color: #000000; text-align: center; }"
                + ".button-container { margin-top: 20px; text-align: center; }"
                + ".invite-button { background-color: #d9534f; color: white; padding: 12px 20px; text-decoration: none; font-size: 16px; font-weight: bold; border-radius: 6px; display: inline-block; }"
                + ".invite-button:hover { background-color: #c9302c; }"
                + ".footer { margin-top: 30px; text-align: left; font-size: 14px; color: #888; }"
                + "</style>"
                + "</head>"
                + "<body>"
                + "<div class='container'>"
                + "<div class='header'>⚠️ Assessment Expiry Reminder</div>"
                + "<div class='content'>"
                + "<p>Hello,</p>"
                + "<p>Your assessment is about to expire soon!</p>"
                + "<p><strong>Expiration Time: " + formattedExpirationDate + " (GMT+7)</strong></p>"
                + "<p>Please complete it as soon as possible.</p>"
                + "<div class='button-container'>"
                + "<a href='" + assessmentLink + "' class='invite-button'>Complete Now</a>"
                + "</div>"
                + "<p>If you have already completed this assessment, you may ignore this message.</p>"
                + "</div>"
                + "<div class='footer'>"
                + "<p>If you have any questions, feel free to contact our support team.</p>"
                + "<p>PhucTH50@fpt.com - Senior IT</p>"
                + "<p>Thank you!</p>"
                + "</div>"
                + "</div>"
                + "</body>"
                + "</html>";
    }
    /**
     * Generates the assessment invitation email body with HTML.
     */
    private String generateAssessmentEmailBody(String assessmentLink, LocalDateTime expirationDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedExpirationDate = expirationDate.format(formatter);

        return "<html>"
                + "<head>"
                + "<style>"
                + "body { font-family: Arial, sans-serif; background-color: #f4f4f9; color: #333; padding: 20px; }"
                + ".container { width: 100%; max-width: 600px; margin: auto; background-color: #ffffff; border-radius: 8px; padding: 20px; box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1); }"
                + ".header { text-align: center; color: #007bff; font-size: 22px; font-weight: bold; }"
                + ".content { margin-top: 20px; font-size: 16px; color: #000000; text-align: center; }"
                + ".button-container { margin-top: 20px; text-align: center; }"
                + ".invite-button { background-color: #007bff; color: white; padding: 12px 20px; text-decoration: none; font-size: 16px; font-weight: bold; border-radius: 6px; display: inline-block; }"
                + ".invite-button:hover { background-color: #0056b3; }"
                + ".footer { margin-top: 30px; text-align: left; font-size: 14px; color: #888; }"
                + "</style>"
                + "</head>"
                + "<body>"
                + "<div class='container'>"
                + "<div class='header'>Assessment Invitation</div>"
                + "<div class='content'>"
                + "<p>Hello,</p>"
                + "<p>You have been invited to participate in an assessment.</p>"
                + "<p>Click the button below to start:</p>"
                + "<div class='button-container'>"
                + "<a href='" + assessmentLink + "' class='invite-button'>Start Assessment</a>"
                + "</div>"
                + "<p><strong>Note:</strong> This invitation will expire on <strong>" + formattedExpirationDate + " (GMT 7+) </strong>.</p>"
                + "<p>If you were not expecting this invitation, please ignore this email.</p>"
                + "</div>"
                + "<div class='footer'>"
                + "<p>If you have any questions, feel free to contact our support team.</p>"
                + "<p>PhucTH50@fpt.com - Senior IT</p>"
                + "<p>Thank you!</p>"
                + "</div>"
                + "</div>"
                + "</body>"
                + "</html>";
    }


    /**
     * Sends an HTML email to a recipient.
     */
    public void sendHtmlEmail(String recipient, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(body, true); // Enables HTML content

            mailSender.send(message);
            System.out.println("Email sent to: " + recipient);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }


    private void sendHtmlEmails(List<String> recipients, String subject, String body) {
        int batchSize = 10; // Adjust batch size as needed

        for (int i = 0; i < recipients.size(); i += batchSize) {
            int end = Math.min(i + batchSize, recipients.size());
            List<String> batch = recipients.subList(i, end);

            try {
                // Send batch
                sendBatch(batch, subject, body);
                System.out.println("Batch sent successfully: " + batch);
            } catch (MailSendException e) {
                // Identify and retry only the valid emails
                List<String> validEmails = extractValidEmails(batch, e);
                if (!validEmails.isEmpty()) {
                    retrySending(validEmails, subject, body);
                }
            } catch (MailException | MessagingException e) {
                System.err.println("Batch failed: " + e.getMessage());
            }
        }
    }

    private void sendBatch(List<String> emails, String subject, String body) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(emails.toArray(new String[0]));
        helper.setSubject(subject);
        helper.setText(body, true);

        mailSender.send(message);
    }

    private List<String> extractValidEmails(List<String> batch, MailSendException e) {
        List<String> validEmails = new ArrayList<>(batch);

        Exception[] messageExceptions = e.getMessageExceptions();
        if (messageExceptions != null) {
            for (Exception me : messageExceptions) {
                System.err.println("Failed email: " + me.getMessage());
                validEmails.remove(me.getMessage()); // Remove invalid emails from batch
            }
        }
        return validEmails;
    }

    private void retrySending(List<String> validEmails, String subject, String body) {
        try {
            sendBatch(validEmails, subject, body);
            System.out.println("Retry successful for: " + validEmails);
        } catch (MessagingException ex) {
            System.err.println("Retry failed: " + ex.getMessage());
        }
    }
}
