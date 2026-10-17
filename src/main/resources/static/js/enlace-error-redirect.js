// ==========================================================
// ENLACE-ERROR-REDIRECT.JS
// Cuando un enlace de un email de Supabase (recuperar contraseña,
// confirmar cuenta...) ha caducado o no es válido, Supabase nos devuelve
// a nuestra web con el error tras el "#":
//   /#error=access_denied&error_code=otp_expired&error_description=...
// Este script lo detecta y lleva al usuario a nuestra página /enlace-error,
// que explica qué ha pasado y qué hacer.
//
// Se carga en index.html, login.html y nueva-contrasena.html: las páginas
// a las que Supabase puede devolvernos. Va el PRIMERO de los scripts para
// actuar antes que nada.
// ==========================================================

(function () {
    const datos = new URLSearchParams(window.location.hash.slice(1));
    if (datos.has("error") || datos.has("error_code")) {
        // replace (y no href) para que "atrás" no vuelva a la URL con el error
        window.location.replace("/enlace-error" + window.location.hash);
    }
})();
