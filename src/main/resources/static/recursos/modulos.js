const ORIGENES_BACKEND_PROPIO = [8080];
const puertoActual = Number(window.location.port);
const esLocalhost = ["localhost", "127.0.0.1"].includes(window.location.hostname);

export const API_URL = "";

function getToken() {
    return localStorage.getItem("token");
}

export function authHeaders() {
    const token = getToken();
    const headers = { "Content-Type": "application/json" };
    if (token) headers.Authorization = `Bearer ${token}`;
    return headers;
}

export async function subirImagenCloudinary(file, tipo) {
    const datos = new FormData();
    datos.append("file", file);

    const headers = {};
    const token = getToken();
    if (token) headers.Authorization = `Bearer ${token}`;

    const response = await fetch(
        `${API_URL}/imagenes/upload?tipo=${encodeURIComponent(tipo)}`,
        { method: "POST", headers, body: datos }
    );

    if (!response.ok) {
        const mensaje = await response.text();
        throw new Error(mensaje || "No se pudo subir la imagen");
    }

    return await response.json();
}

export async function descartarImagenCloudinary(imagen) {
    if (!imagen?.tokenCancelacion) return;

    const response = await fetch(`${API_URL}/imagenes/upload`, {
        method: "DELETE",
        headers: authHeaders(),
        body: JSON.stringify({
            publicId: imagen.publicId,
            tokenCancelacion: imagen.tokenCancelacion
        })
    });
    if (!response.ok) {
        throw new Error("No se pudo limpiar la imagen subida; volvé a intentar antes de cerrar.");
    }
}

export function activarSubidaImagen(fileId, urlId, publicIdId, tipo, statusId) {
    const fileInput = document.getElementById(fileId);
    const urlInput = document.getElementById(urlId);
    const publicIdInput = document.getElementById(publicIdId);
    const status = document.getElementById(statusId);
    let imagenPendiente = null;

    fileInput.addEventListener("change", () => {
        if (fileInput.files?.length && status) {
            status.textContent = "Imagen seleccionada. Se subirá al guardar.";
        }
    });
    urlInput.addEventListener("input", () => {
        fileInput.value = "";
        publicIdInput.value = "";
        if (status) status.textContent = "Se usará la URL ingresada.";
    });

    const subirAlGuardar = async () => {
        const file = fileInput.files?.[0];
        if (!file) {
            if (imagenPendiente && urlInput.value !== imagenPendiente.url) {
                await subirAlGuardar.descartar();
            }
            return;
        }
        if (imagenPendiente) await subirAlGuardar.descartar();
        if (status) status.textContent = "Subiendo imagen…";
        imagenPendiente = await subirImagenCloudinary(file, tipo);
        urlInput.value = imagenPendiente.url;
        publicIdInput.value = imagenPendiente.publicId;
        fileInput.value = "";
        if (status) status.textContent = "Imagen subida. Guardando formulario…";
    };
    subirAlGuardar.confirmar = () => { imagenPendiente = null; };
    subirAlGuardar.descartar = async () => {
        if (!imagenPendiente) return;
        await descartarImagenCloudinary(imagenPendiente);
        if (urlInput.value === imagenPendiente.url) urlInput.value = "";
        publicIdInput.value = "";
        imagenPendiente = null;
        if (status) status.textContent = "La imagen subida se descartó.";
    };
    return subirAlGuardar;
}

export async function sesionActiva() {

    try {

        const response = await fetch(
            API_URL + "/usuarios/me",
            {
                headers: authHeaders()
            }
        );

        if (!response.ok) {
            return null;
        }

        return await response.json();

    } catch (error) {
        return null;
    }
}

export const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function cerrarSesion() {
    localStorage.removeItem("token");
    window.location.href = "../login-folder/Login.html";
}
//Esta mierda es una checkea el status
export async function checkRes(response) {
    if (!response.ok) {
        const text = await response.text();
        const msg = text || "Error desconocido";
        window.location.href = `../error-folder/Error.html?msg=${encodeURIComponent(msg)}`;
        throw new Error(msg);
    }
    return response;
}

export async function checkResSinRedireccion(response) {
    if (!response.ok) {
        const mensaje = await response.text();
        throw new Error(mensaje || "No se pudo guardar");
    }
    return response;
}

export async function apiFetch(url, options = {}) {
    const res = await fetch(url, options);
    return checkRes(res);
}

