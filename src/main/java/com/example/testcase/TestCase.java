package com.example.testcase;

import com.example.assessment.model.ProgrammingLanguage;
import com.example.exercise.model.Exercise;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String input;

    private String expectedOutput;

    @Column(columnDefinition = "TEXT") // Thêm tagSQL vào database
    private String sqlTagNumber;
    @Column(columnDefinition = "boolean")
    private boolean isHidden;



    @ManyToOne
    @JoinColumn(name = "exercise_id" ,referencedColumnName = "id", nullable = false)
    @JsonIgnore
    private Exercise exercise;

    @Override
    public String toString() {
        return "TestCase " + id + ": Input: " + input + " | Expected Output: " + expectedOutput;
    }




}