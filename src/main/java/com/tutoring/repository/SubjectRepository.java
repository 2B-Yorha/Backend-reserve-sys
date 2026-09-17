package com.tutoring.repository;

import com.tutoring.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    List<Subject> findAllByIdIn(List<Long> ids);
}
