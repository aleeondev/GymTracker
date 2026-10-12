async function cargarUsuario() {

    const { data: { user } } = await supabaseClient.auth.getUser();

    if (user) {
        document.getElementById("cerrarSesion").style.display = "block";
    } else {
        return;
    }

    const { data: perfil, error } = await supabaseClient
    .from("profiles")
    .select("name")
    .eq("id", user.id)
    .single();

    console.log(perfil);

    document.getElementById("saludo").textContent = "Hola, " + perfil.name;
}

cargarUsuario();

document.getElementById("cerrarSesion").onclick = async function() {
    await supabaseClient.auth.signOut();
    window.location.href = "/login";
}