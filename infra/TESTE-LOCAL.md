# Passo a passo: subir e testar a aplicação localmente

Guia de teste manual da stack completa (Postgres + backend + n8n + frontend) no Windows.

> Este arquivo vive em `infra/`, que está no `.gitignore` (scripts locais de setup). Se quiser
> versioná-lo para o time, mova para `docs/` ou `infrastructure/`.

---

## 0. Pré-requisitos

- Docker Desktop **rodando** (ícone verde / status `Running`).
- PowerShell aberto na raiz do repositório.
- `infrastructure/.env` preenchido. Se não existir:

```bash
Copy-Item infrastructure\.env.example infrastructure\.env
```

Preencha no mínimo (o script bloqueia se algum estiver vazio ou com `change-me`):
`POSTGRES_PASSWORD`, `JWT_SECRET` (≥ 32 caracteres), `CPF_ENCRYPTION_KEY`, `N8N_SERVICE_TOKEN`,
`TELEGRAM_BOT_TOKEN`, `STT_API_KEY`, `LLM_API_KEY`, `N8N_BASIC_AUTH_USER`, `N8N_BASIC_AUTH_PASSWORD`.

Para um teste puramente local, os tokens de integração externa (Telegram/STT/LLM) podem ser
valores fictícios — só os fluxos do n8n que chamam esses provedores vão falhar.

---

## 1. Subir tudo (primeira vez)

```bash
.\infra\setup.ps1 -Reset -Seed
```

O que cada etapa faz:

| Etapa | O que acontece |
|---|---|
| Pré-requisitos | Confere `docker` no PATH e daemon respondendo |
| `.env` | Valida placeholders e tamanho do `JWT_SECRET` |
| `-Reset` | `docker compose down -v` — apaga volumes, banco sobe vazio |
| Build | `docker compose build` (Maven + npm; na primeira vez leva vários minutos) |
| Up | `docker compose up -d --wait` — o **próprio compose** espera os healthchecks |
| Smoke tests | GET no actuator, no `/healthz` do n8n e no frontend, a partir do host |
| Flyway | Consulta `flyway_schema_history` e mostra a versão do schema (deve ser **V11**) |
| `-Seed` | Aplica `infra/seed-dev.sql` (administrador + professor de teste) |

Saída esperada no final:

```
==> Resumo
  [OK] backend/actuator
  [OK] n8n/healthz
  [OK] frontend
  [OK] flyway
  [OK] seed

Aplicacao de pe! Todos os servicos estao saudaveis.
```

### Nas próximas vezes

```bash
.\infra\setup.ps1
```

Rebuilda as imagens mas **mantém** o banco. Outras opções:

| Comando | Quando usar |
|---|---|
| `.\infra\setup.ps1 -SkipBuild` | Só reiniciar containers, sem rebuild (rápido) |
| `.\infra\setup.ps1 -Seed` | Recriar/repor o admin e o professor de teste |
| `.\infra\setup.ps1 -Reset -Seed` | Começar do zero (apaga banco **e** dados do n8n) |
| `.\infra\setup.ps1 -TimeoutSeconds 600` | Máquina lenta / primeira subida do n8n |

---

## 2. De onde vem o schema do banco

Não há script de criação de tabelas em `infrastructure/postgres/` — e isso é proposital:

- **Tabelas**: criadas pelo **Flyway** (`backend/src/main/resources/db/migration/V1..V11`), aplicadas
  automaticamente no boot do backend (`spring.flyway.enabled=true`).
- **`infrastructure/postgres/`** é montado em `/docker-entrypoint-initdb.d` e só roda na *primeira*
  inicialização de um volume vazio — ou seja, **antes** do Flyway. Serve apenas para `CREATE
  EXTENSION`, roles, locale. Não coloque INSERTs aqui: as tabelas ainda não existem nesse momento.
- **Dados de teste**: `infra/seed-dev.sql`, aplicado *depois* do backend ficar saudável, via
  `-Seed`.

Conferir manualmente:

```bash
docker compose -f infrastructure/docker-compose.yml exec -T postgres psql -U admin -d escola_musica -c "\dt"
```

Devem aparecer: `administrador`, `aluno`, `aula`, `flyway_schema_history`, `professor`,
`professor_aluno`, `relatorio_aula`, `relatorio_semestral`, `tentativa_autenticacao_web`,
`vinculo_telegram`.

---

## 3. Credenciais criadas pelo seed

| Para | Credencial |
|---|---|
| Interface administrativa | `admin@escola.local` / `admin123` |
| Portal do professor / `/start` no Telegram | código de vinculação `DEV-12345678` (validade 7 dias) |

Não existe endpoint para criar administrador — sem o `-Seed` **não há como logar em lugar nenhum**.

O seed **não** cria alunos de propósito: `aluno.cpf_criptografado` é cifrado pela aplicação com
`CPF_ENCRYPTION_KEY`; inserir por SQL geraria linhas que o backend não consegue decifrar. Cadastre
alunos pela API (passo 5).

