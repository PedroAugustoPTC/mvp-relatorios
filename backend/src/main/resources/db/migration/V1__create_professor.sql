-- V1: Professor
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE professor (
    id                            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome                          VARCHAR(255) NOT NULL,
    email                         VARCHAR(255) NOT NULL,
    ativo                         BOOLEAN NOT NULL DEFAULT TRUE,
    codigo_vinculacao             VARCHAR(64),
    codigo_vinculacao_expira_em   TIMESTAMPTZ,
    criado_em                     TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em                 TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_professor_email UNIQUE (email),
    CONSTRAINT uq_professor_codigo_vinculacao UNIQUE (codigo_vinculacao)
);

CREATE INDEX idx_professor_ativo ON professor (ativo);
