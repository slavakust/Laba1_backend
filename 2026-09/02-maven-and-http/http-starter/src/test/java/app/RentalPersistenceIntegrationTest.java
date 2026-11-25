package app;

import app.exception.ConflictException;
import app.exception.ValidationException;
import app.model.Car;
import app.model.CarStatus;
import app.model.CarType;
import app.repository.CarRepository;
import app.repository.DriverRepository;
import app.repository.RentalRepository;
import app.service.RentalService;
import app.web.dto.*;
import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "app.seed=false",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "spring.jpa.show-sql=false"
})
public class RentalPersistenceIntegrationTest {

    @Autowired
    private RentalService service;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("truncate table rentals, cars, drivers cascade");
    }

    @Test
    void testFlywayAppliedAllSchemaVersions() {
        Integer versions = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where success = true", Integer.class);
        assertNotNull(versions);
        assertTrue(versions >= 3);
    }

    @Test
    void testPersistLinkedEntitiesAndCompleteRentalLifecycle() {
        service.createCar("c-db-1", "Porsche", "Panamera", "P111PP", 2024, 5000.0, CarType.PREMIUM);
        service.createDriver("d-db-1", "Alice", "alice@db.com", 28, 5, true);

        RentalResponse createdRental = service.createRental("r-db-1", "c-db-1", "d-db-1");
        assertTrue(createdRental.active());
        assertEquals("c-db-1", createdRental.car().id());
        assertEquals("d-db-1", createdRental.driver().id());

        CarResponse loadedCar = service.getCar("c-db-1");
        assertEquals(CarStatus.RENTED, loadedCar.status());
        assertNotNull(loadedCar.driver());
        assertEquals("Alice", loadedCar.driver().name());

        DriverResponse loadedDriver = service.getDriver("d-db-1");
        assertEquals(1, loadedDriver.rentalCount());
        assertNotNull(loadedDriver.activeCar());
        assertEquals("c-db-1", loadedDriver.activeCar().id());
        assertEquals(1, loadedDriver.rentals().size());

        ReturnResponse ret = service.returnRentalById("r-db-1", 2L, 80.0);
        assertEquals(400.0, ret.totalCost());
        assertEquals(5080.0, ret.mileage());

        CarResponse afterReturnCar = service.getCar("c-db-1");
        assertEquals(CarStatus.AVAILABLE, afterReturnCar.status());
        assertNull(afterReturnCar.driver());
        assertEquals(5080.0, afterReturnCar.mileage());

        DriverResponse afterReturnDriver = service.getDriver("d-db-1");
        assertNull(afterReturnDriver.activeCar());
        assertEquals(1, afterReturnDriver.rentalCount());
        assertFalse(afterReturnDriver.rentals().get(0).active());
        assertEquals(400.0, afterReturnDriver.rentals().get(0).totalCost());

        service.deleteDriver("d-db-1");
        service.deleteCar("c-db-1");
        assertEquals(0, driverRepository.count());
        assertEquals(0, carRepository.count());
        assertEquals(0, rentalRepository.count());
    }

    @Test
    void testTransactionRollsBackCompletelyWhenMultiEntityOperationFails() {
        service.createCar("c-tx-1", "Porsche", "Panamera", "P222PP", 2024, 5000.0, CarType.PREMIUM);
        service.createDriver("d-tx-no-dep", "Young", "young@db.com", 20, 1, false);

        assertThrows(ValidationException.class, () ->
                service.createRental("r-fail", "c-tx-1", "d-tx-no-dep"));

        assertEquals(CarStatus.AVAILABLE, service.getCar("c-tx-1").status());
        assertEquals(0, service.getDriver("d-tx-no-dep").rentalCount());
        assertEquals(0, rentalRepository.count());

        service.createDriver("d-tx-ok", "Bob", "bob@db.com", 30, 7, true);
        service.createRental("r-tx-ok", "c-tx-1", "d-tx-ok");

        jdbcTemplate.update("delete from rentals where id = ?", "r-tx-ok");

        assertThrows(ConflictException.class, () ->
                service.returnCar("c-tx-1", 3L, 120.0));

        Car persistedCar = carRepository.findByIdWithDriver("c-tx-1").orElseThrow();
        assertEquals(CarStatus.RENTED, persistedCar.getStatus());
        assertEquals(5000.0, persistedCar.getMileage());
        assertEquals("d-tx-ok", persistedCar.getDriverId());
    }

    @Test
    void testNoNPlusOneQueriesWhenListingDozensOfLinkedEntities() {
        int total = 40;
        for (int i = 0; i < total; i++) {
            service.createCar("c-n1-" + i, "Brand" + i, "Model" + i, "PL" + (1000 + i), 2022, 1000.0, CarType.ECONOMY);
            service.createDriver("d-n1-" + i, "Driver" + i, "drv" + i + "@n1.com", 25, 3, true);
            service.createRental("r-n1-" + i, "c-n1-" + i, "d-n1-" + i);
            if (i % 2 == 0) {
                service.returnRentalById("r-n1-" + i, 2L, 30.0);
            }
        }

        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);

        stats.clear();
        List<CarResponse> cars = service.listCars(null, null, null, null);
        long carQueries = stats.getPrepareStatementCount();
        assertEquals(total, cars.size());
        assertEquals(1, carQueries);

        stats.clear();
        List<DriverResponse> drivers = service.listDrivers(null, null, null);
        long driverQueries = stats.getPrepareStatementCount();
        assertEquals(total, drivers.size());
        assertEquals(1, driverQueries);

        stats.clear();
        List<RentalResponse> rentals = service.listRentals(null);
        long rentalQueries = stats.getPrepareStatementCount();
        assertEquals(total, rentals.size());
        assertEquals(1, rentalQueries);
    }

    @Test
    void testFlywayMigrationUpgradeFromV1ToV2PreservesExistingData() throws Exception {
        String schema = "migration_upgrade_check";
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            st.execute("drop schema if exists " + schema + " cascade");
            st.execute("create schema " + schema);
        }

        try {
            Flyway v1 = Flyway.configure()
                    .dataSource(dataSource)
                    .schemas(schema)
                    .locations("classpath:db/migration")
                    .target("1")
                    .load();
            v1.migrate();

            try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
                st.execute("insert into " + schema + ".drivers (id, name, email, age, experience, has_deposit) " +
                        "values ('d-old', 'Old Driver', 'old@v1.com', 35, 10, true)");
                st.execute("insert into " + schema + ".cars (id, car_type, brand, model, plate_number, production_year, mileage, status, driver_id) " +
                        "values ('c-old', 'ECONOMY', 'Kia', 'Rio', 'V100VV', 2020, 12345.0, 'RENTED', 'd-old')");
            }

            Flyway v2 = Flyway.configure()
                    .dataSource(dataSource)
                    .schemas(schema)
                    .locations("classpath:db/migration")
                    .load();
            v2.migrate();

            String driverName = jdbcTemplate.queryForObject(
                    "select name from " + schema + ".drivers where id = 'd-old'", String.class);
            Integer rentalCount = jdbcTemplate.queryForObject(
                    "select rental_count from " + schema + ".drivers where id = 'd-old'", Integer.class);
            Integer backfilledRentals = jdbcTemplate.queryForObject(
                    "select count(*) from " + schema + ".rentals where car_id = 'c-old' and driver_id = 'd-old' and active = true",
                    Integer.class);

            assertEquals("Old Driver", driverName);
            assertEquals(1, rentalCount);
            assertEquals(1, backfilledRentals);
        } finally {
            try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
                st.execute("drop schema if exists " + schema + " cascade");
            }
        }
    }

    @Test
    void testConcurrentRentSameCarAgainstPostgresOnlyOneSucceeds() throws Exception {
        service.createCar("c-race", "Kia", "Rio", "R777RR", 2022, 100.0, CarType.ECONOMY);
        int threads = 8;
        for (int i = 0; i < threads; i++) {
            service.createDriver("d-race-" + i, "Driver " + i, "race" + i + "@db.com", 25, 3, true);
        }

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CyclicBarrier barrier = new CyclicBarrier(threads);
        AtomicInteger succeeded = new AtomicInteger(0);
        AtomicInteger rejected = new AtomicInteger(0);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            String driverId = "d-race-" + i;
            futures.add(executor.submit(() -> {
                try {
                    barrier.await();
                    service.rentCar("c-race", driverId);
                    succeeded.incrementAndGet();
                } catch (RuntimeException e) {
                    rejected.incrementAndGet();
                } catch (Exception e) {
                    fail(e);
                }
            }));
        }

        for (Future<?> f : futures) {
            f.get();
        }
        executor.shutdown();

        assertEquals(1, succeeded.get());
        assertEquals(threads - 1, rejected.get());
        Integer activeRentals = jdbcTemplate.queryForObject(
                "select count(*) from rentals where car_id = 'c-race' and active = true", Integer.class);
        assertEquals(1, activeRentals);
    }
}
