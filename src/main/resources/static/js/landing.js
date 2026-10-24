// ==========================================================
// LANDING.JS
// Animaciones de la landing con GSAP (js/vendor/gsap-*.js):
//   - gsap:          el motor. gsap.to / gsap.from / gsap.timeline
//   - ScrollTrigger: lanza (o "ata") animaciones al hacer scroll
//   - SplitText:     parte un texto en líneas / palabras / letras para animarlas
//
// Cómo funciona por encima:
//   1) landing-pre.js pone la clase "lp-anim" en <html> si se puede animar.
//   2) inicio.css esconde el hero mientras tanto (para que no parpadee).
//   3) Aquí, GSAP lo coloca en su posición inicial y lo anima hasta su sitio.
// Si GSAP no carga o la persona prefiere menos movimiento, se quita la
// clase y la página se ve entera y quieta. Nada depende de las animaciones.
// ==========================================================

document.addEventListener("DOMContentLoaded", () => {
    iniciarCabecera();
    iniciarBrilloTarjetas();

    const raiz = document.documentElement;
    const hayGsap = window.gsap && window.ScrollTrigger && window.SplitText;
    if (!hayGsap || !raiz.classList.contains("lp-anim")) {
        raiz.classList.remove("lp-anim");
        return;
    }

    gsap.registerPlugin(ScrollTrigger, SplitText);

    // SplitText mide dónde corta cada línea: hay que esperar a que estén las
    // fuentes de Google (si tardan más de 0,8 s, se anima igualmente).
    const fuentes = document.fonts ? document.fonts.ready : Promise.resolve();
    const espera = new Promise((resolver) => setTimeout(resolver, 800));
    Promise.race([fuentes, espera]).then(() => {
        animarHero();
        animarTitulosAlHacerScroll();
        revelarAlHacerScroll();
        animarGraficaDeLinea();
        animarPasos();
        animarNumeros();
        animarTelefono();
        iniciarParallaxHero();
    });
});


// -----------------------------------------------------------
// 1) Cabecera con sombra al hacer scroll (sin GSAP)
// -----------------------------------------------------------
function iniciarCabecera() {
    const cabecera = document.querySelector("header");
    if (!cabecera) return;

    const alHacerScroll = () => cabecera.classList.toggle("scrolled", window.scrollY > 10);
    alHacerScroll();
    window.addEventListener("scroll", alHacerScroll, { passive: true });
}


// -----------------------------------------------------------
// 2) Brillo que sigue al ratón en las tarjetas de funciones (sin GSAP).
//    Solo cambia dos variables CSS; el degradado está en inicio.css.
// -----------------------------------------------------------
function iniciarBrilloTarjetas() {
    document.querySelectorAll(".lp-tarjeta").forEach((tarjeta) => {
        tarjeta.addEventListener("mousemove", (evento) => {
            const caja = tarjeta.getBoundingClientRect();
            tarjeta.style.setProperty("--x", `${evento.clientX - caja.left}px`);
            tarjeta.style.setProperty("--y", `${evento.clientY - caja.top}px`);
        });
    });
}


// -----------------------------------------------------------
// 3) Entrada del hero: una "línea de tiempo" (timeline) es una lista de
//    animaciones encadenadas. El número o "-=0.6" del final de cada paso
//    dice cuándo empieza (en segundos, o solapado con el anterior).
// -----------------------------------------------------------
function animarHero() {
    const titulo = document.querySelector(".lp-titulo");
    if (!titulo) return;

    // Cada línea del título se parte en letras. "mask" mete cada línea en una
    // caja que recorta lo que sobresale: las letras "suben" desde debajo.
    // SplitText ya deja el texto completo legible para lectores de pantalla.
    const partido = SplitText.create(titulo.children, { type: "lines, chars", mask: "lines" });

    const barras = document.querySelectorAll(".lp-barras span");
    const contadores = document.querySelectorAll(".lp-maqueta [data-contar]");

    const tl = gsap.timeline({ defaults: { ease: "power4.out" } });

    tl.set([titulo, ".lp-hero-visual"], { autoAlpha: 1 })
        .from(partido.chars, { yPercent: 110, duration: 1, stagger: 0.025 })
        .from("[data-anim]", { y: 24, autoAlpha: 0, duration: 0.8, stagger: 0.1 }, "-=0.7")

        // La maqueta del panel entra desde abajo y algo inclinada (en 3D)
        .from(".lp-maqueta", { y: 80, rotationX: 16, autoAlpha: 0, duration: 1.3, ease: "expo.out" }, 0.25)
        .from(barras, { scaleY: 0, duration: 0.9, stagger: 0.06, ease: "back.out(1.6)" }, 0.75)
        .from(".lp-flotante", { scale: 0.6, autoAlpha: 0, duration: 0.7, stagger: 0.2, ease: "back.out(2)" }, 1.1);

    contadores.forEach((contador) => tl.add(contar(contador, 1.6), 0.7));

    // Cuando han entrado, las tarjetitas flotan arriba y abajo sin parar.
    // Se usa yPercent (no "y") para no pelear con el parallax del ratón, que usa x / y.
    tl.add(() => {
        gsap.to(".lp-flotante-record", { yPercent: -12, duration: 2.4, ease: "sine.inOut", repeat: -1, yoyo: true });
        gsap.to(".lp-flotante-serie", { yPercent: 12, duration: 2.8, ease: "sine.inOut", repeat: -1, yoyo: true });
    });
}


