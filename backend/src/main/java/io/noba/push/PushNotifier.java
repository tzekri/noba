package io.noba.push;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.noba.domain.PushSubscription;
import io.noba.domain.Ticket;
import io.noba.repo.PushSubscriptionRepository;
import io.noba.repo.TicketRepository;
import io.noba.web.ApiException;
import io.noba.web.dto.PublicDtos.PushSubscribeRequest;
import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Notifications Web Push : le client est prévenu même si la page de son ticket est fermée.
 * <ul>
 *   <li>« Bientôt votre tour » : une fois, quand il reste {@value #SOON_THRESHOLD} personnes ou moins devant ;</li>
 *   <li>« C'est votre tour » : à l'appel ;</li>
 *   <li>« Rappel » : quand l'agent rappelle le ticket.</li>
 * </ul>
 * Les messages sont préparés dans la transaction métier et envoyés après validation, en arrière-plan.
 */
@Service
public class PushNotifier {

	private static final Logger log = LoggerFactory.getLogger(PushNotifier.class);
	static final int SOON_THRESHOLD = 2;
	private static final int MAX_SUBSCRIPTIONS_PER_TICKET = 5;

	/**
	 * Services push des navigateurs. Le serveur envoie une requête à l'URL fournie par le navigateur :
	 * on la restreint à ces hôtes pour qu'elle ne puisse pas viser un serveur interne (SSRF).
	 */
	private static final List<String> ALLOWED_PUSH_HOSTS = List.of(
			"fcm.googleapis.com", "android.googleapis.com", ".push.services.mozilla.com",
			".notify.windows.com", ".push.apple.com");

	private final PushSubscriptionRepository subscriptions;
	private final TicketRepository tickets;
	private final WebPushSender sender;
	private final ObjectMapper json;
	private final TransactionTemplate tx;
	private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

	public PushNotifier(PushSubscriptionRepository subscriptions, TicketRepository tickets, WebPushSender sender,
			ObjectMapper json, PlatformTransactionManager transactionManager) {
		this.subscriptions = subscriptions;
		this.tickets = tickets;
		this.sender = sender;
		this.json = json;
		this.tx = new TransactionTemplate(transactionManager);
	}

	// ---- Abonnement (appelé par la page du ticket) ----

	@Transactional
	public void subscribe(String token, PushSubscribeRequest request) {
		Ticket ticket = tickets.findByPublicToken(token).orElseThrow(() -> ApiException.notFound("Ticket introuvable."));
		if (!ticket.getStatus().isActive()) {
			return;
		}
		checkEndpoint(request.endpoint());
		checkKeys(request.keys().p256dh(), request.keys().auth());
		subscriptions.findByTicketIdAndEndpoint(ticket.getId(), request.endpoint()).ifPresentOrElse(existing -> {
			existing.setP256dh(request.keys().p256dh());
			existing.setAuth(request.keys().auth());
		}, () -> {
			if (subscriptions.countByTicketId(ticket.getId()) >= MAX_SUBSCRIPTIONS_PER_TICKET) {
				throw ApiException.conflict("Trop d'appareils abonnés à ce ticket.");
			}
			subscriptions.save(new PushSubscription(ticket, request.endpoint(), request.keys().p256dh(), request.keys().auth()));
		});
	}

	@Transactional
	public void unsubscribe(String token, String endpoint) {
		tickets.findByPublicToken(token).ifPresent(t -> subscriptions.deleteByTicketAndEndpoint(t.getId(), endpoint));
	}

	// ---- Événements de file (appelés dans la transaction de l'action du guichet) ----

	public void ticketCalled(Ticket ticket) {
		String counter = ticket.getCounter() == null ? "guichet" : ticket.getCounter().getName();
		dispatch(List.of(message(ticket, "C'est votre tour ! " + ticket.getCode(), "Présentez-vous au " + counter + ".", true)));
	}

	public void ticketRecalled(Ticket ticket) {
		String counter = ticket.getCounter() == null ? "guichet" : ticket.getCounter().getName();
		dispatch(List.of(message(ticket, "Rappel : ticket " + ticket.getCode(), "On vous attend au " + counter + ".", true)));
	}

	/** La file d'un service a avancé : prévient (une fois) les tickets arrivés en tête de file. */
	public void queueAdvanced(Long serviceId, LocalDate day) {
		List<Ticket> head = tickets.findHeadOfQueue(serviceId, day, PageRequest.of(0, SOON_THRESHOLD + 1));
		List<Message> messages = new ArrayList<>();
		for (int ahead = 0; ahead < head.size(); ahead++) {
			Ticket t = head.get(ahead);
			if (t.isSoonNotified()) {
				continue;
			}
			t.setSoonNotified(true);
			String body = ahead == 0
					? "Vous êtes le prochain (ticket " + t.getCode() + "). Rapprochez-vous de l'accueil."
					: "Plus que " + ahead + " personne" + (ahead > 1 ? "s" : "") + " avant vous (ticket " + t.getCode() + ").";
			messages.add(message(t, "Bientôt votre tour", body, false));
		}
		dispatch(messages);
	}

	// ---- Envoi ----

	private Message message(Ticket ticket, String title, String body, boolean urgent) {
		return new Message(ticket.getId(), payload(ticket, title, body, urgent));
	}

	private String payload(Ticket ticket, String title, String body, boolean urgent) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("title", title);
		data.put("body", body);
		data.put("url", "/t/" + ticket.getPublicToken());
		// Même étiquette que les alertes affichées par la page : pas de doublon si elle est ouverte.
		data.put("tag", "noba-" + ticket.getCode());
		data.put("urgent", urgent);
		try {
			return json.writeValueAsString(data);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
	}

	private void dispatch(List<Message> messages) {
		if (messages.isEmpty()) {
			return;
		}
		Map<Long, String> payloadByTicket = new LinkedHashMap<>();
		messages.forEach(m -> payloadByTicket.put(m.ticketId(), m.payload()));
		List<Outgoing> outgoing = subscriptions.findByTicketIdIn(payloadByTicket.keySet()).stream()
				.map(s -> new Outgoing(s.getEndpoint(), s.getP256dh(), s.getAuth(), payloadByTicket.get(s.getTicket().getId())))
				.toList();
		if (outgoing.isEmpty()) {
			return;
		}
		Runnable send = () -> outgoing.forEach(o -> executor.submit(() -> deliver(o)));
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					send.run();
				}
			});
		} else {
			send.run();
		}
	}

	private void deliver(Outgoing o) {
		try {
			int status = sender.send(o.endpoint(), o.p256dh(), o.auth(), o.payload());
			log.debug("Web Push → {} : {}", URI.create(o.endpoint()).getHost(), status);
			if (status == 404 || status == 410) {
				// Abonnement révoqué ou expiré côté navigateur : on l'oublie.
				tx.executeWithoutResult(s -> subscriptions.deleteByEndpoint(o.endpoint()));
			} else if (status >= 400) {
				log.warn("Service push {} : réponse {}", URI.create(o.endpoint()).getHost(), status);
			}
		} catch (Exception e) {
			log.warn("Échec d'envoi Web Push vers {} : {}", URI.create(o.endpoint()).getHost(), e.toString());
		}
	}

	private static void checkEndpoint(String endpoint) {
		URI uri;
		try {
			uri = URI.create(endpoint);
		} catch (IllegalArgumentException e) {
			throw ApiException.badRequest("Abonnement push invalide.");
		}
		String host = uri.getHost();
		boolean allowed = "https".equals(uri.getScheme()) && host != null && uri.getPort() == -1
				&& ALLOWED_PUSH_HOSTS.stream().anyMatch(h -> h.startsWith(".") ? host.endsWith(h) : host.equals(h));
		if (!allowed) {
			throw ApiException.badRequest("Service de notification non reconnu.");
		}
	}

	private static void checkKeys(String p256dh, String auth) {
		try {
			WebPushCrypto.fromUncompressed(Base64.getUrlDecoder().decode(p256dh));
			if (Base64.getUrlDecoder().decode(auth).length != 16) {
				throw new GeneralSecurityException();
			}
		} catch (IllegalArgumentException | GeneralSecurityException e) {
			throw ApiException.badRequest("Abonnement push invalide.");
		}
	}

	@PreDestroy
	void shutdown() {
		executor.shutdown();
	}

	private record Message(Long ticketId, String payload) {
	}

	private record Outgoing(String endpoint, String p256dh, String auth, String payload) {
	}
}
