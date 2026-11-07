package cl.bruma.api.controller;

import cl.bruma.api.dto.BoletaDtos.BoletaRequest;
import cl.bruma.api.dto.BoletaDtos.BoletaResponse;
import cl.bruma.api.dto.BoletaDtos.EstadoRequest;
import cl.bruma.api.service.BoletaService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/boletas")
@Tag(name = "Órdenes (boletas)", description = "Compras de la tienda. CLIENTE crea y ve las suyas; VENDEDOR/ADMIN ven todas; ADMIN gestiona")
@SecurityRequirement(name = "bearerAuth")
public class BoletaController {

    private final BoletaService service;

    public BoletaController(BoletaService service) {
        this.service = service;
    }

    @Operation(summary = "Crear una orden (CLIENTE, ADMIN)",
            description = "Calcula precios y despacho en el servidor, valida y descuenta stock. Queda en estado PENDIENTE.")
    @PostMapping
    public ResponseEntity<BoletaResponse> crear(@Valid @RequestBody BoletaRequest req, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(auth.getName(), req));
    }

    @Operation(summary = "Listar todas las órdenes (ADMIN, VENDEDOR)")
    @GetMapping
    public List<BoletaResponse> listar() {
        return service.listarTodas();
    }

    @Operation(summary = "Listar mis órdenes (cualquier usuario autenticado)")
    @GetMapping("/mias")
    public List<BoletaResponse> mias(Authentication auth) {
        return service.listarDeUsuario(auth.getName());
    }

    @Operation(summary = "Detalle de una orden",
            description = "ADMIN y VENDEDOR ven cualquiera; un CLIENTE solo las suyas (si no, 403).")
    @GetMapping("/{id}")
    public BoletaResponse obtener(@PathVariable Long id, Authentication auth) {
        boolean personal = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_VENDEDOR"));
        return service.obtener(id, auth.getName(), personal);
    }

    @Operation(summary = "Cancelar mi orden pendiente (dueño)",
            description = "Solo mientras la orden esté PENDIENTE. Repone el stock.")
    @PostMapping("/{id}/cancelar")
    public BoletaResponse cancelar(@PathVariable Long id, Authentication auth) {
        return service.cancelarPropia(id, auth.getName());
    }

    @Operation(summary = "Cambiar el estado de una orden (ADMIN)",
            description = "Estados: PENDIENTE, PAGADA, ANULADA. Al anular se repone el stock.")
    @PatchMapping("/{id}/estado")
    public BoletaResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoRequest req) {
        return service.cambiarEstado(id, req.estado());
    }

    @Operation(summary = "Eliminar una orden (ADMIN)",
            description = "No se puede eliminar una orden PAGADA (409). Si estaba PENDIENTE se repone el stock.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
