import { sesionActiva, cerrarSesion } from "../recursos/modulos.js";

const u = await sesionActiva();

if(!u) window.location.href = "../login-folder/Login.html";

render();

function render() {


    const container = document.getElementById("info");

    if (container) {
        container.innerHTML =

            `  <div class="configuracion-titulo" >   
        <h1>Configuracion</h1>
            </div>

            <div class="seccion-configuracion">
                <h2>¿Sos Empleador?</h2>

                <p>¿Sos empleador y todavia no tenes tu Cuenta de Empleador?
                </p>
                <p>Llena este
                    <a href="../formularioempleador-transformarse-folder/FormularioEmpleador.html">
                        Formulario
                    </a>
                </p>
            </div>

            <div class="seccion-configuracion">

                <h2>Ayuda</h2>
                <p>¿Tuviste algun problema?
                    <a href="mailto:appointmee3@gmail.com">Contactate</a>
                </p>


            </div>
            <div class="seccion-configuracion">

                <h2>Documentacion</h2>
                <p>Deseas ver la documentacion del proyecto?
                    <a href="../creditos-folder/Creditos.html">
                        Documentacion
                    </a>
                </p>
            </div>

        
            

            <div class="configuracion-botones">
                <button id="boton-cerrar">Cerrar Sesion</button>
            </div>`;
    }




}



const btn = document.getElementById("boton-cerrar");
if (btn) {

    btn.addEventListener('click', () => {

        cerrarSesion();

    });
}