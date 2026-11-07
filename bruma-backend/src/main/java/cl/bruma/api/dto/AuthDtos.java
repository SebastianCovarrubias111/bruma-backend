package cl.bruma.api.dto;

import cl.bruma.api.model.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegistroRequest(
            @NotBlank(message = "El nombre es obligatorio")
            @Size(min = 3, max = 60, message = "El nombre debe tener entre 3 y 60 caracteres")
            String nombre,
            @NotBlank(message = "El correo es obligatorio")
            @Email(message = "El correo no es válido")
            @Size(max = 120)
            String correo,
            @NotBlank(message = "La contraseña es obligatoria")
            @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
            @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "La contraseña debe incluir letras y números")
            String clave,
            @NotNull(message = "La fecha de nacimiento es obligatoria")
            LocalDate fechaNacimiento) {}

    public record LoginRequest(
            @NotBlank(message = "El correo es obligatorio") String correo,
            @NotBlank(message = "La contraseña es obligatoria") String clave) {}

    public record RefreshRequest(@NotBlank(message = "El refresh token es obligatorio") String refreshToken) {}

    public record UsuarioResumen(Long id, String nombre, String correo, Rol rol) {}

    public record AuthResponse(
            String accessToken,
            String refreshToken,
            String tipo,
            long expiraEnSegundos,
            UsuarioResumen usuario) {}
}
