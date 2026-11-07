package com.gg.turnlook.Backend.Model;



import com.gg.turnlook.Backend.Enum.EstadoSolicitud;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Entity
@Table(name = "solicitudes_empleador")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SolicitudEmpleador {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String motivo;

    @Enumerated(EnumType.STRING)
    private EstadoSolicitud estado;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDate fechaSolicitud;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;



    @PrePersist
    public void prePersist() {

        this.estado = EstadoSolicitud.PENDIENTE;
        this.fechaSolicitud = LocalDate.now();
    }



    ///  CONSTRUCTOR


    public SolicitudEmpleador(String motivo, Usuario usuario) {
        this.motivo = motivo;
        this.usuario = usuario;
    }



}





