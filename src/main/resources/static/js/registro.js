// ==========================================================
// REGISTRO.JS
// ==========================================================

const formulario = document.getElementById("registroForm");
const mensaje = document.getElementById("mensaje");
const submitBtn = document.getElementById("submitBtn");

function mostrarMensaje(texto, tipo) {
    mensaje.textContent = texto;
    mensaje.classList.remove("is-error", "is-success");
    if (tipo) mensaje.classList.add(tipo);
}

formulario.addEventListener("submit", async function (event) {
    event.preventDefault();

    const nombre = document.getElementById("nombre").value.trim();
    const email = document.getElementById("email").value.trim();
    const password = document.getElementById("password").value;

    if (password.length < 6) {
        mostrarMensaje("La contraseña debe tener al menos 6 caracteres.", "is-error");
        return;
    }

    mostrarMensaje("Creando cuenta...");
    submitBtn.classList.add("is-loading");
    submitBtn.disabled = true;

    const { data, error } = await supabaseClient.auth.signUp({
        email,
        password,
        options: {
            // "nombre" lo usa el trigger de Supabase para crear el perfil
            // y Spring para saludarte en el panel.
            data: { nombre },
            // Al pulsar el enlace del email de confirmación, vuelve al login.
            emailRedirectTo: window.location.origin + "/login"
        }
    });

    if (error) {
        mostrarMensaje(traducirError(error), "is-error");
        submitBtn.classList.remove("is-loading");
        submitBtn.disabled = false;
        return;
    }

    // Si Supabase no pide confirmar el email, ya hay sesión: directo al panel.
    if (data.session) {
        guardarCookieSesion(data.session);
        mostrarMensaje("Cuenta creada. Entrando...", "is-success");
        window.location.href = "/dashboard";
        return;
    }

    // Si pide confirmación, avisamos (antes se decía "redirigiendo" y el login fallaba).
    formulario.reset();
    submitBtn.classList.remove("is-loading");
    mostrarMensaje("¡Cuenta creada! Te hemos enviado un email para confirmarla. Cuando lo confirmes, inicia sesión.", "is-success");
});

function traducirError(error) {
    const texto = (error.message || "").toLowerCase();
    if (texto.includes("already registered") || texto.includes("already been registered")) return "Ya existe una cuenta con ese email. Inicia sesión.";
    if (texto.includes("password")) return "La contraseña no es válida: usa al menos 6 caracteres.";
    if (texto.includes("invalid") && texto.includes("email")) return "El email no es válido.";
    if (texto.includes("rate limit") || error.status === 429) return "Demasiados intentos. Espera un momento y vuelve a probar.";
    return "No se ha podido crear la cuenta: " + error.message;
}