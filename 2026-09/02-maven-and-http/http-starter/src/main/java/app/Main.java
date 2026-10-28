package app;

import app.http.CarHandler;
import app.http.DriverHandler;
import app.model.CarType;
import app.repository.CarRepository;
import app.repository.DriverRepository;
import app.service.RentalService;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class Main {

    private static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        CarRepository carRepository = new CarRepository();
        DriverRepository driverRepository = new DriverRepository();
        RentalService rentalService = new RentalService(carRepository, driverRepository);

        rentalService.createCar("car-eco-1", "Kia", "Rio", "A101AA", 2021, 45000.0, CarType.ECONOMY);
        rentalService.createCar("car-prem-1", "Porsche", "Panamera", "P777PP", 2024, 5000.0, CarType.PREMIUM);

        rentalService.createDriver("drv-1", "Alice Young", "alice@test.com", 19, 1, false);
        rentalService.createDriver("drv-2", "Bob Senior", "bob@test.com", 30, 7, true);

        server.createContext("/ping", new PingHandler());
        server.createContext("/cars", new CarHandler(rentalService));
        server.createContext("/drivers", new DriverHandler(rentalService));

        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();

        System.out.println("Server started on port " + PORT);
    }
}
