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
    Long id;
    String name;
    String code;
    String time;
    Double score;

    public StudentExerciseAttemptResponse(Long id, String name, String code, LocalDateTime attemptDate, Double score) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.time = formatRelativeTime(attemptDate);
        this.score = score;
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
