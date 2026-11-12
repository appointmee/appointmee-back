package com.gg.turnlook.config;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Compatibilidad con bases de datos que todavia conservan la columna legada
 * url_alt (NOT NULL) en la tabla imagenes. Cloudinary es el unico proveedor
 * de imagenes, por lo que dicha columna deja de usarse y se elimina.
 */
@Component
public class ImagenSchemaFix implements ApplicationRunner {

    private final DataSource dataSource;

    public ImagenSchemaFix(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE imagenes DROP COLUMN IF EXISTS url_alt");
        }
    }
}
