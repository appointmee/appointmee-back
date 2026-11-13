package com.gg.turnlook.features.imagen.dto;

public record ImagenSubidaDTO(String url, String publicId, String nombreArchivo, String tokenCancelacion) {
    public ImagenSubidaDTO(String url, String publicId, String nombreArchivo) {
        this(url, publicId, nombreArchivo, null);
    }
}
