# MVP — Sistema de Relatórios para Escola de Música
## Arquitetura com IA + n8n + Bot Telegram + Docker

## 1. Visão do produto

O cliente, dono de uma escola de música, possui duas dores principais:

1. Após cada aula, precisa registrar um relatório individual de cada aluno, mas não quer perder tempo digitando.
2. A cada 6 meses, precisa produzir um relatório consolidado de cada aluno com base nos registros das aulas anteriores.

A proposta do MVP é utilizar **áudio + IA + n8n + Telegram** para transformar esse processo em um fluxo rápido e simples.

### Fluxo principal

**Aula acontece → professor envia áudio pelo Telegram → n8n recebe → áudio é transcrito → IA estrutura/refina → backend salva → professor revisa → relatório é aprovado.**

A cada 6 meses:

**Sistema busca os relatórios → IA consolida → relatório semestral é enviado ao professor para revisão → professor aprova.**

---

# 2. Por que utilizar Telegram + n8n?

O Telegram pode funcionar como uma **interface de entrada extremamente simples**, principalmente durante a validação do MVP.

O professor não precisa abrir uma tela complexa para registrar a aula.

Ele pode simplesmente abrir o bot e enviar:

> 🎙️ Áudio da aula

O n8n funciona como uma camada de **orquestração e automação**, conectando:

- Telegram
- Backend
- Serviço de transcrição
- LLM
- Banco de dados
- Armazenamento
- Notificações

Isso permite testar rapidamente o fluxo de negócio sem precisar construir imediatamente toda a experiência de gravação de áudio no frontend.

---

# 3. Papel de cada tecnologia

A divisão recomendada é:

| Tecnologia | Responsabilidade |
|---|---|
| React + TypeScript | Sistema web / administração |
| Spring Boot | Regras de negócio e API |
| PostgreSQL | Dados estruturados |
| Docker | Infraestrutura |
| n8n | Orquestração dos fluxos |
| Telegram Bot | Interface rápida para o professor |
| Serviço de Speech-to-Text | Transcrição |
| LLM | Estruturação, refinamento e geração |
| Storage | Áudios e arquivos |

### Princípio arquitetural

O **n8n não deve substituir o backend**.

O n8n deve ser responsável principalmente por:

> **Orquestrar integrações e automações.**

O Spring Boot continua sendo o responsável pelas regras de negócio e pela fonte oficial dos dados.

---

# 4. Arquitetura geral

```text
                         PROFESSOR
                             │
                             ▼
                       TELEGRAM BOT
                             │
                             ▼
                           n8n
                             │
            ┌────────────────┼─────────────────┐
            │                │                 │
            ▼                ▼                 ▼
       Transcrição          IA             Spring Boot
            │                │                 │
            └────────┬───────┘                 │
                     ▼                         │
               Dados estruturados              │
                     │                         │
                     └────────────┬────────────┘
                                  ▼
                             PostgreSQL
                                  │
                                  ▼
                              Relatórios
```

A aplicação web fica como interface administrativa:

```text
                    ┌──────────────────────┐
                    │    React + TS        │
                    │    Web App           │
                    └──────────┬───────────┘
                               │
                               ▼
                         Spring Boot
                               │
                    ┌──────────┴───────────┐
                    ▼                      ▼
               PostgreSQL              Storage
```

---

# 5. Fluxo principal — Registro de aula

## Etapa 1 — Professor envia áudio

O professor abre o bot do Telegram e envia um áudio.

Exemplo:

> "Hoje trabalhei com a Maria a música X. Ela melhorou bastante a mão direita, mas ainda está com dificuldade no ritmo..."

O Telegram envia a mensagem para o workflow do n8n.

---

# 6. Etapa 2 — n8n identifica o professor

O bot precisa relacionar o usuário do Telegram ao professor cadastrado no sistema.

Exemplo conceitual:

```text
Telegram User ID
       │
       ▼
      n8n
       │
       ▼
Spring Boot
       │
       ▼
Professor
```

O primeiro acesso pode exigir uma etapa de vinculação:

> "Para vincular sua conta, informe o código exibido no sistema."

Depois disso, o Telegram User ID fica associado ao professor.

### Regra importante

