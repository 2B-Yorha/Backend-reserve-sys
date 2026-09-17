package com.tutoring.dto.request;

import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.util.List;

public record TutorProfileUpdateRequest(
        String bio,
        @DecimalMin(value = "0.0", inclusive = true) BigDecimal hourlyRate,
        List<Long> subjectIds
        ) {
}
