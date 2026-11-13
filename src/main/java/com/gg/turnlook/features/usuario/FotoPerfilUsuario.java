package com.gg.turnlook.features.usuario;

import com.gg.turnlook.features.imagen.Imagen;

public final class FotoPerfilUsuario {

    public static final String URL_PREDETERMINADA =
            "https://static.vecteezy.com/system/resources/previews/026/434/409/non_2x/default-avatar-profile-icon-social-media-user-photo-vector.jpg";

    private FotoPerfilUsuario() {
    }

    public static String resolver(Imagen imagen) {
        if (imagen == null) {
            return URL_PREDETERMINADA;
        }
        return resolver(imagen.getFotoValida());
    }

    public static String resolver(String url) {
        if (url == null || url.isBlank()) {
            return URL_PREDETERMINADA;
        }
        return url;
    }
}
