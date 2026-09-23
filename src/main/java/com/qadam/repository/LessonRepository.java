package com.qadam.repository;

import com.qadam.model.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    /** All lessons, newest first; the id breaks ties between lessons created in the same instant. */
    List<Lesson> findAllByOrderByCreatedAtDescIdDesc();
}
