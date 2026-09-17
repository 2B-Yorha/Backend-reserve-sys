package com.tutoring.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record AvailabilitySlotRequest(
        @NotNull Boolean isRecurring,
        Integer dayOfWeek,
        LocalDate specificDate,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        Boolean isBlocked
        ) {
}
