package com.gg.turnlook.features.verificacion;

import com.gg.turnlook.features.verificacion.enums.CanalVerificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificacionRepository  extends JpaRepository<Verificacion,Long> {

    Optional<Verificacion> findFirstByUsuarioIdAndCanalOrderByFechaCreacionDescIdDesc(
            Integer usuarioId, CanalVerificacion canal);

    Optional<Verificacion> findFirstByUsuarioIdAndCanalAndDestinatarioOrderByFechaCreacionDescIdDesc(
            Integer usuarioId, CanalVerificacion canal, String destinatario);

    long countByUsuarioIdAndCanalAndDestinatario(Integer usuarioId, CanalVerificacion canal, String destinatario);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update Verificacion v set v.fechaUso = :ahora where v.usuario.id = :usuarioId and v.fechaUso is null")
    void invalidarPorUsuario(Integer usuarioId, java.time.Instant ahora);

    Optional<Verificacion> findFirstByCanalAndDestinatarioOrderByFechaCreacionDesc(
            CanalVerificacion canal,
            String destinatario);

}
