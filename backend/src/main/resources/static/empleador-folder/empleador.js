import {
    API_URL,
    authHeaders,
    sesionActiva,
    checkRes,
    checkResSinRedireccion,
    formatearFechaLocal,
    DIAS_SEMANA,
    checksDiasAperturaHTML,
    leerDiasApertura,
    marcarTodosLosDias,
    activarSubidaImagen
} from "../recursos/modulos.js";

const user = await sesionActiva();

if (!user) {
    window.location.href = "../login-folder/Login.html";
}

let miId = null;
let misSucursales = [];

await cargarMe();
await cargarSucursales();
bindEventos();

window.abrirModalModificarSucursal = abrirModalModificarSucursal;
window.abrirModalModificarServicio = abrirModalModificarServicio;
window.eliminarSucursal = eliminarSucursal;
window.eliminarServicio = eliminarServicio;
window.finalizarTurno = finalizarTurno;
window.cancelarTurno = cancelarTurno;
window.verDetalleTurno = verDetalleTurno;
window.verDetalleServicio = verDetalleServicio;
window.eliminarEmpleado = eliminarEmpleado;
window.convertirmeEnEmpleado = convertirmeEnEmpleado;
window.abrirModalImagenes = abrirModalImagenes;
window.eliminarImagenSucursal = eliminarImagenSucursal;

function crearModal(id) {
    const old = document.getElementById(id);
    if (old) old.remove();
    const div = document.createElement("div");
    div.className = "modal-overlay";
    div.id = id;
    div.addEventListener("click", (e) => {
        if (e.target === div) cerrarModal(div);
    });
    document.body.appendChild(div);
    return div;
}

async function cerrarModal(div) {
    try {
        await guardarModal(div, async () => {
            if (div.descartarImagenPendiente) await div.descartarImagenPendiente();
            div.remove();
        });
    } catch (error) {
        alert(error.message);
    }
}

async function guardarModal(div, guardar) {
    if (div.dataset.guardando) return;
    div.dataset.guardando = "true";
    const controles = [...div.querySelectorAll("button, input, textarea, select")]
        .map(control => [control, Boolean(control.disabled)]);
    controles.forEach(([control]) => { control.disabled = true; });
    try {
        await guardar();
    } finally {
        controles.forEach(([control, deshabilitado]) => { control.disabled = deshabilitado; });
        delete div.dataset.guardando;
    }
}

// Enciende los botones "Todos" / "Ninguno" de los checks de dias de apertura.
function activarDiasApertura(prefijo, contenedor) {
    contenedor.querySelectorAll("[data-marcar-todos]").forEach(btn => {
        btn.addEventListener("click", () => marcarTodosLosDias(prefijo, true));
    });
    contenedor.querySelectorAll("[data-marcar-ninguno]").forEach(btn => {
        btn.addEventListener("click", () => marcarTodosLosDias(prefijo, false));
    });
}

async function cargarMe() {
    try {
        const res = await fetch(API_URL + "/usuarios/me", { headers: authHeaders() });
        await checkRes(res);
        const data = await res.json();
        miId = data.id;
    } catch (e) {
        alert("No se pudo obtener tu sesión: " + e.message);
    }
}

async function cargarSucursales() {
    try {
        const res = await fetch(API_URL + "/sucursales/listar/propias", { headers: authHeaders() });
        await checkRes(res);
        const todas = await res.json();
        misSucursales = todas;
        renderSucursales(misSucursales);
        poblarSelectsSucursal(misSucursales);
    } catch (e) {
        alert("Error al cargar sucursales: " + e.message);
    }
}

function renderSucursales(sucursales) {
    const body = document.getElementById("sucursalesBody");
    body.innerHTML = "";

    if (!sucursales.length) {
        body.innerHTML = `<tr><td colspan="4" style="color:var(--t3);font-family:var(--mono);font-size:12px;">Sin sucursales</td></tr>`;
        return;
    }

    sucursales.forEach(s => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>${s.nombre}</td>
            <td>${s.categoria}</td>
            <td>${s.puntuacion ? "🐝".repeat(Math.round(s.puntuacion)) : 'Sin calificar'}</td>
            <td>
                <div class="table-actions">
                    <button onclick="abrirModalModificarSucursal(${s.id})">Editar</button>
                    <button onclick="convertirmeEnEmpleado(${s.id})">Ser empleado</button>
                    <button onclick="abrirModalImagenes(${s.id})">Imágenes</button>
                    <button onclick="eliminarSucursal(${s.id})" class="btn-danger">Borrar</button>
                </div>
            </td>
        `;
        body.appendChild(tr);
    });
}

function poblarSelectsSucursal(sucursales) {
    const ids = ["filtro-sucursal-servicios", "filtro-sucursal-turnos", "filtro-sucursal-empleados"];
    ids.forEach(id => {
        const sel = document.getElementById(id);
        sel.innerHTML = `<option value="">Seleccioná una sucursal</option>`;
        sucursales.forEach(s => {
            const opt = document.createElement("option");
            opt.value = s.id;
            opt.textContent = s.nombre;
            sel.appendChild(opt);
        });
    });
}

async function eliminarSucursal(sucursalId) {
    if (!confirm("¿Borrar esta sucursal?")) return;
    try {
        const res = await fetch(API_URL + `/sucursales/borrar-sucursal-propia/${sucursalId}`, {
            method: "DELETE",
            headers: authHeaders()
        });
        await checkRes(res);
        const msg = await res.text();
        alert(msg);
        if (res.ok) await cargarSucursales();
    } catch (e) {
        alert("Error: " + e.message);
    }
}

async function cargarServicios(sucursalId) {
    const body = document.getElementById("serviciosBody");
    body.innerHTML = "";
    if (!sucursalId) return;

    try {
        const res = await fetch(API_URL + `/servicios/listar/sucursal/${sucursalId}`, { headers: authHeaders() });
        await checkRes(res);
        const servicios = await res.json();

        if (!servicios.length) {
            body.innerHTML = `<tr><td colspan="3" style="color:var(--t3);font-family:var(--mono);font-size:12px;">Sin servicios</td></tr>`;
            return;
        }

        servicios.forEach(s => {
            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td>${s.nombre}</td>
                <td>${s.duracion} min</td>
                <td>
                    <div class="table-actions">
                        <button onclick="verDetalleServicio(${s.id})">Detalle</button>
                        <button onclick="abrirModalModificarServicio(${s.id})">Editar</button>
                        <button onclick="eliminarServicio(${s.id}, ${sucursalId})" style="color:var(--red);">Borrar</button>
                    </div>
                </td>
            `;
            body.appendChild(tr);
        });

    } catch (e) {
        alert("Error al cargar servicios: " + e.message);
    }
}

