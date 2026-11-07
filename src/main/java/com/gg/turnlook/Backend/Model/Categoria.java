package com.gg.turnlook.Backend.Model;


import com.gg.turnlook.Backend.Enum.ECategoria;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "categorias")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Categoria {


    /// ATRIBUTOS
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    private ECategoria categoria;


    /// CONSTRUCTORES

    public Categoria(ECategoria categoria) {
        this.categoria = categoria;
    }


}

