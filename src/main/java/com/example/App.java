package com.example;

import nu.pattern.OpenCV;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
/**
 * Hello world!
 *
 */
@SpringBootApplication()
@EnableScheduling
public class App
{
    public static void main( String[] args )
    {
        // This will start the Spring Boot application
        SpringApplication.run(App.class, args);
        OpenCV.loadLocally();
    }
}
