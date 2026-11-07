package cl.bruma.api.exception;

import java.time.LocalDateTime;
import java.util.Map;

/** Formato único de error que devuelve toda la API. */
public record ApiError(LocalDateTime fecha, int estado, String error, String mensaje, Map<String, String> campos) {
    public static ApiError de(int estado, String error, String mensaje) {
        return new ApiError(LocalDateTime.now(), estado, error, mensaje, null);
    }
}