async function eliminarServicio(servicioId, sucursalId) {
    if (!confirm("¿Borrar este servicio?")) return;
    try {
        const res = await fetch(API_URL + `/servicios/eliminar/${servicioId}`, {
            method: "DELETE",
            headers: authHeaders()
        });
        await checkRes(res);
        const msg = await res.text();
        alert(msg);
        if (res.ok) await cargarServicios(sucursalId);
    } catch (e) {
        alert("Error: " + e.message);
    }
}

async function verDetalleServicio(servicioId) {
    const div = crearModal("modal-detalle-servicio");
    div.innerHTML = `
        <div class="form-simple">
            <h1>Detalle del Servicio</h1>
            <div id="detalle-servicio-body" style="color:var(--t3);font-size:12px;">Cargando...</div>
            <div class="form-actions" style="margin-top:20px;">
                <button class="btn-cancel" onclick="this.closest('.modal-overlay').remove()">Cerrar</button>
            </div>
        </div>
    `;

    try {
        const res = await fetch(API_URL + `/servicios/${servicioId}`, { headers: authHeaders() });
        await checkRes(res);
        const s = await res.json();
        const body = document.getElementById("detalle-servicio-body");
        if (!body) return;

        const fila = (label, valor) => `
            <p style="margin:0 0 8px;"><strong>${label}:</strong> ${valor ?? "-"}</p>
        `;

        let html = "";
        if (s.fotoPerfil) {
            html += `<img src="${s.fotoPerfil}" alt="" style="width:100%;max-height:160px;object-fit:cover;border-radius:var(--r-sm);margin-bottom:12px;">`;
        }
        html += fila("Nombre", s.nombre);
        html += fila("Descripción", s.descripcion);
        html += fila("Duración", `${s.duracion} min`);
        html += fila("Precio", `$${s.precio}`);
        html += fila("Puntuación", s.puntuacion ? "🐝".repeat(Math.round(s.puntuacion)) : "Sin reseñas");

        html += `<hr style="border-color:var(--b);margin:14px 0;">`;
        html += `<p style="font-weight:600;color:var(--t);margin-bottom:8px;">Sucursal</p>`;
        html += fila("Nombre", s.nombreSucursal);
        html += fila("Dirección", s.direccionSucursal);
        html += fila("Categoría", s.categoriaSucursal);

        body.innerHTML = html;
    } catch (e) {
        const body = document.getElementById("detalle-servicio-body");
        if (body) body.innerHTML = `<div style="color:var(--red);font-size:12px;">Error al cargar el detalle: ${e.message}</div>`;
    }
}

async function convertirmeEnEmpleado(sucursalId) {
    if (!confirm("¿Querés convertirte en empleado de esta sucursal?")) return;
    try {
        const res = await fetch(API_URL + `/sucursales/${sucursalId}/convertirme-empleado`, {
            method: "PATCH",
            headers: authHeaders()
        });
        await checkRes(res);
        const msg = await res.text();
        alert(msg);
    } catch (e) {
        alert("Error: " + e.message);
    }
}

