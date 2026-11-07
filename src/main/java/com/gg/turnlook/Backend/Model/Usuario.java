package com.gg.turnlook.Backend.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "usuarios")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Usuario {


    /// ATRIBUTOS
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    /// (PK BDD)

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String apellido;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = true)
    private String password;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDate fechaCreacion;

    @Column(nullable = false)
    private boolean activo = true;



    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "imagen_id")
    private Imagen fotoPerfil;


    @ManyToMany()
    @JoinTable(
            name = "usuario_roles",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "rol_id")
    )
    @JsonIgnoreProperties("usuarios")
    private Set<Rol> roles = new HashSet<>();


    @ManyToMany(mappedBy = "empleados")
    @JsonIgnore
    private Set<Sucursal> sucursalesEmpleado = new HashSet<>();



    @PrePersist
    public void prePersist() {
        this.fechaCreacion = LocalDate.now();
        if (email != null) {
            email = email.toLowerCase();
        }
    }

    /// CONSTRUCTORES


    public Usuario(String nombre, String apellido, String email, String password) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.password = password;
    }

    ///  METODOS
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Usuario usuario)) return false;
        return id != null && id.equals(usuario.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

}



