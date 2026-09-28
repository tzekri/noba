package io.noba.domain;

import jakarta.persistence.*;

/** Clés propres à la plateforme, générées une fois puis conservées en base (ex. clés VAPID du Web Push). */
@Entity
@Table(name = "platform_keys")
public class PlatformKey {

	@Id
	@Column(length = 40)
	private String name;

	/** Clé publique encodée X.509, en Base64. */
	@Column(nullable = false, length = 512)
	private String publicKey;

	/** Clé privée encodée PKCS#8, en Base64. */
	@Column(nullable = false, length = 512)
	private String privateKey;

	protected PlatformKey() {
	}

	public PlatformKey(String name, String publicKey, String privateKey) {
		this.name = name;
		this.publicKey = publicKey;
		this.privateKey = privateKey;
	}

	public String getName() { return name; }
	public String getPublicKey() { return publicKey; }
	public String getPrivateKey() { return privateKey; }
}
