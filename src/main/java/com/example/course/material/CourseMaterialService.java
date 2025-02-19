package com.example.course.material;

import com.example.course.section.Section;
import com.example.course.section.SectionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.compress.utils.FileNameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CourseMaterialService {
    private final CourseMaterialRepository courseMaterialRepository;
    private final SectionRepository sectionRepository;

    //Thư mục lưu trữ file
    @Value("${upload.dir}")
    private String uploadDir;

    public List<CourseMaterial> findAll() {
        return courseMaterialRepository.findAll();
    }

    public List<CourseMaterial> findAllBySectionId(Long sectionId) {
        Section section = sectionRepository.findById(sectionId).orElseThrow(EntityNotFoundException::new);
        return courseMaterialRepository.findBySection(section);
    }

    public CourseMaterialDTO createMaterial(MultipartFile file, Long sectionId, String materialType, String title) throws IOException {
        Section section = sectionRepository.findById(sectionId).orElseThrow(EntityNotFoundException::new);
        // 1. Kiểm tra file
        if (file.isEmpty()) {
            throw new RuntimeException("No file selected for upload.");
        }

        if (!"application/pdf".equalsIgnoreCase(file.getContentType())) {
            throw new RuntimeException("Only PDF files are allowed.");
        }

        // 2. Tạo thư mục upload nếu chưa tồn tại
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 3. Đảm bảo tên file không bị trùng
        String originalFileName = Objects.requireNonNull(file.getOriginalFilename());
        String fileID = String.valueOf(System.currentTimeMillis());
        String url = "http://localhost/material/" + fileID + ".pdf";
        //String url = "/material/" + fileID;
        System.out.println(url);

        // 4. Lưu file
        Path filePath = uploadPath.resolve(fileID+".pdf");
        file.transferTo(filePath.toFile());
        CourseMaterial courseMaterial = CourseMaterial.builder()
                .materialId(fileID)
                .section(section)
                .courseMaterialType(CourseMaterial.CourseMaterialType.DOCUMENT)
                .materialType(CourseMaterial.MaterialType.valueOf(materialType))
                .materialName(originalFileName)
                .materialUrl(url)
                .title(title)
                .build();
        return CourseMaterialDTO.toDTO(courseMaterialRepository.save(courseMaterial));

    }


    public void deleteMaterial(Long id) {
        // find material
        CourseMaterial material = courseMaterialRepository.findById(id).orElseThrow(EntityNotFoundException::new);

//        if(uploadDir != null) {
//            Path path = Paths.get(uploadDir);
//            try {
//                Files.deleteIfExists(path);
//
//            }catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//        }
        courseMaterialRepository.deleteById(id);
    }

    public Map<String, Object> uploadText(Long sectionId, String content, String materialType, String title) {
        // find section
        Section section = sectionRepository.findById(sectionId).orElseThrow(EntityNotFoundException::new);
        CourseMaterial courseMaterial = CourseMaterial.builder()
                .section(section)
                .courseMaterialType(CourseMaterial.CourseMaterialType.TEXT)
                .materialType(CourseMaterial.MaterialType.valueOf(materialType))
                .content(content)
                .materialId(String.valueOf(System.currentTimeMillis()))
                .title(title)
                .build();
        courseMaterialRepository.save(courseMaterial);
        Map<String, Object> response = new HashMap<>();
        response.put("id", courseMaterial.getId());
        response.put("title", courseMaterial.getTitle());
        response.put("content", courseMaterial.getContent());
        response.put("typ", courseMaterial.getCourseMaterialType());
        return response;
    }

    public Map<String, Object> uploadLecture(Long sectionId, String videoUrl, String materialType, String title) {
        // find section
        Section section = sectionRepository.findById(sectionId).orElseThrow(EntityNotFoundException::new);
        CourseMaterial courseMaterial = CourseMaterial.builder()
                .section(section)
                .courseMaterialType(CourseMaterial.CourseMaterialType.VIDEO)
                .materialType(CourseMaterial.MaterialType.valueOf(materialType))
                .materialUrl(videoUrl)
                .materialId(getYouTubeVideoID(videoUrl))
                .title(title)
                .build();
        courseMaterialRepository.save(courseMaterial);
        Map<String, Object> response = new HashMap<>();
        response.put("id", courseMaterial.getId());
        response.put("title", courseMaterial.getTitle());
        response.put("url", courseMaterial.getMaterialUrl());
        response.put("typ", courseMaterial.getCourseMaterialType());
        response.put("materialId", courseMaterial.getMaterialId());
        return response;
    }

    public String getYouTubeVideoID(String url) {
        String pattern = "^(?:https?:\\/\\/)?(?:www\\.)?(?:youtube\\.com\\/.*(?:\\?|&)v=|youtu\\.be\\/|youtube\\.com\\/embed\\/)([a-zA-Z0-9_-]{11})";
        Pattern compiledPattern = Pattern.compile(pattern);
        Matcher matcher = compiledPattern.matcher(url);

        return matcher.find() ? matcher.group(1) : null;
    }

    public String uploadImage(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new RuntimeException("No file selected for upload.");
        }

        // 2. Tạo thư mục upload nếu chưa tồn tại
        Path uploadPath = Paths.get(uploadDir+"/image");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFileName = Objects.requireNonNull(file.getOriginalFilename());
        String srcExtension = FileNameUtils.getExtension(originalFileName);
        // 3. Đảm bảo tên file không bị trùng
        String fileID = String.valueOf(System.currentTimeMillis());
        String desFileName = fileID + "." + srcExtension;
        System.out.println(desFileName);

        // 4. Lưu file
        Path filePath = uploadPath.resolve(desFileName);
        file.transferTo(filePath.toFile());
        return desFileName;
    }
}
