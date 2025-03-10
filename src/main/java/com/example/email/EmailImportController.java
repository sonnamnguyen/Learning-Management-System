package com.example.email;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/email")
@RequiredArgsConstructor
public class EmailImportController {

    private final EmailService emailService;


    @GetMapping("/upload-form")
    public String showUploadForm() {
        return "uploadForm"; // Trả về trang HTML chứa form upload
    }
        // Cách lấy email từ excel sử dụng chỉ cột A và điền các mail cần đang kí
    private List<String> extractEmailsFromExcel(InputStream inputStream) throws Exception {
        List<String> emails = new ArrayList<>();
        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet sheet = workbook.getSheetAt(0);

        for (Row row : sheet) {
            Cell cell = row.getCell(0); // Lấy cột đầu tiên (chứa email)
            if (cell != null && cell.getCellType() == CellType.STRING) {
                String email = cell.getStringCellValue().trim();
                if (email.contains("@")) {
                    emails.add(email);
                }
            }
        }
        workbook.close();
        return emails;
    }
}
