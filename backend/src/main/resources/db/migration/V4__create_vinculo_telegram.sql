-- V4: VinculoTelegram (1:1 com Professor)
CREATE TABLE vinculo_telegram (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    professor_id      UUID NOT NULL,
    telegram_user_id  VARCHAR(64) NOT NULL,
    vinculado_em      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_vinculo_telegram_professor FOREIGN KEY (professor_id) REFERENCES professor (id),
    CONSTRAINT uq_vinculo_telegram_professor_id UNIQUE (professor_id),
    CONSTRAINT uq_vinculo_telegram_telegram_user_id UNIQUE (telegram_user_id)
);
