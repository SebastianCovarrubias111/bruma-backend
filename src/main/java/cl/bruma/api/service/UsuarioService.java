package cl.bruma.api.service;

import cl.bruma.api.dto.UsuarioDtos.UsuarioRequest;
import cl.bruma.api.dto.UsuarioDtos.UsuarioResponse;
import cl.bruma.api.exception.ConflictoException;
import cl.bruma.api.exception.RecursoNoEncontradoException;
import cl.bruma.api.exception.SolicitudInvalidaException;
import cl.bruma.api.model.Usuario;
import cl.bruma.api.repository.BoletaRepository;
import cl.bruma.api.repository.UsuarioRepository;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarios;
    private final BoletaRepository boletas;
    private final PasswordEncoder encoder;

    public UsuarioService(UsuarioRepository usuarios, BoletaRepository boletas, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.boletas = boletas;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarios.findAll().stream().map(UsuarioService::aDto).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Long id) {
        return aDto(buscar(id));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse perfil(String correo) {
        return aDto(usuarios.findByCorreo(correo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado.")));
    }

    @Transactional
    public UsuarioResponse crear(UsuarioRequest req) {
        if (req.clave() == null || req.clave().isBlank()) {
            throw new SolicitudInvalidaException("La contraseña es obligatoria al crear un usuario.");
        }
        String correo = req.correo().trim().toLowerCase();
        if (usuarios.existsByCorreo(correo)) {
            throw new ConflictoException("Ese correo ya tiene una cuenta.");
        }
        Usuario u = new Usuario();
        u.setCorreo(correo);
        u.setClave(encoder.encode(req.clave()));
        aplicar(u, req);
        return aDto(usuarios.save(u));
    }

    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioRequest req, String correoQuienEdita) {
        Usuario u = buscar(id);
        String correo = req.correo().trim().toLowerCase();
        usuarios.findByCorreo(correo).ifPresent(otro -> {
            if (!otro.getId().equals(id)) {
                throw new ConflictoException("Ese correo ya tiene una cuenta.");
            }
        });
        // Evita que un administrador se quite a sí mismo el acceso y deje el sistema sin admin
        if (u.getCorreo().equals(correoQuienEdita)
                && (req.rol() != u.getRol() || Boolean.FALSE.equals(req.activo()))) {
            throw new SolicitudInvalidaException("No puedes cambiar tu propio rol ni desactivar tu propia cuenta.");
        }
        u.setCorreo(correo);
        if (req.clave() != null && !req.clave().isBlank()) {
            u.setClave(encoder.encode(req.clave()));
        }
        aplicar(u, req);
        return aDto(usuarios.save(u));
    }

    /** Baja lógica: se conserva el usuario para no romper el historial de boletas. */
    @Transactional
    public void eliminar(Long id, String correoQuienElimina) {
        Usuario u = buscar(id);
        if (u.getCorreo().equals(correoQuienElimina)) {
            throw new SolicitudInvalidaException("No puedes eliminar tu propia cuenta.");
        }
        if (boletas.existsByUsuarioId(id)) {
            u.setActivo(false);
            usuarios.save(u);
        } else {
            usuarios.delete(u);
        }
    }

    private void aplicar(Usuario u, UsuarioRequest req) {
        u.setNombre(req.nombre().trim());
        u.setFechaNacimiento(req.fechaNacimiento());
        u.setRol(req.rol());
        u.setActivo(req.activo() == null || req.activo());
    }

    private Usuario buscar(Long id) {
        return usuarios.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario " + id + " no encontrado."));
    }

    static UsuarioResponse aDto(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNombre(), u.getCorreo(), u.getFechaNacimiento(), u.getRol(),
                u.isActivo());
    }
}
