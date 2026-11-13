



function initNavbar() {
  const navbar = document.getElementById("navbar");
  if (!navbar) return;

  navbar.innerHTML = `
    <nav class="am-nav">
      <div class="am-nav__container">

        <a class="am-nav__brand" href="../index-folder/Index.html">
          <img
            src="../recursos/content.png"
            alt="AppointMee logo"
            class="am-nav__logo"
          />
          <span class="am-nav__title">AppointMee</span>
        </a>

        <div class="am-nav__search">
          <svg class="am-nav__search-icon" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="6.5" cy="6.5" r="5.5"/>
            <line x1="11.5" y1="11.5" x2="15" y2="15"/>
          </svg>
          <input
            class="am-nav__search-input"
            type="search"
            placeholder="Buscá tu próximo turno..."
            autocomplete="off"
            id="search-input"
          />
        </div>

        <ul class="am-nav__links" id="amNavLinks">
        
          <li><a class="am-nav__link" href="../index-folder/Index.html" data-page="home">Home</a></li>
          <li><a class="am-nav__link" href="../misturnos-folder/MisTurnos.html" data-page="turnos">Mis Turnos</a></li>
          <li id="nav-mis-sucursales" style="display:none;"><a class="am-nav__link" href="../mis-sucursales-folder/MisSucursales.html" data-page="sucursales">Mis Sucursales</a></li>
        </ul>

        <div class="am-nav__profile" id="navProfile">
          <button class="am-nav__profile-btn" id="navProfileBtn" data-no-cooldown>
            <span class="am-nav__profile-avatar" id="navProfileAvatar">?</span>
          </button>
          <div class="am-nav__dropdown" id="navDropdown">
            <a class="am-nav__dropdown-link" href="../perfil-folder/Perfil.html">Mi Perfil</a>
            <a class="am-nav__dropdown-link" href="../solicitudes-folder/Solicitudes.html" id="nav-solicitudes-link" style="display:none;">Solicitudes</a>
            <a class="am-nav__dropdown-link" href="../configuracion-folder/configuracion.html">Ajustes</a>
            <button class="am-nav__dropdown-link am-nav__dropdown-link--danger" id="navCerrarSesion">Cerrar Sesión</button>
          </div>
        </div>

        <button class="am-nav__toggle" id="amNavToggle" data-no-cooldown aria-label="Abrir menú" aria-expanded="false" aria-controls="amNavLinks">
          <span class="am-nav__toggle-bar"></span>
          <span class="am-nav__toggle-bar"></span>
          <span class="am-nav__toggle-bar"></span>
        </button>

      </div>
    </nav>
  `;

  _bindToggle();
  _markActiveLink();
  _bindSearch();
  _bindProfileDropdown();
}

function _bindSearch() {
  const input = document.getElementById("search-input");
  if (!input) return;

  document.dispatchEvent(new CustomEvent("navbar:ready", { detail: { input } }));

  const enIndex = window.location.pathname
    .toLowerCase()
    .includes("/index-folder/index.html");

  if (enIndex) return;

  input.addEventListener("keydown", (e) => {
    if (e.key !== "Enter") return;
    e.preventDefault();

    const termino = input.value.trim();
    const url = new URL("../index-folder/Index.html", window.location.href);
    if (termino) {
      url.searchParams.set("busqueda", termino);
    }
    window.location.href = url.toString();
  });
}

function _bindToggle() {
  const toggle = document.getElementById("amNavToggle");
  const links = document.getElementById("amNavLinks");
  if (!toggle || !links) return;

  toggle.addEventListener("click", () => {
    const isOpen = links.classList.toggle("am-nav__links--open");
    toggle.classList.toggle("am-nav__toggle--open", isOpen);
    toggle.setAttribute("aria-expanded", String(isOpen));
  });

  document.addEventListener("click", (e) => {
    if (!toggle.contains(e.target) && !links.contains(e.target)) {
      links.classList.remove("am-nav__links--open");
      toggle.classList.remove("am-nav__toggle--open");
      toggle.setAttribute("aria-expanded", "false");
    }
  });
}

function _markActiveLink() {
  const currentPage = window.location.pathname.split("/").pop() || "Index.html";
  const links = document.querySelectorAll(".am-nav__link");
  links.forEach((link) => {
    const href = link.getAttribute("href").split("/").pop();
    if (href === currentPage) {
      link.classList.add("am-nav__link--active");
    }
  });
}

function _bindProfileDropdown() {
  const btn = document.getElementById("navProfileBtn");
  const dropdown = document.getElementById("navDropdown");
  if (!btn || !dropdown) return;

  btn.addEventListener("click", (e) => {
    e.stopPropagation();
    dropdown.classList.toggle("am-nav__dropdown--open");
  });

  document.addEventListener("click", () => {
    dropdown.classList.remove("am-nav__dropdown--open");
  });

  const cerrar = document.getElementById("navCerrarSesion");
  if (cerrar) {
    cerrar.addEventListener("click", () => {
      localStorage.removeItem("token");
      window.location.href = "../login-folder/Login.html";
    });
  }
}

document.addEventListener("DOMContentLoaded", () => {
  initNavbar();
  mostrarLinkSucursales();
  initBtnCooldown();
});

const COOLDOWN_MS = 1500;

function initBtnCooldown() {
  document.addEventListener("click", (e) => {
    const btn = e.target.closest("button");
    if (btn && !btn.disabled && !btn.hasAttribute("data-no-cooldown")) {
      const cd = parseInt(btn.getAttribute("data-cooldown"), 10);
      const ms = cd > 0 ? cd : COOLDOWN_MS;
      btn.disabled = true;
      btn.style.pointerEvents = "none";
      setTimeout(() => {
        btn.disabled = false;
        btn.style.pointerEvents = "";
      }, ms);
    }

    const link = e.target.closest("a");
    if (link && !link.hasAttribute("data-no-cooldown")) {
      const cd = parseInt(link.getAttribute("data-cooldown"), 10);
      const ms = cd > 0 ? cd : COOLDOWN_MS;
      if (link.dataset.cooldownUntil) {
        const remaining = parseInt(link.dataset.cooldownUntil, 10) - Date.now();
        if (remaining > 0) {
          e.preventDefault();
          return;
        }
      }
      link.dataset.cooldownUntil = Date.now() + ms;
      link.style.pointerEvents = "none";
      setTimeout(() => {
        delete link.dataset.cooldownUntil;
        link.style.pointerEvents = "";
      }, ms);
    }
  }, true);
}

async function mostrarLinkSucursales() {
  try {
    const token = localStorage.getItem("token");
    if (!token) return;

    const res = await fetch("/usuarios/me", {
      headers: { Authorization: `Bearer ${token}` }
    });
    if (!res.ok) return;

    const user = await res.json();
    const roles = user.roles || [];
    const tiene = roles.includes("EMPLEADOR") || roles.includes("EMPLEADO");

    const link = document.getElementById("nav-mis-sucursales");
    if (link) link.style.display = tiene ? "" : "none";

    const solicitudesLink = document.getElementById("nav-solicitudes-link");

    const avatar = document.getElementById("navProfileAvatar");
    if (avatar) {
      if (user.fotoPerfil) {
        avatar.innerHTML = `<img src="${user.fotoPerfil}" alt="" style="width:100%;height:100%;border-radius:50%;object-fit:cover;">`;
      } else {
        avatar.textContent = (user.nombre || "")[0].toUpperCase();
      }
    }
  } catch {
  }
}