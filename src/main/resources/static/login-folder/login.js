import { API_URL, authHeaders, checkRes } from "../recursos/modulos.js";
const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
renderLogin();

function renderLogin() {
    const formContainer = document.getElementById("form-container");

    if (formContainer) {
        formContainer.innerHTML = `
       <div class="form" id="info-login">
          <div class="credentials-form">
            <h2 class="login-title">Iniciar sesión</h2>
            <div class="input-group">
              <input type="text" class="form-control" placeholder="Mail" id="login-mail-input">
            </div>
            <div class="input-group">
              <input type="password" class="form-control" placeholder="Contraseña" id="password-mail-input">
            </div>
            <div class="login-actions">
              <button type="button" id="login-button">Iniciar sesión</button>
              <div class="login-divider"><span>o</span></div>
              <button type="button" id="register-button">Crear cuenta</button>
              <a href="../PerdisteCuenta-folder/PerdiMiCuenta.html" class="login-forgot">
                Olvidé mi cuenta
              </a>
           </div>
          </div>
        </div>
        `;
    }
}

async function userExists(mail, pass) {
    try {
        const response = await fetch(API_URL + `/usuarios/inicio-sesion`,
            {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    email: mail.value,
                    pass: pass.value
                })
            }
        );

        await checkRes(response);

        const data = await response.json();
        localStorage.setItem("token", data.token);
        return data;

    } catch (error) {
        return false;
    }
}

const botonLogin = document.getElementById("login-button");

if (botonLogin) {
    botonLogin.addEventListener("click", async function () {
        const mail = document.getElementById("login-mail-input");
        const pass = document.getElementById("password-mail-input");

        if (!emailRegex.test(mail.value)) {
            alert("Mail inválido");
        } else if (pass.value.length < 4) {
            alert("La contraseña debe tener al menos 4 caracteres");
        } else {
            const existe = await userExists(mail, pass);

            if (!existe) {
                alert("Usuario o contraseña incorrectos");
            } else {
                window.location.href = "../index-folder/Index.html";
            }
        }
    });
}

const cancelButton = document.getElementById("cancel-button");

if (cancelButton) {
    cancelButton.addEventListener("click", () => {
        window.location.href = "../login-folder/Login.html";
    });
}

const registerButton = document.getElementById("register-button");

if (registerButton) {
    registerButton.addEventListener("click", () => {
        window.location.href = "../register-folder/Register.html";
    });
}

