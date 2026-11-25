package app.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateCarRequest(
        @Size(max = 100) String brand,
        @Size(max = 100) String model,
        @Size(max = 32) String plateNumber,
        @Min(1990) Integer productionYear,
        @Min(0) Double mileage
) {}
