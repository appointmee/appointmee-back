import { API_URL, authHeaders, sesionActiva, checkRes } from "../recursos/modulos.js";

let buscadorConectado = false;

const u = await sesionActiva();

if (!u) {
    window.location.href = "../login-folder/Login.html";
}

const parametrosUrl = new URLSearchParams(window.location.search);
const busquedaInicial = parametrosUrl.get("busqueda") || "";

categoriasDisponibles();

if (busquedaInicial) {
    filtrarPorNombre(busquedaInicial);
} else {
    renderSucursales();
}

document.addEventListener("navbar:ready", (e) => {
    iniciarBuscador(busquedaInicial, e.detail.input);
});

const inputYaExistente = document.getElementById("search-input");
if (inputYaExistente) {
    iniciarBuscador(busquedaInicial, inputYaExistente);
}



async function renderSucursales() {
    const sucursales = await buscarSucursales();
    renderListaSucursales(sucursales);
}

function renderListaSucursales(sucursales) {
    const container = document.getElementById("turn-container");
    if (!container) return;

    container.innerHTML = "";

    sucursales.forEach(sucursal => {
        container.innerHTML += `
            <a href="${window.location.origin}/sucursal-folder/Sucursal.html?id=${sucursal.id}">
                <article class="turn-article">
                    <img src="${sucursal.fotoPerfil || 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSQPQenKJTzexez3E1uN7qtSwZ8tgPQsVJ9DQ&s'}">
                    <div class="turn-content">
                        <h2>${sucursal.nombre}</h2>
                        <p>${sucursal.categoria}</p>
                        <span class="detalle-value">
    ${sucursal.puntuacion
                ? "🐝".repeat(Math.round(sucursal.puntuacion))
                : "—"}
</span>
                    </div>
                </article>
            </a>
        `;
    });
}

async function buscarSucursales() {
    try {
        const response = await fetch(API_URL + `/sucursales/listar`, {
            headers: authHeaders()
        });
        console.log(authHeaders());
        await checkRes(response);
        return await response.json();
    } catch (error) {
        alert(error.message);
        return [];
    }
}

async function filtrarPorCategoria(categoria) {
    try {
        const response = await fetch(API_URL +
            `/sucursales/listar/filtrar?categoria=${categoria}`,
            { headers: authHeaders() }
        );
        await checkRes(response);
        const sucursales = await response.json();
        renderListaSucursales(sucursales);
    } catch (error) {
        alert(error.message);
    }
}

async function filtrarPorNombre(texto) {
    try {
        const parametros = new URLSearchParams();
        if (texto) parametros.append("nombre", texto);

        const response = await fetch(API_URL +
            `/sucursales/listar/filtrar?${parametros.toString()}`,
            { headers: authHeaders() }
        );
        await checkRes(response);
        const sucursales = await response.json();
        renderListaSucursales(sucursales);
    } catch (error) {
        alert(error.message);
    }
}

function iniciarBuscador(valorInicial, input) {
    if (buscadorConectado) return;

    if (!input) {
        input = document.getElementById("search-input");
    }
    if (!input) return;

    buscadorConectado = true;

    if (valorInicial) {
        input.value = valorInicial;
    }

    input.addEventListener("keydown", (e) => {
        if (e.key !== "Enter") return;
        e.preventDefault();

        const texto = input.value.trim();

        const url = new URL(window.location.href);
        if (texto) {
            url.searchParams.set("busqueda", texto);
        } else {
            url.searchParams.delete("busqueda");
        }
        window.history.replaceState({}, "", url);

        if (texto) {
            filtrarPorNombre(texto);
        } else {
            renderSucursales();
        }
    });
}


async function buscarCategorias() {
    try {
        const response = await fetch(API_URL + `/sucursales/categorias`, {
            headers: authHeaders()
        });
        await checkRes(response);
        return await response.json();
    } catch (error) {
        alert(error.message);
        return [];
    }
}

function renderCategoriasParaFiltrar(categorias) {
    const containerLeft = document.getElementById("category-container");
    if (!containerLeft) return;

    containerLeft.innerHTML = `
        <a href="#" class="categoria-filtro" data-categoria="">Todas</a>
        ${categorias.map(c => `
            <a href="#" class="categoria-filtro" data-categoria="${c}">${c}</a>
        `).join('')}
    `;

    containerLeft.querySelectorAll(".categoria-filtro").forEach(link => {
        link.addEventListener("click", (e) => {
            e.preventDefault();

            containerLeft.querySelectorAll(".categoria-filtro")
                .forEach(l => l.classList.remove("active"));
            e.target.classList.add("active");

            const cat = e.target.dataset.categoria;
            if (cat === "") {
                renderSucursales();
            } else {
                filtrarPorCategoria(cat);
            }
        });
    });
}

async function categoriasDisponibles() {
    const categorias = await buscarCategorias();
    renderCategoriasParaFiltrar(categorias);
}