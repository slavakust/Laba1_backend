package app.model;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;

@Entity
@Table(name = "rentals")
public class Rental implements Persistable<String> {
    @Id
    @Column(length = 64)
    private String id;

    @Transient
    private boolean entityNew = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "car_id", nullable = false)
    private Car car;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    private Long hours;
    private Double distance;

    @Column(name = "total_cost")
    private Double totalCost;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    public Rental() {}

    public Rental(String id, Car car, Driver driver) {
        this.id = id;
        this.car = car;
        this.driver = driver;
        this.active = true;
        this.startedAt = LocalDateTime.now();
    }

    @Override
    public String getId() { return id; }

    @Override
    public boolean isNew() { return entityNew; }

    @PostLoad
    @PostPersist
    void markNotNew() { this.entityNew = false; }
    public void setId(String id) { this.id = id; }
    public Car getCar() { return car; }
    public void setCar(Car car) { this.car = car; }
    public Driver getDriver() { return driver; }
    public void setDriver(Driver driver) { this.driver = driver; }
    public Long getHours() { return hours; }
    public void setHours(Long hours) { this.hours = hours; }
    public Double getDistance() { return distance; }
    public void setDistance(Double distance) { this.distance = distance; }
    public Double getTotalCost() { return totalCost; }
    public void setTotalCost(Double totalCost) { this.totalCost = totalCost; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }
}
