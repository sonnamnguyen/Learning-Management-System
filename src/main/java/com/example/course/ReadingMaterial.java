package com.example.course;

import com.example.course.material.CourseMaterial;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@Getter
@Setter
public class ReadingMaterial {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "material_id", nullable = true)
    private CourseMaterial material;

    // @Lob
    // private String content;  // Rich text content, as in RichTextUploadingField

    private String url;  // URL to the reading material
    private String title;

    @Override
    public String toString() {
        return title;
    }
}
