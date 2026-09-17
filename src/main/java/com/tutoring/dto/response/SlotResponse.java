package com.tutoring.dto.response;

import java.time.LocalDateTime;

public record SlotResponse(LocalDateTime start, LocalDateTime end) {
}
