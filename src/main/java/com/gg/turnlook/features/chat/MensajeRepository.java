package com.gg.turnlook.features.chat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

    List<Mensaje> findByConversacionIdOrderByIdDesc(Integer conversacionId, Pageable pageable);

    List<Mensaje> findByConversacionIdAndIdLessThanOrderByIdDesc(
            Integer conversacionId, Long id, Pageable pageable);

    /** Recuperación después de una reconexión. */
    List<Mensaje> findByConversacionIdAndIdGreaterThanOrderByIdAsc(
            Integer conversacionId, Long id, Pageable pageable);

    Optional<Mensaje> findTopByConversacionIdOrderByIdDesc(Integer conversacionId);

    Optional<Mensaje> findByConversacionIdAndClienteMensajeId(
            Integer conversacionId, UUID clienteMensajeId);

    long countByConversacionIdAndAutorEmailNot(Integer conversacionId, String email);

    long countByConversacionIdAndIdGreaterThanAndAutorEmailNot(
            Integer conversacionId, Long id, String email);

    /** Marca como entregados los mensajes de otros autores que seguían en ENVIADO. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Mensaje m set m.estadoEntrega = :nuevo
            where m.conversacion.id = :conversacionId
              and m.autor.id <> :usuarioId
              and m.estadoEntrega = :actual
            """)
    int marcarEntregados(@Param("conversacionId") Integer conversacionId,
                         @Param("usuarioId") Integer usuarioId,
                         @Param("actual") EstadoEntrega actual,
                         @Param("nuevo") EstadoEntrega nuevo);

    /** Marca como leídos los mensajes de otros autores hasta el id indicado. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Mensaje m set m.estadoEntrega = :nuevo
            where m.conversacion.id = :conversacionId
              and m.autor.id <> :usuarioId
              and m.id <= :ultimoId
              and m.estadoEntrega <> :nuevo
            """)
    int marcarLeidos(@Param("conversacionId") Integer conversacionId,
                     @Param("usuarioId") Integer usuarioId,
                     @Param("ultimoId") Long ultimoId,
                     @Param("nuevo") EstadoEntrega nuevo);
}
