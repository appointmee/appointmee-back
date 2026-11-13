import {
    API_URL,
    authHeaders,
    sesionActiva,
    checkRes,
    formatearFechaLocal,
    resumenDiasApertura,
    listaDiasAperturaHTML
} from "../recursos/modulos.js";
const IMG_FALLBACK = "https://xentra.glomastore.mx/img/sin_imagen.png";
const user = await sesionActiva();
if (!user) {
    window.location.href = "../login-folder/Login.html";
}
render();

async function render() {
    const container = document.getElementById("container-info-sucursal");
    if (!container) return;
    const sucursalId = getSucursalIdFromUrl();
    if (!sucursalId) return;
    const sucursal = await fetchSucursal(sucursalId);
    if (!sucursal) return;

    const empleadosHtml = `<div class="empleados-grid" id="empleados-grid"></div>`;

    container.innerHTML = `
        <div class="sucursal-galeria" id="sucursal-galeria">
            <div class="principal-imagen-sucursal">
                <img src="${sucursal.fotoPerfil || 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSQPQenKJTzexez3E1uN7qtSwZ8tgPQsVJ9DQ&s'}" alt="${sucursal.nombre}" id="main-sucursal-img">
            </div>
            ${sucursal.imagenes && sucursal.imagenes.length > 0 ? `
            <div class="galeria-thumbs">
                ${sucursal.imagenes.map((img, i) => `
                    <button class="galeria-thumb${i === 0 ? ' galeria-thumb--activo' : ''}" data-url="${img.url}">
                        <img src="${img.url}" alt="">
                    </button>
                `).join("")}
            </div>` : ""}
        </div>

        <div class="info-sucursal">
            <h3>${sucursal.nombre}</h3>

            <p class="descripcion-sucursal">${sucursal.descripcion}</p>

            <div class="detalle-grid">
                <div class="detalle-item">
                    <span class="detalle-label">Dirección</span>
                    <span class="detalle-value">${sucursal.direccion}</span>
                </div>
                <div class="detalle-item">
                    <span class="detalle-label">Teléfono</span>
                    <span class="detalle-value">${sucursal.telefono}</span>
                </div>
                <div class="detalle-item">
                    <span class="detalle-label">Categoría</span>
                    <span class="detalle-value">${sucursal.categoria}</span>
                </div>
                <div class="detalle-item">
                    <span class="detalle-label">Horario</span>
                    <span class="detalle-value">${sucursal.horaApertura} — ${sucursal.horaCierre}</span>
                </div>
                <div class="detalle-item detalle-item--full">
                    <span class="detalle-label">Días de apertura</span>
                    <span class="detalle-value detalle-value--dias">
                        <span class="detalle-sub">${resumenDiasApertura(sucursal.diasApertura)}</span>
                        <ul class="dias-lista">${listaDiasAperturaHTML(sucursal.diasApertura)}</ul>
                    </span>
                </div>
                <div class="detalle-item">
                    <span class="detalle-label">Puntuación</span>
                   <span class="detalle-value">
    ${sucursal.puntuacion
            ? "🐝".repeat(Math.round(sucursal.puntuacion))
            : "—"}
    (${sucursal.puntuacion ?? 0} · ${sucursal.cantidadPuntuaciones ?? 0})
</span>
                </div>
                <div class="detalle-item">
                    <span class="detalle-label">Empleador</span>
                    <span class="detalle-value">${sucursal.empleador?.nombre ?? "No asignado"}</span>
                </div>
                <div class="detalle-item">
                    <span class="detalle-label">Creación</span>
                    <span class="detalle-value">${sucursal.fechaCreacion}</span>
                </div>
                <div class="detalle-item detalle-item--full">
                    <span class="detalle-label">Empleados</span>
                    ${empleadosHtml}
                </div>
            </div>
        </div>

        <div class="botones-sucursal">
            <button class="btn-secondary" id="btn-volver">Volver</button>
            <button class="btn-primary" id="btn-reservar">Reservar turno</button>
        </div>

        <div class="resenias-seccion" id="resenias-seccion">
            <h3>Reseñas</h3>
            <div id="resenias-lista"></div>
        </div>
    `;

    document.getElementById("btn-volver")
        .addEventListener("click", () => window.history.back());
    document.getElementById("btn-reservar")
        .addEventListener("click", () => {
            window.location.href = `../turnos-folder/Turnos.html?sucursalId=${sucursalId}`;
        });

    const mainImg = document.getElementById("main-sucursal-img");
    document.querySelectorAll(".galeria-thumb").forEach(btn => {
        btn.addEventListener("click", () => {
            document.querySelectorAll(".galeria-thumb").forEach(t => t.classList.remove("galeria-thumb--activo"));
            btn.classList.add("galeria-thumb--activo");
            mainImg.src = btn.dataset.url;
        });
    });

    cargarResenias(sucursalId);
    cargarEmpleadosConFotos(sucursalId);
}

async function cargarEmpleadosConFotos(sucursalId) {
    try {
        const res = await fetch(API_URL + `/sucursales/${sucursalId}/elegir-empleado`, {
            headers: authHeaders()
        });
        await checkRes(res);
        const empleados = await res.json();
        const grid = document.getElementById("empleados-grid");
        if (!grid) return;
        if (!empleados.length) {
            grid.innerHTML = "<p class='sin-datos'>Sin empleados registrados</p>";
            return;
        }
        grid.innerHTML = empleados.map(e => `
            <div class="empleado-foto-card">
                <img src="${e.urlFotoPerfil || IMG_FALLBACK}" alt="${e.nombre}"
                     onerror="this.src='${IMG_FALLBACK}'">
                <span class="empleado-foto-nombre">${e.nombre} ${e.apellido}</span>
            </div>
        `).join("");
    } catch {
        const grid = document.getElementById("empleados-grid");
        if (grid) grid.innerHTML = "<p class='sin-datos'>Sin empleados registrados</p>";
    }
}

async function cargarResenias(sucursalId) {
    try {
        const response = await fetch(API_URL + `/sucursales/${sucursalId}/resenias`, {
            headers: authHeaders()
        });
        await checkRes(response);
        const resenias = await response.json();
        const lista = document.getElementById("resenias-lista");
        if (!resenias || resenias.length === 0) {
            lista.innerHTML = "<p class='sin-datos'>Todavía no hay reseñas</p>";
            return;
        }
        lista.innerHTML = resenias.map(r => `
            <div class="resenia-card">
                <div class="resenia-header">
                    <span class="resenia-abejas">${"🐝".repeat(Math.round(r.puntuacion))}</span>
                    <span class="resenia-fecha">${formatearFechaLocal(r.fecha) || new Date(r.fecha).toLocaleDateString()}</span>
                </div>
                <p class="resenia-comentario">${r.comentario || "Sin comentario"}</p>
            </div>
        `).join("");
    } catch (error) {
        console.error("Error al cargar reseñas:", error);
    }
}

function getSucursalIdFromUrl() {
    const queryId = new URLSearchParams(window.location.search).get('id');
    if (queryId) {
        return queryId;
    }

    const parts = window.location.pathname.split('/').filter(Boolean);
    const lastPart = parts[parts.length - 1];
    return lastPart && !Number.isNaN(Number(lastPart)) ? lastPart : null;
}

async function fetchSucursal(id) {
    try {
        const response = await fetch(API_URL + `/sucursales/${id}`, {
            headers: authHeaders()
        });
        await checkRes(response);
        return await response.json();
    } catch (error) {
        alert(error);
        return null;
    }
}


