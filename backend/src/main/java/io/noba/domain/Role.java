package io.noba.domain;

public enum Role {
	/** Opérateur de la plateforme Noba (toutes organisations). */
	SUPER_ADMIN,
	/** Responsable d'une organisation cliente (tous ses établissements). */
	ORG_ADMIN,
	/** Agent de guichet, rattaché à un établissement. */
	AGENT
}
