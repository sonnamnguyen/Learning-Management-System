package com.example.code_judgement.AI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

@Service
public class AiService {
    @Value("${ai.endpoint}")
    private String API_URL;

    @Value("${ai.authentication.username}")
    private String USERNAME;

    @Value("${ai.authentication.password}")
    private String PASSWORD;

    @Value("${ai.model}")
    private String AI_MODEL;

    private final RestTemplate restTemplate;

    public AiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // AiService.java
    public String getAiResponse(String code, String language) {
        // Tạo headers với Basic Authentication
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + encodeCredentials(USERNAME, PASSWORD));

        // Tạo prompt
        String prompt;
        if ("vi".equals(language)) {
            prompt = "Hãy đánh giá đoạn code sau về tính chính xác, hiệu quả và các phương pháp tốt nhất:\n\n" + code;
        } else {
            prompt = "Please review the following code for correctness, efficiency, and best practices:\n\n" + code;
        }
        // Chuẩn bị payload JSON
        AiRequest request = new AiRequest(AI_MODEL, prompt, false);

        HttpEntity<AiRequest> entity = new HttpEntity<>(request, headers);

        try {
            // Gửi request đến API và nhận phản hồi
            ResponseEntity<AiResponse> response = restTemplate.exchange(
                    API_URL, HttpMethod.POST, entity, AiResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody().getResponse();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to connect to AI service.";
        }

        return "No response from AI.";
    }

    // Hàm mã hóa username:password thành Base64 để dùng trong Basic Auth
    private String encodeCredentials(String username, String password) {
        String credentials = username + ":" + password;
        return Base64.getEncoder().encodeToString(credentials.getBytes());
    }
}