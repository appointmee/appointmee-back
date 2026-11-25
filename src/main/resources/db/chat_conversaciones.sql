-- SCRUM-331: modelo de conversaciones y mensajes del chat cliente/empleado.
-- El empleado que atiende puede ser el empleador de la sucursal.
-- El esquema se mantiene con scripts manuales (spring.jpa.hibernate.ddl-auto=validate).

CREATE TABLE IF NOT EXISTS conversaciones (
    id                 SERIAL PRIMARY KEY,
    sucursal_id        INTEGER   NOT NULL REFERENCES sucursales(id),
    turno_id           INTEGER   REFERENCES turnos(id) ON DELETE SET NULL,
    cliente_id         INTEGER   NOT NULL REFERENCES usuarios(id),
    fecha_creacion     TIMESTAMP NOT NULL DEFAULT now(),
    ultimo_mensaje_id  BIGINT
);

-- Una conversación por turno.
CREATE UNIQUE INDEX IF NOT EXISTS uk_conversacion_turno
    ON conversaciones (turno_id)
    WHERE turno_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_conversacion_cliente
    ON conversaciones (cliente_id);


CREATE TABLE IF NOT EXISTS conversacion_participantes (
    id                       SERIAL PRIMARY KEY,
    conversacion_id          INTEGER     NOT NULL REFERENCES conversaciones(id) ON DELETE CASCADE,
    usuario_id               INTEGER     NOT NULL REFERENCES usuarios(id),
    fecha_alta               TIMESTAMP   NOT NULL DEFAULT now(),
    ultimo_mensaje_leido_id  BIGINT,
    CONSTRAINT uk_participante_conversacion_usuario
        UNIQUE (conversacion_id, usuario_id)
);

CREATE INDEX IF NOT EXISTS idx_participante_usuario
    ON conversacion_participantes (usuario_id);


CREATE TABLE IF NOT EXISTS mensajes (
    id                 BIGSERIAL PRIMARY KEY,
    conversacion_id    INTEGER       NOT NULL REFERENCES conversaciones(id) ON DELETE CASCADE,
    autor_id           INTEGER       NOT NULL REFERENCES usuarios(id),
    contenido          VARCHAR(2000) NOT NULL,
    fecha_creacion     TIMESTAMP     NOT NULL DEFAULT now(),
    estado_entrega     VARCHAR(20)   NOT NULL DEFAULT 'ENVIADO',
    cliente_mensaje_id UUID          NOT NULL,
    CONSTRAINT uk_mensaje_conversacion_cliente_mensaje
        UNIQUE (conversacion_id, cliente_mensaje_id)
);

-- El historial se ordena y pagina por id dentro de la conversación.
CREATE INDEX IF NOT EXISTS idx_mensaje_conversacion
    ON mensajes (conversacion_id, id);

CREATE INDEX IF NOT EXISTS idx_mensaje_autor
    ON mensajes (autor_id);
