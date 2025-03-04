package com.example.student_exercise_attemp.model;

import com.example.assessment.model.ProgrammingLanguage;
import com.example.testcase.TestCase;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Exercise {

    public enum Level {EASY, MEDIUM, HARD}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne
    @JoinColumn(name = "programming_language_id", nullable = false)
    private ProgrammingLanguage language;

    @Column(columnDefinition = "TEXT")
    private String setup;

    @Column(columnDefinition = "TEXT")
    private String setupsql;

    @Enumerated(EnumType.STRING)
    private Level level = Level.EASY;

    @OneToMany(mappedBy = "exercise", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TestCase> testCases = new ArrayList<>();

    public List<TestCase> getTwoTestCases() {
        return testCases.size() > 2 ? testCases.subList(0, 2) : new ArrayList<>(testCases);
    }

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