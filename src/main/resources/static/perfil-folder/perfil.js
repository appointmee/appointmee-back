import { API_URL, sesionActiva, authHeaders, cerrarSesion, checkRes, checkResSinRedireccion, activarSubidaImagen } from "../recursos/modulos.js";


const user = await sesionActiva();

if (!user) {
    window.location.href = "../login-folder/Login.html";
}

const usuario = await cargarUsuario(user.id);

if (!usuario) {
    window.location.href = "../login-folder/Login.html";
} else {
    const params = new URLSearchParams(window.location.search);
    if (params.has("solicitudes")) {
        renderSolicitudes(usuario);
    } else {
        renderPerfil(usuario);
    }
}

async function renderPerfil(usuario) {

    const infoAccountDiv = document.getElementById("info-account");

    if (infoAccountDiv) {

        const roles = usuario.roles && usuario.roles.length
            ? usuario.roles.map(r => `<span class="badge badge--blue">${r}</span>`).join(" ")
            : "";

        const fotoHtml = usuario.fotoPerfil
            ? `<img src="${usuario.fotoPerfil}" alt="" onerror="this.style.display='none';this.nextElementSibling.style.display='flex';">`
            : '';

        const inicialHtml = usuario.fotoPerfil
            ? `<div class="profile-inicial" style="display:none;">${(usuario.nombre || '?')[0].toUpperCase()}</div>`
            : `<div class="profile-inicial">${(usuario.nombre || '?')[0].toUpperCase()}</div>`;

        let accionesHtml = `<button type="button" id="btn-opciones-perfil">Opciones</button>`;
        if (usuario.roles.includes("EMPLEADOR")) {
            accionesHtml += `<button type="button" id="btn-panel-empleador">Panel Empleador</button>`;
        }
        if (usuario.roles.includes("EMPLEADO")) {
            accionesHtml += `<button type="button" id="btn-panel-empleado">Panel Empleado</button>`;
        }

        infoAccountDiv.innerHTML = `
<div class="profile-card">
    ${fotoHtml}${inicialHtml}
    <div class="profile-info">
        <h1 style="font-size: clamp(1.4rem, 3vw, 2rem); font-weight: 700; letter-spacing: -.03em; color: var(--t); margin-bottom: 4px;">
            ${usuario.nombre} ${usuario.apellido}
        </h1>
        <h3><strong>Email:</strong> ${usuario.email}</h3>
        <h3><strong>Miembro desde:</strong> ${usuario.fechaCreacion}</h3>
        <div class="profile-roles">${roles}</div>
    </div>
    <div class="profile-actions">
        ${accionesHtml}
    </div>
</div>
`;

        const opciones = document.getElementById("btn-opciones-perfil");
        const btnEmpleador = document.getElementById("btn-panel-empleador");
        const btnEmpleado = document.getElementById("btn-panel-empleado");

        opciones.addEventListener("click", async () => {
            await renderOpciones(usuario);
        });

        if (btnEmpleador) {
            btnEmpleador.addEventListener("click", () => {
                window.location.href = "../empleador-folder/empleador.html";
            });
        }

        if (btnEmpleado) {
            btnEmpleado.addEventListener("click", () => {
                window.location.href = "../empleado-folder/empleado.html";
            });
        }

    }
}
async function renderOpciones(usuario) {

    const infoAccountDiv = document.getElementById("info-account");

    if (!infoAccountDiv) return;

    infoAccountDiv.innerHTML = `
        <div class="funcionalidad-perfil-container">
            <div class="funcionalidad-perfil" id="funcionalidadPerfil">
                <button id="btn-volver-perfil">Volver</button>
                <div id="dependiendo-rol"></div>
            </div>
        </div>
    `;

    console.log(usuario);

    const containerrol = document.getElementById("dependiendo-rol");

    let html = "";

    if (usuario.roles.includes("ADMINISTRADOR")) {
        html += `
            <button id="btn-modo-admin">
                Panel Administrador
            </button>
        `;
        html += `
            <button id="btn-solicitudes-transformacion">
                Solicitudes de transformación
            </button>
        `;
    }

    html += `
        <button id="btn-editar-perfil">
            Editar
        </button>
    `;

    html += `
        <button id="btn-solicitudes-perfil">
            Solicitudes
        </button>
    `;

    containerrol.innerHTML = html;

    const volver = document.getElementById("btn-volver-perfil");
    const editar = document.getElementById("btn-editar-perfil");
    const modoAdmin = document.getElementById("btn-modo-admin");
    const btnSolicitudes = document.getElementById("btn-solicitudes-perfil");
    const btnSolicitudesTransformacion = document.getElementById("btn-solicitudes-transformacion");

    volver.addEventListener("click", () => {
        renderPerfil(usuario);
    });

    if (modoAdmin) {
        modoAdmin.addEventListener("click", () => {
            window.location.href = "../admin-folder/admin.html";
        });
    }

    if (btnSolicitudesTransformacion) {
        btnSolicitudesTransformacion.addEventListener("click", () => {
            renderSolicitudesEmpleador(usuario);
        });
    }

    if (editar) {
        editar.addEventListener("click", () => {
            renderModificar(usuario);
        });
    }

    if (btnSolicitudes) {
        btnSolicitudes.addEventListener("click", () => {
            renderSolicitudes(usuario);
        });
    }
}