// -----------------------------------------------------------
// 4) Títulos de sección: sus líneas suben al aparecer en pantalla.
//    "start: top 85%" = cuando la parte de arriba del título llega al 85 %
//    de la altura de la ventana. "once" = solo la primera vez.
// -----------------------------------------------------------
function animarTitulosAlHacerScroll() {
    document.querySelectorAll("[data-split-scroll]").forEach((titulo) => {
        const partido = SplitText.create(titulo, { type: "lines", mask: "lines" });
        gsap.from(partido.lines, {
            yPercent: 105,
            duration: 1,
            stagger: 0.12,
            ease: "power4.out",
            scrollTrigger: { trigger: titulo, start: "top 85%", once: true }
        });
    });
}


// -----------------------------------------------------------
// 5) Resto de bloques: aparecen subiendo un poco, en grupos.
//    ScrollTrigger.batch junta los que entran a la vez en pantalla y los
//    anima uno detrás de otro (stagger), como una ola.
// -----------------------------------------------------------
function revelarAlHacerScroll() {
    const bloques = gsap.utils.toArray(
        ".lp-etiqueta, .lp-encabezado > p, .lp-tarjeta, .lp-paso, .lp-numero, " +
        ".lp-movil-texto > p, .lp-instalar-paso, .lp-proyecto-texto > *, .lp-faq details, " +
        ".lp-cta-etiqueta, .lp-cta-boton"
    );

    gsap.set(bloques, { autoAlpha: 0, y: 40 });

    ScrollTrigger.batch(bloques, {
        start: "top 90%",
        once: true,
        onEnter: (lote) => gsap.to(lote, {
            autoAlpha: 1,
            y: 0,
            duration: 0.8,
            stagger: 0.09,
            ease: "power3.out",
            // Al terminar, se quita el transform que puso GSAP para que el
            // efecto hover de inicio.css funcione como siempre
            clearProps: "transform"
        })
    });
}


// -----------------------------------------------------------
// 6) La línea de la tarjeta "progreso" se dibuja sola.
//    Truco: el trazo es una línea discontinua con un solo guion igual de
//    largo que el trazo (pathLength="1"). Desplazando ese guion de 1 a 0,
//    parece que se va dibujando.
// -----------------------------------------------------------
function animarGraficaDeLinea() {
    const tarjeta = document.querySelector(".lp-tarjeta-alta");
    if (!tarjeta) return;

    const tl = gsap.timeline({ scrollTrigger: { trigger: tarjeta, start: "top 75%", once: true } });
    tl.fromTo(".lp-linea-trazo", { strokeDashoffset: 1 }, { strokeDashoffset: 0, duration: 1.8, ease: "power2.inOut" })
        .from(".lp-linea-area", { autoAlpha: 0, duration: 1 }, "-=0.8");
}


// -----------------------------------------------------------
// 7) "Cómo funciona": la línea roja avanza con el scroll ("scrub" = la
//    animación va atada a la barra de desplazamiento, adelante y atrás) y
//    cada número se enciende cuando la línea llega a él.
//    gsap.matchMedia: unas animaciones para ordenador y otras para móvil.
// -----------------------------------------------------------
function animarPasos() {
    const lista = document.querySelector(".lp-pasos");
    if (!lista) return;
    const pasos = gsap.utils.toArray(".lp-paso");
    const mm = gsap.matchMedia();

    mm.add("(min-width: 801px)", () => {
        gsap.fromTo(lista, { "--progreso": 0 }, {
            "--progreso": 1,
            ease: "none",
            scrollTrigger: {
                trigger: lista,
                start: "top 75%",
                end: "bottom 55%",
                scrub: 0.6,
                onUpdate: (st) => pasos.forEach((paso, i) => {
                    paso.classList.toggle("activo", st.progress >= (i / (pasos.length - 1)) * 0.98 + 0.01);
                })
            }
        });
    });

    // En móvil van en columna y sin línea: cada número se enciende al llegar a él
    mm.add("(max-width: 800px)", () => {
        pasos.forEach((paso) => ScrollTrigger.create({
            trigger: paso,
            start: "top 70%",
            toggleClass: "activo"
        }));
    });
}


