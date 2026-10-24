// ==========================================================
// PRONTO.JS - Web de prelanzamiento (/pronto)
//
// 1) Pantalla de carga: bloques que se encienden y porcentaje
// 2) Barra de carga del hero, que se queda en el 87 %
// 3) Animaciones con el scroll: aparecer, manifiesto, contadores,
//    texto que se "descifra" y barra de progreso de lectura
// 4) Ratón: botones magnéticos y foco de luz en las tarjetas
// 5) Formulario de acceso anticipado (fetch, sin recargar la página)
// 6) Compartir el enlace
//
// Sin librerías. El texto se escribe siempre con textContent (nunca innerHTML).
// El minijuego está en reto.js.
// ==========================================================

(() => {
    "use strict";

    const html = document.documentElement;
    html.classList.remove("sin-js");
    html.classList.add("js");

    const $ = (id) => document.getElementById(id);
    const sinMovimiento = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    const ratonFino = window.matchMedia("(hover: hover) and (pointer: fine)").matches;

    // sessionStorage puede fallar (modo privado, cookies bloqueadas): nunca debe romper la página
    function leerSesion(clave) {
        try { return sessionStorage.getItem(clave); } catch (e) { return null; }
    }
    function guardarSesion(clave, valor) {
        try { sessionStorage.setItem(clave, valor); } catch (e) { return; }
    }

    // Anima un número de "desde" a "hasta" (frenando al final) y avisa en cada paso
    function animarNumero(desde, hasta, duracion, retraso, alCambiar, alTerminar) {
        let inicio = null;
        const paso = (t) => {
            inicio ??= t + retraso;
            const p = Math.min(1, Math.max(0, (t - inicio) / duracion));
            const suave = 1 - Math.pow(1 - p, 3);
            alCambiar(Math.round(desde + (hasta - desde) * suave));
            if (p < 1) requestAnimationFrame(paso);
            else if (alTerminar) alTerminar();
        };
        requestAnimationFrame(paso);
    }

    // Un único "scroll" para todos los efectos, como mucho una vez por fotograma
    const alHacerScroll = [];
    let pendiente = false;
    function enScroll(funcion) {
        alHacerScroll.push(funcion);
        funcion();
    }
    function avisarScroll() {
        if (pendiente) return;
        pendiente = true;
        requestAnimationFrame(() => {
            pendiente = false;
            alHacerScroll.forEach((f) => f());
        });
    }
    window.addEventListener("scroll", avisarScroll, { passive: true });
    window.addEventListener("resize", avisarScroll);


    // ---------- 1) Pantalla de carga ----------

    const FRASES = ["Calentando motores", "Cargando la barra", "Ajustando los discos",
        "Contando repeticiones", "Batiendo récords", "Casi listo"];

    // Avance con "atascos", como una carga de verdad: [tiempo, progreso]
    const TRAMOS = [[0, 0], [0.3, 0.38], [0.5, 0.58], [0.68, 0.63], [0.85, 0.91], [1, 1]];
    function curva(t) {
        for (let i = 1; i < TRAMOS.length; i++) {
            const [t0, v0] = TRAMOS[i - 1];
            const [t1, v1] = TRAMOS[i];
            if (t <= t1) return v0 + (v1 - v0) * (t - t0) / (t1 - t0);
        }
        return 1;
    }

    function pantallaDeCarga() {
        const intro = $("intro");
        if (!intro) return arrancar();
        // Si vienes a una sección concreta (#reto, #apuntate) o prefieres menos movimiento, sin intro
        if (sinMovimiento || location.hash) {
            intro.remove();
            return arrancar();
        }
        intro.classList.add("activa");
        document.body.classList.add("bloqueado");

        const bloques = intro.querySelectorAll(".bloques span");
        const porcentaje = $("intro-porcentaje");
        const frase = $("intro-frase");
        // La segunda vez en la misma sesión, la carga es más corta
        const duracion = leerSesion("gt_intro_vista") ? 1300 : 3400;
        let inicio = null;
        let terminado = false;

        const paso = (t) => {
            if (terminado) return;
            inicio ??= t;
            const p = Math.min(1, (t - inicio) / duracion);
            const progreso = curva(p);
            porcentaje.textContent = String(Math.floor(progreso * 100)).padStart(3, "0");
            const encendidos = Math.round(progreso * bloques.length);
            bloques.forEach((b, i) => b.classList.toggle("on", i < encendidos));
            const texto = FRASES[Math.min(FRASES.length - 1, Math.floor(progreso * FRASES.length))];
            if (frase.textContent !== texto) frase.textContent = texto;
            if (p < 1) requestAnimationFrame(paso);
            else terminar();
        };

        function terminar() {
            if (terminado) return;
            terminado = true;
            porcentaje.textContent = "100";
            frase.textContent = "Bienvenido a GymTracker";
            bloques.forEach((b) => b.classList.add("on"));
            intro.classList.add("completo");
            guardarSesion("gt_intro_vista", "1");
            setTimeout(() => {
                intro.classList.add("fuera");
                document.body.classList.remove("bloqueado");
                arrancar();
            }, 550);
            setTimeout(() => intro.remove(), 1800);
        }

        // Clic o cualquier tecla: saltar la intro
        intro.addEventListener("click", terminar);
        document.addEventListener("keydown", terminar, { once: true });
        requestAnimationFrame(paso);
    }

    // Cuando se aparta la pantalla de carga empieza todo lo demás
    function arrancar() {
        html.classList.add("listo");
        cargaHero();
        revelarAlBajar();
        descifrarAlVer();
        contadores();
        barraYProgreso();
    }


    // ---------- 2) Barra de carga del hero (se queda en el 87 %) ----------

    function cargaHero() {
        const bloques = document.querySelectorAll("#hero-bloques span");
        const texto = $("hero-porcentaje");
        if (!bloques.length || !texto) return;
        const OBJETIVO = 87;
        const llenos = Math.floor(bloques.length * OBJETIVO / 100);
        const pintar = (valor) => {
            texto.textContent = valor;
            const n = Math.floor(bloques.length * valor / 100);
            bloques.forEach((b, i) => b.classList.toggle("on", i < n));
        };
        const esperar = () => bloques[llenos] && bloques[llenos].classList.add("parpadea");
        if (sinMovimiento) {
            pintar(OBJETIVO);
            esperar();
            return;
        }
        pintar(0);
        animarNumero(0, OBJETIVO, 1900, 650, pintar, esperar);
    }


    // ---------- 3) Animaciones con el scroll ----------

    function cuandoSeVea(elementos, umbral, accion) {
        if (!("IntersectionObserver" in window)) {
            elementos.forEach(accion);
            return;
        }
        const observador = new IntersectionObserver((entradas) => {
            entradas.forEach((entrada) => {
                if (!entrada.isIntersecting) return;
                observador.unobserve(entrada.target);
                accion(entrada.target);
            });
        }, { threshold: umbral, rootMargin: "0px 0px -40px 0px" });
        elementos.forEach((el) => observador.observe(el));
    }

    function revelarAlBajar() {
        cuandoSeVea(document.querySelectorAll(".revelar"), 0.15, (el) => {
            // Los hermanos que aparecen a la vez lo hacen uno detrás de otro
            const hermanos = [...el.parentElement.children].filter((h) => h.classList.contains("revelar"));
            el.style.transitionDelay = (hermanos.indexOf(el) % 6) * 0.08 + "s";
            el.classList.add("visible");
            el.addEventListener("transitionend", () => { el.style.transitionDelay = ""; }, { once: true });
        });
    }

    // Efecto "descifrar": letras al azar que se van convirtiendo en el texto real
    const SIGNOS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789#%&$";
    function descifrar(el) {
        if (sinMovimiento || el.dataset.descifrando) return;
        el.dataset.descifrando = "1";
        const final = el.dataset.textoFinal || el.textContent;
        el.dataset.textoFinal = final;
        const pasos = 20;
        let paso = 0;
        const id = setInterval(() => {
            paso++;
            const revelados = Math.floor(final.length * paso / pasos);
            el.textContent = [...final].map((letra, i) =>
                i < revelados || letra === " " ? letra : SIGNOS[Math.floor(Math.random() * SIGNOS.length)]).join("");
            if (paso >= pasos) {
                clearInterval(id);
                el.textContent = final;
                delete el.dataset.descifrando;
            }
        }, 35);
    }

    function descifrarAlVer() {
        const elementos = document.querySelectorAll("[data-scramble]");
        cuandoSeVea(elementos, 0.6, descifrar);
        // El título clasificado se vuelve a descifrar al pasar el ratón por su tarjeta
        document.querySelectorAll(".tarjeta-secreta [data-scramble]").forEach((el) => {
            el.closest(".tarjeta").addEventListener("pointerenter", () => descifrar(el));
        });
    }

    // Manifiesto: cada palabra se "enciende" según bajas
    function manifiesto() {
        const parrafo = $("manifiesto-texto");
        if (!parrafo) return;
        const CLAVE = ["progreso", "dato"];
        const palabras = parrafo.textContent.trim().split(/\s+/);
        parrafo.textContent = "";
        const spans = palabras.map((palabra, i) => {
            const span = document.createElement("span");
            span.className = "palabra";
            if (CLAVE.some((c) => palabra.toLowerCase().startsWith(c))) span.classList.add("clave");
            span.textContent = palabra;
            parrafo.append(span, i < palabras.length - 1 ? " " : "");
            return span;
        });
        enScroll(() => {
            const caja = parrafo.getBoundingClientRect();
            const alto = window.innerHeight;
            // 0 cuando el texto asoma por abajo; 1 cuando su final pasa por el centro de la pantalla
            const progreso = Math.min(1, Math.max(0, (alto * 0.85 - caja.top) / (caja.height + alto * 0.35)));
            const encendidas = Math.round(progreso * spans.length);
            spans.forEach((s, i) => s.classList.toggle("on", i < encendidas));
        });
    }

    function contadores() {
        const elementos = document.querySelectorAll("[data-contar]");
        elementos.forEach((el) => { el.textContent = "0"; });
        cuandoSeVea(elementos, 0.6, (el) => {
            animarNumero(0, Number(el.dataset.contar), 1400, 0, (v) => { el.textContent = v; });
        });
    }

    function barraYProgreso() {
        const barra = document.querySelector(".barra");
        const linea = $("progreso-scroll");
        enScroll(() => {
            const maximo = document.documentElement.scrollHeight - window.innerHeight;
            linea.style.transform = "scaleX(" + (maximo > 0 ? window.scrollY / maximo : 0) + ")";
            barra.classList.toggle("con-fondo", window.scrollY > 30);
        });
    }


    // ---------- 4) Ratón ----------

    function magneticos() {
        if (!ratonFino || sinMovimiento) return;
        document.querySelectorAll(".magnetico").forEach((el) => {
            el.addEventListener("pointermove", (e) => {
                const caja = el.getBoundingClientRect();
                el.style.setProperty("--mx", ((e.clientX - caja.left - caja.width / 2) * 0.22).toFixed(1) + "px");
                el.style.setProperty("--my", ((e.clientY - caja.top - caja.height / 2) * 0.35).toFixed(1) + "px");
            });
            el.addEventListener("pointerleave", () => {
                el.style.setProperty("--mx", "0px");
                el.style.setProperty("--my", "0px");
            });
        });
    }

    function focosEnTarjetas() {
        if (!ratonFino) return;
        document.querySelectorAll(".tarjeta").forEach((tarjeta) => {
            tarjeta.addEventListener("pointermove", (e) => {
                const caja = tarjeta.getBoundingClientRect();
                tarjeta.style.setProperty("--x", e.clientX - caja.left + "px");
                tarjeta.style.setProperty("--y", e.clientY - caja.top + "px");
            });
        });
    }

    // ---------- 5) Formulario de acceso anticipado ----------

    function formulario() {
        const form = $("form-apuntate");
        if (!form) return;
        const email = $("email");
        const permiso = $("consentimiento");
        const mensaje = $("form-mensaje");
        const boton = $("boton-enviar");

        const mostrarError = (texto, campo) => {
            mensaje.textContent = texto;
            if (campo) {
                campo.setAttribute("aria-invalid", "true");
                campo.focus();
            }
        };
        email.addEventListener("input", () => email.removeAttribute("aria-invalid"));
        permiso.addEventListener("change", () => permiso.removeAttribute("aria-invalid"));

        form.addEventListener("submit", async (e) => {
            e.preventDefault();
            mensaje.textContent = "";
            email.value = email.value.trim();
            if (!email.value || !email.checkValidity()) return mostrarError("Escribe un email válido.", email);
            if (!permiso.checked) return mostrarError("Marca la casilla para que podamos escribirte.", permiso);

            boton.disabled = true;
            boton.classList.add("enviando");
            try {
                // Mismo formulario (con su token CSRF), pero pidiendo la respuesta en JSON
                const respuesta = await fetch(form.action, {
                    method: "POST",
                    headers: { "Accept": "application/json" },
                    body: new URLSearchParams(new FormData(form)),
                    credentials: "same-origin"
                });
                const datos = await respuesta.json().catch(() => ({}));
                if (respuesta.ok) {
                    mostrarExito();
                } else if (respuesta.status === 403) {
                    mostrarError("La página ha caducado. Recárgala e inténtalo de nuevo.");
                } else {
                    mostrarError(datos.error || "No hemos podido apuntarte. Inténtalo de nuevo.");
                }
            } catch (error) {
                mostrarError("Sin conexión. Revisa tu internet e inténtalo de nuevo.");
            } finally {
                boton.disabled = false;
                boton.classList.remove("enviando");
            }
        });

        function mostrarExito() {
            const exito = $("exito");
            form.hidden = true;
            exito.hidden = false;
            exito.focus({ preventScroll: true });
            chispas(exito.parentElement);
        }
    }

    // Cuadraditos que salen disparados al reservar plaza
    function chispas(contenedor) {
        if (sinMovimiento) return;
        for (let i = 0; i < 30; i++) {
            const chispa = document.createElement("span");
            chispa.className = "chispa";
            const angulo = Math.random() * Math.PI * 2;
            const distancia = 90 + Math.random() * 170;
            chispa.style.setProperty("--dx", (Math.cos(angulo) * distancia).toFixed(0) + "px");
            chispa.style.setProperty("--dy", (Math.sin(angulo) * distancia - 40).toFixed(0) + "px");
            chispa.style.setProperty("--giro", Math.round(Math.random() * 540 - 270) + "deg");
            chispa.style.animationDelay = (Math.random() * 0.15).toFixed(2) + "s";
            chispa.addEventListener("animationend", () => chispa.remove());
            contenedor.append(chispa);
        }
    }


    // ---------- 6) Compartir ----------

    function aviso(texto) {
        const caja = $("aviso");
        caja.textContent = texto;
        caja.hidden = false;
        clearTimeout(aviso.temporizador);
        aviso.temporizador = setTimeout(() => { caja.hidden = true; }, 3000);
    }

    function compartir() {
        document.querySelectorAll(".boton-compartir").forEach((boton) => {
            boton.addEventListener("click", async () => {
                const url = location.origin + "/pronto";
                // reto.js pone aquí la puntuación al terminar el Fitness Challenge
                const texto = boton.dataset.texto || "Algo grande se está cargando para el gimnasio. Entra antes que nadie en GymTracker:";
                if (navigator.share) {
                    try {
                        await navigator.share({ title: "GymTracker", text: texto, url: url });
                        return;
                    } catch (error) {
                        if (error.name === "AbortError") return;
                    }
                }
                try {
                    await navigator.clipboard.writeText(texto + " " + url);
                    aviso("Enlace copiado. Pásaselo a quien quieras.");
                } catch (error) {
                    aviso(url);
                }
            });
        });
    }


    // ---------- Arranque ----------

    manifiesto();
    magneticos();
    focosEnTarjetas();
    formulario();
    compartir();
    pantallaDeCarga();
})();