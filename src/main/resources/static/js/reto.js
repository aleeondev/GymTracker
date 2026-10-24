// ==========================================================
// RETO.JS - Fitness Challenge de la web de prelanzamiento (/pronto)
//
// Cada partida: 10 preguntas al azar (4 de entrenamiento, 3 de nutrición
// y 3 de gimnasio) de la lista PREGUNTAS, con 15 segundos cada una.
// Puntos por acierto: 100 + hasta 50 por rapidez + hasta 50 por racha
// (máximo 200 por pregunta, 2000 por partida: el servidor no acepta más).
// La mejor marca se guarda en este navegador y se añade al formulario de
// acceso anticipado. Texto siempre con textContent (nunca innerHTML).
// ==========================================================

(() => {
    "use strict";

    // c = categoría, p = pregunta, o = opciones (LA PRIMERA ES LA CORRECTA; se barajan al mostrarlas), e = explicación
    const PREGUNTAS = [
        // ---- Entrenamiento ----
        { c: "Entrenamiento", p: "¿Qué significa 1RM?",
          o: ["El peso máximo que puedes levantar en una sola repetición", "Una rutina de un solo músculo", "Un minuto de descanso entre series", "La primera serie de calentamiento"],
          e: "1RM es tu repetición máxima. Sirve de referencia para calcular cargas, por ejemplo entrenar al 75 % de tu 1RM." },
        { c: "Entrenamiento", p: "¿Qué es la sobrecarga progresiva?",
          o: ["Aumentar poco a poco el estímulo: peso, repeticiones o series", "Entrenar hasta el fallo en todas las series", "Cargar mucho peso desde el primer día", "Hacer más cardio que pesas"],
          e: "Para seguir progresando, el cuerpo necesita un estímulo algo mayor con el tiempo. Registrar tus entrenos es la forma de saber si lo consigues." },
        { c: "Entrenamiento", p: "¿Qué significa RIR?",
          o: ["Repeticiones en reserva", "Ritmo de intensidad real", "Rutina de iniciación rápida", "Repetición isométrica regulada"],
          e: "RIR 2 quiere decir que terminas la serie pudiendo hacer 2 repeticiones más con buena técnica." },
        { c: "Entrenamiento", p: "¿Qué músculo trabaja principalmente el hip thrust?",
          o: ["Glúteo mayor", "Cuádriceps", "Gemelos", "Dorsal ancho"],
          e: "El hip thrust es de los ejercicios que más activan el glúteo mayor, gracias a la extensión de cadera con carga." },
        { c: "Entrenamiento", p: "¿Cuál de estos músculos NO es protagonista en el press banca?",
          o: ["Bíceps femoral", "Pectoral mayor", "Tríceps", "Deltoides anterior"],
          e: "El bíceps femoral está en la parte trasera del muslo. El press banca trabaja pectoral, tríceps y la parte delantera del hombro." },
        { c: "Entrenamiento", p: "¿Qué músculo es el gran protagonista de las dominadas?",
          o: ["Dorsal ancho", "Pectoral mayor", "Trapecio superior", "Tríceps"],
          e: "El dorsal ancho tira de los brazos hacia abajo y hacia atrás. El bíceps y otros músculos de la espalda le ayudan." },
        { c: "Entrenamiento", p: "El peso muerto rumano trabaja sobre todo...",
          o: ["Isquiotibiales y glúteos", "Pectoral y hombros", "Bíceps y antebrazos", "Abdominales"],
          e: "Al bisagrar la cadera con las piernas casi estiradas, carga la parte posterior del muslo (femoral) y los glúteos." },
        { c: "Entrenamiento", p: "Para ganar músculo, ¿cuántas veces por semana conviene entrenar cada grupo muscular?",
          o: ["Al menos 2", "Solo 1, pero muy fuerte", "Todos los días", "Una vez cada 2 semanas"],
          e: "Los estudios indican que repartir el volumen en 2 o más sesiones por semana suele dar mejores resultados que concentrarlo en una." },
        { c: "Entrenamiento", p: "¿Cómo se calcula el volumen de un ejercicio?",
          o: ["Series × repeticiones × peso", "Peso máximo ÷ días de entreno", "Minutos de entreno × 2", "Repeticiones − series"],
          e: "Es una forma sencilla de medir cuánto trabajo haces. GymTracker lo calcula solo y te enseña cómo evoluciona cada semana." },
        { c: "Entrenamiento", p: "¿Qué cabeza del tríceps se estira más en las extensiones por encima de la cabeza?",
          o: ["La cabeza larga", "La cabeza lateral", "La cabeza medial", "El tríceps no tiene cabezas"],
          e: "La cabeza larga cruza el hombro, así que con el brazo por encima de la cabeza trabaja más estirada." },

        // ---- Nutrición ----
        { c: "Nutrición", p: "¿Cuántas kilocalorías aporta 1 gramo de proteína?",
          o: ["4 kcal", "9 kcal", "7 kcal", "2 kcal"],
          e: "Proteínas y carbohidratos aportan unas 4 kcal por gramo; la grasa, 9; el alcohol, 7." },
        { c: "Nutrición", p: "¿Cuántas kilocalorías aporta 1 gramo de grasa?",
          o: ["9 kcal", "4 kcal", "7 kcal", "12 kcal"],
          e: "La grasa es el macronutriente con más energía: unas 9 kcal por gramo, más del doble que la proteína o los carbohidratos." },
        { c: "Nutrición", p: "¿Cuánta proteína al día se suele recomendar si entrenas fuerza para ganar músculo?",
          o: ["De 1,6 a 2,2 g por kg de peso", "0,3 g por kg de peso", "5 g por kg de peso", "Solo la del batido"],
          e: "Las revisiones científicas sitúan el punto óptimo en torno a 1,6 g por kg al día, con margen hasta unos 2,2 g." },
        { c: "Nutrición", p: "¿Cuál es el suplemento con más evidencia para mejorar la fuerza?",
          o: ["Creatina monohidrato", "BCAA", "Quemagrasas", "Glutamina"],
          e: "La creatina monohidrato es de los suplementos más estudiados: mejora el rendimiento en esfuerzos cortos e intensos y es segura en personas sanas." },
        { c: "Nutrición", p: "¿Qué dosis diaria de creatina se usa habitualmente?",
          o: ["De 3 a 5 g", "50 g", "0,1 g", "Un bote a la semana"],
          e: "Con 3 a 5 g al día los músculos se saturan en unas semanas. La fase de carga es opcional." },
        { c: "Nutrición", p: "Para perder grasa, lo imprescindible es...",
          o: ["Estar en déficit calórico", "Dejar los carbohidratos", "Hacer solo cardio en ayunas", "Tomar quemagrasas"],
          e: "Sin gastar más energía de la que comes no se pierde grasa. La mejor dieta es la que puedes mantener." },
        { c: "Nutrición", p: "¿Qué macronutriente es el principal combustible en entrenamientos intensos?",
          o: ["Carbohidratos", "Proteínas", "Fibra", "Agua"],
          e: "El glucógeno muscular, que viene de los carbohidratos, es el combustible principal en los esfuerzos intensos." },
        { c: "Nutrición", p: "¿Cuánto antes de entrenar se suele tomar la cafeína para notar su efecto?",
          o: ["De 30 a 60 minutos", "Justo al terminar", "6 horas antes", "No tiene ningún efecto"],
          e: "La cafeína alcanza su pico en sangre entre 30 y 60 minutos después de tomarla." },

        // ---- Gimnasio ----
        { c: "Gimnasio", p: "¿Cuánto pesa una barra olímpica estándar de hombre?",
          o: ["20 kg", "10 kg", "15 kg", "25 kg"],
          e: "La barra olímpica masculina pesa 20 kg (la femenina, 15 kg). Cuéntala siempre al sumar el peso total." },
        { c: "Gimnasio", p: "¿Qué es una superserie?",
          o: ["Dos ejercicios seguidos sin descanso entre ellos", "Una serie con tu peso máximo", "Una serie de más de 50 repeticiones", "Entrenar dos veces el mismo día"],
          e: "Encadenar dos ejercicios (por ejemplo, bíceps y tríceps) ahorra tiempo y sube la intensidad." },
        { c: "Gimnasio", p: "¿Cuánto se suele descansar entre series pesadas de fuerza (1 a 5 repeticiones)?",
          o: ["De 2 a 5 minutos", "10 segundos", "30 segundos", "15 minutos"],
          e: "Con cargas muy altas, descansar de 2 a 5 minutos ayuda a rendir igual de bien en la siguiente serie." },
        { c: "Gimnasio", p: "En competición, ¿cuánto pesa el disco olímpico rojo?",
          o: ["25 kg", "20 kg", "15 kg", "10 kg"],
          e: "Código oficial: rojo 25 kg, azul 20, amarillo 15 y verde 10. La calculadora de discos de GymTracker te lo pone fácil." },
        { c: "Gimnasio", p: "¿Qué son las agujetas (DOMS)?",
          o: ["Dolor muscular que aparece uno o dos días después de entrenar", "Cristales de ácido láctico en el músculo", "Una lesión que obliga a parar", "La prueba de que el entreno fue perfecto"],
          e: "Se deben a pequeños daños e inflamación en el músculo, no a cristales de ácido láctico. Tenerlas no significa que el entreno fuera mejor." },
        { c: "Gimnasio", p: "¿Qué significa llegar al fallo muscular?",
          o: ["No poder hacer otra repetición con buena técnica", "Hacer mal un ejercicio", "Saltarse un día de entreno", "Bajar el peso a mitad de serie"],
          e: "Es el punto en el que, con buena técnica, ya no sale otra repetición. Útil a veces, agotador si se abusa." },
        { c: "Gimnasio", p: "¿Cuántas horas de sueño se recomiendan a un adulto?",
          o: ["De 7 a 9 horas", "De 4 a 5 horas", "De 11 a 12 horas", "Da igual si entrenas"],
          e: "Dormir bien mejora la recuperación, el rendimiento y hasta el control del apetito." },
        { c: "Gimnasio", p: "¿Cuál es la mejor forma de calentar antes de una sentadilla pesada?",
          o: ["Series de aproximación subiendo el peso poco a poco", "Estiramientos estáticos largos", "Empezar directamente con el peso máximo", "Correr 45 minutos"],
          e: "Las series de aproximación preparan músculos, articulaciones y técnica para el peso de trabajo." }
    ];

    const REPARTO = { "Entrenamiento": 4, "Nutrición": 3, "Gimnasio": 3 };
    const TOTAL = 10;
    const SEGUNDOS = 15;
    const LETRAS = "ABCD";
    const RANGOS = [
        { min: 1500, nombre: "Leyenda", texto: "Poco te podemos enseñar... salvo tus propios números." },
        { min: 1000, nombre: "Bestia del gym", texto: "Nivel serio. Tu progreso merece un registro a la altura." },
        { min: 500, nombre: "Constante", texto: "Sabes de qué va esto. Ahora toca medirlo." },
        { min: 0, nombre: "Calentando", texto: "Buen comienzo. GymTracker te ayudará a convertir dudas en datos." }
    ];
    const CLAVE_MEJOR = "gt_reto_mejor";

    // Recompensas por puntos (se acumulan; cuenta la mejor marca). Las mismas que la
    // sección "Tus puntos tienen premio" de pronto.html: si cambias una, cambia la otra.
    const PREMIOS = [
        { min: 500, texto: () => "Entras en la primera tanda de invitaciones a la beta" },
        { min: 1000, texto: (rango) => "Insignia «" + rango + "» junto a tu nombre" },
        { min: 1500, texto: () => "Pruebas las novedades antes que nadie" }
    ];

    function rangoDe(puntos) {
        return RANGOS.find((r) => puntos >= r.min);
    }

    // Lista de recompensas para unos puntos. La de fundador se gana al reservar plaza.
    function premiosDe(puntos, alReservar) {
        const rango = rangoDe(puntos).nombre;
        const fundador = "Insignia de Miembro fundador" + (alReservar ? " (al reservar tu plaza)" : "");
        return [fundador].concat(PREMIOS.filter((p) => puntos >= p.min).map((p) => p.texto(rango)));
    }

    function pintarLista(lista, textos) {
        lista.replaceChildren(...textos.map((texto) => {
            const li = document.createElement("li");
            li.textContent = texto;
            return li;
        }));
    }

    const $ = (id) => document.getElementById(id);
    const pantallas = { inicio: $("reto-inicio"), juego: $("reto-juego"), final: $("reto-final") };
    if (!pantallas.inicio) return;

    let partida = null;

    function leerMejor() {
        try { return Number(localStorage.getItem(CLAVE_MEJOR)) || 0; } catch (e) { return 0; }
    }
    function guardarMejor(puntos) {
        try { localStorage.setItem(CLAVE_MEJOR, String(puntos)); } catch (e) { return; }
    }

    function barajar(lista) {
        const copia = [...lista];
        for (let i = copia.length - 1; i > 0; i--) {
            const j = Math.floor(Math.random() * (i + 1));
            [copia[i], copia[j]] = [copia[j], copia[i]];
        }
        return copia;
    }

    function elegirPreguntas() {
        const elegidas = Object.entries(REPARTO).flatMap(([categoria, cuantas]) =>
            barajar(PREGUNTAS.filter((p) => p.c === categoria)).slice(0, cuantas));
        return barajar(elegidas);
    }

    function mostrar(nombre) {
        Object.entries(pantallas).forEach(([clave, el]) => { el.hidden = clave !== nombre; });
    }
    // ---------- Partida ----------

    function empezar() {
        partida = { preguntas: elegirPreguntas(), indice: 0, puntos: 0, racha: 0, mejorRacha: 0, aciertos: 0 };
        $("reto-puntos").textContent = "0";
        $("reto-racha").hidden = true;
        mostrar("juego");
        $("reto-panel").scrollIntoView({ behavior: "smooth", block: "nearest" });
        mostrarPregunta();
    }

    function mostrarPregunta() {
        const pregunta = partida.preguntas[partida.indice];
        partida.respondida = false;
        $("reto-numero").textContent = partida.indice + 1;
        $("reto-categoria").textContent = pregunta.c;
        $("reto-pregunta").textContent = pregunta.p;
        $("reto-respuesta").hidden = true;

        const opciones = barajar(pregunta.o.map((texto, i) => ({ texto: texto, correcta: i === 0 })));
        partida.botones = opciones.map((opcion, i) => {
            const boton = document.createElement("button");
            boton.type = "button";
            boton.className = "opcion";
            const letra = document.createElement("span");
            letra.className = "opcion-letra";
            letra.textContent = LETRAS[i];
            const texto = document.createElement("span");
            texto.textContent = opcion.texto;
            boton.append(letra, texto);
            boton.addEventListener("click", () => responder(boton));
            if (opcion.correcta) partida.correcta = boton;
            return boton;
        });
        $("reto-opciones").replaceChildren(...partida.botones);
        arrancarTiempo();
    }

    function arrancarTiempo() {
        const barra = $("reto-tiempo-barra");
        const caja = barra.parentElement;
        caja.classList.remove("urgente");
        partida.inicio = performance.now();
        cancelAnimationFrame(partida.temporizador);
        const tic = (ahora) => {
            const restante = Math.max(0, SEGUNDOS - (ahora - partida.inicio) / 1000);
            barra.style.transform = "scaleX(" + (restante / SEGUNDOS) + ")";
            caja.classList.toggle("urgente", restante <= 5);
            if (restante <= 0) {
                responder(null);
                return;
            }
            partida.temporizador = requestAnimationFrame(tic);
        };
        partida.temporizador = requestAnimationFrame(tic);
    }

    // boton = opción elegida, o null si se acabó el tiempo
    function responder(boton) {
        if (partida.respondida) return;
        partida.respondida = true;
        cancelAnimationFrame(partida.temporizador);
        $("reto-tiempo-barra").parentElement.classList.remove("urgente");

        const restante = Math.max(0, SEGUNDOS - (performance.now() - partida.inicio) / 1000);
        const acierto = boton === partida.correcta;
        partida.botones.forEach((b) => {
            b.disabled = true;
            if (b !== partida.correcta && b !== boton) b.classList.add("apagada");
        });
        partida.correcta.classList.add("correcta");

        const veredicto = $("reto-veredicto");
        if (acierto) {
            partida.racha++;
            partida.aciertos++;
            partida.mejorRacha = Math.max(partida.mejorRacha, partida.racha);
            const ganados = 100 + Math.round(50 * restante / SEGUNDOS) + Math.min(50, (partida.racha - 1) * 10);
            partida.puntos += ganados;
            veredicto.textContent = "Correcto · +" + ganados + " pts";
            veredicto.className = "reto-veredicto acierto";
            puntosFlotantes("+" + ganados);
        } else {
            partida.racha = 0;
            if (boton) boton.classList.add("incorrecta");
            veredicto.textContent = boton ? "Fallo" : "Se acabó el tiempo";
            veredicto.className = "reto-veredicto fallo";
        }

        $("reto-puntos").textContent = partida.puntos;
        $("reto-racha").hidden = partida.racha < 2;
        $("reto-racha-num").textContent = partida.racha;
        $("reto-explicacion").textContent = partida.preguntas[partida.indice].e;

        const siguiente = $("reto-siguiente");
        siguiente.firstChild.textContent = partida.indice === TOTAL - 1 ? "Ver resultado " : "Siguiente ";
        $("reto-respuesta").hidden = false;
        siguiente.focus({ preventScroll: true });
    }

    function puntosFlotantes(texto) {
        const flotante = document.createElement("span");
        flotante.className = "puntos-flotantes";
        flotante.textContent = texto;
        flotante.addEventListener("animationend", () => flotante.remove());
        document.querySelector(".reto-puntos").append(flotante);
    }

    function siguiente() {
        partida.indice++;
        if (partida.indice < TOTAL) mostrarPregunta();
        else terminar();
    }


    // ---------- Resultado ----------

    function terminar() {
        const puntos = partida.puntos;
        const rango = RANGOS.find((r) => puntos >= r.min);
        mostrar("final");
        $("reto-rango").textContent = rango.nombre;
        $("reto-resumen").textContent = partida.aciertos + " de " + TOTAL + " aciertos · mejor racha: "
            + partida.mejorRacha + ". " + rango.texto;
        contarHasta($("reto-total"), puntos);

        const mejor = Math.max(puntos, leerMejor());
        guardarMejor(mejor);
        pintarMejor(mejor);

        // Lo que desbloquea esta partida y cuánto falta para el siguiente rango
        pintarLista($("reto-premios-lista"), premiosDe(puntos, true));
        const siguienteRango = [...RANGOS].reverse().find((r) => r.min > puntos);
        let proximo = siguienteRango
            ? "Te faltan " + (siguienteRango.min - puntos) + " pts para " + siguienteRango.nombre + "."
            : "Rango máximo. No queda nada por desbloquear... de momento.";
        if (mejor > puntos) proximo += " Para tu plaza cuenta tu mejor marca: " + mejor + " pts.";
        $("reto-proximo").textContent = proximo;

        const compartir = document.querySelector("#reto-final .boton-compartir");
        if (compartir) {
            compartir.dataset.texto = "He hecho " + puntos + " puntos en el Fitness Challenge de GymTracker ("
                + rango.nombre + "). ¿Me superas?";
        }
        $("reto-panel").scrollIntoView({ behavior: "smooth", block: "nearest" });
    }

    function contarHasta(el, objetivo) {
        let inicio = null;
        const paso = (t) => {
            inicio ??= t;
            const p = Math.min(1, (t - inicio) / 1300);
            el.textContent = Math.round(objetivo * (1 - Math.pow(1 - p, 3)));
            if (p < 1) requestAnimationFrame(paso);
        };
        requestAnimationFrame(paso);
    }

    // Mejor marca: en la pantalla de inicio y en el formulario de acceso anticipado
    function pintarMejor(mejor) {
        if (mejor <= 0) return;
        $("reto-mejor").hidden = false;
        $("reto-mejor-puntos").textContent = mejor;
        const campo = $("puntuacion");
        if (campo) {
            campo.value = Math.min(2000, mejor);
            $("puntos-guardados").hidden = false;
            $("puntos-guardados-num").textContent = Math.min(2000, mejor);
            $("puntos-guardados-rango").textContent = rangoDe(mejor).nombre;
        }
        const exito = $("exito-premios");
        if (exito) pintarLista(exito, premiosDe(mejor, false));
        // Tarjetas de "Tus puntos tienen premio": en verde las conseguidas
        document.querySelectorAll(".premio[data-min]").forEach((premio) => {
            const conseguido = mejor >= Number(premio.dataset.min);
            const chip = premio.querySelector(".premio-chip");
            chip.dataset.original ??= chip.textContent;
            chip.textContent = conseguido ? "Conseguido" : chip.dataset.original;
            premio.classList.toggle("conseguido", conseguido);
        });
    }


    // ---------- Controles ----------

    $("reto-empezar").addEventListener("click", empezar);
    $("reto-repetir").addEventListener("click", empezar);
    $("reto-siguiente").addEventListener("click", siguiente);

    // Teclado: 1-4 o A-D para responder
    document.addEventListener("keydown", (e) => {
        if (!partida || partida.respondida || pantallas.juego.hidden) return;
        if (e.target.closest("input, textarea, select") || e.ctrlKey || e.metaKey || e.altKey) return;
        const tecla = e.key.toUpperCase();
        const indice = "1234".includes(tecla) ? Number(tecla) - 1 : LETRAS.indexOf(tecla);
        if (indice >= 0 && partida.botones[indice]) {
            e.preventDefault();
            responder(partida.botones[indice]);
        }
    });

    pintarMejor(leerMejor());
})();