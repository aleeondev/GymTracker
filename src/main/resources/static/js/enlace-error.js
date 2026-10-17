// ==========================================================
// ENLACE-ERROR.JS  (página /enlace-error)
// Lee el código de error que Supabase puso tras el "#" y muestra una
// explicación en español.
// ==========================================================

const datos = new URLSearchParams(window.location.hash.slice(1));
const codigo = datos.get("error_code") || datos.get("error") || "";

// Mensajes según el código. Para añadir uno nuevo, añade una entrada aquí.
const MENSAJES = {
    otp_expired: {
        titulo: "Enlace caducado",
        texto: "Este enlace ha caducado o ya se ha usado. Por seguridad, los enlaces de los emails solo sirven una vez y durante 1 hora."
    },
    access_denied: {
        titulo: "Enlace no válido",
        texto: "No hemos podido comprobar este enlace. Puede que esté incompleto o que ya no sea válido."
    }
};

const mensaje = MENSAJES[codigo] || MENSAJES.access_denied;
document.getElementById("titulo").textContent = mensaje.titulo;
document.getElementById("texto").textContent = mensaje.texto;

// Quitamos el "#error=..." de la barra de direcciones (queda más limpio).
if (window.location.hash) {
    history.replaceState(null, "", window.location.pathname);
}