export function formatearFechaLocal(isoStr) {
    if (!isoStr) return "";
    const partes = isoStr.split("T");
    const [y, m, d] = partes[0].split("-");
    if (!partes[1]) {
        return new Date(+y, +m - 1, +d).toLocaleDateString("es-AR");
    }
    const [h, min, s] = partes[1].split(":");
    const date = new Date(+y, +m - 1, +d, +h, +min, +(s || 0));
    return date.toLocaleString("es-AR");
}

/// Dias de la semana en el orden lunes a domingo (como los devuelve el backend).
export const DIAS_SEMANA = [
    { valor: "MONDAY", corto: "LU", etiqueta: "Lunes" },
    { valor: "TUESDAY", corto: "MA", etiqueta: "Martes" },
    { valor: "WEDNESDAY", corto: "MI", etiqueta: "Miércoles" },
    { valor: "THURSDAY", corto: "JU", etiqueta: "Jueves" },
    { valor: "FRIDAY", corto: "VI", etiqueta: "Viernes" },
    { valor: "SATURDAY", corto: "SA", etiqueta: "Sábado" },
    { valor: "SUNDAY", corto: "DO", etiqueta: "Domingo" },
];

/// Devuelve los dias marcados en el orden lunes a domingo.
/// Si no vine nada se asumen los 7 (una sucursal sin dias cargados abre todos).
function normalizarDiasApertura(dias) {
    const marcados = new Set((Array.isArray(dias) ? dias : []).map(d => String(d).trim().toUpperCase()));
    if (!marcados.size) return DIAS_SEMANA.map(d => d.valor);
    return DIAS_SEMANA.filter(d => marcados.has(d.valor)).map(d => d.valor);
}

/// Resumen legible: "Lunes a Viernes", "Sábado y Domingo" o "Todos los días".
export function resumenDiasApertura(dias) {
    const abiertos = normalizarDiasApertura(dias);

    if (abiertos.length === DIAS_SEMANA.length) return "Todos los días";
    if (!abiertos.length) return "No abre ningún día";

    const indices = abiertos.map(valor => DIAS_SEMANA.findIndex(d => d.valor === valor));
    const tramos = [];

    indices.forEach((indice, i) => {
        const ultimo = indices[i - 1];
        if (ultimo !== undefined && indice === ultimo + 1) {
            tramos[tramos.length - 1].hasta = indice;
            return;
        }
        tramos.push({ desde: indice, hasta: indice });
    });

    return tramos.map(({ desde, hasta }) => {
        if (desde === hasta) return DIAS_SEMANA[desde].etiqueta;
        if (hasta === desde + 1) return `${DIAS_SEMANA[desde].etiqueta} y ${DIAS_SEMANA[hasta].etiqueta}`;
        return `${DIAS_SEMANA[desde].etiqueta} a ${DIAS_SEMANA[hasta].etiqueta}`;
    }).join(", ");
}

/// HTML con un check por dia. Los no marcados son los dias que la sucursal no abre.
export function checksDiasAperturaHTML(prefijo, dias) {
    const marcados = new Set(normalizarDiasApertura(dias));

    return DIAS_SEMANA.map(dia => `
        <label class="dia-check" for="${prefijo}-dia-${dia.valor}">
            <input type="checkbox" class="dia-check__input" id="${prefijo}-dia-${dia.valor}"
                   name="${prefijo}-dias" value="${dia.valor}"${marcados.has(dia.valor) ? " checked" : ""}>
            <span class="dia-check__corto">${dia.corto}</span>
            <span class="dia-check__nombre">${dia.etiqueta}</span>
        </label>`).join("");
}

/// Devuelve los valores de los dias marcados.
export function leerDiasApertura(prefijo) {
    return Array.from(document.querySelectorAll(`input[name="${prefijo}-dias"]:checked`))
        .map(input => input.value);
}

/// Marca o desmarca todos los checks de dias (botones "Todos" / "Ninguno").
export function marcarTodosLosDias(prefijo, marcar) {
    document.querySelectorAll(`input[name="${prefijo}-dias"]`)
        .forEach(input => { input.checked = marcar; });
}

/// HTML con los 7 dias, marcando los que abre la sucursal.
export function listaDiasAperturaHTML(dias) {
    const abiertos = new Set(normalizarDiasApertura(dias));

    return DIAS_SEMANA.map(dia => `
        <li class="dia-chip${abiertos.has(dia.valor) ? "" : " dia-chip--cerrado"}" title="${dia.etiqueta}">
            <span class="dia-chip__corto">${dia.corto}</span>
            <span class="dia-chip__nombre">${dia.etiqueta}</span>
        </li>`).join("");
}
