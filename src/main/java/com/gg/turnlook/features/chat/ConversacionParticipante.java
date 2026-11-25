package com.gg.turnlook.features.chat;

import com.gg.turnlook.features.usuario.Usuario;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Participante autorizado de una conversación: quien solicita el turno o quien
 * lo desarrolla. El rol se deriva del turno al responder, no se persiste.
 */
@Entity
@Table(name = "conversacion_participantes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_participante_conversacion_usuario",
                columnNames = {"conversacion_id", "usuario_id"}))
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConversacionParticipante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversacion_id", nullable = false)
    private Conversacion conversacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_alta", nullable = false)
    private LocalDateTime fechaAlta;

    /** Último mensaje que el participante marcó como leído. */
    @Column(name = "ultimo_mensaje_leido_id")
    private Long ultimoMensajeLeidoId;

    @PrePersist
    public void prePersist() {
        if (this.fechaAlta == null) {
            this.fechaAlta = LocalDateTime.now();
        }
    }
}
