package com.tutoring.service;


import com.tutoring.entity.AvailabilitySlot;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Component
public class AvailabilityResolver {

    public record Window(LocalDateTime start, LocalDateTime end) {}

    private record TimeInterval(LocalTime start, LocalTime end) {}

    public List<Window> resolve(List<AvailabilitySlot> slots, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("'from' must not be after 'to'");
        }

        List<Window> result = new ArrayList<>();

        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            int dow = date.getDayOfWeek().getValue() - 1;

            List<TimeInterval> available = new ArrayList<>();
            List<TimeInterval> blocked = new ArrayList<>();

            for (AvailabilitySlot slot : slots) {
                boolean appliesToday = slot.isRecurring()
                        ? Objects.equals(slot.getDayOfWeek(), dow)
                        : date.equals(slot.getSpecificDate());

                if (!appliesToday) continue;

                TimeInterval interval = new TimeInterval(slot.getStartTime(), slot.getEndTime());
                (slot.isBlocked() ? blocked : available).add(interval);
            }

            List<TimeInterval> net = subtract(merge(available), merge(blocked));

            for (TimeInterval interval : net) {
                result.add(new Window(
                        LocalDateTime.of(date, interval.start()),
                        LocalDateTime.of(date, interval.end())
                ));
            }
        }

        return result;
    }

    private List<TimeInterval> merge(List<TimeInterval> intervals) {
        if (intervals.isEmpty()) return intervals;

        List<TimeInterval> sorted = new ArrayList<>(intervals);
        sorted.sort(Comparator.comparing(TimeInterval::start));

        List<TimeInterval> merged = new ArrayList<>();
        TimeInterval current = sorted.get(0);

        for (int i = 1; i < sorted.size(); i++) {
            TimeInterval next = sorted.get(i);
            if (!next.start().isAfter(current.end())) {
                LocalTime newEnd = current.end().isAfter(next.end()) ? current.end() : next.end();
                current = new TimeInterval(current.start(), newEnd);
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);
        return merged;
    }

    private List<TimeInterval> subtract(List<TimeInterval> base, List<TimeInterval> toRemove) {
        List<TimeInterval> result = new ArrayList<>(base);
        for (TimeInterval remove : toRemove) {
            List<TimeInterval> next = new ArrayList<>();
            for (TimeInterval interval : result) {
                next.addAll(subtractOne(interval, remove));
            }
            result = next;
        }
        return result;
    }

    private List<TimeInterval> subtractOne(TimeInterval a, TimeInterval b) {
        if (!b.start().isBefore(a.end()) || !a.start().isBefore(b.end())) {
            return List.of(a);
        }
        List<TimeInterval> pieces = new ArrayList<>();
        if (a.start().isBefore(b.start())) pieces.add(new TimeInterval(a.start(), b.start()));
        if (a.end().isAfter(b.end()))     pieces.add(new TimeInterval(b.end(), a.end()));
        return pieces;
    }


}