async function cargarTurnos(sucursalId) {
    const body = document.getElementById("turnosBody");
    body.innerHTML = "";
    if (!sucursalId) return;

    const estado = document.getElementById("filtro-estado-turnos").value;
    try {
        const res = await fetch(
            API_URL + `/turnos/de-sucursal/${sucursalId}?estadoTurno=${estado}`,
            { headers: authHeaders() }
        );
        await checkRes(res);
        const turnos = await res.json();

        if (!turnos.length) {
            body.innerHTML = `<tr><td colspan="4" style="color:var(--t3);font-family:var(--mono);font-size:12px;">Sin turnos</td></tr>`;
            return;
        }

        turnos.forEach(t => {
            const fecha = formatearFechaLocal(t.fechaTurno);
            const estado = t.estadoTurno ?? "-";

            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td>${t.nombreServicio ?? "-"}</td>
                <td>${fecha}</td>
                <td><span class="badge badge--blue">${estado}</span></td>
                <td>
                    <div class="table-actions">
                        <button onclick="verDetalleTurno(${t.id})">Detalle</button>
                        <button onclick="finalizarTurno(${t.id}, ${sucursalId})">Finalizar</button>
                        <button onclick="cancelarTurno(${t.id}, ${sucursalId})" style="color:var(--red);">Cancelar</button>
                    </div>
                </td>
            `;
            body.appendChild(tr);
        });

    } catch (e) {
        alert("Error al cargar turnos: " + e.message);
    }
}

document.getElementById("filtro-estado-turnos").addEventListener("change", () => {
    const sucursalId = document.getElementById("filtro-sucursal-turnos").value;
    cargarTurnos(sucursalId);
});

async function finalizarTurno(turnoId, sucursalId) {
    if (!confirm("¿Finalizar este turno?")) return;
    try {
        const res = await fetch(API_URL + `/turnos/finalizar/${turnoId}`, {
            method: "PATCH",
            headers: authHeaders()
        });
        await checkRes(res);
        const msg = await res.text();
        alert(msg);
        if (res.ok) await cargarTurnos(sucursalId);
    } catch (e) {
        alert("Error: " + e.message);
    }
}

async function cancelarTurno(turnoId, sucursalId) {
    if (!confirm("¿Cancelar este turno?")) return;
    try {
        const res = await fetch(API_URL + `/turnos/cancelar/${turnoId}`, {
            method: "DELETE",
            headers: authHeaders()
        });
        await checkRes(res);
        const msg = await res.text();
        alert(msg);
        if (res.ok) await cargarTurnos(sucursalId);
    } catch (e) {
        alert("Error: " + e.message);
    }
}

async function verDetalleTurno(turnoId) {
    const div = crearModal("modal-detalle-turno");
    div.innerHTML = `
        <div class="form-simple">
            <h1>Detalle del Turno</h1>
            <div id="detalle-turno-body" style="color:var(--t3);font-size:12px;">Cargando...</div>
            <div class="form-actions" style="margin-top:20px;">
                <button class="btn-cancel" onclick="this.closest('.modal-overlay').remove()">Cerrar</button>
            </div>
        </div>
    `;

    try {
        const res = await fetch(API_URL + `/turnos/de-sucursal/detalles/${turnoId}`, { headers: authHeaders() });
        await checkRes(res);
        const t = await res.json();
        const body = document.getElementById("detalle-turno-body");
        if (!body) return;

        const fechaTurno = formatearFechaLocal(t.fechaTurno);
        const fechaReserva = formatearFechaLocal(t.fechaReserva);

        const fila = (label, valor) => `
            <p style="margin:0 0 8px;"><strong>${label}:</strong> ${valor ?? "-"}</p>
        `;

        let html = "";
        html += fila("Estado", `<span class="badge badge--blue">${t.estadoTurno}</span>`);
        html += fila("Fecha del turno", fechaTurno);
        html += fila("Fecha de reserva", fechaReserva);

        html += `<hr style="border-color:var(--b);margin:14px 0;">`;
        html += `<p style="font-weight:600;color:var(--t);margin-bottom:8px;">Servicio</p>`;
        html += fila("Nombre", t.servicio?.nombre);
        html += fila("Duración", t.servicio ? `${t.servicio.duracion} min` : "-");
        html += fila("Precio", t.servicio ? `$${t.servicio.precio}` : "-");

        html += `<hr style="border-color:var(--b);margin:14px 0;">`;
        html += `<p style="font-weight:600;color:var(--t);margin-bottom:8px;">Cliente</p>`;
        html += fila("Nombre", t.cliente ? `${t.cliente.nombre} ${t.cliente.apellido}` : "-");

        html += `<hr style="border-color:var(--b);margin:14px 0;">`;
        html += `<p style="font-weight:600;color:var(--t);margin-bottom:8px;">Empleado</p>`;
        html += fila("Nombre", t.empleado ? `${t.empleado.nombre} ${t.empleado.apellido}` : "-");
        html += fila("Email", t.empleado?.email);

        if (t.resenia) {
            html += `<hr style="border-color:var(--b);margin:14px 0;">`;
            html += `<p style="font-weight:600;color:var(--t);margin-bottom:8px;">Reseña</p>`;
            html += fila("Puntuación", "🐝".repeat(Math.round(t.resenia.puntuacion)));
            html += fila("Comentario", t.resenia.comentario);
            html += fila("Fecha", t.resenia.fechaResenia);
        }

        body.innerHTML = html;
    } catch (e) {
        const body = document.getElementById("detalle-turno-body");
        if (body) body.innerHTML = `<div style="color:var(--red);font-size:12px;">Error al cargar el detalle: ${e.message}</div>`;
    }
}

async function cargarEmpleados(sucursalId) {
    const body = document.getElementById("empleadosBody");
    body.innerHTML = "";
    if (!sucursalId) return;

    try {
        const res = await fetch(API_URL + `/sucursales/${sucursalId}/empleados`, { headers: authHeaders() });
        await checkRes(res);
        const empleados = await res.json();

        if (!empleados.length) {
            body.innerHTML = `<tr><td colspan="3" style="color:var(--t3);font-family:var(--mono);font-size:12px;">Sin empleados</td></tr>`;
            return;
        }

        empleados.forEach(e => {
            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td>${e.nombre}</td>
                <td>${e.apellido}</td>
                <td>
                    <div class="table-actions">
                        <button onclick="eliminarEmpleado(${sucursalId}, ${e.id})" style="color:var(--red);">Quitar</button>
                    </div>
                </td>
            `;
            body.appendChild(tr);
        });

    } catch (e) {
        alert("Error al cargar empleados: " + e.message);
    }
}

async function agregarEmpleado(sucursalId) {
    const email = document.getElementById("input-email-empleado").value.trim();
    if (!email) { alert("Ingresá el email del empleado"); return; }

    try {
        const res = await fetch(API_URL + `/sucursales/${sucursalId}/empleados/agregar`, {
            method: "POST",
            headers: authHeaders(),
            body: JSON.stringify({ email })
        });
        await checkRes(res);
        const msg = await res.text();
        alert(msg);
        if (res.ok) {
            document.getElementById("input-email-empleado").value = "";
            await cargarEmpleados(sucursalId);
        }
    } catch (e) {
        alert("Error: " + e.message);
    }
}

async function eliminarEmpleado(sucursalId, empleadoId) {
    if (!confirm("¿Quitar este empleado?")) return;
    try {
        const res = await fetch(API_URL + `/sucursales/${sucursalId}/empleados/eliminar/${empleadoId}`, {
            method: "DELETE",
            headers: authHeaders()
        });
        await checkRes(res);
        const msg = await res.text();
        alert(msg);
        if (res.ok) await cargarEmpleados(sucursalId);
    } catch (e) {
        alert("Error: " + e.message);
    }
}

