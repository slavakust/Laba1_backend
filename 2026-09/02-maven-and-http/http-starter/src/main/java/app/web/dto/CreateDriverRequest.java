package app.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateDriverRequest(
        String id,
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotNull @Min(18) Integer age,
        @NotNull @Min(0) Integer experience,
        @NotNull Boolean hasDeposit
) {}
