<!--
Sync Impact Report
- Version change: (template, unratified) → 1.0.0
- Rationale: Initial ratification of the project constitution. MAJOR because this establishes the
  first binding set of governance rules (no prior enforceable version existed).
- Modified principles: n/a (initial version)
- Added sections:
  - Core Principles: I. Clean Architecture (Non-Negotiable), II. Padrão de Nomenclatura
    Obrigatório, III. Cobertura Mínima de Testes Unitários — 90% (Non-Negotiable),
    IV. Separação n8n / Backend / Banco de Dados, V. Segurança e Privacidade desde o Design
    (LGPD), VI. Simplicidade e Foco no MVP
  - Stack Tecnológica e Restrições Arquiteturais
  - Fluxo de Desenvolvimento e Quality Gates
  - Governance
- Removed sections: none
- Templates requiring follow-up:
  - .specify/templates/plan-template.md — ⚠ pending manual check that its Constitution Check
    gate references Clean Architecture layers and the 90% coverage gate
  - .specify/templates/tasks-template.md — ⚠ pending manual check that generated task lists
    include a test-coverage verification task
  - .specify/templates/spec-template.md — ✅ no changes required (no constitution-specific
    references)
- Deferred items: RATIFICATION_DATE assumed as today's date (2026-08-13) since this is the first
  ratification produced in this session; confirm with the project owner if a different historical
  date should be recorded.
-->

# MVP Escola de Música — IA + n8n + Telegram Constitution

## Core Principles

### I. Clean Architecture (Non-Negotiable)
Todo backend (Spring Boot) DEVE seguir Clean Architecture com separação explícita em camadas,
independentes de framework e testáveis isoladamente:

- **Domain**: entidades e regras de negócio puras, sem dependência de frameworks, banco de dados
  ou bibliotecas externas.
- **UseCase (Application)**: um caso de uso por classe/arquivo, orquestra entidades de domínio e
  portas (interfaces) de saída; não conhece detalhes de HTTP, Telegram, n8n ou persistência.
- **Controller (Interface/Adapter)**: responsável exclusivamente por receber a requisição
  (HTTP, webhook do n8n), validar formato de entrada, invocar o UseCase correspondente e traduzir
  o resultado em resposta. Controllers NÃO contêm regra de negócio.
- **Service**: implementa integrações e regras de aplicação que suportam UseCases (ex.:
  orquestração de chamadas a serviços externos como Speech-to-Text/LLM); Services NÃO substituem
  UseCases nem contêm regra de domínio.
- **Repository/Gateway**: implementações concretas de acesso a dados (PostgreSQL) e integrações
  externas, sempre atrás de uma interface definida na camada de domínio/aplicação.

A regra de dependência é unidirecional: `Controller → UseCase → Domain`, com `Service` e
`Repository` implementando interfaces que o `Domain`/`UseCase` define (Dependency Inversion). Uma
camada interna NUNCA importa uma camada externa. Qualquer violação encontrada em code review é
bloqueante.

**Rationale**: o domínio "aula → relatório" precisa evoluir independentemente dos canais de
entrada (Telegram hoje, Web/WhatsApp amanhã) e dos fornecedores de IA/transcrição. Clean
Architecture garante que o Spring Boot continue sendo a fonte de regras de negócio mesmo quando o
n8n, o Telegram ou o provedor de LLM forem substituídos.

### II. Padrão de Nomenclatura Obrigatório
Nomes de classes, pacotes e arquivos DEVEM comunicar claramente a camada e a responsabilidade,
seguindo os sufixos padronizados abaixo (exemplos ilustrativos, não exaustivos):

- `*Controller` — adaptador de entrada (ex.: `RelatorioController`, `TelegramWebhookController`).
- `*UseCase` — caso de uso único e nomeado como uma ação de negócio (ex.: `AprovarRelatorioUseCase`,
  `GerarRelatorioSemestralUseCase`).
- `*Service` — serviço de aplicação/integração (ex.: `TranscricaoService`, `LlmService`).
- `*Repository` — porta/adaptador de persistência (ex.: `AlunoRepository`,
  `RelatorioRepositoryImpl` para a implementação concreta).
- `*Gateway` — integrações com sistemas externos que não são persistência pura (ex.:
  `TelegramGateway`, `SpeechToTextGateway`).
