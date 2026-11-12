package com.gg.turnlook.features.imagen;

import com.gg.turnlook.features.sucursal.Sucursal;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;






@Entity
@Table(name = "imagenes")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Imagen {



    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;


    @Column(nullable = true, length = 1000)
    private String url;

    @Column(name = "public_id", length = 255)
    private String publicId;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id")
    private Sucursal sucursal;



    /// METODOS

    // Cloudinary es el unico proveedor de imagenes. Si no hay URL (por ejemplo,
    // una foto de perfil opcional), se devuelve null y la capa de presentacion
    // se encarga de mostrar el placeholder correspondiente.
    public String getFotoValida(){
        return this.url;
    }

}



