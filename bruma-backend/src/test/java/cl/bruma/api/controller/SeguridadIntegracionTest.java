package cl.bruma.api.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Pruebas de integración: levantan la aplicación completa (con H2) y verifican autenticación JWT,
 * roles y respuestas de la API usando peticiones HTTP simuladas.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SeguridadIntegracionTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private JsonNode login(String correo, String clave) throws Exception {
        String cuerpo = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correo + "\",\"clave\":\"" + clave + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(cuerpo);
    }

    private String token(String correo, String clave) throws Exception {
        return login(correo, clave).get("accessToken").asText();
    }

    @Test
    void catalogoEsPublico() throws Exception {
        mvc.perform(get("/api/v1/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(8)));
    }

    @Test
    void loginCorrectoEntregaTokensYRol() throws Exception {
        JsonNode r = login("admin@bruma.cl", "Admin1234");
        org.junit.jupiter.api.Assertions.assertFalse(r.get("accessToken").asText().isBlank());
        org.junit.jupiter.api.Assertions.assertFalse(r.get("refreshToken").asText().isBlank());
        org.junit.jupiter.api.Assertions.assertEquals("ADMIN", r.get("usuario").get("rol").asText());
    }

    @Test
    void loginConClaveIncorrectaDevuelve401() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"admin@bruma.cl\",\"clave\":\"incorrecta1\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sinTokenUnRecursoProtegidoDevuelve401() throws Exception {
        mvc.perform(get("/api/v1/boletas")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/usuarios")).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenInvalidoDevuelve401() throws Exception {
        mvc.perform(get("/api/v1/boletas").header("Authorization", "Bearer esto.no.es.un.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void clienteNoPuedeVerTodasLasBoletasNiGestionarProductos() throws Exception {
        String t = token("cliente@bruma.cl", "Cliente1234");
        mvc.perform(get("/api/v1/boletas").header("Authorization", "Bearer " + t)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/usuarios").header("Authorization", "Bearer " + t)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/productos/1").header("Authorization", "Bearer " + t)).andExpect(status().isForbidden());
    }

    @Test
    void vendedorVeProductosYOrdenesPeroNoPuedeModificar() throws Exception {
        String t = token("vendedor@bruma.cl", "Vendedor1234");
        mvc.perform(get("/api/v1/productos/todos").header("Authorization", "Bearer " + t)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/boletas").header("Authorization", "Bearer " + t)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/usuarios").header("Authorization", "Bearer " + t)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/productos/1").header("Authorization", "Bearer " + t)).andExpect(status().isForbidden());
    }

    @Test
    void adminPuedeListarUsuarios() throws Exception {
        String t = token("admin@bruma.cl", "Admin1234");
        mvc.perform(get("/api/v1/usuarios").header("Authorization", "Bearer " + t))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(3)));
    }

    @Test
    void meDevuelveElUsuarioDelToken() throws Exception {
        String t = token("cliente@bruma.cl", "Cliente1234");
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + t))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo", is("cliente@bruma.cl")))
                .andExpect(jsonPath("$.rol", is("CLIENTE")));
    }

    @Test
    void refreshRotaElTokenYElAnteriorQuedaInvalido() throws Exception {
        String refresh = login("cliente@bruma.cl", "Cliente1234").get("refreshToken").asText();

        mvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());

        // El mismo refresh token no se puede usar dos veces
        mvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registroCreaClienteYRechazaMenoresDeEdad() throws Exception {
        mvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana Pérez\",\"correo\":\"ana.test@correo.cl\",\"clave\":\"Clave1234\",\"fechaNacimiento\":\"1995-05-20\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario.rol", is("CLIENTE")));

        mvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Menor Edad\",\"correo\":\"menor.test@correo.cl\",\"clave\":\"Clave1234\",\"fechaNacimiento\":\"2015-01-01\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void clienteCreaUnaOrdenYSoloEseClienteLaPuedeVer() throws Exception {
        String t = token("cliente@bruma.cl", "Cliente1234");
        String orden = "{\"items\":[{\"codigo\":\"BRU001\",\"cantidad\":2}],"
                + "\"envio\":{\"nombre\":\"Cliente Demo\",\"correo\":\"cliente@bruma.cl\",\"telefono\":\"+56911112222\","
                + "\"region\":\"Los Ríos\",\"comuna\":\"Valdivia\",\"direccion\":\"Calle Falsa 123\"}}";

        String respuesta = mvc.perform(post("/api/v1/boletas")
                        .header("Authorization", "Bearer " + t)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orden))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subtotal", is(11980)))
                .andExpect(jsonPath("$.despacho", is(3500)))
                .andExpect(jsonPath("$.total", is(15480)))
                .andExpect(jsonPath("$.estado", is("PENDIENTE")))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(respuesta).get("id").asLong();

        mvc.perform(get("/api/v1/boletas/" + id).header("Authorization", "Bearer " + t)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/boletas/mias").header("Authorization", "Bearer " + t)).andExpect(status().isOk());

        // Otro cliente no puede ver la orden
        mvc.perform(post("/api/v1/auth/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Otro Cliente\",\"correo\":\"otro.test@correo.cl\",\"clave\":\"Clave1234\",\"fechaNacimiento\":\"1990-01-01\"}"));
        String otro = token("otro.test@correo.cl", "Clave1234");
        mvc.perform(get("/api/v1/boletas/" + id).header("Authorization", "Bearer " + otro)).andExpect(status().isForbidden());
    }

    @Test
    void validacionDevuelve400ConElDetalleDeLosCampos() throws Exception {
        String t = token("admin@bruma.cl", "Admin1234");
        mvc.perform(post("/api/v1/productos")
                        .header("Authorization", "Bearer " + t)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"\",\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos").exists());
    }

    @Test
    void productoInexistenteDevuelve404() throws Exception {
        mvc.perform(get("/api/v1/productos/99999")).andExpect(status().isNotFound());
    }

    @Test
    void endpointInternoRequiereLlave() throws Exception {
        mvc.perform(post("/api/v1/interno/boletas/1/pagada")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/interno/boletas/1/pagada").header("X-Internal-Key", "mala"))
                .andExpect(status().isForbidden());
    }
}
