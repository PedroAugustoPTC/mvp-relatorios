-- V9: canal de origem dos relatorios (spec 002, FR-002/FR-022a/FR-023).
--
-- Registra por qual canal cada relatorio foi criado (bot do Telegram ou portal web), permitindo
-- unificar o historico e detectar rascunhos pendentes cross-channel sem duplicar entidades.
--
-- O DEFAULT 'TELEGRAM' faz o backfill das linhas ja existentes (todas criadas pelo unico canal
-- disponivel antes desta feature) e mantem a coluna NOT NULL sem exigir uma migracao em duas
-- etapas. A aplicacao passa a informar o canal explicitamente em cada novo relatorio.
ALTER TABLE relatorio_aula
    ADD COLUMN canal_origem VARCHAR(16) NOT NULL DEFAULT 'TELEGRAM';

ALTER TABLE relatorio_semestral
    ADD COLUMN canal_origem VARCHAR(16) NOT NULL DEFAULT 'TELEGRAM';

ALTER TABLE relatorio_aula
    ADD CONSTRAINT ck_relatorio_aula_canal_origem CHECK (canal_origem IN ('TELEGRAM', 'WEB'));

ALTER TABLE relatorio_semestral
    ADD CONSTRAINT ck_relatorio_semestral_canal_origem CHECK (canal_origem IN ('TELEGRAM', 'WEB'));
