package com.tutoring.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record TutorDetailResponse(
        Long id,
        String fullName,
        String bio,
        BigDecimal hourlyRate,
        List<SubjectResponse> subjects
) {
}
