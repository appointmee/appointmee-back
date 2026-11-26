package com.gg.turnlook.features.solicitud.empleador;

import com.gg.turnlook.features.solicitud.EstadoSolicitud;
import com.gg.turnlook.features.usuario.Usuario;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;






@Entity
@Table(name = "solicitudes_empleador")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SolicitudEmpleador {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 3000)
    private String motivo;

    @Enumerated(EnumType.STRING)
    private EstadoSolicitud estado;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDate fechaSolicitud;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    /// Ubicaciones propuestas en la solicitud. Se guardan con la solicitud y se
    /// recuperan en el detalle. No se usan para crear sucursales.
    @OneToMany(mappedBy = "solicitud", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<UbicacionSolicitud> ubicaciones = new ArrayList<>();



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


    /// Mantiene los dos lados de la relacion para que Hibernate persista cada
    /// ubicacion con su solicitud.
    public void agregarUbicacion(UbicacionSolicitud ubicacion) {
        if (this.ubicaciones == null) {
            this.ubicaciones = new ArrayList<>();
        }
        this.ubicaciones.add(ubicacion);
        ubicacion.setSolicitud(this);
    }



}