function renderModificar(usuario) {
    const infoAccountDiv = document.getElementById("funcionalidadPerfil");

    infoAccountDiv.innerHTML = `
            <div class="input-group">
    <input type="text"
           class="form-control"
           placeholder="Nombre"
           id="nombre-input"
           minlength="3"
           maxlength="60">
</div>

<div class="input-group">
    <input type="text"
           class="form-control"
           placeholder="Apellido"
           id="apellido-input"
           minlength="3"
           maxlength="60">
</div>

<div class="input-group">
    <input type="password"
           class="form-control"
           placeholder="Contraseña"
           id="password-input"
           minlength="8"
           maxlength="67">
</div>
<div class="input-group">
    <input type="password"
           class="form-control"
           placeholder="Repetir Contraseña "
           id="password-confirm-input"
           minlength="8"
           maxlength="67">
</div>
<div class="input-group">
    <input type="email"
           class="form-control"
           placeholder="Email"
           id="email-input"
           minlength="8"
           maxlength="150">
</div>

<div class="input-group">
    <input type="file"
           class="form-control"
           accept="image/jpeg,image/png,image/webp,image/gif"
           id="foto-file-input">
</div>
<p id="foto-upload-status" style="font-size:12px;color:var(--t3);">Elegí JPG, PNG, WEBP o GIF de hasta 5 MB.</p>
<div class="input-group">
    <input type="url"
           class="form-control"
           placeholder="O pegá la URL de foto de perfil"
           id="foto-url-input">
</div>
<input type="hidden" id="foto-public-id-input">
<div class="form-actions">
    <button class="btn-cancel" id="cancelar">Volver</button>
    <button class="btn-submit" id="modificar">Modificar</button>
</div>
<button id="btn-eliminar-perfil" style="margin-top:16px;width:100%;padding:10px;border-radius:var(--r-sm);border:1px solid rgba(239,68,68,.3);background:rgba(239,68,68,.08);color:var(--red);font-size:13px;font-weight:500;cursor:pointer;">Eliminar Cuenta</button>
            `;


    const cancelar = document.getElementById("cancelar");
    const modificar = document.getElementById("modificar");
    const eliminar = document.getElementById("btn-eliminar-perfil");
    const subirFotoPendiente = activarSubidaImagen("foto-file-input", "foto-url-input",
        "foto-public-id-input", "PERFIL", "foto-upload-status");


    cancelar.addEventListener('click', async () => {
        if (cancelar.disabled) return;
        cancelar.disabled = true;
        modificar.disabled = true;
        if (eliminar) eliminar.disabled = true;
        try {
            await subirFotoPendiente.descartar();
            await renderOpciones(usuario);
        } catch (error) {
            alert(error.message);
        } finally {
            cancelar.disabled = false;
            modificar.disabled = false;
            if (eliminar) eliminar.disabled = false;
        }

    });


    modificar.addEventListener('click', () => {

        modificarUsuario(usuario.id, subirFotoPendiente);

    });

    if (eliminar) {
        eliminar.addEventListener("click", async () => {
            if (!confirm("¿Estás seguro de que querés eliminar tu cuenta?")) return;
            const rta = await eliminarCuenta();
            if (rta) {
                alert("Eliminación realizada con éxito");
            } else {
                alert("No se pudo eliminar la cuenta");
            }
        });
    }


}