async function abrirModalModificarSucursal(sucursalId) {
    let suc;
    try {
        const res = await fetch(API_URL + `/sucursales/${sucursalId}`, { headers: authHeaders() });
        await checkRes(res);
        suc = await res.json();
    } catch (e) {
        alert("Error al cargar la sucursal: " + e.message);
        return;
    }

    const div = crearModal("modal-suc");
    div.innerHTML = `
        <div class="form-simple" style="max-width:600px;">
            <h1>Editar Sucursal</h1>
            <p>Nombre</p>
            <div class="input-group">
                <input type="text" class="form-control" id="mod-suc-nombre" value="${suc.nombre ?? ""}">
            </div>
            <p>Dirección</p>
            <div class="input-group">
                <input type="text" class="form-control" id="mod-suc-direccion" value="${suc.direccion ?? ""}">
            </div>
            <p>Teléfono</p>
            <div class="input-group">
                <input type="text" class="form-control" id="mod-suc-telefono" value="${suc.telefono ?? ""}">
            </div>
            <p>Descripción</p>
            <div class="input-group">
                <textarea class="form-control" id="mod-suc-descripcion" rows="3" style="resize:vertical;padding:8px 12px;height:auto;">${suc.descripcion ?? ""}</textarea>
            </div>
            <p>Hora apertura</p>
            <div class="input-group">
                <input type="time" class="form-control" id="mod-suc-horaApertura" value="${suc.horaApertura}">
            </div>
            <p>Hora cierre</p>
            <div class="input-group">
                <input type="time" class="form-control" id="mod-suc-horaCierre" value="${suc.horaCierre}">
            </div>
            <p>Días de apertura</p>
            <div class="dias-apertura">
                <div class="dias-apertura__acciones">
                    <button type="button" class="btn-ghost btn-ghost--mini" data-no-cooldown data-marcar-todos="mod-suc">Todos</button>
                    <button type="button" class="btn-ghost btn-ghost--mini" data-no-cooldown data-marcar-ninguno="mod-suc">Ninguno</button>
                </div>
                <div class="dias-apertura__checks">
                    ${checksDiasAperturaHTML("mod-suc", suc.diasApertura)}
                </div>
                <p class="dias-apertura__ayuda">Desmarcá los días que la sucursal no abre.</p>
            </div>
            <p>URL foto</p>
            <div class="input-group">
                <input type="file" class="form-control" id="mod-suc-fotoFile" accept="image/jpeg,image/png,image/webp,image/gif">
            </div>
            <p id="mod-suc-fotoStatus" style="font-size:12px;color:var(--t3);">También podés elegir una imagen de hasta 5 MB.</p>
            <div class="input-group">
                <input type="url" class="form-control" id="mod-suc-fotoUrl" value="${suc.fotoPerfil ?? ""}">
            </div>
            <input type="hidden" id="mod-suc-fotoPublicId">
            <hr style="border-color:var(--b);margin:20px 0;">
            <p style="font-size:13px;font-weight:600;color:var(--t);margin-bottom:10px;">Empleados</p>
            <div id="empleados-lista-modal" style="color:var(--t3);font-size:12px;">Cargando...</div>
            <div class="form-actions" style="margin-top:20px;">
                <button class="btn-submit" id="btn-guardar-suc">Guardar</button>
                <button class="btn-cancel">Cancelar</button>
            </div>
        </div>
    `;

    const subirFotoPendiente = activarSubidaImagen("mod-suc-fotoFile", "mod-suc-fotoUrl", "mod-suc-fotoPublicId", "SUCURSAL", "mod-suc-fotoStatus");
    div.descartarImagenPendiente = subirFotoPendiente.descartar;
    div.querySelector(".btn-cancel").addEventListener("click", () => cerrarModal(div));

    try {
        const res = await fetch(API_URL + `/sucursales/${sucursalId}/empleados`, { headers: authHeaders() });
        await checkRes(res);
        const empleados = await res.json();
        const container = document.getElementById("empleados-lista-modal");

        if (!empleados || !empleados.length) {
            container.innerHTML = '<div style="color:var(--t3);font-family:var(--mono);font-size:11px;">Sin empleados</div>';
        } else {
            container.innerHTML = empleados.map(e => {
                const avatar = e.urlFotoPerfil
                    ? `<img src="${e.urlFotoPerfil}" alt="" style="width:28px;height:28px;border-radius:50%;object-fit:cover;flex-shrink:0;">`
                    : `<div style="width:28px;height:28px;border-radius:50%;background:var(--s2);color:var(--t2);display:flex;align-items:center;justify-content:center;font-size:11px;font-weight:600;flex-shrink:0;">${(e.nombre || "?")[0].toUpperCase()}</div>`;
                return `
                    <div style="display:flex;align-items:center;gap:8px;padding:5px 0;border-bottom:1px solid var(--b);">
                        ${avatar}
                        <div style="flex:1;min-width:0;">
                            <div style="font-size:12px;font-weight:500;color:var(--t);">${e.nombre} ${e.apellido}</div>
                            <div style="font-size:10.5px;color:var(--t3);font-family:var(--mono);">${e.email}</div>
                        </div>
                    </div>`;
            }).join("");
        }
    } catch (e) {
        const container = document.getElementById("empleados-lista-modal");
        if (container) container.innerHTML = '<div style="color:var(--red);font-size:12px;">Error al cargar empleados</div>';
    }

    activarDiasApertura("mod-suc", div);

    document.getElementById("btn-guardar-suc").addEventListener("click", async () => {
        const diasApertura = leerDiasApertura("mod-suc");

        if (!diasApertura.length) {
            alert("Tenés que marcar al menos un día de apertura");
            return;
        }

        await guardarModal(div, async () => {
            try {
                await subirFotoPendiente();
                const body = {
                    nombre: document.getElementById("mod-suc-nombre").value || undefined,
                    direccion: document.getElementById("mod-suc-direccion").value || undefined,
                    telefono: document.getElementById("mod-suc-telefono").value || undefined,
                    descripcion: document.getElementById("mod-suc-descripcion").value || undefined,
                    horaApertura: document.getElementById("mod-suc-horaApertura").value || undefined,
                    horaCierre: document.getElementById("mod-suc-horaCierre").value || undefined,
                    diasApertura,
                    fotoUrl: document.getElementById("mod-suc-fotoUrl").value || undefined,
                    fotoPublicId: document.getElementById("mod-suc-fotoPublicId").value || undefined,
                };
                const res = await fetch(API_URL + `/sucursales/modificar/${sucursalId}`, {
                    method: "PATCH",
                    headers: authHeaders(),
                    body: JSON.stringify(body)
                });
                await checkResSinRedireccion(res);
                subirFotoPendiente.confirmar();
                const msg = await res.text();
                alert(msg);
                if (res.ok) { div.remove(); await cargarSucursales(); }
            } catch (e) {
                try { await subirFotoPendiente.descartar(); }
                catch (limpieza) { alert(limpieza.message); }
                alert("Error: " + e.message);
            }
        });
    });
}

