package app.service;

import app.exception.ConflictException;
import app.exception.NotFoundException;
import app.exception.ValidationException;
import app.model.*;
import app.repository.CarRepository;
import app.repository.DriverRepository;
import app.repository.RentalRepository;
import app.web.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.UUID;

@Service
public class RentalService {
    private final CarRepository carRepository;
    private final DriverRepository driverRepository;
    private final RentalRepository rentalRepository;

    public RentalService(CarRepository carRepository, DriverRepository driverRepository, RentalRepository rentalRepository) {
        this.carRepository = carRepository;
        this.driverRepository = driverRepository;
        this.rentalRepository = rentalRepository;
    }

    @Transactional
    public CarResponse createCar(String id, String brand, String model, String plateNumber, Integer year, Double mileage, CarType type) {
        if (brand == null || brand.isBlank()) throw new ValidationException("Brand cannot be blank");
        if (model == null || model.isBlank()) throw new ValidationException("Model cannot be blank");
        if (plateNumber == null || plateNumber.isBlank()) throw new ValidationException("Plate number cannot be blank");
        if (type == null) throw new ValidationException("Car type must be ECONOMY or PREMIUM");
        if (year == null) throw new ValidationException("Production year is required");
        if (mileage == null || mileage < 0) throw new ValidationException("Mileage cannot be negative");

        int maxYear = Year.now().getValue() + 1;
        if (year < 1990 || year > maxYear) throw new ValidationException("Year must be between 1990 and " + maxYear);

        String plate = plateNumber.trim().toUpperCase();
        String carId = (id != null && !id.isBlank()) ? id.trim() : "car-" + UUID.randomUUID().toString().substring(0, 8);

        if (carRepository.existsById(carId)) {
            throw new ConflictException("Car with id " + carId + " already exists");
        }
        if (carRepository.existsByPlateNumberIgnoreCase(plate)) {
            throw new ConflictException("Car with plate " + plate + " already exists");
        }

        Car car = (type == CarType.PREMIUM)
                ? new PremiumCar(carId, brand.trim(), model.trim(), plate, year, mileage)
                : new EconomyCar(carId, brand.trim(), model.trim(), plate, year, mileage);

        return toCarResponse(carRepository.save(car));
    }

    @Transactional(readOnly = true)
    public CarResponse getCar(String id) {
        if (id == null) throw new NotFoundException("Car null not found");
        Car car = carRepository.findByIdWithDriver(id)
                .orElseThrow(() -> new NotFoundException("Car " + id + " not found"));
        return toCarResponse(car);
    }

    @Transactional(readOnly = true)
    public List<CarResponse> listCars(CarType type, CarStatus status, Integer minYear, String modelQuery) {
        return carRepository.findAllWithDriver().stream()
                .filter(c -> type == null || c.getType() == type)
                .filter(c -> status == null || c.getStatus() == status)
                .filter(c -> minYear == null || c.getProductionYear() >= minYear)
                .filter(c -> modelQuery == null || modelQuery.isBlank()
                        || c.getModel().toLowerCase().contains(modelQuery.trim().toLowerCase())
                        || c.getBrand().toLowerCase().contains(modelQuery.trim().toLowerCase()))
                .map(this::toCarResponse)
                .toList();
    }

    @Transactional
    public CarResponse updateCar(String id, String brand, String model, String plateNumber, Integer year, Double mileage) {
        if (id == null) throw new NotFoundException("Car null not found");
        Car car = carRepository.findByIdWithDriver(id)
                .orElseThrow(() -> new NotFoundException("Car " + id + " not found"));

        if (car.getStatus() == CarStatus.RENTED) {
            throw new ConflictException("Cannot edit car while it is rented");
        }

        String targetBrand = car.getBrand();
        if (brand != null) {
            if (brand.isBlank()) throw new ValidationException("Brand cannot be blank");
            targetBrand = brand.trim();
        }

        String targetModel = car.getModel();
        if (model != null) {
            if (model.isBlank()) throw new ValidationException("Model cannot be blank");
            targetModel = model.trim();
        }

        String targetPlate = car.getPlateNumber();
        if (plateNumber != null) {
            if (plateNumber.isBlank()) throw new ValidationException("Plate number cannot be blank");
            String newPlate = plateNumber.trim().toUpperCase();
            if (!newPlate.equalsIgnoreCase(car.getPlateNumber())) {
                if (carRepository.existsByPlateNumberIgnoreCase(newPlate)) {
                    throw new ConflictException("Plate " + newPlate + " is already in use");
                }
                targetPlate = newPlate;
            }
        }

        int targetYear = car.getProductionYear();
        if (year != null) {
            int maxYear = Year.now().getValue() + 1;
            if (year < 1990 || year > maxYear) throw new ValidationException("Invalid production year");
            targetYear = year;
        }

        double targetMileage = car.getMileage();
        if (mileage != null) {
            if (mileage < 0) throw new ValidationException("Mileage cannot be negative");
            targetMileage = mileage;
        }

        car.setBrand(targetBrand);
        car.setModel(targetModel);
        car.setPlateNumber(targetPlate);
        car.setProductionYear(targetYear);
        car.setMileage(targetMileage);

        return toCarResponse(carRepository.save(car));
    }

