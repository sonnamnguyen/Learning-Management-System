package com.example.course.material;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/materials")
public class FileController {
    private final CourseMaterialService courseMaterialService;


    public FileController(CourseMaterialService courseMaterialService) {
        this.courseMaterialService = courseMaterialService;
    }

    // Xử lý upload file
    @PostMapping("/upload")
    public ResponseEntity<?> uploadMaterial(
            @RequestParam("file") MultipartFile file,
            @RequestParam("sectionId") Long sectionId,
            @RequestParam("materialType") String materialType,
            @RequestParam("title") String title) {
        try {
            return ResponseEntity.ok(courseMaterialService.createMaterial(file,sectionId, materialType,title));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping("/upload-image")
    public String uploadImage(@RequestParam("upload") MultipartFile file, @RequestParam("CKEditorFuncNum") String callback,  HttpServletRequest request){
        try {
            String baseUrl = request.getScheme() + "://" + request.getServerName();
            String urlImg = baseUrl + "/material/image/" + courseMaterialService.uploadImage(file);
            System.out.println(urlImg);
            return "<script type='text/javascript'>window.parent.CKEDITOR.tools.callFunction(" +
                    callback +
                    ",'" +
                    urlImg +
                    "','image upload success!')</script>";
        }catch (Exception e){
            //return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
            return e.getMessage();
        }
    }
}
