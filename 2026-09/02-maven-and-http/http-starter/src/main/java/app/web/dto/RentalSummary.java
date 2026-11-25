package app.web.dto;

import java.time.LocalDateTime;

public record RentalSummary(
        String id,
        String carId,
        String carBrand,
        String carModel,
        boolean active,
        Double totalCost,
        LocalDateTime startedAt,
        LocalDateTime endedAt
) {}
