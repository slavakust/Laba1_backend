package app.web.dto;

import jakarta.validation.constraints.Min;

public record UpdateCarRequest(
        String brand,
        String model,
        String plateNumber,
        @Min(1990) Integer productionYear,
        @Min(0) Double mileage
) {}
