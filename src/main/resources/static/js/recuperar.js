// ==========================================================
// RECUPERAR.JS  (página /recuperar)
// Paso 1: pide a Supabase que envíe el email con el enlace de recuperación.
// El enlace lleva al usuario a /nueva-contrasena (nueva-contrasena.js).
// ==========================================================

const formulario = document.getElementById("recuperarForm");
const mensaje = document.getElementById("mensaje");
const submitBtn = document.getElementById("submitBtn");

// Supabase no deja pedir otro email para la misma dirección hasta pasados 60 s.
const ESPERA_SEGUNDOS = 60;

function mostrarMensaje(texto, tipo) {
    mensaje.textContent = texto;
    mensaje.classList.remove("is-error", "is-success");
    if (tipo) mensaje.classList.add(tipo);
}

formulario.addEventListener("submit", async (evento) => {
    evento.preventDefault();

    const email = document.getElementById("email").value.trim();
    mostrarMensaje("Enviando...");
    submitBtn.classList.add("is-loading");
    submitBtn.disabled = true;

    const { error } = await supabaseClient.auth.resetPasswordForEmail(email, {
        // A dónde vuelve el usuario al pulsar el enlace del email.
        // Esta dirección tiene que estar permitida en Supabase (Redirect URLs).
        redirectTo: window.location.origin + "/nueva-contrasena"
    });

    submitBtn.classList.remove("is-loading");

    if (error && (error.status === 429 || (error.message || "").toLowerCase().includes("rate limit"))) {
        mostrarMensaje("Has pedido demasiados enlaces. Espera unos minutos y vuelve a probar.", "is-error");
        bloquearBoton();
        return;
    }
    if (error && (error.message || "").toLowerCase().includes("fetch")) {
        mostrarMensaje("No hay conexión con el servidor. Revisa tu internet.", "is-error");
        submitBtn.disabled = false;
        return;
    }

    // Seguridad: damos el mismo mensaje exista o no la cuenta, para que nadie
    // pueda usar esta página para averiguar qué emails están registrados.
    mostrarMensaje("Si existe una cuenta con ese email, te hemos enviado un enlace. Revisa tu bandeja de entrada (y la de spam).", "is-success");
    bloquearBoton();
});

/** Desactiva el botón con una cuenta atrás para no pedir emails seguidos. */
function bloquearBoton() {
    let restantes = ESPERA_SEGUNDOS;
    const textoOriginal = "Enviar enlace";
    submitBtn.disabled = true;
    submitBtn.textContent = `Reenviar en ${restantes} s`;

    const intervalo = setInterval(() => {
        restantes--;
        if (restantes <= 0) {
            clearInterval(intervalo);
            submitBtn.disabled = false;
            submitBtn.textContent = textoOriginal;
            return;
        }
        submitBtn.textContent = `Reenviar en ${restantes} s`;
    }, 1000);
}
