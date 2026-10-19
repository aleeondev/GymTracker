// ==========================================================
// DISCOS.JS  (pantalla de entrenar)
// Calculadora de discos: pones el peso total y la barra (20, 15, 10 u
// "Otra" con el peso que quieras, que se recuerda), y dibuja una
// barra olímpica (SVG) con los discos que van EN CADA LADO.
// Los discos nuevos entran deslizándose; los botones -5/+5 ayudan a cargar.
//
// Cómo calcula: (total - barra) / 2 = kg por lado. Luego va cogiendo
// siempre el disco más grande que quepa (25, 20, 15...) hasta completar.
// ==========================================================

document.addEventListener("DOMContentLoaded", () => {
    const dialogo = document.getElementById("dialogo-discos");
    if (!dialogo) return;

    const SVG = "http://www.w3.org/2000/svg";
    // Medidas de cada disco en el dibujo: grosor y alto (más pesado = más grande)
    const DISCOS = [
        { kg: 25, grosor: 16, alto: 124 },
        { kg: 20, grosor: 15, alto: 116 },
        { kg: 15, grosor: 13, alto: 104 },
        { kg: 10, grosor: 12, alto: 90 },
        { kg: 5, grosor: 10, alto: 66 },
        { kg: 2.5, grosor: 8, alto: 52 },
        { kg: 1.25, grosor: 6, alto: 42 }
    ];
    const ALTO = 140;            // alto del dibujo
    const CENTRO = ALTO / 2;     // la barra pasa por el medio
    const AGARRE = 120;          // zona central donde se cogen las manos
    const TOPE = 10;             // collarín entre el agarre y la manga
    const MANGA_MIN = 110;       // largo mínimo de cada manga (donde van los discos)

    const campoTotal = document.getElementById("discos-total");
    const bloqueOtra = document.getElementById("barra-otra");
    const campoBarra = document.getElementById("barra-peso");
    const radiosBarra = dialogo.querySelectorAll('input[name="barra"]');
    const resultado = document.getElementById("discos-resultado");
    const kg = (v) => String(Math.round(v * 100) / 100).replace(".", ",");
    let pesoDeSerie = null;
    let anteriores = [];         // discos de la vez anterior (para animar solo los nuevos)

    // Recordamos el último campo de kg en el que escribiste: la calculadora se abre con ese peso
    document.addEventListener("focusin", (evento) => {
        const campo = evento.target;
        if (campo.matches && campo.matches('.serie-fila input[step="0.25"]')) {
            pesoDeSerie = campo;
        }
    });

    document.getElementById("abrir-discos").addEventListener("click", () => {
        const peso = pesoDeSerie ? (pesoDeSerie.value || pesoDeSerie.placeholder) : "";
        if (peso && !isNaN(Number(peso))) campoTotal.value = peso;
        anteriores = [];
        calcular();
        dialogo.showModal(); // <dialog> nativo: ventana encima de todo, se cierra con Esc
    });

    dialogo.addEventListener("click", (evento) => {
        if (evento.target === dialogo) dialogo.close(); // tocar fuera = cerrar
    });

    // ---------- Barra: la recordamos para la próxima vez ----------
    // localStorage guarda datos en ESTE navegador (no en el servidor). Si no
    // está disponible (modo privado...), simplemente no se recuerda.
    function guardarBarra() {
        try {
            localStorage.setItem("gt_barra", dialogo.querySelector('input[name="barra"]:checked').value);
            localStorage.setItem("gt_barra_otra", campoBarra.value);
        } catch (e) { /* sin almacenamiento */ }
    }
    try {
        const guardada = localStorage.getItem("gt_barra");
        campoBarra.value = localStorage.getItem("gt_barra_otra") || "";
        radiosBarra.forEach((r) => { if (r.value === guardada) r.checked = true; });
    } catch (e) { /* sin almacenamiento */ }

    /** Peso de la barra elegida, o null si eligió "Otra" y aún no lo ha escrito. */
    function pesoBarra() {
        const elegida = dialogo.querySelector('input[name="barra"]:checked').value;
        bloqueOtra.hidden = elegida !== "otra";
        if (elegida !== "otra") return Number(elegida);
        const peso = parseFloat(String(campoBarra.value).replace(",", "."));
        return isNaN(peso) || peso < 0 ? null : peso;
    }

    campoBarra.addEventListener("input", () => {
        anteriores = [];
        calcular();
        guardarBarra();
    });

    // Botones -5 / -2,5 / +2,5 / +5
    dialogo.querySelectorAll("[data-sumar]").forEach((boton) => {
        boton.addEventListener("click", () => {
            const actual = parseFloat(String(campoTotal.value).replace(",", ".")) || 0;
            campoTotal.value = Math.min(500, Math.max(0, actual + Number(boton.dataset.sumar)));
            calcular();
        });
    });
    campoTotal.addEventListener("input", calcular);
    // Enter en el campo: calcular, no cerrar la ventana (lo que haría el <dialog> por defecto)
    campoTotal.addEventListener("keydown", (evento) => {
        if (evento.key === "Enter") {
            evento.preventDefault();
            calcular();
        }
    });
    radiosBarra.forEach((r) => r.addEventListener("change", () => {
        anteriores = [];
        calcular();
        guardarBarra();
        if (r.value === "otra" && r.checked) campoBarra.focus();
    }));

    // ---------- Cálculo ----------
    function discosPorLado(porLado) {
        const lado = [];
        for (const disco of DISCOS) {
            while (porLado + 1e-9 >= disco.kg) {
                lado.push(disco);
                porLado -= disco.kg;
            }
        }
        return lado;
    }

    function calcular() {
        resultado.replaceChildren();
        const total = parseFloat(String(campoTotal.value).replace(",", "."));
        const barra = pesoBarra();

        if (barra === null) {
            resultado.append(dibujarBarra([], 20), texto("Escribe el peso de tu barra (0 si es una máquina sin barra).", "discos-nota"));
            anteriores = [];
            return;
        }
        if (isNaN(total) || total <= 0) {
            resultado.append(dibujarBarra([], barra), texto("Escribe el peso total o usa los botones +5 / +2,5.", "discos-nota"));
            anteriores = [];
            return;
        }
        if (total < barra) {
            resultado.append(dibujarBarra([], barra), texto(`Es menos que la barra sola (${kg(barra)} kg).`, "discos-aviso"));
            anteriores = [];
            return;
        }

        const lado = discosPorLado((total - barra) / 2);
        const conseguido = barra + lado.reduce((suma, d) => suma + d.kg, 0) * 2;

        resultado.append(dibujarBarra(lado, barra));
        resultado.append(texto(lado.length ? "En cada lado" : "Solo la barra", "discos-titulo-lado"));
        if (lado.length) resultado.append(texto(lado.map((d) => kg(d.kg)).join(" + ") + " kg", "discos-lista"));
        if (Math.abs(conseguido - total) > 0.001) {
            resultado.append(texto(`No sale exacto con discos normales: así quedan ${kg(conseguido)} kg.`, "discos-aviso"));
        }
        anteriores = lado;
    }

    // ---------- Dibujo (SVG) ----------
    function crear(etiqueta, atributos, padre) {
        const nodo = document.createElementNS(SVG, etiqueta);
        Object.entries(atributos).forEach(([k, v]) => nodo.setAttribute(k, v));
        if (padre) padre.append(nodo);
        return nodo;
    }

    /** Dibuja la barra con los mismos discos en los dos lados (espejo). */
    function dibujarBarra(lado, barra) {
        const anchoDiscos = lado.reduce((suma, d) => suma + d.grosor + 2, 0);
        const manga = Math.max(MANGA_MIN, anchoDiscos + 16);
        const ancho = AGARRE + 2 * (TOPE + manga);
        const centroX = ancho / 2;

        const svg = crear("svg", { viewBox: `0 0 ${ancho} ${ALTO}`, class: "barra-svg", "aria-hidden": "true" });

        // Barra: agarre (fino), topes y mangas (más gruesas)
        if (barra > 0) {
            crear("rect", { x: centroX - AGARRE / 2, y: CENTRO - 4, width: AGARRE, height: 8, rx: 2, class: "barra-agarre" }, svg);
            [-1, 1].forEach((sentido) => {
                const inicioTope = sentido > 0 ? centroX + AGARRE / 2 : centroX - AGARRE / 2 - TOPE;
                const inicioManga = sentido > 0 ? centroX + AGARRE / 2 + TOPE : centroX - AGARRE / 2 - TOPE - manga;
                crear("rect", { x: inicioTope, y: CENTRO - 14, width: TOPE, height: 28, rx: 3, class: "barra-tope" }, svg);
                crear("rect", { x: inicioManga, y: CENTRO - 6, width: manga, height: 12, rx: 3, class: "barra-manga" }, svg);
            });
        } else {
            // Barra de 0 kg (máquina): solo un eje fino donde se cuelgan los discos
            crear("rect", { x: 0, y: CENTRO - 3, width: ancho, height: 6, rx: 3, class: "barra-manga" }, svg);
        }

        // Discos: del tope hacia fuera, el más pesado primero; igual en los dos lados
        [-1, 1].forEach((sentido) => {
            const grupo = crear("g", { class: sentido > 0 ? "lado lado-der" : "lado lado-izq" }, svg);
            let x = centroX + sentido * (AGARRE / 2 + TOPE + 2);
            lado.forEach((disco, i) => {
                const izquierda = sentido > 0 ? x : x - disco.grosor;
                const esNuevo = i >= anteriores.length || anteriores[i].kg !== disco.kg;
                const g = crear("g", { class: "plato" + (esNuevo ? " nuevo" : "") }, grupo);
                if (esNuevo) g.style.animationDelay = `${Math.min(i, 6) * 45}ms`; // entran uno detrás de otro
                crear("rect", {
                    x: izquierda, y: CENTRO - disco.alto / 2, width: disco.grosor, height: disco.alto, rx: 3,
                    class: "plato-cuerpo p" + String(disco.kg).replace(".", "-")
                }, g);
                if (disco.grosor >= 10) {
                    const cx = izquierda + disco.grosor / 2;
                    const t = crear("text", {
                        x: cx, y: CENTRO, class: "plato-texto",
                        transform: `rotate(-90 ${cx} ${CENTRO})`, "text-anchor": "middle", "dominant-baseline": "central"
                    }, g);
                    t.textContent = kg(disco.kg);
                }
                x += sentido * (disco.grosor + 2);
            });
        });
        return svg;
    }

    function texto(contenido, clase) {
        const p = document.createElement("p");
        p.textContent = contenido;
        if (clase) p.className = clase;
        return p;
    }
});
