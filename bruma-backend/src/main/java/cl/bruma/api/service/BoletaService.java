package cl.bruma.api.service;

import cl.bruma.api.dto.BoletaDtos.BoletaRequest;
import cl.bruma.api.dto.BoletaDtos.BoletaResponse;
import cl.bruma.api.dto.BoletaDtos.DetalleResponse;
import cl.bruma.api.dto.BoletaDtos.EnvioRequest;
import cl.bruma.api.dto.BoletaDtos.ItemRequest;
import cl.bruma.api.exception.ConflictoException;
import cl.bruma.api.exception.NoAutenticadoException;
import cl.bruma.api.exception.ProhibidoException;
import cl.bruma.api.exception.RecursoNoEncontradoException;
import cl.bruma.api.exception.SolicitudInvalidaException;
import cl.bruma.api.model.Boleta;
import cl.bruma.api.model.DetalleBoleta;
import cl.bruma.api.model.EstadoBoleta;
import cl.bruma.api.model.Producto;
import cl.bruma.api.model.Usuario;
import cl.bruma.api.repository.BoletaRepository;
import cl.bruma.api.repository.ProductoRepository;
import cl.bruma.api.repository.UsuarioRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lógica de negocio de las órdenes de compra (boletas).
 * Reglas: precios y totales SIEMPRE se calculan aquí (nunca se confía en el cliente), se valida y descuenta
 * el stock, y el despacho es gratis desde $30.000.
 */
@Service
public class BoletaService {

    public static final int DESPACHO_GRATIS_DESDE = 30_000;
    public static final int COSTO_DESPACHO = 3_500;
    public static final int MAX_POR_PRODUCTO = 20;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final BoletaRepository boletas;
    private final ProductoRepository productos;
    private final UsuarioRepository usuarios;

    public BoletaService(BoletaRepository boletas, ProductoRepository productos, UsuarioRepository usuarios) {
        this.boletas = boletas;
        this.productos = productos;
        this.usuarios = usuarios;
    }

    /** Misma regla que usa el carrito del frontend. */
    public static int calcularDespacho(int subtotal) {
        return (subtotal == 0 || subtotal >= DESPACHO_GRATIS_DESDE) ? 0 : COSTO_DESPACHO;
    }

    @Transactional
    public BoletaResponse crear(String correoUsuario, BoletaRequest req) {
        Usuario usuario = usuarios.findByCorreo(correoUsuario)
                .orElseThrow(() -> new NoAutenticadoException("Usuario no encontrado."));

        // Si el mismo producto viene repetido en el carrito, se suman las cantidades
        Map<String, Integer> cantidades = new LinkedHashMap<>();
        for (ItemRequest it : req.items()) {
            cantidades.merge(it.codigo().trim().toUpperCase(), it.cantidad(), Integer::sum);
        }

        Boleta boleta = new Boleta();
        boleta.setNumero(generarNumero());
        boleta.setUsuario(usuario);

        int subtotal = 0;
        for (Map.Entry<String, Integer> e : cantidades.entrySet()) {
            Producto p = productos.findByCodigo(e.getKey()).filter(Producto::isActivo)
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "El producto " + e.getKey() + " no existe o no está disponible."));
            int cantidad = e.getValue();
            if (cantidad > MAX_POR_PRODUCTO) {
                throw new SolicitudInvalidaException(
                        "Máximo " + MAX_POR_PRODUCTO + " unidades por producto (" + p.getNombre() + ").");
            }
            if (p.getStock() < cantidad) {
                throw new SolicitudInvalidaException("Stock insuficiente para " + p.getNombre()
                        + ": quedan " + p.getStock() + " unidades.");
            }
            p.setStock(p.getStock() - cantidad);
            productos.save(p);

