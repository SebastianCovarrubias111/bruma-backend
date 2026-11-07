# Cervecería Bruma — Backend (Spring Boot)

API REST de la tienda online **Cervecería Bruma** (Evaluación 3, DSY1104 Desarrollo Fullstack II).
Spring Boot 3.3 · Java 17 · Spring Data JPA · Spring Security + JWT · MySQL · Swagger (springdoc).

Frontend: https://github.com/TU-USUARIO/cerveceria-bruma

## Requisitos
- JDK 17 o superior
- Maven 3.9+ (o el wrapper si lo agregas)
- MySQL 8 (opcional: existe un perfil `h2` en memoria para probar sin instalar nada)

## Cómo ejecutar

### Opción A — MySQL
1. Tener MySQL corriendo en `localhost:3306`. La base `bruma` se crea sola.
2. Configurar usuario/clave (por defecto `root` sin clave) con variables de entorno:
   ```bash
   export DB_USER=root
   export DB_PASSWORD=tu_clave
   ```
   En Windows (PowerShell): `$env:DB_USER="root"; $env:DB_PASSWORD="tu_clave"`
3. Ejecutar:
   ```bash
   mvn spring-boot:run
   ```

### Opción B — Sin MySQL (H2 en memoria)
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```
Los datos se pierden al detener la app.

La API queda en **http://localhost:8080** y Swagger en **http://localhost:8080/swagger-ui.html**.

## Usuarios de ejemplo (se crean al primer arranque)
| Rol | Correo | Contraseña |
|---|---|---|
| ADMIN | admin@bruma.cl | Admin1234 |
| VENDEDOR | vendedor@bruma.cl | Vendedor1234 |
| CLIENTE | cliente@bruma.cl | Cliente1234 |

> Son solo para demostración. Cámbialas o desactiva la carga con `SEED_ENABLED=false` antes de publicar.

## Probar la seguridad en Swagger
1. `POST /api/v1/auth/login` con un usuario de la tabla → copiar el `accessToken`.
2. Botón **Authorize** (arriba a la derecha) → pegar el token → Authorize.
3. Probar endpoints. Con CLIENTE, `GET /api/v1/boletas` responde 403; con ADMIN responde 200.

## Roles y permisos
| Recurso | Público | CLIENTE | VENDEDOR | ADMIN |
|---|:-:|:-:|:-:|:-:|
| Ver catálogo (`GET /productos`, `/categorias`) | ✔ | ✔ | ✔ | ✔ |
| Crear/editar/eliminar productos y categorías | | | | ✔ |
| Crear una orden (`POST /boletas`) | | ✔ | | ✔ |
| Ver mis órdenes / el detalle de una propia | | ✔ | ✔ | ✔ |
| Ver todas las órdenes y su detalle | | | ✔ | ✔ |
| Cambiar estado / eliminar órdenes | | | | ✔ |
| Gestionar usuarios y roles | | | | ✔ |

## Modelo de datos
`USUARIO 1—N BOLETA 1—N DETALLE_BOLETA N—1 PRODUCTO N—1 CATEGORIA` (+ `REFRESH_TOKEN` para renovar sesiones).

## Variables de entorno
| Variable | Descripción | Por defecto |
|---|---|---|
| `DB_URL` | URL JDBC | MySQL local, base `bruma` |
| `DB_USER` / `DB_PASSWORD` | Credenciales de BD | `root` / vacío |
| `JWT_SECRET` | Llave de firma JWT (mín. 32 caracteres) | valor de desarrollo |
| `JWT_ACCESS_MINUTES` | Vida del access token | 30 |
| `JWT_REFRESH_DAYS` | Vida del refresh token | 7 |
| `CORS_ORIGINS` | Orígenes del frontend (coma) | localhost 3000/5500/5173 |
| `INTERNAL_KEY` | Llave para que el servidor Webpay confirme pagos | vacía (deshabilitado) |
| `SEED_ENABLED` | Cargar datos de ejemplo | true |

## Pruebas
```bash
mvn test
```
- `BoletaServiceTest`: lógica de negocio (totales, despacho, stock, anulación, permisos) con Mockito.
- `SeguridadIntegracionTest`: login, JWT, refresh, roles (401/403/200), registro y órdenes con MockMvc + H2.

## Estructura
```
src/main/java/cl/bruma/api
├── config/       SecurityConfig, OpenApiConfig, DataInitializer
├── controller/   Auth, Producto, Categoria, Usuario, Boleta, IntegracionPago
├── dto/          Requests/Responses (records con validaciones)
├── exception/    Excepciones propias + GlobalExceptionHandler
├── model/        Entidades JPA y enums
├── repository/   Spring Data JPA
├── security/     JwtService, JwtAuthFilter, handlers 401/403
└── service/      Lógica de negocio
```
