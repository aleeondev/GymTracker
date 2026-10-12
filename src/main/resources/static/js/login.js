const formulario = document.getElementById("loginForm");
const mensaje = document.getElementById("mensaje");
const submitBtn = document.getElementById("submitBtn");

formulario.addEventListener("submit", async function (event) {

    event.preventDefault();

    const email = document.getElementById("email").value;
    const password = document.getElementById("password").value;

    mensaje.textContent = "Iniciando sesión...";
    mensaje.classList.remove("is-error", "is-success");

    submitBtn.classList.add("is-loading");
    submitBtn.disabled = true;

    const { data, error } = await supabaseClient.auth.signInWithPassword({
        email: email,
        password: password
    });

    if (error) {
        mensaje.textContent = error.message;
        mensaje.classList.add("is-error");

        submitBtn.classList.remove("is-loading");
        submitBtn.disabled = false;
        return;
    }

    console.log(data);

    mensaje.textContent = "Inicio de sesión correcto.";
    mensaje.classList.add("is-success");

    window.location.href = "/dashboard";
});