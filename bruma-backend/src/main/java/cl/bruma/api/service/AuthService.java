package cl.bruma.api.service;

import cl.bruma.api.dto.AuthDtos.AuthResponse;
import cl.bruma.api.dto.AuthDtos.LoginRequest;
import cl.bruma.api.dto.AuthDtos.RegistroRequest;
import cl.bruma.api.dto.AuthDtos.UsuarioResumen;
import cl.bruma.api.exception.ConflictoException;
import cl.bruma.api.exception.NoAutenticadoException;
import cl.bruma.api.exception.SolicitudInvalidaException;
import cl.bruma.api.model.RefreshToken;
import cl.bruma.api.model.Rol;
import cl.bruma.api.model.Usuario;
import cl.bruma.api.repository.RefreshTokenRepository;
import cl.bruma.api.repository.UsuarioRepository;
import cl.bruma.api.security.JwtService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registro, login, renovación (refresh) y cierre de sesión. */
@Service
public class AuthService {

    private static final int EDAD_MINIMA = 18;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UsuarioRepository usuarios;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final long diasRefresh;

    public AuthService(UsuarioRepository usuarios, RefreshTokenRepository refreshTokens, PasswordEncoder encoder,
                       JwtService jwt, @Value("${app.jwt.refresh-days:7}") long diasRefresh) {
        this.usuarios = usuarios;
        this.refreshTokens = refreshTokens;
        this.encoder = encoder;
        this.jwt = jwt;
        this.diasRefresh = diasRefresh;
    }

    @Transactional
    public AuthResponse registrar(RegistroRequest req) {
        // Regla de negocio: venta de alcohol solo a mayores de 18 años
        if (Period.between(req.fechaNacimiento(), LocalDate.now()).getYears() < EDAD_MINIMA) {
            throw new SolicitudInvalidaException("Debes tener 18 años o más para registrarte.");
        }
        String correo = req.correo().trim().toLowerCase();
        if (usuarios.existsByCorreo(correo)) {
            throw new ConflictoException("Ese correo ya tiene una cuenta. Inicia sesión.");
        }
        Usuario u = new Usuario();
        u.setNombre(req.nombre().trim());
        u.setCorreo(correo);
        u.setClave(encoder.encode(req.clave()));
        u.setFechaNacimiento(req.fechaNacimiento());
        u.setRol(Rol.CLIENTE); // el registro público SIEMPRE crea clientes
        u.setActivo(true);
        usuarios.save(u);
        return emitirTokens(u);
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        Usuario u = usuarios.findByCorreo(req.correo().trim().toLowerCase()).orElse(null);
        // Mismo mensaje para "no existe" y "clave incorrecta": no revela qué correos están registrados
        if (u == null || !u.isActivo() || !encoder.matches(req.clave(), u.getClave())) {
            throw new NoAutenticadoException("Correo o contraseña incorrectos.");
        }
        return emitirTokens(u);
    }

    /** Rota el refresh token: el usado se revoca y se entrega uno nuevo junto con un nuevo access token. */
    @Transactional
    public AuthResponse renovar(String refreshToken) {
        RefreshToken guardado = refreshTokens.findByTokenHash(hash(refreshToken))
                .orElseThrow(() -> new NoAutenticadoException("La sesión no es válida. Inicia sesión nuevamente."));
        if (guardado.isRevocado() || guardado.getExpiraEn().isBefore(LocalDateTime.now())
                || !guardado.getUsuario().isActivo()) {
            throw new NoAutenticadoException("La sesión expiró. Inicia sesión nuevamente.");
        }
        guardado.setRevocado(true);
        refreshTokens.save(guardado);
        return emitirTokens(guardado.getUsuario());
    }

    @Transactional
    public void cerrarSesion(String refreshToken) {
        refreshTokens.findByTokenHash(hash(refreshToken)).ifPresent(t -> {
            t.setRevocado(true);
            refreshTokens.save(t);
        });
    }

    private AuthResponse emitirTokens(Usuario u) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String refreshPlano = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken rt = new RefreshToken();
        rt.setTokenHash(hash(refreshPlano)); // en la BD solo se guarda el hash, nunca el token
        rt.setUsuario(u);
        rt.setExpiraEn(LocalDateTime.now().plusDays(diasRefresh));
        rt.setRevocado(false);
        refreshTokens.save(rt);

        return new AuthResponse(jwt.generarAccessToken(u), refreshPlano, "Bearer", jwt.segundosDeVida(),
                new UsuarioResumen(u.getId(), u.getNombre(), u.getCorreo(), u.getRol()));
    }

    private static String hash(String valor) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
