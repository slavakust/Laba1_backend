package app.web.dto;

import java.util.List;

public record DriverResponse(
        String id,
        String name,
        String email,
        int age,
        int experience,
        boolean hasDeposit,
        int rentalCount,
        String carId,
        CarSummary activeCar,
        List<RentalSummary> rentals
) {}
