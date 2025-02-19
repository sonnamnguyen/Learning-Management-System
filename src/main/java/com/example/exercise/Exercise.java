package com.example.exercise;

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
public class Exercise {

    public enum Level { EASY, MEDIUM, HARD }

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

    @Enumerated(EnumType.STRING)
    private Level level = Level.EASY;

    @OneToMany(mappedBy = "exercise", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TestCase> testCases = new ArrayList<>();

    // exercise for Java, C, Python
//    public Exercise(String name, String description, ProgrammingLanguage language, String setup, Level level) {
//        this.name = name;
//        this.description = description;
//        this.language = language;
//        this.setup = setup;
//        this.level = level;
//    }
//
//    @Column(name = "init_data", columnDefinition = "TEXT", nullable = true)
//    private String initData;
//
//    @Column(name = "quantity_question", nullable = true)
//    private Integer quantityQuestion;
//
//    public Exercise(String name, String description, ProgrammingLanguage language, Level level, String initData, Integer quantityQuestion) {
//
//    }


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
}