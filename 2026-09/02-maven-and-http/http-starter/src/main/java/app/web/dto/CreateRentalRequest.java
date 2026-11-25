package app.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRentalRequest(
        @Size(max = 64) String id,
        @NotBlank @Size(max = 64) String carId,
        @NotBlank @Size(max = 64) String driverId
) {}
