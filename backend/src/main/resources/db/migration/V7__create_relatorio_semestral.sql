-- V7: RelatorioSemestral
CREATE TABLE relatorio_semestral (
    id                                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aluno_id                                    UUID NOT NULL,
    professor_id                                UUID NOT NULL,
    periodo_inicio                              DATE NOT NULL,
    periodo_fim                                 DATE NOT NULL,
    quantidade_relatorios_aula_considerados     INTEGER NOT NULL,
    informacoes_gerais                          JSONB,
    frequencia_e_estudo                         JSONB,
    tecnica                                     JSONB,
    musicalidade                                JSONB,
    leitura_e_memorizacao                       JSONB,
    pontos_de_atencao                           JSONB,
    estrategias_pedagogicas                     JSONB,
    acompanhamento_familiar                     JSONB,
    planejamento_proximo_semestre               JSONB,
    parecer_final                               TEXT,
    versao                                      INTEGER NOT NULL DEFAULT 1,
    status                                       VARCHAR(32) NOT NULL DEFAULT 'PENDENTE_REVISAO',
    pdf_url                                      VARCHAR(1024),
    criado_em                                    TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em                                TIMESTAMPTZ NOT NULL DEFAULT now(),
    aprovado_em                                  TIMESTAMPTZ,
    CONSTRAINT fk_relatorio_semestral_aluno FOREIGN KEY (aluno_id) REFERENCES aluno (id),
    CONSTRAINT fk_relatorio_semestral_professor FOREIGN KEY (professor_id) REFERENCES professor (id),
    CONSTRAINT ck_relatorio_semestral_status CHECK (status IN ('PENDENTE_REVISAO', 'APROVADO'))
);

CREATE INDEX idx_relatorio_semestral_aluno_id ON relatorio_semestral (aluno_id);
CREATE INDEX idx_relatorio_semestral_professor_id ON relatorio_semestral (professor_id);
CREATE INDEX idx_relatorio_semestral_status ON relatorio_semestral (status);
CREATE INDEX idx_relatorio_semestral_periodo ON relatorio_semestral (periodo_inicio, periodo_fim);
