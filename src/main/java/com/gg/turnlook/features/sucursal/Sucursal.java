package com.gg.turnlook.features.sucursal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gg.turnlook.common.util.DiasSemanaConverter;
import com.gg.turnlook.features.imagen.Imagen;
import com.gg.turnlook.features.sucursal.categoria.Categoria;
import com.gg.turnlook.features.usuario.Usuario;
import jakarta.persistence.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;






@Entity
@Table(name = "sucursales")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Sucursal {

    /// ATRIBUTOS

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String nombre;

    @Column(unique = true, nullable = false)
    private String direccion;

    @Column(nullable = true)
    private String telefono;

    @Column(nullable = false)
    private String descripcion;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDate fechaCreacion;

    @Column(name = "hora_apertura", nullable = false)
    private LocalTime horaApertura;

    @Column(name = "hora_cierre", nullable = false)
    private LocalTime horaCierre;

    /// Dias de la semana que abre la sucursal (los que no estan marcados se toman como cerrados).
    /// Se guardan en una sola columna de texto con los nombres separados por comas.
    /// Si la columna esta vacia se asume que la sucursal abre todos los dias.

    @Convert(converter = DiasSemanaConverter.class)
    @Column(name = "dias_apertura", length = 120)
    @JsonIgnore
    private Set<DayOfWeek> diasApertura = new HashSet<>();

    @Column(nullable = false)
    private boolean activo = true;


    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "imagen_id")
    private Imagen fotoPerfil;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleador_id")
    private Usuario empleador;


    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "sucursal_empleados",
            joinColumns = @JoinColumn(name = "sucursal_id"),
            inverseJoinColumns = @JoinColumn(name = "usuario_id")
    )
    @JsonIgnoreProperties("sucursalesEmpleado")
    private Set<Usuario> empleados = new HashSet<>();




    @PrePersist
    public void prePersist() {
        this.fechaCreacion = LocalDate.now();
    }


    /// METODOS


    /// Si la sucursal abre ese dia de la semana.
    /// Si todavia no se cargo ningun dia se asume que abre todos (sucursales viejas).

    public boolean abreElDia(DayOfWeek dia) {
        return this.diasApertura == null || this.diasApertura.isEmpty()
                || this.diasApertura.contains(dia);
    }


    /// Reemplaza los dias de apertura por los indicados.
    /// Se modifica la coleccion en el lugar para que Hibernate detecte el cambio.

    public void setDiasAperturaPorNombre(Set<DayOfWeek> dias) {
        if (this.diasApertura == null) this.diasApertura = new HashSet<>();
        this.diasApertura.clear();
        this.diasApertura.addAll(dias);
    }



    /// CONSTRUCTORES


    public Sucursal(String nombre, String direccion, String telefono, String descripcion, LocalTime horaApertura, LocalTime horaCierre, Categoria categoria, Usuario empleador) {
        this.nombre = nombre;
        this.direccion = direccion;
        this.telefono = telefono;
        this.descripcion = descripcion;
        this.horaApertura = horaApertura;
        this.horaCierre = horaCierre;
        this.categoria = categoria;
        this.empleador = empleador;
    }


}



