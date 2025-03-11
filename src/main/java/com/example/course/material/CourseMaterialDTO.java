package com.example.course.material;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseMaterialDTO {
    private Long id;
    private String title;
    private String url;
    private String materialId;
    private String content;
    private String typ;

    public static CourseMaterialDTO toDTO(CourseMaterial courseMaterial) {
        return CourseMaterialDTO.builder()
                .id(courseMaterial.getId())
                .title(courseMaterial.getTitle())
                .url(courseMaterial.getMaterialUrl())
                .materialId(courseMaterial.getMaterialId())
                .typ(courseMaterial.getCourseMaterialType().name())
                .content(courseMaterial.getContent())
                .build();
    }
}
