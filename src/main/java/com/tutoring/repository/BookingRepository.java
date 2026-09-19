package com.tutoring.repository;

import com.tutoring.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByStudentIdOrderBySessionStartDesc(Long studentId);
    List<Booking> findByTutorProfileIdOrderBySessionStartDesc(Long tutorProfileId);

}
