package com.gg.turnlook.features.solicitud.empleador;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Ubicacion propuesta dentro de una solicitud de empleador.
 *
 * No es una Sucursal: solo se guarda para que el administrador evalue el lugar.
 * No seusa para crear sucursales ni se consulta Places desde el backend.
 */
@Entity
@Table(name = "solicitudes_empleador_ubicaciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UbicacionSolicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 500)
    private String direccion;

    @Column(nullable = false)
    private Double latitud;

    @Column(nullable = false)
    private Double longitud;

    @Column(name = "google_place_id", nullable = false, length = 255)
    private String googlePlaceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitud_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_solicitudes_empleador_ubicaciones_solicitud"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private SolicitudEmpleador solicitud;

    public UbicacionSolicitud(String direccion, Double latitud, Double longitud,
                              String googlePlaceId) {
        this.direccion = direccion;
        this.latitud = latitud;
        this.longitud = longitud;
        this.googlePlaceId = googlePlaceId;
    }

}
