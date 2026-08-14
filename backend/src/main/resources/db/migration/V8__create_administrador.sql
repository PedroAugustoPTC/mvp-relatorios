-- V8: Administrador (conta de acesso a interface web administrativa, T058)
CREATE TABLE administrador (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email        VARCHAR(255) NOT NULL,
    senha_hash   VARCHAR(255) NOT NULL,
    criado_em    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_administrador_email UNIQUE (email)
);
