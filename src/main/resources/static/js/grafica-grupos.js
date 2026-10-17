// ==========================================================
// GRAFICA-GRUPOS.JS  (Progreso)
// Barras horizontales: series de cada grupo muscular en las últimas
// 4 semanas, de más a menos. Sirve para ver si estás equilibrado o te
// olvidas de algún músculo. Datos: EntrenamientoController (prepararGraficaGrupos).
//
// ¿Por qué barras y no un "quesito"? Con 11-12 grupos el quesito tendría
// porciones casi iguales, y el ojo compara fatal ángulos. Con barras
// ordenadas se ve al instante quién va primero y quién se queda atrás.
// ==========================================================

document.addEventListener("DOMContentLoaded", () => {
    const lienzo = document.getElementById("grafica-grupos");
    if (!lienzo) return;

    const grupos = GtGraficas.datos(lienzo);
    const { COLORES } = GtGraficas;
    const total = grupos.reduce((suma, g) => suma + g.series, 0);

    // La altura depende de cuántas barras haya: unos 30px por grupo.
    // (Se pone desde JS porque la CSP no permite estilos escritos en el HTML.)
    document.getElementById("caja-grupos").style.height = `${grupos.length * 30 + 30}px`;

    // PLUGIN PROPIO: escribe el número de series al final de cada barra.
    // Un plugin es una función que Chart.js llama en ciertos momentos del
    // dibujo; "afterDatasetsDraw" = justo después de pintar las barras.
    const numeroAlFinal = {
        id: "numeroAlFinal",
        afterDatasetsDraw(chart) {
            const { ctx } = chart;
            ctx.save();
            ctx.font = '800 12px "Plus Jakarta Sans", sans-serif';
            ctx.textBaseline = "middle";
            chart.getDatasetMeta(0).data.forEach((barra, i) => {
                const series = grupos[i].series;
                ctx.fillStyle = series ? COLORES.texto : COLORES.secundario;
                ctx.fillText(String(series), barra.x + 8, barra.y);
            });
            ctx.restore();
        }
    };

    new Chart(lienzo, {
        type: "bar",
        plugins: [numeroAlFinal],
        data: {
            labels: grupos.map((g) => g.grupo),
            datasets: [{
                label: "Series",
                data: grupos.map((g) => g.series),
                backgroundColor: COLORES.principal,
                borderRadius: 6,
                barPercentage: 0.75,
                categoryPercentage: 0.85
            }]
        },
        options: {
            indexAxis: "y", // <- esto es lo que hace las barras horizontales
            layout: { padding: { right: 32 } }, // hueco para el número de la última barra
            scales: {
                x: {
                    beginAtZero: true,
                    display: false // el número va escrito en cada barra: el eje sobra
                },
                y: {
                    grid: { display: false },
                    border: { display: false },
                    ticks: {
                        color: COLORES.texto,
                        font: { weight: "600" }
                    }
                }
            },
            plugins: {
                tooltip: {
                    callbacks: {
                        title: (elementos) => grupos[elementos[0].dataIndex].grupo,
                        label: (elemento) => {
                            const series = grupos[elemento.dataIndex].series;
                            if (!series) return "Sin series en 4 semanas";
                            const porcentaje = Math.round((series / total) * 100);
                            return `${series} ${series === 1 ? "serie" : "series"} · ${porcentaje}% del total`;
                        }
                    }
                }
            }
        }
    });
});
