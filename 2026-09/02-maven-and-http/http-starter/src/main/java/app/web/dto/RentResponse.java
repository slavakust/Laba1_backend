package app.web.dto;

import app.model.CarType;

public record RentResponse(
        String carId,
        String driverId,
        CarType carType,
        String message
) {}
