---

description: "Task list template for feature implementation"
---

# Tasks: MVP Sistema de Relatórios para Escola de Música

**Input**: Design documents from `/specs/001-mvp-relatorios-escola-musica/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md (all present)

**Tests**: Incluídas de forma seletiva (unit tests por use case/controller e testes de integração de
contrato), pois a constituição do projeto exige cobertura mínima de 90% linha/branch no backend
como gate não negociável (Princípio III) — não são testes exaustivos por método, mas suficientes
para atingir e defender esse gate.

**Organization**: Tasks são agrupadas por user story (US1–US4, conforme spec.md) para permitir
implementação e teste independentes de cada história.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Pode rodar em paralelo (arquivos diferentes, sem dependência de tasks incompletas)
- **[Story]**: A qual user story a task pertence (US1, US2, US3, US4)
- Caminhos de arquivo exatos incluídos em cada descrição

## Path Conventions

- **backend/**: `backend/src/main/java/com/escolademusica/relatorios/{domain,usecase,controller,service,gateway,repository,mapper,dto}` + `backend/src/test/java/...` (espelha a árvore de main)
- **frontend/**: `frontend/src/{components,pages,services}` + `frontend/tests/`
- **n8n/**: `n8n/workflows/*.json`
- **infrastructure/**: `infrastructure/docker-compose.yml`, `infrastructure/storage/`, `infrastructure/postgres/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Inicialização dos projetos backend, frontend, infraestrutura Docker e estrutura de workflows n8n.

- [X] T001 Create backend Spring Boot 3.x (Java 21) Maven project with dependencies Web, Validation, Data JPA, Security, Flyway, springdoc-openapi in backend/pom.xml
- [X] T002 [P] Create backend package skeleton (domain, usecase, controller, service, gateway, repository, mapper, dto) under backend/src/main/java/com/escolademusica/relatorios/
- [X] T003 [P] Create frontend Vite + React 18 + TypeScript project with React Router in frontend/
- [X] T004 [P] Configure backend code style (Checkstyle or Spotless plugin) in backend/pom.xml
- [X] T005 [P] Configure frontend ESLint + Prettier in frontend/.eslintrc.cjs, frontend/.prettierrc
- [X] T006 [P] Create infrastructure/docker-compose.yml with services frontend (Nginx), backend (Spring Boot), postgres, n8n, and a dedicated storage volume for PDFs
- [X] T007 [P] Create backend externalized configuration (env-var based: DB, Telegram, STT/LLM keys, CPF encryption key, X-N8N-Service-Token) in backend/src/main/resources/application.yml
- [X] T008 [P] Create n8n/workflows/ directory with README documenting the JSON versioning convention (RegistroAula.json, RelatorioSemestral.json, VinculacaoTelegram.json)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Schema de banco, entidades de domínio, repositórios, segurança e serviços/gateways
transversais que TODAS as user stories dependem.

**⚠️ CRITICAL**: Nenhuma user story pode ser implementada antes desta fase estar completa.

- [X] T009 Create Flyway migration V1__create_professor.sql (id, nome, email, ativo, codigoVinculacao, codigoVinculacaoExpiraEm, timestamps) in backend/src/main/resources/db/migration/
- [X] T010 [P] Create Flyway migration V2__create_aluno.sql (id, nome, dataNascimento, cpfCriptografado, cpfHash unique, nomeResponsavel, ativo, timestamps) in backend/src/main/resources/db/migration/
- [X] T011 [P] Create Flyway migration V3__create_professor_aluno.sql (professorId, alunoId, unique pair, criadoEm) in backend/src/main/resources/db/migration/
- [X] T012 [P] Create Flyway migration V4__create_vinculo_telegram.sql (id, professorId unique, telegramUserId unique, vinculadoEm) in backend/src/main/resources/db/migration/
- [X] T013 [P] Create Flyway migration V5__create_aula.sql (id, professorId, alunoId, dataAula, criadoEm) in backend/src/main/resources/db/migration/
- [X] T014 [P] Create Flyway migration V6__create_relatorio_aula.sql (id, aulaId unique, professorId, alunoId, transcricao, campos estruturados, versao, status enum, pdfUrl, timestamps, aprovadoEm) in backend/src/main/resources/db/migration/
- [X] T015 [P] Create Flyway migration V7__create_relatorio_semestral.sql (id, alunoId, professorId, periodoInicio, periodoFim, quantidadeRelatoriosAulaConsiderados, seções FR-013, versao, status, pdfUrl, timestamps) in backend/src/main/resources/db/migration/
- [X] T016 [P] Create Professor domain entity in backend/src/main/java/com/escolademusica/relatorios/domain/Professor.java
- [X] T017 [P] Create Aluno domain entity in backend/src/main/java/com/escolademusica/relatorios/domain/Aluno.java
- [X] T018 [P] Create ProfessorAluno domain entity in backend/src/main/java/com/escolademusica/relatorios/domain/ProfessorAluno.java
- [X] T019 [P] Create VinculoTelegram domain entity in backend/src/main/java/com/escolademusica/relatorios/domain/VinculoTelegram.java
- [X] T020 [P] Create Aula domain entity in backend/src/main/java/com/escolademusica/relatorios/domain/Aula.java
- [X] T021 [P] Create RelatorioAula domain entity with status enum (RASCUNHO, PENDENTE_REVISAO, APROVADO) in backend/src/main/java/com/escolademusica/relatorios/domain/RelatorioAula.java
- [X] T022 [P] Create RelatorioSemestral domain entity with status enum (PENDENTE_REVISAO, APROVADO) in backend/src/main/java/com/escolademusica/relatorios/domain/RelatorioSemestral.java
- [X] T023 [P] Define ProfessorRepository port + ProfessorRepositoryImpl (JPA) in backend/src/main/java/com/escolademusica/relatorios/repository/
- [X] T024 [P] Define AlunoRepository port + AlunoRepositoryImpl (JPA, busca por cpfHash) in backend/src/main/java/com/escolademusica/relatorios/repository/
- [X] T025 [P] Define ProfessorAlunoRepository port + ProfessorAlunoRepositoryImpl in backend/src/main/java/com/escolademusica/relatorios/repository/
- [X] T026 [P] Define VinculoTelegramRepository port + VinculoTelegramRepositoryImpl in backend/src/main/java/com/escolademusica/relatorios/repository/
- [X] T027 [P] Define AulaRepository port + AulaRepositoryImpl in backend/src/main/java/com/escolademusica/relatorios/repository/
- [X] T028 [P] Define RelatorioAulaRepository port + RelatorioAulaRepositoryImpl in backend/src/main/java/com/escolademusica/relatorios/repository/
- [X] T029 [P] Define RelatorioSemestralRepository port + RelatorioSemestralRepositoryImpl in backend/src/main/java/com/escolademusica/relatorios/repository/
- [X] T030 Implement CriptografiaService (AES-GCM CPF encryption + HMAC-SHA-256 deterministic cpfHash for uniqueness checks) in backend/src/main/java/com/escolademusica/relatorios/service/CriptografiaService.java
- [X] T031 Configure Spring Security: admin JWT authentication filter (for /api/v1/**) and N8N service-token filter validating X-N8N-Service-Token header (for /internal/v1/**) in backend/src/main/java/com/escolademusica/relatorios/config/SecurityConfig.java
- [X] T032 Implement global exception handler producing standard error DTO `{codigo, mensagem}` in backend/src/main/java/com/escolademusica/relatorios/controller/GlobalExceptionHandler.java
- [X] T033 [P] Define SpeechToTextGateway interface in backend/src/main/java/com/escolademusica/relatorios/gateway/SpeechToTextGateway.java
- [X] T034 [P] Define LlmGateway interface (texto → JSON estruturado por schema) in backend/src/main/java/com/escolademusica/relatorios/gateway/LlmGateway.java
- [X] T035 [P] Define TelegramGateway interface in backend/src/main/java/com/escolademusica/relatorios/gateway/TelegramGateway.java
- [X] T036 Implement PdfGeracaoService base (OpenPDF setup, writes to storage volume, returns pdfUrl) in backend/src/main/java/com/escolademusica/relatorios/service/PdfGeracaoService.java
- [X] T037 [P] Unit tests for CriptografiaService (encrypt/decrypt round-trip, hash determinism/uniqueness) in backend/src/test/java/com/escolademusica/relatorios/service/CriptografiaServiceTest.java

**Checkpoint**: Fundação pronta — implementação das user stories pode começar.

---

## Phase 3: User Story 1 - Registrar aula por áudio no Telegram e receber relatório (Priority: P1) 🎯 MVP

**Goal**: Professor envia áudio pelo Telegram, sistema transcreve, estrutura em relatório, gera PDF
de conferência, permite refinamento por linguagem natural e só persiste como aprovado após
confirmação explícita (FR-005 a FR-010, FR-021).

**Independent Test**: Com professor e aluno já cadastrados/vinculados (carga inicial), enviar
`/relatorio`, escolher aluno, enviar áudio de aula, e verificar que um relatório estruturado é
apresentado e, após aprovação, passa a constar no histórico do aluno.

### Implementation for User Story 1

- [X] T038 [P] [US1] Implement SpeechToTextGatewayImpl calling configurable transcription provider via env vars in backend/src/main/java/com/escolademusica/relatorios/gateway/SpeechToTextGatewayImpl.java
- [X] T039 [P] [US1] Implement LlmGatewayImpl with prompt/schema for relatório de aula structuring (JSON validated before persistence) in backend/src/main/java/com/escolademusica/relatorios/gateway/LlmGatewayImpl.java
- [X] T040 [US1] Implement TranscricaoService orchestrating SpeechToTextGateway call and immediate deletion of temporary audio on success (FR-005a) in backend/src/main/java/com/escolademusica/relatorios/service/TranscricaoService.java
- [X] T041 [US1] Implement RegistrarAulaUseCase (create Aula, validate alunoId associado ao professorId via ProfessorAluno FR-018, call TranscricaoService) in backend/src/main/java/com/escolademusica/relatorios/usecase/RegistrarAulaUseCase.java
- [X] T042 [US1] Implement EstruturarRelatorioUseCase (call LlmGateway, validate/repair JSON, detect perguntasPendentes FR-007, auto-generate PDF when no pending questions) in backend/src/main/java/com/escolademusica/relatorios/usecase/EstruturarRelatorioUseCase.java
- [X] T043 [US1] Implement ResponderPerguntaUseCase (apply professor's answer to a pending question, re-check completeness, generate PDF if complete) in backend/src/main/java/com/escolademusica/relatorios/usecase/ResponderPerguntaUseCase.java
- [X] T044 [US1] Implement RevisarRelatorioAulaUseCase (natural-language edit via LlmGateway, versao += 1, novo PDF, FR-009) in backend/src/main/java/com/escolademusica/relatorios/usecase/RevisarRelatorioAulaUseCase.java
- [X] T045 [US1] Implement AprovarRelatorioAulaUseCase (transition to APROVADO only with explicit confirmation on current PDF, set aprovadoEm, FR-008a/FR-010) in backend/src/main/java/com/escolademusica/relatorios/usecase/AprovarRelatorioAulaUseCase.java
- [X] T046 [US1] Extend PdfGeracaoService with simplified layout for RelatorioAula covering all FR-006 fields (FR-008) in backend/src/main/java/com/escolademusica/relatorios/service/PdfGeracaoService.java
- [X] T047 [US1] Implement RelatorioAulaController: POST /internal/v1/relatorios-aula/transcrever, POST /internal/v1/relatorios-aula/{aulaId}/estruturar, POST /internal/v1/relatorios-aula/{relatorioId}/responder-pergunta, POST /internal/v1/relatorios-aula/{relatorioId}/revisar, POST /internal/v1/relatorios-aula/{relatorioId}/aprovar in backend/src/main/java/com/escolademusica/relatorios/controller/RelatorioAulaController.java
- [X] T048 [US1] Implement GET /internal/v1/professores/{professorId}/alunos in backend/src/main/java/com/escolademusica/relatorios/controller/ProfessorInternalController.java (lista de seleção do bot, FR-004) — em classe separada de ProfessorController (US2) para evitar conflito de merge
- [X] T049 [US1] Add DTOs/Mappers for RelatorioAula flow (TranscreverRequestDto, EstruturarRelatorioResponseDto, RevisarRelatorioRequestDto, etc.) in backend/src/main/java/com/escolademusica/relatorios/dto/ and backend/src/main/java/com/escolademusica/relatorios/mapper/
- [X] T050 [US1] Handle 422 for áudio não processável (corrompido/formato inválido/silêncio) without creating a relatório (Edge Case) in TranscricaoService/RelatorioAulaController
- [X] T051 [P] [US1] Unit tests for RegistrarAulaUseCase, EstruturarRelatorioUseCase, ResponderPerguntaUseCase, RevisarRelatorioAulaUseCase, AprovarRelatorioAulaUseCase in backend/src/test/java/com/escolademusica/relatorios/usecase/
- [X] T052 [P] [US1] Unit tests for RelatorioAulaController (MockMvc) covering success, 422 e 409 in backend/src/test/java/com/escolademusica/relatorios/controller/RelatorioAulaControllerTest.java
- [X] T053 [US1] Create n8n workflow RegistroAula.json (Telegram Trigger → seleção de aluno → recebimento de áudio → transcrever → estruturar → perguntas pendentes → exibição do PDF → aprovar/revisar) in n8n/workflows/RegistroAula.json

**Checkpoint**: User Story 1 funcional e testável de ponta a ponta de forma independente.

---

## Phase 4: User Story 2 - Cadastrar professores, alunos e vincular conta do Telegram (Priority: P2)

**Goal**: Administrador cadastra professores/alunos na interface web; professor vincula sua conta
do Telegram usando um código de vinculação de uso único (FR-001, FR-001a, FR-002, FR-003).

**Independent Test**: Criar professor e aluno pela interface web, gerar código de vinculação, e
confirmar pelo Telegram que a conta foi associada corretamente — sem depender do fluxo de áudio.

### Implementation for User Story 2

- [X] T054 [P] [US2] Implement CadastrarProfessorUseCase (gera codigoVinculacao único com expiração, FR-002) in backend/src/main/java/com/escolademusica/relatorios/usecase/CadastrarProfessorUseCase.java
- [X] T055 [P] [US2] Implement CadastrarAlunoUseCase (valida CPF único via cpfHash FR-001a, exige nomeResponsavel quando menor, associa professores) in backend/src/main/java/com/escolademusica/relatorios/usecase/CadastrarAlunoUseCase.java
- [X] T056 [US2] Implement VincularContaTelegramUseCase (valida codigoVinculacao não expirado, cria VinculoTelegram, invalida código, FR-002/FR-003) in backend/src/main/java/com/escolademusica/relatorios/usecase/VincularContaTelegramUseCase.java
- [X] T057 [US2] Implement ReemitirCodigoVinculacaoUseCase in backend/src/main/java/com/escolademusica/relatorios/usecase/ReemitirCodigoVinculacaoUseCase.java
- [X] T058 [US2] Implement admin authentication (BCrypt + JWT curto prazo) in backend/src/main/java/com/escolademusica/relatorios/usecase/AutenticarAdminUseCase.java
- [X] T059 [US2] Implement ProfessorController: POST /api/v1/professores, GET /api/v1/professores, POST /api/v1/professores/{id}/codigo-vinculacao in backend/src/main/java/com/escolademusica/relatorios/controller/ProfessorController.java
- [X] T060 [US2] Implement AlunoController: POST /api/v1/alunos, GET /api/v1/alunos in backend/src/main/java/com/escolademusica/relatorios/controller/AlunoController.java
- [X] T061 [US2] Implement AuthController: POST /api/v1/auth/login in backend/src/main/java/com/escolademusica/relatorios/controller/AuthController.java
- [X] T062 [US2] Implement TelegramWebhookController: POST /internal/v1/telegram/vinculacao, GET /internal/v1/telegram/{telegramUserId}/professor in backend/src/main/java/com/escolademusica/relatorios/controller/TelegramWebhookController.java
- [X] T063 [US2] Add DTOs/Mappers for Professor/Aluno/Auth/Vinculação in backend/src/main/java/com/escolademusica/relatorios/dto/ and backend/src/main/java/com/escolademusica/relatorios/mapper/
- [X] T064 [US2] Enforce FR-020 (telegramUserId não é prova de autenticação suficiente para ações administrativas sensíveis) in SecurityConfig and ProfessorController/AlunoController
- [X] T065 [P] [US2] Unit tests for CadastrarProfessorUseCase, CadastrarAlunoUseCase, VincularContaTelegramUseCase, ReemitirCodigoVinculacaoUseCase in backend/src/test/java/com/escolademusica/relatorios/usecase/
- [X] T066 [P] [US2] Unit tests for ProfessorController, AlunoController, AuthController, TelegramWebhookController in backend/src/test/java/com/escolademusica/relatorios/controller/
- [X] T067 [US2] Create n8n workflow VinculacaoTelegram.json (primeiro contato → solicita código → chama POST /internal/v1/telegram/vinculacao) in n8n/workflows/VinculacaoTelegram.json
- [X] T068 [P] [US2] Frontend: Login page + authService (token storage, expiration) in frontend/src/pages/Login.tsx, frontend/src/services/authService.ts
- [X] T069 [P] [US2] Frontend: CadastroProfessor page (form, exibe codigoVinculacao gerado) + professorService in frontend/src/pages/CadastroProfessor.tsx, frontend/src/services/professorService.ts
- [X] T070 [P] [US2] Frontend: CadastroAluno page (form com CPF, dataNascimento, nomeResponsavel condicional, seleção de professores) + alunoService in frontend/src/pages/CadastroAluno.tsx, frontend/src/services/alunoService.ts
- [X] T071 [P] [US2] Frontend tests for Login, CadastroProfessor, CadastroAluno pages (Vitest + RTL) in frontend/tests/

**Checkpoint**: User Stories 1 e 2 funcionam de forma independente, com dados reais de cadastro.

---

## Phase 5: User Story 3 - Gerar relatório semestral consolidado (Priority: P3)

**Goal**: Professor solicita relatório semestral pelo Telegram; sistema consolida por IA os
relatórios de aula do período em um relatório pedagógico completo, gera PDF e exige aprovação
explícita (FR-011 a FR-016).

**Independent Test**: Com um aluno que já tenha relatórios de aula aprovados no histórico,
solicitar o relatório semestral pelo Telegram, informar aluno e período, e verificar que um
relatório consolidado é gerado e pode ser aprovado.

### Implementation for User Story 3

- [X] T072 [US3] Implement ListarSemestresDisponiveisUseCase (menu pré-definido de semestres a partir da data de cadastro do aluno, FR-011) in backend/src/main/java/com/escolademusica/relatorios/usecase/ListarSemestresDisponiveisUseCase.java
- [X] T073 [US3] Implement ContarRelatoriosPeriodoUseCase (FR-012) in backend/src/main/java/com/escolademusica/relatorios/usecase/ContarRelatoriosPeriodoUseCase.java
- [X] T074 [US3] Implement GerarRelatorioSemestralUseCase (exige quantidade > 0 FR-014, consolida via LlmGateway, gera PDF) in backend/src/main/java/com/escolademusica/relatorios/usecase/GerarRelatorioSemestralUseCase.java
- [X] T075 [US3] Implement RevisarRelatorioSemestralUseCase (alteração em linguagem natural, versao += 1, novo PDF, FR-015) in backend/src/main/java/com/escolademusica/relatorios/usecase/RevisarRelatorioSemestralUseCase.java
- [X] T076 [US3] Implement AprovarRelatorioSemestralUseCase (confirmação explícita, FR-015/FR-016) in backend/src/main/java/com/escolademusica/relatorios/usecase/AprovarRelatorioSemestralUseCase.java
- [X] T077 [US3] Extend LlmGatewayImpl with prompt/schema for consolidação semestral covering all FR-013 sections in backend/src/main/java/com/escolademusica/relatorios/gateway/LlmGatewayImpl.java
- [X] T078 [US3] Extend PdfGeracaoService with full semestral layout (todas as seções FR-013, cabeçalho/rodapé configuráveis pelos dados da escola) in backend/src/main/java/com/escolademusica/relatorios/service/PdfGeracaoService.java
- [X] T079 [US3] Implement RelatorioSemestralController: GET /internal/v1/alunos/{alunoId}/semestres-disponiveis, POST /internal/v1/relatorios-semestrais/contagem, POST /internal/v1/relatorios-semestrais, POST /internal/v1/relatorios-semestrais/{id}/revisar, POST /internal/v1/relatorios-semestrais/{id}/aprovar in backend/src/main/java/com/escolademusica/relatorios/controller/RelatorioSemestralController.java
- [X] T080 [US3] Add DTOs/Mappers for RelatorioSemestral flow in backend/src/main/java/com/escolademusica/relatorios/dto/ and backend/src/main/java/com/escolademusica/relatorios/mapper/
- [X] T081 [P] [US3] Unit tests for ListarSemestresDisponiveisUseCase, ContarRelatoriosPeriodoUseCase, GerarRelatorioSemestralUseCase (incl. caso zero relatórios FR-014), RevisarRelatorioSemestralUseCase, AprovarRelatorioSemestralUseCase in backend/src/test/java/com/escolademusica/relatorios/usecase/
- [X] T082 [P] [US3] Unit tests for RelatorioSemestralController in backend/src/test/java/com/escolademusica/relatorios/controller/RelatorioSemestralControllerTest.java
- [X] T083 [US3] Create n8n workflow RelatorioSemestral.json (comando /relatorio_semestral → seleção de aluno → menu de semestre → contagem → geração → revisão/aprovação) in n8n/workflows/RelatorioSemestral.json

**Checkpoint**: User Stories 1, 2 e 3 funcionam de forma independente.

---

## Phase 6: User Story 4 - Consultar histórico de relatórios na interface web (Priority: P4)

**Goal**: Administrador consulta histórico completo de qualquer aluno pela interface web; professor
consulta histórico dos seus próprios alunos exclusivamente pelo Telegram (FR-017, FR-017a, FR-018).

**Independent Test**: Acessar a interface web com um administrador autenticado e um aluno com
relatórios previamente cadastrados, verificando listagem/detalhe corretos; em paralelo, verificar
pelo Telegram que um professor consegue consultar (e é bloqueado de consultar histórico de aluno
não associado a ele).

### Implementation for User Story 4

- [X] T084 [US4] Implement ConsultarHistoricoUseCase (lista relatórios de aula + semestrais aprovados em ordem cronológica, FR-017/FR-017a) in backend/src/main/java/com/escolademusica/relatorios/usecase/ConsultarHistoricoUseCase.java
- [X] T085 [US4] Enforce controle de acesso: professor só consulta histórico de alunos associados a ele, retorno 403 caso contrário (FR-018) in ConsultarHistoricoUseCase
- [X] T086 [US4] Implement GET /api/v1/alunos/{id}/historico in backend/src/main/java/com/escolademusica/relatorios/controller/AlunoController.java
- [X] T087 [US4] Implement GET /internal/v1/professores/{professorId}/alunos/{alunoId}/historico in backend/src/main/java/com/escolademusica/relatorios/controller/TelegramWebhookController.java
- [X] T088 [P] [US4] Unit tests for ConsultarHistoricoUseCase incl. caso 403 (SC-007) in backend/src/test/java/com/escolademusica/relatorios/usecase/ConsultarHistoricoUseCaseTest.java
- [X] T089 [P] [US4] Unit tests for histórico endpoints (web admin e Telegram) in backend/src/test/java/com/escolademusica/relatorios/controller/
- [X] T090 [US4] Extend n8n workflow with comando de consulta de histórico via Telegram in n8n/workflows/HistoricoTelegram.json
- [X] T091 [P] [US4] Frontend: HistoricoAluno page (lista cronológica de relatórios de aula e semestrais, detalhe, link para PDF) in frontend/src/pages/HistoricoAluno.tsx
- [X] T092 [P] [US4] Frontend: historicoService HTTP client + PdfViewer component in frontend/src/services/historicoService.ts, frontend/src/components/PdfViewer.tsx
- [X] T093 [P] [US4] Frontend tests for HistoricoAluno page in frontend/tests/HistoricoAluno.test.tsx

**Checkpoint**: Todas as user stories (US1–US4) funcionam de forma independente.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Qualidade, observabilidade, documentação e validação final do MVP.

- [X] T094 [P] Configure springdoc-openapi and validate generated OpenAPI spec against contracts/api-web-admin.md and contracts/api-n8n-integration.md in backend/src/main/resources/
- [X] T095 [P] Configure JaCoCo coverage gate ≥90% line/branch, failing the build below threshold (Princípio III) in backend/pom.xml
- [X] T096 Review logs across TranscricaoService, LlmGatewayImpl, controllers to confirm no raw audio, full transcription or plain-text CPF is ever logged (Princípio V)
- [X] T097 [P] Verify actuator health endpoint and docker healthchecks in backend/src/main/resources/application.yml and infrastructure/docker-compose.yml
- [X] T098 [P] Document required environment variables (.env.example): DB credentials, Telegram bot token, STT/LLM provider keys, CPF encryption key, X-N8N-Service-Token in infrastructure/.env.example
- [X] T099 [P] Add navigation linking Login, CadastroProfessor, CadastroAluno, HistoricoAluno pages in frontend/src/App.tsx
- [X] T100 [P] Integration tests validating n8n webhook request/response contracts per contracts/api-n8n-integration.md in backend/src/test/java/com/escolademusica/relatorios/integration/
- [X] T101 Run quickstart.md Cenários 1–4 end-to-end manually against the docker compose environment and confirm SC-001/SC-004/SC-006/SC-007/SC-008 — **executado com Docker real**: `docker compose up --build postgres backend` subiu com sucesso; as 8 migrations Flyway aplicaram no Postgres real; `/actuator/health` respondeu `UP`. Durante essa validação foi encontrado e corrigido um **bug real de produção**: `N8nServiceTokenFilter` era um `@Component`, o que fazia o Spring Boot registrá-lo automaticamente como filtro global (além do uso explícito na cadeia `/internal/v1/**`), bloqueando com 401 toda requisição sem `X-N8N-Service-Token` — incluindo `/actuator/health`, login admin e toda a `/api/v1/**` (corrigido em `SecurityConfig.java` com um `FilterRegistrationBean` desabilitando o registro automático; também corrigido `lineEndings` do Spotless em `pom.xml` para `UNIX`, já que o build dentro do container Linux falhava em arquivos CRLF que passavam no host Windows). Após a correção, validado manualmente com dados reais (Postgres real, sem mocks): login admin → JWT → cadastro de professor (código de vinculação gerado) → cadastro de aluno (CPF criptografado, não exposto) → vinculação Telegram via código real (SC-006, concluída em segundos) → consulta de professor vinculado → histórico via web admin (200) → histórico via Telegram para professor não associado (**403 ALUNO_NAO_ASSOCIADO, confirma SC-007**). Não foi possível exercitar o fluxo completo de áudio→transcrição→PDF (SC-001/SC-004/SC-008) pois isso exige credenciais reais de um provedor de STT e de LLM, que não estão disponíveis neste ambiente; a lógica desses fluxos está coberta pelos 181 testes automatizados (incluindo os testes de contrato de integração T100).

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sem dependências — pode começar imediatamente.
- **Foundational (Phase 2)**: Depende da conclusão do Setup — BLOQUEIA todas as user stories.
- **User Stories (Phase 3-6)**: Todas dependem da conclusão da fase Foundational.
  - US1 (P1) pode começar assim que a Fundação estiver pronta.
  - US2 (P2) pode começar assim que a Fundação estiver pronta (não depende de US1 para ser implementada, embora US1 dependa de dados criados por US2 para teste real).
  - US3 (P3) depende de dados de US1 (relatórios de aula aprovados) para ser testada de ponta a ponta, mas seu código pode ser implementado assim que a Fundação estiver pronta.
  - US4 (P4) depende de dados de US1/US3 para ser testada, mas seu código pode ser implementado assim que a Fundação estiver pronta.
- **Polish (Phase 7)**: Depende de todas as user stories desejadas estarem completas.

### User Story Dependencies

- **US1 (P1)**: Nenhuma dependência de código de outra story; depende de dados de US2 para validação end-to-end real.
- **US2 (P2)**: Nenhuma dependência de outra story.
- **US3 (P3)**: Nenhuma dependência de código de US1/US2, mas consome relatórios de aula aprovados (gerados por US1) para ter dados a consolidar.
- **US4 (P4)**: Nenhuma dependência de código de outra story, mas consome relatórios gerados por US1/US3 para exibir histórico.

### Within Each User Story

- Gateways/serviços antes de use cases.
- Use cases antes de controllers.
- Controllers antes dos workflows n8n que os consomem.
- Testes unitários de use case/controller acompanham a implementação de cada task correspondente.

### Parallel Opportunities

- Todas as tasks [P] da Fase 1 podem rodar em paralelo.
- Todas as migrações Flyway [P] (T010–T015) podem rodar em paralelo após T009.
- Todas as entidades de domínio [P] (T016–T022) podem rodar em paralelo.
- Todos os repositórios [P] (T023–T029) podem rodar em paralelo.
- Após a Fundação (Phase 2), US1 e US2 podem ser desenvolvidas em paralelo por desenvolvedores diferentes; US3 e US4 podem começar em paralelo ao código de US1 (ambas dependem apenas de dados de teste, não do código).
- Dentro de cada story, tasks de frontend [P] são independentes das tasks de backend [P] equivalentes.

---

## Parallel Example: User Story 1

```bash
# Gateways da User Story 1 em paralelo:
Task: "Implement SpeechToTextGatewayImpl in backend/src/main/java/com/escolademusica/relatorios/gateway/SpeechToTextGatewayImpl.java"
Task: "Implement LlmGatewayImpl with prompt/schema for relatório de aula structuring in backend/src/main/java/com/escolademusica/relatorios/gateway/LlmGatewayImpl.java"

# Testes da User Story 1 em paralelo (após implementação):
Task: "Unit tests for RegistrarAulaUseCase, EstruturarRelatorioUseCase, ResponderPerguntaUseCase, RevisarRelatorioAulaUseCase, AprovarRelatorioAulaUseCase"
Task: "Unit tests for RelatorioAulaController (MockMvc)"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Completar Phase 1: Setup
2. Completar Phase 2: Foundational (CRÍTICO — bloqueia todas as stories)
3. Completar Phase 3: User Story 1 (com dados de teste cadastrados via carga inicial ou via um subconjunto mínimo de US2: CadastrarProfessorUseCase + CadastrarAlunoUseCase)
4. **PARAR e VALIDAR**: testar User Story 1 de ponta a ponta (Cenário 1 do quickstart.md)
5. Deploy/demo se pronto

### Incremental Delivery

1. Setup + Foundational → Fundação pronta
2. Adicionar US1 → Testar independentemente → Deploy/Demo (MVP!)
3. Adicionar US2 → Testar independentemente (cadastro completo + vinculação Telegram) → Deploy/Demo
4. Adicionar US3 → Testar independentemente (relatório semestral) → Deploy/Demo
5. Adicionar US4 → Testar independentemente (histórico web + Telegram) → Deploy/Demo
6. Cada story adiciona valor sem quebrar as anteriores

### Parallel Team Strategy

Com múltiplos desenvolvedores:

1. Time completa Setup + Foundational em conjunto.
2. Após a Fundação:
   - Dev A: User Story 1 (fluxo de áudio — maior complexidade, prioridade P1)
   - Dev B: User Story 2 (cadastro/vinculação — necessária para dados reais de teste)
   - Dev C: User Story 3 e depois User Story 4 (dependem de dados gerados por US1, mas o código pode avançar em paralelo)
3. Stories completam e se integram de forma independente; validação end-to-end real (quickstart.md) só é possível após US1 + US2 estarem prontas.
</content>
