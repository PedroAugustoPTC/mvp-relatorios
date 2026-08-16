-- =============================================================================
-- Seed de DESENVOLVIMENTO - nao use em producao.
--
-- Aplicado por `infra\setup.ps1 -Seed`, sempre DEPOIS que o backend subiu (o Flyway cria as
-- tabelas no boot do backend; este arquivo apenas insere dados).
--
-- NAO coloque este arquivo em infrastructure/postgres/ (/docker-entrypoint-initdb.d): aquele
-- diretorio roda na PRIMEIRA inicializacao do volume do Postgres, ou seja, ANTES do Flyway - as
-- tabelas ainda nao existem nesse momento e os INSERTs falhariam.
--
-- O que este seed cria:
--   1. Um administrador, para logar na interface web administrativa. Nao existe endpoint para
--      criar administrador: sem este INSERT nao ha como fazer login em lugar nenhum.
--   2. Um professor com codigo de vinculacao valido, para logar no portal do professor e para
--      testar o /start do bot no Telegram.
--
-- O que este seed deliberadamente NAO cria:
--   - Alunos. A coluna aluno.cpf_criptografado guarda o CPF cifrado pela aplicacao com
--     CPF_ENCRYPTION_KEY, e aluno.cpf_hash e derivado do CPF do mesmo jeito. Inserir por SQL
--     produziria linhas que a aplicacao nao consegue decifrar. Cadastre alunos pela API
--     (POST /api/v1/alunos), que faz a cifragem corretamente.
--
-- E idempotente: pode rodar quantas vezes quiser (ON CONFLICT ... DO UPDATE).
-- =============================================================================

-- NOTICEs do psql saem em stderr; no PowerShell isso vira ruido de NativeCommandError mesmo com
-- o comando bem-sucedido. Silenciamos tudo abaixo de WARNING.
SET client_min_messages = warning;

BEGIN;

-- pgcrypto ja e criada pela migracao V1; garantimos aqui para o caso de rodar o seed isolado.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- -----------------------------------------------------------------------------
-- 1. Administrador da interface web
--    Login: admin@escola.local / senha: admin123
--    crypt(..., gen_salt('bf', 10)) gera um hash BCrypt no formato $2a$10$..., que e exatamente
--    o que o BCryptPasswordEncoder do Spring Security valida em AutenticarAdminUseCase.
-- -----------------------------------------------------------------------------
INSERT INTO administrador (email, senha_hash)
VALUES ('admin@escola.local', crypt('admin123', gen_salt('bf', 10)))
ON CONFLICT (email) DO UPDATE
    SET senha_hash = EXCLUDED.senha_hash;

-- -----------------------------------------------------------------------------
-- 2. Professor de teste com codigo de vinculacao valido por 7 dias
--    O mesmo codigo serve para os dois canais (spec 002: identidade unica):
--      - portal web:  POST /api/v1/professor/auth/vincular  { "codigoVinculacao": "DEV-12345678" }
--      - Telegram:    /start DEV-12345678
--    Diferente do fluxo do Telegram, o login no portal NAO consome o codigo - ele continua
--    valido ate codigo_vinculacao_expira_em.
-- -----------------------------------------------------------------------------
INSERT INTO professor (nome, email, ativo, codigo_vinculacao, codigo_vinculacao_expira_em)
VALUES ('Professor Teste', 'professor.teste@escola.local', TRUE, 'DEV-12345678', now() + interval '7 days')
ON CONFLICT (email) DO UPDATE
    SET ativo                       = TRUE,
        codigo_vinculacao           = EXCLUDED.codigo_vinculacao,
        codigo_vinculacao_expira_em = EXCLUDED.codigo_vinculacao_expira_em,
        atualizado_em               = now();

COMMIT;

-- Resumo do que ficou disponivel.
\echo ''
\echo 'Seed aplicado:'
\echo '  admin  -> admin@escola.local / admin123'
\echo '  codigo de vinculacao do professor de teste -> DEV-12345678'