    @Transactional
    public void deleteCar(String id) {
        if (id == null) throw new NotFoundException("Car null not found");
        Car car = carRepository.findByIdWithDriver(id)
                .orElseThrow(() -> new NotFoundException("Car " + id + " not found"));
        if (car.getStatus() == CarStatus.RENTED) {
            throw new ConflictException("Cannot delete rented car " + id);
        }
        rentalRepository.deleteAllByCarId(id);
        carRepository.deleteById(id);
    }

    @Transactional
    public DriverResponse createDriver(String id, String name, String email, Integer age, Integer experience, Boolean hasDeposit) {
        if (name == null || name.isBlank()) throw new ValidationException("Driver name cannot be blank");
        if (email == null || !email.contains("@")) throw new ValidationException("Valid email is required");
        if (age == null || age < 18) throw new ValidationException("Driver must be at least 18 years old");
        if (experience == null || experience < 0) throw new ValidationException("Experience cannot be negative");
        if (experience > (age - 16)) throw new ValidationException("Experience cannot exceed age minus 16");
        if (hasDeposit == null) throw new ValidationException("hasDeposit is required");

        String normalizedEmail = email.trim().toLowerCase();
        String driverId = (id != null && !id.isBlank()) ? id.trim() : "drv-" + UUID.randomUUID().toString().substring(0, 8);

        if (driverRepository.existsById(driverId)) {
            throw new ConflictException("Driver with id " + driverId + " already exists");
        }
        if (driverRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ConflictException("Driver with email " + normalizedEmail + " already exists");
        }

        Driver driver = new Driver(driverId, name.trim(), normalizedEmail, age, experience, hasDeposit);
        return toDriverResponse(driverRepository.save(driver));
    }

