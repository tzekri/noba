package io.noba.push;

import io.noba.domain.PlatformKey;
import io.noba.repo.PlatformKeyRepository;
import jakarta.annotation.PostConstruct;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * Clés VAPID (identification du serveur auprès des services push).
 * Générées au premier démarrage puis conservées en base : aucune configuration à faire.
 * Si elles changeaient, les abonnements existants deviendraient invalides.
 */
@Component
public class VapidKeys {

	private static final Logger log = LoggerFactory.getLogger(VapidKeys.class);
	private static final String NAME = "vapid";

	private final PlatformKeyRepository repository;
	private ECPublicKey publicKey;
	private ECPrivateKey privateKey;
	private String publicKeyBase64Url;

	public VapidKeys(PlatformKeyRepository repository) {
		this.repository = repository;
	}

	@PostConstruct
	void load() throws GeneralSecurityException {
		PlatformKey stored = repository.findById(NAME).orElseGet(this::generate);
		KeyFactory factory = KeyFactory.getInstance("EC");
		publicKey = (ECPublicKey) factory.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(stored.getPublicKey())));
		privateKey = (ECPrivateKey) factory.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(stored.getPrivateKey())));
		publicKeyBase64Url = Base64.getUrlEncoder().withoutPadding().encodeToString(WebPushCrypto.toUncompressed(publicKey));
	}

	private PlatformKey generate() {
		try {
			KeyPair pair = WebPushCrypto.newKeyPair();
			PlatformKey key = new PlatformKey(NAME,
					Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()),
					Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded()));
			log.info("Clés VAPID (Web Push) générées");
			return repository.saveAndFlush(key);
		} catch (DataIntegrityViolationException createdByAnotherInstance) {
			return repository.findById(NAME).orElseThrow();
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException(e);
		}
	}

	public ECPublicKey publicKey() { return publicKey; }
	public ECPrivateKey privateKey() { return privateKey; }

	/** Clé publique à transmettre au navigateur (applicationServerKey). */
	public String publicKeyBase64Url() { return publicKeyBase64Url; }
}
