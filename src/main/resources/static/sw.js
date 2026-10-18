// ==========================================================
// SW.JS - Service Worker de Gym Tracker
//
// Un service worker es un script que el navegador instala aparte de la
// página y que se coloca "entre" la app y la red: ve pasar cada petición.
// Es lo que permite instalar la web como app y reaccionar sin conexión.
//
// Decisión de seguridad: NUNCA guardamos en caché páginas con datos del
// usuario (panel, historial...). Si alguien usa tu móvil o cierras sesión,
// no deben quedar copias guardadas. Solo cacheamos la página "sin conexión"
// y los archivos que ella necesita.
//
// Si cambias algo de esta lista, sube la versión (v1 -> v2) para que el
// navegador descarte la caché antigua.
// ==========================================================

const CACHE = "gymtracker-v13";
const PAGINA_OFFLINE = "/offline.html";
const ARCHIVOS_OFFLINE = [
    PAGINA_OFFLINE,
    "/css/styles.css",
    "/css/app.css?v=13",
    "/img/favicon-192.png"
];

// 1) Instalación: descarga y guarda los archivos de la página offline.
self.addEventListener("install", (evento) => {
    evento.waitUntil(
        caches.open(CACHE)
            .then((cache) => cache.addAll(ARCHIVOS_OFFLINE))
            .then(() => self.skipWaiting())
    );
});

// 2) Activación: borra las cachés de versiones anteriores.
self.addEventListener("activate", (evento) => {
    evento.waitUntil(
        caches.keys()
            .then((claves) => Promise.all(
                claves.filter((clave) => clave !== CACHE).map((clave) => caches.delete(clave))))
            .then(() => self.clients.claim())
    );
});

// 3) Cada petición va siempre a la red. Solo si falla (sin conexión)
//    respondemos con lo guardado: la página offline o sus estilos.
self.addEventListener("fetch", (evento) => {
    const peticion = evento.request;
    if (peticion.method !== "GET") return; // los POST (guardar entreno) no se tocan

    if (peticion.mode === "navigate") {
        evento.respondWith(fetch(peticion).catch(() => caches.match(PAGINA_OFFLINE)));
        return;
    }

    const url = new URL(peticion.url);
    if (ARCHIVOS_OFFLINE.includes(url.pathname + url.search)) {
        evento.respondWith(fetch(peticion).catch(() => caches.match(peticion)));
    }
});
