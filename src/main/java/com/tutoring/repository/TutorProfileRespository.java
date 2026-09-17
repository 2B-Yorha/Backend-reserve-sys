package com.tutoring.repository;

import com.tutoring.entity.TutorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TutorProfileRespository extends JpaRepository<TutorProfile, Long> {

    Optional<TutorProfile> findByUserId(Long userId);

    @Query("select distinct tp from TutorProfile tp join tp.subjects s where lower(s.name) = lower(:subjectName)")
    List<TutorProfile> findBySubjectNameIgnoreCase(String subjectName);
}
