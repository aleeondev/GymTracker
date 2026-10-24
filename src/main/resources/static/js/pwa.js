// ==========================================================
// PWA.JS
// Registra el service worker (sw.js) para que Gym Tracker se pueda
// instalar en el móvil como una app.
// Solo funciona en "contexto seguro": https o http://localhost.
// ==========================================================

// Botón "Instalar" propio (landing): Chrome/Edge en Android y ordenador lanzan
// el evento "beforeinstallprompt" cuando la app se puede instalar. Lo guardamos,
// enseñamos nuestro botón y, al pulsarlo, abrimos su ventana de instalación.
// Safari (iPhone) no tiene este evento: allí el botón no aparece nunca y se
// siguen las instrucciones escritas en la landing.
let avisoInstalacion = null;

window.addEventListener("beforeinstallprompt", (evento) => {
    evento.preventDefault();
    avisoInstalacion = evento;
    document.querySelectorAll("[data-instalar]").forEach((boton) => { boton.hidden = false; });
});

document.addEventListener("click", async (evento) => {
    const boton = evento.target.closest("[data-instalar]");
    if (!boton || !avisoInstalacion) return;
    avisoInstalacion.prompt();
    await avisoInstalacion.userChoice;
    // El aviso solo se puede usar una vez
    avisoInstalacion = null;
    boton.hidden = true;
});

// Ya instalada: fuera el botón
window.addEventListener("appinstalled", () => {
    document.querySelectorAll("[data-instalar]").forEach((boton) => { boton.hidden = true; });
});

if ("serviceWorker" in navigator) {
    window.addEventListener("load", () => {
        navigator.serviceWorker.register("/sw.js").catch((error) => {
            console.warn("No se pudo registrar el service worker", error);
        });
    });
}
