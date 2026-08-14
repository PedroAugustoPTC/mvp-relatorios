# Quickstart: Validação do MVP Sistema de Relatórios para Escola de Música

**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md) | **Data Model**: [data-model.md](./data-model.md) | **Contracts**: [contracts/](./contracts/)

Guia para subir o ambiente localmente e validar de ponta a ponta cada user story priorizada.

## Pré-requisitos

- Docker + Docker Compose instalados.
- Variáveis de ambiente configuradas (arquivo `.env`, não versionado): credenciais do PostgreSQL,
  token do bot do Telegram, chave/endpoint do provedor de Speech-to-Text, chave/endpoint do
  provedor de LLM, chave de criptografia de CPF, token de serviço `X-N8N-Service-Token`.
- Um bot do Telegram criado (via BotFather) apontando para o webhook do n8n local (ex.: via túnel
  ngrok/similar para desenvolvimento).

## Subindo o ambiente

```bash
docker compose -f infrastructure/docker-compose.yml up -d
```

Serviços esperados: `frontend` (Nginx, porta 80/443), `backend` (Spring Boot, porta 8080),
`postgres` (5432, não exposto publicamente), `n8n` (5678, protegido por autenticação), volume de
`storage` para os PDFs gerados.

Verificar saúde do backend: `GET http://localhost:8080/actuator/health` → `{"status":"UP"}`.

Importar os workflows versionados em `n8n/workflows/*.json` na instância local do n8n e ativá-los.

## Cenário 1 — Registrar aula por áudio e aprovar relatório (US1, P1)

Pré-condição: professor e aluno já cadastrados e vinculados (ver Cenário 2) ou carga inicial via
`POST /api/v1/professores` e `POST /api/v1/alunos`.

1. No Telegram, o professor (conta já vinculada) envia `/relatorio`.
2. Bot lista os alunos associados (`GET /internal/v1/professores/{id}/alunos`); professor escolhe
   um aluno.
3. Bot pede o áudio; professor envia um áudio narrando a aula.
4. n8n chama `POST /internal/v1/relatorios-aula/transcrever` e depois
   `POST /internal/v1/relatorios-aula/{aulaId}/estruturar`.
5. **Esperado**: bot apresenta o relatório estruturado (conteúdo, evolução, dificuldades, tarefas,
   observações); se `perguntasPendentes` não estiver vazio, bot pergunta ativamente antes de gerar
   o PDF (valida Acceptance Scenario 5 / FR-007).
6. **Esperado**: bot disponibiliza o link do PDF gerado (`pdfUrl`) para download e conferência antes
   de qualquer aprovação (valida Acceptance Scenario 2 / FR-008).
7. Professor responde pedindo uma alteração em linguagem natural (ex.: "mude a dificuldade para
   ritmo") → n8n chama `POST /internal/v1/relatorios-aula/{id}/revisar` → **Esperado**: nova versão
   e novo PDF, aprovação pedida novamente (valida Acceptance Scenario 4 / FR-009).
8. Professor aprova → n8n chama `POST /internal/v1/relatorios-aula/{id}/aprovar` → **Esperado**:
   status muda para `APROVADO` e o relatório passa a constar no histórico do aluno (valida
   Acceptance Scenario 3 / FR-010).

**Critério de sucesso da validação**: passos 1–8 concluídos em menos de 3 minutos de interação
ativa (SC-001); relatório visível em `GET /api/v1/alunos/{id}/historico`.

## Cenário 2 — Cadastrar professor/aluno e vincular Telegram (US2, P2)

1. Administrador autenticado (`POST /api/v1/auth/login`) cadastra um professor
   (`POST /api/v1/professores`) → **Esperado**: resposta inclui `codigoVinculacao`.
2. Professor abre o bot pela primeira vez e informa o código → n8n chama
   `POST /internal/v1/telegram/vinculacao` → **Esperado**: conta associada, professor liberado para
   `/relatorio` (valida Acceptance Scenario 2 / FR-002, FR-003; SC-006 em menos de 2 minutos).
3. Administrador cadastra um aluno associando o professor (`POST /api/v1/alunos`) →
   **Esperado**: aluno aparece em `GET /internal/v1/professores/{id}/alunos` (valida Acceptance
   Scenario 3).
4. Tentar usar `/relatorio` com uma conta do Telegram não vinculada → **Esperado**: bot solicita
   vinculação antes de prosseguir (valida Acceptance Scenario 4 / FR-003).

## Cenário 3 — Gerar relatório semestral consolidado (US3, P3)

Pré-condição: aluno com relatórios de aula aprovados em um período (ver Cenário 1, repetido
algumas vezes, ou carga inicial de dados).

1. Professor envia `/relatorio_semestral`, escolhe o aluno, e escolhe um semestre no menu
   apresentado (`GET /internal/v1/alunos/{id}/semestres-disponiveis`) — sem digitar datas
   (valida FR-011).
2. n8n chama `POST /internal/v1/relatorios-semestrais/contagem` → **Esperado**: bot informa quantos
   relatórios de aula foram encontrados antes de gerar (valida Acceptance Scenario 1 / FR-012).
3. Professor confirma → n8n chama `POST /internal/v1/relatorios-semestrais` → **Esperado**:
   relatório consolidado cobrindo todas as seções de FR-013, com PDF disponibilizado antes da
   aprovação (valida Acceptance Scenario 2 e 3 / FR-013, FR-013a).
4. Professor aprova → **Esperado**: relatório semestral salvo, distinto dos relatórios de aula
   individuais, visível no histórico (valida Acceptance Scenario 4 / FR-016).
5. Repetir o fluxo para um aluno sem relatórios no período → **Esperado**: sistema informa ausência
   de dados suficientes, sem gerar relatório vazio nem PDF (valida Acceptance Scenario 6 / FR-014).

**Critério de sucesso da validação**: passos 1–4 concluídos em menos de 5 minutos de interação
ativa para um histórico de até 30 aulas (SC-004).

## Cenário 4 — Consultar histórico na interface web e via Telegram (US4, P4)

1. Administrador acessa `GET /api/v1/alunos/{id}/historico` → **Esperado**: lista cronológica de
   relatórios de aula e semestrais aprovados (valida Acceptance Scenario 1 / FR-017).
2. Professor vinculado solicita pelo bot o histórico de um aluno associado a ele → n8n chama
   `GET /internal/v1/professores/{professorId}/alunos/{alunoId}/historico` → **Esperado**: lista
   cronológica apresentada na conversa (valida Acceptance Scenario 2 / FR-017a).
3. Professor solicita histórico de um aluno não associado a ele → **Esperado**: resposta 403 e bot
   nega o acesso (valida Acceptance Scenario 3 / FR-018; SC-007 verificável por teste de controle
   de acesso).

## Verificação de qualidade (antes de considerar a feature pronta)

- `mvn test` (ou equivalente) no backend com relatório de cobertura ≥90% linha/branch
  (Princípio III, non-negotiable) — falha o build abaixo do limite.
- `npm test` / `vitest run` no frontend para os componentes/páginas críticos.
- Revisão manual de que nenhum log do backend/n8n contém áudio bruto, transcrição completa ou CPF
  em texto claro (Princípio V).
- Confirmar que o áudio original é removido do storage temporário logo após
  `POST /internal/v1/relatorios-aula/transcrever` retornar sucesso (FR-005a).
