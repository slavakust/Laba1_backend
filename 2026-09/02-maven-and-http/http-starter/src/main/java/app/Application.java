package app;

import app.model.CarType;
import app.service.RentalService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public CommandLineRunner initData(RentalService service) {
        return args -> {
            service.createCar("car-eco-1", "Kia", "Rio", "A101AA", 2021, 45000.0, CarType.ECONOMY);
            service.createCar("car-prem-1", "Porsche", "Panamera", "P777PP", 2024, 5000.0, CarType.PREMIUM);
            service.createDriver("drv-1", "Alice Young", "alice@test.com", 19, 1, false);
            service.createDriver("drv-2", "Bob Senior", "bob@test.com", 30, 7, true);
        };
    }
}
