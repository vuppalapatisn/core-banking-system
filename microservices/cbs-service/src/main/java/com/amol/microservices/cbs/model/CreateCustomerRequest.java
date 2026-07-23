package com.amol.microservices.cbs.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Request to onboard a customer. */
public record CreateCustomerRequest(
        @NotBlank(message = "name is required") String name,
        @NotBlank(message = "email is required") @Email(message = "email must be valid") String email) {
}
