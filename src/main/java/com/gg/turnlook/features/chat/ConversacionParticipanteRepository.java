package com.gg.turnlook.features.chat;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversacionParticipanteRepository
        extends JpaRepository<ConversacionParticipante, Integer> {

    Optional<ConversacionParticipante> findByConversacionIdAndUsuarioEmail(
            Integer conversacionId, String email);

    List<ConversacionParticipante> findByConversacionId(Integer conversacionId);

    List<ConversacionParticipante> findByUsuarioEmailOrderByConversacionIdDesc(String email);
}
