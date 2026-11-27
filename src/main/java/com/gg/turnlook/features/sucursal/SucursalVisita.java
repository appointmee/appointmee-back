package com.gg.turnlook.features.sucursal;

import com.gg.turnlook.features.usuario.Usuario;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Registro persistente de la ultima visita de un usuario a una sucursal. */
@Entity
@Table(name = "sucursales_vistas", uniqueConstraints = @UniqueConstraint(
        name = "ux_sucursales_vistas_usuario_sucursal",
        columnNames = {"usuario_id", "sucursal_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SucursalVisita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @Column(name = "fecha_visita", nullable = false)
    private LocalDateTime fechaVisita;

    @PrePersist
    public void prePersist() {
        if (this.fechaVisita == null) {
            this.fechaVisita = LocalDateTime.now();
        }
    }

}
