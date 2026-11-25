package app.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateDriverRequest(
        @Size(max = 64) String id,
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email @Size(max = 150) String email,
        @NotNull @Min(18) Integer age,
        @NotNull @Min(0) Integer experience,
        @NotNull Boolean hasDeposit
) {}
