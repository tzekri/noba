package io.noba.config;

import io.noba.domain.Branch;
import io.noba.domain.Counter;
import io.noba.domain.DailySequence;
import io.noba.domain.Organization;
import io.noba.domain.QueueService;
import io.noba.domain.Role;
import io.noba.domain.StaffUser;
import io.noba.domain.Ticket;
import io.noba.domain.TicketStatus;
import io.noba.repo.BranchRepository;
import io.noba.repo.CounterRepository;
import io.noba.repo.DailySequenceRepository;
import io.noba.repo.OrganizationRepository;
import io.noba.repo.QueueServiceRepository;
import io.noba.repo.StaffUserRepository;
import io.noba.repo.TicketRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Au démarrage : crée le super-admin s'il n'existe pas, puis (si app.seed-demo) une organisation
 * de démonstration fictive avec un historique de la journée, pour tester tous les écrans.
 */
@Component
public class DataSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
	private static final String DEMO_PASSWORD = "demo1234";

	private final OrganizationRepository organizations;
	private final BranchRepository branches;
	private final QueueServiceRepository services;
	private final CounterRepository counters;
	private final StaffUserRepository staff;
	private final TicketRepository tickets;
	private final DailySequenceRepository sequences;
	private final PasswordEncoder passwordEncoder;
	private final Clock clock;

	@Value("${app.super-admin.email}")
	private String superEmail;
	@Value("${app.super-admin.password}")
	private String superPassword;
	@Value("${app.seed-demo}")
	private boolean seedDemo;

	public DataSeeder(OrganizationRepository organizations, BranchRepository branches, QueueServiceRepository services,
			CounterRepository counters, StaffUserRepository staff, TicketRepository tickets,
			DailySequenceRepository sequences, PasswordEncoder passwordEncoder, Clock clock) {
		this.organizations = organizations;
		this.branches = branches;
		this.services = services;
		this.counters = counters;
		this.staff = staff;
		this.tickets = tickets;
		this.sequences = sequences;
		this.passwordEncoder = passwordEncoder;
		this.clock = clock;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (!staff.existsByEmailIgnoreCase(superEmail)) {
			staff.save(new StaffUser(null, null, superEmail, passwordEncoder.encode(superPassword), "Opérateur Noba",
					Role.SUPER_ADMIN));
			log.info("Super-admin créé : {}", superEmail);
		}
		if (seedDemo && organizations.count() == 0) {
			seedDemo();
		}
	}

	private void seedDemo() {
		Organization org = organizations.save(new Organization("Banque Démo", "banque-demo"));
		Branch branch = branches.save(new Branch(org, "Agence Centre", "demo", "12 boulevard Mohammed V, Casablanca"));

		QueueService withdraw = service(branch, "Retrait / Dépôt", "A", "Opérations de caisse", 3, 1);
		QueueService account = service(branch, "Ouverture de compte", "B", "Nouveaux clients, cartes, chéquiers", 10, 2);
		QueueService claims = service(branch, "Réclamations", "C", "Litiges et contestations", 8, 3);

		String hash = passwordEncoder.encode(DEMO_PASSWORD);
		staff.save(new StaffUser(org, null, "admin@demo.noba", hash, "Salma Bennani", Role.ORG_ADMIN));
		StaffUser agent1 = staff.save(new StaffUser(org, branch, "agent@demo.noba", hash, "Youssef Alaoui", Role.AGENT));
		StaffUser agent2 = staff.save(new StaffUser(org, branch, "agent2@demo.noba", hash, "Nadia Tazi", Role.AGENT));

		Counter g1 = counter(branch, "Guichet 1", withdraw, account);
		counter(branch, "Guichet 2", withdraw);
		counter(branch, "Guichet 3", account, claims);

		seedTodayHistory(branch, List.of(withdraw, account, claims), g1, List.of(agent1, agent2));
		log.info("Organisation de démo créée : établissement « demo », comptes admin@demo.noba / agent@demo.noba (mot de passe {})",
				DEMO_PASSWORD);
	}

	/** Quelques tickets déjà traités ce matin (pour les stats et l'estimation) et d'autres en attente. */
	private void seedTodayHistory(Branch branch, List<QueueService> list, Counter counter, List<StaffUser> agents) {
		Random random = new Random(42);
		LocalDate today = LocalDate.now(clock);
		Instant now = Instant.now(clock);
		Instant start = now.minus(Duration.ofMinutes(150));
		int[] numbers = new int[list.size()];

		for (int i = 0; i < 24; i++) {
			int s = random.nextInt(list.size());
			QueueService service = list.get(s);
			Instant created = start.plus(Duration.ofMinutes(i * 5L));
			Ticket t = new Ticket(branch, service, today, ++numbers[s], UUID.randomUUID().toString(), created);
			Instant called = created.plus(Duration.ofMinutes(2 + random.nextInt(12)));
			t.setCalledAt(called);
			t.setCounter(counter);
			t.setAgent(agents.get(i % agents.size()));
			if (random.nextInt(10) == 0) {
				t.setStatus(TicketStatus.NO_SHOW);
				t.setCompletedAt(called.plus(Duration.ofMinutes(2)));
			} else {
				t.setStatus(TicketStatus.DONE);
				t.setStartedAt(called.plus(Duration.ofSeconds(40)));
				t.setCompletedAt(called.plus(Duration.ofMinutes(service.getDefaultServiceMinutes() - 1 + random.nextInt(4))));
				if (random.nextInt(3) > 0) {
					t.setRating(3 + random.nextInt(3));
				}
			}
			tickets.save(t);
		}
		for (int i = 0; i < 7; i++) {
			int s = i % list.size();
			Instant created = now.minus(Duration.ofMinutes(14 - i * 2L));
			tickets.save(new Ticket(branch, list.get(s), today, ++numbers[s], UUID.randomUUID().toString(), created));
		}
		for (int s = 0; s < list.size(); s++) {
			DailySequence seq = new DailySequence(list.get(s).getId(), today);
			for (int n = 0; n < numbers[s]; n++) {
				seq.next();
			}
			sequences.save(seq);
		}
	}

	private QueueService service(Branch branch, String name, String prefix, String description, int minutes, int order) {
		QueueService s = new QueueService(branch, name, prefix);
		s.setDescription(description);
		s.setDefaultServiceMinutes(minutes);
		s.setSortOrder(order);
		return services.save(s);
	}

	private Counter counter(Branch branch, String name, QueueService... served) {
		Counter c = new Counter(branch, name);
		c.getServices().addAll(List.of(served));
		return counters.save(c);
	}
}
