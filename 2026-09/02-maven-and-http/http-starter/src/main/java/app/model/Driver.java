package app.model;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "drivers")
public class Driver implements Persistable<String> {
    @Id
    @Column(length = 64)
    private String id;

    @Transient
    private boolean entityNew = true;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private int age;

    @Column(nullable = false)
    private int experience;

    @Column(name = "has_deposit", nullable = false)
    private boolean hasDeposit;

    @Column(name = "rental_count", nullable = false)
    private int rentalCount;

    @Version
    private Long version;

    @OneToMany(mappedBy = "driver")
    @OrderBy("startedAt desc")
    private List<Rental> rentals = new ArrayList<>();

    public Driver() {}

    public Driver(String id, String name, String email, int age, int experience, boolean hasDeposit) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.age = age;
        this.experience = experience;
        this.hasDeposit = hasDeposit;
        this.rentalCount = 0;
    }

    @Override
    public String getId() { return id; }

    @Override
    public boolean isNew() { return entityNew; }

    @PostLoad
    @PostPersist
    void markNotNew() { this.entityNew = false; }

    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public int getExperience() { return experience; }
    public void setExperience(int experience) { this.experience = experience; }
    public boolean isHasDeposit() { return hasDeposit; }
    public void setHasDeposit(boolean hasDeposit) { this.hasDeposit = hasDeposit; }
    public int getRentalCount() { return rentalCount; }
    public void setRentalCount(int rentalCount) { this.rentalCount = rentalCount; }
    public List<Rental> getRentals() { return rentals; }
    public void setRentals(List<Rental> rentals) { this.rentals = rentals; }

    public Car getActiveCar() {
        return rentals.stream()
                .filter(Rental::isActive)
                .map(Rental::getCar)
                .findFirst()
                .orElse(null);
    }

    public String getCarId() {
        Car car = getActiveCar();
        return car != null ? car.getId() : null;
    }
}
