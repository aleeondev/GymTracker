// ==========================================================
// GRAFICA-MINI.JS  (Progreso → lista de ejercicios)
// Mini-gráficas ("sparklines"): una línea pequeña por ejercicio con la
// forma de su evolución (1RM estimado de las últimas 10 sesiones).
// Sin ejes, números ni tooltip: solo la tendencia de un vistazo.
// Los datos van en data-puntos de cada <canvas> (ver progreso.html).
// ==========================================================

document.addEventListener("DOMContentLoaded", () => {
    const { COLORES } = GtGraficas;

    document.querySelectorAll("canvas.mini-grafica").forEach((lienzo) => {
        const puntos = lienzo.dataset.puntos.split(",").map(Number).slice(-10);
        const ultimo = puntos.length - 1;

        new Chart(lienzo, {
            type: "line",
            data: {
                labels: puntos.map((_, i) => i),
                datasets: [{
                    data: puntos,
                    borderColor: COLORES.principal,
                    borderWidth: 2,
                    tension: 0.35,
                    // Solo se marca el último punto: "dónde estás ahora"
                    pointRadius: puntos.map((_, i) => (i === ultimo ? 3 : 0)),
                    pointBackgroundColor: COLORES.principal
                }]
            },
            options: {
                animation: false,
                events: [],                      // no reacciona al ratón: es decorativa
                layout: { padding: 4 },          // que el punto final no se corte
                scales: { x: { display: false }, y: { display: false } },
                plugins: { tooltip: { enabled: false } }
            }
        });
    });
});
