package app.web;

import app.model.Driver;
import app.service.RentalService;
import app.web.dto.CreateDriverRequest;
import app.web.dto.UpdateDriverRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/drivers")
public class DriverController {
    private final RentalService rentalService;

    public DriverController(RentalService rentalService) {
        this.rentalService = rentalService;
    }

    @GetMapping
    public List<Driver> list(
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Boolean hasCar,
            @RequestParam(required = false) Boolean hasDeposit
    ) {
        return rentalService.listDrivers(minAge, hasCar, hasDeposit);
    }

    @GetMapping("/{id}")
    public Driver get(@PathVariable String id) {
        return rentalService.getDriver(id);
    }

    @PostMapping
    public ResponseEntity<Driver> create(@Valid @RequestBody CreateDriverRequest req) {
        Driver driver = rentalService.createDriver(
                req.id(), req.name(), req.email(), req.age(),
                req.experience(), req.hasDeposit()
        );
        return ResponseEntity.created(URI.create("/drivers/" + driver.getId())).body(driver);
    }

    @PutMapping("/{id}")
    public Driver update(@PathVariable String id, @Valid @RequestBody UpdateDriverRequest req) {
        return rentalService.updateDriver(
                id, req.name(), req.email(), req.age(),
                req.experience(), req.hasDeposit()
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        rentalService.deleteDriver(id);
        return ResponseEntity.noContent().build();
    }
}
