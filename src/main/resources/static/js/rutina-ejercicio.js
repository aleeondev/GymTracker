// ==========================================================
// RUTINA-EJERCICIO.JS
// Filtra el desplegable de ejercicios según el grupo muscular elegido.
// (Antes estaba dentro del HTML; lo sacamos a un archivo porque la
// política de seguridad CSP no permite scripts escritos en la página.)
// ==========================================================

document.addEventListener("DOMContentLoaded", () => {
    const grupo = document.getElementById("grupoMuscular");
    const ejercicio = document.getElementById("ejercicio");
    if (!grupo || !ejercicio) return;

    function filtrar() {
        const elegido = grupo.value;
        for (const opcion of ejercicio.options) {
            if (!opcion.value) continue; // la opción "Selecciona..."
            const visible = !elegido || opcion.dataset.grupo === elegido;
            opcion.hidden = !visible;
            opcion.disabled = !visible;
        }
        const seleccionada = ejercicio.selectedOptions[0];
        if (seleccionada && seleccionada.disabled) {
            ejercicio.value = "";
        }
    }

    // Al editar, empezamos mostrando el grupo del ejercicio ya elegido.
    const actual = ejercicio.selectedOptions[0];
    if (actual && actual.value && actual.dataset.grupo) {
        grupo.value = actual.dataset.grupo;
    }

    grupo.addEventListener("change", filtrar);
    filtrar();
});