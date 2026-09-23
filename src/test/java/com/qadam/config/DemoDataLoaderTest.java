package com.qadam.config;

import com.qadam.model.ProposalStatus;
import com.qadam.model.RatingLevel;
import com.qadam.model.Task;
import com.qadam.model.TaskStatus;
import com.qadam.repository.ProposalRepository;
import com.qadam.repository.TaskRepository;
import com.qadam.repository.TeamRepository;
import com.qadam.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The loader itself is disabled in tests ({@code qadam.demo-data.enabled=false}), so it is run by hand here.
 */
@SpringBootTest
class DemoDataLoaderTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private ProposalRepository proposalRepository;

    @Autowired
    private TaskService taskService;

    private DemoDataLoader loader;

    @BeforeEach
    void setUp() {
        proposalRepository.deleteAll();
        taskRepository.deleteAll();
        teamRepository.deleteAll();
        loader = new DemoDataLoader(taskRepository, teamRepository, proposalRepository, taskService);
    }

    @Test
    void createsDemoTasksTeamsAndProposals() {
        loader.run(null);

        List<Task> published = taskRepository.findAllByStatusOrderByScoreDescIdAsc(TaskStatus.PUBLISHED);
        List<Task> drafts = taskRepository.findAllByStatusOrderByScoreDescIdAsc(TaskStatus.DRAFT);
        assertThat(published).hasSize(5);
        assertThat(published).extracting(Task::getLevel)
                .containsExactlyInAnyOrder(RatingLevel.PRIORITY, RatingLevel.READY, RatingLevel.READY,
                        RatingLevel.WORKING, RatingLevel.DRAFT);
        assertThat(drafts).hasSize(5);
        assertThat(drafts).extracting(Task::getScore).doesNotHaveDuplicates();
        assertThat(teamRepository.count()).isEqualTo(5);
        assertThat(proposalRepository.findAll())
                .hasSize(5)
                .allMatch(proposal -> published.stream().anyMatch(task -> task.getId().equals(proposal.getTaskId())))
                .extracting(proposal -> proposal.getStatus())
                .contains(ProposalStatus.PENDING, ProposalStatus.ACCEPTED, ProposalStatus.REJECTED);
    }

    @Test
    void skipsNonEmptyDatabase() {
        loader.run(null);
        loader.run(null);

        assertThat(taskRepository.count()).isEqualTo(10);
        assertThat(teamRepository.count()).isEqualTo(5);
    }
}
