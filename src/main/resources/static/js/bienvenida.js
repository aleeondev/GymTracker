// ==========================================================
// BIENVENIDA.JS  (página /bienvenida)
// Convierte el formulario en 3 pasos: enseña uno cada vez, mueve la barra
// de progreso y comprueba cada paso antes de dejar avanzar.
// También muestra el "peso objetivo" solo con objetivos de cambiar de peso.
// ==========================================================

document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("form-bienvenida");
    if (!form) return;

    const pasos = [...form.querySelectorAll(".paso")];
    const barra = document.getElementById("barra-pasos");
    const texto = document.getElementById("texto-paso");
    const atras = document.getElementById("paso-atras");
    const siguiente = document.getElementById("paso-siguiente");
    const guardar = document.getElementById("paso-guardar");
    let actual = 0;

    function mostrarPaso(indice, desplazar = true) {
        actual = indice;
        pasos.forEach((paso, i) => { paso.hidden = i !== indice; });
        barra.value = indice + 1;
        texto.textContent = `Paso ${indice + 1} de ${pasos.length}`;
        atras.hidden = indice === 0;
        siguiente.hidden = indice === pasos.length - 1;
        guardar.hidden = indice !== pasos.length - 1;
        if (desplazar) form.scrollIntoView({ behavior: "smooth", block: "start" });
    }

    /** Usa la validación del navegador (required, min, max...) de los campos de un paso. */
    function pasoValido(indice) {
        for (const campo of pasos[indice].querySelectorAll("input, textarea")) {
            if (!campo.checkValidity()) {
                campo.reportValidity(); // el navegador enseña el aviso junto al campo
                return false;
            }
        }
        return true;
    }

    siguiente.addEventListener("click", () => {
        if (pasoValido(actual)) mostrarPaso(actual + 1);
    });
    atras.addEventListener("click", () => mostrarPaso(actual - 1));

    // Al enviar (fase de captura: antes que app.js) revisamos todos los pasos
    // y, si alguno falla, saltamos a él.
    form.addEventListener("submit", (evento) => {
        if (form.dataset.enviando === "1") return;
        for (let i = 0; i < pasos.length; i++) {
            if (!pasoValido(i)) {
                evento.preventDefault();
                evento.stopImmediatePropagation();
                mostrarPaso(i);
                pasoValido(i);
                return;
            }
        }
    }, true);

    // ---------- Peso objetivo: solo si el objetivo es cambiar de peso ----------
    const bloquePeso = document.getElementById("bloque-peso-objetivo");
    const objetivosConPeso = bloquePeso.dataset.objetivos.split(",");

    function actualizarPesoObjetivo() {
        const elegido = form.querySelector('input[name="objetivo"]:checked');
        const visible = elegido != null && objetivosConPeso.includes(elegido.value);
        bloquePeso.hidden = !visible;
        if (!visible) document.getElementById("pesoObjetivoKg").value = "";
    }
    form.querySelectorAll('input[name="objetivo"]').forEach((radio) => radio.addEventListener("change", actualizarPesoObjetivo));
    actualizarPesoObjetivo();

    // Fecha de nacimiento: como máximo hace 12 años (el servidor también lo comprueba)
    const fecha = document.getElementById("fechaNacimiento");
    const limite = new Date();
    limite.setFullYear(limite.getFullYear() - 12);
    fecha.max = limite.toISOString().slice(0, 10);

    mostrarPaso(0, false);
});
