package app.repository;

import app.model.Driver;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DriverRepository {
    private final ConcurrentMap<String, Driver> storage = new ConcurrentHashMap<>();

    public Driver save(Driver driver) {
        storage.put(driver.getId(), driver);
        return driver;
    }

    public Optional<Driver> findById(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(storage.get(id));
    }

    public Optional<Driver> findByEmail(String email) {
        if (email == null) return Optional.empty();
        return storage.values().stream()
                .filter(d -> d.getEmail().equalsIgnoreCase(email.trim()))
                .findFirst();
    }

    public List<Driver> findAll() {
        return new ArrayList<>(storage.values());
    }

    public boolean deleteById(String id) {
        if (id == null) return false;
        return storage.remove(id) != null;
    }
}
