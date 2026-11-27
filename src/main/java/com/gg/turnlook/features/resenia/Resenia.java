package com.gg.turnlook.features.resenia;

import com.gg.turnlook.features.turno.Turno;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;






@Entity
@Table(name = "resenias")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Resenia {


    /// ATRIBUTOS

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer puntuacion;

    @Column(nullable = true)
    private String comentario;

    @Column(name = "fecha_resenia", nullable = false)
    private LocalDate fechaResenia;

    /**
     * Datos historicos del empleado que atendio el turno. Se copian al crear la
     * resenia y no dependen de la entidad Usuario: si la cuenta se da de baja o
     * se desvincula de la sucursal, el nombre mostrado sigue siendo el original.
     * EmpleadoId se guarda como valor simple (sin FK) para que no pueda cascadear
     * sobre la resenia al eliminar la cuenta.
     */
    @Column(name = "empleado_id")
    private Integer empleadoId;

    @Column(name = "empleado_nombre")
    private String empleadoNombre;

    @Column(name = "empleado_apellido")
    private String empleadoApellido;


    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turno_id", unique = true)
    private Turno turno;



    @PrePersist
    public void prePersist() {

        this.fechaResenia = LocalDate.now();
    }



    /// CONSTRUCTOR


    /** Resenia sin empleado: conserva el comportamiento anterior. */
    public Resenia(Integer puntuacion, String comentario, Turno turno) {
        this(puntuacion, comentario, turno, null, null, null);
    }

    public Resenia(Integer puntuacion, String comentario, Turno turno,
                   Integer empleadoId, String empleadoNombre, String empleadoApellido) {
        this.puntuacion = puntuacion;
        this.comentario = comentario;
        this.turno = turno;
        this.empleadoId = empleadoId;
        this.empleadoNombre = empleadoNombre;
        this.empleadoApellido = empleadoApellido;
    }


}




