package com.gg.turnlook.features.imagen;

import com.gg.turnlook.features.sucursal.Sucursal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;







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

    @Modifying
    @Transactional
    @Query(value = """
            DELETE FROM imagenes WHERE public_id = :publicId AND sucursal_id IS NULL
                AND NOT EXISTS (SELECT 1 FROM usuarios u WHERE u.imagen_id = imagenes.id)
                AND NOT EXISTS (SELECT 1 FROM sucursales s WHERE s.imagen_id = imagenes.id)
                AND NOT EXISTS (SELECT 1 FROM servicios v WHERE v.imagen_id = imagenes.id)
            """, nativeQuery = true)
    int eliminarHuerfanasPorPublicId(@Param("publicId") String publicId);

}


