package com.example.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Controller
@RequestMapping("/tools")
public class ToolController {
    private final ToolService toolService;

    public ToolController(ToolService toolService) {
        this.toolService = toolService;
    }

    @GetMapping()
    public String tools() {
        return "tools/convert_excel";
    }

    @GetMapping("/convert_txt")
    public String convertTxt() {
        return "tools/convert_txt";
    }

    @GetMapping("/generate_exam")
    public String generateExam() {
        return "tools/generate_exam";
    }

    @PostMapping("/upload_excel")
    public String handleFileUpload(
            @RequestParam("excelFile") List<MultipartFile> files,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (files == null || files.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "No files uploaded.");
            return "redirect:/tools/covert_excel";
        }

        try {
            Map<String, Object> rawJsonData = toolService.convertExcelFilesToJson(files);

            ObjectMapper objectMapper = new ObjectMapper();
            String jsonString = objectMapper.writeValueAsString(rawJsonData);

            model.addAttribute("jsonData", jsonString);

            return "tools/excel_to_json_view";
        } catch (IOException e) {
            model.addAttribute("error", "Error occurred while processing the file: " + e.getMessage());
            return "redirect:/tools/covert_excel";
        }
    }

    @PostMapping("/upload_txt")
    public String handleTxtContentUpload(
            @RequestParam Map<String, String> txtFile,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (txtFile == null || txtFile.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "No content provided.");
            return "redirect:/tools/convert_txt";
        }

        // Process each file content
        Map<String, String> fileContents = new HashMap<>();
        AtomicInteger counter = new AtomicInteger(1); // Counter for unnamed files

        txtFile.forEach((key, value) -> {
            if (!value.trim().isEmpty()) {
                // Extract the file name from the content
                String fileName = toolService.extractFileName(value);

                if (fileName == null) {
                    // If no valid CODE is found, generate a default name
                    fileName = String.format("file_%02d", counter.getAndIncrement());
                }

                fileContents.put(fileName, value); // Use the determined file name
            }
        });

        if (fileContents.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "All fields are empty.");
            return "redirect:/tools/convert_txt";
        }

        model.addAttribute("txtData", fileContents);
        return "tools/txt_to_json_view";
    }
}
