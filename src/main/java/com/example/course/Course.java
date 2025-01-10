package com.example.course;

import com.example.tag.Tag;
import com.example.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Entity
@NoArgsConstructor
@Getter
@Setter
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String code;

    @Lob
    private String description;  // Rich text field equivalent in Spring Boot

    @ManyToOne
    @JoinColumn(name = "creator_id", nullable = true)
    private User creator;

    @ManyToOne
    @JoinColumn(name = "instructor_id", nullable = true)
    private User instructor;

    private boolean published;

    @ManyToMany
    @JoinTable(
            name = "course_prerequisites",
            joinColumns = @JoinColumn(name = "course_id"),
            inverseJoinColumns = @JoinColumn(name = "prerequisite_id")
    )
    private List<Course> prerequisites;

    @ManyToMany
    @JoinTable(
            name = "course_tags",
            joinColumns = @JoinColumn(name = "course_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private List<Tag> tags;

    private String image;

    private float price;
    private float discount;

    private int durationInWeeks; // Added field for course duration
    private String language; // Added field for course language
    private String level; // Added field for course level (e.g., Beginner, Intermediate, Advanced)

    public float getDiscountedPrice() {
        return price * (1 - discount / 100);
    }

    @Override
    public String toString() {
        return name;
    }
}
