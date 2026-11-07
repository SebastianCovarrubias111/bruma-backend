package cl.bruma.api.dto;

import cl.bruma.api.model.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public final class UsuarioDtos {
    private UsuarioDtos() {}

    /** Usado por el administrador para crear/editar usuarios. En edición la clave es opcional. */
    public record UsuarioRequest(
            @NotBlank(message = "El nombre es obligatorio")
            @Size(min = 3, max = 60, message = "El nombre debe tener entre 3 y 60 caracteres")
            String nombre,
            @NotBlank(message = "El correo es obligatorio")
            @Email(message = "El correo no es válido")
            String correo,
            @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
            String clave,
            LocalDate fechaNacimiento,
            @NotNull(message = "El rol es obligatorio")
            Rol rol,
            Boolean activo) {}

    public record UsuarioResponse(
            Long id, String nombre, String correo, LocalDate fechaNacimiento, Rol rol, boolean activo) {}
}
