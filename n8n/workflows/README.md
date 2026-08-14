# n8n Workflows

Este diretório contém os workflows de orquestração n8n usados pelo MVP Sistema de Relatórios
para Escola de Música. Os workflows conversam com professores via Telegram e chamam a API interna
do backend (Spring Boot) em `/internal/v1/**` — ver
`specs/001-mvp-relatorios-escola-musica/contracts/api-n8n-integration.md` para o contrato completo
de cada endpoint consumido.

## Arquivos esperados

Cada workflow é exportado como um único arquivo JSON a partir do editor do n8n e commitado neste
diretório. Os nomes de arquivo são fixos:

| Arquivo | Introduzido em | Descrição |
|---|---|---|
| `RegistroAula.json` | Fase US1 (T053) | Fluxo de registro de aula: seleção de aluno, recebimento de áudio, transcrição, estruturação do relatório, perguntas de acompanhamento, exibição do PDF e aprovação/revisão. |
| `VinculacaoTelegram.json` | Fase US2 (T067) | Primeiro contato do professor no bot: solicita código de vinculação e chama `POST /internal/v1/telegram/vinculacao`. |
| `RelatorioSemestral.json` | Fase US3 (T083) | Comando `/relatorio_semestral`: seleção de aluno, menu de semestre, contagem de relatórios de aula, geração e revisão/aprovação do relatório semestral. |
| `HistoricoTelegram.json` | Fase US4 (T090) | Comando de consulta de histórico de relatórios aprovados via Telegram. |

**Nenhum destes arquivos existe ainda** — são adicionados incrementalmente, à medida que os
endpoints de backend correspondentes forem implementados nas fases futuras (ver
`specs/001-mvp-relatorios-escola-musica/tasks.md`). Não crie arquivos JSON vazios/placeholder para
eles antecipadamente.

## Convenção de versionamento

- Cada workflow é editado na UI do n8n e depois **exportado como JSON** e commitado neste
  diretório — o JSON exportado é a fonte da verdade versionada em git, não o estado salvo apenas
  no banco interno do n8n.
- Sempre que um workflow for alterado na UI, repita o processo de exportação e commit da nova
  versão do arquivo, para que o histórico do git reflita a evolução do workflow.
- Evite editar o JSON exportado manualmente fora de casos pontuais (ex.: ajustar um `id` ou
  `webhookId`); prefira sempre editar no editor visual do n8n e reexportar.

### Como exportar (commitar uma alteração)

Na UI do n8n: abra o workflow → menu **⋮** (três pontos) → **Download** → salve sobrescrevendo o
arquivo correspondente neste diretório.

Ou via n8n CLI:

```bash
n8n export:workflow --id=<workflow_id> --output=n8n/workflows/RegistroAula.json
```

### Como (re)importar

Na UI do n8n: **Workflows** → menu **⋮** → **Import from File** → selecione o arquivo `.json`
deste diretório.

Ou via n8n CLI:

```bash
n8n import:workflow --input=n8n/workflows/RegistroAula.json
```

## Princípio de responsabilidade: orquestração, não persistência

Os workflows n8n **apenas orquestram** a conversa no Telegram e fazem chamadas HTTP para os
endpoints internos do backend (`/internal/v1/**`, autenticados via header
`X-N8N-Service-Token`). Isso segue o Princípio IV (Clean Architecture / separação de
responsabilidades) do projeto:

- **n8n**: recebe/envia mensagens do Telegram, decide o fluxo de conversa, chama a API interna do
  backend e apresenta respostas ao professor.
- **Spring Boot (backend)**: é o único componente que aplica regras de negócio e escreve no
  PostgreSQL. Todas as escritas — vinculação, transcrição, estruturação de relatório, aprovação,
  etc. — passam pelos endpoints `/internal/v1/**` descritos no contrato.

**O n8n nunca escreve diretamente no PostgreSQL.** Nenhum workflow deve conter um node de banco de
dados (Postgres node) que grave dados de domínio; toda persistência é delegada ao backend.
