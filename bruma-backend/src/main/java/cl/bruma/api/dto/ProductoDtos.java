package cl.bruma.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ProductoDtos {
    private ProductoDtos() {}

    public record ProductoRequest(
            @NotBlank(message = "El código es obligatorio")
            @Size(max = 20, message = "El código admite hasta 20 caracteres")
            String codigo,
            @NotBlank(message = "El nombre es obligatorio")
            @Size(max = 80, message = "El nombre admite hasta 80 caracteres")
            String nombre,
            @NotBlank(message = "El estilo es obligatorio")
            @Size(max = 60)
            String estilo,
            @NotNull(message = "La categoría es obligatoria")
            Long categoriaId,
            @NotNull(message = "El ABV es obligatorio")
            @DecimalMin(value = "0.0", message = "El ABV no puede ser negativo")
            @DecimalMax(value = "20.0", message = "El ABV máximo es 20")
            Double abv,
            @NotNull(message = "El IBU es obligatorio")
            @Min(value = 0, message = "El IBU no puede ser negativo")
            @Max(value = 120, message = "El IBU máximo es 120")
            Integer ibu,
            @NotBlank(message = "La presentación es obligatoria")
            @Size(max = 40)
            String presentacion,
            @NotNull(message = "El precio es obligatorio")
            @Min(value = 1, message = "El precio debe ser mayor a 0")
            Integer precio,
            @Size(max = 500, message = "La descripción admite hasta 500 caracteres")
            String descripcion,
            @NotNull(message = "El stock es obligatorio")
            @Min(value = 0, message = "El stock no puede ser negativo")
            Integer stock,
            Boolean activo) {}

    public record ProductoResponse(
            Long id,
            String codigo,
            String nombre,
            String estilo,
            Long categoriaId,
            String categoria,
            double abv,
            int ibu,
            String presentacion,
            int precio,
            String descripcion,
            int stock,
            boolean activo) {}
}
