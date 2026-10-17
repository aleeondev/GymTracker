// ==========================================================
// GRAFICA-EVOLUCION.JS  (Progreso → un ejercicio)
// Líneas con la evolución de un ejercicio sesión a sesión:
//   - 1RM estimado (rojo): la fuerza "teórica" a una repetición.
//   - Mejor peso del día (gris): el peso más alto que levantaste.
// Botones 1M / 3M / 6M / Todo para elegir el periodo.
// Datos: EntrenamientoController (progresoEjercicio → puntosJson).
// Sustituye a la gráfica SVG hecha a mano de la v1.0 (progreso.js).
// ==========================================================

document.addEventListener("DOMContentLoaded", () => {
    const lienzo = document.getElementById("grafica-evolucion");
    if (!lienzo) return;

    const { COLORES } = GtGraficas;
    const DIA = 24 * 60 * 60 * 1000;
    const kg = (valor) => `${String(Math.round(valor * 10) / 10).replace(".", ",")} kg`;

    // Cada sesión: fecha como número (milisegundos) para colocarla en el eje X
    // a su distancia real: dos sesiones separadas un mes quedan más lejos que
    // dos separadas una semana.
    const sesiones = GtGraficas.datos(lienzo).map((s) => ({
        ...s,
        tiempo: new Date(s.fecha + "T12:00:00").getTime()
    }));

    /** Fecha corta en español para el eje: "13 oct" */
    const fechaCorta = (tiempo) =>
        new Date(tiempo).toLocaleDateString("es-ES", { day: "numeric", month: "short" }).replace(".", "");

    const grafica = new Chart(lienzo, {
        type: "line",
        data: {
            datasets: [
                {
                    label: "1RM estimado",
                    data: [],
                    borderColor: COLORES.principal,
                    backgroundColor: "rgba(201, 47, 75, 0.08)",
                    fill: true,                 // zona sombreada bajo la línea
                    pointBackgroundColor: "#fff",
                    borderWidth: 2.5
                },
                {
                    label: "Mejor peso",
                    data: [],
                    borderColor: COLORES.secundario,
                    pointBackgroundColor: "#fff",
                    borderWidth: 2,
                    borderDash: [6, 4]          // discontinua: es la línea secundaria
                }
            ]
        },
        options: {
            elements: {
                line: { tension: 0.3 },        // curva suave entre puntos
                point: { radius: 4, hoverRadius: 6, borderWidth: 2 }
            },
            // Al pasar el dedo cerca de una sesión, el tooltip enseña sus dos valores
            interaction: { mode: "index", intersect: false },
            scales: {
                x: {
                    type: "linear",             // eje de tiempo real (sin librerías extra)
                    grid: { display: false },
                    border: { display: false },
                    ticks: { maxTicksLimit: 6, callback: (valor) => fechaCorta(valor) }
                },
                y: {
                    border: { display: false },
                    grid: { color: COLORES.rejilla },
                    ticks: { maxTicksLimit: 5, callback: (valor) => `${valor} kg` }
                }
            },
            plugins: {
                // Dos líneas = leyenda (con una sola no hace falta)
                legend: {
                    display: true,
                    position: "top",
                    align: "start",
                    labels: { usePointStyle: true, pointStyle: "line", boxWidth: 24, color: COLORES.texto }
                },
                tooltip: {
                    callbacks: {
                        title: (elementos) => elementos[0].raw.etiqueta,
                        label: (elemento) => {
                            const s = elemento.raw;
                            return elemento.datasetIndex === 0
                                ? `1RM estimado: ${kg(s.y)}`
                                : `Mejor peso: ${kg(s.y)} × ${s.reps}`;
                        }
                    }
                }
            }
        }
    });

    // ---------- Botones de periodo ----------
    const botones = document.querySelectorAll("[data-rango]");

    function mostrar(dias) {
        const ultima = sesiones[sesiones.length - 1].tiempo;
        const visibles = dias ? sesiones.filter((s) => s.tiempo >= ultima - dias * DIA) : sesiones;

        grafica.data.datasets[0].data = visibles.map((s) => ({ x: s.tiempo, y: s.unoRm, etiqueta: s.etiqueta }));
        grafica.data.datasets[1].data = visibles.map((s) => ({ x: s.tiempo, y: s.mejorPeso, reps: s.reps, etiqueta: s.etiqueta }));

        // Un poco de margen a los lados para que los puntos no queden pegados al borde
        const desde = visibles[0].tiempo;
        const hasta = visibles[visibles.length - 1].tiempo;
        const margen = Math.max((hasta - desde) * 0.04, DIA);
        grafica.options.scales.x.min = desde - margen;
        grafica.options.scales.x.max = hasta + margen;
        grafica.update();

        botones.forEach((b) => b.setAttribute("aria-pressed", String(Number(b.dataset.rango) === dias)));
    }

    botones.forEach((boton) => {
        boton.addEventListener("click", () => mostrar(Number(boton.dataset.rango)));
    });
    mostrar(0); // al abrir: todo el historial
});
