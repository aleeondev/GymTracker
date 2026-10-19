// ==========================================================
// GRAFICA-PESO.JS  (Mi perfil)
// Línea con tu peso corporal a lo largo del tiempo y, si tienes peso
// objetivo, una línea horizontal discontinua con la meta.
// Datos: PerfilController (pesosJson) y data-objetivo del <canvas>.
// ==========================================================

document.addEventListener("DOMContentLoaded", () => {
    const lienzo = document.getElementById("grafica-peso");
    if (!lienzo) return;

    const { COLORES } = GtGraficas;
    const kg = (valor) => `${String(Math.round(valor * 10) / 10).replace(".", ",")} kg`;
    const fechaCorta = (tiempo) =>
        new Date(tiempo).toLocaleDateString("es-ES", { day: "numeric", month: "short" }).replace(".", "");

    const pesos = GtGraficas.datos(lienzo).map((p) => ({
        x: new Date(p.fecha + "T12:00:00").getTime(),
        y: p.peso,
        etiqueta: p.etiqueta
    }));
    const objetivo = lienzo.dataset.objetivo ? Number(lienzo.dataset.objetivo) : null;

    const series = [{
        label: "Tu peso",
        data: pesos,
        borderColor: COLORES.principal,
        backgroundColor: COLORES.principal,
        borderWidth: 2.5,
        tension: 0.3,
        pointRadius: 3,
        pointHoverRadius: 6
    }];

    // La meta: una línea plana del primer al último día, discontinua y gris.
    // (Es solo una segunda serie con dos puntos: así no necesitamos ningún plugin.)
    if (objetivo) {
        series.push({
            label: "Objetivo",
            data: [{ x: pesos[0].x, y: objetivo }, { x: pesos[pesos.length - 1].x, y: objetivo }],
            borderColor: COLORES.secundario,
            borderWidth: 2,
            borderDash: [6, 5],
            pointRadius: 0,
            pointHoverRadius: 0
        });
    }

    new Chart(lienzo, {
        type: "line",
        data: { datasets: series },
        options: {
            interaction: { mode: "nearest", intersect: false },
            scales: {
                x: {
                    type: "linear",
                    grid: { display: false },
                    border: { display: false },
                    ticks: { maxTicksLimit: 6, callback: (valor) => fechaCorta(valor) }
                },
                y: {
                    border: { display: false },
                    grid: { color: COLORES.rejilla },
                    grace: "10%", // margen arriba y abajo: que la línea no toque los bordes
                    ticks: { maxTicksLimit: 5, callback: (valor) => `${valor} kg` }
                }
            },
            plugins: {
                legend: {
                    display: objetivo != null, // con una sola línea no hace falta leyenda
                    position: "top",
                    align: "start",
                    labels: { usePointStyle: true, pointStyle: "line", boxWidth: 24, color: COLORES.texto }
                },
                tooltip: {
                    // El tooltip solo para los puntos de peso (la línea del objetivo es una guía)
                    filter: (elemento) => elemento.datasetIndex === 0,
                    callbacks: {
                        title: (elementos) => elementos[0].raw.etiqueta,
                        label: (elemento) => {
                            const texto = kg(elemento.raw.y);
                            if (!objetivo) return texto;
                            const falta = Math.round((objetivo - elemento.raw.y) * 10) / 10;
                            if (falta === 0) return `${texto} · ¡objetivo alcanzado!`;
                            return `${texto} · ${falta > 0 ? "faltan" : "sobran"} ${kg(Math.abs(falta))}`;
                        }
                    }
                }
            }
        }
    });
});
