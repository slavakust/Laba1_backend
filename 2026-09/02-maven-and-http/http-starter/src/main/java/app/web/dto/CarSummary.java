package app.web.dto;

import app.model.CarStatus;
import app.model.CarType;

public record CarSummary(
        String id,
        String brand,
        String model,
        String plateNumber,
        CarType type,
        CarStatus status
) {}
