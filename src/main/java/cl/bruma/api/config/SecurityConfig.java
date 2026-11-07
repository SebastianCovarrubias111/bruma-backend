package cl.bruma.api.config;

import cl.bruma.api.security.JwtAuthFilter;
import cl.bruma.api.security.RestAccessDeniedHandler;
import cl.bruma.api.security.RestAuthEntryPoint;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Reglas de acceso de la API (Roles: ADMIN, VENDEDOR, CLIENTE).
 * El orden importa: Spring evalúa las reglas de arriba hacia abajo y aplica la primera que coincide.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtFilter,
                                           RestAuthEntryPoint entryPoint, RestAccessDeniedHandler deniedHandler)
            throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // API stateless con JWT en header: no usa cookies de sesión
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint).accessDeniedHandler(deniedHandler))
            .authorizeHttpRequests(a -> a
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/error").permitAll()
                // Documentación Swagger
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                // Autenticación
                .requestMatchers("/api/v1/auth/me").authenticated()
                .requestMatchers("/api/v1/auth/**").permitAll()
                // Endpoint de servidor a servidor (valida su propia llave interna)
                .requestMatchers("/api/v1/interno/**").permitAll()
                // Catálogo: lectura pública, escritura solo ADMIN
                .requestMatchers(HttpMethod.GET, "/api/v1/productos/todos").hasAnyRole("ADMIN", "VENDEDOR")
                .requestMatchers(HttpMethod.GET, "/api/v1/productos", "/api/v1/productos/**",
                        "/api/v1/categorias", "/api/v1/categorias/**").permitAll()
                .requestMatchers("/api/v1/productos/**", "/api/v1/categorias/**").hasRole("ADMIN")
                // Usuarios: solo ADMIN (excepto el perfil propio)
                .requestMatchers("/api/v1/usuarios/me").authenticated()
                .requestMatchers("/api/v1/usuarios/**").hasRole("ADMIN")
                // Órdenes (boletas)
                .requestMatchers(HttpMethod.GET, "/api/v1/boletas/mias").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/v1/boletas").hasAnyRole("CLIENTE", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/boletas").hasAnyRole("ADMIN", "VENDEDOR")
                .requestMatchers(HttpMethod.GET, "/api/v1/boletas/*").authenticated() // el servicio valida que sea suya
                .requestMatchers(HttpMethod.POST, "/api/v1/boletas/*/cancelar").authenticated() // el servicio valida que sea suya
                .requestMatchers("/api/v1/boletas/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins}") String origenes) {
        CorsConfiguration cfg = new CorsConfiguration();
        List<String> lista = Arrays.stream(origenes.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        cfg.setAllowedOrigins(lista);
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/**", cfg);
        return fuente;
    }
}
