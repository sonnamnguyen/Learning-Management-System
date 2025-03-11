package com.example.course.material;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/course-materials")
public class CourseMaterialController {
    @Autowired
    private CourseMaterialService courseMaterialService;

    @GetMapping
    public ResponseEntity<?> getAllMaterials(@RequestParam Long sectionId) {
        try {
            return ResponseEntity.ok(courseMaterialService.findAllBySectionId(sectionId));
        }catch (Exception e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteMaterial(@PathVariable Long id) {
        try {
            courseMaterialService.deleteMaterial(id);
            return ResponseEntity.ok("Deleted Material");
        }catch (Exception e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/upload-text")
    public ResponseEntity<?> uploadText(@RequestParam("sectionId") Long sectionId,
                                        @RequestParam("content") String content,
                                        @RequestParam("materialType") String materialType,
                                        @RequestParam("title") String title) {
        try {

            return ResponseEntity.ok(courseMaterialService.uploadText(sectionId, content, materialType, title));
        }catch (Exception e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/upload-lecture")
    public ResponseEntity<?> uploadLecture(@RequestParam("sectionId") Long sectionId,
                                        @RequestParam("video") String videoUrl,
                                        @RequestParam("materialType") String materialType,
                                        @RequestParam("title") String title) {
        try {

            return ResponseEntity.ok(courseMaterialService.uploadLecture(sectionId, videoUrl, materialType, title));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
