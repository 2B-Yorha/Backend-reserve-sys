package com.tutoring.dto.response;

import com.tutoring.entity.BookingStatus;

import java.time.LocalDateTime;

public record BookingResponse(

        Long id,
        Long studentId,
        Long tutorProfileId,
        Long subjectId,
        LocalDateTime sessionStart,
        LocalDateTime sessionEnd,
        BookingStatus status,
        LocalDateTime createdAt

) {
}