O Telegram User ID não deve ser utilizado como substituto da autenticação do sistema.

Ele é um identificador de integração.

O backend continua controlando autorização e permissões.

---

# 7. Etapa 3 — Identificar o aluno

Existem algumas opções.

## Opção A — Professor informa o aluno antes do áudio

Exemplo:

```text
/relatorio

Qual aluno?

Maria Souza
João Silva
Pedro Santos
```

Depois:

```text
🎙️ Envie o áudio da aula.
```

Essa é a abordagem recomendada para o MVP.

---

## Opção B — Professor envia o nome junto com o áudio

Exemplo:

> "Maria Souza. Hoje trabalhamos..."

É mais simples, mas aumenta o risco de erro de identificação.

---

## Opção C — Contexto da última aula

O bot pode lembrar o aluno selecionado anteriormente.

Exemplo:

```text
Aluno atual: Maria Souza

🎙️ Envie o áudio.
```

Essa abordagem pode ser adicionada posteriormente.

---

# 8. Fluxo recomendado no Telegram

```text
Professor
   │
   ▼
/relatorio
   │
   ▼
Bot
   │
   ▼
"Qual aluno?"
   │
   ▼
Professor seleciona Maria
   │
   ▼
Bot
   │
   ▼
"Envie o áudio da aula"
   │
   ▼
Professor envia áudio
   │
   ▼
n8n
```

---

# 9. Etapa 4 — n8n baixa o áudio

O workflow recebe a mensagem do Telegram.

O n8n:

1. Identifica a mensagem
2. Obtém o arquivo
3. Baixa o áudio
4. Armazena temporariamente
5. Envia para transcrição

Conceitualmente:

```text
Telegram
   │
   ▼
Telegram Trigger
   │
   ▼
Get File
   │
   ▼
Download
   │
   ▼
Speech-to-Text
```

---

# 10. Etapa 5 — Transcrição

O áudio é enviado para um serviço de Speech-to-Text.

Resultado:

```text
"Hoje trabalhei com a Maria a música X..."
```

O n8n recebe a transcrição.

---

# 11. Etapa 6 — IA estrutura o conteúdo

Aqui está uma das partes mais importantes.

Não devemos pedir simplesmente:

> "Faça um relatório."

O prompt deve solicitar uma estrutura bem definida.

Exemplo conceitual:

```json
{
  "conteudos_trabalhados": [],
  "evolucao": "",
  "dificuldades": [],
  "atividades_propostas": [],
  "observacoes": "",
  "proximos_passos": []
}
```

A IA transforma a fala do professor em dados estruturados.

---

# 12. Etapa 7 — Refinamento pelo n8n

O n8n pode funcionar como uma camada de **refinamento e validação**.

Fluxo:

```text
Áudio
  │
  ▼
Transcrição
  │
  ▼
IA
  │
  ▼
JSON estruturado
  │
  ▼
Validação
  │
  ├── Dados incompletos?
  │       │
  │       ▼
  │    Perguntar ao professor
  │
  └── Dados suficientes
          │
          ▼
      Spring Boot
```

Esse é um ponto muito interessante para o MVP.

Se a IA não conseguir determinar algo importante, o bot pode perguntar:

> "Entendi que a Maria trabalhou a música X. Você deseja registrar alguma tarefa para casa?"

O professor pode responder:

> "Sim, praticar o exercício de ritmo."

O n8n adiciona essa informação ao fluxo.

---

# 13. n8n como agente de refinamento

O n8n não precisa ser somente um pipeline.

Ele pode coordenar uma pequena conversa de refinamento.

Exemplo:

```text
Professor:
🎙️ "Hoje trabalhei escalas com João..."

Bot:
"Entendi. Registrei:
- Conteúdo: escalas
- Evolução: ...
- Dificuldade: ...

Deseja acrescentar alguma observação?"

Professor:
"Sim. Ele teve dificuldade na mão esquerda."

Bot:
"Perfeito. Adicionei essa observação."

[Salvar relatório]
```

Isso aumenta a qualidade do relatório sem obrigar o professor a preencher formulários.

---

# 14. Estado da conversa

Como o Telegram pode envolver várias etapas, o workflow precisa controlar estado.

Exemplo:

