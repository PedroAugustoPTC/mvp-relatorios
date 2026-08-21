# MVP — Sistema de Relatórios para Escola de Música

Sistema que substitui o preenchimento manual de relatórios de aula por um fluxo assistido por voz:
o professor grava um áudio contando o que foi trabalhado, a Inteligência Artificial transcreve e
estrutura esse conteúdo em um relatório, gera um PDF para conferência e só salva no histórico do
aluno após aprovação explícita do professor. O mesmo padrão é reutilizado para consolidar, a cada
semestre, o histórico de relatórios de um aluno em um relatório pedagógico completo.

## Funcionalidades

- **Registro de aula por voz (Telegram)** — o professor seleciona o aluno, envia um áudio da aula e
  recebe um relatório estruturado (conteúdos trabalhados, evolução, dificuldades, tarefas,
  observações). Se faltar alguma informação relevante, o sistema pergunta ativamente antes de
  fechar o relatório.
- **Conferência obrigatória em PDF** — todo relatório (de aula ou semestral) é disponibilizado em
  PDF para o professor baixar e conferir. Só é salvo como aprovado depois que o professor confirma
  explicitamente; ele também pode pedir alterações em linguagem natural (ex.: "mude a dificuldade
  para ritmo"), gerando uma nova versão e um novo PDF.
- **Relatório semestral consolidado** — o professor solicita, pelo Telegram, a consolidação dos
  relatórios de aula de um aluno em um semestre (menu de opções pré-definidas, sem digitação livre
  de datas). A IA gera um relatório pedagógico completo (informações gerais, frequência e estudo,
  técnica, musicalidade, leitura e memorização, pontos de atenção, estratégias pedagógicas,
  acompanhamento familiar, planejamento do próximo semestre e parecer final), também sujeito a
  conferência e aprovação via PDF.
- **Cadastro e vinculação (interface web administrativa)** — um administrador cadastra professores
  e alunos. Cada professor recebe um código de vinculação de uso único para associar sua conta do
  Telegram ao cadastro, sem o qual não consegue usar os comandos de registro/consulta de aulas.
- **Controle de acesso por professor** — cada professor só enxerga, gera e aprova relatórios dos
  alunos associados a ele.
- **Histórico de relatórios** — o administrador consulta, pela interface web, o histórico completo
  de qualquer aluno; o professor consulta, exclusivamente pelo Telegram, o histórico dos seus
  próprios alunos (ele não possui login na interface web).

## Arquitetura

```
Telegram (professor)
      │  áudio / comandos
      ▼
   n8n  ──────────────► orquestra: Telegram, Speech-to-Text, LLM, fluxo conversacional
      │  chama endpoints internos (autenticados por token de serviço)
      ▼
 Spring Boot (backend) ── único ponto de escrita no banco; valida, persiste, gera PDF,
      │                    aplica todas as regras de autorização e negócio
      ▼
  PostgreSQL (fonte oficial de dados)

Administrador ──► React (frontend) ──► Spring Boot (mesma API, autenticação de sessão/token)
```

Princípios da arquitetura:

- **n8n orquestra, nunca decide**: os workflows do n8n cuidam apenas da interação com o Telegram e
  das chamadas aos provedores externos (transcrição e LLM); toda regra de negócio, validação e
  persistência vive no backend.
- **PostgreSQL é a única fonte da verdade**: o n8n nunca escreve diretamente no banco — todas as
  escritas passam pelos endpoints internos do backend.
- **Backend em Clean Architecture**: camadas `domain → usecase → controller`, com
  `service/gateway/repository/mapper/dto` implementando interfaces definidas nas camadas internas
  (facilita trocar o provedor de Speech-to-Text ou de LLM sem alterar regra de negócio).
- **Áudio não é retido**: o arquivo de áudio é usado apenas como insumo para a transcrição e
  excluído imediatamente após ela ser concluída com sucesso; permanecem apenas o texto transcrito e
  o relatório gerado.

## Tecnologias

| Camada | Stack |
| --- | --- |
| Backend | Java 21, Spring Boot 3.x (Web, Validation, Data JPA, Security), Flyway (migrações), springdoc-openapi |
| Frontend | React 18 + TypeScript 5, Vite, React Router, Vitest + React Testing Library |
| Orquestração | n8n self-hosted (workflows versionados como JSON) |
| Banco de dados | PostgreSQL 16 |
| IA (transcrição e LLM) | Provedores externos plugáveis via Gateway (padrão: Groq — compatível com a API de áudio/chat da OpenAI) |
| Geração de PDF | Biblioteca open-source no backend (ex.: OpenPDF/iText) |
| Infraestrutura | Docker Compose (frontend via Nginx, backend, PostgreSQL, n8n, volume dedicado para PDFs) |
| Testes | JUnit 5 + Mockito + Spring Boot Test/MockMvc (backend), Vitest + Testing Library (frontend) |

## Estrutura do repositório

```
backend/          Spring Boot — domain / usecase / controller / service / gateway / repository / mapper / dto
frontend/         React + TypeScript — interface administrativa (cadastro, vinculação, histórico)
n8n/workflows/    Workflows de orquestração (Telegram, transcrição, LLM) versionados em JSON
infrastructure/   docker-compose.yml, scripts de init do Postgres, volume de PDFs
specs/            Especificação funcional, plano técnico e contratos de API do MVP
```

## Fluxos da API (visão geral)

A API é dividida em dois grupos, com mecanismos de autenticação distintos:

- **`/api/v1/**`** — consumida pelo frontend administrativo. Exige sessão/token de administrador
  (Spring Security) em todas as rotas, exceto login. Cobre cadastro de professores e alunos,
  emissão de código de vinculação e consulta de histórico.
- **`/internal/v1/**`** — consumida apenas pelos workflows do n8n, autenticada por um token de
  serviço dedicado (header próprio, distinto da autenticação do administrador) e não exposta
  publicamente. Cobre:
  - vinculação e verificação da conta do Telegram de um professor;
  - listagem dos alunos associados a um professor (para o menu do bot);
  - o ciclo de vida de um relatório de aula — transcrição do áudio, estruturação por IA,
    respostas a perguntas de acompanhamento, revisão em linguagem natural e aprovação;
  - o ciclo de vida de um relatório semestral — consulta de semestres disponíveis, contagem de
    relatórios no período, geração, revisão e aprovação;
  - consulta de histórico de um aluno pelo professor responsável por ele, com bloqueio quando o
    aluno não está associado ao professor solicitante.

Todas as respostas de erro seguem um formato padrão (`{ "codigo", "mensagem" }`), permitindo que o
n8n traduza códigos de erro em mensagens amigáveis para o professor sem acoplar texto de negócio ao
workflow.

Detalhes completos de rotas, payloads e códigos de status ficam documentados em
[`specs/001-mvp-relatorios-escola-musica/contracts/`](specs/001-mvp-relatorios-escola-musica/contracts/)
e, em runtime, no OpenAPI gerado pelo springdoc a partir do próprio backend.

## Segurança e privacidade (visão geral)

- CPF do aluno (ou do responsável, se menor de idade) é o identificador único de cadastro e é
  armazenado **criptografado** no banco.
- O identificador de conta do Telegram nunca é tratado como prova de autenticação suficiente para
  ações administrativas sensíveis — essas permanecem restritas à interface web autenticada do
  administrador.
- Segredos (tokens de API, credenciais de banco, chaves de criptografia) são fornecidos somente via
  variáveis de ambiente, nunca versionados no repositório.
- Os serviços de banco de dados e de orquestração (n8n) não são expostos publicamente sem
  autenticação; em desenvolvimento local ficam publicados apenas em loopback (`127.0.0.1`).
- Toda comunicação externa é feita via HTTPS.

Este README propositalmente não descreve valores de configuração sensíveis, segredos, tokens ou
detalhes de implementação de controles de segurança — apenas a existência dessas proteções.

## Como rodar (desenvolvimento local)

Pré-requisitos: Docker e Docker Compose.

```bash
cp infrastructure/.env.example infrastructure/.env
# edite infrastructure/.env com valores reais (senhas, tokens do Telegram/Groq, etc. — nunca versionar este arquivo)
docker compose -f infrastructure/docker-compose.yml up --build
```

Serviços expostos localmente:

- Frontend administrativo: `http://localhost:8080`
- Backend (API): `http://127.0.0.1:8081`
- n8n (workflows): `http://127.0.0.1:5678` (Basic Auth)
- PostgreSQL: `127.0.0.1:5432` (uso local/ferramentas de banco)

## Documentação adicional

- Especificação funcional completa: [`specs/001-mvp-relatorios-escola-musica/spec.md`](specs/001-mvp-relatorios-escola-musica/spec.md)
- Plano técnico e decisões de arquitetura: [`specs/001-mvp-relatorios-escola-musica/plan.md`](specs/001-mvp-relatorios-escola-musica/plan.md)
- Contratos de API: [`specs/001-mvp-relatorios-escola-musica/contracts/`](specs/001-mvp-relatorios-escola-musica/contracts/)
- Modelo de dados: [`specs/001-mvp-relatorios-escola-musica/data-model.md`](specs/001-mvp-relatorios-escola-musica/data-model.md)
