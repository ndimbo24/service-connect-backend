package com.serviceconnect.util;

import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Utility class that transforms AI-generated technical responses into simple,
 * everyday Swahili that normal Tanzanian users can easily understand.
 *
 * <p>System Prompt (embedded logic):</p>
 * <ul>
 *   <li>Use simple Swahili (Kiswahili cha kawaida)</li>
 *   <li>Avoid technical terms completely</li>
 *   <li>Keep responses short and clear</li>
 *   <li>Sound like a normal person, not an expert</li>
 *   <li>Always output in Swahili, regardless of input language</li>
 * </ul>
 */
@Component
public class SwahiliResponseRewriter {

    /**
     * The system prompt that guides the transformation behavior.
     * This is kept as a documented constant so it can be reused
     * if an external LLM is introduced later.
     */
    public static final String SYSTEM_PROMPT =
            "Wewe ni msaada wa karibu anayezungumza Kiswahili cha kawaida. " +
            "Tumia maneno rahisi kabisa. Epuka maneno ya kitaalamu. " +
            "Jibu kwa ufupi na kwa lugha ya kawaida kama mtu wa kila siku. " +
            "Sema kama mtu wa karibu, sio kama mtaalamu.";

    /**
     * Takes the original AI response (usually English/technical) and rewrites it
     * into simple Swahili that a normal Tanzanian user can understand.
     *
     * @param aiResponse the original technical description from the AI
     * @param category   the detected service category (e.g., plumbing, electrical)
     * @return a short, simple Swahili sentence describing the problem and the needed technician
     */
    public static String rewriteToSimpleSwahili(String aiResponse, String category) {
        if (aiResponse == null || aiResponse.isBlank()) {
            return "Samahani, hatukuweza kuelewa tatizo lako. Tafadhali jaribu tena.";
        }

        String problem = getSimpleProblemDescription(category);
        String technician = getSimpleTechnicianDescription(category);

        return problem + " " + technician;
    }

    /**
     * Returns a simple Swahili explanation of the problem.
     */
    private static String getSimpleProblemDescription(String category) {
        return switch (normalize(category)) {
            case "plumbing"    -> "Inaonekana kuna tatizo la maji.";
            case "gas"         -> "Kuna shida na gesi.";
            case "stove"       -> "Kuna shida na jiko.";
            case "electrical"  -> "Kuna shida ya umeme.";
            case "automotive"  -> "Gari yako ina matatizo.";
            case "construction"-> "Kuna shida ya ujenzi.";
            case "carpentry"   -> "Kuna shida ya samani au mbao.";
            case "ac_repair"   -> "Kuna shida na baridi au joto.";
            case "painting"    -> "Kuna haja ya kupaka rangi.";
            case "welding"     -> "Kuna shida ya chuma.";
            case "solar"       -> "Kuna shida na jua la kupatikana.";
            case "decoration"  -> "Kuna haja ya kupamba nyumba.";
            default            -> "Tatizo halijafahamika vizuri.";
        };
    }

    /**
     * Returns a simple Swahili statement of the type of technician needed.
     */
    public static String getSimpleTechnicianDescription(String category) {
        return switch (normalize(category)) {
            case "plumbing"    -> "Unahitaji fundi bomba aangalie hilo tatizo.";
            case "gas"         -> "Unahitaji fundi gesi aangalie hilo tatizo.";
            case "stove"       -> "Unahitaji fundi jiko aangalie hilo tatizo.";
            case "electrical"  -> "Unahitaji fundi umeme aangalie hilo tatizo.";
            case "automotive"  -> "Unahitaji fundi gari aangalie hilo tatizo.";
            case "construction"-> "Unahitaji fundi ujenzi aangalie hilo tatizo.";
            case "carpentry"   -> "Unahitaji fundi mbao aangalie hilo tatizo.";
            case "ac_repair"   -> "Unahitaji fundi baridi aangalie hilo tatizo.";
            case "painting"    -> "Unahitaji fundi rangi aangalie hilo tatizo.";
            case "welding"     -> "Unahitaji fundi chuma aangalie hilo tatizo.";
            case "solar"       -> "Unahitaji fundi solar aangalie hilo tatizo.";
            case "decoration"  -> "Unahitaji fundi mapambo aangalie hilo tatizo.";
            default            -> "Tutakutafutia fundi sahihi.";
        };
    }

    /**
     * Normalizes a category string to lower-case and trims whitespace.
     */
    private static String normalize(String category) {
        if (category == null) {
            return "";
        }
        return category.toLowerCase(Locale.ROOT).trim();
    }
}

