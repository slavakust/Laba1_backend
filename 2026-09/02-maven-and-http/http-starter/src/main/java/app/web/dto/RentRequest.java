package app.web.dto;

import jakarta.validation.constraints.NotBlank;

public record RentRequest(
        @NotBlank String driverId
) {}
