const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const test = require("node:test");
const vm = require("node:vm");

const archivo = path.resolve(__dirname, "../../main/resources/static/empleador-folder/empleador.js");
const codigo = fs.readFileSync(archivo, "utf8");
const finControles = codigo.indexOf("// Enciende los botones");
const archivoModulo = path.resolve(__dirname, "../../main/resources/static/recursos/modulos.js");
const codigoImagen = fs.readFileSync(archivoModulo, "utf8");
const inicio = codigoImagen.indexOf("export function activarSubidaImagen(");
const fin = codigoImagen.indexOf("export async function sesionActiva(", inicio);
const controladorImagen = codigoImagen.slice(inicio, fin).replace("export ", "");
assert.ok(inicio >= 0 && fin > inicio, "No se encontró el controlador de archivos");

function crearInput() {
    const eventos = {};
    let valor = "";
    return {
        files: [],
        eventos,
        addEventListener(nombre, funcion) { eventos[nombre] = funcion; },
        get value() { return valor; },
        set value(nuevoValor) {
            valor = nuevoValor;
            if (nuevoValor === "") this.files = [];
        }
    };
}

test("el formulario no permite cancelar mientras se guarda", async () => {
    const inicioGuardar = codigo.indexOf("async function guardarModal(");
    const guardarModal = vm.runInNewContext(
        codigo.slice(inicioGuardar, finControles) + "\nguardarModal"
    );
    const botones = [{ disabled: false }, { disabled: false }, { disabled: true }];
    const modal = { dataset: {}, querySelectorAll: selector => {
        assert.match(selector, /input/);
        assert.match(selector, /textarea/);
        return botones;
    } };
    let terminarGuardado;
    const guardado = guardarModal(modal, () => new Promise(resolve => { terminarGuardado = resolve; }));

    assert.equal(modal.dataset.guardando, "true");
    assert.ok(botones.every(boton => boton.disabled));

    terminarGuardado();
    await guardado;
    assert.equal(modal.dataset.guardando, undefined);
    assert.equal(botones[0].disabled, false);
    assert.equal(botones[1].disabled, false);
    assert.equal(botones[2].disabled, true);
});

test("no permite guardar mientras se descarta una imagen al cancelar", async () => {
    const inicioCerrar = codigo.indexOf("async function cerrarModal(");
    const botones = [{ disabled: false }, { disabled: false }];
    let terminarDescarte;
    let guardados = 0;
    const modal = {
        dataset: {}, querySelectorAll: () => botones,
        descartarImagenPendiente: () => new Promise(resolve => { terminarDescarte = resolve; }),
        remove() { this.eliminado = true; }
    };
    const contexto = vm.createContext({ alert: () => {} });
    vm.runInContext(codigo.slice(inicioCerrar, finControles), contexto);
    const cierre = vm.runInContext("cerrarModal", contexto)(modal);

    assert.equal(modal.dataset.guardando, "true");
    assert.ok(botones.every(boton => boton.disabled));
    await vm.runInContext("guardarModal", contexto)(modal, () => { guardados++; });
    assert.equal(guardados, 0);

    terminarDescarte();
    await cierre;
    assert.equal(modal.eliminado, true);
});

test("seleccionar y cancelar no sube; guardar sí sube la imagen", async () => {
    const archivoInput = crearInput();
    const urlInput = crearInput();
    const publicIdInput = crearInput();
    const estado = { textContent: "" };
    const elementos = { archivoInput, urlInput, publicIdInput, estado };
    const subidas = [];
    const contexto = vm.createContext({
        document: { getElementById: id => elementos[id] },
        subirImagenCloudinary: async (archivoSeleccionado, tipo) => {
            subidas.push({ archivoSeleccionado, tipo });
            return { url: "https://res.cloudinary.com/cloud/foto.jpg", publicId: "appointmee/servicios/foto" };
        }
    });
    const activarSubidaImagen = vm.runInContext(controladorImagen + "\nactivarSubidaImagen", contexto);
    const subirAlGuardar = activarSubidaImagen("archivoInput", "urlInput", "publicIdInput", "SERVICIO", "estado");

    archivoInput.files = [{ name: "foto.jpg" }];
    archivoInput.eventos.change();
    assert.equal(subidas.length, 0);
    assert.match(estado.textContent, /Se subirá al guardar/);

    // Cerrar el formulario no invoca subirAlGuardar.
    assert.equal(subidas.length, 0);

    await subirAlGuardar();
    assert.equal(subidas.length, 1);
    assert.equal(subidas[0].tipo, "SERVICIO");
    assert.equal(urlInput.value, "https://res.cloudinary.com/cloud/foto.jpg");
    assert.equal(publicIdInput.value, "appointmee/servicios/foto");
});

