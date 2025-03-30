package com.example.exercise.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class StudentExerciseAttemptResponse {
    Integer id;
    String name;
    String time;

    public StudentExerciseAttemptResponse(Integer id, String name, LocalDateTime attemptDate) {
        this.id = id;
        this.name = name;
        this.time = formatRelativeTime(attemptDate);
    }

    private String formatRelativeTime(LocalDateTime attemptDate) {
        LocalDateTime now = LocalDateTime.now();

        long months = ChronoUnit.MONTHS.between(attemptDate, now);
        long days = ChronoUnit.DAYS.between(attemptDate, now);
        long hours = ChronoUnit.HOURS.between(attemptDate, now);

        if (months > 0) return months + " months ago";
        if (days > 0) return days + " days ago";
        if (hours > 0) return hours + " hours ago";
        return "Just now";
    }

}
