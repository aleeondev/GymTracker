// ==========================================================
// AUTH-SESSION.JS
// Puente entre Supabase (login en el navegador) y Spring (servidor).
//
// Supabase guarda la sesión en el navegador. Spring no puede leer eso,
// así que copiamos el "access token" (un JWT firmado por Supabase) en
// la cookie "gt_session". El navegador la envía en cada petición y
// Spring comprueba la firma (ver SecurityConfig.java).
//
// Se carga en login, registro y todas las páginas de la app.
// ==========================================================

const GT_COOKIE = "gt_session";

/** Guarda (o borra, si no hay sesión) el token en la cookie. */
function guardarCookieSesion(session) {
    const seguro = location.protocol === "https:" ? "; Secure" : "";

    if (session && session.access_token) {
        // La cookie dura lo mismo que el token (normalmente 1 hora).
        const segundos = Math.max(0, (session.expires_at || 0) - Math.floor(Date.now() / 1000));
        document.cookie = `${GT_COOKIE}=${session.access_token}; Path=/; Max-Age=${segundos}; SameSite=Lax${seguro}`;
    } else {
        document.cookie = `${GT_COOKIE}=; Path=/; Max-Age=0; SameSite=Lax${seguro}`;
    }
}

/**
 * Pide la sesión a Supabase. Si el token ha caducado, Supabase lo renueva
 * solo (con el "refresh token"). Después actualiza la cookie.
 */
async function sesionActualizada() {
    const { data } = await supabaseClient.auth.getSession();
    guardarCookieSesion(data.session);
    return data.session;
}

// Cada vez que Supabase inicia, renueva o cierra la sesión, la cookie se actualiza.
supabaseClient.auth.onAuthStateChange((_evento, session) => {
    guardarCookieSesion(session);
});

/** Cierra la sesión en Supabase, borra la cookie y vuelve a la portada. */
async function cerrarSesion() {
    await supabaseClient.auth.signOut();
    guardarCookieSesion(null);
    window.location.href = "/";
}