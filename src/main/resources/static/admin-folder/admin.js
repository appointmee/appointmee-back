import { API_URL, authHeaders, sesionActiva, checkRes, resumenDiasApertura, listaDiasAperturaHTML } from "../recursos/modulos.js";

const usuariosBody = document.getElementById("usuariosBody");
const sucursalesBody = document.getElementById("sucursalesBody");

const user = await sesionActiva();

if (!user) {
    window.location.href = "../login-folder/Login.html";
}

render();

async function render() {
    if (!usuariosBody || !sucursalesBody ) return;

    const [usuariosList, sucursalesList] = await Promise.all([
        buscarUsuarios(),
        buscarSucursales(),
    ]);

    let usuariosHTML = "";
    usuariosList.forEach(u => {
        const estadoTexto = u.estado ? "Activo" : "Inactivo";
        const estadoClase = u.estado ? "badge--green" : "badge--red";
        usuariosHTML += `
            <tr>
                <td>${u.id}</td>
                <td>${u.nombre}</td>
                <td>${u.apellido}</td>
                <td><span class="badge ${estadoClase}">${estadoTexto}</span></td>
                <td>
                    <div class="table-actions">
                        <button class="btn-detalle-usuario" data-id="${u.id}">Detalle</button>
                        <button class="btn-eliminar-usuario btn-danger" data-id="${u.id}">Eliminar</button>
                    </div>
                </td>
            </tr>`;
    });
    usuariosBody.innerHTML = usuariosHTML;

    let sucursalesHTML = "";
    sucursalesList.forEach(s => {
        sucursalesHTML += `
            <tr>
                <td>${s.nombre}</td>
                <td>${s.categoria}</td>
                <td>${s.puntuacion ? "🐝".repeat(Math.round(s.puntuacion)) : "-"}</td>
                <td>
                    <div class="table-actions">
                        <button class="btn-detalle-sucursal" data-id="${s.id}">Detalle</button>
                        <button class="btn-eliminar-sucursal btn-danger" data-id="${s.id}">Eliminar</button>
                    </div>
                </td>
            </tr>`;
    });
    sucursalesBody.innerHTML = sucursalesHTML;

    registrarEventos();
}

function registrarEventos() {
    usuariosBody.addEventListener("click", async (e) => {
        const btnDetalle = e.target.closest(".btn-detalle-usuario");
        if (btnDetalle) {
            const userId = btnDetalle.dataset.id;
            await abrirModalUsuario(userId);
            return;
        }

        const btnEliminar = e.target.closest(".btn-eliminar-usuario");
        if (btnEliminar) {
            const userId = btnEliminar.dataset.id;
            eliminarUsuario(userId);
        }
    });

    sucursalesBody.addEventListener("click", (e) => {
        const btnDetalle = e.target.closest(".btn-detalle-sucursal");
        if (btnDetalle) {
            const sucursalId = btnDetalle.dataset.id;
            abrirModalSucursal(sucursalId);
            return;
        }

        const btnEliminar = e.target.closest(".btn-eliminar-sucursal");
        if (btnEliminar) {
            const sucursalId = btnEliminar.dataset.id;
            eliminarSucursal(sucursalId);
        }
    });
}

