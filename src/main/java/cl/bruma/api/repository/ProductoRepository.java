package cl.bruma.api.repository;

import cl.bruma.api.model.Producto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    List<Producto> findByActivoTrueOrderByIdAsc();
    List<Producto> findAllByOrderByIdAsc();
    Optional<Producto> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);
    boolean existsByCategoriaId(Long categoriaId);
}