- `*Mapper`/`*Assembler` — conversão entre DTOs e entidades de domínio.
- `*Dto` / `*Request` / `*Response` — objetos de transporte na borda da aplicação.
- Entidades de domínio usam substantivos de negócio sem sufixo técnico (ex.: `Aluno`, `Professor`,
  `Relatorio`, `Aula`).

Pacotes seguem a convenção `com.<empresa>.<projeto>.<contexto>.<camada>`
(ex.: `com.escolademusica.relatorios.usecase`, `com.escolademusica.relatorios.controller`). Nomes
em inglês são obrigatórios no código (classes, métodos, variáveis); textos voltados ao professor
(mensagens do bot, relatórios) podem permanecer em português.

**Rationale**: nomenclatura previsível reduz o tempo de onboarding, torna a arquitetura auto-
documentada e evita que regra de negócio "vaze" para a camada errada por ambiguidade de
responsabilidade.

### III. Cobertura Mínima de Testes Unitários — 90% (Non-Negotiable)
Todo código de produção DEVE atingir no mínimo 90% de cobertura de testes unitários, medida por
linha e por branch, antes de ser mesclado à branch principal. Regras associadas:

- UseCases e regras de domínio DEVEM ter testes unitários que cubram os caminhos de sucesso, de
  erro e de borda (dados incompletos, aluno não identificado, JSON inválido da IA, etc.).
- Controllers e Services DEVEM ter testes que verifiquem contrato de entrada/saída usando dublês
  (mocks/stubs) para dependências externas (n8n, Telegram, LLM, Speech-to-Text).
- Integrações externas reais (Telegram, LLM, Speech-to-Text, PostgreSQL) NÃO contam para a
  cobertura unitária; testes de integração são complementares e não substituem os unitários.
- O pipeline de CI DEVE falhar automaticamente caso a cobertura fique abaixo de 90% ou caso
  qualquer teste seja quebrado.
- Nenhum Pull Request é aprovado sem os testes correspondentes às mudanças, mesmo em correções
  pequenas.

**Rationale**: o domínio envolve dados sensíveis de menores de idade e decisões geradas por IA que
alimentam relatórios oficiais; falhas silenciosas nesse fluxo têm alto custo de confiança com o
cliente. Uma cobertura mínima alta e obrigatória reduz regressões e documenta o comportamento
esperado de cada UseCase.

### IV. Separação n8n / Backend / Banco de Dados
A responsabilidade entre as camadas de infraestrutura é fixa e não deve ser diluída:

- **n8n** orquestra integrações, automações, webhooks do Telegram, chamadas a Speech-to-Text/LLM e
  conversas de refinamento. n8n NÃO implementa regra de negócio, autorização ou validação de
  domínio.
- **Spring Boot** governa regras de negócio, autorização, validações e é o único ponto de escrita
  no banco de dados. Todo dado que chega via n8n é validado e persistido através de UseCases do
  backend, nunca gravado diretamente pelo n8n.
- **PostgreSQL** é a fonte oficial da verdade; o estado de conversa do bot (ex.:
  `SELECTING_STUDENT`, `WAITING_AUDIO`) pode transitar pelo n8n, mas seu registro definitivo é no
  backend.
- O Telegram User ID é apenas um identificador de integração; NUNCA substitui autenticação e
  autorização controladas pelo Spring Boot.

**Rationale**: mantém o domínio desacoplado do canal de entrada (Telegram hoje, Web/WhatsApp no
futuro) e evita que lógica crítica fique presa em workflows visuais difíceis de testar e versionar.

### V. Segurança e Privacidade desde o Design (LGPD)
Dados de alunos — incluindo possíveis menores de idade — exigem tratamento cuidadoso em todo o
ciclo de vida:

- Segredos (tokens do Telegram, chaves de LLM/Speech-to-Text, credenciais de banco) DEVEM ficar em
  variáveis de ambiente/secret manager, nunca em código versionado.
- Toda comunicação externa DEVE usar HTTPS; o n8n e o PostgreSQL NÃO podem ficar expostos
  publicamente sem autenticação.
- Logs NÃO podem conter dados sensíveis desnecessários (áudio bruto, transcrição completa,
  identificadores pessoais) além do estritamente necessário para depuração.
- Política de retenção de áudio DEVE ser definida e aplicada (ex.: exclusão do áudio original após
  prazo definido, mantendo transcrição/relatório conforme necessidade legal).
