# Phase 1 Data Model: MVP Sistema de Relatórios para Escola de Música

**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md) | **Research**: [research.md](./research.md)

Entidades de domínio derivadas da seção "Key Entities" da spec e dos requisitos funcionais.
Nomenclatura de campos em português (domínio de negócio); tipos/classes em inglês no código,
conforme Princípio II da constituição.

## Professor

Pessoa que ministra aulas e usa o Telegram para registrar relatórios.

| Campo | Tipo | Regras |
|---|---|---|
| id | UUID | PK, gerado pelo backend |
| nome | string | obrigatório |
| email | string | obrigatório, único (login/contato administrativo) |
| ativo | boolean | default true |
| codigoVinculacao | string | único, uso único, gerado no cadastro (FR-002); pode ser nulo após uso ou reemitido |
| codigoVinculacaoExpiraEm | timestamp | expiração do código enquanto não usado |
| criadoEm / atualizadoEm | timestamp | auditoria |

**Relacionamentos**: 1 Professor → N Aluno (associação N:N via tabela de vínculo
`ProfessorAluno`); 1 Professor → 0..1 VinculoTelegram; 1 Professor → N RelatorioAula; 1 Professor →
N RelatorioSemestral.

**Regras de validação**: e-mail único; não pode haver dois professores com o mesmo
`codigoVinculacao` ativo simultaneamente (FR-002).

## Aluno

Pessoa (possivelmente menor de idade) que recebe aulas.

| Campo | Tipo | Regras |
|---|---|---|
| id | UUID | PK |
| nome | string | obrigatório |
| dataNascimento | date | obrigatório (determina se é menor de idade) |
| cpfCriptografado | bytes | obrigatório; CPF do aluno ou do responsável quando menor; criptografado em repouso (FR-001a) |
| cpfHash | string | HMAC determinístico do CPF normalizado, usado para checar duplicidade sem descriptografar (ver research.md §5) |
| nomeResponsavel | string | obrigatório quando `dataNascimento` indica menor de idade; nulo caso contrário |
| ativo | boolean | default true |
| criadoEm / atualizadoEm | timestamp | auditoria |

**Relacionamentos**: N Aluno ↔ N Professor (via `ProfessorAluno`); 1 Aluno → N Aula; 1 Aluno → N
RelatorioAula; 1 Aluno → N RelatorioSemestral.

**Regras de validação**: `cpfHash` único no sistema — impede cadastro duplicado (FR-001a);
`nomeResponsavel` obrigatório se `dataNascimento` implica idade < 18 anos na data de cadastro.

## ProfessorAluno (associação)

Vínculo de responsabilidade pedagógica entre professor e aluno, usado para controle de acesso
(FR-018) e para a lista de seleção do bot (FR-004, US2 cenário 3).

| Campo | Tipo | Regras |
|---|---|---|
| professorId | UUID | FK Professor |
| alunoId | UUID | FK Aluno |
| criadoEm | timestamp | auditoria |

**Regra de validação**: par (`professorId`, `alunoId`) único.

## VinculoTelegram

Associação entre o identificador de usuário do Telegram e o cadastro de um professor.

| Campo | Tipo | Regras |
|---|---|---|
| id | UUID | PK |
| professorId | UUID | FK Professor, único (1:1) |
| telegramUserId | string | único no sistema |
| vinculadoEm | timestamp | data da vinculação bem-sucedida |

**Regras de validação**: `telegramUserId` não pode estar associado a mais de um professor
simultaneamente; a vinculação só é criada mediante `codigoVinculacao` válido e não expirado
(FR-002, FR-003).

**Nota de segurança**: este registro é apenas identificador de integração; nunca é tratado como
prova de autenticação para ações administrativas sensíveis (FR-020).

## Aula

Evento pontual de ensino entre um professor e um aluno.

| Campo | Tipo | Regras |
|---|---|---|
| id | UUID | PK |
| professorId | UUID | FK Professor |
| alunoId | UUID | FK Aluno; deve constar em `ProfessorAluno` para esse professor (FR-018) |
| dataAula | date | obrigatório |
| criadoEm | timestamp | auditoria |

**Relacionamentos**: 1 Aula → 1 RelatorioAula (associada e única, conforme Key Entities da spec).

**Regra de negócio**: múltiplas Aulas podem existir para o mesmo par (professor, aluno) na mesma
`dataAula` — cada uma gera seu próprio relatório, sem deduplicação (Edge Case da spec).

## RelatorioAula

