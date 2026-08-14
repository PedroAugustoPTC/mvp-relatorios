-- V6: RelatorioAula
CREATE TABLE relatorio_aula (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aula_id                  UUID NOT NULL,
    professor_id             UUID NOT NULL,
    aluno_id                 UUID NOT NULL,
    transcricao              TEXT,
    conteudos_trabalhados    JSONB,
    evolucao                 TEXT,
    dificuldades             JSONB,
    atividades_propostas     JSONB,
    observacoes              TEXT,
    versao                   INTEGER NOT NULL DEFAULT 1,
    status                   VARCHAR(32) NOT NULL DEFAULT 'RASCUNHO',
    pdf_url                  VARCHAR(1024),
    criado_em                TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em             TIMESTAMPTZ NOT NULL DEFAULT now(),
    aprovado_em               TIMESTAMPTZ,
    CONSTRAINT fk_relatorio_aula_aula FOREIGN KEY (aula_id) REFERENCES aula (id),
    CONSTRAINT fk_relatorio_aula_professor FOREIGN KEY (professor_id) REFERENCES professor (id),
    CONSTRAINT fk_relatorio_aula_aluno FOREIGN KEY (aluno_id) REFERENCES aluno (id),
    CONSTRAINT uq_relatorio_aula_aula_id UNIQUE (aula_id),
    CONSTRAINT ck_relatorio_aula_status CHECK (status IN ('RASCUNHO', 'PENDENTE_REVISAO', 'APROVADO'))
);

CREATE INDEX idx_relatorio_aula_professor_id ON relatorio_aula (professor_id);
CREATE INDEX idx_relatorio_aula_aluno_id ON relatorio_aula (aluno_id);
CREATE INDEX idx_relatorio_aula_status ON relatorio_aula (status);
