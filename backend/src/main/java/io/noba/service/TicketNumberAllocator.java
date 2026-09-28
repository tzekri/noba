package io.noba.service;

import io.noba.domain.DailySequence;
import io.noba.repo.DailySequenceRepository;
import java.time.LocalDate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Attribue les numéros de ticket sans doublon, même si deux clients scannent au même instant :
 * la ligne du jour est créée si besoin (transaction séparée), puis verrouillée en écriture.
 */
@Component
public class TicketNumberAllocator {

	private final DailySequenceRepository sequences;

	public TicketNumberAllocator(DailySequenceRepository sequences) {
		this.sequences = sequences;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void ensureSequence(Long serviceId, LocalDate day) {
		if (sequences.existsByServiceIdAndDay(serviceId, day)) {
			return;
		}
		try {
			sequences.saveAndFlush(new DailySequence(serviceId, day));
		} catch (DataIntegrityViolationException alreadyCreatedConcurrently) {
			// Un autre appel l'a créée entre-temps : rien à faire.
		}
	}

	/** À appeler dans la transaction qui crée le ticket. */
	@Transactional(propagation = Propagation.MANDATORY)
	public int next(Long serviceId, LocalDate day) {
		return sequences.lock(serviceId, day)
				.orElseThrow(() -> new IllegalStateException("Séquence absente pour le service " + serviceId))
				.next();
	}
}
