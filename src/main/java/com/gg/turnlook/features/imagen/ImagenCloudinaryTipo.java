package com.gg.turnlook.features.imagen;

public enum ImagenCloudinaryTipo {
    PERFIL("perfiles"),
    SUCURSAL("sucursales"),
    SERVICIO("servicios"),
    GALERIA("galerias");

    private final String carpeta;

    ImagenCloudinaryTipo(String carpeta) {
        this.carpeta = carpeta;
    }

    public String getCarpeta() {
        return carpeta;
    }
}
