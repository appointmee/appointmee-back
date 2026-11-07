import { API_URL, authHeaders, sesionActiva, checkRes } from "../recursos/modulos.js";

const user = await sesionActiva();
if (!user) {
    window.location.href = "../login-folder/Login.html";
}

const container = document.getElementById("sucursales-container");
const roles = user.roles || [];

let sucursales = [];

if (roles.includes("EMPLEADOR")) {
    const propias = await fetchPropias();
    sucursales = propias.map(s => ({ ...s, tipo: "Propia" }));
}

if (roles.includes("EMPLEADO")) {
    const asociadas = await fetchAsociadas();
    const ids = new Set(sucursales.map(s => s.id));
    for (const s of asociadas) {
        if (!ids.has(s.id)) {
            sucursales.push({ ...s, tipo: "Asociada" });
            ids.add(s.id);
        }
    }
}

if (sucursales.length === 0 && !roles.includes("EMPLEADOR") && !roles.includes("EMPLEADO")) {
    container.innerHTML = `<p style="color:var(--t3);font-family:var(--mono);text-align:center;padding:32px 0;">No tenés sucursales asociadas.</p>`;
} else if (sucursales.length === 0) {
    container.innerHTML = `<p style="color:var(--t3);font-family:var(--mono);text-align:center;padding:32px 0;">No se encontraron sucursales.</p>`;
} else {
    renderSucursales(sucursales);
}

function renderSucursales(lista) {
    container.innerHTML = lista.map(s => `
        <div style="background:#1e1e2f;border-radius:10px;padding:20px;margin-bottom:16px;display:flex;align-items:center;gap:16px;">
            <img src="${s.fotoPerfil || 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSQPQenKJTzexez3E1uN7qtSwZ8tgPQsVJ9DQ&s'}"
                 alt="${s.nombre}"
                 style="width:80px;height:80px;border-radius:8px;object-fit:cover;">
            <div style="flex:1;">
                <h3 style="margin:0 0 4px;">${s.nombre}</h3>
                <p style="margin:0 0 2px;color:var(--t3);font-size:14px;">${s.categoria || ""} ${s.tipo ? "· " + s.tipo : ""}</p>
                <p style="margin:0;color:var(--t3);font-size:14px;">${s.puntuacion ? "🐝".repeat(Math.round(s.puntuacion)) : "Sin calificar"}</p>
            </div>
            <a href="../sucursal-folder/Sucursal.html?id=${s.id}" style="background:#444;border:none;color:#fff;padding:8px 18px;border-radius:6px;text-decoration:none;font-size:14px;">Ver</a>
        </div>
    `).join("");
}

async function fetchPropias() {
    try {
        const res = await fetch(API_URL + "/sucursales/listar/propias", { headers: authHeaders() });
        await checkRes(res);
        return await res.json();
    } catch {
        return [];
    }
}

async function fetchAsociadas() {
    try {
        const res = await fetch(API_URL + `/usuarios/${user.id}`, { headers: authHeaders() });
        await checkRes(res);
        const perfil = await res.json();
        return perfil.sucursalesEmpleado || [];
    } catch {
        return [];
    }
}
