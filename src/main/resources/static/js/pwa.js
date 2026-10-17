// ==========================================================
// PWA.JS
// Registra el service worker (sw.js) para que Gym Tracker se pueda
// instalar en el móvil como una app.
// Solo funciona en "contexto seguro": https o http://localhost.
// ==========================================================

if ("serviceWorker" in navigator) {
    window.addEventListener("load", () => {
        navigator.serviceWorker.register("/sw.js").catch((error) => {
            console.warn("No se pudo registrar el service worker", error);
        });
    });
}