```text
ConversationState

TELEGRAM_USER
      │
      ▼
SELECTING_STUDENT
      │
      ▼
WAITING_AUDIO
      │
      ▼
PROCESSING
      │
      ▼
REVIEWING
      │
      ▼
APPROVED
```

O estado pode ser armazenado no próprio backend/PostgreSQL.

O n8n não deve ser considerado a fonte definitiva desse estado.

---

# 15. Aprovação do relatório

Depois da geração:

```text
Bot:

Relatório de Maria Souza:

Conteúdo:
Música X

Evolução:
...

Dificuldades:
...

Tarefas:
...

[✅ Aprovar]
[✏️ Editar]
[❌ Cancelar]
```

O professor pode aprovar diretamente pelo Telegram.

---

# 16. Edição pelo Telegram

Para o MVP, a edição pode ser simples.

Exemplo:

> "Altere a dificuldade para ritmo na segunda parte."

O n8n envia a solicitação para a IA.

A IA retorna o relatório atualizado.

O bot apresenta novamente.

```text
[✅ Aprovar]
[✏️ Editar]
```

Isso cria uma experiência muito mais natural.

---

# 17. Persistência no backend

Depois da aprovação:

```text
n8n
 │
 ▼
Spring Boot API
 │
 ├── aluno
 ├── aula
 ├── transcrição
 ├── dados estruturados
 └── relatório
        │
        ▼
    PostgreSQL
```

O backend registra:

- aluno;
- professor;
- data;
- áudio;
- transcrição;
- conteúdo;
- evolução;
- dificuldades;
- tarefas;
- observações;
- relatório final;
- status;
- timestamps.

---

# 18. Relatório semestral

O relatório semestral pode utilizar o mesmo ecossistema.

Existem duas formas possíveis.

## Opção A — Professor solicita pelo Telegram

Exemplo:

```text
/relatorio_semestral
```

Bot:

> "Qual aluno?"

Professor:

> Maria Souza

Bot:

> "Qual período?"

Professor:

> Janeiro a Junho de 2026

O n8n solicita os dados ao Spring Boot.

---

## Opção B — Geração automática

O sistema identifica alunos que completaram 6 meses.

Exemplo:

```text
Cron
 │
 ▼
n8n
 │
 ▼
Busca alunos elegíveis
 │
 ▼
Busca relatórios
 │
 ▼
IA consolida
 │
 ▼
Relatório semestral
 │
 ▼
Professor recebe no Telegram
```

Para o MVP, a **Opção A é mais simples**.

A automação completa pode vir depois.

---

# 19. Geração do relatório semestral

O backend fornece ao n8n os dados estruturados.

Exemplo:

```json
{
  "aluno": "Maria Souza",
  "periodo": "Janeiro a Junho",
  "aulas": [
    {
      "data": "...",
      "conteudos": [],
      "evolucao": "...",
      "dificuldades": []
    }
  ]
}
```

O n8n envia esse contexto para a IA.

A IA gera:

```text
RELATÓRIO SEMESTRAL

1. Desenvolvimento geral

2. Conteúdos trabalhados

3. Evolução técnica

4. Principais dificuldades

5. Pontos de destaque

6. Recomendações

7. Próximos objetivos
```

Depois:

```text
IA
 │
 ▼
n8n
 │
 ▼
Telegram
 │
 ▼
Professor
 │
 ├── Aprovar
 └── Solicitar alteração
```

---

# 20. Arquitetura Docker

A infraestrutura principal pode ser executada em containers.

Exemplo:

```text
Docker Compose

├── frontend
│    └── React + Nginx
│
├── backend
│    └── Spring Boot
│
├── postgres
│    └── PostgreSQL
│
├── n8n
│    └── Workflows / automações
│
└── storage
     └── Volumes persistentes
```

Serviços externos:

```text
Telegram
    │
    ▼
n8n

n8n
    │
    ├── Speech-to-Text
    └── LLM
```

---

# 21. Responsabilidade do n8n vs Backend

Essa separação é fundamental.

## n8n

Responsável por:

- Telegram
- Webhooks
- Orquestração
- Integração com IA
- Transcrição
- Fluxos automáticos
- Notificações
- Agendamento
- Conversas de refinamento

## Spring Boot