async function modificarUsuario(id, subirFotoPendiente) {
    const botonModificar = document.getElementById("modificar");
    if (botonModificar.disabled) return;
    const botonCancelar = document.getElementById("cancelar");
    const botonEliminar = document.getElementById("btn-eliminar-perfil");
    const campos = ["nombre-input", "apellido-input", "email-input", "password-input",
        "password-confirm-input", "foto-file-input", "foto-url-input"]
        .map(idCampo => document.getElementById(idCampo)).filter(Boolean)
        .map(campo => [campo, Boolean(campo.disabled)]);
    campos.forEach(([campo]) => { campo.disabled = true; });
    botonModificar.disabled = true;
    botonCancelar.disabled = true;
    if (botonEliminar) botonEliminar.disabled = true;
    try {

        const nombre = document.getElementById("nombre-input").value.trim();
        const apellido = document.getElementById("apellido-input").value.trim();
        const password = document.getElementById("password-input").value.trim();
        const email = document.getElementById("email-input").value.trim();
        const fotoFileInput = document.getElementById("foto-file-input");
        const fotoUrlInput = document.getElementById("foto-url-input");
        const fotoPublicIdInput = document.getElementById("foto-public-id-input");
        const password2 = document.getElementById("password-confirm-input").value.trim();

        const body = {};

        if (
            nombre === "" &&
            apellido === "" &&
            email === "" &&
            fotoUrlInput.value.trim() === "" &&
            !fotoFileInput.files?.length &&
            password2 === "" &&
            password === ""
        ) {
            alert("Tenes que poner algo en los campos");
            return;
        }

        if (nombre) body.nombre = nombre;
        if (apellido) body.apellido = apellido;
        if (password !== password2) {

            alert("Las contraseñas no son iguales");
            return;
        }
        if (password) body.password = password;
        if (email) body.email = email;

        await subirFotoPendiente();

        const fotoUrl = fotoUrlInput.value.trim();
        const fotoPublicId = fotoPublicIdInput.value.trim();
        if (fotoUrl) body.fotoUrl = fotoUrl;
        if (fotoPublicId) body.fotoPublicId = fotoPublicId;

        const response = await fetch(
            API_URL + `/usuarios/modificar/${id}`,
            {
                method: "PATCH",
                body: JSON.stringify(body),
                headers: authHeaders(),
            }
        );
        await checkResSinRedireccion(response);
        subirFotoPendiente.confirmar();

        alert("Usuario modificado con éxito");

    } catch (error) {
        try {
            await subirFotoPendiente.descartar();
        } catch (limpieza) {
            alert(limpieza.message);
        }
        alert(error.message);
        console.error(error);
    } finally {
        botonModificar.disabled = false;
        botonCancelar.disabled = false;
        if (botonEliminar) botonEliminar.disabled = false;
        campos.forEach(([campo, deshabilitado]) => { campo.disabled = deshabilitado; });
    }
}

async function eliminarCuenta() {

    try {

        const response = await fetch(API_URL + `/usuarios/borrar-cuenta`, {
            headers: authHeaders(),
            method: "DELETE"
        });
        await checkRes(response);

        cerrarSesion();
        return true;

    } catch (error) {
        return false;
    }

}




