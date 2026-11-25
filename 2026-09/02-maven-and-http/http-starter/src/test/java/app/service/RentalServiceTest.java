package app.service;

import app.exception.ConflictException;
import app.exception.NotFoundException;
import app.exception.ValidationException;
import app.model.*;
import app.repository.CarRepository;
import app.repository.DriverRepository;
import app.repository.RentalRepository;
import app.web.dto.CarResponse;
import app.web.dto.DriverResponse;
import app.web.dto.RentResponse;
import app.web.dto.ReturnResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class RentalServiceTest {

    private CarRepository carRepository;
    private DriverRepository driverRepository;
    private RentalRepository rentalRepository;
    private RentalService service;

    @BeforeEach
    void setUp() {
        carRepository = Mockito.mock(CarRepository.class);
        driverRepository = Mockito.mock(DriverRepository.class);
        rentalRepository = Mockito.mock(RentalRepository.class);
        service = new RentalService(carRepository, driverRepository, rentalRepository);

        when(carRepository.save(any(Car.class))).thenAnswer(inv -> inv.getArgument(0));
        when(driverRepository.save(any(Driver.class))).thenAnswer(inv -> inv.getArgument(0));
        when(rentalRepository.save(any(Rental.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void testCreateCarSuccess() {
        when(carRepository.existsById("c-1")).thenReturn(false);
        when(carRepository.existsByPlateNumberIgnoreCase("A111AA")).thenReturn(false);

        CarResponse car = service.createCar("c-1", "Kia", "Rio", "a111aa", 2021, 1000.0, CarType.ECONOMY);
        assertEquals("c-1", car.id());
        assertEquals("Kia", car.brand());
        assertEquals("Rio", car.model());
        assertEquals("A111AA", car.plateNumber());
        assertEquals(2021, car.productionYear());
        assertEquals(1000.0, car.mileage());
        assertEquals(CarStatus.AVAILABLE, car.status());
        assertEquals(CarType.ECONOMY, car.type());
        verify(carRepository).save(any(EconomyCar.class));
    }

    @Test
    void testCreateCarDuplicatePlateThrowsConflict() {
        when(carRepository.existsById("c-2")).thenReturn(false);
        when(carRepository.existsByPlateNumberIgnoreCase("A111AA")).thenReturn(true);

        assertThrows(ConflictException.class, () ->
                service.createCar("c-2", "Hyundai", "Solaris", "a111aa", 2022, 500.0, CarType.ECONOMY));
        verify(carRepository, never()).save(any());
    }

    @Test
    void testCreateCarDuplicateIdThrowsConflict() {
        when(carRepository.existsById("c-1")).thenReturn(true);

        assertThrows(ConflictException.class, () ->
                service.createCar("c-1", "Hyundai", "Solaris", "B222BB", 2022, 500.0, CarType.ECONOMY));
        verify(carRepository, never()).save(any());
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
    void testUpdateCarAtomicityOnValidationFailure() {
        Car existing = new EconomyCar("c-1", "Kia", "Rio", "A111AA", 2021, 1000.0);
        when(carRepository.findByIdWithDriver("c-1")).thenReturn(Optional.of(existing));

        assertThrows(ValidationException.class, () ->
                service.updateCar("c-1", "ChangedBrand", "ChangedModel", "A111AA", 1980, 1000.0));

        assertEquals("Kia", existing.getBrand());
        assertEquals("Rio", existing.getModel());
        assertEquals(2021, existing.getProductionYear());
        verify(carRepository, never()).save(any());
    }

    @Test
    void testUpdateCarRentedThrowsConflict() {
        Car existing = new EconomyCar("c-1", "Kia", "Rio", "A111AA", 2021, 1000.0);
        existing.setStatus(CarStatus.RENTED);
        when(carRepository.findByIdWithDriver("c-1")).thenReturn(Optional.of(existing));

        assertThrows(ConflictException.class, () ->
                service.updateCar("c-1", "NewBrand", null, null, null, null));
    }

    @Test
    void testDeleteCarRentedThrowsConflict() {
        Car existing = new EconomyCar("c-1", "Kia", "Rio", "A111AA", 2021, 1000.0);
        existing.setStatus(CarStatus.RENTED);
        when(carRepository.findByIdWithDriver("c-1")).thenReturn(Optional.of(existing));

        assertThrows(ConflictException.class, () -> service.deleteCar("c-1"));
        verify(carRepository, never()).deleteById(any());
    }

    @Test
    void testListCarsFiltering() {
        Car c1 = new EconomyCar("c-1", "Kia", "Rio", "A111AA", 2020, 1000.0);
        c1.setStatus(CarStatus.RENTED);
        Car c2 = new PremiumCar("c-2", "Porsche", "Panamera", "P777PP", 2024, 2000.0);
        when(carRepository.findAllWithDriver()).thenReturn(List.of(c1, c2));

        assertEquals(1, service.listCars(CarType.PREMIUM, null, null, null).size());
        assertEquals(1, service.listCars(null, CarStatus.RENTED, null, null).size());
        assertEquals(1, service.listCars(null, CarStatus.AVAILABLE, null, null).size());
        assertEquals(1, service.listCars(null, null, 2022, null).size());
        assertEquals(1, service.listCars(null, null, null, "panamera").size());
        assertEquals(2, service.listCars(null, null, null, null).size());
    }

    @Test
    void testCreateDriverSuccessAndBoundaryExperience() {
        when(driverRepository.existsById("d-1")).thenReturn(false);
        when(driverRepository.existsByEmailIgnoreCase("bob@test.com")).thenReturn(false);

        DriverResponse driver = service.createDriver("d-1", "Bob", "BOB@test.com", 19, 3, true);
        assertEquals("d-1", driver.id());
        assertEquals("Bob", driver.name());
        assertEquals("bob@test.com", driver.email());
        assertEquals(19, driver.age());
        assertEquals(3, driver.experience());
        assertTrue(driver.hasDeposit());
        assertEquals(0, driver.rentalCount());
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
    void testUpdateDriverAtomicityAndActiveRentalRestriction() {
        Driver driver = new Driver("d-1", "Bob", "bob@test.com", 30, 7, true);
        when(driverRepository.findByIdWithDetails("d-1")).thenReturn(Optional.of(driver));

        assertThrows(ValidationException.class, () ->
                service.updateDriver("d-1", "Changed", "changed@test.com", 19, 10, false));
        assertEquals("Bob", driver.getName());
        assertEquals(30, driver.getAge());

        Car activePrem = new PremiumCar("c-1", "Porsche", "Panamera", "P777PP", 2024, 2000.0);
        driver.getRentals().add(new Rental("r-active", activePrem, driver));
        assertThrows(ConflictException.class, () ->
                service.updateDriver("d-1", null, null, 20, null, null));
        assertThrows(ConflictException.class, () ->
                service.updateDriver("d-1", null, null, null, 1, null));
        assertThrows(ConflictException.class, () ->
                service.updateDriver("d-1", null, null, null, null, false));
    }

    @Test
    void testDeleteDriverWithActiveRentalThrowsConflict() {
        Driver driver = new Driver("d-1", "Alice", "alice@test.com", 20, 1, false);
        Car activeEco = new EconomyCar("c-1", "Kia", "Rio", "A111AA", 2021, 1000.0);
        driver.getRentals().add(new Rental("r-active", activeEco, driver));
        when(driverRepository.findByIdWithDetails("d-1")).thenReturn(Optional.of(driver));

        assertThrows(ConflictException.class, () -> service.deleteDriver("d-1"));
        verify(driverRepository, never()).deleteById(any());
    }

    @Test
    void testRentCarSuccessUpdatesAllLinkedEntities() {
        Car car = new EconomyCar("c-1", "Kia", "Rio", "A111AA", 2021, 1000.0);
        Driver driver = new Driver("d-1", "Alice", "alice@test.com", 20, 1, false);

        when(rentalRepository.existsById(anyString())).thenReturn(false);
        when(carRepository.findByIdWithDriver("c-1")).thenReturn(Optional.of(car));
        when(driverRepository.findByIdWithDetails("d-1")).thenReturn(Optional.of(driver));

        RentResponse resp = service.rentCar("c-1", "d-1");
        assertNotNull(resp.rentalId());
        assertEquals("c-1", resp.carId());
        assertEquals("d-1", resp.driverId());
        assertEquals(CarStatus.RENTED, car.getStatus());
        assertEquals(driver, car.getDriver());
        assertEquals(car, driver.getActiveCar());
        assertEquals(1, driver.getRentalCount());
        verify(rentalRepository).save(any(Rental.class));
    }

    @Test
    void testRentCarRejectsNonExistentResourcesAndConflicts() {
        Car rented = new EconomyCar("c-rented", "Kia", "Rio", "A111AA", 2021, 1000.0);
        rented.setStatus(CarStatus.RENTED);
        Car premium = new PremiumCar("c-prem", "Porsche", "Panamera", "P777PP", 2024, 2000.0);
        Driver youngNoDep = new Driver("d-young", "Young", "y@test.com", 22, 1, false);

        when(carRepository.findByIdWithDriver("c-missing")).thenReturn(Optional.empty());
        when(carRepository.findByIdWithDriver("c-rented")).thenReturn(Optional.of(rented));
        when(carRepository.findByIdWithDriver("c-prem")).thenReturn(Optional.of(premium));
        when(driverRepository.findByIdWithDetails("d-young")).thenReturn(Optional.of(youngNoDep));
        when(driverRepository.findByIdWithDetails("d-missing")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.rentCar("c-missing", "d-young"));
        assertThrows(NotFoundException.class, () -> service.rentCar("c-prem", "d-missing"));
        assertThrows(ConflictException.class, () -> service.rentCar("c-rented", "d-young"));
        assertThrows(ValidationException.class, () -> service.rentCar("c-prem", "d-young"));
    }

    @Test
    void testReturnCarSuccessUpdatesCarAndClosesRental() {
        Driver driver = new Driver("d-1", "Alice", "alice@test.com", 20, 1, false);
        Car car = new EconomyCar("c-1", "Kia", "Rio", "A111AA", 2021, 1000.0);
        car.setStatus(CarStatus.RENTED);
        car.setDriver(driver);
        Rental rental = new Rental("r-1", car, driver);
        driver.getRentals().add(rental);

        when(carRepository.findByIdWithDriver("c-1")).thenReturn(Optional.of(car));
        when(rentalRepository.findFirstByCarIdAndActiveTrue("c-1")).thenReturn(Optional.of(rental));

        ReturnResponse ret = service.returnCar("c-1", 2L, 50.0);
        assertEquals("r-1", ret.rentalId());
        assertEquals(80.0, ret.totalCost());
        assertEquals(1050.0, ret.mileage());
        assertEquals(CarStatus.AVAILABLE, car.getStatus());
        assertNull(car.getDriver());
        assertNull(driver.getActiveCar());
        assertFalse(rental.isActive());
        assertEquals(80.0, rental.getTotalCost());
        assertNotNull(rental.getEndedAt());
    }

    @Test
    void testReturnCarInvalidInputsAndState() {
        Car available = new EconomyCar("c-1", "Kia", "Rio", "A111AA", 2021, 1000.0);
        when(carRepository.findByIdWithDriver("c-1")).thenReturn(Optional.of(available));

        assertThrows(ConflictException.class, () -> service.returnCar("c-1", 1L, 10.0));
        assertThrows(ValidationException.class, () -> service.returnCar("c-1", 0L, 10.0));
        assertThrows(ValidationException.class, () -> service.returnCar("c-1", 1L, -5.0));
    }
}
