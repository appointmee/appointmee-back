import { API_URL, authHeaders, sesionActiva, checkRes } from "../recursos/modulos.js";
const user = await sesionActiva();

if (!user) {
    window.location.href = "../login-folder/Login.html";
}

render();

function render() {

    const container = document.getElementById("info");
    container.innerHTML =
        ` <h1>Empleador...</h1>
            <p>Ingresá tu información y, una vez que validemos tus datos, te registraremos en el sistema como empleador.
            </p>

            <div class="input-group">
                <textarea class="form-control" placeholder="Mensaje" id="mensaje"></textarea>
            </div>
            <div class="formulario-transformar-empleador-botones">
                <button class="btn-cancel" id="volver">Volver</button>
                <button class="btn-submit" id="enviar">Enviar</button>
            </div>`;
    const enviar = document.getElementById("enviar");
    const volver = document.getElementById("volver");

    enviar.addEventListener('click', () => {

        const mensaje = document.getElementById("mensaje").value;

        postMensaje(mensaje);

    });


    volver.addEventListener('click', () => {

        window.history.back();

    });
}

async function postMensaje(mensaje) {


    try {
        const response = await fetch(
            API_URL + `/solicitudes-empleador/solicitar`,
             {
            method: 'POST',
            headers: authHeaders(),
            body: JSON.stringify({ motivo: mensaje })
        });
        await checkRes(response);
        const data = await response.text();

        if (response.ok) alert("enviado con exito");

    } catch (error) {
        alert("Error al enviar: " + error.message);
    }




}