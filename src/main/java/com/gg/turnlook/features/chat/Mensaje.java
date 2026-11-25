package com.gg.turnlook.features.chat;

import com.gg.turnlook.features.usuario.Usuario;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mensaje persistido de una conversación.
 * El estado de entrega arranca en ENVIADO y se actualiza con los ACK del destinatario.
 */
@Entity
@Table(name = "mensajes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_mensaje_conversacion_cliente_mensaje",
                columnNames = {"conversacion_id", "cliente_mensaje_id"}))
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Mensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversacion_id", nullable = false)
    private Conversacion conversacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @Column(name = "contenido", nullable = false, length = 2000)
    private String contenido;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_entrega", nullable = false, length = 20)
    private EstadoEntrega estadoEntrega = EstadoEntrega.ENVIADO;

    /** Identificador generado por el cliente para evitar duplicados al reintentar. */
    @Column(name = "cliente_mensaje_id", nullable = false)
    private UUID clienteMensajeId;

    @PrePersist
    public void prePersist() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = LocalDateTime.now();
        }
        if (this.estadoEntrega == null) {
            this.estadoEntrega = EstadoEntrega.ENVIADO;
        }
    }
}
