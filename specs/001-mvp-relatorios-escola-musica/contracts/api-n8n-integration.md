# Contract: API interna — Integração n8n ↔ Spring Boot

Endpoints internos chamados pelos workflows de n8n (Telegram Trigger → n8n → Spring Boot). Não são
expostos publicamente sem autenticação (Princípio V); autenticação via token de serviço
(header `X-N8N-Service-Token`), distinto da autenticação de administrador. O n8n nunca escreve
diretamente no PostgreSQL (Princípio IV) — todas as escritas passam por estes endpoints.

## Vinculação Telegram ↔ Professor

### POST /internal/v1/telegram/vinculacao

**Request**:
```json
{ "telegramUserId": "123456789", "codigoVinculacao": "AB12CD" }
```

**Response 200**: `{ "professorId": "uuid", "nomeProfessor": "Maria Silva" }` (FR-002, FR-003).

**Response 404**: código inválido/expirado.

### GET /internal/v1/telegram/{telegramUserId}/professor

Usado pelo n8n a cada mensagem para checar se o usuário já está vinculado (FR-003).

**Response 200**: `{ "professorId": "uuid", "nomeProfessor": "..." }`

**Response 404**: ainda não vinculado → n8n deve solicitar o código de vinculação.

---

## Seleção de aluno

### GET /internal/v1/professores/{professorId}/alunos

Lista de alunos associados ao professor, para o menu de seleção do bot (FR-004).

**Response 200**: `[{ "id", "nome" }]`

---

## Registro de aula (US1)

### POST /internal/v1/relatorios-aula/transcrever

Aciona o `SpeechToTextGateway` a partir de um áudio recebido no Telegram e retorna a transcrição
(FR-005). O backend exclui o arquivo de áudio temporário imediatamente após a transcrição ser
concluída com sucesso (FR-005a).

**Request**: multipart/form-data com `alunoId`, `professorId`, `dataAula`, `audio` (arquivo).

**Response 200**:
```json
{ "aulaId": "uuid", "transcricao": "Hoje trabalhei com a Maria..." }
```

**Response 422**: áudio não processável (corrompido/formato inválido/silêncio) — sem criar
relatório (Edge Case da spec).

### POST /internal/v1/relatorios-aula/{aulaId}/estruturar

Aciona o `LlmGateway` para transformar a transcrição em relatório estruturado (FR-006). Retorna
também a lista de perguntas de acompanhamento quando informação relevante não puder ser extraída
(FR-007).

**Response 200**:
```json
{
  "relatorioId": "uuid",
  "status": "PENDENTE_REVISAO",
  "conteudosTrabalhados": ["Música X"],
  "evolucao": "Melhora na mão direita",
  "dificuldades": ["Ritmo na segunda parte"],
  "atividadesPropostas": [],
  "observacoes": "",
  "perguntasPendentes": ["Deseja registrar alguma tarefa para casa?"],
  "pdfUrl": null
}
```

Quando `perguntasPendentes` está vazio, o backend já gera o PDF e retorna `pdfUrl` preenchido.

### POST /internal/v1/relatorios-aula/{relatorioId}/responder-pergunta

Envia a resposta do professor a uma pergunta de acompanhamento; pode gerar o PDF se não houver mais
pendências.

**Request**: `{ "resposta": "Sim, praticar o exercício de ritmo." }`

**Response 200**: mesmo shape do endpoint de estruturação, atualizado.

### POST /internal/v1/relatorios-aula/{relatorioId}/revisar

Aplica uma solicitação de alteração em linguagem natural, gera nova versão e novo PDF (FR-009).

**Request**: `{ "instrucao": "Mude a dificuldade para ritmo" }`

**Response 200**: relatório atualizado (versao incrementada, novo `pdfUrl`).

### POST /internal/v1/relatorios-aula/{relatorioId}/aprovar

Confirma aprovação explícita do professor sobre o PDF vigente (FR-008a). Só então o relatório é
persistido como `APROVADO` (FR-010).

**Response 200**: `{ "relatorioId", "status": "APROVADO", "aprovadoEm": "..." }`

**Response 409**: relatório já aprovado ou PDF desatualizado (nova versão gerada após o pedido).

---

## Relatório semestral (US3)

### GET /internal/v1/alunos/{alunoId}/semestres-disponiveis

Lista o menu pré-definido de semestres selecionáveis (FR-011).

**Response 200**: `[{ "chave": "2026-1", "rotulo": "1º semestre 2026", "inicio": "2026-01-01", "fim": "2026-06-30" }]`

### POST /internal/v1/relatorios-semestrais/contagem

Informa quantos relatórios de aula existem no período antes de gerar (FR-012).

**Request**: `{ "alunoId": "uuid", "periodoChave": "2026-1" }`

**Response 200**: `{ "quantidadeRelatoriosAula": 24 }`

**Response 200 (zero)**: `{ "quantidadeRelatoriosAula": 0 }` — n8n deve informar ausência de dados
e não prosseguir (FR-014).

### POST /internal/v1/relatorios-semestrais

Gera a consolidação via `LlmGateway` e o PDF (FR-013, FR-013a). Requer `quantidadeRelatoriosAula > 0`.

**Request**: `{ "alunoId": "uuid", "professorId": "uuid", "periodoChave": "2026-1" }`

**Response 201**: relatório semestral estruturado + `pdfUrl`, `status: "PENDENTE_REVISAO"`.

**Response 422**: período sem relatórios de aula (defesa em profundidade de FR-014).

### POST /internal/v1/relatorios-semestrais/{id}/revisar

Alteração em linguagem natural, nova versão, novo PDF (FR-015 — mesmo padrão de `.../revisar`
acima).

### POST /internal/v1/relatorios-semestrais/{id}/aprovar

Confirma aprovação explícita (FR-015, FR-016).

---

## Histórico via Telegram (US4)

### GET /internal/v1/professores/{professorId}/alunos/{alunoId}/historico

Histórico de relatórios aprovados de um aluno associado ao professor solicitante (FR-017a).

**Response 200**: mesmo shape de `GET /api/v1/alunos/{id}/historico`.

**Response 403**: aluno não associado a esse professor (FR-018, US4 cenário 3) — n8n deve negar o
acesso e informar o professor.

---

## Convenção de erros

Todas as respostas de erro seguem `{ "codigo": "...", "mensagem": "..." }`, permitindo que o n8n
traduza `codigo` em mensagens amigáveis ao professor (ex.: `AUDIO_NAO_PROCESSAVEL`,
`ALUNO_NAO_ASSOCIADO`, `PERIODO_SEM_RELATORIOS`) sem acoplar texto de negócio ao workflow.
