package cl.bruma.api.controller;

import cl.bruma.api.dto.BoletaDtos.BoletaResponse;
import cl.bruma.api.exception.ProhibidoException;
import cl.bruma.api.service.BoletaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint servidor-a-servidor: el servidor de pagos Webpay (Node) lo llama cuando Transbank aprueba un pago.
 * No usa JWT: se protege con una llave compartida (INTERNAL_KEY) en el header X-Internal-Key.
 * Si INTERNAL_KEY no está configurada, el endpoint queda deshabilitado (siempre 403).
 */
@RestController
@RequestMapping("/api/v1/interno")
@Tag(name = "Integración de pagos", description = "Uso interno del servidor Webpay. No es para el frontend")
public class IntegracionPagoController {

    private final BoletaService boletas;
    private final String llaveInterna;

    public IntegracionPagoController(BoletaService boletas, @Value("${app.internal-key:}") String llaveInterna) {
        this.boletas = boletas;
        this.llaveInterna = llaveInterna;
    }

    @Operation(summary = "Confirmar pago de una orden (servidor Webpay)",
            description = "Marca la boleta como PAGADA. Requiere el header X-Internal-Key.")
    @PostMapping("/boletas/{id}/pagada")
    public BoletaResponse pagada(@PathVariable Long id,
                                 @RequestHeader(value = "X-Internal-Key", required = false) String llave,
                                 @RequestBody(required = false) Map<String, String> cuerpo) {
        if (llaveInterna.isBlank() || llave == null || !MessageDigest.isEqual(
                llaveInterna.getBytes(StandardCharsets.UTF_8), llave.getBytes(StandardCharsets.UTF_8))) {
            throw new ProhibidoException("Llave interna inválida.");
        }
        String orden = cuerpo == null ? null : cuerpo.get("ordenWebpay");
        return boletas.confirmarPago(id, orden);
    }
}
