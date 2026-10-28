package app.model;

public class EconomyCar extends Car {

    public EconomyCar() {}

    public EconomyCar(String id, String brand, String model, String plateNumber, int productionYear, double mileage) {
        super(id, brand, model, plateNumber, productionYear, mileage);
    }

    @Override
    public CarType getType() {
        return CarType.ECONOMY;
    }

    @Override
    public int getMinAge() {
        return 18;
    }

    @Override
    public int getMinExperience() {
        return 0;
    }

    @Override
    public boolean isDepositRequired() {
        return false;
    }

    @Override
    public double getHourlyRate() {
        return 15.0;
    }

    @Override
    public double getFreeKmPerHour() {
        return 20.0;
    }

    @Override
    public double getExtraKmRate() {
        return 5.0;
    }
}
