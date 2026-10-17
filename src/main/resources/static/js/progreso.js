// ==========================================================
// PROGRESO.JS
// Gráfica de línea (SVG, sin librerías) del 1RM estimado por sesión.
// Los datos llegan en data-puntos desde EntrenamientoController.
// Al pasar el ratón o el dedo muestra la sesión más cercana.
// ==========================================================

document.addEventListener("DOMContentLoaded", () => {
    const contenedor = document.getElementById("grafica");
    if (!contenedor) return;

    const puntos = JSON.parse(contenedor.dataset.puntos).map((p) => ({
        ...p,
        tiempo: new Date(p.fecha + "T12:00:00").getTime()
    }));
    if (puntos.length < 2) return;

    const SVG = "http://www.w3.org/2000/svg";
    const numero = (v) => String(Math.round(v * 10) / 10).replace(".", ",");

    function crear(etiqueta, atributos, padre) {
        const nodo = document.createElementNS(SVG, etiqueta);
        Object.entries(atributos).forEach(([k, v]) => nodo.setAttribute(k, v));
        if (padre) padre.append(nodo);
        return nodo;
    }

    /** Divisiones "redondas" del eje Y (p. ej. 60, 70, 80...). */
    function marcas(min, max, cuantas) {
        const paso = Math.pow(10, Math.floor(Math.log10((max - min) / cuantas)));
        const opciones = [1, 2, 2.5, 5, 10].map((m) => m * paso);
        const elegido = opciones.find((o) => (max - min) / o <= cuantas) || opciones[opciones.length - 1];
        const lista = [];
        for (let v = Math.floor(min / elegido) * elegido; v <= max + 1e-9; v += elegido) lista.push(v);
        return lista;
    }

    const tooltip = document.createElement("div");
    tooltip.className = "grafica-tooltip";
    tooltip.hidden = true;

    function dibujar() {
        contenedor.replaceChildren();
        const ancho = Math.max(280, contenedor.clientWidth);
        const alto = ancho < 500 ? 220 : 280;
        const margen = { arriba: 16, derecha: 18, abajo: 30, izquierda: 42 };

        const valores = puntos.map((p) => p.unoRm);
        let min = Math.min(...valores);
        let max = Math.max(...valores);
        if (min === max) { min -= 5; max += 5; }
        const holgura = (max - min) * 0.15;
        const ticks = marcas(Math.max(0, min - holgura), max + holgura, 4);
        const yMin = ticks[0];
        const yMax = ticks[ticks.length - 1];

        const t0 = puntos[0].tiempo;
        const t1 = puntos[puntos.length - 1].tiempo;
        const x = (t) => margen.izquierda + (t1 === t0 ? 0.5 : (t - t0) / (t1 - t0)) * (ancho - margen.izquierda - margen.derecha);
        const y = (v) => margen.arriba + (1 - (v - yMin) / (yMax - yMin)) * (alto - margen.arriba - margen.abajo);

        const svg = crear("svg", { viewBox: `0 0 ${ancho} ${alto}`, width: ancho, height: alto });

        // Rejilla y eje Y (discretos)
        ticks.forEach((v) => {
            crear("line", { class: "eje", x1: margen.izquierda, x2: ancho - margen.derecha, y1: y(v), y2: y(v) }, svg);
            const texto = crear("text", { class: "eje-texto", x: margen.izquierda - 8, y: y(v) + 4, "text-anchor": "end" }, svg);
            texto.textContent = numero(v);
        });

        // Eje X: primera y última fecha (y la del medio si cabe)
        const etiquetasX = [puntos[0], puntos[puntos.length - 1]];
        if (puntos.length > 4 && ancho > 500) etiquetasX.splice(1, 0, puntos[Math.floor(puntos.length / 2)]);
        etiquetasX.forEach((p, i) => {
            const ancla = i === 0 ? "start" : (i === etiquetasX.length - 1 ? "end" : "middle");
            const texto = crear("text", { class: "eje-texto", x: x(p.tiempo), y: alto - 8, "text-anchor": ancla }, svg);
            texto.textContent = p.etiqueta;
        });

        // Área suave + línea
        const camino = puntos.map((p, i) => `${i ? "L" : "M"}${x(p.tiempo)},${y(p.unoRm)}`).join(" ");
        const base = y(yMin);
        crear("path", { class: "area", d: `${camino} L${x(t1)},${base} L${x(t0)},${base} Z` }, svg);
        crear("path", { class: "linea", d: camino }, svg);

        // Línea vertical de referencia al pasar el ratón
        const guia = crear("line", { class: "eje", y1: margen.arriba, y2: base, visibility: "hidden" }, svg);

        const circulos = puntos.map((p) =>
            crear("circle", { class: "punto", cx: x(p.tiempo), cy: y(p.unoRm), r: 4 }, svg));

        // Etiqueta directa solo en el último punto
        const ultimo = puntos[puntos.length - 1];
        const etiqueta = crear("text", {
            class: "eje-texto", x: x(ultimo.tiempo), y: y(ultimo.unoRm) - 12, "text-anchor": "end", "font-weight": "800"
        }, svg);
        etiqueta.textContent = numero(ultimo.unoRm) + " kg";

        // Zona de interacción (más grande que los puntos)
        const zona = crear("rect", {
            x: margen.izquierda, y: 0, width: ancho - margen.izquierda - margen.derecha, height: alto, fill: "transparent"
        }, svg);

        function mostrar(evento) {
            const rect = svg.getBoundingClientRect();
            const px = (evento.clientX - rect.left) * (ancho / rect.width);
            let cercano = 0;
            puntos.forEach((p, i) => {
                if (Math.abs(x(p.tiempo) - px) < Math.abs(x(puntos[cercano].tiempo) - px)) cercano = i;
            });
            const p = puntos[cercano];
            circulos.forEach((c, i) => c.classList.toggle("activo", i === cercano));
            guia.setAttribute("x1", x(p.tiempo));
            guia.setAttribute("x2", x(p.tiempo));
            guia.setAttribute("visibility", "visible");

            tooltip.textContent = `${p.etiqueta} · 1RM ${numero(p.unoRm)} kg · mejor ${numero(p.mejorPeso)} kg`;
            tooltip.hidden = false;
            const escala = rect.width / ancho;
            const izquierda = Math.min(Math.max(x(p.tiempo) * escala, 90), rect.width - 90);
            tooltip.style.left = `${izquierda}px`;
            tooltip.style.top = `${y(p.unoRm) * escala}px`;
        }

        function ocultar() {
            tooltip.hidden = true;
            guia.setAttribute("visibility", "hidden");
            circulos.forEach((c) => c.classList.remove("activo"));
        }

        zona.addEventListener("pointermove", mostrar);
        zona.addEventListener("pointerdown", mostrar);
        zona.addEventListener("pointerleave", ocultar);

        contenedor.append(svg, tooltip);
    }

    dibujar();
    let espera;
    window.addEventListener("resize", () => {
        clearTimeout(espera);
        espera = setTimeout(dibujar, 150);
    });
});