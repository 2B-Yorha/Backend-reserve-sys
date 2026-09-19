package com.tutoring.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record BookingRequest(

        @NotNull Long tutorProfileId,
        @NotNull Long subjectId,
        @NotNull LocalDateTime sessionStart,
        @NotNull LocalDateTime sessionEnd

        ) {
}
