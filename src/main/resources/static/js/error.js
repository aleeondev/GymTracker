// ==========================================================
// ERROR.JS  (páginas de error)
// Botones "Reintentar" (recarga la página) y "Atrás" (solo si hay
// una página anterior a la que volver).
// ==========================================================

document.querySelectorAll("[data-recargar]").forEach((boton) => {
    boton.addEventListener("click", () => window.location.reload());
});

document.querySelectorAll("[data-atras]").forEach((boton) => {
    if (window.history.length > 1) {
        boton.hidden = false;
        boton.addEventListener("click", () => window.history.back());
    }
});