async function abrirModalUsuario(userId) {
    const perfil = await buscarPerfilUsuario(userId);
    if (!perfil) {
        alert("No se pudo cargar la información del usuario.");
        return;
    }

    const roles = perfil.roles ? [...perfil.roles] : [];
    const coloresBadge = {
        ADMINISTRADOR: "badge--red",
        EMPLEADOR: "badge--blue",
        EMPLEADO: "badge--green",
        CLIENTE: "badge--amber"
    };
    const rolesHTML = roles.map(r =>
        `<span class="badge ${coloresBadge[r] || ""}">${r}</span>`
    ).join(" ");

    const fotoPerfil = perfil.fotoPerfil
        ? `<img src="${perfil.fotoPerfil}" alt="Foto" class="avatar-round">`
        : `<div class="avatar-initial">
               ${(perfil.nombre || "?")[0].toUpperCase()}
           </div>`;

    let sucursalesSection = "";

    function renderSucursalList(lista, titulo) {
        if (!lista || !lista.length) return "";
        const items = lista.map(s => `
            <li class="suc-item">
                <div>
                    <strong>${s.nombre}</strong>
                    <span class="suc-item-cat">${s.categoria || ""}</span>
                </div>
                <div class="suc-item-actions">
                    <span class="badge badge--amber">${s.puntuacion ? "🐝".repeat(Math.round(s.puntuacion)) : "-"}</span>
                    <a href="../sucursal-folder/Sucursal.html?id=${s.id}"
                       target="_blank" class="suc-item-link">Ver</a>
                </div>
            </li>`).join("");
        return `
            <hr class="suc-list-hr">
            <div class="suc-list-title">${titulo}</div>
            <ul class="suc-list">${items}</ul>`;
    }

    sucursalesSection += renderSucursalList(perfil.sucursalesEmpleado, "Sucursales donde trabaja");
    sucursalesSection += renderSucursalList(perfil.sucursalesEmpleador, "Sucursales que posee");

    let seccionExtra = "";
    if (roles.includes("EMPLEADOR")) {
        seccionExtra += `<div class="rol-info rol-info--blue"><strong>Empleador:</strong> Dueño/gestor de sucursales.</div>`;
    }
    if (roles.includes("CLIENTE")) {
        seccionExtra += `<div class="rol-info rol-info--amber"><strong>Cliente:</strong> Puede reservar turnos.</div>`;
    }
    if (roles.includes("EMPLEADO")) {
        seccionExtra += `<div class="rol-info rol-info--green"><strong>Empleado:</strong> Trabaja en una o más sucursales.</div>`;
    }
    if (roles.includes("ADMINISTRADOR")) {
        seccionExtra += `<div class="rol-info rol-info--red"><strong>Administrador:</strong> Acceso completo al panel.</div>`;
    }

    const backdrop = document.createElement("div");
    backdrop.className = "modal-overlay";
    backdrop.innerHTML = `
        <div class="form-simple form-simple--wide">
            <div class="modal-head">
                <h1 class="modal-title">Detalle de Usuario</h1>
                <button id="btn-cerrar-modal" class="modal-close">&times;</button>
            </div>
            <div class="modal-avatar">${fotoPerfil}</div>
            <div class="modal-roles">${rolesHTML}</div>
            <div class="detail-grid">
                <div><small class="detail-label">ID</small><div class="detail-value">${perfil.id}</div></div>
                <div><small class="detail-label">Fecha de creación</small><div class="detail-value">${perfil.fechaCreacion ?? "-"}</div></div>
                <div><small class="detail-label">Nombre</small><div class="detail-value">${perfil.nombre ?? "-"}</div></div>
                <div><small class="detail-label">Apellido</small><div class="detail-value">${perfil.apellido ?? "-"}</div></div>
            </div>
            <div class="detail-section">
                <small class="detail-label">Email</small>
                <div class="detail-value">${perfil.email ?? "-"}</div>
            </div>
            ${seccionExtra}
            ${sucursalesSection}
            <div class="modal-foot">
                <button id="btn-cerrar-modal-bottom" class="btn-ghost">Cerrar</button>
            </div>
        </div>
    `;
    document.body.appendChild(backdrop);

    const cerrar = () => backdrop.remove();
    backdrop.querySelector("#btn-cerrar-modal").onclick = cerrar;
    backdrop.querySelector("#btn-cerrar-modal-bottom").onclick = cerrar;
    backdrop.addEventListener("click", (e) => { if (e.target === backdrop) cerrar(); });

}

