-- V11: status CANCELADO para relatorios (spec 002).
--
-- O portal web permite ao professor abandonar explicitamente um rascunho que decidiu nao seguir
-- ("cancelar", contracts/portal-web-api.md). Modelamos isso como um novo status terminal em vez de
-- apagar a linha: preserva rastreabilidade, mantem a integridade das FKs com `aula` e faz o
-- rascunho cancelado deixar de ser oferecido para retomada (DetectarRascunhoPendenteUseCase) sem
-- nenhuma regra adicional.
ALTER TABLE relatorio_aula
    DROP CONSTRAINT ck_relatorio_aula_status;

ALTER TABLE relatorio_aula
    ADD CONSTRAINT ck_relatorio_aula_status
    CHECK (status IN ('RASCUNHO', 'PENDENTE_REVISAO', 'APROVADO', 'CANCELADO'));

ALTER TABLE relatorio_semestral
    DROP CONSTRAINT ck_relatorio_semestral_status;

ALTER TABLE relatorio_semestral
    ADD CONSTRAINT ck_relatorio_semestral_status
    CHECK (status IN ('PENDENTE_REVISAO', 'APROVADO', 'CANCELADO'));
