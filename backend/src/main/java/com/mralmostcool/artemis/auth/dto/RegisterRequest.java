package com.mralmostcool.artemis.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "First name is required") @Size(max = 100) String firstName,

        @NotBlank(message = "Last name is required") @Size(max = 100) String lastName,

        @NotBlank(message = "Email is required") @Size(max = 255) @Email(message = "Email is not valid") String email,

        @NotBlank(message = "Password is required") @Size(min = 8, max = 255, message = "Password must be at least 8 characters long") String password) {
}
