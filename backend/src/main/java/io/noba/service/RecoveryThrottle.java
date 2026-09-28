package io.noba.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Limite les tentatives de récupération de ticket (code à 4 chiffres = 10 000 combinaisons) :
 * au plus {@value #MAX_PER_CLIENT} essais échoués par adresse IP et {@value #MAX_PER_TICKET} par ticket,
 * sur une fenêtre de 15 minutes. Deviner le code d'un ticket précis devient impraticable.
 * <p>
 * En mémoire : valable pour une instance (comme le temps réel) ; Redis en multi-instances.
 */
@Component
public class RecoveryThrottle {

	static final int MAX_PER_CLIENT = 10;
	static final int MAX_PER_TICKET = 5;
	private static final Duration WINDOW = Duration.ofMinutes(15);

	private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();
	private final Clock clock;

	public RecoveryThrottle(Clock clock) {
		this.clock = clock;
	}

	public boolean isBlocked(String clientKey, String ticketKey) {
		return count("ip:" + clientKey) >= MAX_PER_CLIENT || count("t:" + ticketKey) >= MAX_PER_TICKET;
	}

	public void recordFailure(String clientKey, String ticketKey) {
		Instant now = Instant.now(clock);
		for (String key : new String[] {"ip:" + clientKey, "t:" + ticketKey}) {
			Deque<Instant> deque = failures.computeIfAbsent(key, k -> new ArrayDeque<>());
			synchronized (deque) {
				deque.addLast(now);
			}
		}
	}

	private int count(String key) {
		Deque<Instant> deque = failures.get(key);
		if (deque == null) {
			return 0;
		}
		synchronized (deque) {
			prune(deque);
			return deque.size();
		}
	}

	private void prune(Deque<Instant> deque) {
		Instant limit = Instant.now(clock).minus(WINDOW);
		while (!deque.isEmpty() && deque.peekFirst().isBefore(limit)) {
			deque.pollFirst();
		}
	}

	/** Évite que la table grossisse indéfiniment. */
	@Scheduled(fixedRate = 10 * 60 * 1000)
	void cleanup() {
		failures.entrySet().removeIf(e -> {
			synchronized (e.getValue()) {
				prune(e.getValue());
				return e.getValue().isEmpty();
			}
		});
	}
}
