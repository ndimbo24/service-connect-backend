package com.serviceconnect.controller;

import com.serviceconnect.dto.request.CreateServiceRequest;
import com.serviceconnect.dto.response.AiInputAnalysisResponse;
import com.serviceconnect.dto.response.ApiResponse;
import com.serviceconnect.service.AiInputAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiAssistantController {

    private final AiInputAnalysisService aiInputAnalysisService;

    @PostMapping("/analyze-request")
    public ResponseEntity<ApiResponse<AiInputAnalysisResponse>> analyzeRequestInput(
            @RequestBody CreateServiceRequest request,
            @RequestHeader(value = "Accept-Language", defaultValue = "en") String language) {
        // Extract language code (first 2 characters, e.g., 'sw' from 'sw-KE')
        String langCode = language.length() >= 2 ? language.substring(0, 2).toLowerCase() : "en";
        
        AiInputAnalysisResponse analysis = aiInputAnalysisService.analyze(
                request.getDescription(),
                request.getVoiceText(),
                request.getImageData(),
                request.getServiceType(),
                langCode);
        return ResponseEntity.ok(ApiResponse.success(analysis));
    }
}
