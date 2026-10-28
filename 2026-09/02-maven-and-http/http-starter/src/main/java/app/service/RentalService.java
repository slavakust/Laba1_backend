package app.service;

import app.exception.ConflictException;
import app.exception.NotFoundException;
import app.exception.ValidationException;
import app.model.*;
import app.repository.CarRepository;
import app.repository.DriverRepository;

import java.time.Year;
import java.util.List;
import java.util.UUID;

public class RentalService {
    private final CarRepository carRepository;
    private final DriverRepository driverRepository;
    private final Object lock = new Object();

    public RentalService(CarRepository carRepository, DriverRepository driverRepository) {
        this.carRepository = carRepository;
        this.driverRepository = driverRepository;
    }

    public Car createCar(String id, String brand, String model, String plateNumber, int year, double mileage, CarType type) {
        if (brand == null || brand.isBlank()) throw new ValidationException("Brand cannot be blank");
        if (model == null || model.isBlank()) throw new ValidationException("Model cannot be blank");
        if (plateNumber == null || plateNumber.isBlank()) throw new ValidationException("Plate number cannot be blank");
        if (type == null) throw new ValidationException("Car type must be ECONOMY or PREMIUM");
        if (mileage < 0) throw new ValidationException("Mileage cannot be negative");

        int maxYear = Year.now().getValue() + 1;
        if (year < 1990 || year > maxYear) throw new ValidationException("Year must be between 1990 and " + maxYear);

        String plate = plateNumber.trim().toUpperCase();
        if (carRepository.findByPlate(plate).isPresent()) {
            throw new ConflictException("Car with plate " + plate + " already exists");
        }

        String carId = (id != null && !id.isBlank()) ? id.trim() : "car-" + UUID.randomUUID().toString().substring(0, 8);
        if (carRepository.findById(carId).isPresent()) {
            throw new ConflictException("Car with id " + carId + " already exists");
        }

        Car car = (type == CarType.PREMIUM)
                ? new PremiumCar(carId, brand.trim(), model.trim(), plate, year, mileage)
                : new EconomyCar(carId, brand.trim(), model.trim(), plate, year, mileage);

        return carRepository.save(car);
    }

    public Car getCar(String id) {
        return carRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Car " + id + " not found"));
    }

    public List<Car> listCars(CarType type, CarStatus status, Integer minYear, String modelQuery) {
        return carRepository.findAll().stream()
                .filter(c -> type == null || c.getType() == type)
                .filter(c -> status == null || c.getStatus() == status)
                .filter(c -> minYear == null || c.getProductionYear() >= minYear)
                .filter(c -> modelQuery == null || modelQuery.isBlank()
                        || c.getModel().toLowerCase().contains(modelQuery.trim().toLowerCase())
                        || c.getBrand().toLowerCase().contains(modelQuery.trim().toLowerCase()))
                .toList();
    }

    public Car updateCar(String id, String brand, String model, String plateNumber, Integer year, Double mileage) {
        Car car = getCar(id);
        if (car.getStatus() == CarStatus.RENTED) {
            throw new ConflictException("Cannot edit car while it is rented");
        }
        if (brand != null && !brand.isBlank()) car.setBrand(brand.trim());
        if (model != null && !model.isBlank()) car.setModel(model.trim());
        if (plateNumber != null && !plateNumber.isBlank()) {
            String newPlate = plateNumber.trim().toUpperCase();
            if (!newPlate.equalsIgnoreCase(car.getPlateNumber())) {
                if (carRepository.findByPlate(newPlate).isPresent()) {
                    throw new ConflictException("Plate " + newPlate + " is already in use");
                }
                car.setPlateNumber(newPlate);
            }
        }
        if (year != null) {
            int maxYear = Year.now().getValue() + 1;
            if (year < 1990 || year > maxYear) throw new ValidationException("Invalid production year");
            car.setProductionYear(year);
        }
        if (mileage != null) {
            if (mileage < 0) throw new ValidationException("Mileage cannot be negative");
            car.setMileage(mileage);
        }
        return carRepository.save(car);
    }

    public void deleteCar(String id) {
        Car car = getCar(id);
        if (car.getStatus() == CarStatus.RENTED) {
            throw new ConflictException("Cannot delete rented car " + id);
        }
        carRepository.deleteById(id);
    }