Responsável por:

- Regras de negócio
- Autorização
- Usuários
- Professores
- Alunos
- Aulas
- Relatórios
- Histórico
- Persistência
- Validações
- API principal

## PostgreSQL

Responsável por:

- Fonte oficial dos dados

### Regra de ouro

> **n8n orquestra. Spring Boot governa o negócio. PostgreSQL guarda a verdade.**

---

# 22. Estrutura sugerida do projeto

```text
music-school/
│
├── backend/
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
│
├── frontend/
│   ├── src/
│   ├── Dockerfile
│   └── package.json
│
├── n8n/
│   ├── workflows/
│   └── README.md
│
├── infrastructure/
│   ├── docker-compose.yml
│   ├── postgres/
│   └── storage/
│
└── README.md
```

Os workflows do n8n devem ser versionados junto do projeto sempre que possível.

---

# 23. Workflow principal do n8n

Um workflow inicial poderia ser:

```text
Telegram Trigger
      │
      ▼
Identify User
      │
      ▼
Check Session
      │
      ▼
Receive Audio
      │
      ▼
Download Audio
      │
      ▼
Speech-to-Text
      │
      ▼
Get Student Context
      │
      ▼
LLM — Structure
      │
      ▼
Validate JSON
      │
      ▼
Need Refinement?
   ┌──┴──┐
   │     │
  SIM   NÃO
   │     │
   ▼     ▼
Telegram  Spring Boot
Question    │
   │        ▼
   ▼     PostgreSQL
Response
   │
   ▼
LLM — Refine
   │
   ▼
Spring Boot
```

---

# 24. Tratamento de erros

O n8n deve ter caminhos explícitos para erros.

Exemplos:

### Áudio não processado

```text
"Não consegui processar o áudio.
Tente enviar novamente."
```

### Transcrição com baixa qualidade

```text
"Não consegui entender alguns trechos.
Você pode enviar o áudio novamente?"
```

### Aluno não identificado

```text
"Não consegui identificar o aluno.
Selecione um aluno:"
```

### IA retornou estrutura inválida

O workflow deve tentar corrigir/validar antes de enviar ao backend.

---

# 25. Evitar dependência excessiva do n8n

Apesar de o n8n ser muito útil, não é recomendável colocar toda a lógica de negócio dentro dos workflows.

Evitar:

```text
n8n
 ├── regra de aluno
 ├── regra de relatório
 ├── regra de permissão
 ├── regra de professor
 └── regra financeira
```

Preferir:

```text
n8n
 └── chama API

Spring Boot
 ├── regras
 ├── validações
 ├── autorização
 └── persistência
```

Isso torna o sistema mais fácil de manter e permite substituir o Telegram ou o n8n no futuro.

---

# 26. Telegram como MVP, não necessariamente como produto final

Uma grande vantagem dessa abordagem é que o Telegram pode ser considerado apenas um **canal de entrada**.

No futuro, o mesmo backend pode receber áudio de:

```text
Telegram
   │
   ├── n8n
   │
   ▼
Spring Boot
```

ou:

```text
Web App
   │
   ▼
Spring Boot
```

ou futuramente:

```text
WhatsApp
   │
   ▼
n8n
   │
   ▼
Spring Boot
```

Assim, o domínio do sistema não fica acoplado ao Telegram.

---

# 27. MVP recomendado

O MVP passa a ter:

| Funcionalidade | MVP |
|---|---|
| Login | ✅ |
| Cadastro de professores | ✅ |
| Cadastro de alunos | ✅ |
| Bot Telegram | ⭐ Principal |
| Vinculação Telegram ↔ Professor | ⭐ Principal |
| Seleção de aluno pelo Telegram | ⭐ Principal |
| Envio de áudio | ⭐ Principal |
| Transcrição | ⭐ Principal |
| Estruturação por IA | ⭐ Principal |
| Refinamento conversacional | ⭐ Principal |
| Aprovação pelo Telegram | ⭐ Principal |
| Histórico de relatórios | ✅ |
| Interface web administrativa | ✅ |
| Relatório semestral | ⭐ Principal |
| Geração automática semestral | ❌ Inicialmente |
| WhatsApp | ❌ |
| Financeiro | ❌ |
| Aplicativo mobile nativo | ❌ |

