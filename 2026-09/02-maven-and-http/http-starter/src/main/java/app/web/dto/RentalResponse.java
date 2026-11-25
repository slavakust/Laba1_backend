package app.web.dto;

import java.time.LocalDateTime;

public record RentalResponse(
        String id,
        boolean active,
        Long hours,
        Double distance,
        Double totalCost,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        CarSummary car,
        DriverSummary driver
) {}
