package io.noba.push;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Envoi d'un message Web Push chiffré à un service push (FCM, Mozilla, Apple…), authentifié par VAPID. */
@Component
public class WebPushSender {

	/** Un message non délivré après 10 min n'a plus d'intérêt (le client a sans doute déjà été appelé). */
	private static final int TTL_SECONDS = 600;
	private static final Duration JWT_VALIDITY = Duration.ofHours(12);

	private final VapidKeys keys;
	private final String subject;
	private final HttpClient http = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(10))
			.proxy(ProxySelector.getDefault())
			.build();
	private final Map<String, CachedJwt> jwtByAudience = new ConcurrentHashMap<>();

	public WebPushSender(VapidKeys keys, @Value("${app.push.subject}") String subject) {
		this.keys = keys;
		this.subject = subject;
	}

	/** @return le code HTTP du service push (201 = accepté ; 404/410 = abonnement expiré) */
	public int send(String endpoint, String p256dh, String auth, String payloadJson) throws Exception {
		Base64.Decoder b64 = Base64.getUrlDecoder();
		byte[] body = WebPushCrypto.encrypt(payloadJson.getBytes(StandardCharsets.UTF_8), b64.decode(p256dh), b64.decode(auth));
		URI uri = URI.create(endpoint);
		HttpRequest request = HttpRequest.newBuilder(uri)
				.timeout(Duration.ofSeconds(15))
				.header("TTL", String.valueOf(TTL_SECONDS))
				.header("Urgency", "high")
				.header("Content-Encoding", "aes128gcm")
				.header("Content-Type", "application/octet-stream")
				.header("Authorization", "vapid t=" + jwt(uri.getScheme() + "://" + uri.getAuthority()) + ", k=" + keys.publicKeyBase64Url())
				.POST(HttpRequest.BodyPublishers.ofByteArray(body))
				.build();
		return http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
	}

	/** Jeton VAPID (JWT ES256) par service push, réutilisé tant qu'il reste valide plus d'une heure. */
	private String jwt(String audience) throws Exception {
		CachedJwt cached = jwtByAudience.get(audience);
		Instant now = Instant.now();
		if (cached != null && cached.expiresAt().isAfter(now.plus(Duration.ofHours(1)))) {
			return cached.token();
		}
		Instant expiresAt = now.plus(JWT_VALIDITY);
		SignedJWT jwt = new SignedJWT(
				new JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT).build(),
				new JWTClaimsSet.Builder().audience(audience).subject(subject).expirationTime(Date.from(expiresAt)).build());
		jwt.sign(new ECDSASigner(keys.privateKey()));
		String token = jwt.serialize();
		jwtByAudience.put(audience, new CachedJwt(token, expiresAt));
		return token;
	}

	private record CachedJwt(String token, Instant expiresAt) {
	}
}