---

# 28. Experiência ideal do professor

O objetivo é chegar a algo próximo disso:

```text
Professor:
 /relatorio

Bot:
 Qual aluno?

Professor:
 Maria Souza

Bot:
 🎙️ Envie o áudio da aula.

Professor:
 [ÁUDIO]

Bot:
 Processando...

Bot:
 Relatório gerado:

 Conteúdo:
 Música X

 Evolução:
 Melhora na mão direita.

 Dificuldade:
 Ritmo na segunda parte.

 Tarefa:
 Exercício de ritmo.

 Deseja alterar alguma coisa?

 [✏️ Editar]
 [✅ Aprovar]

Professor:
 [✅ Aprovar]

Bot:
 ✅ Relatório salvo com sucesso.
```

Esse fluxo é simples, rápido e possui baixo atrito.

---

# 29. Relatório semestral no Telegram

Experiência:

```text
Professor:
/relatorio_semestral

Bot:
 Qual aluno?

Professor:
 Maria Souza

Bot:
 Encontrei 24 relatórios nos últimos 6 meses.
 Deseja gerar o relatório?

 [Gerar]

Professor:
 [Gerar]

Bot:
 Analisando histórico...

 Bot:
 Relatório semestral gerado.

 [📄 Visualizar]
 [✏️ Editar]
 [✅ Aprovar]
```

---

# 30. Segurança

O bot precisa considerar:

- Telegram User ID
- Autenticação da conta
- Autorização por professor
- Controle de acesso aos alunos
- Tokens seguros
- HTTPS
- Secrets fora do código
- Variáveis de ambiente
- Backup
- Logs sem dados sensíveis desnecessários
- Controle de acesso ao n8n
- Controle de acesso ao PostgreSQL

O n8n deve ser protegido e não deve ficar exposto publicamente sem autenticação adequada.

---

# 31. LGPD e dados de alunos

Como o sistema poderá trabalhar com dados de alunos e possivelmente menores de idade, privacidade deve ser considerada desde o início.

É necessário definir:

- Quais dados serão enviados para IA
- Quanto tempo os áudios serão mantidos
- Quem pode acessar os relatórios
- Quem pode excluir dados
- Política de retenção
- Controle de permissões
- Procedimentos para menores de idade
- Tratamento dos dados pelos fornecedores de IA
- Backups
- Logs e auditoria

O áudio original não precisa necessariamente ser mantido indefinidamente.

Uma estratégia possível é permitir:

```text
Áudio original
     │
     ▼
Transcrição
     │
     ▼
Relatório
     │
     ▼
Política de retenção
     │
     └── Excluir áudio após período definido
```

Essa decisão deve ser tomada com o cliente e de acordo com os requisitos jurídicos aplicáveis.

---

# 32. Custos

O modelo é interessante porque o consumo de IA é relativamente previsível.

Por aula:

```text
Áudio
  ↓
Transcrição
  ↓
Estruturação
  ↓
Relatório
```

A cada 6 meses:

```text
Relatórios
  ↓
Consolidação
  ↓
Relatório semestral
```

Os principais custos serão:

- Servidor para os containers
- PostgreSQL
- Armazenamento
- n8n
- Telegram
- API de transcrição
- API de LLM
- Backup
- Domínio
- Monitoramento

Durante o MVP, medir:

- custo por áudio;
- custo por aula;
- custo por aluno;
- custo do relatório semestral;
- custo mensal total.

---

# 33. Critério de sucesso

O MVP será considerado validado se:

1. O professor conseguir registrar uma aula rapidamente pelo Telegram.
2. A transcrição for suficientemente boa.
3. A IA interpretar corretamente as informações pedagógicas.
4. O professor fizer poucas correções.
5. O relatório final economizar tempo.
6. O relatório semestral representar adequadamente a evolução do aluno.
7. O professor confiar no sistema.
8. O fluxo puder ser utilizado repetidamente sem gerar fricção.

A métrica principal continua sendo:

> **Tempo gasto pelo professor antes vs. depois do sistema.**

---

# 34. Roadmap

## Fase 1 — Prova do fluxo

Antes de construir todo o sistema:

