package app.service;

import app.exception.ConflictException;
import app.exception.NotFoundException;
import app.exception.ValidationException;
import app.model.*;
import app.repository.CarRepository;
import app.repository.DriverRepository;
import app.web.dto.RentResponse;
import app.web.dto.ReturnResponse;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class RentalService {
    private static final int LOCK_STRIPES = 64;
    private final CarRepository carRepository;
    private final DriverRepository driverRepository;
    private final ReentrantReadWriteLock[] carLocks = createStripes();
    private final ReentrantReadWriteLock[] driverLocks = createStripes();

    public RentalService(CarRepository carRepository, DriverRepository driverRepository) {
        this.carRepository = carRepository;
        this.driverRepository = driverRepository;
    }

    private static ReentrantReadWriteLock[] createStripes() {
        ReentrantReadWriteLock[] stripes = new ReentrantReadWriteLock[LOCK_STRIPES];
        for (int i = 0; i < LOCK_STRIPES; i++) {
            stripes[i] = new ReentrantReadWriteLock();
        }
        return stripes;
    }

    private ReentrantReadWriteLock carLock(String carId) {
        return carLocks[Math.floorMod(carId.hashCode(), LOCK_STRIPES)];
    }

    private ReentrantReadWriteLock driverLock(String driverId) {
        return driverLocks[Math.floorMod(driverId.hashCode(), LOCK_STRIPES)];
    }

    public Car createCar(String id, String brand, String model, String plateNumber, Integer year, Double mileage, CarType type) {
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

        Lock lock = carLock(carId).writeLock();
        lock.lock();
        try {
            if (carRepository.findById(carId).isPresent()) {
                throw new ConflictException("Car with id " + carId + " already exists");
            }
            if (!carRepository.reservePlate(plate, carId)) {
                throw new ConflictException("Car with plate " + plate + " already exists");
            }

            Car car = (type == CarType.PREMIUM)
                    ? new PremiumCar(carId, brand.trim(), model.trim(), plate, year, mileage)
                    : new EconomyCar(carId, brand.trim(), model.trim(), plate, year, mileage);

            return carRepository.save(car);
        } finally {
            lock.unlock();
        }
    }

    public Car getCar(String id) {
        if (id == null) throw new NotFoundException("Car null not found");
        Lock lock = carLock(id).readLock();
        lock.lock();
        try {
            return carRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Car " + id + " not found"));
        } finally {
            lock.unlock();
        }
    }

    public List<Car> listCars(CarType type, CarStatus status, Integer minYear, String modelQuery) {
        return carRepository.findAll().stream()
                .map(c -> {
                    Lock lock = carLock(c.getId()).readLock();
                    lock.lock();
                    try {
                        return carRepository.findById(c.getId()).orElse(null);
                    } finally {
                        lock.unlock();
                    }
                })
                .filter(Objects::nonNull)
                .filter(c -> type == null || c.getType() == type)
                .filter(c -> status == null || c.getStatus() == status)
                .filter(c -> minYear == null || c.getProductionYear() >= minYear)
                .filter(c -> modelQuery == null || modelQuery.isBlank()
                        || c.getModel().toLowerCase().contains(modelQuery.trim().toLowerCase())
                        || c.getBrand().toLowerCase().contains(modelQuery.trim().toLowerCase()))
                .toList();
    }

    public Car updateCar(String id, String brand, String model, String plateNumber, Integer year, Double mileage) {
        if (id == null) throw new NotFoundException("Car null not found");
        Lock lock = carLock(id).writeLock();
        lock.lock();
        try {
            Car existing = carRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Car " + id + " not found"));
            if (existing.getStatus() == CarStatus.RENTED) {
                throw new ConflictException("Cannot edit car while it is rented");
            }

            String targetBrand = existing.getBrand();
            if (brand != null) {
                if (brand.isBlank()) throw new ValidationException("Brand cannot be blank");
                targetBrand = brand.trim();
            }

            String targetModel = existing.getModel();
            if (model != null) {
                if (model.isBlank()) throw new ValidationException("Model cannot be blank");
                targetModel = model.trim();
            }

            String oldPlate = existing.getPlateNumber();
            String targetPlate = oldPlate;
            boolean plateChanged = false;
            if (plateNumber != null) {
                if (plateNumber.isBlank()) throw new ValidationException("Plate number cannot be blank");
                String newPlate = plateNumber.trim().toUpperCase();
                if (!newPlate.equalsIgnoreCase(oldPlate)) {
                    targetPlate = newPlate;
                    plateChanged = true;
                }
            }

            int targetYear = existing.getProductionYear();
            if (year != null) {
                int maxYear = Year.now().getValue() + 1;
                if (year < 1990 || year > maxYear) throw new ValidationException("Invalid production year");
                targetYear = year;
            }

            double targetMileage = existing.getMileage();
            if (mileage != null) {
                if (mileage < 0) throw new ValidationException("Mileage cannot be negative");
                targetMileage = mileage;
            }

            if (plateChanged && !carRepository.reservePlate(targetPlate, id)) {
                throw new ConflictException("Plate " + targetPlate + " is already in use");
            }

            Car updated = existing.copy();
            updated.setBrand(targetBrand);
            updated.setModel(targetModel);
            updated.setPlateNumber(targetPlate);
            updated.setProductionYear(targetYear);
            updated.setMileage(targetMileage);

            Car saved = carRepository.save(updated);
            if (plateChanged) {
                carRepository.releasePlate(oldPlate, id);
            }
            return saved;
        } finally {
            lock.unlock();
        }
    }

    public void deleteCar(String id) {
        if (id == null) throw new NotFoundException("Car null not found");
        Lock lock = carLock(id).writeLock();
        lock.lock();
        try {
            Car car = carRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Car " + id + " not found"));
            if (car.getStatus() == CarStatus.RENTED) {
                throw new ConflictException("Cannot delete rented car " + id);
            }
            carRepository.deleteById(id);
        } finally {
            lock.unlock();
        }
    }

    public Driver createDriver(String id, String name, String email, Integer age, Integer experience, Boolean hasDeposit) {
        if (name == null || name.isBlank()) throw new ValidationException("Driver name cannot be blank");
        if (email == null || !email.contains("@")) throw new ValidationException("Valid email is required");
        if (age == null || age < 18) throw new ValidationException("Driver must be at least 18 years old");
        if (experience == null || experience < 0) throw new ValidationException("Experience cannot be negative");
        if (experience > (age - 16)) throw new ValidationException("Experience cannot exceed age minus 16");
        if (hasDeposit == null) throw new ValidationException("hasDeposit is required");

        String normalizedEmail = email.trim().toLowerCase();
        String driverId = (id != null && !id.isBlank()) ? id.trim() : "drv-" + UUID.randomUUID().toString().substring(0, 8);

        Lock lock = driverLock(driverId).writeLock();
        lock.lock();
        try {
            if (driverRepository.findById(driverId).isPresent()) {
                throw new ConflictException("Driver with id " + driverId + " already exists");
            }
            if (!driverRepository.reserveEmail(normalizedEmail, driverId)) {
                throw new ConflictException("Driver with email " + normalizedEmail + " already exists");
            }

            Driver driver = new Driver(driverId, name.trim(), normalizedEmail, age, experience, hasDeposit);
            return driverRepository.save(driver);
        } finally {
            lock.unlock();
        }
    }

    public Driver getDriver(String id) {
        if (id == null) throw new NotFoundException("Driver null not found");
        Lock lock = driverLock(id).readLock();
        lock.lock();
        try {
            return driverRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Driver " + id + " not found"));
        } finally {
            lock.unlock();
        }
    }

    public List<Driver> listDrivers(Integer minAge, Boolean hasCar, Boolean hasDeposit) {
        return driverRepository.findAll().stream()
                .map(d -> {
                    Lock lock = driverLock(d.getId()).readLock();
                    lock.lock();
                    try {
                        return driverRepository.findById(d.getId()).orElse(null);
                    } finally {
                        lock.unlock();
                    }
                })
                .filter(Objects::nonNull)
                .filter(d -> minAge == null || d.getAge() >= minAge)
                .filter(d -> hasCar == null || ((d.getCarId() != null) == hasCar))
                .filter(d -> hasDeposit == null || d.isHasDeposit() == hasDeposit)
                .toList();
    }

    public Driver updateDriver(String id, String name, String email, Integer age, Integer experience, Boolean hasDeposit) {
        if (id == null) throw new NotFoundException("Driver null not found");
        Lock lock = driverLock(id).writeLock();
        lock.lock();
        try {
            Driver existing = driverRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Driver " + id + " not found"));

            boolean ageChanged = age != null && age != existing.getAge();
            boolean expChanged = experience != null && experience != existing.getExperience();
            boolean depChanged = hasDeposit != null && hasDeposit != existing.isHasDeposit();
            if (existing.getCarId() != null && (ageChanged || expChanged || depChanged)) {
                throw new ConflictException("Cannot change age, experience or deposit status while driver has an active rental");
            }

            String targetName = existing.getName();
            if (name != null) {
                if (name.isBlank()) throw new ValidationException("Driver name cannot be blank");
                targetName = name.trim();
            }

            String oldEmail = existing.getEmail();
            String targetEmail = oldEmail;
            boolean emailChanged = false;
            if (email != null) {
                if (!email.contains("@")) throw new ValidationException("Valid email is required");
                String newEmail = email.trim().toLowerCase();
                if (!newEmail.equalsIgnoreCase(oldEmail)) {
                    targetEmail = newEmail;
                    emailChanged = true;
                }
            }

            int targetAge = age != null ? age : existing.getAge();
            int targetExp = experience != null ? experience : existing.getExperience();
            if (targetAge < 18) throw new ValidationException("Age must be >= 18");
            if (targetExp < 0 || targetExp > (targetAge - 16)) throw new ValidationException("Experience cannot exceed age minus 16");

            boolean targetDeposit = hasDeposit != null ? hasDeposit : existing.isHasDeposit();

            if (emailChanged && !driverRepository.reserveEmail(targetEmail, id)) {
                throw new ConflictException("Email " + targetEmail + " already registered");
            }

            Driver updated = existing.copy();
            updated.setName(targetName);
            updated.setEmail(targetEmail);
            updated.setAge(targetAge);
            updated.setExperience(targetExp);
            updated.setHasDeposit(targetDeposit);

            Driver saved = driverRepository.save(updated);
            if (emailChanged) {
                driverRepository.releaseEmail(oldEmail, id);
            }
            return saved;
        } finally {
            lock.unlock();
        }
    }

    public void deleteDriver(String id) {
        if (id == null) throw new NotFoundException("Driver null not found");
        Lock lock = driverLock(id).writeLock();
        lock.lock();
        try {
            Driver driver = driverRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Driver " + id + " not found"));
            if (driver.getCarId() != null) {
                throw new ConflictException("Cannot delete driver with active car rental");
            }
            driverRepository.deleteById(id);
        } finally {
            lock.unlock();
        }
    }

    public RentResponse rentCar(String carId, String driverId) {
        if (carId == null) throw new NotFoundException("Car null not found");
        if (driverId == null || driverId.isBlank()) throw new ValidationException("driverId is required");
        String trimmedDriverId = driverId.trim();

        Lock cLock = carLock(carId).writeLock();
        Lock dLock = driverLock(trimmedDriverId).writeLock();
        cLock.lock();
        try {
            dLock.lock();
            try {
                Car car = carRepository.findById(carId)
                        .orElseThrow(() -> new NotFoundException("Car " + carId + " not found"));
                Driver driver = driverRepository.findById(trimmedDriverId)
                        .orElseThrow(() -> new NotFoundException("Driver " + trimmedDriverId + " not found"));

                if (car.getStatus() == CarStatus.RENTED) {
                    throw new ConflictException("Car " + carId + " is already rented");
                }
                if (driver.getCarId() != null) {
                    throw new ConflictException("Driver " + trimmedDriverId + " already rents car " + driver.getCarId());
                }

                car.validateDriver(driver);

                Car updatedCar = car.copy();
                updatedCar.setStatus(CarStatus.RENTED);
                updatedCar.setDriverId(driver.getId());

                Driver updatedDriver = driver.copy();
                updatedDriver.setCarId(car.getId());

                carRepository.save(updatedCar);
                driverRepository.save(updatedDriver);

                return new RentResponse(updatedCar.getId(), updatedDriver.getId(), updatedCar.getType(),
                        "Car " + updatedCar.getId() + " successfully rented to " + updatedDriver.getName());
            } finally {
                dLock.unlock();
            }
        } finally {
            cLock.unlock();
        }
    }

    public ReturnResponse returnCar(String carId, Long hours, Double distance) {
        if (carId == null) throw new NotFoundException("Car null not found");
        long effectiveHours = (hours != null) ? hours : 1L;
        double effectiveDistance = (distance != null) ? distance : 0.0;

        if (effectiveHours <= 0) throw new ValidationException("Hours must be > 0");
        if (effectiveDistance < 0) throw new ValidationException("Distance cannot be negative");

        Lock cLock = carLock(carId).writeLock();
        cLock.lock();
        try {
            Car car = carRepository.findById(carId)
                    .orElseThrow(() -> new NotFoundException("Car " + carId + " not found"));
            if (car.getStatus() != CarStatus.RENTED) {
                throw new ConflictException("Car " + carId + " is not currently rented");
            }

            String driverId = car.getDriverId();
            double totalCost = car.calculateCost(effectiveHours, effectiveDistance);

            Car updatedCar = car.copy();
            updatedCar.setMileage(car.getMileage() + effectiveDistance);
            updatedCar.setStatus(CarStatus.AVAILABLE);
            updatedCar.setDriverId(null);

            if (driverId != null) {
                Lock dLock = driverLock(driverId).writeLock();
                dLock.lock();
                try {
                    carRepository.save(updatedCar);
                    driverRepository.findById(driverId).ifPresent(d -> {
                        Driver updatedDriver = d.copy();
                        updatedDriver.setCarId(null);
                        driverRepository.save(updatedDriver);
                    });
                } finally {
                    dLock.unlock();
                }
            } else {
                carRepository.save(updatedCar);
            }

            return new ReturnResponse(updatedCar.getId(), driverId, totalCost, updatedCar.getMileage(),
                    "Car " + updatedCar.getId() + " returned. Total cost: $" + totalCost);
        } finally {
            cLock.unlock();
        }
    }
}
