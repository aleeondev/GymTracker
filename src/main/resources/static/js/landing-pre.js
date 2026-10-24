// ==========================================================
// LANDING-PRE.JS
// Se carga en el <head> de la landing, ANTES de que se pinte nada.
// Añade la clase "lp-anim" al <html>: con ella, inicio.css esconde lo que
// GSAP va a animar al entrar. Así no se ve el contenido un instante y luego
// "salta" a su animación.
//
// Si la persona tiene activado "reducir movimiento" en su sistema, no se
// añade: la página se ve completa y quieta desde el principio.
// Red de seguridad: si landing.js no llegara a ejecutarse (error, GSAP no
// cargado...), inicio.css vuelve a mostrar todo a los 2,5 segundos.
// ==========================================================

if (!window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
    document.documentElement.classList.add("lp-anim");
}
