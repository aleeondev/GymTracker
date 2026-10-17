// ==========================================================
// ENTRENAR.JS
// Pantalla de registrar un entrenamiento.
//
// - Dibuja un bloque por ejercicio con sus series (kg + repeticiones).
// - El botón ✓ marca la serie como hecha (si está vacía, copia los
//   valores sugeridos) y arranca el temporizador de descanso.
// - Guarda un BORRADOR en el navegador (localStorage) con cada cambio:
//   si se recarga la página o se cierra el móvil, no se pierde nada.
// - Al enviar, crea los campos ocultos series[0].peso, series[0].repeticiones...
//   que Spring convierte en EntrenamientoForm.
// ==========================================================

document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("form-entreno");
    if (!form) return;

    const datos = JSON.parse(form.dataset.datos);
    const editando = datos.id != null;
    const clave = datos.claveBorrador;
    const HORAS_VALIDEZ_BORRADOR = 18;

    const contenedor = document.getElementById("bloques");
    const camposOcultos = document.getElementById("campos-ocultos");
    const campoNombre = document.getElementById("nombre");
    const campoFecha = document.getElementById("fecha");
    const campoDuracion = document.getElementById("duracionMinutos");
    const campoNotas = document.getElementById("notas");

    // ---------- Estado inicial (borrador guardado o datos del servidor) ----------

    let estado;
    const borrador = leerBorrador();

    if (borrador) {
        estado = borrador.estado;
        campoNombre.value = borrador.campos.nombre || "";
        campoFecha.value = borrador.campos.fecha || datos.fecha;
        campoDuracion.value = borrador.campos.duracion || "";
        campoNotas.value = borrador.campos.notas || "";
        document.getElementById("aviso-borrador").hidden = false;
    } else {
        estado = {
            inicio: editando ? null : Date.now(),
            bloques: datos.bloques.map((b) => ({
                ejercicioId: b.ejercicioId,
                nombre: b.nombre,
                grupo: b.grupo,
                descanso: b.descanso || 90,
                objetivo: b.objetivo || null,
                series: b.series.length
                    ? b.series.map((s) => ({ peso: s.peso, reps: s.reps, hecha: !!s.hecha }))
                    : filasVacias(b.objetivo ? b.objetivo.series : 3)
            }))
        };
        campoNombre.value = datos.nombre || "";
        campoFecha.value = datos.fecha;
        campoDuracion.value = datos.duracionMinutos || "";
        campoNotas.value = datos.notas || "";
    }

    rellenarSelectorEjercicios();
    dibujar();
    iniciarCronometro();

    // ---------- Dibujar ----------

    function filasVacias(n) {
        return Array.from({ length: n }, () => ({ peso: "", reps: "", hecha: false }));
    }

    function el(etiqueta, clase, texto) {
        const nodo = document.createElement(etiqueta);
        if (clase) nodo.className = clase;
        if (texto != null) nodo.textContent = texto;
        return nodo;
    }

    function num(valor) {
        // 62.5 -> "62,5"
        return String(valor).replace(".", ",");
    }

    function dibujar() {
        contenedor.replaceChildren();

        if (!estado.bloques.length) {
            const vacio = el("div", "vacio");
            vacio.append(el("h3", null, "Añade tu primer ejercicio"));
            vacio.append(el("p", null, "Elige un ejercicio en el desplegable de abajo y pulsa \"Añadir ejercicio\"."));
            contenedor.append(vacio);
            return;
        }

        estado.bloques.forEach((bloque, indiceBloque) => {
            contenedor.append(dibujarBloque(bloque, indiceBloque));
        });
    }

    function dibujarBloque(bloque, indiceBloque) {
        const seccion = el("section", "bloque");
        const ultima = datos.ultimaVez[String(bloque.ejercicioId)];

        // Cabecera del ejercicio
        const cabecera = el("div", "bloque-head");
        cabecera.append(el("div", "orden", String(indiceBloque + 1)));

        const textos = el("div", "fila-cuerpo");
        textos.append(el("h3", null, bloque.nombre));

        const info = [];
        if (bloque.grupo) info.push(bloque.grupo);
        if (bloque.objetivo) {
            let objetivo = `Objetivo ${bloque.objetivo.series} × ${bloque.objetivo.reps}`;
            if (bloque.objetivo.peso) objetivo += ` · ${num(bloque.objetivo.peso)} kg`;
            info.push(objetivo);
        }
        if (info.length) textos.append(el("p", "bloque-info", info.join(" · ")));

        if (ultima) {
            const p = el("p", "bloque-info");
            p.append(el("b", null, `Última vez (${ultima.fecha}): `));
            p.append(ultima.series.map((s) => `${num(s.peso)}×${s.reps}`).join(" · "));
            textos.append(p);
        }
        cabecera.append(textos);

        const quitar = el("button", "icono-btn", "✕");
        quitar.type = "button";
        quitar.setAttribute("aria-label", "Quitar ejercicio");
        quitar.addEventListener("click", () => {
            const tieneDatos = bloque.series.some((s) => s.reps !== "" && s.reps != null);
            if (tieneDatos && !confirm(`¿Quitar ${bloque.nombre} y sus series?`)) return;
            estado.bloques.splice(indiceBloque, 1);
            guardarBorrador();
            dibujar();
        });
        cabecera.append(quitar);
        seccion.append(cabecera);

        // Tabla de series
        const tabla = el("div", "series-tabla");
        const encabezado = el("div", "serie-fila cabecera");
        ["Serie", "Kg", "Reps", "Hecha", ""].forEach((t) => encabezado.append(el("span", t === "Serie" ? "serie-num" : null, t)));
        tabla.append(encabezado);

        bloque.series.forEach((serie, indiceSerie) => {
            tabla.append(dibujarSerie(bloque, serie, indiceSerie, ultima));
        });
        seccion.append(tabla);

        const anadir = el("button", "anadir-serie", "+ Añadir serie");
        anadir.type = "button";
        anadir.addEventListener("click", () => {
            const anterior = bloque.series[bloque.series.length - 1];
            bloque.series.push({ peso: anterior ? anterior.peso : "", reps: "", hecha: false });
            guardarBorrador();
            dibujar();
        });
        seccion.append(anadir);

        return seccion;
    }

    function dibujarSerie(bloque, serie, indiceSerie, ultima) {
        const fila = el("div", "serie-fila" + (serie.hecha ? " hecha" : ""));
        fila.append(el("span", "serie-num", String(indiceSerie + 1)));

        // Valores sugeridos: lo que hiciste la última vez o el objetivo de la rutina
        const anteriorMismaSerie = ultima ? (ultima.series[indiceSerie] || ultima.series[ultima.series.length - 1]) : null;
        const pesoSugerido = anteriorMismaSerie ? anteriorMismaSerie.peso : (bloque.objetivo && bloque.objetivo.peso ? bloque.objetivo.peso : "");
        const repsSugeridas = bloque.objetivo ? bloque.objetivo.reps : (anteriorMismaSerie ? anteriorMismaSerie.reps : "");

        const peso = el("input");
        peso.type = "number";
        peso.min = "0";
        peso.max = "1500";
        peso.step = "0.25";
        peso.inputMode = "decimal";
        peso.placeholder = pesoSugerido === "" ? "kg" : String(pesoSugerido);
        peso.value = serie.peso;
        peso.setAttribute("aria-label", `Kg serie ${indiceSerie + 1}`);
        peso.addEventListener("input", () => { serie.peso = peso.value; guardarBorrador(); });

        const reps = el("input");
        reps.type = "number";
        reps.min = "1";
        reps.max = "1000";
        reps.step = "1";
        reps.inputMode = "numeric";
        reps.placeholder = repsSugeridas === "" ? "reps" : String(repsSugeridas);
        reps.value = serie.reps;
        reps.setAttribute("aria-label", `Repeticiones serie ${indiceSerie + 1}`);
        reps.addEventListener("input", () => { serie.reps = reps.value; guardarBorrador(); });

        const check = el("button", "check-serie", "✓");
        check.type = "button";
        check.setAttribute("aria-label", "Marcar serie como hecha");
        check.setAttribute("aria-pressed", serie.hecha ? "true" : "false");
        check.addEventListener("click", () => {
            if (!serie.hecha) {
                // Serie vacía: copiamos lo sugerido para apuntarla con un solo toque
                if (reps.value === "" && repsSugeridas !== "") reps.value = repsSugeridas;
                if (peso.value === "" && pesoSugerido !== "") peso.value = pesoSugerido;
                if (reps.value === "") {
                    reps.focus();
                    return;
                }
                serie.reps = reps.value;
                serie.peso = peso.value;
                serie.hecha = true;
                iniciarDescanso(bloque.descanso || 90);
            } else {
                serie.hecha = false;
            }
            fila.classList.toggle("hecha", serie.hecha);
            check.setAttribute("aria-pressed", serie.hecha ? "true" : "false");
            guardarBorrador();
        });

        const quitar = el("button", "icono-btn quitar-serie", "✕");
        quitar.type = "button";
        quitar.setAttribute("aria-label", "Quitar serie");
        quitar.addEventListener("click", () => {
            bloque.series.splice(indiceSerie, 1);
            guardarBorrador();
            dibujar();
        });

        fila.append(peso, reps, check, quitar);
        return fila;
    }

    // ---------- Añadir ejercicio ----------

    function rellenarSelectorEjercicios() {
        const selector = document.getElementById("selector-ejercicio");
        selector.append(new Option("Elige un ejercicio para añadir...", ""));
        const grupos = new Map();
        datos.catalogo.forEach((e) => {
            if (!grupos.has(e.grupo)) grupos.set(e.grupo, []);
            grupos.get(e.grupo).push(e);
        });
        grupos.forEach((ejercicios, grupo) => {
            const optgroup = document.createElement("optgroup");
            optgroup.label = grupo;
            ejercicios.forEach((e) => optgroup.append(new Option(e.nombre, e.id)));
            selector.append(optgroup);
        });

        document.getElementById("btn-anadir-ejercicio").addEventListener("click", () => {
            const id = Number(selector.value);
            const ejercicio = datos.catalogo.find((e) => e.id === id);
            if (!ejercicio) {
                selector.focus();
                return;
            }
            estado.bloques.push({
                ejercicioId: ejercicio.id,
                nombre: ejercicio.nombre,
                grupo: ejercicio.grupo,
                descanso: 90,
                objetivo: null,
                series: filasVacias(3)
            });
            selector.value = "";
            guardarBorrador();
            dibujar();
            contenedor.lastElementChild.scrollIntoView({ behavior: "smooth", block: "center" });
        });
    }

    // ---------- Borrador en el navegador ----------

    function guardarBorrador() {
        try {
            localStorage.setItem(clave, JSON.stringify({
                guardado: Date.now(),
                estado,
                campos: {
                    nombre: campoNombre.value,
                    fecha: campoFecha.value,
                    duracion: campoDuracion.value,
                    notas: campoNotas.value
                }
            }));
        } catch (e) {
            // Sin localStorage (modo privado...) simplemente no hay borrador.
        }
    }

    function leerBorrador() {
        try {
            const guardado = JSON.parse(localStorage.getItem(clave));
            if (!guardado) return null;
            if (Date.now() - guardado.guardado > HORAS_VALIDEZ_BORRADOR * 3600 * 1000) {
                localStorage.removeItem(clave);
                return null;
            }
            return guardado;
        } catch (e) {
            return null;
        }
    }

    function borrarBorrador() {
        try {
            localStorage.removeItem(clave);
        } catch (e) {
            /* nada */
        }
    }

    [campoNombre, campoFecha, campoDuracion, campoNotas].forEach((campo) =>
        campo.addEventListener("input", guardarBorrador));

    document.getElementById("empezar-de-cero").addEventListener("click", () => {
        if (!confirm("¿Borrar lo apuntado y empezar de cero?")) return;
        borrarBorrador();
        window.location.reload();
    });

    const descartar = document.getElementById("descartar-entreno");
    if (descartar) {
        descartar.addEventListener("click", (evento) => {
            const hayDatos = estado.bloques.some((b) => b.series.some((s) => s.reps !== "" && s.reps != null));
            if (hayDatos && !confirm("Tienes series apuntadas sin guardar. ¿Descartarlas?")) {
                evento.preventDefault();
                return;
            }
            borrarBorrador();
        });
    }

    // ---------- Cronómetro del entrenamiento ----------

    function iniciarCronometro() {
        const valor = document.getElementById("cronometro-valor");
        if (!valor || !estado.inicio) return;
        const actualizar = () => {
            valor.textContent = formatoTiempo(Math.floor((Date.now() - estado.inicio) / 1000));
        };
        actualizar();
        setInterval(actualizar, 1000);
    }

    function formatoTiempo(segundos) {
        const h = Math.floor(segundos / 3600);
        const m = Math.floor((segundos % 3600) / 60);
        const s = segundos % 60;
        const dos = (n) => String(n).padStart(2, "0");
        return h > 0 ? `${h}:${dos(m)}:${dos(s)}` : `${dos(m)}:${dos(s)}`;
    }

    // ---------- Temporizador de descanso ----------

    const panelDescanso = document.getElementById("descanso");
    const textoDescanso = document.getElementById("descanso-tiempo");
    const etiquetaDescanso = document.getElementById("descanso-etiqueta");
    let finDescanso = null;
    let intervaloDescanso = null;

    function iniciarDescanso(segundos) {
        if (!segundos) return;
        finDescanso = Date.now() + segundos * 1000;
        panelDescanso.hidden = false;
        panelDescanso.classList.remove("terminado");
        etiquetaDescanso.textContent = "DESCANSO";
        clearInterval(intervaloDescanso);
        intervaloDescanso = setInterval(tickDescanso, 250);
        tickDescanso();
    }

    function tickDescanso() {
        const quedan = Math.ceil((finDescanso - Date.now()) / 1000);
        if (quedan > 0) {
            const m = Math.floor(quedan / 60);
            textoDescanso.textContent = `${m}:${String(quedan % 60).padStart(2, "0")}`;
            return;
        }
        clearInterval(intervaloDescanso);
        panelDescanso.classList.add("terminado");
        etiquetaDescanso.textContent = "¡SIGUIENTE SERIE!";
        textoDescanso.textContent = "0:00";
        if (navigator.vibrate) navigator.vibrate([200, 100, 200]);
        setTimeout(() => { panelDescanso.hidden = true; }, 5000);
    }

    document.getElementById("descanso-mas").addEventListener("click", () => {
        if (panelDescanso.classList.contains("terminado")) {
            iniciarDescanso(15);
        } else {
            finDescanso += 15000;
            tickDescanso();
        }
    });

    document.getElementById("descanso-saltar").addEventListener("click", () => {
        clearInterval(intervaloDescanso);
        panelDescanso.hidden = true;
    });

    // ---------- Enviar ----------

    function seriesApuntadas() {
        const lista = [];
        estado.bloques.forEach((bloque, indiceBloque) => {
            bloque.series.forEach((s) => {
                const reps = parseInt(s.reps, 10);
                if (!reps || reps < 1) return;
                const peso = parseFloat(String(s.peso).replace(",", "."));
                lista.push({ bloque: indiceBloque, ejercicioId: bloque.ejercicioId, reps, peso: isNaN(peso) ? 0 : peso });
            });
        });
        return lista;
    }

    // Comprobación previa (fase de captura: se ejecuta antes que app.js)
    document.addEventListener("submit", (evento) => {
        if (evento.target !== form || form.dataset.enviando === "1") return;
        if (!seriesApuntadas().length) {
            evento.preventDefault();
            evento.stopPropagation();
            alert("Apunta al menos una serie (kg y repeticiones) antes de guardar.");
        }
    }, true);

    // app.js lanza este evento justo antes de enviar el formulario
    form.addEventListener("antes-de-enviar", () => {
        camposOcultos.replaceChildren();
        const oculto = (nombre, valor) => {
            if (valor == null || valor === "") return;
            const input = document.createElement("input");
            input.type = "hidden";
            input.name = nombre;
            input.value = valor;
            camposOcultos.append(input);
        };

        oculto("id", datos.id);
        oculto("rutinaId", datos.rutinaId);
        seriesApuntadas().forEach((s, i) => {
            oculto(`series[${i}].bloque`, s.bloque);
            oculto(`series[${i}].ejercicioId`, s.ejercicioId);
            oculto(`series[${i}].repeticiones`, s.reps);
            oculto(`series[${i}].peso`, s.peso);
        });

        if (!editando && !campoDuracion.value && estado.inicio) {
            campoDuracion.value = Math.max(1, Math.round((Date.now() - estado.inicio) / 60000));
        }

        // El borrador se borra cuando el servidor confirma que se guardó (ver app.js)
        try {
            sessionStorage.setItem("gt_borrador_pendiente", clave);
        } catch (e) {
            /* nada */
        }
    });
});