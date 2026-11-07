package com.gg.turnlook.Backend.Model;



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


    @Column(nullable = true)
    private String url;


    @Column(name = "url_alt" ,nullable = false)
    private String urlAlt = "https://pimedelaar.org/wp-content/uploads/2023/05/no-image.png";
                                // borrar esto dsp cuando g pase la suya O dejar esta


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id")
    private Sucursal sucursal;



    /// METODOS

    public String getFotoValida(){
        return this.url != null ? url : urlAlt;
    }

}



