package com.qadam.repository;

import com.qadam.model.Proposal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {

    List<Proposal> findAllByTaskIdOrderByCreatedAtAscIdAsc(Long taskId);
}
