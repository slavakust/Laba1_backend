package app.web.dto;

import app.model.CarType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCarRequest(
        @Size(max = 64) String id,
        @NotBlank @Size(max = 100) String brand,
        @NotBlank @Size(max = 100) String model,
        @NotBlank @Size(max = 32) String plateNumber,
        @NotNull @Min(1990) Integer productionYear,
        @NotNull @Min(0) Double mileage,
        @NotNull CarType type
) {}
