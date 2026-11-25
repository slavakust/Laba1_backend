package app.web.dto;

public record DriverSummary(
        String id,
        String name,
        String email,
        int age,
        int experience,
        boolean hasDeposit
) {}
