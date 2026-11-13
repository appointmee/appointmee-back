import {
    API_URL,
    sesionActiva,
    authHeaders,
    checkRes,
    formatearFechaLocal
} from "../recursos/modulos.js";


const user = await sesionActiva();

if (!user) {
    window.location.href = "../login-folder/Login.html";
}

let turnos = [];
let misSucursales = [];

document
    .getElementById("buscar-turno")
    .addEventListener("input", renderTurnos);

await init();

async function init() {

    await cargarSucursales();

    document.getElementById("filtro-sucursal-turnos").addEventListener("change", (e) => {
        const id = e.target.value;
        if (id) {
            cargarTurnos(parseInt(id));
        } else {
            turnos = [];
            renderTurnos();
        }
    });

}

async function cargarSucursales() {
    try {
        const res = await fetch(API_URL + `/usuarios/${user.id}`, { headers: authHeaders() });
        await checkRes(res);
        const perfil = await res.json();
        misSucursales = perfil.sucursalesEmpleado || [];
        const sel = document.getElementById("filtro-sucursal-turnos");
        sel.innerHTML = '<option value="">Seleccioná una sucursal</option>';
        misSucursales.forEach(s => {
            const opt = document.createElement("option");
            opt.value = s.id;
            opt.textContent = s.nombre;
            sel.appendChild(opt);
        });
        if (misSucursales.length === 1) {
            sel.value = misSucursales[0].id;
            const event = new Event("change");
            sel.dispatchEvent(event);
        }
    } catch (e) {
        console.error("Error al cargar sucursales:", e);
    }
}

async function cargarTurnos(sucursalId) {

    try {
        const headers = authHeaders();
        const [pendientesResponse, confirmadosResponse] = await Promise.all([
            fetch(API_URL + `/turnos/de-sucursal/${sucursalId}/propios?estadoTurno=PENDIENTE`, { headers }),
            fetch(API_URL + `/turnos/de-sucursal/${sucursalId}/propios?estadoTurno=CONFIRMADO`, { headers })
        ]);
        await Promise.all([checkRes(pendientesResponse), checkRes(confirmadosResponse)]);
        const [pendientes, confirmados] = await Promise.all([
            pendientesResponse.json(),
            confirmadosResponse.json()
        ]);
        turnos = [...confirmados, ...pendientes].sort(
            (a, b) => new Date(a.fechaTurno) - new Date(b.fechaTurno)
        );
    } catch (e) {
        turnos = [];
    }
    renderTurnos();

}

function renderTurnos() {

    const container =
        document.getElementById("turnos-container");

    const texto =
        document
            .getElementById("buscar-turno")
            .value
            .toLowerCase();

    container.innerHTML = "";

    const filtrados = turnos.filter(t =>
        JSON.stringify(t)
            .toLowerCase()
            .includes(texto)
    );

    if (filtrados.length === 0) {

        container.innerHTML =
            "<p style='color:var(--t3);font-family:var(--mono);font-size:12px;'>No hay turnos para mostrar.</p>";

        return;
    }

    filtrados.forEach(turno => {

        const fecha = formatearFechaLocal(turno.fechaTurno);
        const estadoBadge = {
            PENDIENTE: 'badge--amber',
            CONFIRMADO: 'badge--green',
            REALIZADO: 'badge--green',
            CANCELADO: 'badge--red',
            PAGO_RECHAZADO: 'badge--red'
        }[turno.estadoTurno] || 'badge--blue';

        const card = document.createElement("div");

        card.className = "card";

        card.innerHTML = `
            <div style="display:flex;align-items:center;gap:12px;">
                <div style="flex:1;min-width:0;">
                    <h3 style="font-size:13px;font-weight:500;color:var(--t);margin:0 0 4px;">${turno.nombreServicio}</h3>
                    <p style="font-size:11px;color:var(--t3);font-family:var(--mono);margin:0;">${fecha}</p>
                </div>
                <div style="display:flex;align-items:center;gap:8px;">
                    <span class="badge ${estadoBadge}">${turno.estadoTurno}</span>
                    <button class="btn-realizar" data-id="${turno.id}">Realizar</button>
                </div>
            </div>
        `;

        card.querySelector(".btn-realizar").addEventListener("click", async (e) => {
            e.stopPropagation();
            const btn = e.currentTarget;
            btn.disabled = true;
            btn.textContent = "⋯";
            try {
                const res = await fetch(API_URL + `/turnos/finalizar/${turno.id}`, {
                    method: "PATCH",
                    headers: authHeaders()
                });
                await checkRes(res);
                turnos = turnos.filter(t => t.id !== turno.id);
                renderTurnos();
            } catch (err) {
                alert("Error al finalizar el turno: " + err.message);
                btn.disabled = false;
                btn.textContent = "Realizar";
            }
        });

        container.appendChild(card);

    });

}
