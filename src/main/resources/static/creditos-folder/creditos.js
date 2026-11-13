import { API_URL, sesionActiva } from "../recursos/modulos.js";

renderCreditos();

const user = await sesionActiva();

if (!user) {
    window.location.href = "../login-folder/Login.html";
}

function renderCreditos() {

    const aboutContainer = document.getElementById('about-container');

    if (aboutContainer) {
        aboutContainer.innerHTML = `
          <div class="about-info">

                <h1>Sobre el Proyecto</h1>
                <hr>
                <h2>Informacion</h2>
                <p>
                    <strong>AppointMee</strong> es una red social de turnos, esta plataforma fue diseñada para optimizar
                    la gestión de
                    turnos y mejorar la comunicación
                    entre clientes y establecimientos. El objetivo principal es facilitar la reserva, organización y
                    administración de citas de manera rápida, intuitiva y accesible desde cualquier dispositivo.
                </p>
                <p>

                    La aplicación permite a los usuarios consultar disponibilidad, solicitar turnos y recibir
                    información actualizada sobre sus reservas. Ademas que brinda a los empleados y empleadores
                    herramientas
                    para gestionar agendas, controlar la ocupación de horarios y mantener un registro ordenado de las
                    citas programadas.
                </p>
                <p>

                    Durante el desarrollo del proyecto se priorizaron aspectos como la experiencia de usuario, la
                    accesibilidad, la seguridad de los datos y la escalabilidad del sistema. Para ello, se aplicaron
                    buenas prácticas de desarrollo web y una arquitectura orientada a facilitar el mantenimiento y la
                    incorporación de futuras funcionalidades.
                </p>
                <p>

                    Nuestro equipo en esta oportunidad buscò ofrecer una solución moderna y eficiente para negocios y
                    organizaciones que
                    requieren una gestión organizada de turnos, reduciendo tiempos de espera y mejorando la experiencia
                    tanto de los clientes como del personal administrativo de las organizaciones.
                </p>

                <img src="https://mdp.utn.edu.ar/UTNMDQdata/thumbnail.jpg" alt="">
                <hr>
                <h2>Documentacion</h2>
                <div class="documentacion-links">

            
                </div>
                <div class="documentacion-links-container">
                    <a href="https://github.com/gonnnzaDev/AppointMee" target="_blank">
                        <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" fill="currentColor"
                            class="bi bi-github" viewBox="0 0 16 16">
                            <path
                                d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27s1.36.09 2 .27c1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.01 8.01 0 0 0 16 8c0-4.42-3.58-8-8-8" />
                        </svg>

                    </a>
                   <a href="${API_URL}/swagger-ui.html" target="_blank">
                    <svg fill="#ffffff" viewBox="0 0 32 32" xmlns="http://www.w3.org/2000/svg"><g id="SVGRepo_bgCarrier" stroke-width="0"></g><g id="SVGRepo_tracerCarrier" stroke-linecap="round" stroke-linejoin="round"></g><g id="SVGRepo_iconCarrier"> <path d="M16 0c-8.823 0-16 7.177-16 16s7.177 16 16 16c8.823 0 16-7.177 16-16s-7.177-16-16-16zM16 1.527c7.995 0 14.473 6.479 14.473 14.473s-6.479 14.473-14.473 14.473c-7.995 0-14.473-6.479-14.473-14.473s6.479-14.473 14.473-14.473zM11.161 7.823c-0.188-0.005-0.375 0-0.568 0.005-1.307 0.079-2.093 0.693-2.312 1.964-0.151 0.891-0.125 1.796-0.188 2.692-0.020 0.464-0.067 0.928-0.156 1.38-0.177 0.813-0.525 1.068-1.353 1.109-0.111 0.011-0.22 0.032-0.324 0.057v1.948c1.5 0.073 1.704 0.605 1.823 2.172 0.048 0.573-0.015 1.147 0.021 1.719 0.027 0.543 0.099 1.079 0.208 1.6 0.344 1.432 1.745 1.911 3.433 1.624v-1.713c-0.272 0-0.511 0.005-0.74 0-0.579-0.016-0.792-0.161-0.844-0.713-0.079-0.713-0.057-1.437-0.099-2.156-0.089-1.339-0.235-2.651-1.541-3.5 0.672-0.495 1.161-1.084 1.312-1.865 0.109-0.547 0.177-1.099 0.219-1.651s-0.025-1.12 0.021-1.667c0.077-0.885 0.135-1.249 1.197-1.213 0.161 0 0.317-0.021 0.495-0.036v-1.745c-0.213 0-0.411-0.005-0.604-0.011zM21.287 7.839c-0.365-0.011-0.729 0.016-1.089 0.079v1.697c0.329 0 0.584 0 0.833 0.005 0.439 0.005 0.772 0.177 0.813 0.661 0.041 0.443 0.041 0.891 0.083 1.339 0.089 0.896 0.136 1.796 0.292 2.677 0.136 0.724 0.636 1.265 1.255 1.713-1.088 0.729-1.411 1.776-1.463 2.953-0.032 0.801-0.052 1.615-0.093 2.427-0.037 0.74-0.297 0.979-1.043 0.995-0.208 0.011-0.411 0.027-0.64 0.041v1.74c0.432 0 0.833 0.027 1.235 0 1.239-0.073 1.995-0.677 2.239-1.885 0.104-0.661 0.167-1.333 0.183-2.005 0.041-0.615 0.036-1.235 0.099-1.844 0.093-0.953 0.532-1.349 1.484-1.411 0.089-0.011 0.177-0.032 0.267-0.057v-1.953c-0.161-0.021-0.271-0.037-0.391-0.041-0.713-0.032-1.068-0.272-1.251-0.948-0.109-0.433-0.177-0.876-0.197-1.324-0.052-0.823-0.047-1.656-0.099-2.479-0.109-1.588-1.063-2.339-2.516-2.38zM12.099 14.875c-1.432 0-1.536 2.109-0.115 2.245h0.079c0.609 0.036 1.131-0.427 1.167-1.037v-0.061c0.011-0.62-0.484-1.136-1.104-1.147zM15.979 14.875c-0.593-0.020-1.093 0.448-1.115 1.043 0 0.036 0 0.067 0.005 0.104 0 0.672 0.459 1.099 1.147 1.099 0.677 0 1.104-0.443 1.104-1.136-0.005-0.672-0.459-1.115-1.141-1.109zM19.927 14.875c-0.624-0.011-1.145 0.485-1.167 1.115 0 0.625 0.505 1.131 1.136 1.131h0.011c0.567 0.099 1.135-0.448 1.172-1.104 0.031-0.609-0.521-1.141-1.152-1.141z"></path> </g></svg>
                     </a>
                     
                </div>

                <hr>

                <h2>About us</h2>
                <h3>Gonzalo Leonel Lopez</h3>
                <p>
                    Hola, soy Gonza un estudiante de 19 años. Actualmente cursando el segundo año de
                    Programación en la Universidad Tecnologica Nacional. En este proyecto soy el desarrollador encargado
                    de realizar el Frontend.
                </p>
                <h3>Giovanni Morro Bai</h3>
                <p>
                    Hola soy Gio tengo 19 años, estoy cursando mi segundo año de programación en la UTN y en este proyecto fui el
                    encargado del Backend. 
                </p>


            </div>

        `;


    }

}