    public Driver createDriver(String id, String name, String email, int age, int experience, boolean hasDeposit) {
        if (name == null || name.isBlank()) throw new ValidationException("Driver name cannot be blank");
        if (email == null || !email.contains("@")) throw new ValidationException("Valid email is required");
        if (age < 18) throw new ValidationException("Driver must be at least 18 years old");
        if (experience < 0) throw new ValidationException("Experience cannot be negative");
        if (experience > (age - 16)) throw new ValidationException("Experience cannot exceed age minus 16");

        String normalizedEmail = email.trim().toLowerCase();
        if (driverRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new ConflictException("Driver with email " + normalizedEmail + " already exists");
        }

        String driverId = (id != null && !id.isBlank()) ? id.trim() : "drv-" + UUID.randomUUID().toString().substring(0, 8);
        if (driverRepository.findById(driverId).isPresent()) {
            throw new ConflictException("Driver with id " + driverId + " already exists");
        }

        Driver driver = new Driver(driverId, name.trim(), normalizedEmail, age, experience, hasDeposit);
        return driverRepository.save(driver);
    }

    public Driver getDriver(String id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Driver " + id + " not found"));
    }

    public List<Driver> listDrivers(Integer minAge, Boolean hasCar, Boolean hasDeposit) {
        return driverRepository.findAll().stream()
                .filter(d -> minAge == null || d.getAge() >= minAge)
                .filter(d -> hasCar == null || ((d.getCarId() != null) == hasCar))
                .filter(d -> hasDeposit == null || d.isHasDeposit() == hasDeposit)
                .toList();
    }

    public Driver updateDriver(String id, String name, String email, Integer age, Integer experience, Boolean hasDeposit) {
        Driver driver = getDriver(id);
        if (name != null && !name.isBlank()) driver.setName(name.trim());
        if (email != null && email.contains("@")) {
            String newEmail = email.trim().toLowerCase();
            if (!newEmail.equalsIgnoreCase(driver.getEmail())) {
                if (driverRepository.findByEmail(newEmail).isPresent()) {
                    throw new ConflictException("Email " + newEmail + " already registered");
                }
                driver.setEmail(newEmail);
            }
        }
        int targetAge = age != null ? age : driver.getAge();
        int targetExp = experience != null ? experience : driver.getExperience();
        if (targetAge < 18) throw new ValidationException("Age must be >= 18");
        if (targetExp < 0 || targetExp > (targetAge - 16)) throw new ValidationException("Invalid experience");

        if (age != null) driver.setAge(age);
        if (experience != null) driver.setExperience(experience);
        if (hasDeposit != null) driver.setHasDeposit(hasDeposit);

        return driverRepository.save(driver);
    }

    public void deleteDriver(String id) {
        Driver driver = getDriver(id);
        if (driver.getCarId() != null) {
            throw new ConflictException("Cannot delete driver with active car rental");
        }
        driverRepository.deleteById(id);
    }

    public RentalInfo.RentResult rentCar(String carId, String driverId) {
        if (driverId == null || driverId.isBlank()) throw new ValidationException("driverId is required");

        synchronized (lock) {
            Car car = getCar(carId);
            Driver driver = getDriver(driverId.trim());

            if (car.getStatus() == CarStatus.RENTED) {
                throw new ConflictException("Car " + carId + " is already rented");
            }
            if (driver.getCarId() != null) {
                throw new ConflictException("Driver " + driverId + " already rents car " + driver.getCarId());
            }

            car.validateDriver(driver);

            car.setStatus(CarStatus.RENTED);
            car.setDriverId(driver.getId());
            driver.setCarId(car.getId());

            carRepository.save(car);
            driverRepository.save(driver);

            return new RentalInfo.RentResult(car.getId(), driver.getId(), car.getType(),
                    "Car " + car.getId() + " successfully rented to " + driver.getName());
        }
    }

    public RentalInfo.ReturnResult returnCar(String carId, Long hours, Double distance) {
        long effectiveHours = (hours != null) ? hours : 1L;
        double effectiveDistance = (distance != null) ? distance : 0.0;

        if (effectiveHours <= 0) throw new ValidationException("Hours must be > 0");
        if (effectiveDistance < 0) throw new ValidationException("Distance cannot be negative");

        synchronized (lock) {
            Car car = getCar(carId);
            if (car.getStatus() != CarStatus.RENTED) {
                throw new ConflictException("Car " + carId + " is not currently rented");
            }

            double totalCost = car.calculateCost(effectiveHours, effectiveDistance);
            car.setMileage(car.getMileage() + effectiveDistance);
            car.setStatus(CarStatus.AVAILABLE);

            String driverId = car.getDriverId();
            car.setDriverId(null);
            carRepository.save(car);

            if (driverId != null) {
                driverRepository.findById(driverId).ifPresent(d -> {
                    d.setCarId(null);
                    driverRepository.save(d);
                });
            }

            return new RentalInfo.ReturnResult(car.getId(), driverId, totalCost, car.getMileage(),
                    "Car " + car.getId() + " returned. Total cost: $" + totalCost);
        }
    }
}
