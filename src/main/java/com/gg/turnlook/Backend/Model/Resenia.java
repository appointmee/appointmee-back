package com.gg.turnlook.Backend.Model;



import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;


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


    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turno_id", unique = true)
    private Turno turno;



    @PrePersist
    public void prePersist() {

        this.fechaResenia = LocalDate.now();
    }



    /// CONSTRUCTOR


    public Resenia(Integer puntuacion, String comentario, Turno turno) {
        this.puntuacion = puntuacion;
        this.comentario = comentario;
        this.turno = turno;
    }


}




