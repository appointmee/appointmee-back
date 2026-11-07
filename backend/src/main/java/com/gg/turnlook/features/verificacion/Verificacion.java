package com.gg.turnlook.features.verificacion;

import com.gg.turnlook.features.verificacion.enums.CanalVerificacion;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name="verificacion")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Setter

public class Verificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Null en códigos genéricos; los códigos de cuenta se vinculan a su usuario.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private com.gg.turnlook.features.usuario.Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private CanalVerificacion canal;

    @Column(nullable=false)
    private String destinatario;

    @Column(nullable=false)
    private String codigoHash;

    @Column(nullable=false)
    private Instant fechaCreacion;

    @Column(nullable=false)
    private Instant fechaVencimiento;

    private Instant fechaUso;

    @Column(nullable = false)
    private Integer intentosFallidos;

}
