package app.repository;

import app.model.Car;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CarRepository extends JpaRepository<Car, String> {

    boolean existsByPlateNumberIgnoreCase(String plateNumber);

    Optional<Car> findByPlateNumberIgnoreCase(String plateNumber);

    @Query("select c from Car c left join fetch c.driver order by c.id")
    List<Car> findAllWithDriver();

    @Query("select c from Car c left join fetch c.driver where c.id = :id")
    Optional<Car> findByIdWithDriver(@Param("id") String id);
}
