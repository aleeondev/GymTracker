// ==========================================================
// APP.JS
// Comportamiento común a todas las páginas de la app.
// ==========================================================

document.addEventListener("DOMContentLoaded", () => {
    initCerrarSesion();
    initFormulariosPost();
    initCabeceraScroll();
    limpiarBorradorGuardado();
});

// Botón "Salir"
function initCerrarSesion() {
    document.querySelectorAll("[data-cerrar-sesion]").forEach((boton) => {
        boton.addEventListener("click", async () => {
            boton.disabled = true;
            await cerrarSesion();
        });
    });
}

// Antes de enviar cualquier formulario POST:
//  1) si tiene data-confirmar, pide confirmación (para borrar cosas);
//  2) se asegura de que el token de sesión está al día. Importante en el
//     gimnasio: un entrenamiento dura más de 1 hora y el token caduca.
function initFormulariosPost() {
    document.querySelectorAll('form[method="post"]').forEach((form) => {
        form.addEventListener("submit", async (evento) => {
            if (form.dataset.enviando === "1") {
                return; // segunda vuelta: ya revisado, se envía de verdad
            }
            evento.preventDefault();

            const mensaje = form.dataset.confirmar;
            if (mensaje && !window.confirm(mensaje)) {
                return;
            }

            const boton = form.querySelector('button[type="submit"]');
            if (boton) {
                boton.classList.add("is-loading");
                boton.disabled = true;
            }

            try {
                await sesionActualizada();
            } catch (e) {
                console.warn("No se pudo renovar la sesión", e);
            }

            form.dataset.enviando = "1";
            form.dispatchEvent(new Event("antes-de-enviar"));
            form.submit();
        });
    });
}

// Sombra en la cabecera al hacer scroll (igual que en la landing)
function initCabeceraScroll() {
    const header = document.querySelector(".app-header");
    if (!header) return;
    const alHacerScroll = () => header.classList.toggle("scrolled", window.scrollY > 10);
    alHacerScroll();
    window.addEventListener("scroll", alHacerScroll, { passive: true });
}
// Tras guardar un entrenamiento, el servidor redirige a /historial/{id}?guardado.
// Solo entonces borramos el borrador del navegador (si hubo un error, se conserva).
function limpiarBorradorGuardado() {
    if (!new URLSearchParams(window.location.search).has("guardado")) return;
    try {
        const clave = sessionStorage.getItem("gt_borrador_pendiente");
        if (clave) {
            localStorage.removeItem(clave);
            sessionStorage.removeItem("gt_borrador_pendiente");
        }
    } catch (e) {
        /* sin almacenamiento disponible */
    }
}
