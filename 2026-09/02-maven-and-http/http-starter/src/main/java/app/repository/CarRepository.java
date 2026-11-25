package app.repository;

import app.model.Car;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class CarRepository {
    private final ConcurrentMap<String, Car> storage = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> plateIndex = new ConcurrentHashMap<>();

    public Car save(Car car) {
        Car stored = car.copy();
        storage.put(stored.getId(), stored);
        if (stored.getPlateNumber() != null) {
            plateIndex.putIfAbsent(stored.getPlateNumber().trim().toUpperCase(), stored.getId());
        }
        return stored.copy();
    }

    public boolean reservePlate(String plate, String carId) {
        if (plate == null || carId == null) return false;
        return plateIndex.putIfAbsent(plate.trim().toUpperCase(), carId) == null;
    }

    public void releasePlate(String plate, String carId) {
        if (plate != null && carId != null) {
            plateIndex.remove(plate.trim().toUpperCase(), carId);
        }
    }

    public Optional<Car> findById(String id) {
        if (id == null) return Optional.empty();
        Car car = storage.get(id);
        return car == null ? Optional.empty() : Optional.of(car.copy());
    }

    public Optional<Car> findByPlate(String plate) {
        if (plate == null) return Optional.empty();
        String norm = plate.trim().toUpperCase();
        String id = plateIndex.get(norm);
        if (id != null) {
            Car car = storage.get(id);
            if (car != null && norm.equalsIgnoreCase(car.getPlateNumber())) {
                return Optional.of(car.copy());
            }
        }
        return storage.values().stream()
                .filter(c -> c.getPlateNumber().equalsIgnoreCase(norm))
                .findFirst()
                .map(Car::copy);
    }

    public List<Car> findAll() {
        return storage.values().stream()
                .map(Car::copy)
                .toList();
    }

    public boolean deleteById(String id) {
        if (id == null) return false;
        Car removed = storage.remove(id);
        if (removed != null) {
            if (removed.getPlateNumber() != null) {
                plateIndex.remove(removed.getPlateNumber().trim().toUpperCase(), id);
            }
            return true;
        }
        return false;
    }
}
