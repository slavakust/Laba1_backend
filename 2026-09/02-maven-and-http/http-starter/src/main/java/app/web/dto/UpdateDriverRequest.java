package app.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateDriverRequest(
        @Size(max = 100) String name,
        @Email @Size(max = 150) String email,
        @Min(18) Integer age,
        @Min(0) Integer experience,
        Boolean hasDeposit
) {}
