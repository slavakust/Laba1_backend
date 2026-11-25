package app.web.dto;

public record ReturnResponse(
        String rentalId,
        String carId,
        String driverId,
        double totalCost,
        double mileage,
        String message
) {}