---

## 4. Endereços

| Serviço | URL |
|---|---|
| Frontend (admin) | http://localhost:8080 |
| Portal do professor | http://localhost:8080/portal |
| Swagger / OpenAPI | http://127.0.0.1:8081/swagger-ui.html |
| Backend health | http://127.0.0.1:8081/actuator/health |
| n8n | http://127.0.0.1:5678 (basic auth do `.env`) |
| Postgres | `127.0.0.1:5432` |

O backend é publicado em `127.0.0.1:8081`, mas o navegador chama sempre `localhost:8080/api/...` —
o nginx do frontend faz proxy de `/api/` para `backend:8080` dentro da rede do compose.

---

## 5. Roteiro de teste pela API (validando ponta a ponta)

Cole no **Git Bash** (no PowerShell a sintaxe de aspas muda).

**5.1 Login do administrador**

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d '{"email":"admin@escola.local","senha":"admin123"}' | sed -E 's/.*"token":"([^"]+)".*/\1/'); echo $TOKEN
```

**5.2 Listar professores e pegar o id do professor de teste**

```bash
curl -s http://localhost:8080/api/v1/professores -H "Authorization: Bearer $TOKEN"
```

**5.3 Cadastrar um aluno vinculado a esse professor** (troque `<PROFESSOR_ID>`)

```bash
curl -s -X POST http://localhost:8080/api/v1/alunos -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"nome":"Aluno Teste","dataNascimento":"2010-05-20","cpf":"12345678909","nomeResponsavel":"Mae Teste","professorIds":["<PROFESSOR_ID>"]}'
```

Esperado: **HTTP 201** com o id do aluno e `"menorDeIdade": true`.

**5.4 Login no portal do professor** (mesmo código do Telegram; o login web **não** consome o código)

```bash
PT=$(curl -s -X POST http://localhost:8080/api/v1/professor/auth/vincular -H "Content-Type: application/json" -d '{"codigoVinculacao":"DEV-12345678"}' | sed -E 's/.*"token":"([^"]+)".*/\1/'); echo $PT
```

**5.5 Ver os alunos do professor autenticado**

```bash
curl -s http://localhost:8080/api/v1/professor/alunos -H "Authorization: Bearer $PT"
```

Esperado: a lista contendo `Aluno Teste`.

**5.6 Rate limit do login (FR-004a)** — 5 tentativas erradas seguidas devem passar a responder 429:

```bash
for i in 1 2 3 4 5 6; do curl -s -o /dev/null -w "tentativa $i -> %{http_code}\n" -X POST http://localhost:8080/api/v1/professor/auth/vincular -H "Content-Type: application/json" -d '{"codigoVinculacao":"CODIGO-ERRADO"}'; done
```

Depois disso o código correto também fica bloqueado por `WEB_LOGIN_LOCKOUT_MINUTES` (15 min) para
essa origem. Para desbloquear no teste:

```bash
docker compose -f infrastructure/docker-compose.yml exec -T postgres psql -U admin -d escola_musica -c "DELETE FROM tentativa_autenticacao_web;"
```

**5.7 Emitir um novo código de vinculação** (troque `<PROFESSOR_ID>`)

```bash
curl -s -X POST http://localhost:8080/api/v1/professores/<PROFESSOR_ID>/codigo-vinculacao -H "Authorization: Bearer $TOKEN"
```

---

## 6. Teste pela interface

1. http://localhost:8080 → login `admin@escola.local` / `admin123` → cadastre professor e aluno.
2. Copie o código de vinculação do professor recém-criado.
3. http://localhost:8080/portal → cole o código → deve abrir a lista de alunos do professor.
4. Fluxos de áudio/relatório dependem do n8n com STT/LLM reais configurados.

---

## 7. Diagnóstico quando algo falha

```bash
docker compose -f infrastructure/docker-compose.yml ps
```

```bash
docker compose -f infrastructure/docker-compose.yml logs backend --tail 100
```

| Sintoma | Causa provável |
|---|---|
| `up --wait` estoura o timeout | Primeira subida do n8n (migrações internas). Use `-TimeoutSeconds 600` |
| backend reinicia em loop | Variável obrigatória ausente no `.env` (`JWT_SECRET`, `CPF_ENCRYPTION_KEY`, ...). O log mostra `Could not resolve placeholder` |
| backend sobe mas `/api` dá 500 | Veja o log; erros de banco aparecem como `PSQLException` |
| `Validate failed` do Flyway | Migração alterada depois de aplicada. Rode com `-Reset` |
| Porta 8080/5432 ocupada | Outro processo/stack no ar: `docker compose ... down` ou mude a porta publicada |
| 401 em `/api/v1/...` | Token expirado (admin: 2h; professor: 12h de inatividade). Refaça o login |

Derrubar tudo:

```bash
docker compose -f infrastructure/docker-compose.yml down
```

Derrubar apagando os dados:

```bash
docker compose -f infrastructure/docker-compose.yml down -v
```
