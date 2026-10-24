// ==========================================================
// REGISTRO.JS
// - Comprueba mientras escribes si el nombre de usuario está libre
//   (pregunta a /api/usuario-disponible, en PerfilController).
// - Indica la fortaleza de la contraseña y que las dos coincidan.
// - Crea la cuenta en Supabase. El nombre, el @usuario y la fecha del
//   consentimiento viajan como "user_metadata"; al entrar por primera vez,
//   Spring los copia a nuestra tabla "perfiles" (PerfilService.asegurarPerfil).
// ==========================================================

const formulario = document.getElementById("registroForm");
const mensaje = document.getElementById("mensaje");
const submitBtn = document.getElementById("submitBtn");
const campoUsuario = document.getElementById("usuario");
const estadoUsuario = document.getElementById("estado-usuario");
const campoPassword = document.getElementById("password");
const fuerza = document.getElementById("fuerza");
const fuerzaTexto = document.getElementById("fuerza-texto");

const FORMATO_USUARIO = /^[a-z0-9_]{3,20}$/;
let usuarioLibre = false;
let temporizador = null;

function mostrarMensaje(texto, tipo) {
    mensaje.textContent = texto;
    mensaje.classList.remove("is-error", "is-success");
    if (tipo) mensaje.classList.add(tipo);
}

function estado(texto, tipo) {
    estadoUsuario.textContent = texto;
    estadoUsuario.classList.remove("is-error", "is-success");
    if (tipo) estadoUsuario.classList.add(tipo);
}

/** "@Alex_Gym " -> "alex_gym" (igual que PerfilService.normalizarUsuario) */
function normalizar(texto) {
    return texto.trim().replace(/^@/, "").toLowerCase();
}

// ---------- ¿Nombre de usuario libre? ----------
async function comprobarUsuario() {
    const nombre = normalizar(campoUsuario.value);
    usuarioLibre = false;
    if (!FORMATO_USUARIO.test(nombre)) {
        estado(nombre ? "De 3 a 20 caracteres: solo letras, números y \"_\"." : "De 3 a 20 caracteres: letras, números y \"_\".", nombre ? "is-error" : null);
        return false;
    }
    try {
        const respuesta = await fetch("/api/usuario-disponible?u=" + encodeURIComponent(nombre));
        const { estado: resultado } = await respuesta.json();
        if (normalizar(campoUsuario.value) !== nombre) return false; // ha seguido escribiendo
        usuarioLibre = resultado === "libre";
        estado(usuarioLibre ? `✓ @${nombre} está libre` : `✗ @${nombre} ya está cogido`, usuarioLibre ? "is-success" : "is-error");
    } catch (e) {
        // Sin conexión con el servidor: dejamos seguir (se vuelve a comprobar al entrar)
        usuarioLibre = true;
        estado("");
    }
    return usuarioLibre;
}

// Espera a que deje de escribir 400 ms antes de preguntar al servidor
campoUsuario.addEventListener("input", () => {
    clearTimeout(temporizador);
    usuarioLibre = false;
    temporizador = setTimeout(comprobarUsuario, 400);
});

// ---------- Fortaleza de la contraseña ----------
function nivelContrasena(password) {
    if (!password) return 0;
    const tipos = [/[a-z]/, /[A-Z]/, /[0-9]/, /[^A-Za-z0-9]/].filter((r) => r.test(password)).length;
    if (password.length >= 10 && tipos >= 3) return 3;
    if (password.length >= 8 && tipos >= 2) return 2;
    return 1;
}

campoPassword.addEventListener("input", () => {
    const nivel = nivelContrasena(campoPassword.value);
    fuerza.dataset.nivel = String(nivel);
    fuerzaTexto.textContent = ["", "Débil: usa 8 o más caracteres y mezcla letras y números", "Media: añade mayúsculas o símbolos para hacerla fuerte", "Fuerte"][nivel];
});

// ---------- Crear la cuenta ----------
formulario.addEventListener("submit", async function (event) {
    event.preventDefault();

    const nombre = document.getElementById("nombre").value.trim();
    const usuario = normalizar(campoUsuario.value);
    const email = document.getElementById("email").value.trim();
    const password = campoPassword.value;
    const repetida = document.getElementById("password2").value;

    if (!(await comprobarUsuario())) {
        mostrarMensaje("Elige un nombre de usuario válido y libre.", "is-error");
        campoUsuario.focus();
        return;
    }
    if (password.length < 8) {
        mostrarMensaje("La contraseña debe tener al menos 8 caracteres.", "is-error");
        return;
    }
    if (password !== repetida) {
        mostrarMensaje("Las dos contraseñas no coinciden.", "is-error");
        return;
    }
    if (!document.getElementById("acepto").checked) {
        mostrarMensaje("Para crear la cuenta tienes que aceptar cómo guardamos tus datos.", "is-error");
        return;
    }

    mostrarMensaje("Creando cuenta...");
    submitBtn.classList.add("is-loading");
    submitBtn.disabled = true;

    const { data, error } = await supabaseClient.auth.signUp({
        email,
        password,
        options: {
            // user_metadata: Spring lo lee del token (UsuarioActual.nombre / metadato)
            data: { nombre, nombre_usuario: usuario, consentimiento: new Date().toISOString() },
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

    // Si Supabase no pide confirmar el email, ya hay sesión: directo al panel
    // (que, al ser la primera vez, te lleva al cuestionario de bienvenida).
    if (data.session) {
        guardarCookieSesion(data.session);
        mostrarMensaje("Cuenta creada. Entrando...", "is-success");
        window.location.href = "/dashboard";
        return;
    }

    formulario.reset();
    fuerza.dataset.nivel = "0";
    fuerzaTexto.textContent = "";
    estado("De 3 a 20 caracteres: letras, números y \"_\".");
    submitBtn.classList.remove("is-loading");
    mostrarMensaje("¡Cuenta creada! Te hemos enviado un email para confirmarla. Cuando lo confirmes, inicia sesión.", "is-success");
});

function traducirError(error) {
    const texto = (error.message || "").toLowerCase();
    if (texto.includes("already registered") || texto.includes("already been registered")) return "Ya existe una cuenta con ese email. Inicia sesión.";
    if (texto.includes("password")) return "La contraseña no es válida: usa al menos 8 caracteres.";
    if (texto.includes("invalid") && texto.includes("email")) return "El email no es válido.";
    if (texto.includes("rate limit") || error.status === 429) return "Demasiados intentos. Espera un momento y vuelve a probar.";
    return "No se ha podido crear la cuenta: " + error.message;
}