async function renderSolicitudesEmpleador(usuario) {
    const infoAccountDiv = document.getElementById("info-account");

    infoAccountDiv.innerHTML = `
        <div class="funcionalidad-perfil-container">
            <div class="funcionalidad-perfil">
                <h2 style="font-size:1rem;font-weight:600;color:var(--t);margin-bottom:16px;">Solicitudes para ser Empleador</h2>
                <div id="solicitudes-lista" style="color:var(--t3);font-size:13px;">Cargando...</div>
                <div style="margin-top:16px;">
                    <button class="btn-cancel" id="volver-solicitudes">Volver</button>
                </div>
            </div>
        </div>
    `;

    document.getElementById("volver-solicitudes").addEventListener("click", () => {
        renderPerfil(usuario);
    });

    try {
        const res = await fetch(API_URL + `/solicitudes-empleador/listar`, { headers: authHeaders() });
        await checkRes(res);
        const solicitudes = await res.json();
        const container = document.getElementById("solicitudes-lista");

        if (!solicitudes.length) {
            container.innerHTML = '<div style="color:var(--t3);font-family:var(--mono);font-size:12px;">No hay solicitudes pendientes</div>';
            return;
        }

        container.innerHTML = solicitudes.map(s => {
            const fecha = new Date(s.fechaSolicitud).toLocaleDateString("es-AR");
            return `
                <div class="card solicitud-card" style="cursor:pointer;" data-id="${s.id}">
                    <h3>${s.nombreUsuario} ${s.apellidoUsuario}</h3>
                    <p>${fecha}</p>
                    <div style="display:flex;gap:8px;flex-wrap:wrap;">
                        <button class="detalle-btn" data-id="${s.id}">Ver Detalle</button>
                    </div>
                </div>`;
        }).join("");

        container.querySelectorAll(".detalle-btn").forEach(btn => {
            btn.addEventListener("click", () => verDetalleSolicitudEmpleador(parseInt(btn.dataset.id)));
        });
    } catch (e) {
        const container = document.getElementById("solicitudes-lista");
        if (container) container.innerHTML = '<div style="color:var(--red);font-size:12px;">Error al cargar solicitudes</div>';
    }

    async function verDetalleSolicitudEmpleador(id) {
        const old = document.getElementById("modal-detalle-solicitud");
        if (old) old.remove();

        const div = document.createElement("div");
        div.className = "modal-overlay";
        div.id = "modal-detalle-solicitud";
        div.addEventListener("click", (e) => {
            if (e.target === div) div.remove();
        });
        document.body.appendChild(div);

        div.innerHTML = `
            <div class="form-simple">
                <h1>Detalle de Solicitud</h1>
                <div id="detalle-solicitud-body" style="color:var(--t3);font-size:12px;">Cargando...</div>
                <div class="form-actions" style="margin-top:20px;">
                    <button class="btn-cancel" onclick="this.closest('.modal-overlay').remove()">Cerrar</button>
                </div>
            </div>
        `;

        try {
            const res = await fetch(API_URL + `/solicitudes-empleador/${id}/detalles`, { headers: authHeaders() });
            await checkRes(res);
            const s = await res.json();
            const body = document.getElementById("detalle-solicitud-body");
            if (!body) return;

            const fila = (label, valor) => `
                <p style="margin:0 0 8px;"><strong>${label}:</strong> ${valor ?? "-"}</p>
            `;

            const estadoBadge = {
                PENDIENTE: 'badge--amber',
                APROBADA: 'badge--green',
                RECHAZADA: 'badge--red'
            };

            let html = "";
            html += fila("Estado", `<span class="badge ${estadoBadge[s.estado] || 'badge--blue'}">${s.estado}</span>`);
            html += fila("Fecha", new Date(s.fechaSolicitud).toLocaleDateString("es-AR"));
            html += `<hr style="border-color:var(--b);margin:14px 0;">`;
            html += `<p style="font-weight:600;color:var(--t);margin-bottom:8px;">Usuario</p>`;
            html += fila("Nombre", `${s.usuario?.nombre ?? ""} ${s.usuario?.apellido ?? ""}`);
            html += fila("Email", s.usuario?.email);
            html += `<hr style="border-color:var(--b);margin:14px 0;">`;
            html += `<p style="font-weight:600;color:var(--t);margin-bottom:8px;">Motivo</p>`;
            html += `<p style="color:var(--t2);font-size:13px;background:rgba(255,255,255,.03);border:1px solid var(--b);border-radius:var(--r-sm);padding:12px;">${s.motivo ?? "Sin motivo"}</p>`;

            if (s.estado === 'PENDIENTE') {
                html += `
                    <div class="form-actions" style="margin-top:16px;">
                        <button class="btn-submit" id="btn-aceptar-detalle-solicitud">Aprobar</button>
                        <button class="btn-cancel" id="btn-rechazar-detalle-solicitud" style="color:var(--red);">Rechazar</button>
                    </div>
                `;
            }

            body.innerHTML = html;

            const btnAceptar = document.getElementById("btn-aceptar-detalle-solicitud");
            const btnRechazar = document.getElementById("btn-rechazar-detalle-solicitud");

            if (btnAceptar) {
                btnAceptar.addEventListener("click", async () => {
                    div.remove();
                    await aprobarSolicitudEmpleador(id);
                });
            }

            if (btnRechazar) {
                btnRechazar.addEventListener("click", async () => {
                    div.remove();
                    await rechazarSolicitudEmpleador(id);
                });
            }

        } catch (e) {
            const body = document.getElementById("detalle-solicitud-body");
            if (body) body.innerHTML = `<div style="color:var(--red);font-size:12px;">Error al cargar el detalle</div>`;
        }
    }

    async function aprobarSolicitudEmpleador(id) {
        try {
            const res = await fetch(API_URL + `/solicitudes-empleador/${id}/aprobar`, {
                method: "PATCH",
                headers: authHeaders()
            });
            await checkRes(res);
            alert("Solicitud aprobada");
            renderSolicitudesEmpleador(usuario);
        } catch {
            alert("No se pudo aprobar la solicitud");
        }
    }

    async function rechazarSolicitudEmpleador(id) {
        try {
            const res = await fetch(API_URL + `/solicitudes-empleador/${id}/rechazar`, {
                method: "PATCH",
                headers: authHeaders()
            });
            await checkRes(res);
            alert("Solicitud rechazada");
            renderSolicitudesEmpleador(usuario);
        } catch {
            alert("No se pudo rechazar la solicitud");
        }
    }
}

