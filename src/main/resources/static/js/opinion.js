// ==========================================================
// OPINION.JS
// Botón "Opinar" de la cabecera (fragments/app.html):
//  - abre el diálogo y apunta desde qué página se abre;
//  - envía la opinión con fetch a /opinion (OpinionController), sin recargar
//    la página: si estás a mitad de un entreno, no pierdes nada;
//  - enseña el "¡Gracias!" o el error que devuelve el servidor.
// El token CSRF viaja dentro del formulario (campo oculto _csrf que pone Thymeleaf).
// ==========================================================

document.addEventListener("DOMContentLoaded", () => {
    const dialogo = document.getElementById("dialogo-opinion");
    if (!dialogo) return;

    const form = dialogo.querySelector("form");
    const mensaje = dialogo.querySelector(".opinion-mensaje");
    const boton = form.querySelector('button[type="submit"]');

    const mostrar = (texto, tipo) => {
        mensaje.textContent = texto;
        mensaje.classList.remove("is-error", "is-success");
        if (tipo) mensaje.classList.add(tipo);
    };

    document.querySelectorAll("[data-abrir-opinion]").forEach((abrir) => {
        abrir.addEventListener("click", () => {
            form.elements.pagina.value = window.location.pathname;
            mostrar("");
            dialogo.showModal();
            form.elements.texto.focus();
        });
    });

    dialogo.querySelector("[data-cerrar-opinion]").addEventListener("click", () => dialogo.close());
    // Clic fuera de la ventana (en el fondo oscuro) = cerrar
    dialogo.addEventListener("click", (evento) => {
        if (evento.target === dialogo) dialogo.close();
    });

    form.addEventListener("submit", async (evento) => {
        evento.preventDefault();
        boton.classList.add("is-loading");
        boton.disabled = true;
        mostrar("");

        try {
            // Por si el token de sesión ha caducado (entrenos largos): lo renueva
            await sesionActualizada();
            const respuesta = await fetch(form.action, {
                method: "POST",
                body: new URLSearchParams(new FormData(form)),
                headers: { "Accept": "application/json" }
            });
            const datos = await respuesta.json().catch(() => ({}));

            if (respuesta.ok && datos.ok) {
                form.reset();
                mostrar(datos.ok, "is-success");
                setTimeout(() => dialogo.close(), 1800);
            } else {
                mostrar(datos.error || "No se ha podido enviar. Recarga la página y vuelve a probar.", "is-error");
            }
        } catch (e) {
            mostrar("No hay conexión con el servidor. Revisa tu internet.", "is-error");
        } finally {
            boton.classList.remove("is-loading");
            boton.disabled = false;
        }
    });
});
