package cl.bruma.api.dto;

import cl.bruma.api.model.EstadoBoleta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public final class BoletaDtos {
    private BoletaDtos() {}

    public record ItemRequest(
            @NotBlank(message = "El código del producto es obligatorio") String codigo,
            @NotNull(message = "La cantidad es obligatoria")
            @Min(value = 1, message = "La cantidad mínima es 1")
            @Max(value = 20, message = "La cantidad máxima por producto es 20")
            Integer cantidad) {}

    public record EnvioRequest(
            @NotBlank(message = "El nombre es obligatorio") @Size(max = 60) String nombre,
            @Size(max = 120) String correo,
            @Size(max = 20) String telefono,
            @NotBlank(message = "La región es obligatoria") @Size(max = 80) String region,
            @NotBlank(message = "La comuna es obligatoria") @Size(max = 80) String comuna,
            @NotBlank(message = "La dirección es obligatoria") @Size(min = 5, max = 160) String direccion,
            @Size(max = 300) String referencia) {}

    public record BoletaRequest(
            @NotEmpty(message = "El carrito está vacío") @Valid List<ItemRequest> items,
            @NotNull(message = "Faltan los datos de despacho") @Valid EnvioRequest envio) {}

    public record EstadoRequest(@NotNull(message = "El estado es obligatorio") EstadoBoleta estado) {}

    public record DetalleResponse(
            Long productoId, String codigo, String nombre, int cantidad, int precioUnitario, int subtotal) {}

    public record BoletaResponse(
            Long id,
            String numero,
            LocalDateTime fecha,
            EstadoBoleta estado,
            int subtotal,
            int despacho,
            int total,
            Long usuarioId,
            String usuarioNombre,
            String usuarioCorreo,
            EnvioRequest envio,
            String ordenWebpay,
            List<DetalleResponse> detalles) {}
}