async function abrirModalModificarServicio(servicioId) {
    try {
        const res = await fetch(API_URL + `/servicios/${servicioId}`, { headers: authHeaders() });
        await checkRes(res);
        const s = await res.json();

        const div = crearModal("modal-serv");
        div.innerHTML = `
            <div class="form-simple">
                <h1>Editar Servicio</h1>
                <p>Nombre</p>
                <div class="input-group">
                    <input type="text" class="form-control" id="mod-serv-nombre" value="${s.nombre}">
                </div>
                <p>Descripción</p>
                <div class="input-group">
                    <textarea class="form-control" id="mod-serv-descripcion" rows="3" style="resize:vertical;padding:8px 12px;height:auto;">${s.descripcion}</textarea>
                </div>
                <p>Duración (min)</p>
                <div class="input-group">
                    <input type="number" class="form-control" id="mod-serv-duracion" value="${s.duracion}" min="30" max="360">
                </div>
                <p>Precio</p>
                <div class="input-group">
                    <input type="number" class="form-control" id="mod-serv-precio" value="${s.precio}" min="0.01" step="0.01">
                </div>
                <p>URL foto</p>
                <div class="input-group">
                    <input type="file" class="form-control" id="mod-serv-fotoFile" accept="image/jpeg,image/png,image/webp,image/gif">
                </div>
                <p id="mod-serv-fotoStatus" style="font-size:12px;color:var(--t3);">También podés elegir una imagen de hasta 5 MB.</p>
                <div class="input-group">
                    <input type="url" class="form-control" id="mod-serv-fotoUrl" value="${s.fotoPerfil ?? ""}">
                </div>
                <input type="hidden" id="mod-serv-fotoPublicId">
                <div class="form-actions">
                    <button class="btn-submit" id="btn-guardar-serv">Guardar</button>
                    <button class="btn-cancel">Cancelar</button>
                </div>
            </div>
        `;

        const subirFotoPendiente = activarSubidaImagen("mod-serv-fotoFile", "mod-serv-fotoUrl", "mod-serv-fotoPublicId", "SERVICIO", "mod-serv-fotoStatus");
        div.descartarImagenPendiente = subirFotoPendiente.descartar;
        div.querySelector(".btn-cancel").addEventListener("click", () => cerrarModal(div));

        document.getElementById("btn-guardar-serv").addEventListener("click", async () => {
            await guardarModal(div, async () => {
                try {
                    await subirFotoPendiente();
                    const body = {
                        nombre: document.getElementById("mod-serv-nombre").value || undefined,
                        descripcion: document.getElementById("mod-serv-descripcion").value || undefined,
                        duracion: parseInt(document.getElementById("mod-serv-duracion").value) || undefined,
                        precio: parseFloat(document.getElementById("mod-serv-precio").value) || undefined,
                        fotoUrl: document.getElementById("mod-serv-fotoUrl").value || undefined,
                        fotoPublicId: document.getElementById("mod-serv-fotoPublicId").value || undefined,
                    };
                    const res = await fetch(API_URL + `/servicios/modificar/${servicioId}`, {
                        method: "PATCH",
                        headers: authHeaders(),
                        body: JSON.stringify(body)
                    });
                    await checkResSinRedireccion(res);
                    subirFotoPendiente.confirmar();
                    const msg = await res.text();
                    alert(msg);
                    if (res.ok) {
                        div.remove();
                        const sucursalId = document.getElementById("filtro-sucursal-servicios").value;
                        if (sucursalId) await cargarServicios(sucursalId);
                    }
                } catch (e) {
                    try { await subirFotoPendiente.descartar(); }
                    catch (limpieza) { alert(limpieza.message); }
                    alert("Error: " + e.message);
                }
            });
        });

    } catch (e) {
        alert("Error al cargar servicio: " + e.message);
    }
}

