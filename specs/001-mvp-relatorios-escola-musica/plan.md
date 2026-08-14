# Implementation Plan: MVP Sistema de Relatórios para Escola de Música

**Branch**: `001-mvp-relatorios-escola-musica` | **Date**: 2026-08-13 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-mvp-relatorios-escola-musica/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

Substituir o registro manual de relatórios de aula por um fluxo assistido por voz: o professor
envia um áudio pelo bot do Telegram, o n8n orquestra a transcrição (Speech-to-Text) e a
estruturação por LLM, e o Spring Boot valida, persiste e governa a autorização, gerando um PDF de
conferência antes de qualquer aprovação. O mesmo padrão (consolidação por IA + PDF + aprovação
explícita) é reutilizado para o relatório semestral. React + TypeScript fornece a interface web
administrativa (cadastro de professores/alunos, vinculação Telegram, histórico), enquanto
PostgreSQL é a única fonte oficial de dados. Toda a infraestrutura roda via Docker Compose.

## Technical Context

**Language/Version**: Java 21 (Spring Boot 3.x) para o backend; TypeScript 5.x (React 18+) para o
frontend; n8n (workflows JSON, sem código de negócio customizado além de pequenas expressões de
orquestração).

**Primary Dependencies**: Spring Boot (Web, Validation, Data JPA, Security), Flyway (migrações),
springdoc-openapi (contratos REST); React + Vite, React Router, cliente HTTP (fetch/axios); n8n
(self-hosted) com nós de Telegram Trigger/Telegram, HTTP Request, e chamadas a um provedor de
Speech-to-Text e a um provedor de LLM (tratados como integrações substituíveis via Gateway no
backend, conforme Princípio I/IV da constituição); biblioteca de geração de PDF no backend (ex.:
OpenPDF/iText ou equivalente compatível com licença open-source) para materializar os relatórios
aprováveis.

**Storage**: PostgreSQL 16 como fonte oficial de dados estruturados (professores, alunos, aulas,
relatórios, vínculos Telegram, PDFs referenciados por caminho/URL em storage de arquivos). Volume
Docker dedicado para os PDFs gerados; nenhum áudio bruto é retido (FR-005a).

**Testing**: JUnit 5 + Mockito (+ Spring Boot Test / MockMvc) no backend, com meta de cobertura
≥90% por linha e branch (Princípio III, non-negotiable); Vitest/React Testing Library no frontend
para componentes e fluxos críticos da interface administrativa; testes de integração leves para os
workflows de n8n (validação de contrato de entrada/saída dos webhooks) que não substituem os
unitários do backend.

**Target Platform**: Contêineres Linux via Docker Compose (frontend Nginx, backend Spring Boot,
PostgreSQL, n8n, storage de arquivos), hospedados em um único ambiente (single-tenant).

**Project Type**: Web application (frontend + backend) com orquestração externa (n8n) e canal de
entrada via bot do Telegram — variação do template "web" com um componente adicional de
orquestração de integrações.

**Performance Goals**: Fluxo de registro de aula (áudio → relatório estruturado → PDF) concluído em
menos de 3 minutos de interação ativa do professor (SC-001); fluxo de relatório semestral (até 30
aulas no histórico) concluído em menos de 5 minutos de interação ativa (SC-004). Sem metas de
throughput elevado no MVP (uso interno de uma única escola).

**Constraints**: Sem retenção do áudio original além da transcrição bem-sucedida (FR-005a); CPF do
aluno/responsável armazenado de forma criptografada (FR-001a); Telegram User ID nunca é tratado
como autenticação suficiente para ações administrativas sensíveis (FR-020); toda comunicação
externa via HTTPS; segredos apenas em variáveis de ambiente/secret manager (Princípio V); n8n e
PostgreSQL não expostos publicamente sem autenticação.

**Scale/Scope**: MVP single-tenant (uma única escola de música); 4 user stories (P1–P4); histórico
de até dezenas de alunos e centenas de relatórios de aula no horizonte inicial; relatório semestral
processa até ~30 aulas por execução (SC-004).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Avaliação contra `.specify/memory/constitution.md` (v1.0.0):

