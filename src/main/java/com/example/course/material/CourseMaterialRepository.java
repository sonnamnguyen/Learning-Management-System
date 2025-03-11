package com.example.course.material;

import com.example.course.section.Section;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseMaterialRepository extends JpaRepository<CourseMaterial, Long> {
    List<CourseMaterial> findBySection(Section section);
    Optional<CourseMaterial> findByMaterialId(String materialId);
}
