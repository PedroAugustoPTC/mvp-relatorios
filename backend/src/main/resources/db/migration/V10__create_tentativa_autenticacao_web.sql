-- V10: TentativaAutenticacaoWeb (spec 002, FR-004a).
--
-- Rastreia tentativas de autenticacao por codigo de vinculacao no portal web para aplicar o rate
-- limit (bloqueio temporario apos N tentativas incorretas consecutivas).
--
-- A chave e o `identificador` (hash da origem da requisicao — IP/dispositivo), NAO o professor:
-- o limite precisa ser aplicado ANTES de validar o codigo, quando ainda nao se sabe (e pode nem
-- existir) a qual professor a tentativa se refere.
CREATE TABLE tentativa_autenticacao_web (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identificador          VARCHAR(128) NOT NULL,
    tentativas_incorretas  INTEGER NOT NULL DEFAULT 0,
    bloqueado_ate          TIMESTAMPTZ,
    atualizado_em          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_tentativa_autenticacao_web_identificador UNIQUE (identificador)
);

CREATE INDEX idx_tentativa_autenticacao_web_bloqueado_ate
    ON tentativa_autenticacao_web (bloqueado_ate);
