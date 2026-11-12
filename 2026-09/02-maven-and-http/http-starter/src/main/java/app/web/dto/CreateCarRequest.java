package app.web.dto;

import app.model.CarType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCarRequest(
        String id,
        @NotBlank String brand,
        @NotBlank String model,
        @NotBlank String plateNumber,
        @NotNull @Min(1990) Integer productionYear,
        @NotNull @Min(0) Double mileage,
        @NotNull CarType type
) {}
