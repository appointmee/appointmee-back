package com.gg.turnlook.Backend.Model;



import com.gg.turnlook.Backend.Enum.EstadoSolicitud;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;



@Entity
@Table(name = "solicitudes_empleado")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SolicitudEmpleado {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    private EstadoSolicitud estado;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDate fechaSolicitud;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id")
    private Usuario empleado;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id")
    private Sucursal sucursal;



    @PrePersist
    public void prePersist() {
        this.estado = EstadoSolicitud.PENDIENTE;
        this.fechaSolicitud = LocalDate.now();
    }



    /// CONSTRUCTOR


    public SolicitudEmpleado(Sucursal sucursal, Usuario empleado) {
        this.sucursal = sucursal;
        this.empleado = empleado;
    }


}





