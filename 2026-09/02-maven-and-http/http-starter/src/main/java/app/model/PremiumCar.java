package app.model;

public class PremiumCar extends Car {

    public PremiumCar() {}

    public PremiumCar(String id, String brand, String model, String plateNumber, int productionYear, double mileage) {
        super(id, brand, model, plateNumber, productionYear, mileage);
    }

    @Override
    public CarType getType() {
        return CarType.PREMIUM;
    }

    @Override
    public int getMinAge() {
        return 23;
    }

    @Override
    public int getMinExperience() {
        return 2;
    }

    @Override
    public boolean isDepositRequired() {
        return true;
    }

    @Override
    public double getHourlyRate() {
        return 50.0;
    }

    @Override
    public double getFreeKmPerHour() {
        return 30.0;
    }

    @Override
    public double getExtraKmRate() {
        return 15.0;
    }
}
