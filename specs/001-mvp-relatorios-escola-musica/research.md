# Phase 0 Research: MVP Sistema de Relatórios para Escola de Música

**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md)

Este documento resolve as decisões técnicas necessárias para a Fase 1 de design. A especificação
não deixou nenhuma pergunta de negócio em aberto (todas as `NEEDS CLARIFICATION` da sessão de
`/speckit-clarify` já foram respondidas em `spec.md`); as pesquisas abaixo tratam de decisões de
implementação implícitas exigidas pela constituição do projeto e pelo documento de referência
`MVP_Escola_de_Musica_IA_n8n_Telegram_Docker.md`.

## 1. Provedor de Speech-to-Text

- **Decision**: Tratar o provedor de Speech-to-Text como uma integração substituível acessada
  exclusivamente por um `SpeechToTextGateway` no backend (interface definida em `domain`/`usecase`,
  implementação concreta em `gateway`), chamado a partir do workflow do n8n via HTTP Request node
  que aciona um endpoint interno do backend (não diretamente pela API do provedor a partir do n8n).
  A escolha do produto específico (ex.: Whisper via API, Azure Speech, Google Speech-to-Text) fica
  como parâmetro de configuração de ambiente, não como decisão arquitetural do MVP.
- **Rationale**: A constituição (Princípio IV) exige que o n8n não implemente regra de negócio nem
  seja o dono da integração de domínio; ao centralizar a chamada real de transcrição atrás de um
  Gateway no Spring Boot, o backend permanece a fonte de verdade e o provedor pode ser trocado sem
  alterar workflows do n8n. Isso também simplifica a aplicação da política de exclusão imediata do
  áudio (FR-005a), já que o backend controla o ciclo de vida do arquivo.
- **Alternatives considered**: (a) n8n chamando a API de transcrição diretamente e enviando somente
  o texto ao backend — rejeitado por espalhar lógica de retenção/exclusão de áudio fora do domínio
  controlado pelo Spring Boot, dificultando auditar a política de LGPD; (b) transcrição síncrona
  dentro do próprio Spring Boot sem n8n — rejeitado por contrariar o Princípio IV (n8n é responsável
  por orquestrar integrações de IA) e o documento de referência, que define o n8n como camada de
  orquestração desse fluxo.

## 2. Provedor de LLM para estruturação e consolidação

- **Decision**: Mesmo padrão do item 1 — `LlmGateway` no backend, acionado por um endpoint interno
  chamado pelo workflow do n8n, responsável tanto pela estruturação do relatório de aula (a partir
  da transcrição) quanto pela consolidação do relatório semestral (a partir dos relatórios de aula
  do período). O contrato de saída da IA é um JSON estruturado validado pelo backend antes de
  qualquer persistência (schema descrito em `contracts/`).
