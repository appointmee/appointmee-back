package com.gg.turnlook.features.chat;

import com.gg.turnlook.features.sucursal.Sucursal;
import com.gg.turnlook.features.turno.Turno;
import com.gg.turnlook.features.usuario.Usuario;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Conversación entre el cliente y el empleado que lo atiende.
 * El empleado puede ser el empleador de la sucursal. Puede estar asociada
 * a un turno o existir de forma independiente.
 */
@Entity
@Table(name = "conversaciones")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Conversacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turno_id")
    private Turno turno;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Usuario cliente;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    /** Último mensaje del hilo. Se mantiene para ordenar listados sin subconsultas. */
    @Column(name = "ultimo_mensaje_id")
    private Long ultimoMensajeId;

    @PrePersist
    public void prePersist() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = LocalDateTime.now();
        }
    }
}
