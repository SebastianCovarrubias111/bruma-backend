package cl.bruma.api.repository;

import cl.bruma.api.model.DetalleBoleta;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetalleBoletaRepository extends JpaRepository<DetalleBoleta, Long> {
    boolean existsByProductoId(Long productoId);
}
