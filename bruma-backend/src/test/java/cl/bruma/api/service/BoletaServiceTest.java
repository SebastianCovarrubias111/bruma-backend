package cl.bruma.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.bruma.api.dto.BoletaDtos.BoletaRequest;
import cl.bruma.api.dto.BoletaDtos.BoletaResponse;
import cl.bruma.api.dto.BoletaDtos.EnvioRequest;
import cl.bruma.api.dto.BoletaDtos.ItemRequest;
import cl.bruma.api.exception.ConflictoException;
import cl.bruma.api.exception.ProhibidoException;
import cl.bruma.api.exception.RecursoNoEncontradoException;
import cl.bruma.api.exception.SolicitudInvalidaException;
import cl.bruma.api.model.Boleta;
import cl.bruma.api.model.Categoria;
import cl.bruma.api.model.DetalleBoleta;
import cl.bruma.api.model.EstadoBoleta;
import cl.bruma.api.model.Producto;
import cl.bruma.api.model.Rol;
import cl.bruma.api.model.Usuario;
import cl.bruma.api.repository.BoletaRepository;
import cl.bruma.api.repository.ProductoRepository;
import cl.bruma.api.repository.UsuarioRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BoletaServiceTest {

    @Mock BoletaRepository boletas;
    @Mock ProductoRepository productos;
    @Mock UsuarioRepository usuarios;

    BoletaService service;
    Usuario cliente;
    Producto ipa;

    @BeforeEach
    void preparar() {
        service = new BoletaService(boletas, productos, usuarios);

        cliente = new Usuario();
        cliente.setId(1L);
        cliente.setNombre("Cliente Demo");
        cliente.setCorreo("cliente@bruma.cl");
        cliente.setRol(Rol.CLIENTE);

        Categoria categoria = new Categoria();
        categoria.setId(1L);
        categoria.setNombre("IPA");

        ipa = new Producto();
        ipa.setId(3L);
        ipa.setCodigo("BRU003");
        ipa.setNombre("Volcánica IPA");
        ipa.setEstilo("IPA");
        ipa.setCategoria(categoria);
        ipa.setPrecio(6990);
        ipa.setStock(10);
        ipa.setActivo(true);
    }

    private BoletaRequest pedido(String codigo, int cantidad) {
        EnvioRequest envio = new EnvioRequest("Cliente Demo", "cliente@bruma.cl", "+56911112222",
                "Los Ríos", "Valdivia", "Calle Falsa 123", null);
        return new BoletaRequest(List.of(new ItemRequest(codigo, cantidad)), envio);
    }

    private void simularGuardado() {
        when(usuarios.findByCorreo("cliente@bruma.cl")).thenReturn(Optional.of(cliente));
        when(productos.findByCodigo("BRU003")).thenReturn(Optional.of(ipa));
        lenient().when(boletas.save(any(Boleta.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void despachoGratisDesde30000() {
        assertEquals(3500, BoletaService.calcularDespacho(29_999));
        assertEquals(0, BoletaService.calcularDespacho(30_000));
        assertEquals(0, BoletaService.calcularDespacho(0));
    }

    @Test
    void creaBoletaCalculandoTotalesEnElServidor() {
        simularGuardado();

        BoletaResponse r = service.crear("cliente@bruma.cl", pedido("bru003", 2));

        assertEquals(13_980, r.subtotal());          // 2 x 6.990
        assertEquals(3_500, r.despacho());           // bajo $30.000
        assertEquals(17_480, r.total());
        assertEquals(EstadoBoleta.PENDIENTE, r.estado());
        assertEquals(1, r.detalles().size());
        assertEquals(6990, r.detalles().get(0).precioUnitario());
    }

    @Test
    void despachoGratisCuandoElSubtotalLlegaA30000() {
        ipa.setPrecio(10_000);
        simularGuardado();

        BoletaResponse r = service.crear("cliente@bruma.cl", pedido("BRU003", 3));

        assertEquals(30_000, r.subtotal());
        assertEquals(0, r.despacho());
        assertEquals(30_000, r.total());
    }

    @Test
    void descuentaElStockAlComprar() {
        simularGuardado();

        service.crear("cliente@bruma.cl", pedido("BRU003", 4));

        assertEquals(6, ipa.getStock());
    }

    @Test
    void rechazaCuandoNoHayStockSuficiente() {
        simularGuardado();

        SolicitudInvalidaException e = assertThrows(SolicitudInvalidaException.class,
                () -> service.crear("cliente@bruma.cl", pedido("BRU003", 11)));

        assertTrue(e.getMessage().contains("Stock insuficiente"));
        assertEquals(10, ipa.getStock());
        verify(boletas, never()).save(any(Boleta.class));
    }

    @Test
    void rechazaProductoInexistente() {
        when(usuarios.findByCorreo("cliente@bruma.cl")).thenReturn(Optional.of(cliente));
        when(productos.findByCodigo("NOEXISTE")).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.crear("cliente@bruma.cl", pedido("NOEXISTE", 1)));
    }

    @Test
    void rechazaProductoDadoDeBaja() {
        ipa.setActivo(false);
        when(usuarios.findByCorreo("cliente@bruma.cl")).thenReturn(Optional.of(cliente));
        when(productos.findByCodigo("BRU003")).thenReturn(Optional.of(ipa));

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.crear("cliente@bruma.cl", pedido("BRU003", 1)));
    }

    private Boleta boletaExistente(EstadoBoleta estado) {
        Boleta b = new Boleta();
        b.setId(7L);
        b.setNumero("BRTEST");
        b.setUsuario(cliente);
        b.setEstado(estado);
        b.setNombreEnvio("Cliente Demo");
        b.setRegion("Los Ríos");
        b.setComuna("Valdivia");
        b.setDireccion("Calle Falsa 123");
        DetalleBoleta d = new DetalleBoleta();
        d.setProducto(ipa);
        d.setCantidad(3);
        d.setPrecioUnitario(6990);
        d.setSubtotal(20_970);
        b.agregarDetalle(d);
        return b;
    }

    @Test
    void anularUnaBoletaReponeElStock() {
        Boleta b = boletaExistente(EstadoBoleta.PENDIENTE);
        when(boletas.findById(7L)).thenReturn(Optional.of(b));
        when(boletas.save(any(Boleta.class))).thenAnswer(inv -> inv.getArgument(0));

        BoletaResponse r = service.cambiarEstado(7L, EstadoBoleta.ANULADA);

        assertEquals(EstadoBoleta.ANULADA, r.estado());
        assertEquals(13, ipa.getStock()); // 10 + 3
    }

    @Test
    void unaBoletaAnuladaNoPuedeCambiarDeEstado() {
        when(boletas.findById(7L)).thenReturn(Optional.of(boletaExistente(EstadoBoleta.ANULADA)));

        assertThrows(ConflictoException.class, () -> service.cambiarEstado(7L, EstadoBoleta.PAGADA));
    }

    @Test
    void unaBoletaPagadaNoSePuedeEliminar() {
        when(boletas.findById(7L)).thenReturn(Optional.of(boletaExistente(EstadoBoleta.PAGADA)));

        assertThrows(ConflictoException.class, () -> service.eliminar(7L));
    }

    @Test
    void unClienteNoPuedeVerLaBoletaDeOtro() {
        when(boletas.findById(7L)).thenReturn(Optional.of(boletaExistente(EstadoBoleta.PENDIENTE)));

        assertThrows(ProhibidoException.class, () -> service.obtener(7L, "otro@correo.cl", false));
    }

    @Test
    void elPersonalPuedeVerCualquierBoleta() {
        when(boletas.findById(7L)).thenReturn(Optional.of(boletaExistente(EstadoBoleta.PENDIENTE)));

        BoletaResponse r = service.obtener(7L, "vendedor@bruma.cl", true);

        assertEquals("BRTEST", r.numero());
    }

    @Test
    void confirmarPagoMarcaPagadaYGuardaLaOrdenWebpay() {
        Boleta b = boletaExistente(EstadoBoleta.PENDIENTE);
        when(boletas.findById(7L)).thenReturn(Optional.of(b));
        when(boletas.save(any(Boleta.class))).thenAnswer(inv -> inv.getArgument(0));

        BoletaResponse r = service.confirmarPago(7L, "ORDEN123");

        assertEquals(EstadoBoleta.PAGADA, r.estado());
        assertEquals("ORDEN123", r.ordenWebpay());
    }

    @Test
    void elDuenoPuedeCancelarSuOrdenPendienteYSeRepone_elStock() {
        Boleta b = boletaExistente(EstadoBoleta.PENDIENTE);
        when(boletas.findById(7L)).thenReturn(Optional.of(b));
        when(boletas.save(any(Boleta.class))).thenAnswer(inv -> inv.getArgument(0));

        BoletaResponse r = service.cancelarPropia(7L, "cliente@bruma.cl");

        assertEquals(EstadoBoleta.ANULADA, r.estado());
        assertEquals(13, ipa.getStock());
    }

    @Test
    void nadieMasPuedeCancelarUnaOrdenAjena() {
        when(boletas.findById(7L)).thenReturn(Optional.of(boletaExistente(EstadoBoleta.PENDIENTE)));

        assertThrows(ProhibidoException.class, () -> service.cancelarPropia(7L, "otro@correo.cl"));
    }

    @Test
    void noSePuedeCancelarUnaOrdenYaPagada() {
        when(boletas.findById(7L)).thenReturn(Optional.of(boletaExistente(EstadoBoleta.PAGADA)));

        assertThrows(ConflictoException.class, () -> service.cancelarPropia(7L, "cliente@bruma.cl"));
    }

    @Test
    void laExpiracionAnulaLasPendientesAntiguasYReponeStock() {
        Boleta vieja = boletaExistente(EstadoBoleta.PENDIENTE);
        when(boletas.findByEstadoAndFechaBefore(any(EstadoBoleta.class), any(java.time.LocalDateTime.class)))
                .thenReturn(List.of(vieja));

        int n = service.expirarPendientes(30);

        assertEquals(1, n);
        assertEquals(EstadoBoleta.ANULADA, vieja.getEstado());
        assertEquals(13, ipa.getStock());
    }
}
