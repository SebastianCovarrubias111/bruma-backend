package cl.bruma.api.config;

import cl.bruma.api.model.Categoria;
import cl.bruma.api.model.Producto;
import cl.bruma.api.model.Rol;
import cl.bruma.api.model.Usuario;
import cl.bruma.api.repository.CategoriaRepository;
import cl.bruma.api.repository.ProductoRepository;
import cl.bruma.api.repository.UsuarioRepository;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Carga datos de ejemplo la primera vez que arranca con la BD vacía:
 * categorías, las 8 cervezas del catálogo y un usuario por cada rol.
 * Las contraseñas son SOLO para demostración: cámbialas antes de publicar.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final CategoriaRepository categorias;
    private final ProductoRepository productos;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;

    public DataInitializer(CategoriaRepository categorias, ProductoRepository productos,
                           UsuarioRepository usuarios, PasswordEncoder encoder) {
        this.categorias = categorias;
        this.productos = productos;
        this.usuarios = usuarios;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (categorias.count() == 0) {
            cargarCatalogo();
        }
        if (usuarios.count() == 0) {
            cargarUsuarios();
        }
    }

    private void cargarCatalogo() {
        Map<String, Categoria> cat = new LinkedHashMap<>();
        String[][] categoriasBase = {
            {"Ale", "Cervezas de fermentación alta: frutadas y aromáticas"},
            {"IPA", "India Pale Ale: lúpulo intenso y amargor marcado"},
            {"Stout", "Cervezas oscuras con notas de café y cacao"},
            {"Lager", "Fermentación baja: doradas, limpias y refrescantes"},
            {"Trigo", "Cervezas de trigo: suaves y especiadas"}};
        for (String[] c : categoriasBase) {
            Categoria nueva = new Categoria();
            nueva.setNombre(c[0]);
            nueva.setDescripcion(c[1]);
            cat.put(c[0], categorias.save(nueva));
        }

        Object[][] cervezas = {
            {"BRU001", "Bruma Costera", "Pale Ale", "Ale", 5.2, 32, "Botella 330 ml", 5990,
                "Pale Ale ligera con notas cítricas de lúpulo Cascade y final seco, pensada para el atardecer frente al mar."},
            {"BRU002", "Niebla Negra", "Stout", "Stout", 6.8, 40, "Botella 330 ml", 6490,
                "Stout cremosa con café tostado y cacao amargo, madurada en barrica. La favorita de las noches de lluvia."},
            {"BRU003", "Volcánica IPA", "IPA", "IPA", 7.1, 61, "Botella 330 ml", 6990,
                "India Pale Ale intensa en lúpulo, con aromas a pino y fruta tropical. Amargor firme y memorable."},
            {"BRU004", "Rubia del Lago", "Lager", "Lager", 4.6, 18, "Botella 330 ml", 5490,
                "Lager dorada, liviana y refrescante, fermentada en frío durante seis semanas para máxima limpieza."},
            {"BRU005", "Ámbar de Turbera", "Amber Ale", "Ale", 5.6, 28, "Botella 330 ml", 6190,
                "Ale ámbar con maltas caramelo y un leve toque ahumado que recuerda a las turberas del sur."},
            {"BRU006", "Doble Lúpulo", "Double IPA", "IPA", 8.4, 74, "Botella 330 ml", 7490,
                "Double IPA robusta con dry-hopping triple de variedades del hemisferio sur, para paladares que buscan intensidad."},
            {"BRU007", "Trigo del Estuario", "Witbier", "Trigo", 4.9, 14, "Botella 330 ml", 5790,
                "Cerveza de trigo turbia con cáscara de naranja y cilantro. Suave, especiada y muy fácil de beber."},
            {"BRU008", "Imperial de Roble", "Imperial Stout", "Stout", 9.5, 55, "Botella 500 ml", 8990,
                "Imperial Stout envejecida en barricas de roble ex-whisky. Edición limitada de temporada fría."}};
        for (Object[] c : cervezas) {
            Producto p = new Producto();
            p.setCodigo((String) c[0]);
            p.setNombre((String) c[1]);
            p.setEstilo((String) c[2]);
            p.setCategoria(cat.get((String) c[3]));
            p.setAbv((Double) c[4]);
            p.setIbu((Integer) c[5]);
            p.setPresentacion((String) c[6]);
            p.setPrecio((Integer) c[7]);
            p.setDescripcion((String) c[8]);
            p.setStock(50);
            p.setActivo(true);
            productos.save(p);
        }
        log.info("Catálogo de ejemplo cargado: {} categorías, {} productos", categorias.count(), productos.count());
    }

    private void cargarUsuarios() {
        crearUsuario("Administrador Bruma", "admin@bruma.cl", "Admin1234", Rol.ADMIN);
        crearUsuario("Vendedor Bruma", "vendedor@bruma.cl", "Vendedor1234", Rol.VENDEDOR);
        crearUsuario("Cliente Demo", "cliente@bruma.cl", "Cliente1234", Rol.CLIENTE);
        log.info("Usuarios de ejemplo creados: admin@bruma.cl, vendedor@bruma.cl, cliente@bruma.cl");
    }

    private void crearUsuario(String nombre, String correo, String clave, Rol rol) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setCorreo(correo);
        u.setClave(encoder.encode(clave));
        u.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        u.setRol(rol);
        u.setActivo(true);
        usuarios.save(u);
    }
}
