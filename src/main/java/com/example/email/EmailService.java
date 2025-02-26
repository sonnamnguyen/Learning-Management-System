package com.example.email;

import com.example.assessment.service.AssessmentService;
import com.google.common.hash.Hashing;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.hashids.Hashids;
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

        // Send the email
        sendHtmlEmails(recipients, "Assessment Invitation", generateAssessmentEmailBody(assessmentLink));
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


    /**
     * Sends an HTML email to multiple recipients.
     */
    private void sendHtmlEmails(String[] recipients, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(recipients);
            helper.setSubject(subject);
            helper.setText(body, true); // Enables HTML content

            mailSender.send(message);
            System.out.println("Email sent to: " + String.join(", ", recipients));
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
    /**
     * Sends an OTP verification email.
     */
    public void sendOtpEmail(String recipient, String otpCode) {
        String subject = "Verify Your Account";
        String body = generateOtpEmailBody(otpCode);
        sendHtmlEmail(recipient, subject, body);
    }

    /**
     * Generates the OTP email body.
     */
    private String generateOtpEmailBody(String otpCode) {
        return "<html>"
                + "<head><style>"
                + "body { font-family: Arial, sans-serif; background-color: #f9f9f9; margin: 0; padding: 0; }"
                + ".email-container { max-width: 600px; margin: 20px auto; background-color: #ffffff; border: 1px solid #dddddd; border-radius: 8px; overflow: hidden; }"
                + ".header { background-color: #4CAF50; color: #ffffff; padding: 20px; text-align: center; }"
                + ".content { padding: 20px; color: #333333; text-align: center; }"
                + ".otp-code { font-size: 24px; font-weight: bold; color: #4CAF50; background-color: #f4f4f4; padding: 10px 20px; border-radius: 5px; display: inline-block; margin: 20px 0; }"
                + ".footer { background-color: #f4f4f4; color: #888888; padding: 10px; text-align: center; font-size: 12px; }"
                + "</style></head>"
                + "<body><div class='email-container'>"
                + "<div class='header'><h1>Verify Your Account</h1></div>"
                + "<div class='content'>"
                + "<p>Dear User,</p>"
                + "<p>Thank you for signing up! To complete your registration, use the OTP code below:</p>"
                + "<div class='otp-code'>" + otpCode + "</div>"
                + "<p>This OTP code is valid for <strong>10 minutes</strong>. If you did not request this, please ignore this email.</p>"
                + "<p>Best regards,<br>The Team</p>"
                + "</div>"
                + "<div class='footer'>&copy; 2025 Your Company. All rights reserved.</div>"
                + "</div></body></html>";
    }

    /**
     * Encrypts an email address using AES and encodes it in a URL-safe format.
     */
    public String encodeEmail(String email) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encrypted = cipher.doFinal(email.getBytes(StandardCharsets.UTF_8));

        return Base64.getUrlEncoder().withoutPadding().encodeToString(encrypted);
    }

    /**
     * Decrypts an email address from its AES encrypted form.
     */
    public String decodeEmail(String encryptedEmail) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decodedBytes = Base64.getUrlDecoder().decode(encryptedEmail);

        return new String(cipher.doFinal(decodedBytes), StandardCharsets.UTF_8);
    }

}
