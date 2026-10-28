package app.repository;

import app.model.Car;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CarRepository {
    private final ConcurrentMap<String, Car> storage = new ConcurrentHashMap<>();

    public Car save(Car car) {
        storage.put(car.getId(), car);
        return car;
    }

    public Optional<Car> findById(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(storage.get(id));
    }

    public Optional<Car> findByPlate(String plate) {
        if (plate == null) return Optional.empty();
        return storage.values().stream()
                .filter(c -> c.getPlateNumber().equalsIgnoreCase(plate.trim()))
                .findFirst();
    }

    public List<Car> findAll() {
        return new ArrayList<>(storage.values());
    }

    public boolean deleteById(String id) {
        if (id == null) return false;
        return storage.remove(id) != null;
    }
}
