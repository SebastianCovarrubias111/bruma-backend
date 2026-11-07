package cl.bruma.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "boleta")
public class Boleta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    @Column(nullable = false)
    private int subtotal;

    @Column(nullable = false)
    private int despacho;

    @Column(nullable = false)
    private int total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoBoleta estado = EstadoBoleta.PENDIENTE;

    @Column(nullable = false, length = 60)
    private String nombreEnvio;

    @Column(length = 120)
    private String correoEnvio;

    @Column(length = 20)
    private String telefonoEnvio;

    @Column(nullable = false, length = 80)
    private String region;

    @Column(nullable = false, length = 80)
    private String comuna;

    @Column(nullable = false, length = 160)
    private String direccion;

    @Column(length = 300)
    private String referencia;

    @Column(length = 40)
    private String ordenWebpay;

    @OneToMany(mappedBy = "boleta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleBoleta> detalles = new ArrayList<>();

    public void agregarDetalle(DetalleBoleta d) {
        d.setBoleta(this);
        this.detalles.add(d);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public int getSubtotal() { return subtotal; }
    public void setSubtotal(int subtotal) { this.subtotal = subtotal; }
    public int getDespacho() { return despacho; }
    public void setDespacho(int despacho) { this.despacho = despacho; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public EstadoBoleta getEstado() { return estado; }
    public void setEstado(EstadoBoleta estado) { this.estado = estado; }
    public String getNombreEnvio() { return nombreEnvio; }
    public void setNombreEnvio(String nombreEnvio) { this.nombreEnvio = nombreEnvio; }
    public String getCorreoEnvio() { return correoEnvio; }
    public void setCorreoEnvio(String correoEnvio) { this.correoEnvio = correoEnvio; }
    public String getTelefonoEnvio() { return telefonoEnvio; }
    public void setTelefonoEnvio(String telefonoEnvio) { this.telefonoEnvio = telefonoEnvio; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getComuna() { return comuna; }
    public void setComuna(String comuna) { this.comuna = comuna; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }
    public String getOrdenWebpay() { return ordenWebpay; }
    public void setOrdenWebpay(String ordenWebpay) { this.ordenWebpay = ordenWebpay; }
    public List<DetalleBoleta> getDetalles() { return detalles; }
    public void setDetalles(List<DetalleBoleta> detalles) { this.detalles = detalles; }
}
