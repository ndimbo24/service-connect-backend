package com.serviceconnect.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiInputAnalysisResponse {
    private String detectedCategory;
    private String suggestedTechnicianType;
    private String confidence;
    private String description;
    private String userFriendlyDescription;

    // Explicit getters for compiler reliability
    public String getDetectedCategory() { return detectedCategory; }
    public String getSuggestedTechnicianType() { return suggestedTechnicianType; }
    public String getConfidence() { return confidence; }
    public String getDescription() { return description; }
    public String getUserFriendlyDescription() { return userFriendlyDescription; }
}
