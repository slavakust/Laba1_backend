package app.model;

import app.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CarTest {

    @Test
    void testEconomyCarRulesAndCostCalculation() {
        Car car = new EconomyCar("c1", "Kia", "Rio", "A111AA", 2021, 1000.0);
        assertEquals(CarType.ECONOMY, car.getType());
        assertEquals(18, car.getMinAge());
        assertEquals(0, car.getMinExperience());
        assertFalse(car.isDepositRequired());

        double costWithin = car.calculateCost(2, 30.0);
        assertEquals(30.0, costWithin);

        double costExtra = car.calculateCost(2, 50.0);
        assertEquals(80.0, costExtra);
    }

    @Test
    void testPremiumCarRulesAndCostCalculation() {
        Car car = new PremiumCar("c2", "Porsche", "Panamera", "P777PP", 2024, 2000.0);
        assertEquals(CarType.PREMIUM, car.getType());
        assertEquals(23, car.getMinAge());
        assertEquals(2, car.getMinExperience());
        assertTrue(car.isDepositRequired());

        double costWithin = car.calculateCost(2, 50.0);
        assertEquals(100.0, costWithin);

        double costExtra = car.calculateCost(2, 80.0);
        assertEquals(400.0, costExtra);
    }

    @Test
    void testValidateDriverEconomySuccess() {
        Car car = new EconomyCar("c1", "Kia", "Rio", "A111AA", 2021, 1000.0);
        Driver driver = new Driver("d1", "Alice", "alice@test.com", 18, 0, false);
        assertDoesNotThrow(() -> car.validateDriver(driver));
    }

    @Test
    void testValidateDriverEconomyUnderageThrows() {
        Car car = new EconomyCar("c1", "Kia", "Rio", "A111AA", 2021, 1000.0);
        Driver driver = new Driver("d1", "Underage", "u@test.com", 17, 0, false);
        assertThrows(ValidationException.class, () -> car.validateDriver(driver));
    }

    @Test
    void testValidateDriverPremiumChecks() {
        Car car = new PremiumCar("c2", "Porsche", "Panamera", "P777PP", 2024, 2000.0);

        Driver young = new Driver("d1", "Young", "y@test.com", 22, 3, true);
        assertThrows(ValidationException.class, () -> car.validateDriver(young));

        Driver inexperienced = new Driver("d2", "NoExp", "ne@test.com", 25, 1, true);
        assertThrows(ValidationException.class, () -> car.validateDriver(inexperienced));

        Driver noDeposit = new Driver("d3", "NoDep", "nd@test.com", 25, 3, false);
        assertThrows(ValidationException.class, () -> car.validateDriver(noDeposit));

        Driver valid = new Driver("d4", "Valid", "v@test.com", 25, 3, true);
        assertDoesNotThrow(() -> car.validateDriver(valid));
    }

    @Test
    void testValidateDriverNullThrows() {
        Car car = new EconomyCar("c1", "Kia", "Rio", "A111AA", 2021, 1000.0);
        assertThrows(ValidationException.class, () -> car.validateDriver(null));
    }
}
