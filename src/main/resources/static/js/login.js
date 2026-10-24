// ==========================================================
// LOGIN.JS
// ==========================================================

const formulario = document.getElementById("loginForm");
const mensaje = document.getElementById("mensaje");
const submitBtn = document.getElementById("submitBtn");
const parametros = new URLSearchParams(window.location.search);

/** Página a la que volver tras iniciar sesión (solo rutas de nuestra web). */
function destino() {
    const siguiente = parametros.get("next");
    // Solo rutas de nuestra web: "/" + letras, números y / _ - ? = & (sin "//").
    // Así no cuela "/<tabulador>/web-falsa.com", que el navegador convertiría en "//web-falsa.com".
    if (siguiente && /^\/(?!\/)[A-Za-z0-9\/_?=&-]*$/.test(siguiente)) {
        return siguiente;
    }
    return "/dashboard";
}

function mostrarMensaje(texto, tipo) {
    mensaje.textContent = texto;
    mensaje.classList.remove("is-error", "is-success");
    if (tipo) mensaje.classList.add(tipo);
}

// Venimos de "Eliminar mi cuenta" (perfil.js)
if (parametros.has("eliminada")) {
    mostrarMensaje("Tu cuenta y todos tus datos se han eliminado. ¡Gracias por probar Gym Tracker!", "is-success");
}

// Si ya hay una sesión de Supabase (por ejemplo, el servidor nos mandó aquí
// porque el token había caducado), la renovamos y volvemos solos.
// El contador evita un bucle infinito si el servidor la rechazara.
(async function sesionExistente() {
    const session = await sesionActualizada();
    if (!session) return;

    const ultimo = Number(sessionStorage.getItem("gt_auto_login") || 0);
    if (Date.now() - ultimo < 5000) {
        await supabaseClient.auth.signOut();
        guardarCookieSesion(null);
        mostrarMensaje("Tu sesión ha caducado. Vuelve a iniciar sesión.", "is-error");
        return;
    }
    sessionStorage.setItem("gt_auto_login", String(Date.now()));
    mostrarMensaje("Ya tienes una sesión abierta. Entrando...", "is-success");
    window.location.replace(destino());
})();

formulario.addEventListener("submit", async function (event) {
    event.preventDefault();

    const email = document.getElementById("email").value.trim();
    const password = document.getElementById("password").value;

    mostrarMensaje("Iniciando sesión...");
    submitBtn.classList.add("is-loading");
    submitBtn.disabled = true;

    const { data, error } = await supabaseClient.auth.signInWithPassword({ email, password });

    if (error) {
        mostrarMensaje(traducirError(error), "is-error");
        submitBtn.classList.remove("is-loading");
        submitBtn.disabled = false;
        return;
    }

    guardarCookieSesion(data.session);
    sessionStorage.removeItem("gt_auto_login");
    mostrarMensaje("Inicio de sesión correcto.", "is-success");
    window.location.href = destino();
});

/** Mensajes de Supabase (en inglés) a español. */
function traducirError(error) {
    const texto = (error.message || "").toLowerCase();
    if (texto.includes("invalid login credentials")) return "Email o contraseña incorrectos.";
    if (texto.includes("email not confirmed")) return "Aún no has confirmado tu email. Revisa tu bandeja de entrada (y la de spam).";
    if (texto.includes("rate limit") || error.status === 429) return "Demasiados intentos. Espera un momento y vuelve a probar.";
    if (texto.includes("fetch")) return "No hay conexión con el servidor. Revisa tu internet.";
    return "No se ha podido iniciar sesión: " + error.message;
}