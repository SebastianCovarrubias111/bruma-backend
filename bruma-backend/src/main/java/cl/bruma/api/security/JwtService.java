package cl.bruma.api.security;

import cl.bruma.api.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Genera y valida los access tokens JWT (firmados con HMAC-SHA256). */
@Service
public class JwtService {

    private final SecretKey clave;
    private final long minutosAcceso;

    public JwtService(@Value("${app.jwt.secret}") String secreto,
                      @Value("${app.jwt.access-minutes:30}") long minutosAcceso) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret debe tener al menos 32 caracteres (256 bits).");
        }
        this.clave = Keys.hmacShaKeyFor(bytes);
        this.minutosAcceso = minutosAcceso;
    }

    public String generarAccessToken(Usuario u) {
        Date ahora = new Date();
        Date expira = new Date(ahora.getTime() + minutosAcceso * 60_000L);
        return Jwts.builder()
                .subject(u.getCorreo())
                .claim("rol", u.getRol().name())
                .claim("nombre", u.getNombre())
                .issuedAt(ahora)
                .expiration(expira)
                .signWith(clave)
                .compact();
    }

    /** Lanza JwtException si el token está vencido, mal firmado o malformado. */
    public Claims validar(String token) throws JwtException {
        return Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload();
    }

    public long segundosDeVida() {
        return minutosAcceso * 60L;
    }
}