test("escribir una URL descarta el archivo seleccionado", async () => {
    const archivoInput = crearInput();
    const urlInput = crearInput();
    const publicIdInput = crearInput();
    const elementos = { archivoInput, urlInput, publicIdInput };
    let subidas = 0;
    const contexto = vm.createContext({
        document: { getElementById: id => elementos[id] },
        subirImagenCloudinary: async () => { subidas++; }
    });
    const activarSubidaImagen = vm.runInContext(controladorImagen + "\nactivarSubidaImagen", contexto);
    const subirAlGuardar = activarSubidaImagen("archivoInput", "urlInput", "publicIdInput", "SUCURSAL", "estado");

    archivoInput.files = [{ name: "foto.jpg" }];
    archivoInput.eventos.change();
    urlInput.value = "https://example.com/foto.jpg";
    urlInput.eventos.input();
    await subirAlGuardar();

    assert.equal(subidas, 0);
    assert.equal(archivoInput.files.length, 0);
});

test("si falla el guardado se puede descartar la imagen recién subida", async () => {
    const archivoInput = crearInput();
    const urlInput = crearInput();
    const publicIdInput = crearInput();
    const elementos = { archivoInput, urlInput, publicIdInput };
    const descartadas = [];
    const contexto = vm.createContext({
        document: { getElementById: id => elementos[id] },
        subirImagenCloudinary: async () => ({
            url: "https://res.cloudinary.com/cloud/foto.jpg",
            publicId: "appointmee/servicios/foto",
            tokenCancelacion: "token"
        }),
        descartarImagenCloudinary: async imagen => { descartadas.push(imagen.publicId); }
    });
    const activarSubidaImagen = vm.runInContext(controladorImagen + "\nactivarSubidaImagen", contexto);
    const subirAlGuardar = activarSubidaImagen("archivoInput", "urlInput", "publicIdInput", "SERVICIO", "estado");

    archivoInput.files = [{ name: "foto.jpg" }];
    await subirAlGuardar();
    await subirAlGuardar.descartar();

    assert.deepEqual(descartadas, ["appointmee/servicios/foto"]);
    assert.equal(urlInput.value, "");
    assert.equal(publicIdInput.value, "");
});

test("la foto de perfil se sube al pulsar Modificar", async () => {
    const rutaPerfil = path.resolve(__dirname, "../../main/resources/static/perfil-folder/perfil.js");
    const codigoPerfil = fs.readFileSync(rutaPerfil, "utf8");
    const inicioPerfil = codigoPerfil.indexOf("async function modificarUsuario(");
    const finPerfil = codigoPerfil.indexOf("async function eliminarCuenta(", inicioPerfil);
    assert.ok(inicioPerfil >= 0 && finPerfil > inicioPerfil);

    const elementos = {
        modificar: { disabled: false },
        cancelar: { disabled: false },
        "nombre-input": { value: "" },
        "apellido-input": { value: "" },
        "password-input": { value: "" },
        "email-input": { value: "" },
        "password-confirm-input": { value: "" },
        "foto-file-input": crearInput(),
        "foto-url-input": crearInput(),
        "foto-public-id-input": crearInput(),
        "foto-upload-status": { textContent: "" }
    };
    elementos["foto-file-input"].files = [{ name: "perfil.jpg" }];
    const peticiones = [];
    let subidas = 0;
    const contexto = vm.createContext({
        document: { getElementById: id => elementos[id] },
        subirImagenCloudinary: async () => {
            assert.equal(elementos["foto-file-input"].disabled, true);
            assert.equal(elementos["foto-url-input"].disabled, true);
            subidas++;
            return { url: "https://res.cloudinary.com/cloud/perfil.jpg", publicId: "appointmee/perfiles/perfil", tokenCancelacion: "token" };
        },
        descartarImagenCloudinary: async () => {},
        fetch: async (url, opciones) => {
            peticiones.push({ url, opciones });
            return { ok: true };
        },
        authHeaders: () => ({}),
        checkResSinRedireccion: async () => {},
        alert: () => {},
        console,
        API_URL: ""
    });
    const modificarUsuario = vm.runInContext(
        controladorImagen + "\n" + codigoPerfil.slice(inicioPerfil, finPerfil) + "\nmodificarUsuario", contexto
    );
    const fotoPendiente = vm.runInContext("activarSubidaImagen", contexto)(
        "foto-file-input", "foto-url-input", "foto-public-id-input", "PERFIL", "foto-upload-status");

    assert.equal(subidas, 0);
    await modificarUsuario(12, fotoPendiente);

    assert.equal(subidas, 1);
    assert.equal(peticiones.length, 1);
    assert.equal(peticiones[0].url, "/usuarios/modificar/12");
    assert.deepEqual(JSON.parse(peticiones[0].opciones.body), {
        fotoUrl: "https://res.cloudinary.com/cloud/perfil.jpg",
        fotoPublicId: "appointmee/perfiles/perfil"
    });
    assert.equal(elementos.modificar.disabled, false);
    assert.equal(elementos.cancelar.disabled, false);
    assert.equal(elementos["foto-file-input"].disabled, false);
});

