package cl.bruma.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class CategoriaDtos {
    private CategoriaDtos() {}

    public record CategoriaRequest(
            @NotBlank(message = "El nombre es obligatorio")
            @Size(max = 40, message = "El nombre admite hasta 40 caracteres")
            String nombre,
            @Size(max = 200, message = "La descripción admite hasta 200 caracteres")
            String descripcion) {}

    public record CategoriaResponse(Long id, String nombre, String descripcion) {}
}