async function renderSolicitudes(usuario) {
    const infoAccountDiv = document.getElementById("info-account");

    let solicitudesCache = [];

    infoAccountDiv.innerHTML = `
        <div class="funcionalidad-perfil-container">
            <div class="funcionalidad-perfil">
                <h2 style="font-size:1rem;font-weight:600;color:var(--t);margin-bottom:16px;">Invitaciones a Sucursales</h2>
                <div id="solicitudes-lista" style="color:var(--t3);font-size:13px;">Cargando...</div>
                <div style="margin-top:16px;">
                    <button class="btn-cancel" id="volver-solicitudes">Volver</button>
                </div>
            </div>
        </div>
    `;

    document.getElementById("volver-solicitudes").addEventListener("click", () => {
        renderPerfil(usuario);
    });

    try {
        const res = await fetch(API_URL + `/solicitudes-empleado/recibidas`, { headers: authHeaders() });

        await checkRes(res);

        solicitudesCache = await res.json();
        renderLista(solicitudesCache);
    } catch (e) {
        const container = document.getElementById("solicitudes-lista");
        console.log(e);
        if (container) container.innerHTML = '<div style="color:var(--red);font-size:12px;">Error al cargar invitaciones</div>';
    }


    function renderLista(solicitudes) {
        const container = document.getElementById("solicitudes-lista");

        if (!solicitudes || !solicitudes.length) {
            container.innerHTML = '<div style="color:var(--t3);font-family:var(--mono);font-size:12px;">No tenés invitaciones pendientes</div>';
            return;
        }

        const estadoBadge = {
            PENDIENTE: 'badge--amber',
            APROBADA: 'badge--green',
            RECHAZADA: 'badge--red'
        };

        container.innerHTML = solicitudes.map(s => {
            const fecha = new Date(s.fechaSolicitud).toLocaleDateString("es-AR");
            const esPendiente = s.estadoSolicitud === 'PENDIENTE';
            return `
                <div class="card solicitud-card" data-id="${s.id}">
                    <h3>${s.nombreSucursal}</h3>
                    <p>${fecha} — Te invitaron a trabajar en esta sucursal</p>
                    <div style="display:flex;gap:8px;flex-wrap:wrap;">
                        <button class="detalle-btn" data-id="${s.id}">Ver Detalle</button>
                    </div>
                </div>`;
        }).join("");

        container.querySelectorAll(".detalle-btn").forEach(btn => {
            btn.addEventListener("click", () => verDetalleSolicitud(parseInt(btn.dataset.id)));
        });
        container.querySelectorAll(".aceptar-btn").forEach(btn => {
            btn.addEventListener("click", () => aprobarSolicitud(parseInt(btn.dataset.id)));
        });
        container.querySelectorAll(".rechazar-btn").forEach(btn => {
            btn.addEventListener("click", () => rechazarSolicitud(parseInt(btn.dataset.id)));
        });
    }

    async function verDetalleSolicitud(id) {
        const old = document.getElementById("modal-detalle-solicitud");
        if (old) old.remove();

        const div = document.createElement("div");
        div.className = "modal-overlay";
        div.id = "modal-detalle-solicitud";
        div.addEventListener("click", (e) => {
            if (e.target === div) div.remove();
        });
        document.body.appendChild(div);

        div.innerHTML = `
            <div class="form-simple">
                <h1>Detalle de la Solicitud</h1>
                <div id="detalle-solicitud-body" style="color:var(--t3);font-size:12px;">Cargando...</div>
                <div class="form-actions" style="margin-top:20px;">
                    <button class="btn-cancel" onclick="this.closest('.modal-overlay').remove()">Cerrar</button>
                </div>
            </div>
        `;

        try {
            const res = await fetch(API_URL + `/solicitudes-empleado/${id}/detalles`, { headers: authHeaders() });
            await checkRes(res);
            const s = await res.json();
            const body = document.getElementById("detalle-solicitud-body");
            if (!body) return;

            const fila = (label, valor) => `
                <p style="margin:0 0 8px;"><strong>${label}:</strong> ${valor ?? "-"}</p>
            `;

            const estadoBadgeDetalle = {
                PENDIENTE: 'badge--amber',
                APROBADA: 'badge--green',
                RECHAZADA: 'badge--red'
            };

            let html = "";
            html += fila("Estado", `<span class="badge ${estadoBadgeDetalle[s.estadoSolicitud] || 'badge--blue'}">${s.estadoSolicitud}</span>`);
            html += fila("Fecha de solicitud", new Date(s.fechaSolicitud).toLocaleDateString("es-AR"));

            html += `<hr style="border-color:var(--b);margin:14px 0;">`;
            html += `<p style="font-weight:600;color:var(--t);margin-bottom:8px;">Empleado</p>`;
            html += fila("Nombre", s.empleado ? `${s.empleado.nombre} ${s.empleado.apellido}` : "-");
            html += fila("Email", s.empleado?.email);

            html += `<hr style="border-color:var(--b);margin:14px 0;">`;
            html += `<p style="font-weight:600;color:var(--t);margin-bottom:8px;">Sucursal</p>`;
            html += fila("Nombre", s.sucursal?.nombre);
            html += fila("Categoría", s.sucursal?.categoria);
            html += fila("Puntuación", s.sucursal?.puntuacion
                ? "🐝".repeat(Math.round(s.sucursal.puntuacion))
                : "Sin reseñas");

            if (s.estadoSolicitud === 'PENDIENTE') {
                html += `
                    <div class="form-actions" style="margin-top:16px;">
                        <button class="btn-submit" id="btn-aceptar-detalle-solicitud">Aceptar</button>
                        <button class="btn-cancel" id="btn-rechazar-detalle-solicitud" style="color:var(--red);">Rechazar</button>
                    </div>
                `;
            }

            body.innerHTML = html;

            const btnAceptar = document.getElementById("btn-aceptar-detalle-solicitud");
            const btnRechazar = document.getElementById("btn-rechazar-detalle-solicitud");

            if (btnAceptar) {
                btnAceptar.addEventListener("click", async () => {
                    div.remove();
                    await aprobarSolicitud(id);
                });
            }

            if (btnRechazar) {
                btnRechazar.addEventListener("click", async () => {
                    div.remove();
                    await rechazarSolicitud(id);
                });
            }

        } catch (e) {
            const body = document.getElementById("detalle-solicitud-body");
            if (body) body.innerHTML = `<div style="color:var(--red);font-size:12px;">Error al cargar el detalle: ${e.message}</div>`;
        }
    }

    async function aprobarSolicitud(id) {
        try {
            const res = await fetch(API_URL + `/solicitudes-empleado/${id}/aprobar`, {
                method: "PATCH",
                headers: authHeaders()
            });
            await checkRes(res);
            alert("Solicitud aceptada");
            const res2 = await fetch(API_URL + `/solicitudes-empleado/recibidas`, { headers: authHeaders() });
            await checkRes(res2);
            solicitudesCache = await res2.json();
            renderLista(solicitudesCache);
        } catch {
            alert("No se pudo aceptar la solicitud");
        }
    }

    async function rechazarSolicitud(id) {
        try {
            const res = await fetch(API_URL + `/solicitudes-empleado/${id}/rechazar`, {
                method: "PATCH",
                headers: authHeaders()
            });
            await checkRes(res);
            alert("Solicitud rechazada");
            const res2 = await fetch(API_URL + `/solicitudes-empleado/recibidas`, { headers: authHeaders() });
            await checkRes(res2);
            solicitudesCache = await res2.json();
            renderLista(solicitudesCache);
        } catch {
            alert("No se pudo rechazar la solicitud");
        }
    }
}


async function cargarUsuario(id) {
    try {


        const response = await fetch(API_URL + `/usuarios/${id}`,
            {
                headers: authHeaders()
            }
        );
        await checkRes(response);

        return await response.json();
    } catch (error) {
        alert(error.message);
        return null;
    }
}
