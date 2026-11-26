-- SCRUM-326 / US-NOT-02 · notificaciones in-app por usuario.
-- El esquema se aplica a mano: spring.jpa.hibernate.ddl-auto=validate.

CREATE TABLE IF NOT EXISTS notificaciones (
    id             SERIAL      PRIMARY KEY,
    usuario_id     INTEGER     NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    tipo           VARCHAR(30) NOT NULL,
    titulo         VARCHAR(120) NOT NULL,
    mensaje        VARCHAR(500) NOT NULL,
    fecha_creacion TIMESTAMP   NOT NULL DEFAULT now(),
    leida          BOOLEAN     NOT NULL DEFAULT FALSE
);

-- Listado del usuario ordenado de la más nueva a la más vieja.
CREATE INDEX IF NOT EXISTS idx_notificaciones_usuario_fecha
    ON notificaciones (usuario_id, fecha_creacion DESC, id DESC);

-- Filtro por estado de lectura y contador de no leídas.
CREATE INDEX IF NOT EXISTS idx_notificaciones_usuario_leida
    ON notificaciones (usuario_id, leida);
