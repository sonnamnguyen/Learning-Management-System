package com.example.code_judgement.AI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/code")
public class CodeReviewController {

    private final AiService aiService;

    @Autowired
    public CodeReviewController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/review")
    public ResponseEntity<AiReviewResponse> reviewCode(@RequestBody AiReviewRequest request) {
        try {
            // Get AI response from the service with language parameter
            String aiResponse = aiService.getAiResponse(request.getCode(), request.getLanguage());

            // Return successful response
            return ResponseEntity.ok(new AiReviewResponse(aiResponse, null));
        } catch (Exception e) {
            // Handle errors and return error response
            return ResponseEntity.ok(new AiReviewResponse(null, "Error: " + e.getMessage()));
        }
    }
}