// -----------------------------------------------------------
// 8) Números que cuentan desde 0 al aparecer
// -----------------------------------------------------------
function animarNumeros() {
    document.querySelectorAll(".lp-numeros [data-contar]").forEach((numero) => {
        ScrollTrigger.create({
            trigger: numero,
            start: "top 85%",
            once: true,
            onEnter: () => contar(numero, 2)
        });
    });
}

/** Anima el texto de un elemento de 0 a su data-contar (8420 se ve como 8.420). */
function contar(elemento, duracion) {
    const final = Number(elemento.dataset.contar);
    const valor = { actual: 0 };
    elemento.textContent = "0";
    return gsap.to(valor, {
        actual: final,
        duration: duracion,
        ease: "power2.out",
        onUpdate: () => {
            elemento.textContent = String(Math.round(valor.actual)).replace(/\B(?=(\d{3})+(?!\d))/g, ".");
        }
    });
}


// -----------------------------------------------------------
// 9) El móvil de la sección "App móvil" entra girando y las filas del
//    entreno aparecen una a una. Después, el descanso cuenta hacia atrás.
// -----------------------------------------------------------
function animarTelefono() {
    const telefono = document.querySelector(".lp-telefono");
    if (!telefono) return;

    const tl = gsap.timeline({ scrollTrigger: { trigger: ".lp-movil", start: "top 70%", once: true } });
    tl.from(telefono, { y: 120, rotation: 8, autoAlpha: 0, duration: 1.2, ease: "expo.out" })
        .from(".lp-tel-ejercicio, .lp-tel-descanso", { x: 30, autoAlpha: 0, duration: 0.6, stagger: 0.12, ease: "power3.out" }, "-=0.7")
        .add(cuentaAtrasDescanso);
}

function cuentaAtrasDescanso() {
    const reloj = document.querySelector(".lp-tel-descanso strong");
    if (!reloj) return;
    const tiempo = { segundos: 90 };
    // 90 s reales en bucle: ease "none" = velocidad constante, como un reloj
    gsap.to(tiempo, {
        segundos: 0,
        duration: 90,
        ease: "none",
        repeat: -1,
        onUpdate: () => {
            const s = Math.ceil(tiempo.segundos);
            reloj.textContent = `${String(Math.floor(s / 60)).padStart(2, "0")}:${String(s % 60).padStart(2, "0")}`;
        }
    });
}


// -----------------------------------------------------------
// 10) Parallax con el ratón en el hero: la maqueta se inclina un poco y
//     cada tarjetita se mueve más o menos según su data-parallax.
//     gsap.quickTo crea una función que anima suavemente hacia el valor que
//     le pases: ideal para eventos que se disparan muchas veces (mousemove).
//     Solo en ordenador (puntero fino); en móvil no hay ratón.
// -----------------------------------------------------------
function iniciarParallaxHero() {
    const hero = document.querySelector(".lp-hero");
    const maqueta = document.querySelector(".lp-maqueta");
    if (!hero || !maqueta) return;

    gsap.matchMedia().add("(pointer: fine) and (min-width: 1001px)", () => {
        const capas = gsap.utils.toArray("[data-parallax]").map((capa) => ({
            factor: Number(capa.dataset.parallax),
            x: gsap.quickTo(capa, "x", { duration: 0.8, ease: "power3.out" }),
            y: gsap.quickTo(capa, "y", { duration: 0.8, ease: "power3.out" })
        }));
        const girarY = gsap.quickTo(maqueta, "rotationY", { duration: 0.8, ease: "power3.out" });
        const girarX = gsap.quickTo(maqueta, "rotationX", { duration: 0.8, ease: "power3.out" });

        const mover = (evento) => {
            const caja = hero.getBoundingClientRect();
            // Posición del ratón respecto al centro del hero, de -0,5 a 0,5
            const rx = (evento.clientX - caja.left) / caja.width - 0.5;
            const ry = (evento.clientY - caja.top) / caja.height - 0.5;
            capas.forEach((capa) => {
                capa.x(rx * 16 * capa.factor);
                capa.y(ry * 16 * capa.factor);
            });
            girarY(rx * 8);
            girarX(-ry * 6);
        };
        const volver = () => {
            capas.forEach((capa) => { capa.x(0); capa.y(0); });
            girarY(0);
            girarX(0);
        };

        hero.addEventListener("mousemove", mover);
        hero.addEventListener("mouseleave", volver);
        // Si la ventana se hace pequeña, matchMedia ejecuta esto y quita los eventos
        return () => {
            hero.removeEventListener("mousemove", mover);
            hero.removeEventListener("mouseleave", volver);
        };
    });
}
