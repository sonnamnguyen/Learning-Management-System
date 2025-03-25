package com.example;

import nu.pattern.OpenCV;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

/**
 * Hello world!
 *
 */
@SpringBootApplication()
public class App
{
    public static void main( String[] args )
    {
        // This will start the Spring Boot application
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SpringApplication.run(App.class, args);
        OpenCV.loadLocally();
    }
}
