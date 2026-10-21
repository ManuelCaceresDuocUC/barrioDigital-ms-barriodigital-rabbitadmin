package cl.barriodigital.rabbitadmin.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtRolesConversionTest {

    private final SecurityConfig config = new SecurityConfig();

    private Jwt jwt(Map<String, Object> claims) {
        return new Jwt("token", Instant.now(), Instant.now().plusSeconds(300),
                Map.of("alg", "none"), claims);
    }

    @Test
    void elClaimRolesSeConvierteEnAutoridadesSinPrefijo() {
        var authentication = config.jwtAuthenticationConverter()
                .convert(jwt(Map.of("sub", "u1", "roles", List.of("Admin", "Auditor"))));

        // Spring Security 7 agrega además FACTOR_BEARER; lo que importa es que los roles lleguen intactos.
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .contains("Admin", "Auditor");
    }

    @Test
    void unTokenSinRolesNoTieneNingunRol() {
        var authentication = config.jwtAuthenticationConverter().convert(jwt(Map.of("sub", "u1")));

        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .allMatch(authority -> authority.startsWith("FACTOR_"));
    }
}