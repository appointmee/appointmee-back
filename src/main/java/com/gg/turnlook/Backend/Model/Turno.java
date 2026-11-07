package com.gg.turnlook.Backend.Model;




import com.gg.turnlook.Backend.Enum.EstadoTurno;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;




@Entity
@Table(name = "turnos")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Turno {



    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "fecha_reserva", nullable = false)
    private LocalDateTime fechaReserva;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoTurno estado = EstadoTurno.PENDIENTE;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Usuario cliente;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id")
    private Usuario empleado;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servicio_id")
    private Servicio servicio;


    @OneToOne(mappedBy = "turno")
    private Resenia resenia;




    @PrePersist
    public void prePersist() {
        this.fechaReserva = LocalDateTime.now();
    }




    /// CONSTRUCTOR


    public Turno(LocalDateTime fechaHora, Usuario cliente, Usuario empleado, Servicio servicio) {
        this.fechaHora = fechaHora;
        this.cliente = cliente;
        this.empleado = empleado;
        this.servicio = servicio;
    }


}