    @Transactional(readOnly = true)
    public DriverResponse getDriver(String id) {
        if (id == null) throw new NotFoundException("Driver null not found");
        Driver driver = driverRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new NotFoundException("Driver " + id + " not found"));
        return toDriverResponse(driver);
    }

    @Transactional(readOnly = true)
    public List<DriverResponse> listDrivers(Integer minAge, Boolean hasCar, Boolean hasDeposit) {
        return driverRepository.findAllWithDetails().stream()
                .filter(d -> minAge == null || d.getAge() >= minAge)
                .filter(d -> hasCar == null || ((d.getActiveCar() != null) == hasCar))
                .filter(d -> hasDeposit == null || d.isHasDeposit() == hasDeposit)
                .map(this::toDriverResponse)
                .toList();
    }

    @Transactional
    public DriverResponse updateDriver(String id, String name, String email, Integer age, Integer experience, Boolean hasDeposit) {
        if (id == null) throw new NotFoundException("Driver null not found");
        Driver driver = driverRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new NotFoundException("Driver " + id + " not found"));

        boolean ageChanged = age != null && age != driver.getAge();
        boolean expChanged = experience != null && experience != driver.getExperience();
        boolean depChanged = hasDeposit != null && hasDeposit != driver.isHasDeposit();
        if (driver.getActiveCar() != null && (ageChanged || expChanged || depChanged)) {
            throw new ConflictException("Cannot change age, experience or deposit status while driver has an active rental");
        }

        String targetName = driver.getName();
        if (name != null) {
            if (name.isBlank()) throw new ValidationException("Driver name cannot be blank");
            targetName = name.trim();
        }

        String targetEmail = driver.getEmail();
        if (email != null) {
            if (!email.contains("@")) throw new ValidationException("Valid email is required");
            String newEmail = email.trim().toLowerCase();
            if (!newEmail.equalsIgnoreCase(driver.getEmail())) {
                if (driverRepository.existsByEmailIgnoreCase(newEmail)) {
                    throw new ConflictException("Email " + newEmail + " already registered");
                }
                targetEmail = newEmail;
            }
        }

        int targetAge = age != null ? age : driver.getAge();
        int targetExp = experience != null ? experience : driver.getExperience();
        if (targetAge < 18) throw new ValidationException("Age must be >= 18");
        if (targetExp < 0 || targetExp > (targetAge - 16)) throw new ValidationException("Experience cannot exceed age minus 16");

        boolean targetDeposit = hasDeposit != null ? hasDeposit : driver.isHasDeposit();

        driver.setName(targetName);
        driver.setEmail(targetEmail);
        driver.setAge(targetAge);
        driver.setExperience(targetExp);
        driver.setHasDeposit(targetDeposit);

        return toDriverResponse(driverRepository.save(driver));
    }

    @Transactional
    public void deleteDriver(String id) {
        if (id == null) throw new NotFoundException("Driver null not found");
        Driver driver = driverRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new NotFoundException("Driver " + id + " not found"));
        if (driver.getActiveCar() != null) {
            throw new ConflictException("Cannot delete driver with active car rental");
        }
        rentalRepository.deleteAllByDriverId(id);
        driverRepository.deleteById(id);
    }

    @Transactional
    public RentResponse rentCar(String carId, String driverId) {
        Rental rental = createRentalInternal(null, carId, driverId);
        return new RentResponse(
                rental.getId(),
                rental.getCar().getId(),
                rental.getDriver().getId(),
                rental.getCar().getType(),
                "Car " + rental.getCar().getId() + " successfully rented to " + rental.getDriver().getName()
        );
    }

    @Transactional
    public RentalResponse createRental(String rentalId, String carId, String driverId) {
        return toRentalResponse(createRentalInternal(rentalId, carId, driverId));
    }

    private Rental createRentalInternal(String rentalId, String carId, String driverId) {
        if (carId == null || carId.isBlank()) throw new ValidationException("carId is required");
        if (driverId == null || driverId.isBlank()) throw new ValidationException("driverId is required");

        String trimmedCarId = carId.trim();
        String trimmedDriverId = driverId.trim();
        String effRentalId = (rentalId != null && !rentalId.isBlank())
                ? rentalId.trim()
                : "rent-" + UUID.randomUUID().toString().substring(0, 8);

        if (rentalRepository.existsById(effRentalId)) {
            throw new ConflictException("Rental with id " + effRentalId + " already exists");
        }

        Car car = carRepository.findByIdWithDriver(trimmedCarId)
                .orElseThrow(() -> new NotFoundException("Car " + trimmedCarId + " not found"));
        Driver driver = driverRepository.findByIdWithDetails(trimmedDriverId)
                .orElseThrow(() -> new NotFoundException("Driver " + trimmedDriverId + " not found"));

        if (car.getStatus() == CarStatus.RENTED) {
            throw new ConflictException("Car " + trimmedCarId + " is already rented");
        }
        if (driver.getActiveCar() != null) {
            throw new ConflictException("Driver " + trimmedDriverId + " already rents car " + driver.getActiveCar().getId());
        }

        car.validateDriver(driver);

        car.setStatus(CarStatus.RENTED);
        car.setDriver(driver);
        driver.setRentalCount(driver.getRentalCount() + 1);

        Rental rental = new Rental(effRentalId, car, driver);

        carRepository.save(car);
        driverRepository.save(driver);
        Rental saved = rentalRepository.save(rental);
        driver.getRentals().add(0, saved);
        return saved;
    }

    @Transactional
    public ReturnResponse returnCar(String carId, Long hours, Double distance) {
        if (carId == null) throw new NotFoundException("Car null not found");
        long effectiveHours = (hours != null) ? hours : 1L;
        double effectiveDistance = (distance != null) ? distance : 0.0;

        if (effectiveHours <= 0) throw new ValidationException("Hours must be > 0");
        if (effectiveDistance < 0) throw new ValidationException("Distance cannot be negative");

        Car car = carRepository.findByIdWithDriver(carId)
                .orElseThrow(() -> new NotFoundException("Car " + carId + " not found"));
        if (car.getStatus() != CarStatus.RENTED) {
            throw new ConflictException("Car " + carId + " is not currently rented");
        }

        Driver driver = car.getDriver();
        String driverId = driver != null ? driver.getId() : null;
        double totalCost = car.calculateCost(effectiveHours, effectiveDistance);

        car.setMileage(car.getMileage() + effectiveDistance);
        car.setStatus(CarStatus.AVAILABLE);
        car.setDriver(null);
        carRepository.save(car);

        Rental rental = rentalRepository.findFirstByCarIdAndActiveTrue(carId)
                .orElseThrow(() -> new ConflictException("Active rental not found for car " + carId));
        rental.setHours(effectiveHours);
        rental.setDistance(effectiveDistance);
        rental.setTotalCost(totalCost);
        rental.setActive(false);
        rental.setEndedAt(LocalDateTime.now());
        rentalRepository.save(rental);

        return new ReturnResponse(
                rental.getId(),
                car.getId(),
                driverId,
                totalCost,
                car.getMileage(),
                "Car " + car.getId() + " returned. Total cost: $" + totalCost
        );
    }

    @Transactional
    public ReturnResponse returnRentalById(String rentalId, Long hours, Double distance) {
        if (rentalId == null) throw new NotFoundException("Rental null not found");
        Rental rental = rentalRepository.findByIdWithDetails(rentalId)
                .orElseThrow(() -> new NotFoundException("Rental " + rentalId + " not found"));
        if (!rental.isActive()) {
            throw new ConflictException("Rental " + rentalId + " is already completed");
        }
        return returnCar(rental.getCar().getId(), hours, distance);
    }

    @Transactional(readOnly = true)
    public List<RentalResponse> listRentals(Boolean active) {
        return rentalRepository.findAllWithDetails().stream()
                .filter(r -> active == null || r.isActive() == active)
                .map(this::toRentalResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RentalResponse getRental(String id) {
        if (id == null) throw new NotFoundException("Rental null not found");
        Rental rental = rentalRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new NotFoundException("Rental " + id + " not found"));
        return toRentalResponse(rental);
    }

    private CarSummary toCarSummary(Car car) {
        return new CarSummary(car.getId(), car.getBrand(), car.getModel(), car.getPlateNumber(), car.getType(), car.getStatus());
    }

    private DriverSummary toDriverSummary(Driver driver) {
        return new DriverSummary(driver.getId(), driver.getName(), driver.getEmail(), driver.getAge(), driver.getExperience(), driver.isHasDeposit());
    }

    private CarResponse toCarResponse(Car car) {
        Driver driver = car.getDriver();
        return new CarResponse(
                car.getId(),
                car.getBrand(),
                car.getModel(),
                car.getPlateNumber(),
                car.getProductionYear(),
                car.getMileage(),
                car.getStatus(),
                car.getType(),
                car.getMinAge(),
                car.getMinExperience(),
                car.isDepositRequired(),
                car.getHourlyRate(),
                car.getFreeKmPerHour(),
                car.getExtraKmRate(),
                driver != null ? driver.getId() : null,
                driver != null ? toDriverSummary(driver) : null
        );
    }

    private DriverResponse toDriverResponse(Driver driver) {
        Car activeCar = driver.getActiveCar();
        List<RentalSummary> rentals = driver.getRentals().stream()
                .map(r -> new RentalSummary(
                        r.getId(),
                        r.getCar().getId(),
                        r.getCar().getBrand(),
                        r.getCar().getModel(),
                        r.isActive(),
                        r.getTotalCost(),
                        r.getStartedAt(),
                        r.getEndedAt()
                ))
                .toList();
        return new DriverResponse(
                driver.getId(),
                driver.getName(),
                driver.getEmail(),
                driver.getAge(),
                driver.getExperience(),
                driver.isHasDeposit(),
                driver.getRentalCount(),
                activeCar != null ? activeCar.getId() : null,
                activeCar != null ? toCarSummary(activeCar) : null,
                rentals
        );
    }

    private RentalResponse toRentalResponse(Rental rental) {
        return new RentalResponse(
                rental.getId(),
                rental.isActive(),
                rental.getHours(),
                rental.getDistance(),
                rental.getTotalCost(),
                rental.getStartedAt(),
                rental.getEndedAt(),
                toCarSummary(rental.getCar()),
                toDriverSummary(rental.getDriver())
        );
    }
}
