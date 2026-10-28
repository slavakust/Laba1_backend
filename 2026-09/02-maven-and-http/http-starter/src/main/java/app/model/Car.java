package app.model;

import app.exception.ValidationException;

public abstract class Car {
    private String id;
    private String brand;
    private String model;
    private String plateNumber;
    private int productionYear;
    private double mileage;
    private CarStatus status;
    private String driverId;

    public Car() {
        this.status = CarStatus.AVAILABLE;
    }

    public Car(String id, String brand, String model, String plateNumber, int productionYear, double mileage) {
        this.id = id;
        this.brand = brand;
        this.model = model;
        this.plateNumber = plateNumber;
        this.productionYear = productionYear;
        this.mileage = mileage;
        this.status = CarStatus.AVAILABLE;
        this.driverId = null;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getPlateNumber() { return plateNumber; }
    public void setPlateNumber(String plateNumber) { this.plateNumber = plateNumber; }
    public int getProductionYear() { return productionYear; }
    public void setProductionYear(int productionYear) { this.productionYear = productionYear; }
    public double getMileage() { return mileage; }
    public void setMileage(double mileage) { this.mileage = mileage; }
    public CarStatus getStatus() { return status; }
    public void setStatus(CarStatus status) { this.status = status; }
    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }

    public abstract CarType getType();
    public abstract int getMinAge();
    public abstract int getMinExperience();
    public abstract boolean isDepositRequired();
    public abstract double getHourlyRate();
    public abstract double getFreeKmPerHour();
    public abstract double getExtraKmRate();

    public void validateDriver(Driver driver) {
        if (driver == null) {
            throw new ValidationException("Driver is required");
        }
        if (driver.getAge() < getMinAge()) {
            throw new ValidationException("Driver age " + driver.getAge() + " is below minimum " + getMinAge() + " for " + getType());
        }
        if (driver.getExperience() < getMinExperience()) {
            throw new ValidationException("Driver experience " + driver.getExperience() + " is below minimum " + getMinExperience() + " for " + getType());
        }
        if (isDepositRequired() && !driver.isHasDeposit()) {
            throw new ValidationException("Deposit is required for car type " + getType());
        }
    }

    public double calculateCost(long hours, double distance) {
        long effectiveHours = Math.max(1, hours);
        double base = effectiveHours * getHourlyRate();
        double freeKm = effectiveHours * getFreeKmPerHour();
        double extraKm = Math.max(0.0, distance - freeKm);
        double extraCost = extraKm * getExtraKmRate();
        return Math.round((base + extraCost) * 100.0) / 100.0;
    }
}
