package cl.bruma.api.controller;

import cl.bruma.api.dto.AuthDtos.AuthResponse;
import cl.bruma.api.dto.AuthDtos.LoginRequest;
import cl.bruma.api.dto.AuthDtos.RefreshRequest;
import cl.bruma.api.dto.AuthDtos.RegistroRequest;
import cl.bruma.api.dto.UsuarioDtos.UsuarioResponse;
import cl.bruma.api.service.AuthService;
import cl.bruma.api.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación", description = "Registro, login, renovación de token y sesión actual")
public class AuthController {

    private final AuthService auth;
    private final UsuarioService usuarios;

    public AuthController(AuthService auth, UsuarioService usuarios) {
        this.auth = auth;
        this.usuarios = usuarios;
    }

    @Operation(summary = "Registrar cliente", description = "Crea una cuenta con rol CLIENTE (mayores de 18 años) y entrega los tokens.")
    @PostMapping("/registro")
    public ResponseEntity<AuthResponse> registro(@Valid @RequestBody RegistroRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(auth.registrar(req));
    }

    @Operation(summary = "Iniciar sesión", description = "Valida correo y contraseña y devuelve accessToken (JWT) y refreshToken.")
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req);
    }

    @Operation(summary = "Renovar tokens", description = "Canjea un refreshToken vigente por un nuevo accessToken y un nuevo refreshToken (el anterior queda revocado).")
    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest req) {
        return auth.renovar(req.refreshToken());
    }

    @Operation(summary = "Cerrar sesión", description = "Revoca el refreshToken recibido.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest req) {
        auth.cerrarSesion(req.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Usuario de la sesión actual", security = @SecurityRequirement(name = "bearerAuth"),
            description = "Devuelve los datos del dueño del token. El frontend lo usa para restaurar la sesión al recargar.")
    @GetMapping("/me")
    public UsuarioResponse me(Authentication authentication) {
        return usuarios.perfil(authentication.getName());
    }
}
