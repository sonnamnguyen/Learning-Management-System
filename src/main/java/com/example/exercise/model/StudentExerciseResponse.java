package com.example.exercise.model;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StudentExerciseResponse {
    private int easyExercises;
    private int hardExercises;
    private int mediumExercises;
    private int userExercises;
    private int userPassedExercises;
    private int perfectScoreUserExercises;
    private int userEasyExercises;
    private int userHardExercises;
    private int userMediumExercises;
    private Map<String, Integer> passedTestsPerMonth;
    private int exercisesWithMoreThanFiveAttempts;
    private int exercisesSubmittedMidnight;
    private int exercisesSubmittedEarly;
    private int easyExercisesNoLanguage;
    private int hardExercisesNoLanguage;
    private int mediumExercisesNoLanguage;
    private int userEasyExercisesNoLanguage;
    private int userHardExercisesNoLanguage;
    private int userMediumExercisesNoLanguage;
    private int totalJava;
    private int totalC;
    private int totalCsharp;
    private int totalCpp;
    private int totalSql;
    private int userJava;
    private int userC;
    private int userCsharp;
    private int userCpp;
    private int userSql;
}
