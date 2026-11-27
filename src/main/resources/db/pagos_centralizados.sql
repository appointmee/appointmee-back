-- SCRUM-349: modelo de pagos centralizados (opción C, alcance tesis).
-- AppointMee cobra a su propia cuenta y registra el adeudo a la sucursal.
-- spring.jpa.hibernate.ddl-auto=validate: este script debe ejecutarse antes de arrancar.

CREATE TABLE IF NOT EXISTS pagos (
    id                  SERIAL PRIMARY KEY,
    turno_id            INTEGER       NOT NULL REFERENCES turnos(id),
    sucursal_id         INTEGER       NOT NULL REFERENCES sucursales(id),
    external_reference  VARCHAR(100)  NOT NULL,
    provider            VARCHAR(30)   NOT NULL DEFAULT 'MERCADOPAGO',
    provider_payment_id VARCHAR(100),
    provider_status     VARCHAR(50),
    payment_state       VARCHAR(30)   NOT NULL DEFAULT 'CREATED',
    settlement_state    VARCHAR(30)   NOT NULL DEFAULT 'NOT_DUE',
    amount              NUMERIC(18,2) NOT NULL,
    currency            VARCHAR(3)    NOT NULL DEFAULT 'ARS',
    idempotency_key     VARCHAR(100)  NOT NULL,
    checkout_url        VARCHAR(1000),
    created_at          TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP,
    captured_at         TIMESTAMP,
    CONSTRAINT uk_pago_external_reference UNIQUE (external_reference),
    CONSTRAINT uk_pago_idempotency_key UNIQUE (idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_pago_turno ON pagos (turno_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_pago_sucursal ON pagos (sucursal_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_pago_estados ON pagos (payment_state, settlement_state);


CREATE TABLE IF NOT EXISTS payment_transactions (
    id                     BIGSERIAL PRIMARY KEY,
    pago_id                INTEGER       NOT NULL REFERENCES pagos(id) ON DELETE CASCADE,
    provider_transaction_id VARCHAR(100) NOT NULL,
    amount                 NUMERIC(18,2) NOT NULL,
    status                 VARCHAR(50),
    status_detail          VARCHAR(80),
    payment_method         VARCHAR(50),
    installments           INTEGER,
    CONSTRAINT uk_payment_transaction_proveedor
        UNIQUE (pago_id, provider_transaction_id)
);

CREATE INDEX IF NOT EXISTS idx_payment_transaction_pago
    ON payment_transactions (pago_id);


CREATE TABLE IF NOT EXISTS payment_webhook_events (
    id           BIGSERIAL PRIMARY KEY,
    provider     VARCHAR(30)  NOT NULL,
    event_id     VARCHAR(150) NOT NULL,
    topic        VARCHAR(80),
    action       VARCHAR(80),
    payload_hash VARCHAR(64),
    received_at  TIMESTAMP    NOT NULL DEFAULT now(),
    processed_at TIMESTAMP,
    attempts     INTEGER      NOT NULL DEFAULT 0,
    last_error   VARCHAR(500),
    CONSTRAINT uk_webhook_event_proveedor UNIQUE (provider, event_id)
);

CREATE INDEX IF NOT EXISTS idx_webhook_event_received
    ON payment_webhook_events (received_at);


CREATE TABLE IF NOT EXISTS payment_releases (
    id                BIGSERIAL PRIMARY KEY,
    pago_id           INTEGER       NOT NULL REFERENCES pagos(id) ON DELETE CASCADE,
    turno_id          INTEGER       NOT NULL REFERENCES turnos(id),
    amount            NUMERIC(18,2) NOT NULL,
    reason            VARCHAR(100),
    status            VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    idempotency_key   VARCHAR(100)  NOT NULL,
    provider_reference VARCHAR(100),
    requested_at      TIMESTAMP     NOT NULL DEFAULT now(),
    released_at       TIMESTAMP,
    CONSTRAINT uk_payment_release_idempotency UNIQUE (idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_payment_release_pago ON payment_releases (pago_id);
CREATE INDEX IF NOT EXISTS idx_payment_release_status ON payment_releases (status);


CREATE TABLE IF NOT EXISTS financial_operations (
    id                BIGSERIAL PRIMARY KEY,
    pago_id           INTEGER       NOT NULL REFERENCES pagos(id) ON DELETE CASCADE,
    type              VARCHAR(30)   NOT NULL,
    amount            NUMERIC(18,2) NOT NULL,
    from_state        VARCHAR(30),
    to_state          VARCHAR(30),
    provider_reference VARCHAR(100),
    occurred_at       TIMESTAMP     NOT NULL DEFAULT now(),
    prev_hash         VARCHAR(64),
    hash              VARCHAR(64),
    actor             VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_financial_operation_pago
    ON financial_operations (pago_id, id);
CREATE INDEX IF NOT EXISTS idx_financial_operation_occurred
    ON financial_operations (occurred_at);


CREATE TABLE IF NOT EXISTS payment_configurations (
    id              SERIAL PRIMARY KEY,
    sucursal_id     INTEGER      NOT NULL REFERENCES sucursales(id) ON DELETE CASCADE,
    provider_code   VARCHAR(30)  NOT NULL DEFAULT 'MERCADOPAGO',
    enabled_methods VARCHAR(500) NOT NULL DEFAULT 'credit_card,debit_card,account_money',
    environment     VARCHAR(20)  NOT NULL DEFAULT 'TEST',
    active          BOOLEAN      NOT NULL DEFAULT true,
    CONSTRAINT uk_payment_config_sucursal_proveedor
        UNIQUE (sucursal_id, provider_code)
);


-- SCRUM-353: resultados persistidos de las órdenes de pago simuladas.
CREATE TABLE IF NOT EXISTS payout_operations (
    id                BIGSERIAL PRIMARY KEY,
    reference         VARCHAR(100)  NOT NULL,
    status            VARCHAR(30)   NOT NULL,
    amount            NUMERIC(18,2) NOT NULL,
    sucursal_id       INTEGER       NOT NULL REFERENCES sucursales(id),
    idempotency_key   VARCHAR(100)  NOT NULL,
    provider_reference VARCHAR(100) NOT NULL,
    processed_at      TIMESTAMP     NOT NULL DEFAULT now(),
    reason            VARCHAR(100),
    CONSTRAINT uk_payout_operation_reference UNIQUE (reference),
    CONSTRAINT uk_payout_operation_idempotency UNIQUE (idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_payout_operation_sucursal
    ON payout_operations (sucursal_id, processed_at DESC);
