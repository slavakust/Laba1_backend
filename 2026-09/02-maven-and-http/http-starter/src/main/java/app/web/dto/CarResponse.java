package app.web.dto;

import app.model.CarStatus;
import app.model.CarType;

public record CarResponse(
        String id,
        String brand,
        String model,
        String plateNumber,
        int productionYear,
        double mileage,
        CarStatus status,
        CarType type,
        int minAge,
        int minExperience,
        boolean depositRequired,
        double hourlyRate,
        double freeKmPerHour,
        double extraKmRate,
        String driverId,
        DriverSummary driver
) {}
