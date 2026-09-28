package io.noba.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

/** Compteur de numéros par service et par jour (verrouillé en écriture pour éviter les doublons). */
@Entity
@Table(name = "daily_sequences", uniqueConstraints = @UniqueConstraint(columnNames = {"service_id", "seq_day"}))
public class DailySequence {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "service_id", nullable = false)
	private Long serviceId;

	@Column(name = "seq_day", nullable = false)
	private LocalDate day;

	@Column(nullable = false)
	private int lastNumber;

	protected DailySequence() {
	}

	public DailySequence(Long serviceId, LocalDate day) {
		this.serviceId = serviceId;
		this.day = day;
	}

	public int next() {
		return ++lastNumber;
	}

	public int getLastNumber() { return lastNumber; }
}
