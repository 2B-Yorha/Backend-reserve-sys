package com.tutoring.controller;


import com.tutoring.dto.request.AvailabilitySlotRequest;
import com.tutoring.dto.request.TutorProfileUpdateRequest;
import com.tutoring.dto.response.AvailabilitySlotResponse;
import com.tutoring.dto.response.SlotResponse;
import com.tutoring.dto.response.TutorDetailResponse;
import com.tutoring.dto.response.TutorSummaryResponse;
import com.tutoring.service.TutorService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class TutorController {

    private final TutorService tutorService;

    public TutorController(TutorService tutorService) {
        this.tutorService = tutorService;
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
}
