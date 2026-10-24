# Gym Tracker — guía del proyecto para Claude

App web para registrar entrenamientos de gimnasio y ver el progreso. Uso personal y de un grupo reducido.
Idioma de la app, del código (nombres) y de las explicaciones: **español**.

## Stack
- Spring Boot 4.1 · Spring Security 7 · Hibernate 7 · Thymeleaf 3.1 · Lombok · Jackson 3 (`tools.jackson`)
- Postgres en Supabase (pooler, **máx. 15 conexiones**) · Login con Supabase Auth (supabase-js en el navegador)
- Chart.js 4.5.0 para gráficas · Librerías de terceros copiadas en `static/js/vendor/` (nunca CDN: lo impide la CSP)
- App instalable (PWA): `static/manifest.webmanifest`, `static/sw.js`, `static/offline.html`

## Cómo funciona el login
Supabase da un JWT en el navegador → `auth-session.js` lo copia a la cookie `gt_session` →
`config/SupabaseCookieFilter` lo verifica (ES256, emisor, audiencia, caducidad) → `UsuarioActual.id()` = UUID del usuario.
Sesiones STATELESS. CSRF con `CookieCsrfTokenRepository` (sin rotar: `NullAuthenticatedSessionStrategy`).

## Estructura (`src/main/java/com/gymtracker/gymtracker/`)
- `config/`: SecurityConfig (rutas públicas, CSP, cabeceras), SupabaseCookieFilter, UsuarioActual, Formato (`${@fmt...}` en vistas), InicializacionBaseDatos (activa RLS en `TABLAS` y siembra la biblioteca de ejercicios)
- `model/` · `repository/` · `service/` (lógica) · `dto/` (formularios y datos de vistas) · `controller/`
- `controller/DatosComunesAdvice`: datos de todas las vistas (usuarioNombre, rutaActual, `area`, opcionesPerfil)
- `templates/fragments/app.html` (cabecera, navegación por áreas, barra móvil, avisos, scripts) · `fragments/iconos.html` (iconos SVG) · `fragments/error.html` + `templates/error/*.html`
- Web de prelanzamiento `/pronto` (pública): `PrelanzamientoController`, `templates/pronto.html` + `pronto-baja.html`, `css/pronto.css` (diseño oscuro propio), `js/pronto.js` (intro, animaciones, formulario) y `js/reto.js` (Fitness Challenge). Lista de espera en la tabla `interesados` (`InteresadoService`, límite por IP en `config/LimitePeticiones`, baja con token en `/pronto/baja`)
- `static/css/app.css` (dividido en secciones con título) · `static/js/` (un JS por pantalla; `graficas.js` = estilo común de Chart.js)

## Reglas del proyecto (respetarlas siempre)
- **Seguridad de datos:** toda consulta filtra por `UsuarioActual.id()`; un id ajeno → 404. Formularios con DTO o `@InitBinder(setAllowedFields)`. Validar todo en el servidor.
- **Tabla nueva:** añadirla a `TABLAS` de `InicializacionBaseDatos` (RLS). Ruta pública nueva: a `RUTAS_PUBLICAS` de `SecurityConfig`.
- **CSP estricta:** nada de `<script>` ni `style="..."` dentro del HTML; JS en archivos propios; nunca `innerHTML` con datos del usuario.
- **Enlaces a CSS/JS siempre con Thymeleaf** (`th:href="@{/css/...}"`, `th:src`): llevan huella automática de caché. No usar `?v=`.
- Sin emojis en la interfaz: usar `fragments/iconos.html`. Los atributos `hidden` siempre ocultan (regla global en styles.css).
- La landing (`templates/index.html`, `css/inicio.css`) está cerrada: no tocar salvo petición expresa.
- Redirecciones con datos del usuario: solo rutas internas que pasen la lista blanca (`EjercicioController.rutaInternaValida`).

## Forma de trabajar con el usuario
- Pasos pequeños; explicar en español, archivo por archivo, qué se ha hecho y dónde tocar después.
- **El usuario hace los commits y los push**: dar los comandos, no ejecutarlos.
- Ramas: una rama por día/sesión desde `main`; `main` = producción, se fusiona tras pruebas.
- Verificación mínima (compilar + tests) y terminar con una lista "Pruébalo tú" para el usuario.

## Comandos
- Arrancar: desde VS Code (perfil `local`, credenciales en `application-local.properties`, ignorado por git), puerto 8081.
- Tests: `./mvnw -q test -Dtest='PaginasErrorTests,SesionCsrfTests,FlujoUsuarioTests'` (usan la BD real con rollback y pool de 2 conexiones).
  `GymtrackerApplicationTests` falla porque no usa el perfil `local` (pendiente de arreglar).
- En Windows, escribir archivos con PowerShell `[IO.File]::WriteAllText` (UTF-8 sin BOM) o Python; ojo con las barras invertidas en heredocs de Git Bash.

## Pendiente / ideas
- Pruebas de seguridad manuales del usuario (lista en la conversación del 19/10).
- Antes de publicar: rol de BD con permisos mínimos (no `postgres`), límite de peticiones, contraseña mínima 8, decidir si cerrar el registro público.
- Futuro: Nutrición y Coach IA (ahora páginas "Próximamente"), publicar en Render con HTTPS, plan gratis + suscripción.