function abrirModalAgregarSucursal() {
    const div = crearModal("modal-agregar-suc");
    div.innerHTML = `
        <div class="form-simple">
            <h1>Agregar Sucursal</h1>

            <p>Nombre de la sucursal</p>
            <div class="input-group">
                <input type="text" class="form-control" placeholder="Nombre" id="add-suc-nombre" minlength="4" maxlength="120" required>
            </div>

            <p>Dirección</p>
            <div class="input-group">
                <input type="text" class="form-control" placeholder="Dirección" id="add-suc-direccion" minlength="10" maxlength="60" required>
            </div>

            <p>Teléfono</p>
            <div class="input-group">
                <input type="tel" class="form-control" placeholder="Teléfono" id="add-suc-telefono" minlength="8" maxlength="20">
            </div>

            <p>Descripción</p>
            <div class="input-group">
                <textarea class="form-control" placeholder="Descripción" id="add-suc-descripcion" minlength="10" maxlength="1500" rows="4" required style="resize:vertical;padding:8px 12px;height:auto;"></textarea>
            </div>

            <p>Categoría</p>
            <div class="input-group">
                <select class="form-control" id="add-suc-categoria" required>
                    <option value="">Seleccioná una categoría</option>
                </select>
            </div>

            <p>Hora de apertura</p>
            <div class="input-group">
                <input type="time" class="form-control" id="add-suc-horaApertura" required>
            </div>

            <p>Hora de cierre</p>
            <div class="input-group">
                <input type="time" class="form-control" id="add-suc-horaCierre" required>
            </div>

            <p>Días de apertura</p>
            <div class="dias-apertura">
                <div class="dias-apertura__acciones">
                    <button type="button" class="btn-ghost btn-ghost--mini" data-no-cooldown data-marcar-todos="add-suc">Todos</button>
                    <button type="button" class="btn-ghost btn-ghost--mini" data-no-cooldown data-marcar-ninguno="add-suc">Ninguno</button>
                </div>
                <div class="dias-apertura__checks">
                    ${checksDiasAperturaHTML("add-suc", DIAS_SEMANA.map(d => d.valor))}
                </div>
                <p class="dias-apertura__ayuda">Desmarcá los días que la sucursal no abre.</p>
            </div>

            <p>URL foto de perfil</p>
            <div class="input-group">
                <input type="file" class="form-control" id="add-suc-fotoFile" accept="image/jpeg,image/png,image/webp,image/gif">
            </div>
            <p id="add-suc-fotoStatus" style="font-size:12px;color:var(--t3);">También podés subir una imagen de hasta 5 MB.</p>
            <div class="input-group">
                <input type="url" class="form-control" placeholder="https://..." id="add-suc-fotoUrl" required>
            </div>
            <input type="hidden" id="add-suc-fotoPublicId">

            <div class="form-actions">
                <button class="btn-submit" id="btn-crear-suc">Enviar</button>
                <button class="btn-cancel">Cancelar</button>
            </div>
        </div>
    `;

    const subirFotoPendiente = activarSubidaImagen("add-suc-fotoFile", "add-suc-fotoUrl", "add-suc-fotoPublicId", "SUCURSAL", "add-suc-fotoStatus");
    div.descartarImagenPendiente = subirFotoPendiente.descartar;
    div.querySelector(".btn-cancel").addEventListener("click", () => cerrarModal(div));

    cargarCategoriasSucursal();

    activarDiasApertura("add-suc", div);

    document.getElementById("btn-crear-suc").addEventListener("click", async (e) => {
        e.preventDefault();

        const nombre = document.getElementById("add-suc-nombre").value;
        const direccion = document.getElementById("add-suc-direccion").value;
        const telefono = document.getElementById("add-suc-telefono").value;
        const descripcion = document.getElementById("add-suc-descripcion").value;
        const categoriaId = document.getElementById("add-suc-categoria").value;
        const horaApertura = document.getElementById("add-suc-horaApertura").value;
        const horaCierre = document.getElementById("add-suc-horaCierre").value;
        const diasApertura = leerDiasApertura("add-suc");

        if (!diasApertura.length) {
            alert("Tenés que marcar al menos un día de apertura");
            return;
        }

        try {
            await guardarModal(div, async () => {
                await subirFotoPendiente();
                await postSucursal(nombre, direccion, telefono, descripcion, categoriaId, horaApertura, horaCierre, diasApertura,
                    document.getElementById("add-suc-fotoUrl").value,
                    document.getElementById("add-suc-fotoPublicId").value, div);
                subirFotoPendiente.confirmar();
            });
        } catch (error) {
            try { await subirFotoPendiente.descartar(); }
            catch (limpieza) { alert(limpieza.message); }
            alert("Error al guardar: " + error.message);
        }
    });
}

async function cargarCategoriasSucursal() {
    try {
        const response = await fetch(API_URL + `/sucursales/categorias`, {
            headers: authHeaders()
        });

        await checkRes(response);

        const categorias = await response.json();
        const select = document.getElementById("add-suc-categoria");

        categorias.forEach((cat, i) => {
            const option = document.createElement("option");
            option.value = i + 1;
            option.textContent = cat;
            select.appendChild(option);
        });

    } catch (error) {
        alert("Error al cargar categorías: " + error.message);
    }
}

async function postSucursal(nombre, direccion, telefono, descripcion, categoriaId, horaApertura, horaCierre, diasApertura, fotoUrl, fotoPublicId, modalDiv) {
    const datos = {
        nombre,
        direccion,
        telefono,
        descripcion,
        categoriaId,
        horaApertura,
        horaCierre,
        diasApertura,
        fotoUrl,
        fotoPublicId: fotoPublicId || undefined
    };

    const response = await fetch(API_URL + `/sucursales/crear`, {
            method: "POST",
            headers: authHeaders(),
            body: JSON.stringify(datos)
    });

    await checkResSinRedireccion(response);
    const data = await response.text();
    alert(data);

    if (response.ok) {
        modalDiv.remove();
        await cargarSucursales();
    }
}

