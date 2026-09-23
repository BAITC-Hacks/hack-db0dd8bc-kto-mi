package com.qadam.dto;

import com.qadam.model.Proposal;
import com.qadam.model.ProposalStatus;
import com.qadam.model.Team;

import java.time.Instant;

/**
 * @param teamPoints current points of the team
 */
public record ProposalResponse(
        Long id,
        Long taskId,
        Long teamId,
        String teamName,
        String idea,
        String plan,
        String duration,
        String prototypeUrl,
        ProposalStatus status,
        int confirmedMilestones,
        int teamPoints,
        Instant createdAt
) {

    public static ProposalResponse of(Proposal proposal, Team team) {
        return new ProposalResponse(proposal.getId(), proposal.getTaskId(), proposal.getTeamId(), team.getName(),
                proposal.getIdea(), proposal.getPlan(), proposal.getDuration(), proposal.getPrototypeUrl(),
                proposal.getStatus(), proposal.getConfirmedMilestones(), team.getPoints(), proposal.getCreatedAt());
    }
}
