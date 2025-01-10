package com.example.tag;

import com.example.topic.Topic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Integer> {
    Page<Tag> findAll(Pageable pageable);
    Page<Tag> findByTagNameContainingIgnoreCase(String tagName, Pageable pageable);
    Optional<Tag> findByTagName(String tagName);
    boolean existsByTagNameAndTopic(String tagName, Topic topic);
}



