import { API_URL, checkRes } from "../recursos/modulos.js";
const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const nameRegex = /^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+$/;
const passRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@#$%&*!_-]).+$/;

renderRegister();

const registerPerson = document.getElementById("register-person");

if (registerPerson) {
    registerPerson.addEventListener("click", function () {
        const name = document.getElementById("register-name-input").value.trim();
        const surname = document.getElementById("register-surname-input").value.trim();
        const mail = document.getElementById("register-mail-input").value.trim();
        const pass1 = document.getElementById("register-password1-input").value;
        const pass2 = document.getElementById("register-password2-input").value;

        if (!name || !surname || !mail || !pass1 || !pass2) {
            alert("Completá todos los campos");
        } else if (name.length < 3 || name.length > 60) {
            alert("El nombre debe tener entre 3 y 60 caracteres");
        } else if (!nameRegex.test(name)) {
            alert("El nombre solo puede contener letras");
        } else if (surname.length < 3 || surname.length > 60) {
            alert("El apellido debe tener entre 3 y 60 caracteres");
        } else if (!nameRegex.test(surname)) {
            alert("El apellido solo puede contener letras");
        } else if (!emailRegex.test(mail)) {
            alert("Mail inválido");
        } else if (mail.length < 8 || mail.length > 150) {
            alert("El email debe tener entre 8 y 150 caracteres");
        } else if (pass1.length < 8 || pass1.length > 67) {
            alert("La contraseña debe tener entre 8 y 67 caracteres");
        } else if (!passRegex.test(pass1)) {
            alert("La contraseña debe contener al menos una minúscula, una mayúscula, un número y un carácter especial (@#$%&*!_-)");
        } else if (pass1 !== pass2) {
            alert("Las contraseñas no coinciden");
        } else {
            postUser(name, surname, mail, pass1);
        }
    });
}

const cancelButton = document.getElementById("cancel-button");

if (cancelButton) {
    cancelButton.addEventListener("click", () => {
        window.location.href = "../login-folder/Login.html";
    });
}
function postUser(name, surname, mail, pass) {


    fetch(API_URL + "/usuarios/crear", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            nombre: name,
            apellido: surname,
            password: pass,
            email: mail
        })
    }).then(async response => { await checkRes(response); return response; })
        .then(data => {
            alert("Registrado con exito!");
            window.location.href = "../login-folder/Login.html";

        })
        .catch(error => {
            alert(error);
        });
}


function renderRegister() {

    const container = document.getElementById("register-form")

    if (container) {

        container.innerHTML = `
        <div class="form" id="info-register">
            <div class="credentials-form" id="realizar-registro-button">
                <h2 class="login-title">Crear cuenta</h2>
                <div class="input-group">
                    <input type="text" class="form-control" placeholder="Nombre" id="register-name-input">
                </div>
                <div class="input-group">
                    <input type="text" class="form-control" placeholder="Apellido" id="register-surname-input">
                </div>
                <div class="input-group">
                    <input type="text" class="form-control" placeholder="Email" id="register-mail-input">
                </div>
                <div class="input-group">
                    <input type="password" class="form-control" placeholder="Contraseña" id="register-password1-input">
                </div>
                <div class="input-group">
                    <input type="password" class="form-control" placeholder="Confirmar Contraseña" id="register-password2-input">
                </div>
                <div class="login-actions">
                    <button type="button" id="register-person">Crear Cuenta</button>
                    <button type="button" id="cancel-button">Volver a Login</button>
                </div>
            </div>
        </div>
        `
    }

}