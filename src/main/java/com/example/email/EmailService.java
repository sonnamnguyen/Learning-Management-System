package com.example.email;

import com.example.assessment.service.AssessmentService;
import com.google.common.hash.Hashing;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.hashids.Hashids;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    private final AssessmentService assessmentService;

    // Declaration of email hasher
    private final String EMAIL_SECRET_KEY = "SuGiaBienGioi123";
    private final SecretKey secretKey = new SecretKeySpec(EMAIL_SECRET_KEY.getBytes(), "AES");

    //    Changing this to the deploy server url later or change this according to the port of ur docker-compose
    //    private final String inviteUrlHeader = "https://group-02.cookie-candy.id.vn/assessment/invite/";
    private final String inviteUrlHeader = "http://localhost:9091/assessments/invite/";
    //  private final String inviteUrlHeader = "https://java02.fsa.io.vn/assessments/invite/";


    public String abc(){
        return "abc";
    }
    /**
     * Sends a single invitation email to multiple recipients.
     */
    @Async
    public void sendAssessmentInvite(List<String> emailList, long assessmentId) {
        if (emailList.isEmpty()) return;

        String encodedId = assessmentService.encodeId(assessmentId);
        assessmentService.increaseInvitedCount(assessmentId, emailList.size());

        // Generate assessment link
        String assessmentLink = inviteUrlHeader + encodedId + "/verify-email";

        // Convert list to array for sending
        String[] recipients = emailList.toArray(new String[0]);
        List<String> recipientList = Arrays.asList(recipients);

        // Send the email
        sendHtmlEmails(recipientList, "Assessment Invitation", generateAssessmentEmailBody(assessmentLink));
    }

    /**
     * Generates the assessment invitation email body with HTML.
     */
    private String generateAssessmentEmailBody(String assessmentLink) {
        return "<html>"
                + "<head><style>"
                + "body { font-family: Arial, sans-serif; color: #333; background-color: #f4f4f4; padding: 0; margin: 0; }"
                + ".email-container { background-color: white; padding: 30px; border-radius: 8px; max-width: 480px; margin: 20px auto; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }"
                + ".email-header { background-color: #007bff; color: white; padding: 15px; text-align: center; font-size: 18px; font-weight: bold; border-radius: 8px 8px 0 0; }"
                + ".email-body { padding: 20px; text-align: center; }"
                + ".invite-button { background-color: #007bff; color: white; padding: 12px 18px; text-decoration: none; font-size: 16px; font-weight: bold; border-radius: 6px; display: inline-block; }"
                + ".invite-button:hover { background-color: #0056b3; }"
                + "</style></head>"
                + "<body><div class='email-container'>"
                + "<div class='email-header'>Assessment Invitation</div>"
                + "<div class='email-body'>"
                + "<p>Hello,</p>"
                + "<p>You have been invited to participate in an assessment.</p>"
                + "<p>Click the button below to start:</p>"
                + "<p><a href='" + assessmentLink + "' class='invite-button'>Start Assessment</a></p>"
                + "<p>If you were not expecting this invitation, please ignore this email.</p>"
                + "</div></div></body></html>";
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
