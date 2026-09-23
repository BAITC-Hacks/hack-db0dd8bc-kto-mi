package com.qadam.repository;

import com.qadam.model.Task;
import com.qadam.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    /** Tasks with the given status, best rated first; the id breaks ties. */
    List<Task> findAllByStatusOrderByScoreDescIdAsc(TaskStatus status);
}
