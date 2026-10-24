# Gym Tracker — guía del proyecto para Claude

App web para registrar entrenamientos de gimnasio y ver el progreso. Uso personal y de un grupo reducido.
Idioma de la app, del código (nombres) y de las explicaciones: **español**.

## Stack
- Spring Boot 4.1 · Spring Security 7 · Hibernate 7 · Thymeleaf 3.1 · Lombok · Jackson 3 (`tools.jackson`)
- Postgres en Supabase (pooler, **máx. 15 conexiones**) · Login con Supabase Auth (supabase-js en el navegador)
- Chart.js 4.5.0 para gráficas · GSAP 3.13.0 (+ ScrollTrigger, SplitText) para animar la landing · Librerías de terceros copiadas en `static/js/vendor/` (nunca CDN: lo impide la CSP)
- App instalable (PWA): `static/manifest.webmanifest`, `static/sw.js`, `static/offline.html`

## Cómo funciona el login
Supabase da un JWT en el navegador → `auth-session.js` lo copia a la cookie `gt_session` →
`config/SupabaseCookieFilter` lo verifica (ES256, emisor, audiencia, caducidad) → `UsuarioActual.id()` = UUID del usuario.
Sesiones STATELESS. CSRF con `CookieCsrfTokenRepository` (sin rotar: `NullAuthenticatedSessionStrategy`).

## Estructura (`src/main/java/com/gymtracker/gymtracker/`)
- `config/`: SecurityConfig (rutas públicas, CSP, cabeceras), SupabaseCookieFilter, UsuarioActual, Formato (`${@fmt...}` en vistas), InicializacionBaseDatos (activa RLS en `TABLAS` y siembra la biblioteca de ejercicios)
- `model/` · `repository/` · `service/` (lógica) · `dto/` (formularios y datos de vistas) · `controller/`
- `controller/DatosComunesAdvice`: datos de todas las vistas (usuarioNombre, rutaActual, `area`, opcionesPerfil)
- `templates/fragments/app.html` (cabecera, navegación por áreas, barra móvil, avisos, scripts) · `fragments/iconos.html` (iconos SVG) · `fragments/auth.html` (panel rojo de marca de las 5 pantallas de acceso, estilos en `auth.css`, logros en `auth-ui.js`) · `fragments/error.html` + `templates/error/*.html`
- Botón "Opinar" (alpha): botón en la cabecera + `<dialog>` en el fragmento `scripts` de `app.html`, `js/opinion.js` (fetch, sin recargar), `OpinionController` (JSON) → tabla `opiniones`. Formularios con `data-sin-recarga` los ignora `app.js`
- Eliminar mi cuenta (Mi perfil, sección 04): `perfil.js` → `CuentaController` (JSON) → `CuentaService` (borra nuestras tablas en una transacción) + `SupabaseAdmin` (borra auth.users con `supabase.secret-key`, variable `SUPABASE_SECRET_KEY`; en tests es un `@MockitoBean`)
- Privacidad `/privacidad` (pública): `LegalController`, `templates/privacidad.html`, `css/legal.css`. Responsable y email en `app.responsable.nombre` / `app.contacto.email`. Si se añade un servicio externo o un dato nuevo, actualizar la página (y su fecha)
- `static/css/app.css` (dividido en secciones con título) · `static/js/` (un JS por pantalla; `graficas.js` = estilo común de Chart.js)

## Reglas del proyecto (respetarlas siempre)
- **Seguridad de datos:** toda consulta filtra por `UsuarioActual.id()`; un id ajeno → 404. Formularios con DTO o `@InitBinder(setAllowedFields)`. Validar todo en el servidor.
- **Tabla nueva:** añadirla a `TABLAS` de `InicializacionBaseDatos` (RLS) y, si tiene datos de usuario, a `BORRADOS` de `CuentaService` ("Eliminar mi cuenta") y a `/privacidad`. Ruta pública nueva: a `RUTAS_PUBLICAS` de `SecurityConfig`.
- **CSP estricta:** nada de `<script>` ni `style="..."` dentro del HTML; JS en archivos propios; nunca `innerHTML` con datos del usuario.
- **Enlaces a CSS/JS siempre con Thymeleaf** (`th:href="@{/css/...}"`, `th:src`): llevan huella automática de caché. No usar `?v=`.
- Sin emojis en la interfaz: usar `fragments/iconos.html`. Los atributos `hidden` siempre ocultan (regla global en styles.css).
- La landing (`templates/index.html`, `css/inicio.css`, `js/landing.js` + `landing-pre.js`) se rehízo el 24/10 con GSAP: no tocar salvo petición expresa. Clases con prefijo `lp-`; lo que GSAP revela con transform usa `translate` en el hover.
- Redirecciones con datos del usuario: solo rutas internas que pasen la lista blanca (`EjercicioController.rutaInternaValida`).

## Forma de trabajar con el usuario
- Pasos pequeños; explicar en español, archivo por archivo, qué se ha hecho y dónde tocar después.
- **El usuario hace los commits y los push**: dar los comandos, no ejecutarlos.
- Ramas: una rama por día/sesión desde `main`; `main` = producción, se fusiona tras pruebas.
- Verificación mínima (compilar + tests) y terminar con una lista "Pruébalo tú" para el usuario.

## Comandos
- Arrancar: desde VS Code (perfil `local`, credenciales en `application-local.properties`, ignorado por git), puerto 8081.
- Tests: `./mvnw -q test -Dtest='PaginasErrorTests,PaginasPublicasTests,SesionCsrfTests,FlujoUsuarioTests'` (usan la BD real con rollback y pool de 2 conexiones).
  `GymtrackerApplicationTests` falla porque no usa el perfil `local` (pendiente de arreglar).
- Publicación: Render (plan gratis, región Frankfurt) con el `Dockerfile` (build sin tests, perfil `prod` = `application-prod.properties`: puerto `PORT`, cabeceras de proxy HTTPS, pool de 5). Variables en Render: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `SUPABASE_SECRET_KEY`. Correos de Supabase por SMTP de Brevo (remitente gymtraacker.app@gmail.com).
- En Windows, escribir archivos con PowerShell `[IO.File]::WriteAllText` (UTF-8 sin BOM) o Python; ojo con las barras invertidas en heredocs de Git Bash.

## Pendiente / ideas
- Pruebas de seguridad manuales del usuario (lista en la conversación del 19/10).
- Antes de publicar: rol de BD con permisos mínimos (no `postgres`), límite de peticiones, poner mínimo 8 en Supabase (la web ya pide 8), decidir si cerrar el registro público.
- Futuro: Nutrición y Coach IA (ahora páginas "Próximamente"), publicar en Render con HTTPS, plan gratis + suscripción.