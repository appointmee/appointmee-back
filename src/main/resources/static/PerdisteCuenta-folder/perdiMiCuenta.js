import { API_URL, checkRes } from "../recursos/modulos.js";
const cancelar = document.getElementById("cancelar");
const enviar = document.getElementById("enviar");


if (!cancelar || !enviar) {
    window.history.back();

};



enviar.addEventListener('click', e => {
    const email = document.getElementById("email-denuncia-input").value;
    const descripcion = document.getElementById("descripcion-denuncia-input").value;
    const pass = document.getElementById("pass-denuncia-input").value;

    if (email === "") {

        alert("El email es obligatorio");
        return;
    }
    if (descripcion === "") {
        alert("La descripcion es obligatoria");
        return;

    }

    if (email !== "" && descripcion !== "") {
        postFormulario(email, descripcion, pass);
    }

});
cancelar.addEventListener('click', e => {

    window.history.back();

});

async function postFormulario(email, descripcion, pass) {
    try {
        const response = await fetch(API_URL + "/usuarios/recuperar-cuenta", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                email: email,
                password: pass,
                descripcion: descripcion,
            })
        });
        await checkRes(response);
        alert("Realizado con exito!");
    } catch (error) {
        alert(error);
    }
}







