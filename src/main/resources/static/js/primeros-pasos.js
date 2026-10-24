// ==========================================================
// PRIMEROS-PASOS.JS  (panel)
// Botón "Ocultar" de la tarjeta Primeros pasos: la esconde y lo recuerda en
// este navegador (localStorage). Es solo una comodidad: si el navegador no
// deja guardar (modo privado...), simplemente vuelve a salir la próxima vez.
// Cuando están los cuatro pasos hechos, el servidor ya ni la pinta.
// ==========================================================

(function () {
    const tarjeta = document.getElementById("primeros-pasos");
    if (!tarjeta) return;
    const CLAVE = "gt_primeros_pasos_oculto";

    try {
        if (localStorage.getItem(CLAVE) === "1") {
            tarjeta.hidden = true;
            return;
        }
    } catch (e) {
        /* sin almacenamiento: se muestra */
    }

    tarjeta.querySelector("[data-ocultar-pasos]").addEventListener("click", () => {
        tarjeta.hidden = true;
        try {
            localStorage.setItem(CLAVE, "1");
        } catch (e) {
            /* no se puede recordar: solo se oculta ahora */
        }
    });
})();
