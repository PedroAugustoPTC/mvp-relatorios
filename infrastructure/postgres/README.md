# infrastructure/postgres

Este diretório é montado em `/docker-entrypoint-initdb.d` no contêiner `postgres` do
`infrastructure/docker-compose.yml`.

O schema real do banco de dados (tabelas `professor`, `aluno`, `professor_aluno`,
`vinculo_telegram`, `aula`, `relatorio_aula`, `relatorio_semestral`, etc.) **não** é definido
aqui — ele é gerenciado por migrações Flyway versionadas no backend, em
`backend/src/main/resources/db/migration/` (ver tasks T009–T015), e aplicado automaticamente
pelo Spring Boot ao subir o serviço `backend`.

Este diretório serve apenas para scripts de inicialização **opcionais** do PostgreSQL que
precisem rodar antes do Flyway — por exemplo, criação de extensões (`CREATE EXTENSION`), ajustes
de locale, ou criação de usuários/roles adicionais para desenvolvimento local. Scripts colocados
aqui (`.sql` ou `.sh`) são executados pela imagem oficial `postgres` apenas na primeira
inicialização de um volume de dados vazio, na ordem alfabética de seus nomes.

Se nenhum script de inicialização for necessário, este diretório pode permanecer vazio.
