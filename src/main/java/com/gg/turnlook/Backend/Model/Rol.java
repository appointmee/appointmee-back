package com.gg.turnlook.Backend.Model;

import com.gg.turnlook.Backend.Enum.ERol;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.minidev.json.annotate.JsonIgnore;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Rol {

    /// ATRIBUTOS
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    /// (PK BDD)

    @Enumerated(EnumType.STRING)
    private ERol rol;

    @ManyToMany(mappedBy = "roles")
    @JsonIgnore
    private Set<Usuario> usuarios = new HashSet<>();

    /// METODOS
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Rol)) return false;
        Rol rol = (Rol) o;
        return id != null && id.equals(rol.id);
    }



    public String getNombre(){
        return rol.name();
    }


    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
