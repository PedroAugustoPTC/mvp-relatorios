-- V2: Aluno
CREATE TABLE aluno (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome                  VARCHAR(255) NOT NULL,
    data_nascimento       DATE NOT NULL,
    cpf_criptografado     BYTEA NOT NULL,
    cpf_hash              VARCHAR(128) NOT NULL,
    nome_responsavel      VARCHAR(255),
    ativo                 BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em             TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_aluno_cpf_hash UNIQUE (cpf_hash)
);

CREATE INDEX idx_aluno_ativo ON aluno (ativo);
