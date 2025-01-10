package com.example.department;

public class DepartmentAlreadyExistsException extends Throwable {
    public DepartmentAlreadyExistsException(String message) {
        super(message);
    }
}
