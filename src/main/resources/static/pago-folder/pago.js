import { API_URL, authHeaders, sesionActiva, checkRes } from "../recursos/modulos.js";

const user = await sesionActiva();

if (!user) {
    window.location.href = "../login-folder/Login.html";
}

const turnoId = new URLSearchParams(window.location.search).get("turnoId");

const cancelar = document.getElementById("cancelar");
const enviar = document.getElementById("pagar");
const mp = document.getElementById("boton-mp");


if (!cancelar || !enviar || !mp) {
    window.history.back();
};

if (!turnoId) {
    alert("No se especificó un turno para pagar.");
    window.location.href = "../misturnos-folder/MisTurnos.html";
}

cancelar.addEventListener('click', e => {

    window.history.back();

});

let metodoSeleccionado = null;

mp.addEventListener('click', () => {
    metodoSeleccionado = "MERCADOPAGO";
    mp.style.outline = "2px solid #009ee3";
    mp.style.outlineOffset = "2px";
});

enviar.addEventListener('click', async e => {

    if (metodoSeleccionado === "MERCADOPAGO") {
        await pagarConMercadoPago();
    } else {
        alert("Seleccioná un método de pago.");
    }

});

async function pagarConMercadoPago() {
    enviar.disabled = true;

    try {
        const response = await fetch(API_URL + `/pagos/${turnoId}`, {
            method: 'POST',
            headers: authHeaders()
        });
        await checkRes(response);
        const data = await response.text();

        window.location.href = data;

    } catch (error) {
        alert(error.message);
        enviar.disabled = false;
    }
}