test("si falla el guardado del perfil, descarta la foto subida", async () => {
    const rutaPerfil = path.resolve(__dirname, "../../main/resources/static/perfil-folder/perfil.js");
    const codigoPerfil = fs.readFileSync(rutaPerfil, "utf8");
    const inicioPerfil = codigoPerfil.indexOf("async function modificarUsuario(");
    const finPerfil = codigoPerfil.indexOf("async function eliminarCuenta(", inicioPerfil);
    const elementos = {
        modificar: { disabled: false }, cancelar: { disabled: false },
        "nombre-input": { value: "" }, "apellido-input": { value: "" },
        "password-input": { value: "" }, "email-input": { value: "" },
        "password-confirm-input": { value: "" },
        "foto-file-input": crearInput(), "foto-url-input": crearInput(),
        "foto-public-id-input": crearInput(), "foto-upload-status": { textContent: "" }
    };
    elementos["foto-file-input"].files = [{ name: "perfil.jpg" }];
    const descartadas = [];
    const contexto = vm.createContext({
        document: { getElementById: id => elementos[id] },
        subirImagenCloudinary: async () => ({
            url: "https://res.cloudinary.com/cloud/perfil.jpg",
            publicId: "appointmee/perfiles/perfil", tokenCancelacion: "token"
        }),
        descartarImagenCloudinary: async imagen => { descartadas.push(imagen.publicId); },
        fetch: async () => ({ ok: false, text: async () => "Falló el guardado" }),
        checkResSinRedireccion: async response => {
            if (!response.ok) throw new Error(await response.text());
        },
        authHeaders: () => ({}), alert: () => {},
        console: { error: () => {} }, API_URL: ""
    });
    const modificarUsuario = vm.runInContext(
        controladorImagen + "\n" + codigoPerfil.slice(inicioPerfil, finPerfil) + "\nmodificarUsuario", contexto
    );
    const fotoPendiente = vm.runInContext("activarSubidaImagen", contexto)(
        "foto-file-input", "foto-url-input", "foto-public-id-input", "PERFIL", "foto-upload-status");

    await modificarUsuario(12, fotoPendiente);

    assert.deepEqual(descartadas, ["appointmee/perfiles/perfil"]);
    assert.equal(elementos["foto-url-input"].value, "");
    assert.equal(elementos["foto-public-id-input"].value, "");
    assert.equal(elementos.cancelar.disabled, false);
});

test("un error de guardado no cambia de página antes de limpiar la imagen", async () => {
    const rutaModulo = path.resolve(__dirname, "../../main/resources/static/recursos/modulos.js");
    const codigoModulo = fs.readFileSync(rutaModulo, "utf8");
    const inicioVerificador = codigoModulo.indexOf("export async function checkResSinRedireccion(");
    const finVerificador = codigoModulo.indexOf("export async function apiFetch(", inicioVerificador);
    const ventana = { location: { href: "perfil.html" } };
    const comprobar = vm.runInNewContext(
        codigoModulo.slice(inicioVerificador, finVerificador).replace("export ", "")
            + "\ncheckResSinRedireccion", { window: ventana }
    );

    await assert.rejects(
        comprobar({ ok: false, text: async () => "Falló el guardado" }),
        /Falló el guardado/
    );
    assert.equal(ventana.location.href, "perfil.html");
});

test("la galería conecta su botón Cerrar y bloquea el cierre mientras sube", async () => {
    const elementos = {};
    const modal = {
        dataset: {},
        set innerHTML(html) {
            for (const coincidencia of html.matchAll(/id="([^"]+)"/g)) {
                elementos[coincidencia[1]] = { ...crearInput(), disabled: false, textContent: "" };
            }
        },
        querySelectorAll: () => Object.values(elementos),
        remove() { this.eliminado = true; }
    };
    let terminarSubida;
    const contexto = vm.createContext({
        crearModal: () => modal,
        document: {
            getElementById: id => elementos[id],
            querySelectorAll: () => []
        },
        cargarImagenesEnModal: async () => {},
        FormData: class { append() {} },
        fetch: () => new Promise(resolve => { terminarSubida = resolve; }),
        authHeaders: () => ({}),
        checkResSinRedireccion: async () => {},
        API_URL: "", alert: () => {}
    });
    const inicioCerrar = codigo.indexOf("async function cerrarModal(");
    vm.runInContext(codigo.slice(inicioCerrar, finControles), contexto);
    const inicioGaleria = codigo.indexOf("async function abrirModalImagenes(");
    const finGaleria = codigo.indexOf("async function cargarImagenesEnModal(", inicioGaleria);
    const abrirGaleria = vm.runInContext(
        codigo.slice(inicioGaleria, finGaleria) + "\nabrirModalImagenes", contexto);

    await abrirGaleria(26);
    const cerrar = elementos["btn-cerrar-imagenes"].eventos.click;
    const archivoInput = elementos["imagenes-archivos"];
    archivoInput.files = [{ name: "galeria.jpg" }];
    const subida = archivoInput.eventos.change({ currentTarget: archivoInput });
    await cerrar();
    assert.equal(modal.eliminado, undefined);
    assert.equal(archivoInput.disabled, true);

    terminarSubida({ ok: true });
    await subida;
    assert.equal(archivoInput.disabled, false);
    await cerrar();
    assert.equal(modal.eliminado, true);
});
