# Learning Management System (LMS)

This is a web-based Learning Management System that allows administrators, instructors, and students to manage courses, learning materials, quiz, code exercises, and assessment test. The system helps streamline the educational process in a structured and interactive way.

## Technologies Used

- Backend: Java Servlet, JSP
- Frontend: HTML, CSS, JavaScript, Bootstrap
- Database: PostgreSQL
- Other Libraries: JDBC, JSTL

## Features

### SuperAdmin
- Manage all user roles (Admin, Instructor, Student)
- View system-wide statistics and logs
- Grant or revoke administrative privileges
- Perform system-level configuration and maintenance

### Admin
- Manage accounts for students and instructors
- Approve or reject new course requests
- Monitor overall system usage

### Instructor
- Create and manage courses
- Create quiz or code exercise
- Create assessment test
- Upload materials and assignments
- Review and grade student submissions

### Student
- Register and enroll in available courses
- Access course content and download resources
- Practice quiz and code exercise
- Submit assignments and view grades

## Getting Started

### Prerequisites

Ensure you have the following tools and software installed:

- **Docker** (for containerized deployment)
- **JDK 21**
- **Apache Tomcat** (for running the web app without Docker)
- **PostgreSQL** (for the database)
- A Java IDE (NetBeans, IntelliJ IDEA, Eclipse, etc.)

### Docker Setup

To run the project using Docker:

1. Clone the repository:

    ```bash
    git clone https://github.com/sonnamnguyen/Learning-Management-System.git
    ```
2.  Build the JAR file:
   
    Use the following command to build the JAR file:This will generate a JAR file in the target directory, e.g., target/LMS2025-1.0-SNAPSHOT.jar
    ```bash
    cd Learning-Management-System
    mvn clean package 
    ```

4. Build the Docker container:

    ```bash
    cd lms_project
    docker-compose up --build 
    ```

5. Open your browser and go to:

    ```
    http://localhost:9090
    ```



##  **Contact & Contributions**  
**Auhtor:** Son Nam Nguyen - [GitHub Profile](https://github.com/sonnamnguyen) and Friend.   
**Email:** [sonnamsonnam402@gmail.com](mailto:sonnamsonnam402@gmail.com)  
**LinkedIn:** [MyLinkedln](https://www.linkedin.com/in/son-nam-nguyen-0a8094354/)

Let me know if you want to add any database schema details, user credentials for testing, or sample screenshots.
