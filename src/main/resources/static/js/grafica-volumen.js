// ==========================================================
// GRAFICA-VOLUMEN.JS  (Panel)
// Barras con el volumen (kg × reps) de cada una de las últimas 8 semanas.
// Los datos los prepara DashboardController (prepararGraficaVolumen).
// ==========================================================

document.addEventListener("DOMContentLoaded", () => {
    const lienzo = document.getElementById("grafica-volumen");
    if (!lienzo) return; // sin entrenos todavía: la plantilla no pinta la gráfica

    const semanas = GtGraficas.datos(lienzo);
    const { COLORES, entero } = GtGraficas;

    new Chart(lienzo, {
        type: "bar",
        data: {
            labels: semanas.map((s) => s.etiqueta),
            datasets: [{
                label: "Volumen",
                data: semanas.map((s) => s.volumen),
                // Esta semana en rojo intenso; las anteriores, en rojo suave
                backgroundColor: semanas.map((s) => (s.actual ? COLORES.principal : COLORES.principalSuave)),
                hoverBackgroundColor: COLORES.principal,
                borderRadius: 6,          // esquinas redondeadas arriba (abajo quedan rectas)
                maxBarThickness: 38,
                categoryPercentage: 0.7
            }]
        },
        options: {
            scales: {
                x: {
                    grid: { display: false },
                    border: { display: false }
                },
                y: {
                    beginAtZero: true,
                    border: { display: false },
                    grid: { color: COLORES.rejilla },
                    ticks: {
                        maxTicksLimit: 5,
                        callback: (valor) => entero(valor)
                    }
                }
            },
            plugins: {
                tooltip: {
                    callbacks: {
                        title: (elementos) => semanas[elementos[0].dataIndex].titulo,
                        label: (elemento) => {
                            const s = semanas[elemento.dataIndex];
                            if (!s.entrenos) return "Sin entrenos";
                            return `${entero(s.volumen)} kg · ${s.entrenos} ${s.entrenos === 1 ? "entreno" : "entrenos"}`;
                        }
                    }
                }
            }
        }
    });
});
