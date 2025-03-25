package com.example.email;

import com.example.assessment.service.AssessmentService;
import com.example.config.AppConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
@Controller
public class EmailController {
    @Autowired
    private final EmailService emailService;
    @Autowired
    private final AssessmentService assessmentService;


//    Changing this to the deploy server url later or change this according to the port of ur docker-compose
//    private final String inviteUrlHeader = "https://group-02.cookie-candy.id.vn/assessment/invite/";
        @Value("${invite.url.header}")
        private String inviteUrlHeader;
  //  private final String inviteUrlHeader = "https://java02.fsa.io.vn/assessments/invite/";

    @Autowired
    public EmailController(EmailService emailService, AssessmentService assessmentService) {
        this.emailService = emailService;
        this.assessmentService = assessmentService;
    }

    @PostMapping("/send-invitations")
    public String sendInvitations(
            @RequestParam("emails") String emails,
            @RequestParam("assessmentId") Long assessmentId,
              @RequestParam("expirationDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime expirationDate,
            Model model) {

        List<String> emailList = Arrays.asList(emails.split("\\s*,\\s*"));
        LocalDateTime invitationDate = LocalDateTime.now();

        // Ensure expiration date is after invitation date
        if (expirationDate.isBefore(invitationDate)) {
            model.addAttribute("error", "Expiration date must be after the invitation date.");
            return "invite";  // Return back to the invite page with error
        }


        // Send invitations
          emailService.sendAssessmentInvite(emailList, assessmentId, expirationDate);
        // Store invited emails with full date-time values
        assessmentService.storeInvitedEmail(assessmentId, emailList, invitationDate, expirationDate);
        System.out.println("Invited email sent: "+ emailList);

        model.addAttribute("message", "Invitations sent successfully!");
        return "redirect:/assessments";
    }
}

