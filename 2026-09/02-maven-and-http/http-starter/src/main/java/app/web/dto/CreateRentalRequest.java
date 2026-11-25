package app.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateRentalRequest(
        String id,
        @NotBlank String carId,
        @NotBlank String driverId
) {}
