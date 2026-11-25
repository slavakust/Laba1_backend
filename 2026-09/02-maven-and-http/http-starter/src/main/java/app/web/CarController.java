package app.web;

import app.model.Car;
import app.model.CarStatus;
import app.model.CarType;
import app.service.RentalService;
import app.web.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/cars")
public class CarController {
    private final RentalService rentalService;

    public CarController(RentalService rentalService) {
        this.rentalService = rentalService;
    }

    @GetMapping
    public List<Car> list(
            @RequestParam(required = false) CarType type,
            @RequestParam(required = false) CarStatus status,
            @RequestParam(required = false) Integer minYear,
            @RequestParam(required = false) String model
    ) {
        return rentalService.listCars(type, status, minYear, model);
    }

    @GetMapping("/{id}")
    public Car get(@PathVariable String id) {
        return rentalService.getCar(id);
    }

    @PostMapping
    public ResponseEntity<Car> create(@Valid @RequestBody CreateCarRequest req) {
        Car car = rentalService.createCar(
                req.id(), req.brand(), req.model(), req.plateNumber(),
                req.productionYear(), req.mileage(), req.type()
        );
        return ResponseEntity.created(URI.create("/cars/" + car.getId())).body(car);
    }

    @PutMapping("/{id}")
    public Car update(@PathVariable String id, @Valid @RequestBody UpdateCarRequest req) {
        return rentalService.updateCar(
                id, req.brand(), req.model(), req.plateNumber(),
                req.productionYear(), req.mileage()
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        rentalService.deleteCar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/rent")
    public RentResponse rent(@PathVariable String id, @Valid @RequestBody RentRequest req) {
        return rentalService.rentCar(id, req.driverId());
    }

    @PostMapping("/{id}/return")
    public ReturnResponse returnCar(@PathVariable String id, @Valid @RequestBody(required = false) ReturnRequest req) {
        Long hours = req != null ? req.hours() : null;
        Double distance = req != null ? req.distance() : null;
        return rentalService.returnCar(id, hours, distance);
    }
}
