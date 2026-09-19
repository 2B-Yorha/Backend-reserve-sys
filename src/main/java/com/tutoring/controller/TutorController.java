package com.tutoring.controller;


import com.tutoring.dto.request.AvailabilitySlotRequest;
import com.tutoring.dto.request.TutorProfileUpdateRequest;
import com.tutoring.dto.response.*;
import com.tutoring.repository.UserRepository;
import com.tutoring.service.BookingService;
import com.tutoring.service.TutorService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class TutorController {

    private final TutorService tutorService;
    private final UserRepository userRepository;
    private final BookingService bookingService;

    public TutorController(TutorService tutorService, UserRepository userRepository, BookingService bookingService) {
        this.tutorService = tutorService;
        this.userRepository = userRepository;
        this.bookingService = bookingService;
    }

    @GetMapping("/tutors")
    public List<TutorSummaryResponse> listTutors(@RequestParam(required = false) String subject) {
        return tutorService.listTutors(subject);
    }

    @GetMapping("/tutors/{id}")
    public TutorDetailResponse getTutor(@PathVariable Long id) {
        return tutorService.getTutorDetail(id);
    }

    @GetMapping("/tutors/{id}/slots")
    public List<SlotResponse> getSlots(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return tutorService.getBookableSlots(id, from, to);
    }

    @PutMapping("/tutors/me/profile")
    @PreAuthorize("hasRole('TUTOR')")
    public TutorDetailResponse updateProfile(Authentication auth, @Valid @RequestBody TutorProfileUpdateRequest request) {
        return tutorService.updateOwnProfile(auth.getName(), request);
    }

    @PostMapping("/tutors/me/availability")
    @PreAuthorize("hasRole('TUTOR')")
    public AvailabilitySlotResponse addAvailability(Authentication auth, @Valid @RequestBody AvailabilitySlotRequest request) {
        return tutorService.addAvailability(auth.getName(), request);
    }

    @DeleteMapping("/tutors/me/availability/{id}")
    @PreAuthorize("hasRole('TUTOR')")
    public void deleteAvailability(Authentication auth, @PathVariable Long id) {
        tutorService.deleteAvailability(auth.getName(), id);
    }


    @GetMapping("/me/bookings")
    public ResponseEntity<List<BookingResponse>> myTutorBookings(Authentication auth) {
        Long tutorProfileId = tutorService.getTutorProfileIdForUser(auth.getName());
        return ResponseEntity.ok(bookingService.getBookingsForTutor(tutorProfileId));
    }
}
