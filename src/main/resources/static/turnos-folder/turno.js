import { API_URL, authHeaders, sesionActiva, checkRes } from "../recursos/modulos.js";

const user = await sesionActiva();
if (!user) {
    window.location.href = "../login-folder/Login.html";
}


const IMG_FALLBACK = "https://xentra.glomastore.mx/img/sin_imagen.png";
const sucursalId = new URLSearchParams(window.location.search).get("sucursalId");

let empleadoSeleccionado = null;
let servicioSeleccionado = null;
let fechaHoraSeleccionada = null;

const MESES = ["Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"];
const hoy = new Date();
let vistaYear = hoy.getFullYear();
let vistaMes = hoy.getMonth();
let diaSeleccionado = null;
let horariosDelDia = [];
let turnoSeleccionado = null;

if (!sucursalId) {
    document.getElementById("agendarTurno-container").innerHTML =
        `<p class="turno-sin-dia">No se encontró la sucursal. Volvé atrás e intentá de nuevo.</p>`;
} else {
    renderPaso1();
}

// ---------- Utilidades ----------

// Escapa texto para insertarlo de forma segura dentro de innerHTML (previene XSS).
function escaparHTML(str) {
    if (str === null || str === undefined) return "";
    return String(str)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;");
}

// Escapa texto para insertarlo de forma segura dentro de un atributo onclick="..."
// (además de escapar HTML, neutraliza backslashes para que no se pueda
// "romper" la comilla simple que envuelve el argumento).
function escaparAtributo(str) {
    if (str === null || str === undefined) return "";
    return String(str)
        .replace(/\\/g, "\\\\")
        .replace(/'/g, "\\'")
        .replace(/"/g, "&quot;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;");
}

function renderPaso2() {
    const c = document.getElementById("agendarTurno-container");
    c.innerHTML = `
        <div class="agendarTurno-info">
            <div class="paso-indicador">
                <span class="paso-punto paso-punto--completado"></span>
                <span class="paso-linea paso-linea--completado"></span>
                <span class="paso-punto paso-punto--activo"></span>
                <span class="paso-linea"></span>
                <span class="paso-punto"></span>
            </div>
            <p class="paso-label">Paso 2 de 3 — Profesional</p>
            <h2 class="turno-paso-titulo">Elegí un profesional</h2>
            <div class="empleados-container" id="empleados-container">
                <p class="turno-sin-dia">Cargando profesionales…</p>
            </div>
            <div class="info-seleccionada" id="info-seleccionada"></div>
            <div class="btn-group">
                <button class="btn-proceso btn-proceso--secundario" id="volver" data-no-cooldown>Volver</button>
                <button class="btn-proceso" id="siguiente" disabled>Siguiente</button>
            </div>
        </div>`;

    cargarEmpleados();

    document.getElementById("volver").addEventListener("click", () => {
        renderPaso1();
    });

    document.getElementById("siguiente").addEventListener("click", () => {
        renderPaso3();
    });

}

const EMPLEADOS_POR_PAGINA = 4;
let empleadosCache = [];
let paginaEmpleados = 0;

async function cargarEmpleados() {
    try {
        const res = await fetch(API_URL + `/sucursales/${sucursalId}/elegir-empleado`, {
            headers: authHeaders()
        });
        await checkRes(res);
        empleadosCache = await res.json();

        const contenedor = document.getElementById("empleados-container");
        if (!contenedor) return;

        if (!empleadosCache.length) {
            contenedor.innerHTML = `<p class="turno-sin-dia">Esta sucursal no tiene profesionales disponibles.</p>`;
            return;
        }

        paginaEmpleados = 0;
        renderPaginaEmpleados();

    } catch (err) {
        const contenedor = document.getElementById("empleados-container");
        if (contenedor) contenedor.innerHTML = `<p class="turno-sin-dia">No se pudieron cargar los profesionales.</p>`;
    }
}

function renderPaginaEmpleados() {
    const contenedor = document.getElementById("empleados-container");
    if (!contenedor) return;

    const totalPaginas = Math.ceil(empleadosCache.length / EMPLEADOS_POR_PAGINA);
    const inicio = paginaEmpleados * EMPLEADOS_POR_PAGINA;
    const fin = inicio + EMPLEADOS_POR_PAGINA;
    const pagina = empleadosCache.slice(inicio, fin);

    contenedor.innerHTML =
        `<div class="empleados-pagina">` +
            pagina.map(e => {
                const nombreCompleto = `${e.nombre} ${e.apellido}`;
                const seleccionado = empleadoSeleccionado?.id === e.id;
                return `
                <article class="empleado-article${seleccionado ? " empleado-article--seleccionado" : ""}" data-id="${e.id}" onclick="seleccionarEmpleado(${e.id}, this)">
                    <img src="${escaparAtributo(e.urlFotoPerfil || IMG_FALLBACK)}"
                         alt="Foto de ${escaparAtributo(nombreCompleto)}"
                         onerror="this.src='${IMG_FALLBACK}'">
                    <p class="empleado-nombre">${escaparHTML(nombreCompleto)}</p>
                    <p class="empleado-valoracion">
                        ${e.puntuacion ? "🐝".repeat(Math.round(e.puntuacion)) : "Sin calificar"} 
                    </p>
                </article>`;
            }).join("") +
        `</div>` +
        (totalPaginas > 1 ? `
            <div class="empleados-paginacion">
                <button class="btn-proceso btn-proceso--secundario empleados-pag-btn" id="btn-empleados-prev" data-no-cooldown ${paginaEmpleados === 0 ? "disabled" : ""}>‹ Anterior</button>
                <span class="empleados-pag-info">${paginaEmpleados + 1} / ${totalPaginas}</span>
                <button class="btn-proceso btn-proceso--secundario empleados-pag-btn" id="btn-empleados-next" data-no-cooldown ${paginaEmpleados >= totalPaginas - 1 ? "disabled" : ""}>Siguiente ›</button>
            </div>` : "");

    document.getElementById("btn-empleados-prev")?.addEventListener("click", () => {
        if (paginaEmpleados > 0) {
            paginaEmpleados--;
            renderPaginaEmpleados();
        }
    });
    document.getElementById("btn-empleados-next")?.addEventListener("click", () => {
        if (paginaEmpleados < totalPaginas - 1) {
            paginaEmpleados++;
            renderPaginaEmpleados();
        }
    });
}


window.seleccionarEmpleado = function (id, el) {
    document.querySelectorAll(".empleado-article").forEach(a =>
        a.classList.remove("empleado-article--seleccionado"));
    el.classList.add("empleado-article--seleccionado");

    // Buscamos los datos en la cache en vez de leerlos del DOM,
    // así evitamos depender de cómo quedó renderizado el texto.
    const datos = empleadosCache.find(e => e.id === id);

    empleadoSeleccionado = {
        id,
        nombre: datos ? `${datos.nombre} ${datos.apellido}` : (el.querySelector(".empleado-nombre")?.textContent ?? ""),
        imagen: datos?.urlFotoPerfil || el.querySelector("img")?.src || IMG_FALLBACK
    };

    const info = document.getElementById("info-seleccionada");
    if (info) info.innerHTML = `
        <img src="${escaparAtributo(empleadoSeleccionado.imagen)}"
             alt="${escaparAtributo(empleadoSeleccionado.nombre)}"
             onerror="this.src='${IMG_FALLBACK}'">
        <h3>${escaparHTML(empleadoSeleccionado.nombre)}</h3>`;

    document.getElementById("siguiente").disabled = false;
};

function renderPaso1() {
    const c = document.getElementById("agendarTurno-container");
    c.innerHTML = `
        <div class="agendarTurno-info">
            <div class="paso-indicador">
                <span class="paso-punto paso-punto--activo"></span>
                <span class="paso-linea"></span>
                <span class="paso-punto"></span>
                <span class="paso-linea"></span>
                <span class="paso-punto"></span>
            </div>
            <p class="paso-label">Paso 1 de 3 — Servicio</p>
            <h2 class="turno-paso-titulo">Elegí un servicio</h2>
            <div class="servicios-container" id="servicios-container">
                <p class="turno-sin-dia">Cargando servicios…</p>
            </div>
            <div class="btn-group">
                <button class="btn-proceso btn-proceso--secundario" id="volver" data-no-cooldown>Volver</button>
                <button class="btn-proceso" id="siguiente" disabled>Siguiente</button>
            </div>
        </div>`;

    cargarServicios();
    document.getElementById("siguiente").addEventListener("click", renderPaso2);
    document.getElementById("volver").addEventListener("click", () => {
        history.go(-1);
    });
}

let serviciosCache = [];

async function cargarServicios() {
    try {
        const res = await fetch(API_URL + `/servicios/listar/sucursal/${sucursalId}`, {
            headers: authHeaders()
        });
        await checkRes(res);
        serviciosCache = await res.json();

        const contenedor = document.getElementById("servicios-container");
        if (!contenedor) return;

        if (!serviciosCache.length) {
            contenedor.innerHTML = `<p class="turno-sin-dia">Esta sucursal no tiene servicios disponibles.</p>`;
            return;
        }

        contenedor.innerHTML = serviciosCache.map(s => `
            <article class="empleado-article" data-id="${s.id}" onclick="seleccionarServicio(${s.id}, this)">
                <img src="${escaparAtributo(s.fotoPerfil || IMG_FALLBACK)}"
                     alt="${escaparAtributo(s.nombre)}"
                     onerror="this.src='${IMG_FALLBACK}'">
                <p class="empleado-nombre">${escaparHTML(s.nombre)}</p>
                <p class="empleado-valoracion">
                    ⏱ ${s.duracion} min 
                </p>
            </article>`).join("");

    } catch (err) {
        const contenedor = document.getElementById("servicios-container");
        if (contenedor) contenedor.innerHTML = `<p class="turno-sin-dia">No se pudieron cargar los servicios.</p>`;
    }
}

window.seleccionarServicio = function (id, el) {
    document.querySelectorAll(".servicios-container .empleado-article").forEach(a =>
        a.classList.remove("empleado-article--seleccionado"));
    el.classList.add("empleado-article--seleccionado");

    // Tomamos el nombre desde la cache (datos reales del backend),
    // no reconstruido a partir de un string ya escapado para onclick.
    const datos = serviciosCache.find(s => s.id === id);
    servicioSeleccionado = { id, nombre: datos?.nombre ?? el.querySelector(".empleado-nombre")?.textContent ?? "" };
    document.getElementById("siguiente").disabled = false;
};

let cacheDisponibilidad = null;
// Token de carrera: identifica la combinación empleado+servicio vigente
// para descartar respuestas de fetch obsoletas si el usuario cambia de
// selección antes de que la request anterior termine.
let disponibilidadToken = 0;

function renderPaso3() {
    diaSeleccionado = null;
    turnoSeleccionado = null;
    fechaHoraSeleccionada = null;
    cacheDisponibilidad = null;
    vistaYear = hoy.getFullYear();
    vistaMes = hoy.getMonth();

    const c = document.getElementById("agendarTurno-container");
    c.innerHTML = `
        <div class="agendarTurno-info">
            <div class="paso-indicador">
                <span class="paso-punto paso-punto--completado"></span>
                <span class="paso-linea paso-linea--completado"></span>
                <span class="paso-punto paso-punto--completado"></span>
                <span class="paso-linea paso-linea--completado"></span>
                <span class="paso-punto paso-punto--activo"></span>
            </div>
            <p class="paso-label">Paso 3 de 3 — Fecha y hora</p>
            <div class="turno-container">
                <div class="turno-header">
                    <h1>¿Cuándo querés venir?</h1>
                    <p class="turno-paso-sub">
                        ${escaparHTML(empleadoSeleccionado.nombre)} &nbsp;·&nbsp; ${escaparHTML(servicioSeleccionado.nombre)}
                    </p>
                </div>

                <div class="cal-nav">
                    <button class="cal-nav-btn" id="btn-prev" data-no-cooldown>&#8249;</button>
                    <span class="cal-mes-nombre" id="cal-mes-nombre"></span>
                    <button class="cal-nav-btn" id="btn-next" data-no-cooldown>&#8250;</button>
                </div>

                <div class="cal-semana">
                    <div class="cal-semana-label">DO</div>
                    <div class="cal-semana-label">LU</div>
                    <div class="cal-semana-label">MA</div>
                    <div class="cal-semana-label">MI</div>
                    <div class="cal-semana-label">JU</div>
                    <div class="cal-semana-label">VI</div>
                    <div class="cal-semana-label">SA</div>
                </div>

                <div class="cal-grid" id="cal-grid"></div>

                <p class="turno-horarios-titulo" id="horarios-titulo">Horarios disponibles</p>
                <div class="turno-horarios-grid" id="horarios-grid">
                    <span class="turno-sin-dia">Seleccioná un día para ver los horarios.</span>
                </div>

                <div class="turno-resumen" id="turno-resumen" style="display:none">
                    <span class="turno-resumen-texto" id="turno-resumen-texto"></span>
                </div>

                <div class="turno-footer">
                    <button class="btn-proceso btn-proceso--secundario" id="volver" data-no-cooldown>Volver</button>
                    <button class="turno-btn-siguiente" id="siguiente" disabled>
                        Confirmar turno <span>→</span>
                    </button>
                </div>
            </div>
        </div>`;

    document.getElementById("btn-prev").addEventListener("click", () => {
        if (--vistaMes < 0) { vistaMes = 11; vistaYear--; }
        renderCalendario();
    });
    document.getElementById("btn-next").addEventListener("click", () => {
        if (++vistaMes > 11) { vistaMes = 0; vistaYear++; }
        renderCalendario();
    });
    document.getElementById("volver").addEventListener("click", renderPaso2);
    document.getElementById("siguiente").addEventListener("click", confirmarTurno);

    cargarDisponibilidad();
}

async function cargarDisponibilidad() {
    // Cada llamada genera su propio token; si el usuario navega fuera de
    // este paso antes de que la respuesta llegue, el token ya no coincide
    // y el resultado se descarta en vez de pisar datos nuevos.
    const tokenActual = ++disponibilidadToken;

    try {
        const res = await fetch(
            API_URL + `/turnos/disponibilidad/empleado/${empleadoSeleccionado.id}/servicio/${servicioSeleccionado.id}`,
            { headers: authHeaders() }
        );
        await checkRes(res);
        const datos = await res.json();

        if (tokenActual !== disponibilidadToken) return; // respuesta obsoleta, se ignora
        cacheDisponibilidad = datos;
    } catch (err) {
        if (tokenActual !== disponibilidadToken) return;
        cacheDisponibilidad = [];
        alert("No se pudo cargar la disponibilidad: " + err.message);
    }

    if (tokenActual === disponibilidadToken) {
        renderCalendario();
    }
}

function horariosParaFecha(y, m, d) {
    if (!cacheDisponibilidad) return null;
    const key = `${y}-${String(m + 1).padStart(2, "0")}-${String(d).padStart(2, "0")}`;
    const entrada = cacheDisponibilidad.find(e => e.fecha === key);
    return entrada ? entrada.horarios : [];
}

function renderCalendario() {
    const grid = document.getElementById("cal-grid");
    if (!grid) return;

    document.getElementById("cal-mes-nombre").textContent =
        `${MESES[vistaMes]} ${vistaYear}`;
    grid.innerHTML = "";

    const primerDia = new Date(vistaYear, vistaMes, 1).getDay();
    const totalDias = new Date(vistaYear, vistaMes + 1, 0).getDate();
    const hoyNorm = new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate());

    for (let i = 0; i < primerDia; i++) {
        const v = document.createElement("div");
        v.className = "cal-dia cal-dia--vacio";
        grid.appendChild(v);
    }

    for (let d = 1; d <= totalDias; d++) {
        const btn = document.createElement("button");
        btn.className = "cal-dia";
        btn.textContent = d;

        const fecha = new Date(vistaYear, vistaMes, d);
        const esPasado = fecha < hoyNorm;
        const esHoy = d === hoy.getDate() && vistaMes === hoy.getMonth() && vistaYear === hoy.getFullYear();
        const esSel = diaSeleccionado?.d === d && diaSeleccionado?.m === vistaMes && diaSeleccionado?.y === vistaYear;
        const horarios = horariosParaFecha(vistaYear, vistaMes, d);
        // Si todavía no cargó la disponibilidad (horarios === null), no marcamos
        // el día como "sin turnos": esperamos a tener datos reales.
        const sinTurnos = horarios !== null && horarios.length === 0;

        if (esPasado || sinTurnos) { btn.classList.add("cal-dia--pasado"); btn.disabled = true; }
        if (esHoy) btn.classList.add("cal-dia--hoy");
        if (esSel) btn.classList.add("cal-dia--seleccionado");
        if (!esPasado && !sinTurnos && cacheDisponibilidad !== null) {
            btn.addEventListener("click", () => seleccionarDia(d, vistaMes, vistaYear));
        } else if (!esPasado && cacheDisponibilidad === null) {
            // Disponibilidad aún cargando: deshabilitamos temporalmente para
            // evitar que el usuario seleccione un día sin datos.
            btn.disabled = true;
        }
        grid.appendChild(btn);
    }
}

function seleccionarDia(d, m, y) {
    diaSeleccionado = { d, m, y };
    turnoSeleccionado = null;
    fechaHoraSeleccionada = null;

    const resumen = document.getElementById("turno-resumen");
    const btnSig = document.getElementById("siguiente");
    if (resumen) resumen.style.display = "none";
    if (btnSig) btnSig.disabled = true;

    renderCalendario();
    renderHorarios();
}

function renderHorarios() {
    const grid = document.getElementById("horarios-grid");
    const titulo = document.getElementById("horarios-titulo");
    if (!grid || !titulo) return;

    grid.innerHTML = "";

    if (!diaSeleccionado) {
        grid.innerHTML = `<span class="turno-sin-dia">Seleccioná un día para ver los horarios.</span>`;
        titulo.innerHTML = "Horarios disponibles";
        return;
    }

    const { d, m, y } = diaSeleccionado;
    const horarios = horariosParaFecha(y, m, d);
    titulo.innerHTML = `Horarios disponibles <span>— ${d} de ${escaparHTML(MESES[m])}</span>`;

    if (!horarios || !horarios.length) {
        grid.innerHTML = `<span class="turno-sin-dia">Sin turnos disponibles para este día.</span>`;
        return;
    }

    horarios.forEach(t => {
        const label = t.slice(0, 5);
        const btn = document.createElement("button");
        btn.className = "turno-hora-btn" + (turnoSeleccionado === t ? " turno-hora-btn--seleccionado" : "");
        btn.textContent = label;
        btn.addEventListener("click", () => {
            turnoSeleccionado = t;
            const { d, m, y } = diaSeleccionado;

            const fecha = `${y}-${String(m + 1).padStart(2, "0")}-${String(d).padStart(2, "0")}`;
            fechaHoraSeleccionada = `${fecha}T${label}`;

            document.getElementById("turno-resumen-texto").innerHTML =
                `Turno: <strong>${d} de ${escaparHTML(MESES[m])} de ${y}</strong> a las <strong>${escaparHTML(label)} hs</strong>`;
            document.getElementById("turno-resumen").style.display = "flex";
            document.getElementById("siguiente").disabled = false;
            renderHorarios();
        });
        grid.appendChild(btn);
    });
}

async function confirmarTurno() {
    if (!empleadoSeleccionado || !servicioSeleccionado || !fechaHoraSeleccionada) return;

    const btnSiguiente = document.getElementById("siguiente");
    if (btnSiguiente) btnSiguiente.disabled = true;

    try {
        const resCrear = await fetch(API_URL + `/turnos/registrar`, {
            method: "POST",
            headers: authHeaders(),
            body: JSON.stringify({
                fechaHora: fechaHoraSeleccionada,
                empleadoId: empleadoSeleccionado.id,
                servicioId: servicioSeleccionado.id
            })
        });
        await checkRes(resCrear);

        // Si el endpoint de creación devuelve el turno creado (o su id),
        // usalo directamente acá en vez de "adivinarlo" después.
        // Ejemplo: const turnoCreado = await resCrear.json(); const turnoId = turnoCreado.id;
        const turnoId = await buscarTurnoRecienCreado();

        if (!turnoId) {
            alert("El turno se reservó, pero no se pudo identificar para continuar con el pago. Revisalo en Mis Turnos.");
            window.location.href = "../misturnos-folder/MisTurnos.html";
            return;
        }

        window.location.href = `../pago-folder/Pago.html?turnoId=${turnoId}`;

    } catch (e) {
        alert("Error al reservar el turno: " + e.message);
        if (btnSiguiente) btnSiguiente.disabled = false;
    }
}

// Compara fecha+hora de forma tolerante a formatos distintos entre
// fechaHoraSeleccionada ("YYYY-MM-DDTHH:mm") y lo que devuelva el backend
// (que puede incluir segundos, milisegundos u offset de zona horaria).
// En vez de comparar timestamps absolutos (sensibles a UTC vs hora local),
// comparamos año/mes/día/hora/minuto en hora LOCAL de ambos valores.
function mismaFechaHora(fechaA, fechaB) {
    const a = new Date(fechaA);
    const b = new Date(fechaB);
    if (isNaN(a) || isNaN(b)) return false;
    return a.getFullYear() === b.getFullYear() &&
        a.getMonth() === b.getMonth() &&
        a.getDate() === b.getDate() &&
        a.getHours() === b.getHours() &&
        a.getMinutes() === b.getMinutes();
}

async function buscarTurnoRecienCreado() {
    try {
        const res = await fetch(API_URL + `/turnos/propios`, { headers: authHeaders() });
        await checkRes(res);

        const turnos = await res.json();

        const candidatos = turnos.filter(t =>
            t.nombreServicio === servicioSeleccionado.nombre &&
            t.estadoTurno === "PENDIENTE" &&
            mismaFechaHora(t.fechaTurno, fechaHoraSeleccionada)
        );

        if (!candidatos.length) return null;

        candidatos.sort((a, b) => b.id - a.id);
        return candidatos[0].id;

    } catch (e) {
        return null;
    }
}
