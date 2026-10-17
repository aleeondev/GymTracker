// ==========================================================
// GRAFICAS.JS
// Configuración común de todas las gráficas (Chart.js).
// Se carga después de js/vendor/chart-4.5.0.umd.min.js y antes del JS
// de cada gráfica (por ejemplo grafica-volumen.js).
//
// Aquí se decide el "estilo de la casa": colores, fuentes, tooltips y
// formato de números. Si cambias algo aquí, cambia en todas las gráficas.
// ==========================================================

const GtGraficas = (() => {
    // Colores: los leemos de las variables CSS (styles.css / app.css) para que
    // la gráfica use exactamente los mismos que el resto de la app.
    const estilos = getComputedStyle(document.documentElement);
    const variable = (nombre, porDefecto) => estilos.getPropertyValue(nombre).trim() || porDefecto;

    const COLORES = {
        principal: variable("--color-principal", "#c92f4b"),   // dato destacado (p. ej. esta semana)
        principalSuave: "#e08a9b",                             // resto de datos de la misma serie
        texto: variable("--color-texto", "#252525"),
        secundario: variable("--color-texto-secundario", "#707070"),
        rejilla: variable("--color-borde", "#eeeeee")
    };

    // Valores por defecto de Chart.js para TODAS las gráficas
    Chart.defaults.font.family = '"Plus Jakarta Sans", sans-serif';
    Chart.defaults.font.size = 12;
    Chart.defaults.color = COLORES.secundario;
    Chart.defaults.maintainAspectRatio = false;       // la altura la marca el CSS (.grafica-caja)
    Chart.defaults.plugins.legend.display = false;    // con una sola serie, el título ya dice qué es

    const tooltip = Chart.defaults.plugins.tooltip;
    tooltip.backgroundColor = COLORES.texto;
    tooltip.padding = 12;
    tooltip.cornerRadius = 10;
    tooltip.displayColors = false;
    tooltip.titleFont = { family: '"Nunito", sans-serif', weight: "800", size: 13 };
    tooltip.bodyFont = { size: 13 };

    // Si el usuario pidió "reducir movimiento" en su sistema, sin animaciones
    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
        Chart.defaults.animation = false;
    }

    /** 12450.4 -> "12.450" (como en el resto de la app, con punto de miles) */
    function entero(valor) {
        return String(Math.round(valor)).replace(/\B(?=(\d{3})+(?!\d))/g, ".");
    }

    /** Lee el JSON que el servidor dejó en data-datos de un elemento. */
    function datos(elemento) {
        return JSON.parse(elemento.dataset.datos);
    }

    return { COLORES, entero, datos };
})();
