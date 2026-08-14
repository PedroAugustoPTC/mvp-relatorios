-- V5: Aula
-- Multiplas aulas podem existir para o mesmo par (professor, aluno) na mesma data_aula
-- (cada uma gera seu proprio relatorio, sem deduplicacao) - portanto sem unique constraint.
CREATE TABLE aula (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    professor_id   UUID NOT NULL,
    aluno_id       UUID NOT NULL,
    data_aula      DATE NOT NULL,
    criado_em      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_aula_professor FOREIGN KEY (professor_id) REFERENCES professor (id),
    CONSTRAINT fk_aula_aluno FOREIGN KEY (aluno_id) REFERENCES aluno (id)
);

CREATE INDEX idx_aula_professor_id ON aula (professor_id);
CREATE INDEX idx_aula_aluno_id ON aula (aluno_id);
CREATE INDEX idx_aula_data_aula ON aula (data_aula);
