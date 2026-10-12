// ==========================================================
// AUTH-UI.JS
// Comportamiento visual compartido entre login.html y registro.html.
// No toca Supabase ni el envío de formularios: eso sigue en
// login.js / registro.js.
// ==========================================================

document.addEventListener("DOMContentLoaded", () => {
  initPasswordToggles();
});

function initPasswordToggles() {
  document.querySelectorAll(".toggle-password").forEach((button) => {
    const input = document.getElementById(button.dataset.target);
    if (!input) return;

    button.addEventListener("click", () => {
      const showing = input.type === "password";

      input.type = showing ? "text" : "password";
      button.classList.toggle("is-visible", showing);
      button.setAttribute(
        "aria-label",
        showing ? "Ocultar contraseña" : "Mostrar contraseña"
      );
    });
  });
}