| Princípio | Status | Observação |
|---|---|---|
| I. Clean Architecture (Non-Negotiable) | ✅ PASS | Backend Spring Boot estruturado em Domain → UseCase → Controller, com Service/Repository/Gateway atrás de interfaces (ver Project Structure). |
| II. Padrão de Nomenclatura Obrigatório | ✅ PASS | Convenções `*Controller/*UseCase/*Service/*Repository/*Gateway/*Mapper/*Dto` adotadas nos artefatos de design (data-model.md, contracts/). |
| III. Cobertura Mínima 90% (Non-Negotiable) | ⚠ A VERIFICAR NA IMPLEMENTAÇÃO | Gate de CI a ser configurado nas tasks; não é violado pelo plano, mas depende da fase de implementação para ser cumprido. |
| IV. Separação n8n / Backend / Banco de Dados | ✅ PASS | n8n limitado a orquestração (Telegram, Speech-to-Text, LLM, refinamento conversacional); Spring Boot é o único ponto de escrita no PostgreSQL; estado de conversa tem registro definitivo no backend. |
| V. Segurança e Privacidade (LGPD) | ✅ PASS | CPF criptografado, exclusão do áudio pós-transcrição, HTTPS, segredos via env vars, controle de acesso por professor — todos capturados em Technical Context/Constraints. |
| VI. Simplicidade e Foco no MVP | ✅ PASS | Sem automação de geração semestral agendada, sem WhatsApp/financeiro/app mobile; seleção de aluno antes do áudio conforme documento de referência. |

Nenhuma violação que exija entrada em Complexity Tracking. Gate satisfeito para prosseguir à Fase 0.

## Project Structure

### Documentation (this feature)

```text
specs/001-mvp-relatorios-escola-musica/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
├── checklists/
│   └── requirements.md
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/escolademusica/relatorios/
│   ├── domain/              # Entidades e regras puras: Aluno, Professor, Aula,
│   │                         # RelatorioAula, RelatorioSemestral, VinculoTelegram
│   ├── usecase/              # Um caso de uso por classe (ex.: RegistrarAulaUseCase,
│   │                         # AprovarRelatorioUseCase, GerarRelatorioSemestralUseCase,
│   │                         # VincularContaTelegramUseCase, CadastrarProfessorUseCase,
│   │                         # CadastrarAlunoUseCase, ConsultarHistoricoUseCase)
│   ├── controller/           # REST (interface web admin) e Webhook (n8n):
│   │                         # ProfessorController, AlunoController, RelatorioController,
│   │                         # TelegramWebhookController
│   ├── service/              # TranscricaoService, LlmService, PdfGeracaoService,
│   │                         # CriptografiaService (CPF)
│   ├── gateway/               # SpeechToTextGateway, LlmGateway, TelegramGateway
│   │                         # (interfaces + implementações substituíveis)
│   ├── repository/           # Portas de domínio + *RepositoryImpl (JPA/PostgreSQL)
│   ├── mapper/                # *Mapper/*Assembler entre DTO e domínio
│   └── dto/                   # *Dto/*Request/*Response
└── src/test/java/...          # Espelha a árvore acima: unit (domain/usecase/service/
                                # controller com mocks) + integration (contract dos
                                # webhooks/API)

frontend/
├── src/
│   ├── components/            # Componentes reutilizáveis (tabelas, formulários, PDF viewer)
│   ├── pages/                 # Login, CadastroProfessor, CadastroAluno, HistoricoAluno
│   └── services/               # Clientes HTTP para a API do Spring Boot
└── tests/                      # Vitest + React Testing Library

n8n/
└── workflows/                  # JSON versionado: RegistroAula.json,
                                 # RelatorioSemestral.json, VinculacaoTelegram.json

infrastructure/
├── docker-compose.yml          # frontend, backend, postgres, n8n, storage
├── postgres/                   # scripts de init (se necessário) — migrações reais via Flyway
└── storage/                    # volume para PDFs gerados
```

**Structure Decision**: Aplicação web (Opção 2 do template) acrescida de um diretório `n8n/` para
versionar os workflows de orquestração, conforme Princípio IV da constituição (n8n orquestra,
Spring Boot governa o negócio, PostgreSQL guarda a verdade). O backend segue Clean Architecture
com as camadas `domain → usecase → controller`, e `service/gateway/repository` implementando
interfaces definidas em `domain`/`usecase` (Dependency Inversion, Princípio I). O frontend React
serve exclusivamente a interface administrativa (FR-001, FR-017); o professor nunca autentica
nela (FR-017a).

## Complexity Tracking

*Nenhuma violação da Constitution Check identificada — seção não aplicável.*

## Post-Design Constitution Check

*Re-avaliação após Fase 1 (data-model.md, contracts/, quickstart.md).*

Os artefatos de design mantêm a separação `n8n orquestra / Spring Boot governa / PostgreSQL guarda
a verdade`: todos os endpoints em `contracts/api-n8n-integration.md` são internos ao backend, o
n8n nunca grava diretamente no PostgreSQL, e o estado do relatório (`RASCUNHO`,
`PENDENTE_REVISAO`, `APROVADO`) é persistido pelo backend (data-model.md). A criptografia de CPF e
a exclusão do áudio pós-transcrição foram detalhadas em `research.md` (§5, §1) e refletidas nos
contratos. Nenhuma nova violação foi introduzida pelo design; o gate permanece satisfeito.
