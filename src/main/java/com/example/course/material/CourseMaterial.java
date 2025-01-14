package com.example.course.material;

import com.example.course.section.Section;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@Getter
@Setter
public class CourseMaterial {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public enum MaterialType {
        ASSIGNMENTS, LABS, LECTURES, REFERENCES, ASSESSMENTS
    }

    @ManyToOne
    @JoinColumn(name = "section_id", nullable = true)
    private Section section;

    private int materialId;  // Assuming it's a unique identifier for the material

    @Enumerated(EnumType.STRING)
    private MaterialType materialType;

    private int orderNum;

    private String title;

    private Float expectDuration;

    private int wordCount;

    @Override
    public String toString() {
        return String.format("Session ID: %d   Title: %s", section.getId(), title);
    }
}
