package com.gg.turnlook.features.chat;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversacionRepository extends JpaRepository<Conversacion, Integer> {

    /** Una sola conversación por turno. */
    Optional<Conversacion> findByTurnoId(Integer turnoId);
}
