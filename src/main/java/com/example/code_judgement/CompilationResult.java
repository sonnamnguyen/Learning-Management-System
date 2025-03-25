package com.example.code_judgement;

import lombok.*;

import java.io.File;

@NoArgsConstructor
@Data
@Getter
@Setter
@AllArgsConstructor
public class CompilationResult {
    private boolean success;   // Trạng thái biên dịch thành công hay không
    private String errorMessage;  // Thông báo lỗi nếu biên dịch thất bại
    private String originalFileName;  // Tên lớp đã biên dịch (nếu thành công)
    private File randomFileName;
    private String extensionFileName;
}
