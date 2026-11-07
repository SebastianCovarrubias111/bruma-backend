package cl.bruma.api.service;

import cl.bruma.api.dto.ProductoDtos.ProductoRequest;
import cl.bruma.api.dto.ProductoDtos.ProductoResponse;
import cl.bruma.api.exception.ConflictoException;
import cl.bruma.api.exception.RecursoNoEncontradoException;
import cl.bruma.api.exception.SolicitudInvalidaException;
import cl.bruma.api.model.Categoria;
import cl.bruma.api.model.Producto;
import cl.bruma.api.repository.CategoriaRepository;
import cl.bruma.api.repository.DetalleBoletaRepository;
import cl.bruma.api.repository.ProductoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductoService {

    private final ProductoRepository productos;
    private final CategoriaRepository categorias;
    private final DetalleBoletaRepository detalles;

    public ProductoService(ProductoRepository productos, CategoriaRepository categorias,
                           DetalleBoletaRepository detalles) {
        this.productos = productos;
        this.categorias = categorias;
        this.detalles = detalles;
    }

    /** Catálogo público: solo productos activos. Filtros opcionales por categoría y texto. */
    @Transactional(readOnly = true)
    public List<ProductoResponse> listarActivos(String categoria, String texto) {
        return filtrar(productos.findByActivoTrueOrderByIdAsc(), categoria, texto);
    }

    /** Vista de gestión (ADMIN/VENDEDOR): incluye productos dados de baja. */
    @Transactional(readOnly = true)
    public List<ProductoResponse> listarTodos(String categoria, String texto) {
        return filtrar(productos.findAllByOrderByIdAsc(), categoria, texto);
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtener(Long id) {
        return aDto(buscar(id));
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest req) {
        String codigo = req.codigo().trim().toUpperCase();
        if (productos.existsByCodigo(codigo)) {
            throw new ConflictoException("Ya existe un producto con el código " + codigo + ".");
        }
        Producto p = new Producto();
        aplicar(p, req);
        p.setCodigo(codigo);
        return aDto(productos.save(p));
    }

    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest req) {
        Producto p = buscar(id);
        String codigo = req.codigo().trim().toUpperCase();
        productos.findByCodigo(codigo).ifPresent(otro -> {
            if (!otro.getId().equals(id)) {
                throw new ConflictoException("Ya existe un producto con el código " + codigo + ".");
            }
        });
        aplicar(p, req);
        p.setCodigo(codigo);
        return aDto(productos.save(p));
    }

    /** Si el producto ya fue vendido se da de baja (activo=false) para no romper el historial de boletas. */
    @Transactional
    public void eliminar(Long id) {
        Producto p = buscar(id);
        if (detalles.existsByProductoId(id)) {
            p.setActivo(false);
            productos.save(p);
        } else {
            productos.delete(p);
        }
    }

    private void aplicar(Producto p, ProductoRequest req) {
        Categoria c = categorias.findById(req.categoriaId())
                .orElseThrow(() -> new SolicitudInvalidaException("La categoría " + req.categoriaId() + " no existe."));
        p.setNombre(req.nombre().trim());
        p.setEstilo(req.estilo().trim());
        p.setCategoria(c);
        p.setAbv(req.abv());
        p.setIbu(req.ibu());
        p.setPresentacion(req.presentacion().trim());
        p.setPrecio(req.precio());
        p.setDescripcion(req.descripcion());
        p.setStock(req.stock());
        p.setActivo(req.activo() == null || req.activo());
    }

    private Producto buscar(Long id) {
        return productos.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto " + id + " no encontrado."));
    }

    private List<ProductoResponse> filtrar(List<Producto> lista, String categoria, String texto) {
        return lista.stream()
                .filter(p -> categoria == null || categoria.isBlank()
                        || p.getCategoria().getNombre().equalsIgnoreCase(categoria.trim()))
                .filter(p -> texto == null || texto.isBlank()
                        || p.getNombre().toLowerCase().contains(texto.trim().toLowerCase())
                        || p.getEstilo().toLowerCase().contains(texto.trim().toLowerCase()))
                .map(ProductoService::aDto)
                .toList();
    }

    static ProductoResponse aDto(Producto p) {
        return new ProductoResponse(p.getId(), p.getCodigo(), p.getNombre(), p.getEstilo(),
                p.getCategoria().getId(), p.getCategoria().getNombre(), p.getAbv(), p.getIbu(),
                p.getPresentacion(), p.getPrecio(), p.getDescripcion(), p.getStock(), p.isActivo());
    }
}
