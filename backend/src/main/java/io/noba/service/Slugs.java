package io.noba.service;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;

/** Génère des identifiants lisibles pour les URLs (organisations, codes publics d'établissement). */
public final class Slugs {

	private static final SecureRandom RANDOM = new SecureRandom();
	private static final String ALPHABET = "abcdefghjkmnpqrstuvwxyz23456789";

	private Slugs() {
	}

	public static String slugify(String text) {
		String ascii = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		String slug = ascii.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
		if (slug.length() > 40) {
			slug = slug.substring(0, 40).replaceAll("-$", "");
		}
		return slug.isEmpty() ? "noba" : slug;
	}

	/** « Agence Centre » → « agence-centre-k7m2 », en garantissant l'unicité. */
	public static String uniqueCode(String name, Predicate<String> exists) {
		String base = slugify(name);
		String candidate;
		do {
			candidate = base + "-" + randomSuffix(4);
		} while (exists.test(candidate));
		return candidate;
	}

	private static String randomSuffix(int length) {
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
		}
		return sb.toString();
	}
}
