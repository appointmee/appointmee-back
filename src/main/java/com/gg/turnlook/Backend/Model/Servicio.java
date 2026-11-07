package com.gg.turnlook.Backend.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "servicios")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Servicio {

    /// ATRIBUTOS

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String descripcion;

    @Column(nullable = false)
    private Integer duracion;

    @Column(nullable = false)
    private BigDecimal precio;

    @Column(nullable = false)
    private boolean activo = true;


    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "imagen_id")
    private Imagen fotoPerfil;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id")
    private Sucursal sucursal;




    /// CONSTRUCTORES
    public Servicio(String nombre, String descripcion, Integer duracion, BigDecimal precio, Sucursal sucursal) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.duracion = duracion;
        this.precio = precio;
        this.sucursal = sucursal;
    }

}
