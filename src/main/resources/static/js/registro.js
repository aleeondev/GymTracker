const formulario = document.getElementById("registroForm");
const mensaje = document.getElementById("mensaje");
const submitBtn = document.getElementById("submitBtn");

formulario.addEventListener("submit", async function (event) {

    event.preventDefault();

    const nombre = document.getElementById("nombre").value;
    const email = document.getElementById("email").value;
    const password = document.getElementById("password").value;

    mensaje.textContent = "Creando cuenta...";
    mensaje.classList.remove("is-error", "is-success");

    submitBtn.classList.add("is-loading");
    submitBtn.disabled = true;

    const { data, error } = await supabaseClient.auth.signUp({
        email: email,
        password: password,
        options: {
            data: {
                nombre: nombre
            }
        }
    });

    if (error) {
        mensaje.textContent = error.message;
        mensaje.classList.add("is-error");

        submitBtn.classList.remove("is-loading");
        submitBtn.disabled = false;
        return;
    }

    console.log(data);

    mensaje.textContent = "Cuenta creada correctamente. Redirigiendo a iniciar sesión...";
    mensaje.classList.add("is-success");

    setTimeout(() => {
        window.location.href = "/login";
    }, 1800);
});