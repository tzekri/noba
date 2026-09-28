package io.noba.realtime;

/**
 * Événement diffusé aux abonnés d'un établissement.
 *
 * @param type        ISSUED, CALLED, RECALLED, STARTED, COMPLETED, NO_SHOW, CANCELLED, TRANSFERRED, COUNTER, CONFIG
 * @param ticketCode  numéro affiché (ex. B-042), null pour les événements de guichet/configuration
 * @param counterName guichet concerné, pour l'annonce « B-042 → Guichet 4 »
 */
public record QueueEvent(String type, String ticketCode, String counterName) {

	public static QueueEvent of(String type) {
		return new QueueEvent(type, null, null);
	}
}
