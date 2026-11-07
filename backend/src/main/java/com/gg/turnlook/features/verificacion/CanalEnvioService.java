package com.gg.turnlook.features.verificacion;

import com.gg.turnlook.features.verificacion.enums.CanalVerificacion;

public interface CanalEnvioService {

    CanalVerificacion getCanal();

    void enviarCodigo(String destinatario,String codigo);


}
