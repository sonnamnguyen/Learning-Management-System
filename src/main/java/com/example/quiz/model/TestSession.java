package com.example.quiz.model;


import com.example.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Builder
@Table(name ="test_session")
public class TestSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long assessment_id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private boolean checkPractice;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
    public TestSession(User user) {
        this.user = user;
        this.startTime = LocalDateTime.now();
    }

    @OneToMany(mappedBy = "testSession", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Result> results ;

    @OneToMany(mappedBy = "testSession", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Answer> answers;


}
