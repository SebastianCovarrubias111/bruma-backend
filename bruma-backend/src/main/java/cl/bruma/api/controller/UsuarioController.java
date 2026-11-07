package cl.bruma.api.controller;

import cl.bruma.api.dto.UsuarioDtos.UsuarioRequest;
import cl.bruma.api.dto.UsuarioDtos.UsuarioResponse;
import cl.bruma.api.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/usuarios")
@Tag(name = "Usuarios", description = "Gestión de usuarios y roles. Solo ADMIN (excepto /me)")
@SecurityRequirement(name = "bearerAuth")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @Operation(summary = "Mi perfil (cualquier usuario autenticado)")
    @GetMapping("/me")
    public UsuarioResponse me(Authentication auth) {
        return service.perfil(auth.getName());
    }

    @Operation(summary = "Listar usuarios (ADMIN)")
    @GetMapping
    public List<UsuarioResponse> listar() {
        return service.listar();
    }

    @Operation(summary = "Obtener un usuario (ADMIN)")
    @GetMapping("/{id}")
    public UsuarioResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @Operation(summary = "Crear usuario con cualquier rol (ADMIN)")
    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody UsuarioRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(req));
    }

    @Operation(summary = "Actualizar usuario, rol o estado (ADMIN)",
            description = "La contraseña es opcional: si no se envía, se mantiene la actual.")
    @PutMapping("/{id}")
    public UsuarioResponse actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequest req, Authentication auth) {
        return service.actualizar(id, req, auth.getName());
    }

    @Operation(summary = "Eliminar usuario (ADMIN)",
            description = "Si el usuario tiene boletas se desactiva en lugar de borrarse.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, Authentication auth) {
        service.eliminar(id, auth.getName());
        return ResponseEntity.noContent().build();
    }
}
