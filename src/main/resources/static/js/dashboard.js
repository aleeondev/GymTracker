
async function cargarDashboard() {
    const saludo = document.getElementById("saludo");
    const mensaje = document.getElementById("mensajeDashboard");

    const { data, error } = await supabaseClient.auth.getUser();

    if (error || !data.user) {
        window.location.href = "/login";
        return;
    }

    const user = data.user;

    const { data: perfil, error: errorPerfil } = await supabaseClient
        .from("profiles")
        .select("name")
        .eq("id", user.id)
        .single();

    if (errorPerfil) {
        console.error("Error al cargar el perfil:", errorPerfil);
        saludo.textContent = "Bienvenido a Gym Tracker";
        mensaje.textContent = "No hemos podido cargar tu perfil.";
    } else {
        const nombre = perfil.name || "deportista";
        saludo.textContent = "Hola, " + nombre;
        mensaje.textContent = "Tu espacio personal está listo.";
    }
}

document.getElementById("cerrarSesion").onclick = async function () {
    const boton = this;
    boton.disabled = true;
    const { error } = await supabaseClient.auth.signOut();
    if (error) {
        console.error("Error al cerrar sesión:", error);
        boton.disabled = false;
        return;
    }
    window.location.href = "/login";
};

cargarDashboard();