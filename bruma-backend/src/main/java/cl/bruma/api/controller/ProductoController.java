package cl.bruma.api.controller;

import cl.bruma.api.dto.ProductoDtos.ProductoRequest;
import cl.bruma.api.dto.ProductoDtos.ProductoResponse;
import cl.bruma.api.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/productos")
@Tag(name = "Productos", description = "Catálogo de cervezas. Lectura pública; escritura solo ADMIN")
public class ProductoController {

    private final ProductoService service;

    public ProductoController(ProductoService service) {
        this.service = service;
    }

    @Operation(summary = "Listar productos activos (público)",
            description = "Filtros opcionales: ?categoria=IPA y ?q=texto (busca en nombre y estilo).")
    @GetMapping
    public List<ProductoResponse> listar(@RequestParam(required = false) String categoria,
                                         @RequestParam(required = false) String q) {
        return service.listarActivos(categoria, q);
    }

    @Operation(summary = "Listar todos los productos, incluso dados de baja (ADMIN, VENDEDOR)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/todos")
    public List<ProductoResponse> listarTodos(@RequestParam(required = false) String categoria,
                                              @RequestParam(required = false) String q) {
        return service.listarTodos(categoria, q);
    }

    @Operation(summary = "Detalle de un producto (público)")
    @GetMapping("/{id}")
    public ProductoResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @Operation(summary = "Crear producto (ADMIN)", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(req));
    }

    @Operation(summary = "Actualizar producto (ADMIN)", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}")
    public ProductoResponse actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest req) {
        return service.actualizar(id, req);
    }

    @Operation(summary = "Eliminar producto (ADMIN)", security = @SecurityRequirement(name = "bearerAuth"),
            description = "Si el producto ya fue vendido se da de baja (activo=false) en lugar de borrarse.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
