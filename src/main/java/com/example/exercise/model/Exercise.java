package com.example.exercise.model;

import com.example.assessment.model.Assessment;
import com.example.assessment.model.ProgrammingLanguage;
import com.example.testcase.TestCase;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Exercise {

    public enum Level { EASY, MEDIUM, HARD }

    //Dashboard
    public enum ExerciseStatus {
        COMPLETED,
        PENDING
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)  // Nếu status là Enum
    private ExerciseStatus status;

    private String name;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @ManyToOne
    @JoinColumn(name = "programming_language_id", nullable = false)
    private ProgrammingLanguage language;

    @OneToMany(mappedBy = "exercise", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<ExerciseCategory> exerciseCategories;

    @Column(columnDefinition = "TEXT")
    private String setup;

    @Column(columnDefinition = "TEXT")
    private String setupsql;

    @Enumerated(EnumType.STRING)
    private Level level;

    //Asm has exercise
    @ManyToMany(mappedBy = "exercises")
    private Set<Assessment> assessments = new HashSet<>();

    @OneToMany(mappedBy = "exercise", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TestCase> testCases = new ArrayList<>();

    @OneToMany(mappedBy = "submitted_exercise", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StudentExerciseAttempt> studentExerciseAttempts = new ArrayList<>();

    public void setTestCases(List<TestCase> testCases) {
        if (testCases == null) return;
        this.testCases.clear();
        for (TestCase testCase : testCases) {
            this.addTestCase(testCase);
        }
    }

    public void addTestCase(TestCase testCase) {
        if (testCase != null) {
            testCase.setExercise(this);
            testCases.add(testCase);
        }
    }

    //Nếu không phải SQL thì setupsql null
    public void setLanguage(ProgrammingLanguage language) {
        this.language = language;
        if (language != null && !"SQL".equalsIgnoreCase(language.getLanguage()) && this.setupsql != null) {
            this.setupsql = "";
        }
    }
}