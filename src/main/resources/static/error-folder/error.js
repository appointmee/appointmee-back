import { sesionActiva } from "../recursos/modulos.js";

const user = await sesionActiva();

if (!user) {
    window.location.href = "../login-folder/Login.html";
}
renderError();

function renderError(){

    const params = new URLSearchParams(window.location.search);
    const msg = params.get("msg");

    const errorMsg = document.getElementById("msg-error");
    if (errorMsg) {
        errorMsg.textContent = msg || "Ocurrió un error desconocido";
    }
}




