package com.gg.turnlook.Backend.Repository;



import com.gg.turnlook.Backend.Model.Imagen;
import com.gg.turnlook.Backend.Model.Sucursal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;



public interface ImagenRepository extends JpaRepository<Imagen, Integer> {

    List<Imagen> findBySucursal(Sucursal sucursal);

    long countBySucursal(Sucursal sucursal);

    @Query(value = """
            SELECT COUNT(*) FROM imagenes i WHERE i.public_id = :publicId AND (
                    i.sucursal_id IS NOT NULL
                    OR EXISTS (SELECT 1 FROM usuarios u WHERE u.imagen_id = i.id)
                    OR EXISTS (SELECT 1 FROM sucursales s WHERE s.imagen_id = i.id)
                    OR EXISTS (SELECT 1 FROM servicios v WHERE v.imagen_id = i.id)
                )
            """, nativeQuery = true)
    long contarEnUsoPorPublicId(@Param("publicId") String publicId);

}
