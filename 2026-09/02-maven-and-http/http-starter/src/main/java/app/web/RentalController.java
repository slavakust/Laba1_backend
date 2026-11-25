package app.web;

import app.service.RentalService;
import app.web.dto.CreateRentalRequest;
import app.web.dto.RentalResponse;
import app.web.dto.ReturnRequest;
import app.web.dto.ReturnResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/rentals")
public class RentalController {
    private final RentalService rentalService;

    public RentalController(RentalService rentalService) {
        this.rentalService = rentalService;
    }

    @GetMapping
    public List<RentalResponse> list(@RequestParam(required = false) Boolean active) {
        return rentalService.listRentals(active);
    }

    @GetMapping("/{id}")
    public RentalResponse get(@PathVariable String id) {
        return rentalService.getRental(id);
    }

    @PostMapping
    public ResponseEntity<RentalResponse> create(@Valid @RequestBody CreateRentalRequest req) {
        RentalResponse rental = rentalService.createRental(req.id(), req.carId(), req.driverId());
        return ResponseEntity.created(URI.create("/rentals/" + rental.id())).body(rental);
    }

    @PostMapping("/{id}/return")
    public ReturnResponse returnRental(@PathVariable String id, @Valid @RequestBody(required = false) ReturnRequest req) {
        Long hours = req != null ? req.hours() : null;
        Double distance = req != null ? req.distance() : null;
        return rentalService.returnRentalById(id, hours, distance);
    }
}
