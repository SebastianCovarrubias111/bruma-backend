package cl.bruma.api.service;

import cl.bruma.api.dto.CategoriaDtos.CategoriaRequest;
import cl.bruma.api.dto.CategoriaDtos.CategoriaResponse;
import cl.bruma.api.exception.ConflictoException;
import cl.bruma.api.exception.RecursoNoEncontradoException;
import cl.bruma.api.model.Categoria;
import cl.bruma.api.repository.CategoriaRepository;
import cl.bruma.api.repository.ProductoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoriaService {

    private final CategoriaRepository categorias;
    private final ProductoRepository productos;

    public CategoriaService(CategoriaRepository categorias, ProductoRepository productos) {
        this.categorias = categorias;
        this.productos = productos;
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar() {
        return categorias.findAll().stream().map(CategoriaService::aDto).toList();
    }

    @Transactional(readOnly = true)
    public CategoriaResponse obtener(Long id) {
        return aDto(buscar(id));
    }

    @Transactional
    public CategoriaResponse crear(CategoriaRequest req) {
        if (categorias.existsByNombreIgnoreCase(req.nombre().trim())) {
            throw new ConflictoException("Ya existe una categoría con ese nombre.");
        }
        Categoria c = new Categoria();
        c.setNombre(req.nombre().trim());
        c.setDescripcion(req.descripcion());
        return aDto(categorias.save(c));
    }

    @Transactional
    public CategoriaResponse actualizar(Long id, CategoriaRequest req) {
        Categoria c = buscar(id);
        categorias.findByNombreIgnoreCase(req.nombre().trim()).ifPresent(otra -> {
            if (!otra.getId().equals(id)) {
                throw new ConflictoException("Ya existe una categoría con ese nombre.");
            }
        });
        c.setNombre(req.nombre().trim());
        c.setDescripcion(req.descripcion());
        return aDto(categorias.save(c));
    }

    @Transactional
    public void eliminar(Long id) {
        Categoria c = buscar(id);
        if (productos.existsByCategoriaId(id)) {
            throw new ConflictoException("No se puede eliminar: hay productos asociados a esta categoría.");
        }
        categorias.delete(c);
    }

    private Categoria buscar(Long id) {
        return categorias.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría " + id + " no encontrada."));
    }

    static CategoriaResponse aDto(Categoria c) {
        return new CategoriaResponse(c.getId(), c.getNombre(), c.getDescripcion());
    }
}
