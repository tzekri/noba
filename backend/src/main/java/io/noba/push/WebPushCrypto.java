package io.noba.push;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Chiffrement des messages Web Push (RFC 8291, encodage « aes128gcm » de la RFC 8188),
 * avec uniquement la cryptographie du JDK : ECDH P-256, HKDF-SHA256, AES-128-GCM.
 */
public final class WebPushCrypto {

	static final int RECORD_SIZE = 4096;
	private static final SecureRandom RANDOM = new SecureRandom();
	private static final ECParameterSpec P256 = p256();

	private WebPushCrypto() {
	}

	/** Chiffre la charge utile pour un abonnement (clé publique p256dh et secret d'authentification du navigateur). */
	public static byte[] encrypt(byte[] plaintext, byte[] uaPublic, byte[] authSecret) throws GeneralSecurityException {
		byte[] salt = new byte[16];
		RANDOM.nextBytes(salt);
		return encrypt(plaintext, uaPublic, authSecret, newKeyPair(), salt);
	}

	/** Variante déterministe (clé éphémère et sel fournis), utilisée par les tests. */
	static byte[] encrypt(byte[] plaintext, byte[] uaPublic, byte[] authSecret, KeyPair asKeys, byte[] salt)
			throws GeneralSecurityException {
		if (plaintext.length + 17 > RECORD_SIZE) {
			throw new IllegalArgumentException("Message trop long pour un seul enregistrement");
		}
		byte[] asPublic = toUncompressed((ECPublicKey) asKeys.getPublic());
		byte[] ecdhSecret = ecdh(asKeys.getPrivate(), fromUncompressed(uaPublic));

		// RFC 8291 §3.4 : combinaison du secret ECDH et du secret d'authentification
		byte[] prkKey = hmac(authSecret, ecdhSecret);
		byte[] keyInfo = concat("WebPush: info".getBytes(StandardCharsets.US_ASCII), new byte[] {0}, uaPublic, asPublic, new byte[] {1});
		byte[] ikm = hmac(prkKey, keyInfo);

		// RFC 8188 §2.2 : clé de chiffrement et nonce
		byte[] prk = hmac(salt, ikm);
		byte[] cek = Arrays.copyOf(hmac(prk, info("Content-Encoding: aes128gcm")), 16);
		byte[] nonce = Arrays.copyOf(hmac(prk, info("Content-Encoding: nonce")), 12);

		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(cek, "AES"), new GCMParameterSpec(128, nonce));
		// Délimiteur 0x02 : dernier (et unique) enregistrement, sans remplissage
		byte[] ciphertext = cipher.doFinal(concat(plaintext, new byte[] {2}));

		ByteBuffer header = ByteBuffer.allocate(16 + 4 + 1 + asPublic.length);
		header.put(salt).putInt(RECORD_SIZE).put((byte) asPublic.length).put(asPublic);
		return concat(header.array(), ciphertext);
	}

	public static KeyPair newKeyPair() throws GeneralSecurityException {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
		generator.initialize(new ECGenParameterSpec("secp256r1"), RANDOM);
		return generator.generateKeyPair();
	}

	/** Clé publique au format « point non compressé » (65 octets : 0x04 || X || Y), celui de l'API Push. */
	public static byte[] toUncompressed(ECPublicKey key) {
		return concat(new byte[] {4}, fixed(key.getW().getAffineX()), fixed(key.getW().getAffineY()));
	}

	public static ECPublicKey fromUncompressed(byte[] point) throws GeneralSecurityException {
		if (point.length != 65 || point[0] != 4) {
			throw new GeneralSecurityException("Clé publique P-256 invalide");
		}
		BigInteger x = new BigInteger(1, Arrays.copyOfRange(point, 1, 33));
		BigInteger y = new BigInteger(1, Arrays.copyOfRange(point, 33, 65));
		return (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(new ECPoint(x, y), P256));
	}

	static byte[] ecdh(PrivateKey privateKey, ECPublicKey publicKey) throws GeneralSecurityException {
		KeyAgreement agreement = KeyAgreement.getInstance("ECDH");
		agreement.init(privateKey);
		agreement.doPhase(publicKey, true);
		return agreement.generateSecret();
	}

	static byte[] hmac(byte[] key, byte[] data) throws GeneralSecurityException {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(key, "HmacSHA256"));
		return mac.doFinal(data);
	}

	static byte[] info(String label) {
		return concat(label.getBytes(StandardCharsets.US_ASCII), new byte[] {0, 1});
	}

	static byte[] concat(byte[]... parts) {
		int length = 0;
		for (byte[] p : parts) {
			length += p.length;
		}
		byte[] out = new byte[length];
		int offset = 0;
		for (byte[] p : parts) {
			System.arraycopy(p, 0, out, offset, p.length);
			offset += p.length;
		}
		return out;
	}

	private static byte[] fixed(BigInteger value) {
		byte[] raw = value.toByteArray();
		if (raw.length == 32) {
			return raw;
		}
		byte[] out = new byte[32];
		if (raw.length > 32) {
			System.arraycopy(raw, raw.length - 32, out, 0, 32);
		} else {
			System.arraycopy(raw, 0, out, 32 - raw.length, raw.length);
		}
		return out;
	}

	private static ECParameterSpec p256() {
		try {
			AlgorithmParameters params = AlgorithmParameters.getInstance("EC");
			params.init(new ECGenParameterSpec("secp256r1"));
			return params.getParameterSpec(ECParameterSpec.class);
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException(e);
		}
	}
}
