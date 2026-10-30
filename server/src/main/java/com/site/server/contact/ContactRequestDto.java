package com.site.server.contact;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ContactRequestDto(
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 80, message = "Name must be between 2 and 80 characters")
        String fullName,

        @NotBlank(message = "Phone is required")
        @Pattern(
                regexp = "^(?:0|\\+972)[ -]?(?:(?:5[0-9](?:[ -]?[0-9]){7}|[23489](?:[ -]?[0-9]){7}))$",
                message = "Enter a valid Israeli mobile or landline number"
        )
        String phone,

        @Email(message = "Enter a valid email address")
        @Size(max = 254, message = "Email must be at most 254 characters")
        String email,

        @Size(max = 60, message = "City must be at most 60 characters")
        String city,

        @NotNull(message = "Work type is required")
        WorkType workType,

        @Size(max = 1000, message = "Description must be at most 1000 characters")
        String description
) {
    public ContactRequestDto {
        fullName = trim(fullName);
        phone = trim(phone);
        email = blankToNull(email);
        city = blankToNull(city);
        description = blankToNull(description);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String blankToNull(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isEmpty() ? null : trimmed;
    }

    public String normalizedPhone() {
        String digits = phone.replaceAll("[ -]", "");
        if (digits.startsWith("+972")) {
            return "0" + digits.substring(4);
        }
        return digits;
    }
}