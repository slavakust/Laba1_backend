package app.repository;

import app.model.Driver;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class DriverRepository {
    private final ConcurrentMap<String, Driver> storage = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> emailIndex = new ConcurrentHashMap<>();

    public Driver save(Driver driver) {
        Driver stored = driver.copy();
        storage.put(stored.getId(), stored);
        if (stored.getEmail() != null) {
            emailIndex.putIfAbsent(stored.getEmail().trim().toLowerCase(), stored.getId());
        }
        return stored.copy();
    }

    public boolean reserveEmail(String email, String driverId) {
        if (email == null || driverId == null) return false;
        return emailIndex.putIfAbsent(email.trim().toLowerCase(), driverId) == null;
    }

    public void releaseEmail(String email, String driverId) {
        if (email != null && driverId != null) {
            emailIndex.remove(email.trim().toLowerCase(), driverId);
        }
    }

    public Optional<Driver> findById(String id) {
        if (id == null) return Optional.empty();
        Driver driver = storage.get(id);
        return driver == null ? Optional.empty() : Optional.of(driver.copy());
    }

    public Optional<Driver> findByEmail(String email) {
        if (email == null) return Optional.empty();
        String norm = email.trim().toLowerCase();
        String id = emailIndex.get(norm);
        if (id != null) {
            Driver driver = storage.get(id);
            if (driver != null && norm.equalsIgnoreCase(driver.getEmail())) {
                return Optional.of(driver.copy());
            }
        }
        return storage.values().stream()
                .filter(d -> d.getEmail().equalsIgnoreCase(norm))
                .findFirst()
                .map(Driver::copy);
    }

    public List<Driver> findAll() {
        return storage.values().stream()
                .map(Driver::copy)
                .toList();
    }

    public boolean deleteById(String id) {
        if (id == null) return false;
        Driver removed = storage.remove(id);
        if (removed != null) {
            if (removed.getEmail() != null) {
                emailIndex.remove(removed.getEmail().trim().toLowerCase(), id);
            }
            return true;
        }
        return false;
    }
}
