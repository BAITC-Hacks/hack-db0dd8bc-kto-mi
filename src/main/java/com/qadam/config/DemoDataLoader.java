package com.qadam.config;

import com.qadam.model.Proposal;
import com.qadam.model.Task;
import com.qadam.model.Team;
import com.qadam.repository.ProposalRepository;
import com.qadam.repository.TaskRepository;
import com.qadam.repository.TeamRepository;
import com.qadam.service.DemoData;
import com.qadam.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Fills an empty database with {@link DemoData}: tasks (rated by the regular {@link TaskService}),
 * teams and proposals. No LLM calls. Disabled with {@code qadam.demo-data.enabled=false}.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "qadam.demo-data.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class DemoDataLoader implements ApplicationRunner {

    private final TaskRepository taskRepository;
    private final TeamRepository teamRepository;
    private final ProposalRepository proposalRepository;
    private final TaskService taskService;

    @Override
    public void run(ApplicationArguments args) {
        if (taskRepository.count() > 0 || teamRepository.count() > 0) {
            log.info("Database is not empty, demo data skipped");
            return;
        }
        List<Task> tasks = DemoData.TASKS.stream()
                .map(demo -> taskService.save(demo.industry(), demo.draftText(), demo.card(), demo.status()))
                .toList();
        List<Team> teams = DemoData.TEAMS.stream()
                .map(demo -> {
                    Team team = new Team(demo.name(), demo.interests(), demo.skills(), demo.technologies());
                    team.setPoints(demo.points());
                    return teamRepository.save(team);
                })
                .toList();
        for (DemoData.DemoProposal demo : DemoData.PROPOSALS) {
            Proposal proposal = new Proposal();
            proposal.setTaskId(tasks.get(demo.task()).getId());
            proposal.setTeamId(teams.get(demo.team()).getId());
            proposal.setIdea(demo.idea());
            proposal.setPlan(demo.plan());
            proposal.setDuration(demo.duration());
            proposal.setPrototypeUrl(demo.prototypeUrl());
            proposal.setStatus(demo.status());
            proposal.setConfirmedMilestones(demo.confirmedMilestones());
            proposalRepository.save(proposal);
        }
        log.info("Demo data created: {} tasks, {} teams, {} proposals",
                tasks.size(), teams.size(), DemoData.PROPOSALS.size());
    }
}
