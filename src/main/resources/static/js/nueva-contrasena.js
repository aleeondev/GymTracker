// ==========================================================
// NUEVA-CONTRASENA.JS  (página /nueva-contrasena)
// Paso 2: el usuario llega desde el enlace del email.
//
// Cómo funciona el enlace: Supabase lo comprueba y redirige aquí con los
// datos de una sesión temporal al final de la URL, tras el "#":
//   /nueva-contrasena#access_token=...&type=recovery
// La librería de Supabase lee esa parte sola y abre la sesión. Con esa
// sesión podemos cambiar la contraseña (updateUser).
// Si el enlace ha caducado o ya se usó, llega "#error=..." y lo gestiona
// la página /enlace-error (ver enlace-error-redirect.js).
// ==========================================================

const formulario = document.getElementById("nuevaForm");
const subtitulo = document.getElementById("subtitulo");
const mensaje = document.getElementById("mensaje");
const submitBtn = document.getElementById("submitBtn");
const enlaceNuevo = document.getElementById("enlaceNuevo");

// Leemos el "#..." ahora, antes de que Supabase lo borre de la barra de direcciones.
const datosEnlace = new URLSearchParams(window.location.hash.slice(1));
let vieneDelEmail = datosEnlace.get("type") === "recovery";

// Por si Supabase procesa el enlace antes que nosotros, también escuchamos su aviso.
supabaseClient.auth.onAuthStateChange((evento) => {
    if (evento === "PASSWORD_RECOVERY") vieneDelEmail = true;
});

function mostrarMensaje(texto, tipo) {
    mensaje.textContent = texto;
    mensaje.classList.remove("is-error", "is-success");
    if (tipo) mensaje.classList.add(tipo);
}

/** El enlace no sirve: lo explicamos y ofrecemos pedir otro. */
function enlaceNoValido(texto) {
    subtitulo.textContent = texto;
    enlaceNuevo.hidden = false;
}

(async function comprobarEnlace() {
    // Si el enlace trae un error (caducado...), enlace-error-redirect.js ya
    // nos ha mandado a /enlace-error antes de llegar aquí.

    // getSession espera a que Supabase termine de leer el enlace.
    const session = await sesionActualizada();
    limpiarUrl();

    // Seguridad: solo dejamos cambiar la contraseña si se llega desde el email.
    // Así, alguien que encuentre tu sesión abierta no puede cambiarla desde aquí.
    if (!session || !vieneDelEmail) {
        enlaceNoValido("Para cambiar tu contraseña, abre esta página desde el enlace que te enviamos por email.");
        return;
    }

    subtitulo.textContent = "Elige una contraseña nueva para tu cuenta.";
    formulario.hidden = false;
    document.getElementById("password").focus();
})();

/** Quita el "#access_token=..." de la URL para que no quede en el historial. */
function limpiarUrl() {
    if (window.location.hash) {
        history.replaceState(null, "", window.location.pathname);
    }
}

formulario.addEventListener("submit", async (evento) => {
    evento.preventDefault();

    const password = document.getElementById("password").value;
    const repetida = document.getElementById("password2").value;

    if (password.length < 8) {
        mostrarMensaje("La contraseña debe tener al menos 8 caracteres.", "is-error");
        return;
    }
    if (password !== repetida) {
        mostrarMensaje("Las dos contraseñas no coinciden.", "is-error");
        return;
    }

    mostrarMensaje("Guardando...");
    submitBtn.classList.add("is-loading");
    submitBtn.disabled = true;

    const { error } = await supabaseClient.auth.updateUser({ password });

    if (error) {
        mostrarMensaje(traducirError(error), "is-error");
        submitBtn.classList.remove("is-loading");
        submitBtn.disabled = false;
        return;
    }

    await sesionActualizada();
    formulario.hidden = true;
    subtitulo.textContent = "¡Listo! Tu contraseña se ha cambiado.";
    mostrarMensaje("Entrando en tu panel...", "is-success");
    setTimeout(() => window.location.replace("/dashboard"), 1500);
});

/** Mensajes de Supabase (en inglés) a español. */
function traducirError(error) {
    const codigo = error.code || "";
    const texto = (error.message || "").toLowerCase();
    if (codigo === "same_password" || texto.includes("different from the old")) return "La nueva contraseña tiene que ser distinta de la anterior.";
    if (codigo === "weak_password" || texto.includes("weak")) return "Esa contraseña es demasiado débil. Prueba con una más larga o con números y símbolos.";
    if (texto.includes("session") || error.status === 401) return "El enlace ha caducado. Pide uno nuevo desde «¿Has olvidado tu contraseña?».";
    if (texto.includes("fetch")) return "No hay conexión con el servidor. Revisa tu internet.";
    return "No se ha podido cambiar la contraseña: " + error.message;
}
