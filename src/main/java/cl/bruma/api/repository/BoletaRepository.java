package cl.bruma.api.repository;

import cl.bruma.api.model.Boleta;
import cl.bruma.api.model.EstadoBoleta;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoletaRepository extends JpaRepository<Boleta, Long> {
    List<Boleta> findAllByOrderByFechaDesc();
    List<Boleta> findByUsuarioIdOrderByFechaDesc(Long usuarioId);
    boolean existsByUsuarioId(Long usuarioId);
    List<Boleta> findByEstadoAndFechaBefore(EstadoBoleta estado, LocalDateTime fecha);
}
