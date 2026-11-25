package app.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;

public record UpdateDriverRequest(
        String name,
        @Email String email,
        @Min(18) Integer age,
        @Min(0) Integer experience,
        Boolean hasDeposit
) {}
