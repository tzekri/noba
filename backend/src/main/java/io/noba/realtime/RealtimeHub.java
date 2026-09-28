package io.noba.realtime;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Diffusion temps réel par établissement, en Server-Sent Events.
 * <p>
 * Les téléphones des clients, l'écran d'affichage et les postes agents s'abonnent au flux de leur
 * établissement ; à chaque événement ils rechargent l'état dont ils ont besoin. Les événements ne
 * contiennent que des données publiques (numéro de ticket, nom du guichet), jamais de jeton.
 * <p>
 * Limite connue : diffusion en mémoire, valable pour une seule instance du backend.
 * Pour passer à plusieurs instances, relayer les événements via Redis pub/sub.
 */
@Component
public class RealtimeHub {

	private static final Logger log = LoggerFactory.getLogger(RealtimeHub.class);
	private static final long EMITTER_TIMEOUT_MS = 30 * 60 * 1000L;

	private final Map<Long, List<SseEmitter>> emittersByBranch = new ConcurrentHashMap<>();

	public SseEmitter subscribe(Long branchId) {
		SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
		List<SseEmitter> list = emittersByBranch.computeIfAbsent(branchId, id -> new CopyOnWriteArrayList<>());
		list.add(emitter);
		Runnable remove = () -> list.remove(emitter);
		emitter.onCompletion(remove);
		emitter.onTimeout(remove);
		emitter.onError(e -> remove.run());
		try {
			emitter.send(SseEmitter.event().name("hello").data("ok"));
		} catch (IOException e) {
			remove.run();
		}
		return emitter;
	}

	/** Diffuse l'événement une fois la transaction validée, pour que les clients relisent des données à jour. */
	public void publishAfterCommit(Long branchId, QueueEvent event) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					publish(branchId, event);
				}
			});
		} else {
			publish(branchId, event);
		}
	}

	public void publish(Long branchId, QueueEvent event) {
		send(branchId, SseEmitter.event().name("queue").data(event));
	}

	/** Maintient les connexions ouvertes à travers les proxys et purge les abonnés déconnectés. */
	@Scheduled(fixedRate = 25_000)
	void heartbeat() {
		emittersByBranch.keySet().forEach(id -> send(id, SseEmitter.event().comment("ping")));
	}

	public int subscriberCount() {
		return emittersByBranch.values().stream().mapToInt(List::size).sum();
	}

	private void send(Long branchId, SseEmitter.SseEventBuilder event) {
		List<SseEmitter> list = emittersByBranch.get(branchId);
		if (list == null) {
			return;
		}
		for (SseEmitter emitter : list) {
			try {
				emitter.send(event);
			} catch (IOException | IllegalStateException e) {
				list.remove(emitter);
				log.debug("Abonné SSE déconnecté (établissement {})", branchId);
			}
		}
	}
}
