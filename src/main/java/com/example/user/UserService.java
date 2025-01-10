package com.example.user;

import com.example.role.Role;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    // Method to fetch all users
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Method to fetch a user by ID
    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    // Method to create a new user
    public void createUser(User user) {
        // Ensure user has a valid username before saving
        if (user.getUsername() == null || user.getUsername().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        userRepository.save(user);
    }

    // Method to update an existing user
    public User updateUser(Long id, User user) {
        Optional<User> existingUser = userRepository.findById(id);
        if (existingUser.isPresent()) {
            User updatedUser = existingUser.get();
            updatedUser.setUsername(user.getUsername());
            updatedUser.setPassword(user.getPassword());
            updatedUser.setFirstName(user.getFirstName());
            updatedUser.setLastName(user.getLastName());
            updatedUser.setEmail(user.getEmail());
            updatedUser.setIs2faEnabled(user.getIs2faEnabled());
            updatedUser.setIsLocked(user.getIsLocked());
            updatedUser.setRoles(user.getRoles());
            return userRepository.save(updatedUser);
        }
        return null;
    }

    // Method to delete a user by ID
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    // Method to fetch paginated users
    public Page<User> getUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return userRepository.findAll(pageable);
    }

    // Method to search users by username
    public Page<User> searchUsers(String searchQuery, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return userRepository.findByUsernameContainingIgnoreCase(searchQuery, pageable);
    }

    // Method to import users from an Excel file
    public List<User> importExcel(MultipartFile file) {
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int rowCount = sheet.getPhysicalNumberOfRows();
            List<User> users = new ArrayList<>();

            for (int i = 1; i < rowCount; i++) { // Start from 1 to skip the header row
                Row row = sheet.getRow(i);
                if (row != null) {
                    // Assume user data is in columns
                    String username = row.getCell(0).getStringCellValue().trim();
                    String password = row.getCell(1).getStringCellValue().trim();
                    String firstName = row.getCell(2).getStringCellValue().trim();
                    String lastName = row.getCell(3).getStringCellValue().trim();
                    String email = row.getCell(4).getStringCellValue().trim();

                    // Create a new User object and add it to the list
                    User user = new User();
                    user.setUsername(username);
                    user.setPassword(password);
                    user.setFirstName(firstName);
                    user.setLastName(lastName);
                    user.setEmail(email);
                    users.add(user);
                }
            }
            // Save users to the database
            return userRepository.saveAll(users);
        } catch (IOException e) {
            throw new RuntimeException("Error importing users from Excel", e);
        }
    }

    // Method to export users to an Excel file
    public ByteArrayInputStream exportUsersToExcel(List<User> users) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Users");

        // Create the header row
        String[] headers = {"User ID", "Username", "First Name", "Last Name", "Email"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        // Populate data rows
        int rowNum = 1;
        for (User user : users) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(user.getId());
            row.createCell(1).setCellValue(user.getUsername());
            row.createCell(2).setCellValue(user.getFirstName());
            row.createCell(3).setCellValue(user.getLastName());
            row.createCell(4).setCellValue(user.getEmail());
        }

        // Write the workbook to a byte array output stream
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            workbook.write(out);
            workbook.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    // Method to check if a username already exists
    public boolean isUsernameExists(String username) {
        return userRepository.findByUsername(username).isPresent();
    }

    // Method to check if an email already exists
    public boolean isEmailExists(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    public void saveAll(List<User> users) {
        userRepository.saveAll(users);  // Save all users at once
    }
}
