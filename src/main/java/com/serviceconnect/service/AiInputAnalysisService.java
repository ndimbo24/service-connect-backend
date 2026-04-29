package com.serviceconnect.service;

import com.serviceconnect.dto.response.AiInputAnalysisResponse;
import com.serviceconnect.util.SwahiliResponseRewriter;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Service that analyzes user input (text, voice, image) to detect the type of
 * service needed and suggest the appropriate technician.
 *
 * <p><b>Transformation Layer:</b></p>
 * Although the system accepts both Swahili and English input, the final response
 * is ALWAYS rewritten into simple, everyday Swahili by
 * {@link SwahiliResponseRewriter#rewriteToSimpleSwahili(String, String)} so that
 * normal Tanzanian users receive friendly, non-technical output.
 */
@Service
public class AiInputAnalysisService {

    /**
     * Analyzes the user request and returns a response where both the technical
     * description and the user-friendly description are rewritten into simple Swahili.
     *
     * @param text                text description from the user
     * @param voiceText           voice-to-text transcription
     * @param imageData           base64 image data (optional)
     * @param selectedServiceType service type pre-selected by the user (optional)
     * @param language            language code from the Accept-Language header
     * @return AiInputAnalysisResponse with simple Swahili descriptions
     */
    public AiInputAnalysisResponse analyze(String text, String voiceText, String imageData, String selectedServiceType, String language) {
        String merged = ((text == null ? "" : text) + " " + (voiceText == null ? "" : voiceText)).toLowerCase(Locale.ROOT);

        // Start from unknown — prioritize the user's actual description over the selected service
        String category = "unknown";
        String confidence = "LOW";

        if (containsAny(merged, "pipe", "maji", "leak", "sink", "drain", "bomba", "mto")) {
            category = "plumbing";
            confidence = "HIGH";
        } else if (containsAny(merged, "gesi", "gas", "burner", "cylinder", "kibiriti")) {
            category = "gas";
            confidence = "HIGH";
        } else if (containsAny(merged, "stove", "jiko", "oven", "cooker", "kochi")) {
            category = "stove";
            confidence = "HIGH";
        } else if (containsAny(merged, "power", "eme", "wiring", "socket", "electric", "umeme", "taa", "switch")) {
            category = "electrical";
            confidence = "HIGH";
        } else if (containsAny(merged, "car", "engine", "vehicle", "automotive", "brake", "gari", "break")) {
            category = "automotive";
            confidence = "HIGH";
        } else if (containsAny(merged, "ujenzi", "construction", "building", "fundation", "mchanga", "tile", "bati", "cement")) {
            category = "construction";
            confidence = "HIGH";
        } else if (containsAny(merged, "seremala", "carpentry", "furniture", "mbao", "wood", "door", "window", "dirisha", "kiti")) {
            category = "carpentry";
            confidence = "HIGH";
        } else if (containsAny(merged, "viyoyozi", "ac", "air condition", "cooling", "fridge", "baridi", "refrigerator", "freezer")) {
            category = "ac_repair";
            confidence = "HIGH";
        } else if (containsAny(merged, "rangi", "paint", "painting", "kupaka", "wall", "ukuta", "color", "nyumba")) {
            category = "painting";
            confidence = "HIGH";
        } else if (containsAny(merged, "kuchomelea", "welding", "chuma", "metal", "iron", "gate", "mlango", "chomelea")) {
            category = "welding";
            confidence = "HIGH";
        } else if (containsAny(merged, "solar", "panel", "nishati", "energy", "jua", "sun", "battery", "pv")) {
            category = "solar";
            confidence = "HIGH";
        } else if (containsAny(merged, "mapambo", "decoration", "design", "interior", "pambo", "remodel", "paint")) {
            category = "decoration";
            confidence = "HIGH";
        }

        // Fallback to selected service only when the text truly gives no clues
        if ("unknown".equals(category) && selectedServiceType != null && !selectedServiceType.isBlank()) {
            category = selectedServiceType;
            confidence = imageData != null && !imageData.isBlank() ? "LOW" : "MEDIUM";
        }

        // ------------------------------------------------------------------
        // RESPONSE TRANSFORMATION LAYER
        // ------------------------------------------------------------------
        // 1. Generate the original English/technical description (internal only)
        String originalTechnical = getTechnicalDescription(category);
        String originalFriendly  = getUserFriendlyDescription(category);

        // 2. Rewrite both into simple Swahili before sending to the frontend
        String swahiliProblem      = SwahiliResponseRewriter.rewriteToSimpleSwahili(originalTechnical, category);
        String swahiliTechnician   = SwahiliResponseRewriter.getSimpleTechnicianDescription(category);

        return AiInputAnalysisResponse.builder()
                .detectedCategory(category)
                .suggestedTechnicianType(category)
                .confidence(confidence)
                .description(swahiliProblem)          // simple explanation of the problem
                .userFriendlyDescription(swahiliTechnician) // type of technician needed
                .build();
    }

    // Backward compatibility method for calls without language parameter
    public AiInputAnalysisResponse analyze(String text, String voiceText, String imageData, String selectedServiceType) {
        return analyze(text, voiceText, imageData, selectedServiceType, "en");
    }

    private boolean containsAny(String content, String... keywords) {
        for (String keyword : keywords) {
            if (content.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the original English technical description.
     * This is used as input to the transformation layer and is never sent directly to the user.
     */
    private String getTechnicalDescription(String category) {
        return switch (category) {
            case "plumbing" -> "Water system repairs including pipes, leaks, drains, and fixtures";
            case "gas" -> "Gas appliance installation, repair, and safety checks";
            case "stove" -> "Cooking appliance repair and maintenance";
            case "electrical" -> "Electrical wiring, outlets, switches, and power systems";
            case "automotive" -> "Vehicle repair and maintenance services";
            case "construction" -> "Building construction and structural work";
            case "carpentry" -> "Woodworking, furniture repair, and carpentry services";
            case "ac_repair" -> "Air conditioning and refrigeration system repair";
            case "painting" -> "Interior and exterior painting services";
            case "welding" -> "Metal fabrication and welding services";
            case "solar" -> "Solar panel installation and renewable energy systems";
            case "decoration" -> "Interior design and home decoration services";
            default -> "General repair and maintenance service";
        };
    }

    /**
     * Returns the original English user-friendly description.
     * This is used as input to the transformation layer and is never sent directly to the user.
     */
    private String getUserFriendlyDescription(String category) {
        return switch (category) {
            case "plumbing" -> "Expert plumber to fix your water pipes, leaks, and bathroom fixtures";
            case "gas" -> "Professional gas technician for safe appliance installation and repairs";
            case "stove" -> "Cooking expert to repair your stove, oven, or kitchen appliances";
            case "electrical" -> "Certified electrician for all your wiring and electrical needs";
            case "automotive" -> "Skilled mechanic to service and repair your vehicle";
            case "construction" -> "Professional builder for construction and renovation work";
            case "carpentry" -> "Expert carpenter for furniture repair and woodworking";
            case "ac_repair" -> "AC specialist to cool down your home or office";
            case "painting" -> "Professional painter to beautify your space";
            case "welding" -> "Metal expert for fabrication and welding services";
            case "solar" -> "Solar specialist for clean energy installation";
            case "decoration" -> "Interior designer to transform your living space";
            default -> "Skilled professional for your repair and maintenance needs";
        };
    }
}

