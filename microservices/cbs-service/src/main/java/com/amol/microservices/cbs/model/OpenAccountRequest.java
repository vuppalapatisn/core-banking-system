package com.amol.microservices.cbs.model;

import com.amol.microservices.cbs.domain.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Request to open a CASA account for a customer. */
public record OpenAccountRequest(
        @NotBlank(message = "customerId is required") String customerId,
        @NotNull(message = "type is required (CURRENT or SAVINGS)") AccountType type,
        String currency) {
}
