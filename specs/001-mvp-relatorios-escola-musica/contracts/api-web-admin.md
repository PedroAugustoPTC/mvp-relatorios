# Contract: API REST — Interface Web Administrativa

Consumida pelo frontend React. Autenticação: sessão/token de administrador (Spring Security), obrigatória
em todas as rotas exceto login. Base path sugerido: `/api/v1`.

## Autenticação

### POST /api/v1/auth/login

Autentica o administrador.

**Request**:
```json
{ "email": "admin@escola.com", "senha": "..." }
```

**Response 200**:
```json
{ "token": "jwt...", "expiraEm": "2026-08-13T20:00:00Z" }
```

**Response 401**: credenciais inválidas.

---

## Professores

### POST /api/v1/professores

Cadastra um professor e gera código de vinculação (FR-001, FR-002).

**Request**:
```json
{ "nome": "Maria Silva", "email": "maria@escola.com" }
```

**Response 201**:
```json
{
  "id": "uuid",
  "nome": "Maria Silva",
  "email": "maria@escola.com",
  "codigoVinculacao": "AB12CD",
  "codigoVinculacaoExpiraEm": "2026-08-14T00:00:00Z"
}
```

**Response 409**: e-mail já cadastrado.

### GET /api/v1/professores

Lista professores cadastrados (para associar alunos, ver vínculo Telegram).

**Response 200**: `[{ "id", "nome", "email", "vinculadoTelegram": true|false }]`

### POST /api/v1/professores/{id}/codigo-vinculacao

Reemite um código de vinculação (ex.: expirado).

**Response 200**: mesmo shape do cadastro.

---

## Alunos

### POST /api/v1/alunos

Cadastra um aluno associando um ou mais professores (FR-001, FR-001a, US2 cenário 3).

**Request**:
```json
{
  "nome": "João Pedro",
  "dataNascimento": "2014-05-10",
  "cpf": "000.000.000-00",
  "nomeResponsavel": "Ana Pedro",
  "professorIds": ["uuid-professor-1"]
}
```

**Response 201**: dados do aluno cadastrado (sem expor o CPF em claro fora do necessário).

**Response 409**: CPF já cadastrado (FR-001a — verificado via `cpfHash`).

**Response 400**: `nomeResponsavel` ausente quando `dataNascimento` indica menor de idade.

### GET /api/v1/alunos

Lista alunos, com filtro opcional por `professorId`.

### GET /api/v1/alunos/{id}/historico

Histórico completo (relatórios de aula + semestrais aprovados) de um aluno, ordem cronológica
(FR-017).

**Response 200**:
```json
{
  "aluno": { "id", "nome" },
  "relatorios": [
    { "tipo": "AULA", "id", "dataAula", "status", "pdfUrl", "aprovadoEm" },
    { "tipo": "SEMESTRAL", "id", "periodoInicio", "periodoFim", "status", "pdfUrl", "aprovadoEm" }
  ]
}
```

---

## Erros

Formato padrão de erro para todas as rotas:

```json
{ "codigo": "ALUNO_CPF_DUPLICADO", "mensagem": "Já existe um aluno cadastrado com este CPF." }
```
