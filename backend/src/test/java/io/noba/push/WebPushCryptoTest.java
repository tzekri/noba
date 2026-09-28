package io.noba.push;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

/**
 * Vérifie le chiffrement Web Push en jouant le rôle du navigateur : on déchiffre le message
 * avec la clé privée « navigateur » en suivant la RFC 8291 côté réception.
 */
class WebPushCryptoTest {

	@Test
	void le_navigateur_retrouve_le_message() throws Exception {
		KeyPair browser = WebPushCrypto.newKeyPair();
		byte[] uaPublic = WebPushCrypto.toUncompressed((ECPublicKey) browser.getPublic());
		byte[] authSecret = "0123456789abcdef".getBytes(StandardCharsets.US_ASCII);
		String message = "{\"title\":\"C'est votre tour ! B-042\",\"body\":\"Présentez-vous au Guichet 3.\"}";

		byte[] body = WebPushCrypto.encrypt(message.getBytes(StandardCharsets.UTF_8), uaPublic, authSecret);

		assertThat(new String(decryptAsBrowser(body, browser.getPrivate(), uaPublic, authSecret), StandardCharsets.UTF_8))
				.isEqualTo(message);
	}

	@Test
	void entete_conforme_a_la_rfc_8188() throws Exception {
		KeyPair browser = WebPushCrypto.newKeyPair();
		byte[] uaPublic = WebPushCrypto.toUncompressed((ECPublicKey) browser.getPublic());
		byte[] salt = new byte[16];
		Arrays.fill(salt, (byte) 7);
		KeyPair server = WebPushCrypto.newKeyPair();

		byte[] body = WebPushCrypto.encrypt("ok".getBytes(StandardCharsets.UTF_8), uaPublic, new byte[16], server, salt);

		ByteBuffer header = ByteBuffer.wrap(body);
		byte[] readSalt = new byte[16];
		header.get(readSalt);
		assertThat(readSalt).isEqualTo(salt);
		assertThat(header.getInt()).isEqualTo(WebPushCrypto.RECORD_SIZE);
		assertThat(header.get()).isEqualTo((byte) 65);
		byte[] keyId = new byte[65];
		header.get(keyId);
		assertThat(keyId).isEqualTo(WebPushCrypto.toUncompressed((ECPublicKey) server.getPublic()));
		// "ok" + délimiteur (1 octet) + tag GCM (16 octets)
		assertThat(header.remaining()).isEqualTo(2 + 1 + 16);
	}

	@Test
	void refuse_une_cle_navigateur_invalide() {
		assertThatThrownBy(() -> WebPushCrypto.encrypt(new byte[1], new byte[10], new byte[16]))
				.isInstanceOf(GeneralSecurityException.class);
	}

	/** Déchiffrement côté navigateur (RFC 8291 §3.4 et RFC 8188 §2), écrit indépendamment du code testé. */
	private static byte[] decryptAsBrowser(byte[] body, PrivateKey uaPrivate, byte[] uaPublic, byte[] authSecret) throws Exception {
		ByteBuffer in = ByteBuffer.wrap(body);
		byte[] salt = new byte[16];
		in.get(salt);
		in.getInt();
		byte[] asPublic = new byte[in.get() & 0xff];
		in.get(asPublic);
		byte[] ciphertext = new byte[in.remaining()];
		in.get(ciphertext);

		byte[] ecdh = WebPushCrypto.ecdh(uaPrivate, WebPushCrypto.fromUncompressed(asPublic));
		byte[] prkKey = WebPushCrypto.hmac(authSecret, ecdh);
		byte[] ikm = WebPushCrypto.hmac(prkKey, WebPushCrypto.concat(
				"WebPush: info\0".getBytes(StandardCharsets.US_ASCII), uaPublic, asPublic, new byte[] {1}));
		byte[] prk = WebPushCrypto.hmac(salt, ikm);
		byte[] cek = Arrays.copyOf(WebPushCrypto.hmac(prk, "Content-Encoding: aes128gcm\0\1".getBytes(StandardCharsets.US_ASCII)), 16);
		byte[] nonce = Arrays.copyOf(WebPushCrypto.hmac(prk, "Content-Encoding: nonce\0\1".getBytes(StandardCharsets.US_ASCII)), 12);

		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(cek, "AES"), new GCMParameterSpec(128, nonce));
		byte[] padded = cipher.doFinal(ciphertext);
		assertThat(padded[padded.length - 1]).as("délimiteur de dernier enregistrement").isEqualTo((byte) 2);
		return Arrays.copyOf(padded, padded.length - 1);
	}
}