- Controle de acesso por professor DEVE garantir que um professor só acesse dados dos próprios
  alunos.

**Rationale**: o produto lida com dados pedagógicos de crianças e adolescentes; tratamento
inadequado gera risco legal (LGPD) e de confiança que pode inviabilizar o MVP com o cliente.

### VI. Simplicidade e Foco no MVP
Novas abstrações, camadas ou integrações só são adicionadas quando o MVP atual exige. Preferir a
opção mais simples descrita no documento de referência do produto (ex.: seleção de aluno antes do
áudio, geração de relatório semestral sob demanda) antes de construir automações completas (ex.:
geração automática semestral, WhatsApp, financeiro), que ficam fora de escopo até serem
explicitamente priorizadas.

**Rationale**: o roadmap do MVP prioriza validar a hipótese de valor (professor economiza tempo)
antes de investir em funcionalidades avançadas; complexidade prematura atrasa essa validação.

## Stack Tecnológica e Restrições Arquiteturais

- **Frontend**: React + TypeScript, servido via Nginx em produção.
- **Backend**: Spring Boot (Java), organizado em Clean Architecture conforme Princípio I.
- **Banco de dados**: PostgreSQL como única fonte oficial de dados estruturados.
- **Orquestração**: n8n, responsável por Telegram, webhooks, chamadas a Speech-to-Text/LLM e
  automações, conforme Princípio IV.
- **Interface do professor**: Bot do Telegram como canal primário do MVP; o backend não deve
  acoplar regra de negócio a esse canal específico.
- **IA**: serviços de Speech-to-Text e LLM são tratados como integrações externas substituíveis,
  acessadas por Services/Gateways dedicados.
- **Infraestrutura**: Docker Compose para orquestrar frontend, backend, PostgreSQL, n8n e storage;
  todo serviço necessário para rodar o MVP localmente DEVE estar containerizado.

## Fluxo de Desenvolvimento e Quality Gates

- Todo Pull Request DEVE demonstrar aderência às camadas de Clean Architecture (Princípio I) e ao
  padrão de nomenclatura (Princípio II); revisores bloqueiam PRs que misturem responsabilidades de
  camada.
- Todo Pull Request DEVE incluir os testes unitários correspondentes e manter cobertura ≥ 90%
  (Princípio III); o CI é a fonte de verdade para essa métrica.
- Mudanças que envolvam workflows do n8n DEVEM ser versionadas junto do repositório e revisadas
  quanto à separação de responsabilidades (Princípio IV).
- Mudanças que envolvam dados de aluno, áudio ou integrações de IA DEVEM ser revisadas quanto a
  segurança e privacidade (Princípio V) antes do merge.
- Funcionalidades fora do escopo do MVP atual (WhatsApp, financeiro, app mobile, geração automática
  semestral) exigem justificativa explícita e aprovação antes de entrarem em desenvolvimento
  (Princípio VI).

## Governance

Esta constituição prevalece sobre qualquer prática, convenção informal ou preferência individual
em conflito com ela. Em caso de dúvida entre este documento e outra documentação do projeto
(incluindo `MVP_Escola_de_Musica_IA_n8n_Telegram_Docker.md`), a constituição tem precedência para
decisões de arquitetura, nomenclatura e qualidade de testes.

**Processo de emenda**: alterações a esta constituição exigem (1) proposta explícita descrevendo a
mudança e sua motivação, (2) atualização do texto com o número de versão incrementado conforme a
política abaixo, e (3) verificação de que os templates dependentes (`plan-template.md`,
`spec-template.md`, `tasks-template.md`) permanecem consistentes com os princípios revisados.

**Política de versionamento semântico**:
- **MAJOR**: remoção ou redefinição incompatível de um princípio existente (ex.: abandonar Clean
  Architecture ou reduzir a cobertura mínima abaixo de 90%).
- **MINOR**: adição de um novo princípio ou expansão material de uma seção existente.
- **PATCH**: esclarecimentos, correções de redação ou ajustes não semânticos.

**Revisão de conformidade**: todo Pull Request DEVE ser avaliado contra os princípios desta
constituição durante o code review; violações não justificadas bloqueiam o merge. Justificativas
de exceção DEVEM ser registradas na descrição do PR e revisadas por outro desenvolvedor.

**Version**: 1.0.0 | **Ratified**: 2026-08-13 | **Last Amended**: 2026-08-13
</content>
