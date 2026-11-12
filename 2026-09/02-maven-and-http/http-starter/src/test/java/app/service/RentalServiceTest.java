package app.service;

import app.exception.ConflictException;
import app.exception.NotFoundException;
import app.exception.ValidationException;
import app.model.*;
import app.repository.CarRepository;
import app.repository.DriverRepository;
import app.web.dto.RentResponse;
import app.web.dto.ReturnResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class RentalServiceTest {

    private CarRepository carRepository;
    private DriverRepository driverRepository;
    private RentalService service;

    @BeforeEach
    void setUp() {
        carRepository = new CarRepository();
        driverRepository = new DriverRepository();
        service = new RentalService(carRepository, driverRepository);
    }

    @Test
    void testCreateCarSuccess() {
        Car car = service.createCar("c-1", "Kia", "Rio", "A111AA", 2021, 1000.0, CarType.ECONOMY);
        assertNotNull(car);
        assertEquals("c-1", car.getId());
        assertEquals("Kia", car.getBrand());
        assertEquals("Rio", car.getModel());
        assertEquals("A111AA", car.getPlateNumber());
        assertEquals(2021, car.getProductionYear());
        assertEquals(1000.0, car.getMileage());
        assertEquals(CarStatus.AVAILABLE, car.getStatus());
        assertEquals(CarType.ECONOMY, car.getType());
    }

    @Test
    void testCreateCarDuplicatePlateThrowsConflict() {
        service.createCar("c-1", "Kia", "Rio", "A111AA", 2021, 1000.0, CarType.ECONOMY);
        assertThrows(ConflictException.class, () ->
                service.createCar("c-2", "Hyundai", "Solaris", "a111aa", 2022, 500.0, CarType.ECONOMY));
    }

    @Test
    void testCreateCarDuplicateIdThrowsConflict() {
        service.createCar("c-1", "Kia", "Rio", "A111AA", 2021, 1000.0, CarType.ECONOMY);
        assertThrows(ConflictException.class, () ->
                service.createCar("c-1", "Hyundai", "Solaris", "B222BB", 2022, 500.0, CarType.ECONOMY));
    }

    @Test
    void testCreateCarInvalidInputsThrowValidation() {
        assertThrows(ValidationException.class, () ->
                service.createCar("c-1", "", "Rio", "A111AA", 2021, 1000.0, CarType.ECONOMY));
        assertThrows(ValidationException.class, () ->
                service.createCar("c-1", "Kia", " ", "A111AA", 2021, 1000.0, CarType.ECONOMY));
        assertThrows(ValidationException.class, () ->
                service.createCar("c-1", "Kia", "Rio", "", 2021, 1000.0, CarType.ECONOMY));
        assertThrows(ValidationException.class, () ->
                service.createCar("c-1", "Kia", "Rio", "A111AA", 1980, 1000.0, CarType.ECONOMY));
        assertThrows(ValidationException.class, () ->
                service.createCar("c-1", "Kia", "Rio", "A111AA", 2099, 1000.0, CarType.ECONOMY));
        assertThrows(ValidationException.class, () ->
                service.createCar("c-1", "Kia", "Rio", "A111AA", 2021, -1.0, CarType.ECONOMY));
        assertThrows(ValidationException.class, () ->
                service.createCar("c-1", "Kia", "Rio", "A111AA", 2021, 1000.0, null));
    }

    @Test
    void testUpdateCarSuccess() {
        service.createCar("c-1", "Kia", "Rio", "A111AA", 2021, 1000.0, CarType.ECONOMY);
        Car updated = service.updateCar("c-1", "Kia Motors", "Rio X", "A111AB", 2022, 1200.0);
        assertEquals("Kia Motors", updated.getBrand());
        assertEquals("Rio X", updated.getModel());
        assertEquals("A111AB", updated.getPlateNumber());
        assertEquals(2022, updated.getProductionYear());
        assertEquals(1200.0, updated.getMileage());
    }

    @Test
    void testUpdateCarAtomicityOnValidationFailure() {
        service.createCar("c-1", "Kia", "Rio", "A111AA", 2021, 1000.0, CarType.ECONOMY);

        assertThrows(ValidationException.class, () ->
                service.updateCar("c-1", "ChangedBrand", "ChangedModel", "A111AA", 1980, 1000.0));

        Car car = service.getCar("c-1");
        assertEquals("Kia", car.getBrand());
        assertEquals("Rio", car.getModel());
        assertEquals(2021, car.getProductionYear());
    }

    @Test
    void testUpdateCarDuplicatePlateThrowsConflict() {
        service.createCar("c-1", "Kia", "Rio", "A111AA", 2021, 1000.0, CarType.ECONOMY);
        service.createCar("c-2", "Toyota", "Camry", "B222BB", 2022, 2000.0, CarType.ECONOMY);

        assertThrows(ConflictException.class, () ->
                service.updateCar("c-2", null, null, "a111aa", null, null));
    }

    @Test
    void testUpdateCarRentedThrowsConflict() {
        service.createCar("c-1", "Kia", "Rio", "A111AA", 2021, 1000.0, CarType.ECONOMY);
        service.createDriver("d-1", "Alice", "alice@test.com", 20, 1, false);
        service.rentCar("c-1", "d-1");

        assertThrows(ConflictException.class, () ->
                service.updateCar("c-1", "NewBrand", null, null, null, null));
    }

    @Test
    void testDeleteCarSuccess() {
        service.createCar("c-1", "Kia", "Rio", "A111AA", 2021, 1000.0, CarType.ECONOMY);
        service.deleteCar("c-1");
        assertThrows(NotFoundException.class, () -> service.getCar("c-1"));
    }

    @Test
    void testDeleteCarRentedThrowsConflict() {
        service.createCar("c-1", "Kia", "Rio", "A111AA", 2021, 1000.0, CarType.ECONOMY);
        service.createDriver("d-1", "Alice", "alice@test.com", 20, 1, false);
        service.rentCar("c-1", "d-1");

        assertThrows(ConflictException.class, () -> service.deleteCar("c-1"));
    }

    @Test
    void testListCarsFiltering() {
        service.createCar("c-1", "Kia", "Rio", "A111AA", 2020, 1000.0, CarType.ECONOMY);
        service.createCar("c-2", "Porsche", "Panamera", "P777PP", 2024, 2000.0, CarType.PREMIUM);
        service.createDriver("d-1", "Alice", "alice@test.com", 20, 1, false);
        service.rentCar("c-1", "d-1");

        assertEquals(1, service.listCars(CarType.PREMIUM, null, null, null).size());
        assertEquals(1, service.listCars(null, CarStatus.RENTED, null, null).size());
        assertEquals(1, service.listCars(null, CarStatus.AVAILABLE, null, null).size());
        assertEquals(1, service.listCars(null, null, 2022, null).size());
        assertEquals(1, service.listCars(null, null, null, "panamera").size());
        assertEquals(2, service.listCars(null, null, null, null).size());
    }

    @Test
    void testCreateDriverSuccess() {
        Driver driver = service.createDriver("d-1", "Bob", "bob@test.com", 30, 7, true);
        assertNotNull(driver);
        assertEquals("d-1", driver.getId());
        assertEquals("Bob", driver.getName());
        assertEquals("bob@test.com", driver.getEmail());
        assertEquals(30, driver.getAge());
        assertEquals(7, driver.getExperience());
        assertTrue(driver.isHasDeposit());
        assertNull(driver.getCarId());
    }

    @Test
    void testCreateDriverDuplicateEmailThrowsConflict() {
        service.createDriver("d-1", "Bob", "bob@test.com", 30, 7, true);
        assertThrows(ConflictException.class, () ->
                service.createDriver("d-2", "Robert", "BOB@test.com", 35, 10, true));
    }

    @Test
    void testCreateDriverValidationFailures() {
        assertThrows(ValidationException.class, () ->
                service.createDriver("d-1", "", "bob@test.com", 25, 2, true));
        assertThrows(ValidationException.class, () ->
                service.createDriver("d-1", "Bob", "not-an-email", 25, 2, true));
        assertThrows(ValidationException.class, () ->
                service.createDriver("d-1", "Bob", "bob@test.com", 17, 0, true));
        assertThrows(ValidationException.class, () ->
                service.createDriver("d-1", "Bob", "bob@test.com", 25, -1, true));
        assertThrows(ValidationException.class, () ->
                service.createDriver("d-1", "Bob", "bob@test.com", 19, 5, true));
        assertThrows(ValidationException.class, () ->
                service.createDriver("d-1", "Bob", "bob@test.com", 25, 2, null));
    }

    @Test
    void testCreateDriverBoundaryExperience() {
        assertDoesNotThrow(() ->
                service.createDriver("d-1", "Bob", "bob@test.com", 19, 3, true));
    }

    @Test
    void testUpdateDriverSuccess() {
        service.createDriver("d-1", "Bob", "bob@test.com", 30, 7, true);
        Driver updated = service.updateDriver("d-1", "Robert", "robert@test.com", 31, 8, false);
        assertEquals("Robert", updated.getName());
        assertEquals("robert@test.com", updated.getEmail());
        assertEquals(31, updated.getAge());
        assertEquals(8, updated.getExperience());
        assertFalse(updated.isHasDeposit());
    }

    @Test
    void testUpdateDriverAtomicityOnFailure() {
        service.createDriver("d-1", "Bob", "bob@test.com", 30, 7, true);

        assertThrows(ValidationException.class, () ->
                service.updateDriver("d-1", "ChangedName", "newemail@test.com", 19, 10, true));

        Driver driver = service.getDriver("d-1");
        assertEquals("Bob", driver.getName());
        assertEquals("bob@test.com", driver.getEmail());
        assertEquals(30, driver.getAge());
    }

    @Test
    void testDeleteDriverSuccess() {
        service.createDriver("d-1", "Bob", "bob@test.com", 30, 7, true);
        service.deleteDriver("d-1");
        assertThrows(NotFoundException.class, () -> service.getDriver("d-1"));
    }

    @Test
    void testDeleteDriverWithActiveRentalThrowsConflict() {
        service.createCar("c-1", "Kia", "Rio", "A111AA", 2021, 1000.0, CarType.ECONOMY);
        service.createDriver("d-1", "Alice", "alice@test.com", 20, 1, false);
        service.rentCar("c-1", "d-1");

        assertThrows(ConflictException.class, () -> service.deleteDriver("d-1"));
    }

    @Test
    void testListDriversFiltering() {
        service.createCar("c-1", "Kia", "Rio", "A111AA", 2021, 1000.0, CarType.ECONOMY);
        service.createDriver("d-1", "Alice", "alice@test.com", 19, 1, false);
        service.createDriver("d-2", "Bob", "bob@test.com", 32, 8, true);
        service.rentCar("c-1", "d-1");

        assertEquals(1, service.listDrivers(25, null, null).size());
        assertEquals(1, service.listDrivers(null, true, null).size());
        assertEquals(1, service.listDrivers(null, false, null).size());
        assertEquals(1, service.listDrivers(null, null, true).size());
        assertEquals(2, service.listDrivers(null, null, null).size());
    }

    @Test
    void testRentEconomyCarSuccess() {
        service.createCar("c-1", "Kia", "Rio", "A111AA", 2021, 1000.0, CarType.ECONOMY);
        service.createDriver("d-1", "Alice", "alice@test.com", 18, 0, false);

        RentResponse resp = service.rentCar("c-1", "d-1");
        assertEquals("c-1", resp.carId());
        assertEquals("d-1", resp.driverId());
        assertEquals(CarType.ECONOMY, resp.carType());

        assertEquals(CarStatus.RENTED, service.getCar("c-1").getStatus());
        assertEquals("c-1", service.getDriver("d-1").getCarId());
    }

    @Test
    void testRentPremiumCarSuccess() {
        service.createCar("c-1", "Porsche", "Panamera", "P777PP", 2024, 2000.0, CarType.PREMIUM);
        service.createDriver("d-1", "Bob", "bob@test.com", 25, 3, true);

        RentResponse resp = service.rentCar("c-1", "d-1");
        assertEquals("c-1", resp.carId());
        assertEquals(CarStatus.RENTED, service.getCar("c-1").getStatus());
    }

    @Test
    void testRentPremiumCarValidationRules() {
        service.createCar("c-1", "Porsche", "Panamera", "P777PP", 2024, 2000.0, CarType.PREMIUM);
        service.createDriver("d-underage", "Young", "y@test.com", 22, 3, true);
        service.createDriver("d-noexp", "NoExp", "ne@test.com", 25, 1, true);
        service.createDriver("d-nodeposit", "NoDep", "nd@test.com", 25, 3, false);

        assertThrows(ValidationException.class, () -> service.rentCar("c-1", "d-underage"));
        assertThrows(ValidationException.class, () -> service.rentCar("c-1", "d-noexp"));
        assertThrows(ValidationException.class, () -> service.rentCar("c-1", "d-nodeposit"));
    }

    @Test
    void testRentAlreadyRentedCarThrowsConflict() {
        service.createCar("c-1", "Kia", "Rio", "A111AA", 2021, 1000.0, CarType.ECONOMY);
        service.createDriver("d-1", "Alice", "alice@test.com", 20, 1, false);
        service.createDriver("d-2", "Bob", "bob@test.com", 30, 5, true);

        service.rentCar("c-1", "d-1");
        assertThrows(ConflictException.class, () -> service.rentCar("c-1", "d-2"));
    }

    @Test
    void testRentDriverWithActiveRentalThrowsConflict() {
        service.createCar("c-1", "Kia", "Rio", "A111AA", 2021, 1000.0, CarType.ECONOMY);
        service.createCar("c-2", "Hyundai", "Solaris", "B222BB", 2022, 2000.0, CarType.ECONOMY);
        service.createDriver("d-1", "Alice", "alice@test.com", 20, 1, false);

        service.rentCar("c-1", "d-1");
        assertThrows(ConflictException.class, () -> service.rentCar("c-2", "d-1"));
    }

    @Test
    void testReturnCarSuccess() {
        service.createCar("c-1", "Kia", "Rio", "A111AA", 2021, 1000.0, CarType.ECONOMY);
        service.createDriver("d-1", "Alice", "alice@test.com", 20, 1, false);
        service.rentCar("c-1", "d-1");

        ReturnResponse ret = service.returnCar("c-1", 2L, 50.0);
        assertEquals("c-1", ret.carId());
        assertEquals("d-1", ret.driverId());
        assertEquals(80.0, ret.totalCost());
        assertEquals(1050.0, ret.mileage());

        Car car = service.getCar("c-1");
        assertEquals(CarStatus.AVAILABLE, car.getStatus());
        assertNull(car.getDriverId());
        assertEquals(1050.0, car.getMileage());

        Driver driver = service.getDriver("d-1");
        assertNull(driver.getCarId());
    }

    @Test
    void testReturnCarNotRentedThrowsConflict() {
        service.createCar("c-1", "Kia", "Rio", "A111AA", 2021, 1000.0, CarType.ECONOMY);
        assertThrows(ConflictException.class, () -> service.returnCar("c-1", 1L, 10.0));
    }

    @Test
    void testReturnCarInvalidHoursOrDistanceThrowsValidation() {
        service.createCar("c-1", "Kia", "Rio", "A111AA", 2021, 1000.0, CarType.ECONOMY);
        service.createDriver("d-1", "Alice", "alice@test.com", 20, 1, false);
        service.rentCar("c-1", "d-1");

        assertThrows(ValidationException.class, () -> service.returnCar("c-1", 0L, 10.0));
        assertThrows(ValidationException.class, () -> service.returnCar("c-1", 1L, -5.0));
    }

    @Test
    void testConcurrentRentalsOnlyOneSucceeds() throws Exception {
        service.createCar("c-race", "Kia", "Rio", "R111RR", 2021, 1000.0, CarType.ECONOMY);
        int threadCount = 10;
        List<String> driverIds = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            String id = "drv-race-" + i;
            service.createDriver(id, "Driver " + i, "d" + i + "@race.com", 25, 3, false);
            driverIds.add(id);
        }

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (String dId : driverIds) {
            futures.add(executor.submit(() -> {
                try {
                    barrier.await();
                    service.rentCar("c-race", dId);
                    successCount.incrementAndGet();
                } catch (ConflictException e) {
                    conflictCount.incrementAndGet();
                } catch (Exception e) {
                    fail("Unexpected exception: " + e.getMessage());
                }
            }));
        }

        for (Future<?> f : futures) {
            f.get();
        }
        executor.shutdown();

        assertEquals(1, successCount.get());
        assertEquals(threadCount - 1, conflictCount.get());
        assertEquals(CarStatus.RENTED, service.getCar("c-race").getStatus());
    }

    @Test
    void testConcurrentCreationSamePlateOnlyOneSucceeds() throws Exception {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            futures.add(executor.submit(() -> {
                try {
                    barrier.await();
                    service.createCar("car-conc-" + idx, "Brand", "Model", "SAME-PLATE", 2022, 100.0, CarType.ECONOMY);
                    successCount.incrementAndGet();
                } catch (ConflictException e) {
                    conflictCount.incrementAndGet();
                } catch (Exception e) {
                    fail("Unexpected exception: " + e.getMessage());
                }
            }));
        }

        for (Future<?> f : futures) {
            f.get();
        }
        executor.shutdown();

        assertEquals(1, successCount.get());
        assertEquals(threadCount - 1, conflictCount.get());
    }
}
