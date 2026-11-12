package app.web.dto;

import jakarta.validation.constraints.Min;

public record ReturnRequest(
        @Min(1) Long hours,
        @Min(0) Double distance
) {}