            DetalleBoleta d = new DetalleBoleta();
            d.setProducto(p);
            d.setCantidad(cantidad);
            d.setPrecioUnitario(p.getPrecio()); // precio vigente en la BD, no el que mande el navegador
            d.setSubtotal(p.getPrecio() * cantidad);
            boleta.agregarDetalle(d);
            subtotal += d.getSubtotal();
        }

        int despacho = calcularDespacho(subtotal);
        boleta.setSubtotal(subtotal);
        boleta.setDespacho(despacho);
        boleta.setTotal(subtotal + despacho);
        boleta.setEstado(EstadoBoleta.PENDIENTE);

        EnvioRequest envio = req.envio();
        boleta.setNombreEnvio(envio.nombre().trim());
        boleta.setCorreoEnvio(envio.correo());
        boleta.setTelefonoEnvio(envio.telefono());
        boleta.setRegion(envio.region().trim());
        boleta.setComuna(envio.comuna().trim());
        boleta.setDireccion(envio.direccion().trim());
        boleta.setReferencia(envio.referencia());

        return aDto(boletas.save(boleta));
    }

    @Transactional(readOnly = true)
    public List<BoletaResponse> listarTodas() {
        return boletas.findAllByOrderByFechaDesc().stream().map(BoletaService::aDto).toList();
    }

    @Transactional(readOnly = true)
    public List<BoletaResponse> listarDeUsuario(String correo) {
        Usuario u = usuarios.findByCorreo(correo)
                .orElseThrow(() -> new NoAutenticadoException("Usuario no encontrado."));
        return boletas.findByUsuarioIdOrderByFechaDesc(u.getId()).stream().map(BoletaService::aDto).toList();
    }

    /** ADMIN y VENDEDOR ven cualquier boleta; un CLIENTE solo las suyas. */
    @Transactional(readOnly = true)
    public BoletaResponse obtener(Long id, String correo, boolean esPersonal) {
        Boleta b = buscar(id);
        if (!esPersonal && !b.getUsuario().getCorreo().equals(correo)) {
            throw new ProhibidoException("No puedes ver una orden que no es tuya.");
        }
        return aDto(b);
    }

    @Transactional
    public BoletaResponse cambiarEstado(Long id, EstadoBoleta nuevo) {
        Boleta b = buscar(id);
        EstadoBoleta actual = b.getEstado();
        if (actual == nuevo) {
            return aDto(b);
        }
        if (actual == EstadoBoleta.ANULADA) {
            throw new ConflictoException("Una boleta anulada no puede cambiar de estado.");
        }
        if (actual == EstadoBoleta.PAGADA && nuevo == EstadoBoleta.PENDIENTE) {
            throw new ConflictoException("Una boleta pagada no puede volver a pendiente.");
        }
        if (nuevo == EstadoBoleta.ANULADA) {
            reponerStock(b);
        }
        b.setEstado(nuevo);
        return aDto(boletas.save(b));
    }

    /**
     * Confirmación de pago hecha por el servidor de Webpay (llamada servidor a servidor).
     * Es idempotente: si ya estaba pagada devuelve la misma boleta.
     */
    @Transactional
    public BoletaResponse confirmarPago(Long id, String ordenWebpay) {
        Boleta b = buscar(id);
        if (b.getEstado() == EstadoBoleta.ANULADA) {
            throw new ConflictoException("La boleta fue anulada y no puede pagarse.");
        }
        b.setEstado(EstadoBoleta.PAGADA);
        b.setOrdenWebpay(ordenWebpay);
        return aDto(boletas.save(b));
    }

    /** El dueño de la orden puede cancelarla mientras esté PENDIENTE (por ejemplo si no llegó a pagar). */
    @Transactional
    public BoletaResponse cancelarPropia(Long id, String correo) {
        Boleta b = buscar(id);
        if (!b.getUsuario().getCorreo().equals(correo)) {
            throw new ProhibidoException("No puedes cancelar una orden que no es tuya.");
        }
        if (b.getEstado() != EstadoBoleta.PENDIENTE) {
            throw new ConflictoException("Solo se pueden cancelar órdenes pendientes de pago.");
        }
        reponerStock(b);
        b.setEstado(EstadoBoleta.ANULADA);
        return aDto(boletas.save(b));
    }

    /** Anula las órdenes PENDIENTES con más de N minutos y devuelve su stock. Retorna cuántas anuló. */
    @Transactional
    public int expirarPendientes(int minutos) {
        List<Boleta> vencidas = boletas.findByEstadoAndFechaBefore(
                EstadoBoleta.PENDIENTE, LocalDateTime.now().minusMinutes(minutos));
        for (Boleta b : vencidas) {
            reponerStock(b);
            b.setEstado(EstadoBoleta.ANULADA);
            boletas.save(b);
        }
        return vencidas.size();
    }

    /** Una boleta pagada no se elimina (se anula). Si se elimina una pendiente, se devuelve el stock. */
    @Transactional
    public void eliminar(Long id) {
        Boleta b = buscar(id);
        if (b.getEstado() == EstadoBoleta.PAGADA) {
            throw new ConflictoException("Una boleta pagada no se puede eliminar. Anúlala en su lugar.");
        }
        if (b.getEstado() == EstadoBoleta.PENDIENTE) {
            reponerStock(b);
        }
        boletas.delete(b);
    }

    private void reponerStock(Boleta b) {
        for (DetalleBoleta d : b.getDetalles()) {
            Producto p = d.getProducto();
            p.setStock(p.getStock() + d.getCantidad());
            productos.save(p);
        }
    }

    private Boleta buscar(Long id) {
        return boletas.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Boleta " + id + " no encontrada."));
    }

    private static String generarNumero() {
        String tiempo = Long.toString(System.currentTimeMillis(), 36).toUpperCase();
        String azar = String.format("%03X", RANDOM.nextInt(0x1000));
        return "BR" + tiempo + azar; // máx. 26 caracteres, compatible con el buy_order de Webpay
    }

    static BoletaResponse aDto(Boleta b) {
        List<DetalleResponse> detalles = b.getDetalles().stream()
                .map(d -> new DetalleResponse(d.getProducto().getId(), d.getProducto().getCodigo(),
                        d.getProducto().getNombre(), d.getCantidad(), d.getPrecioUnitario(), d.getSubtotal()))
                .toList();
        EnvioRequest envio = new EnvioRequest(b.getNombreEnvio(), b.getCorreoEnvio(), b.getTelefonoEnvio(),
                b.getRegion(), b.getComuna(), b.getDireccion(), b.getReferencia());
        Usuario u = b.getUsuario();
        return new BoletaResponse(b.getId(), b.getNumero(), b.getFecha(), b.getEstado(), b.getSubtotal(),
                b.getDespacho(), b.getTotal(), u.getId(), u.getNombre(), u.getCorreo(), envio,
                b.getOrdenWebpay(), detalles);
    }
}