- **Rationale**: Mantém a IA como um detalhe de infraestrutura substituível, alinhado ao Princípio I
  (Domain/UseCase não conhecem o provedor de IA) e ao Princípio VI (não investir em abstração além
  do necessário — um único Gateway cobre os dois casos de uso porque ambos são "texto → JSON
  estruturado segundo um schema").
- **Alternatives considered**: Prompts/orquestração de múltiplas chamadas de IA inteiramente dentro
  do n8n — rejeitado pelo mesmo motivo do item 1 (Princípio IV) e porque dificultaria testar a
  lógica de validação/correção de JSON exigida pelos Edge Cases da spec com cobertura unitária ≥90%.

## 3. Refinamento conversacional (perguntas de acompanhamento e edições em linguagem natural)

- **Decision**: O n8n mantém o estado transitório da conversa (qual pergunta está pendente, qual
  edição foi solicitada) e o repassa ao backend a cada interação; o Spring Boot é quem decide, via
  `UseCase`, se uma resposta do professor é suficiente ou se uma nova pergunta deve ser feita,
  retornando ao n8n apenas o texto/opções a exibir no Telegram. O estado definitivo do fluffo
  (`SELECTING_STUDENT`, `WAITING_AUDIO`, `PROCESSING`, `REVIEWING`, `APPROVED`) é persistido no
  PostgreSQL pelo backend, conforme a seção 14 do documento de referência e o Princípio IV da
  constituição.
- **Rationale**: Evita que a regra "o que perguntar quando falta informação" (FR-007) fique
  implementada apenas em nós visuais do n8n, o que impediria cobertura de teste unitário e criaria
  uma segunda fonte de verdade para o estado da conversa.
- **Alternatives considered**: Estado de conversa mantido inteiramente no n8n (via seu próprio
  armazenamento de execução) — rejeitado explicitamente pela seção 14 do documento de referência
  ("O n8n não deve ser considerado a fonte definitiva desse estado") e pelo Princípio IV.

## 4. Geração de PDF

- **Decision**: Geração de PDF é responsabilidade de um `PdfGeracaoService` no backend (Spring
  Boot), usando uma biblioteca Java open-source (ex.: OpenPDF) para montar o PDF de relatório de
  aula (layout simples) e do relatório semestral (seguindo a estrutura de seções do modelo de
  referência do cliente, com cabeçalho/rodapé configuráveis). O arquivo gerado é salvo em um volume
  de storage e referenciado no banco por caminho/URL; o n8n apenas repassa o link/arquivo para o
  Telegram.
- **Rationale**: Mantém a "etapa de revisão obrigatória antes da aprovação" (FR-008, FR-013a) como
  parte do domínio controlado e testável do backend, evitando duplicar a lógica de layout em
  múltiplos lugares (n8n vs. backend) e permitindo reaproveitar o mesmo serviço para os dois tipos
  de relatório (Princípio VI).
- **Alternatives considered**: Gerar o PDF via node de terceiros dentro do próprio n8n — rejeitado
  por tornar a "etapa de revisão obrigatória" dependente de um workflow visual difícil de testar
  com a cobertura unitária exigida (Princípio III), além de misturar responsabilidade de
  apresentação com orquestração.

## 5. Criptografia do CPF

- **Decision**: O CPF (do aluno ou responsável) é armazenado criptografado em repouso no
  PostgreSQL usando criptografia simétrica autenticada (ex.: AES-GCM) aplicada na camada de
  persistência do backend (`AlunoRepositoryImpl` ou um `CriptografiaService` dedicado), com a chave
  gerenciada via variável de ambiente/secret manager (nunca em código versionado). A unicidade do
  CPF (FR-001a) é garantida validando duplicidade a partir de um hash determinístico (ex.:
  HMAC-SHA-256 com chave de aplicação) armazenado ao lado do valor criptografado, permitindo checar
  duplicidade sem descriptografar todos os registros a cada cadastro.
- **Rationale**: Atende FR-001a (armazenamento criptografado) e ao mesmo tempo permite impor a
  restrição de unicidade exigida pela spec sem expor o CPF em texto claro para indexação.
- **Alternatives considered**: Criptografia apenas em nível de disco/banco (ex.: TDE do PostgreSQL)
  sem criptografia de aplicação — rejeitado por não atender explicitamente "armazenado de forma
  criptografada" como controle de aplicação auditável exigido pela spec e pelo Princípio V; hashing
  irreversível do CPF sem forma de recuperá-lo — rejeitado porque a interface administrativa
  precisa eventualmente exibir/usar o CPF cadastrado, não apenas verificá-lo.

## 6. Autenticação da interface web administrativa

- **Decision**: Autenticação própria do backend (Spring Security) para o administrador, com
  usuário/senha (hash com BCrypt) e sessão via token (ex.: JWT de curta duração). Não há
  autenticação federada (SSO) no MVP, mantendo simplicidade (Princípio VI). O professor nunca
  autentica nessa interface (FR-017a).
- **Rationale**: A spec assume um único perfil de administrador operando a interface web; um
  mecanismo de autenticação simples e local é suficiente e evita dependências externas
  desnecessárias no MVP.
- **Alternatives considered**: OAuth/SSO com provedor externo — rejeitado por adicionar
  complexidade não exigida pelo MVP atual (Princípio VI); nenhuma alternativa de "sem autenticação"
  foi considerada, pois FR-020 exige que ações administrativas sensíveis fiquem restritas à
  interface web autenticada.

## 7. Código de vinculação Telegram↔Professor

- **Decision**: Código de vinculação de uso único, gerado pelo backend no momento do cadastro do
  professor (ex.: código alfanumérico curto com expiração), exibido na interface web e validado
  pelo `VincularContaTelegramUseCase` quando o professor o informa ao bot. Após uso bem-sucedido, o
  código é invalidado.
- **Rationale**: Atende FR-002/FR-003/SC-006 (vinculação em menos de 2 minutos, sem suporte manual)
  com um mecanismo simples, alinhado ao Princípio VI e à seção 6 do documento de referência.
- **Alternatives considered**: Vincular automaticamente pelo primeiro contato sem código —
  rejeitado pelo Edge Case da spec sobre uso indevido da conta do Telegram e pelo FR-020 (Telegram
  User ID não é prova de identidade suficiente sozinho).
