package com.tutoring.service;


import com.tutoring.dto.request.BookingRequest;
import com.tutoring.dto.response.BookingResponse;
import com.tutoring.entity.AvailabilitySlot;
import com.tutoring.entity.Booking;
import com.tutoring.entity.BookingStatus;
import com.tutoring.exception.BookingNotFoundException;
import com.tutoring.exception.CancellationNotAllowedException;
import com.tutoring.exception.SlotUnavailableException;
import com.tutoring.repository.AvailabilitySlotRepository;
import com.tutoring.repository.BookingRepository;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final AvailabilitySlotRepository availabilitySlotRepository;
    private final AvailabilityResolver availabilityResolver;

    public BookingService(BookingRepository bookingRepository,
                          AvailabilitySlotRepository availabilitySlotRepository,
                          AvailabilityResolver availabilityResolver) {
        this.bookingRepository = bookingRepository;
        this.availabilitySlotRepository = availabilitySlotRepository;
        this.availabilityResolver = availabilityResolver;
    }

    @Transactional
    public BookingResponse createBooking(Long studentId, BookingRequest request){
        if(!request.sessionStart().isBefore(request.sessionEnd())){
            throw new IllegalArgumentException("sessionStart must be before sessionEnd");
        }

        LocalDate day = request.sessionStart().toLocalDate();

        List<AvailabilitySlot> slots =
                availabilitySlotRepository.findAllByTutorProfile(request.tutorProfileId());

        boolean withinAvailableWindow = availabilityResolver
                .resolve(slots, day, day)
                .stream()
                .anyMatch(w -> !request.sessionStart().isBefore(w.start())
                        && !request.sessionEnd().isAfter(w.end()));

        if (!withinAvailableWindow) {
            throw new SlotUnavailableException("Requested time is not within tutor's available slots");
        }

        Booking booking = new Booking();
        booking.setStudentId(studentId);
        booking.setTutorProfileId(request.tutorProfileId());
        booking.setSubjectId(request.subjectId());
        booking.setSessionStart(request.sessionStart());
        booking.setSessionEnd(request.sessionEnd());
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setCreatedAt(LocalDateTime.now());

        try {
            Booking saved = bookingRepository.saveAndFlush(booking);
            return toResponse(saved);
        } catch(DataIntegrityViolationException e){
            if (isExclusionViolation(e)){
                throw new SlotUnavailableException("This slot was just booked - please choose another slot");
            }
            throw e;
        }
    }

    public List<BookingResponse> getBookingsForStudent(Long studentId){
        return bookingRepository.findByStudentIdOrderBySessionStartDesc(studentId)
                .stream().map(this::toResponse).toList();
    }

    public List<BookingResponse> getBookingsForTutor(Long tutorProfileId){
        return bookingRepository.findByTutorProfileIdOrderBySessionStartDesc(tutorProfileId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public BookingResponse cancelBooking(Long studentId, Long bookingId){
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found: " + bookingId));

        if (!booking.getStudentId().equals(studentId)){
            throw new BookingNotFoundException("Booking not found: " + bookingId);
        }

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.COMPLETED){
            throw new CancellationNotAllowedException("Booking is already "+ booking.getStatus());
        }

        if(LocalDateTime.now().isAfter(booking.getSessionStart().minusHours(24))){
            throw new CancellationNotAllowedException(
                    "Cancellation window has passed (must cancel more than 24 hours before session start)"
            );
        }
        booking.setStatus(BookingStatus.CANCELLED);
        return toResponse(bookingRepository.save(booking));
    }

    private boolean isExclusionViolation(DataIntegrityViolationException e){
        Throwable root = e.getRootCause();
        String msg = root != null ? root.getMessage() : e.getMessage();
        return msg != null && msg.contains("no_overlapping_bookings");
    }

    private BookingResponse toResponse(Booking b){
        return new BookingResponse(
                b.getId(),
                b.getStudentId(),
                b.getTutorProfileId(),
                b.getSubjectId(),
                b.getSessionStart(),
                b.getSessionEnd(),
                b.getStatus(),
                b.getCreatedAt()
        );
    }

}
