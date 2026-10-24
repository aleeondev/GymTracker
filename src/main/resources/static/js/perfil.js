// ==========================================================
// PERFIL.JS  (página /perfil)
// Lo que se guarda en Supabase Auth se cambia desde aquí, con su librería:
//   1) Nombre visible  -> updateUser({ data: { nombre } })
//   2) Contraseña      -> comprueba la actual y luego updateUser({ password })
//   3) Cerrar sesión en todos los dispositivos -> signOut({ scope: "global" })
//   4) Eliminar mi cuenta -> POST a CuentaController (con fetch) y, si sale
//      bien, cerrar también la sesión de Supabase guardada en el navegador.
// El nombre de usuario (@) es un formulario normal que va a PerfilController.
// ==========================================================

document.addEventListener("DOMContentLoaded", () => {
    initNombre();
    initContrasena();
    initCerrarTodas();
    initEliminarCuenta();
});

function mostrar(elemento, texto, tipo) {
    elemento.textContent = texto;
    elemento.classList.remove("is-error", "is-success");
    if (tipo) elemento.classList.add(tipo);
}

/** Desactiva el botón mientras se guarda (y lo devuelve a su estado). */
function cargando(form, activo) {
    const boton = form.querySelector('button[type="submit"]');
    boton.disabled = activo;
    boton.classList.toggle("is-loading", activo);
}

// ---------- 1) Nombre visible ----------
function initNombre() {
    const form = document.getElementById("form-nombre");
    const mensaje = document.getElementById("mensaje-nombre");

    form.addEventListener("submit", async (evento) => {
        evento.preventDefault();
        const nombre = document.getElementById("nombre").value.trim();
        if (!nombre) {
            mostrar(mensaje, "Escribe tu nombre.", "is-error");
            return;
        }

        cargando(form, true);
        mostrar(mensaje, "Guardando...");
        const { error } = await supabaseClient.auth.updateUser({ data: { nombre } });
        if (error) {
            cargando(form, false);
            mostrar(mensaje, "No se ha podido guardar el nombre. Inténtalo de nuevo.", "is-error");
            return;
        }

        // El nombre viaja DENTRO del token de sesión: pedimos uno nuevo que ya
        // lo lleve, lo pasamos a la cookie y recargamos para que el servidor lo vea.
        const { data } = await supabaseClient.auth.refreshSession();
        guardarCookieSesion(data.session);
        window.location.replace("/perfil?nombre");
    });
}

// ---------- 2) Contraseña ----------
function initContrasena() {
    const form = document.getElementById("form-contrasena");
    const mensaje = document.getElementById("mensaje-contrasena");

    form.addEventListener("submit", async (evento) => {
        evento.preventDefault();
        const actual = document.getElementById("actual").value;
        const nueva = document.getElementById("nueva").value;
        const repetida = document.getElementById("repetida").value;

        if (!actual) return mostrar(mensaje, "Escribe tu contraseña actual.", "is-error");
        if (nueva.length < 8) return mostrar(mensaje, "La nueva contraseña debe tener al menos 8 caracteres.", "is-error");
        if (nueva !== repetida) return mostrar(mensaje, "Las dos contraseñas nuevas no coinciden.", "is-error");
        if (nueva === actual) return mostrar(mensaje, "La nueva contraseña tiene que ser distinta de la actual.", "is-error");

        cargando(form, true);
        mostrar(mensaje, "Comprobando...");

        // Seguridad: antes de cambiarla comprobamos la actual. Así, si alguien
        // encuentra tu sesión abierta, no puede cambiarte la contraseña.
        const comprobacion = await supabaseClient.auth.signInWithPassword({ email: form.dataset.email, password: actual });
        if (comprobacion.error) {
            cargando(form, false);
            const texto = (comprobacion.error.message || "").toLowerCase();
            mostrar(mensaje, texto.includes("rate limit") || comprobacion.error.status === 429
                ? "Demasiados intentos. Espera un momento y vuelve a probar."
                : "La contraseña actual no es correcta.", "is-error");
            return;
        }

        const { error } = await supabaseClient.auth.updateUser({ password: nueva });
        cargando(form, false);
        if (error) {
            const codigo = error.code || "";
            mostrar(mensaje, codigo === "weak_password"
                ? "Esa contraseña es demasiado débil. Prueba con una más larga o con números y símbolos."
                : "No se ha podido cambiar la contraseña: " + error.message, "is-error");
            return;
        }

        await sesionActualizada();
        form.reset();
        mostrar(mensaje, "¡Contraseña cambiada! La próxima vez entra con la nueva.", "is-success");
    });
}

// ---------- 3) Cerrar sesión en todos los dispositivos ----------
function initCerrarTodas() {
    const boton = document.getElementById("cerrar-todas");
    boton.addEventListener("click", async () => {
        if (!confirm("Se cerrará tu sesión en todos los dispositivos, también en este. ¿Continuar?")) return;
        boton.disabled = true;
        await supabaseClient.auth.signOut({ scope: "global" });
        guardarCookieSesion(null);
        window.location.href = "/login";
    });
}

// ---------- 4) Eliminar mi cuenta ----------
// El servidor borra los datos y la cuenta de Supabase. Aquí solo pedimos una
// última confirmación y, al terminar, limpiamos la sesión del navegador: si no,
// supabase-js seguiría guardando un token de una cuenta que ya no existe.
function initEliminarCuenta() {
    const form = document.getElementById("form-eliminar");
    const mensaje = document.getElementById("mensaje-eliminar");
    if (!form) return;

    form.addEventListener("submit", async (evento) => {
        evento.preventDefault();
        const palabra = form.elements.confirmacion.value.trim().toUpperCase();
        if (palabra !== "ELIMINAR") {
            mostrar(mensaje, "Para confirmar, escribe ELIMINAR en el campo.", "is-error");
            return;
        }
        if (!confirm("Última comprobación: se borrarán tu cuenta y TODOS tus datos para siempre. ¿Eliminar?")) return;

        cargando(form, true);
        mostrar(mensaje, "Eliminando...");
        try {
            await sesionActualizada(); // por si el token había caducado
            const respuesta = await fetch(form.action, {
                method: "POST",
                body: new URLSearchParams(new FormData(form)),
                headers: { "Accept": "application/json" }
            });
            const datos = await respuesta.json().catch(() => ({}));
            if (!respuesta.ok || !datos.ok) {
                cargando(form, false);
                mostrar(mensaje, datos.error || "No se ha podido eliminar la cuenta. Recarga la página y vuelve a probar.", "is-error");
                return;
            }
        } catch (e) {
            cargando(form, false);
            mostrar(mensaje, "No hay conexión con el servidor. Revisa tu internet.", "is-error");
            return;
        }

        // Cuenta borrada: fuera la sesión local (scope "local": la cuenta ya no existe en Supabase)
        try {
            await supabaseClient.auth.signOut({ scope: "local" });
        } catch (e) {
            /* da igual: la cuenta ya no existe */
        }
        guardarCookieSesion(null);
        window.location.replace("/login?eliminada");
    });
}
