package com.tutoring.controller;


import com.tutoring.dto.request.BookingRequest;
import com.tutoring.dto.response.BookingResponse;
import com.tutoring.repository.UserRepository;
import com.tutoring.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final UserRepository userRepository;

    public BookingController(BookingService bookingService, UserRepository userRepository) {
        this.bookingService = bookingService;
        this.userRepository = userRepository;
    }


    @PostMapping
    public ResponseEntity<BookingResponse> create(
            Authentication auth,
            @Valid @RequestBody BookingRequest request) {
        Long studentId = resolveUserId(auth);
        BookingResponse response = bookingService.createBooking(studentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<List<BookingResponse>> myBookings(Authentication auth) {
        Long studentId = resolveUserId(auth);
        return ResponseEntity.ok(bookingService.getBookingsForStudent(studentId));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancel(
            Authentication auth,
            @PathVariable Long id) {
        Long studentId = resolveUserId(auth);
        return ResponseEntity.ok(bookingService.cancelBooking(studentId, id));
    }

    private Long resolveUserId(Authentication auth) {
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + email))
                .getId();
    }


}