async function abrirModalSucursal(sucursalId) {
    const s = await buscarSucursalPorId(sucursalId);
    if (!s) {
        alert("No se pudo cargar la información de la sucursal.");
        return;
    }

    const foto = s.fotoPerfil
        ? `<img src="${s.fotoPerfil}" alt="Foto" class="avatar-round--sm">`
        : `<div class="avatar-initial avatar-initial--sm">
               ${(s.nombre || "?")[0].toUpperCase()}
           </div>`;

    let empleadosHTML = '<div class="detail-body" style="color:var(--t3)">Cargando...</div>';

    const backdrop = document.createElement("div");
    backdrop.className = "modal-overlay";
    backdrop.innerHTML = `
        <div class="form-simple form-simple--wide">
            <div class="modal-head">
                <h1 class="modal-title">Detalle de Sucursal</h1>
                <button id="btn-cerrar-modal" class="modal-close">&times;</button>
            </div>
            <div class="modal-avatar">${foto}</div>
            <div class="detail-grid detail-section">
                <div><small class="detail-label">Nombre</small><div class="detail-value">${s.nombre}</div></div>
                <div><small class="detail-label">Categoría</small><div class="detail-value">${s.categoria ?? "-"}</div></div>
                <div class="col-full"><small class="detail-label">Dirección</small><div class="detail-value">${s.direccion ?? "-"}</div></div>
                <div><small class="detail-label">Teléfono</small><div class="detail-value">${s.telefono ?? "-"}</div></div>
                <div><small class="detail-label">Fecha de creación</small><div class="detail-value">${s.fechaCreacion ?? "-"}</div></div>
                <div><small class="detail-label">Horario</small><div class="detail-value">${s.horaApertura ?? "-"} — ${s.horaCierre ?? "-"}</div></div>
                <div class="col-full"><small class="detail-label">Días de apertura</small><div class="detail-value detalle-value--dias">
                    <span class="detalle-sub">${resumenDiasApertura(s.diasApertura)}</span>
                    <ul class="dias-lista">${listaDiasAperturaHTML(s.diasApertura)}</ul>
                </div></div>
                <div><small class="detail-label">Puntuación</small><div class="detail-value">${s.puntuacion ? "🐝".repeat(Math.round(s.puntuacion)) : "-"}</div></div>
                <div><small class="detail-label">Empleador</small><div class="detail-value">${s.empleador ? s.empleador.nombre + " " + s.empleador.apellido : "-"}</div></div>
            </div>
            <div class="detail-section">
                <small class="detail-label">Descripción</small>
                <div class="detail-body">${s.descripcion ?? "-"}</div>
            </div>
            <div class="detail-section" id="seccion-empleados">
                <small class="detail-label">Empleados</small>
                <div id="lista-empleados">${empleadosHTML}</div>
            </div>
            <div class="modal-foot--flex">
                <a href="../sucursal-folder/Sucursal.html?id=${s.id}"
                   target="_blank" class="btn-link">Ir a la sucursal</a>
                <button id="btn-cerrar-modal-bottom" class="btn-ghost">Cerrar</button>
            </div>
        </div>
    `;
    document.body.appendChild(backdrop);

    const cerrar = () => backdrop.remove();
    backdrop.querySelector("#btn-cerrar-modal").onclick = cerrar;
    backdrop.querySelector("#btn-cerrar-modal-bottom").onclick = cerrar;
    backdrop.addEventListener("click", (e) => { if (e.target === backdrop) cerrar(); });

    try {
        const res = await fetch(API_URL + `/sucursales/${sucursalId}/empleados`, { headers: authHeaders() });
        await checkRes(res);
        const empleados = await res.json();
        const lista = document.getElementById("lista-empleados");

        if (!empleados || !empleados.length) {
            lista.innerHTML = '<div class="detail-body" style="color:var(--t3)">Sin empleados</div>';
        } else {
            lista.innerHTML = empleados.map(e => {
                const avatar = e.urlFotoPerfil
                    ? `<img src="${e.urlFotoPerfil}" alt="" style="width:32px;height:32px;border-radius:50%;object-fit:cover;flex-shrink:0;">`
                    : `<div style="width:32px;height:32px;border-radius:50%;background:var(--s2);color:var(--t2);display:flex;align-items:center;justify-content:center;font-size:12px;font-weight:600;flex-shrink:0;">${(e.nombre || "?")[0].toUpperCase()}</div>`;
                return `
                    <div style="display:flex;align-items:center;gap:10px;padding:6px 0;border-bottom:1px solid var(--b);">
                        ${avatar}
                        <div style="flex:1;min-width:0;">
                            <div style="font-size:13px;font-weight:500;color:var(--t);">${e.nombre} ${e.apellido}</div>
                            <div style="font-size:11px;color:var(--t3);font-family:var(--mono);">${e.email}</div>
                        </div>
                    </div>`;
            }).join("");
        }
    } catch (e) {
        const lista = document.getElementById("lista-empleados");
        if (lista) lista.innerHTML = '<div class="detail-body" style="color:var(--red)">Error al cargar empleados</div>';
    }
}

async function buscarSucursalPorId(id) {
    try {
        const res = await fetch(API_URL + `/sucursales/${id}`, { headers: authHeaders() });
        await checkRes(res);
        return await res.json();
    } catch (e) {
        console.error("Error al cargar sucursal:", e);
        return null;
    }
}

async function buscarPerfilUsuario(userId) {
    try {
        const response = await fetch(API_URL + `/usuarios/${userId}`, { headers: authHeaders() });
        await checkRes(response);
        return await response.json();
    } catch (error) {
        console.error("Error al cargar perfil de usuario:", error);
        return null;
    }
}

async function buscarUsuarios() {
    try {
        const response = await fetch(API_URL + "/usuarios/listar", { headers: authHeaders() });
        await checkRes(response);
        return await response.json();
    } catch (error) {
        alert(error.message);
        return [];
    }
}

async function buscarSucursales() {
    try {
        const response = await fetch(API_URL + "/sucursales/listar", { headers: authHeaders() });
        await checkRes(response);
        return await response.json();
    } catch (error) {
        alert(error.message);
        return [];
    }
}

async function eliminarUsuario(userId) {
    if (!confirm(`¿Seguro que querés eliminar el usuario #${userId}?`)) return;
    try {
        const response = await fetch(API_URL + `/usuarios/eliminar/${userId}`, {
            method: "DELETE",
            headers: authHeaders()
        });
        await checkRes(response);
        alert("Usuario eliminado correctamente.");
        render();
    } catch (error) {
        alert("Error al eliminar usuario: " + error.message);
    }
}

async function eliminarSucursal(sucursalId) {
    if (!confirm(`¿Seguro que querés eliminar la sucursal #${sucursalId}?`)) return;
    try {
        const response = await fetch(API_URL + `/sucursales/eliminar/${sucursalId}`, {
            method: "DELETE",
            headers: authHeaders()
        });
        await checkRes(response);
        alert("Sucursal eliminada correctamente.");
        render();
    } catch (error) {
        alert("Error al eliminar la sucursal: " + error.message);
    }
}