- [ ] Criar bot Telegram
- [ ] Configurar n8n
- [ ] Receber áudio
- [ ] Transcrever
- [ ] Enviar para IA
- [ ] Gerar relatório
- [ ] Responder no Telegram

Objetivo:

> Validar se a IA consegue transformar a fala do professor em um relatório realmente útil.

---

## Fase 2 — MVP funcional

- [ ] Spring Boot
- [ ] PostgreSQL
- [ ] Cadastro de professores
- [ ] Cadastro de alunos
- [ ] Vinculação Telegram
- [ ] Registro de aulas
- [ ] Histórico
- [ ] Aprovação
- [ ] Relatório semestral
- [ ] React administrativo
- [ ] Docker Compose

---

## Fase 3 — Refinamento

- [ ] Conversa inteligente
- [ ] Perguntas automáticas quando faltarem informações
- [ ] Personalização do estilo do professor
- [ ] Melhoria dos prompts
- [ ] Controle de custos
- [ ] Logs
- [ ] Monitoramento
- [ ] Backup automatizado

---

## Fase 4 — Evolução

Possibilidades:

- [ ] WhatsApp
- [ ] Aplicativo
- [ ] Notificações
- [ ] Agenda
- [ ] Presença
- [ ] Financeiro
- [ ] Dashboard
- [ ] Análise de evolução
- [ ] Sugestão de exercícios
- [ ] Objetivos personalizados

---

# 35. Decisão arquitetural final

A arquitetura recomendada para o MVP é:

```text
                    ┌───────────────────┐
                    │     PROFESSOR     │
                    └─────────┬─────────┘
                              │
                              ▼
                    ┌───────────────────┐
                    │   TELEGRAM BOT    │
                    └─────────┬─────────┘
                              │
                              ▼
                    ┌───────────────────┐
                    │       n8n         │
                    │   Orquestração    │
                    └─────────┬─────────┘
                              │
                ┌─────────────┼─────────────┐
                ▼             ▼             ▼
          Speech-to-Text     LLM       Spring Boot
                │             │             │
                └──────┬──────┘             │
                       ▼                    │
                 Dados estruturados         │
                       │                    │
                       └─────────┬──────────┘
                                 ▼
                         ┌───────────────┐
                         │  PostgreSQL   │
                         └───────────────┘

                         INFRAESTRUTURA
                              │
                              ▼
                            Docker
```

### Responsabilidades

**Telegram**
> Interface rápida do professor.

**n8n**
> Orquestração, automação, integração e refinamento conversacional.

**IA**
> Transcrição, interpretação, estruturação e geração.

**Spring Boot**
> Domínio, regras de negócio, segurança e API.

**PostgreSQL**
> Fonte oficial dos dados.

**React**
> Administração e visualização.

**Docker**
> Execução e padronização da infraestrutura.

---

# 36. Conclusão

A inclusão do **n8n + Telegram** torna o MVP ainda mais interessante.

Em vez de começar construindo uma interface sofisticada para gravação de aulas, podemos validar primeiro a experiência mais importante:

> **Professor manda um áudio pelo Telegram e recebe um relatório pronto para revisar.**

O n8n funciona como uma ponte entre o professor e a inteligência artificial.

O fluxo ideal é:

```text
Professor
   ↓
Telegram
   ↓
n8n
   ↓
Transcrição
   ↓
IA
   ↓
Refinamento
   ↓
Spring Boot
   ↓
PostgreSQL
   ↓
Relatório
   ↓
Telegram
   ↓
Professor aprova
```

Depois de 6 meses:

```text
PostgreSQL
   ↓
Histórico das aulas
   ↓
n8n
   ↓
IA
   ↓
Relatório semestral
   ↓
Telegram
   ↓
Professor revisa
   ↓
Aprova
```

Essa arquitetura mantém uma separação saudável:

> **n8n orquestra, Spring Boot governa o negócio, PostgreSQL guarda a verdade e a IA interpreta e gera conteúdo.**

Além disso, o Telegram é apenas um canal. O backend não precisa saber se a solicitação veio do Telegram, de uma aplicação web ou futuramente do WhatsApp.

Isso reduz o risco arquitetural e permite validar a ideia rapidamente antes de investir em uma interface mais complexa.
