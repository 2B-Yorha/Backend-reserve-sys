package com.tutoring.service;


import com.tutoring.dto.request.AvailabilitySlotRequest;
import com.tutoring.dto.request.TutorProfileUpdateRequest;
import com.tutoring.dto.response.*;
import com.tutoring.entity.AvailabilitySlot;
import com.tutoring.entity.Subject;
import com.tutoring.entity.TutorProfile;
import com.tutoring.entity.User;
import com.tutoring.exception.InvalidAvailabilityException;
import com.tutoring.exception.ResourceNotFoundException;
import com.tutoring.repository.AvailabilitySlotRepository;
import com.tutoring.repository.SubjectRepository;
import com.tutoring.repository.TutorProfileRespository;
import com.tutoring.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TutorService {

    private final TutorProfileRespository tutorProfileRespository;
    private final SubjectRepository subjectRepository;
    private final AvailabilitySlotRepository availabilitySlotRepository;
    private final UserRepository userRepository;
    private final AvailabilityResolver availabilityResolver;

    public TutorService(TutorProfileRespository tutorProfileRespository, SubjectRepository subjectRepository, AvailabilitySlotRepository availabilitySlotRepository, UserRepository userRepository, AvailabilityResolver availabilityResolver) {
        this.tutorProfileRespository = tutorProfileRespository;
        this.subjectRepository = subjectRepository;
        this.availabilitySlotRepository = availabilitySlotRepository;
        this.userRepository = userRepository;
        this.availabilityResolver = availabilityResolver;
    }

    @Transactional
    public TutorDetailResponse updateOwnProfile(String userEmail, TutorProfileUpdateRequest request){
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow( () -> new ResourceNotFoundException("User not found"));

        TutorProfile profile = tutorProfileRespository.findByUserId(user.getId())
                .orElseGet(() -> {
                    TutorProfile p = new TutorProfile();
                    p.setUser(user);
                    return tutorProfileRespository.save(p);
                });

        profile.setBio(request.bio());
        profile.setHourlyRate(request.hourlyRate());

        if(request.subjectIds() != null){
            List<Subject> subjects = subjectRepository.findAllByIdIn(request.subjectIds());
            if (subjects.size() != request.subjectIds().size()){
                throw new ResourceNotFoundException("One or more subject IDs do not exist");
            }
            profile.setSubjects(Set.copyOf(subjects));
        }

        return toDetailResponse(profile);
    }


    @Transactional
    public AvailabilitySlotResponse addAvailability(String userEmail, AvailabilitySlotRequest request){
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        TutorProfile profile = tutorProfileRespository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Tutor profile not found — set up your profile first"));

        validateSlotRequest(request);

        AvailabilitySlot slot = new AvailabilitySlot();
        slot.setTutorProfile(profile);
        slot.setRecurring(request.isRecurring());
        slot.setDayOfWeek(request.isRecurring() ? request.dayOfWeek() : null);
        slot.setSpecificDate(request.isRecurring() ? null : request.specificDate());
        slot.setStartTime(request.startTime());
        slot.setEndTime(request.endTime());
        slot.setBlocked(request.isBlocked() != null && request.isBlocked());

        return toSlotResponse(availabilitySlotRepository.save(slot));
    }

    @Transactional
    public void deleteAvailability(String userEmail, Long slotId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        TutorProfile profile = tutorProfileRespository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Tutor profile not found"));

        AvailabilitySlot slot = availabilitySlotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Availability slot not found"));

        if (!slot.getTutorProfile().getId().equals(profile.getId())) {
            throw new ResourceNotFoundException("Availability slot not found");
        }

        availabilitySlotRepository.delete(slot);
    }

    public List<TutorSummaryResponse> listTutors(String subject) {
        List<TutorProfile> profiles = (subject == null || subject.isBlank())
                ? tutorProfileRespository.findAll()
                : tutorProfileRespository.findBySubjectNameIgnoreCase(subject);

        return profiles.stream().map(this::toSummaryResponse).toList();
    }

    public TutorDetailResponse getTutorDetail(Long tutorProfileId) {
        TutorProfile profile = tutorProfileRespository.findById(tutorProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Tutor not found"));
        return toDetailResponse(profile);
    }

    public List<SlotResponse> getBookableSlots(Long tutorProfileId, LocalDate from, LocalDate to) {
        if (!tutorProfileRespository.existsById(tutorProfileId)) {
            throw new ResourceNotFoundException("Tutor not found");
        }

        List<AvailabilitySlot> slots = availabilitySlotRepository.findAllByTutorProfile(tutorProfileId);
        List<AvailabilityResolver.Window> windows = availabilityResolver.resolve(slots, from, to);



        return windows.stream().map(w -> new SlotResponse(w.start(), w.end())).toList();
    }

    private void validateSlotRequest(AvailabilitySlotRequest req) {
        if (!req.startTime().isBefore(req.endTime())) {
            throw new InvalidAvailabilityException("startTime must be before endTime");
        }
        if (req.isRecurring()) {
            if (req.dayOfWeek() == null || req.dayOfWeek() < 0 || req.dayOfWeek() > 6) {
                throw new InvalidAvailabilityException("dayOfWeek must be 0-6 for recurring slots");
            }
        } else if (req.specificDate() == null) {
            throw new InvalidAvailabilityException("specificDate is required for non-recurring slots");
        }
    }

    private TutorSummaryResponse toSummaryResponse(TutorProfile profile) {
        return new TutorSummaryResponse(
                profile.getId(),
                profile.getUser().getFullName(),
                profile.getBio(),
                profile.getHourlyRate(),
                profile.getSubjects().stream().map(Subject::getName).toList()
        );
    }

    private TutorDetailResponse toDetailResponse(TutorProfile profile) {
        return new TutorDetailResponse(
                profile.getId(),
                profile.getUser().getFullName(),
                profile.getBio(),
                profile.getHourlyRate(),
                profile.getSubjects().stream()
                        .map(s -> new SubjectResponse(s.getId(), s.getName()))
                        .collect(Collectors.toList())
        );
    }

    private AvailabilitySlotResponse toSlotResponse(AvailabilitySlot slot) {
        return new AvailabilitySlotResponse(
                slot.getId(), slot.isRecurring(), slot.getDayOfWeek(),
                slot.getSpecificDate(), slot.getStartTime(), slot.getEndTime(), slot.isBlocked()
        );
    }

}
