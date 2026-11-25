package app.repository;

import app.model.Rental;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RentalRepository extends JpaRepository<Rental, String> {

    @Query("select r from Rental r join fetch r.car join fetch r.driver order by r.startedAt desc")
    List<Rental> findAllWithDetails();

    @Query("select r from Rental r join fetch r.car join fetch r.driver where r.id = :id")
    Optional<Rental> findByIdWithDetails(@Param("id") String id);

    Optional<Rental> findFirstByCarIdAndActiveTrue(String carId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Rental r where r.car.id = :carId")
    void deleteAllByCarId(@Param("carId") String carId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Rental r where r.driver.id = :driverId")
    void deleteAllByDriverId(@Param("driverId") String driverId);
}