Registro estruturado gerado a partir do áudio de uma aula.

| Campo | Tipo | Regras |
|---|---|---|
| id | UUID | PK |
| aulaId | UUID | FK Aula, único |
| professorId | UUID | FK Professor (denormalizado para consulta/controle de acesso) |
| alunoId | UUID | FK Aluno (denormalizado para consulta/controle de acesso) |
| transcricao | text | texto resultante da transcrição do áudio (FR-005); mantido mesmo após o áudio ser excluído (FR-005a) |
| conteudosTrabalhados | text/JSON | lista estruturada (FR-006) |
| evolucao | text | (FR-006) |
| dificuldades | text/JSON | lista estruturada (FR-006) |
| atividadesPropostas | text/JSON | lista estruturada (FR-006) |
| observacoes | text | (FR-006) |
| versao | integer | incrementada a cada solicitação de alteração (FR-009); inicia em 1 |
| status | enum | `RASCUNHO`, `PENDENTE_REVISAO`, `APROVADO` (FR-021) |
| pdfUrl | string | caminho/URL do PDF gerado para a versão atual (FR-008) |
| criadoEm / atualizadoEm | timestamp | auditoria |
| aprovadoEm | timestamp | nulo até aprovação explícita (FR-008a) |

**Transições de estado** (ver quickstart.md para o fluxo completo):

```text
RASCUNHO → PENDENTE_REVISAO (PDF gerado e disponibilizado)
PENDENTE_REVISAO → PENDENTE_REVISAO (professor solicita alteração; versao += 1; novo PDF)
PENDENTE_REVISAO → APROVADO (professor confirma aderência ao PDF)
PENDENTE_REVISAO → (cancelado / não persistido como aprovado, permanece pendente ou é descartado)
```

**Regras de validação**: não pode transicionar para `APROVADO` sem confirmação explícita do
professor sobre o PDF vigente (FR-008a); `alunoId` deve estar associado ao `professorId` via
`ProfessorAluno` (FR-018).

## RelatorioSemestral

Consolidação de múltiplos relatórios de aula de um aluno em um período (semestre).

| Campo | Tipo | Regras |
|---|---|---|
| id | UUID | PK |
| alunoId | UUID | FK Aluno |
| professorId | UUID | FK Professor que solicitou/aprovou |
| periodoInicio | date | início do semestre selecionado no menu pré-definido (FR-011) |
| periodoFim | date | fim do semestre selecionado |
| quantidadeRelatoriosAulaConsiderados | integer | informado ao professor antes da geração (FR-012) |
| informacoesGerais | text/JSON | seção do modelo (FR-013) |
| frequenciaEEstudo | text/JSON | seção do modelo |
| tecnica | text/JSON | seção do modelo |
| musicalidade | text/JSON | seção do modelo |
| leituraEMemorizacao | text/JSON | seção do modelo |
| pontosDeAtencao | text/JSON | seção do modelo |
| estrategiasPedagogicas | text/JSON | seção do modelo |
| acompanhamentoFamiliar | text/JSON | seção do modelo |
| planejamentoProximoSemestre | text/JSON | seção do modelo |
| parecerFinal | text | seção do modelo |
| versao | integer | incrementada a cada alteração solicitada (FR-015) |
| status | enum | `PENDENTE_REVISAO`, `APROVADO` (FR-021) |
| pdfUrl | string | caminho/URL do PDF gerado para a versão atual (FR-013a) |
| criadoEm / atualizadoEm / aprovadoEm | timestamp | auditoria |

**Relacionamentos**: N RelatorioAula (via `aulaId`/`dataAula` dentro de `periodoInicio`..
`periodoFim`) contribuem para 1 RelatorioSemestral — relação de leitura/consolidação, não FK direta
obrigatória (os relatórios de aula continuam existindo independentemente).

**Regras de validação**: não é criado (nem seu PDF) quando `quantidadeRelatoriosAulaConsiderados`
for zero no período (FR-014); não transiciona para `APROVADO` sem confirmação explícita sobre o
PDF vigente (FR-015).

## Resumo de Status (para rastreabilidade — FR-021)

Ambos `RelatorioAula` e `RelatorioSemestral` compartilham o mesmo vocabulário de status
(`PENDENTE_REVISAO`, `APROVADO`; `RelatorioAula` adicionalmente começa em `RASCUNHO` enquanto
aguarda a primeira estruturação), permitindo consulta unificada de histórico por aluno (FR-017,
FR-017a) ordenada cronologicamente por `dataAula`/`periodoInicio`.