function abrirModalAgregarServicio(sucursalId) {
    const div = crearModal("modal-agregar-serv");
    div.innerHTML = `
        <div class="form-simple">
            <h1>Agregar Servicio</h1>

            <p>Nombre del servicio</p>
            <div class="input-group">
                <input type="text" class="form-control" placeholder="Nombre" id="add-serv-nombre" minlength="4" maxlength="60" required>
            </div>

            <p>Descripción</p>
            <div class="input-group">
                <textarea class="form-control" placeholder="Descripción" id="add-serv-descripcion" minlength="10" maxlength="500" rows="4" required style="resize:vertical;padding:8px 12px;height:auto;"></textarea>
            </div>

            <p>Duración (minutos)</p>
            <div class="input-group">
                <input type="number" class="form-control" placeholder="Ej: 30" id="add-serv-duracion" min="30" max="360" required>
            </div>

            <p>Precio</p>
            <div class="input-group">
                <input type="number" class="form-control" placeholder="Ej: 1500.00" id="add-serv-precio" min="0.01" max="999999.99" step="0.01" required>
            </div>

            <p>URL foto de perfil</p>
            <div class="input-group">
                <input type="file" class="form-control" id="add-serv-fotoFile" accept="image/jpeg,image/png,image/webp,image/gif">
            </div>
            <p id="add-serv-fotoStatus" style="font-size:12px;color:var(--t3);">También podés subir una imagen de hasta 5 MB.</p>
            <div class="input-group">
                <input type="url" class="form-control" placeholder="https://..." id="add-serv-fotoUrl" required>
            </div>
            <input type="hidden" id="add-serv-fotoPublicId">

            <div class="form-actions">
                <button class="btn-submit" id="btn-crear-serv">Enviar</button>
                <button class="btn-cancel">Cancelar</button>
            </div>
        </div>
    `;

    const subirFotoPendiente = activarSubidaImagen("add-serv-fotoFile", "add-serv-fotoUrl", "add-serv-fotoPublicId", "SERVICIO", "add-serv-fotoStatus");
    div.descartarImagenPendiente = subirFotoPendiente.descartar;
    div.querySelector(".btn-cancel").addEventListener("click", () => cerrarModal(div));

    document.getElementById("btn-crear-serv").addEventListener("click", async (e) => {
        e.preventDefault();

        const nombre = document.getElementById("add-serv-nombre").value;
        const descripcion = document.getElementById("add-serv-descripcion").value;
        const duracion = parseInt(document.getElementById("add-serv-duracion").value);
        const precio = parseFloat(document.getElementById("add-serv-precio").value);
        try {
            await guardarModal(div, async () => {
                await subirFotoPendiente();
                await postServicio(nombre, descripcion, duracion, precio, sucursalId,
                    document.getElementById("add-serv-fotoUrl").value,
                    document.getElementById("add-serv-fotoPublicId").value, div);
                subirFotoPendiente.confirmar();
            });
        } catch (error) {
            try { await subirFotoPendiente.descartar(); }
            catch (limpieza) { alert(limpieza.message); }
            alert("Error al guardar: " + error.message);
        }
    });
}

async function postServicio(nombre, descripcion, duracion, precio, sucursalId, fotoUrl, fotoPublicId, modalDiv) {
    const datos = {
        nombre,
        descripcion,
        duracion,
        precio,
        sucursalId,
        fotoUrl,
        fotoPublicId: fotoPublicId || undefined
    };

    const response = await fetch(API_URL + `/servicios/crear`, {
            method: "POST",
            headers: authHeaders(),
            body: JSON.stringify(datos)
    });

    await checkResSinRedireccion(response);
    const data = await response.text();
    alert(data);

    if (response.ok) {
        modalDiv.remove();
        await cargarServicios(sucursalId);
    }
}


async function abrirModalImagenes(sucursalId) {
    const div = crearModal("modal-imagenes");
    div.innerHTML = `
        <div class="form-simple" style="max-width:700px;">
            <h1>Imágenes de la Sucursal</h1>
            
            <div id="imagenes-grid" style="display:grid; grid-template-columns:repeat(auto-fill, minmax(150px, 1fr)); gap:12px; margin:20px 0;">
                <div style="color:var(--t3);font-size:12px;">Cargando imágenes...</div>
            </div>
            
            <hr style="border-color:var(--b);margin:20px 0;">
            
            <h2 style="font-size:16px; margin-bottom:12px;">Agregar Imágenes</h2>
            <p style="font-size:12px; color:var(--t3); margin-bottom:8px;">Subí archivos a Cloudinary o ingresá URLs de imágenes.</p>
            <div class="input-group">
                <input type="file" class="form-control" id="imagenes-archivos" accept="image/jpeg,image/png,image/webp,image/gif" multiple>
            </div>
            <p id="imagenes-upload-status" style="font-size:12px;color:var(--t3);"></p>
            
            <div class="input-group">
                <input type="url" class="form-control" id="input-imagen-url-1" placeholder="https://ejemplo.com/imagen1.jpg">
            </div>
            <div class="input-group">
                <input type="url" class="form-control" id="input-imagen-url-2" placeholder="https://ejemplo.com/imagen2.jpg">
            </div>
            <div class="input-group">
                <input type="url" class="form-control" id="input-imagen-url-3" placeholder="https://ejemplo.com/imagen3.jpg">
            </div>
            <div class="input-group">
                <input type="url" class="form-control" id="input-imagen-url-4" placeholder="https://ejemplo.com/imagen4.jpg">
            </div>
            <div class="input-group">
                <input type="url" class="form-control" id="input-imagen-url-5" placeholder="https://ejemplo.com/imagen5.jpg">
            </div>
            
            <div class="form-actions" style="margin-top:20px;">
                <button class="btn-submit" id="btn-agregar-imagenes">Agregar Imágenes</button>
                <button class="btn-cancel" id="btn-cerrar-imagenes">Cerrar</button>
            </div>
        </div>
    `;

    await cargarImagenesEnModal(sucursalId);
    document.getElementById("btn-cerrar-imagenes").addEventListener("click", () => cerrarModal(div));

    document.getElementById("imagenes-archivos").addEventListener("change", async (event) => {
        const input = event.currentTarget;
        const archivos = Array.from(input.files || []);
        if (!archivos.length) return;

        const cantidadActual = document.querySelectorAll("#imagenes-grid img").length;
        if (cantidadActual + archivos.length > 5) {
            alert(`Esta sucursal admite hasta 5 imágenes. Ya tiene ${cantidadActual}.`);
            input.value = "";
            return;
        }

        const status = document.getElementById("imagenes-upload-status");
        await guardarModal(div, async () => {
            try {
                for (let indice = 0; indice < archivos.length; indice++) {
                    status.textContent = `Subiendo imagen ${indice + 1} de ${archivos.length}…`;
                    const datos = new FormData();
                    datos.append("file", archivos[indice]);
                    const headers = authHeaders();
                    delete headers["Content-Type"];
                    const response = await fetch(API_URL + `/sucursales/${sucursalId}/imagenes/upload`, {
                        method: "POST",
                        headers,
                        body: datos
                    });
                    await checkResSinRedireccion(response);
                }
                status.textContent = "Imágenes cargadas.";
                await cargarImagenesEnModal(sucursalId);
            } catch (error) {
                status.textContent = error.message;
                await cargarImagenesEnModal(sucursalId);
            } finally {
                input.value = "";
            }
        });
    });

    document.getElementById("btn-agregar-imagenes").addEventListener("click", async () => {
        const urls = [];

        for (let i = 1; i <= 5; i++) {
            const url = document.getElementById(`input-imagen-url-${i}`).value.trim();
            if (url) urls.push(url);
        }

        if (urls.length === 0) {
            alert("Ingresá al menos una URL de imagen");
            return;
        }

        await agregarImagenes(sucursalId, urls, div);
    });
}

