package app.model;

public class RentalInfo {
    public record RentRequest(String driverId) {}

    public record ReturnRequest(Long hours, Double distance) {}

    public record RentResult(String carId, String driverId, CarType carType, String message) {}

    public record ReturnResult(String carId, String driverId, double totalCost, double mileage, String message) {}

    public record ErrorResponse(int status, String error, String message) {}
}
