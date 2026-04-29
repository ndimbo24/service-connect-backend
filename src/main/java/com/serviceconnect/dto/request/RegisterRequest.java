package com.serviceconnect.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class RegisterRequest {
    @NotBlank(message = "Name is required")
    @Size(min = 2, message = "Name must be at least 2 characters")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "Name must contain only letters and spaces")
    private String name;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^\\d{10}$", message = "Phone must be exactly 10 digits")
    private String phone;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
             message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character")
    private String password;

    @NotBlank(message = "Role is required")
    private String role;

    private String email;

    // Technician-specific fields
    private List<String> serviceTypes;
    private TechnicianLocationData location;

    @Data
    public static class TechnicianLocationData {
        @NotBlank(message = "Region is required")
        @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "Region must contain only letters and spaces")
        private String region;

        @NotBlank(message = "District is required")
        @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "District must contain only letters and spaces")
        private String district;

        @NotBlank(message = "Street is required")
        @Pattern(regexp = "^[a-zA-Z0-9\\s]+$", message = "Street must contain only letters, numbers, and spaces")
        private String street;
    }
}
