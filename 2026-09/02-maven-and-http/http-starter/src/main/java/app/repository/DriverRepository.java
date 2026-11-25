package app.repository;

import app.model.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver, String> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<Driver> findByEmailIgnoreCase(String email);

    @Query("select distinct d from Driver d left join fetch d.rentals r left join fetch r.car order by d.id")
    List<Driver> findAllWithDetails();

    @Query("select distinct d from Driver d left join fetch d.rentals r left join fetch r.car where d.id = :id")
    Optional<Driver> findByIdWithDetails(@Param("id") String id);
}