async function cargarImagenesEnModal(sucursalId) {
    const grid = document.getElementById("imagenes-grid");
    if (!grid) return;

    try {
        const res = await fetch(API_URL + `/sucursales/${sucursalId}/imagenes`, {
            headers: authHeaders()
        });
        await checkRes(res);
        const imagenes = await res.json();

        if (!imagenes || !imagenes.length) {
            grid.innerHTML = `<div style="color:var(--t3);font-family:var(--mono);font-size:12px;grid-column:1/-1;">Sin imágenes</div>`;
            return;
        }

        grid.innerHTML = imagenes.map(img => `
            <div style="position:relative; border-radius:var(--r-sm); overflow:hidden; aspect-ratio:1; background:var(--s2);">
                <img src="${img.url}" alt="Imagen de sucursal" 
                    style="width:100%; height:100%; object-fit:cover;"
                    onerror="this.parentElement.innerHTML='<div style=\\'width:100%;height:100%;display:flex;align-items:center;justify-content:center;color:var(--t3);font-size:10px;\\'>Error al cargar</div>'">
                <button onclick="eliminarImagenSucursal(${sucursalId}, ${img.id})" 
                    style="position:absolute; top:4px; right:4px; background:var(--red); color:white; border:none; border-radius:50%; width:24px; height:24px; display:flex; align-items:center; justify-content:center; cursor:pointer; font-size:14px; line-height:1; opacity:0.9; transition:opacity 0.2s;"
                    onmouseover="this.style.opacity='1'"
                    onmouseout="this.style.opacity='0.9'"
                    title="Eliminar imagen">
                    ×
                </button>
            </div>
        `).join("");

    } catch (e) {
        grid.innerHTML = `<div style="color:var(--red);font-size:12px;grid-column:1/-1;">Error al cargar imágenes: ${e.message}</div>`;
    }
}

async function agregarImagenes(sucursalId, urls, modalDiv) {
    try {
        const res = await fetch(API_URL + `/sucursales/${sucursalId}/agregar/imagenes`, {
            method: "POST",
            headers: authHeaders(),
            body: JSON.stringify({ urls: urls })
        });

        await checkRes(res);
        const msg = await res.text();
        alert(msg);

        if (res.ok) {
            for (let i = 1; i <= 5; i++) {
                document.getElementById(`input-imagen-url-${i}`).value = "";
            }
            await cargarImagenesEnModal(sucursalId);
        }
    } catch (e) {
        alert("Error al agregar imágenes: " + e.message);
    }
}

async function eliminarImagenSucursal(sucursalId, imagenId) {
    if (!confirm("¿Eliminar esta imagen?")) return;

    try {
        const res = await fetch(API_URL + `/sucursales/${sucursalId}/eliminar/imagen/${imagenId}`, {
            method: "DELETE",
            headers: authHeaders()
        });

        await checkRes(res);
        const msg = await res.text();
        alert(msg);

        if (res.ok) {
            await cargarImagenesEnModal(sucursalId);
        }
    } catch (e) {
        alert("Error al eliminar la imagen: " + e.message);
    }
}


function bindEventos() {

    document.getElementById("btn-agregar-sucursal").addEventListener("click", () => {
        abrirModalAgregarSucursal();
    });

    document.getElementById("btn-agregar-servicio").addEventListener("click", () => {
        const sucursalId = document.getElementById("filtro-sucursal-servicios").value;
        if (!sucursalId) { alert("Seleccioná una sucursal primero"); return; }
        abrirModalAgregarServicio(sucursalId);
    });

    document.getElementById("filtro-sucursal-servicios").addEventListener("change", (e) => {
        cargarServicios(e.target.value);
    });

    document.getElementById("filtro-sucursal-turnos").addEventListener("change", (e) => {
        cargarTurnos(e.target.value);
    });

    document.getElementById("filtro-sucursal-empleados").addEventListener("change", (e) => {
        cargarEmpleados(e.target.value);
    });

    document.getElementById("btn-agregar-empleado").addEventListener("click", () => {
        const sucursalId = document.getElementById("filtro-sucursal-empleados").value;
        if (!sucursalId) { alert("Seleccioná una sucursal primero"); return; }
        agregarEmpleado(sucursalId);
    });
}
