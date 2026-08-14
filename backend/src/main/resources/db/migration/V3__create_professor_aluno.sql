-- V3: ProfessorAluno (associacao N:N)
CREATE TABLE professor_aluno (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    professor_id   UUID NOT NULL,
    aluno_id       UUID NOT NULL,
    criado_em      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_professor_aluno_professor FOREIGN KEY (professor_id) REFERENCES professor (id),
    CONSTRAINT fk_professor_aluno_aluno FOREIGN KEY (aluno_id) REFERENCES aluno (id),
    CONSTRAINT uq_professor_aluno_pair UNIQUE (professor_id, aluno_id)
);

CREATE INDEX idx_professor_aluno_professor_id ON professor_aluno (professor_id);
CREATE INDEX idx_professor_aluno_aluno_id ON professor_aluno (aluno_id);
