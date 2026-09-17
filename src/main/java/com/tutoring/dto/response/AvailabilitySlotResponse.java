package com.tutoring.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;

public record AvailabilitySlotResponse(
        Long id,
        boolean isRecurring,
        Integer dayOfWeek,
        LocalDate specificDate,
        LocalTime startTime,
        LocalTime endTime,
        boolean isBlocked
) {
}
