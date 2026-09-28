package io.noba.security;

import io.noba.domain.StaffUser;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

	private final JwtEncoder encoder;
	private final Duration validity;

	public TokenService(JwtEncoder encoder, @Value("${app.jwt.validity-hours}") long validityHours) {
		this.encoder = encoder;
		this.validity = Duration.ofHours(validityHours);
	}

	public String issue(StaffUser user) {
		Instant now = Instant.now();
		JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
				.issuer("noba")
				.subject(user.getId().toString())
				.issuedAt(now)
				.expiresAt(now.plus(validity))
				.claim("role", user.getRole().name())
				.claim("name", user.getFullName());
		if (user.getOrganization() != null) {
			claims.claim("orgId", user.getOrganization().getId());
		}
		if (user.getBranch() != null) {
			claims.claim("branchId", user.getBranch().getId());
		}
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
	}
}
