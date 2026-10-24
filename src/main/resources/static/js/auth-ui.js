// ==========================================================
// AUTH-UI.JS
// Comportamiento visual compartido por las pantallas de acceso
// (login, registro, recuperar, nueva contraseña, enlace caducado).
// No toca Supabase ni el envío de formularios: eso sigue en
// login.js / registro.js / recuperar.js / nueva-contrasena.js.
// ==========================================================

document.addEventListener("DOMContentLoaded", () => {
  initPasswordToggles();
  initSaludo();
  initLogros();
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

// "Buenos días / Buenas tardes / Buenas noches" según la hora del móvil
// (solo en las páginas que tienen el hueco: <p data-saludo>).
function initSaludo() {
  const saludo = document.querySelector("[data-saludo]");
  if (!saludo) return;

  const hora = new Date().getHours();
  if (hora >= 6 && hora < 13) saludo.textContent = "Buenos días";
  else if (hora >= 13 && hora < 21) saludo.textContent = "Buenas tardes";
  else saludo.textContent = "Buenas noches";
}

// Tarjetitas de logros del panel rojo (fragments/auth.html): cada 3,5 s
// se quita "activo" a la actual y se le pone a la siguiente; la transición
// (aparecer subiendo) la hace auth.css. Con "reducir movimiento" se queda quieta.
function initLogros() {
  const logros = document.querySelectorAll(".auth-logro");
  const quieto = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  if (logros.length < 2 || quieto) return;

  let actual = 0;
  setInterval(() => {
    logros[actual].classList.remove("activo");
    actual = (actual + 1) % logros.length;
    logros[actual].classList.add("activo");
  }, 3500);
}
