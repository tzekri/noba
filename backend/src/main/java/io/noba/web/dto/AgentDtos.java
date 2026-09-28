package io.noba.web.dto;

import io.noba.domain.TicketStatus;
import io.noba.web.dto.PublicDtos.CalledTicket;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

/** Interface guichet. */
public final class AgentDtos {

	private AgentDtos() {
	}

	public record BranchSummary(Long id, String name, String code, boolean open) {
	}

	public record BoardView(BranchSummary branch, List<ServiceBoard> services, List<CounterView> counters,
			List<AgentTicket> waiting, List<CalledTicket> recent) {
	}

	public record ServiceBoard(Long id, String name, String prefix, long waiting, int estimatedWaitMinutes,
			int avgServiceMinutes) {
	}

	public record CounterView(Long id, String name, boolean active, Long agentId, String agentName,
			List<Long> serviceIds, AgentTicket current) {
	}

	public record AgentTicket(Long id, String code, TicketStatus status, Long serviceId, String serviceName,
			Instant queuedAt, Instant calledAt, Instant startedAt, int recallCount) {
	}

	public record TransferRequest(@NotNull Long serviceId) {
	}
}
