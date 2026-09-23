package com.qadam.service;

import com.qadam.dto.CreateProposalRequest;
import com.qadam.dto.ProposalResponse;
import com.qadam.model.Proposal;
import com.qadam.model.ProposalStatus;
import com.qadam.model.Task;
import com.qadam.model.TaskStatus;
import com.qadam.model.Team;
import com.qadam.repository.ProposalRepository;
import com.qadam.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Team proposals for published tasks. Accepting, rejecting and confirming milestones are manual
 * actions of the business; any number of proposals per task may be accepted, including none.
 */
@Service
@RequiredArgsConstructor
public class ProposalService {

    static final int MILESTONE_POINTS = 10;

    private final ProposalRepository proposalRepository;
    private final TeamRepository teamRepository;
    private final TaskService taskService;

    @Transactional
    public ProposalResponse create(long taskId, CreateProposalRequest request) {
        Task task = taskService.find(taskId);
        if (task.getStatus() != TaskStatus.PUBLISHED) {
            throw new InvalidStateException("Task " + taskId + " is not published",
                    "Откликнуться можно только на опубликованную задачу");
        }
        Team team = findTeam(request.teamId());

        Proposal proposal = new Proposal();
        proposal.setTaskId(task.getId());
        proposal.setTeamId(team.getId());
        proposal.setIdea(request.idea().strip());
        proposal.setPlan(request.plan().strip());
        proposal.setDuration(request.duration().strip());
        proposal.setPrototypeUrl(request.prototypeUrl() == null || request.prototypeUrl().isBlank()
                ? null
                : request.prototypeUrl().strip());
        return ProposalResponse.of(proposalRepository.saveAndFlush(proposal), team);
    }

    @Transactional(readOnly = true)
    public List<ProposalResponse> listForTask(long taskId) {
        taskService.find(taskId);
        List<Proposal> proposals = proposalRepository.findAllByTaskIdOrderByCreatedAtAscIdAsc(taskId);
        Map<Long, Team> teams = teamRepository.findAllById(proposals.stream().map(Proposal::getTeamId).toList())
                .stream()
                .collect(Collectors.toMap(Team::getId, Function.identity()));
        return proposals.stream()
                .map(proposal -> ProposalResponse.of(proposal, teams.get(proposal.getTeamId())))
                .toList();
    }

    @Transactional
    public ProposalResponse accept(long id) {
        return decide(id, ProposalStatus.ACCEPTED);
    }

    @Transactional
    public ProposalResponse reject(long id) {
        return decide(id, ProposalStatus.REJECTED);
    }

    /**
     * The business confirms a milestone of an accepted proposal; the team gets {@value #MILESTONE_POINTS} points.
     */
    @Transactional
    public ProposalResponse confirmMilestone(long id) {
        Proposal proposal = findProposal(id);
        if (proposal.getStatus() != ProposalStatus.ACCEPTED) {
            throw new InvalidStateException("Proposal " + id + " is not accepted",
                    "Подтвердить этап можно только для принятого отклика");
        }
        Team team = findTeam(proposal.getTeamId());
        team.setPoints(team.getPoints() + MILESTONE_POINTS);
        proposal.setConfirmedMilestones(proposal.getConfirmedMilestones() + 1);
        return ProposalResponse.of(proposal, team);
    }

    private ProposalResponse decide(long id, ProposalStatus decision) {
        Proposal proposal = findProposal(id);
        if (proposal.getStatus() != ProposalStatus.PENDING) {
            throw new InvalidStateException("Proposal " + id + " is already " + proposal.getStatus(),
                    "Отклик уже рассмотрен");
        }
        proposal.setStatus(decision);
        return ProposalResponse.of(proposal, findTeam(proposal.getTeamId()));
    }

    private Proposal findProposal(long id) {
        return proposalRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(NotFoundException.Resource.PROPOSAL, id));
    }

    private Team findTeam(long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(NotFoundException.Resource.TEAM, id));
    }
}
