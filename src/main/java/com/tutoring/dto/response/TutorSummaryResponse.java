package com.tutoring.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record TutorSummaryResponse(
        Long id,
        String fullName,
        String bio,
        BigDecimal hourlyRate,
        List<String> subjects
) {
}
