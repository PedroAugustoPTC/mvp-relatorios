-- Corrige o pdf_url dos relatorios ja gerados.
--
-- Ate aqui, PdfGeracaoService gravava o caminho de FILESYSTEM do container (ex.:
-- /app/storage/pdfs/relatorio-aula-<uuid>-v1.pdf). Esse valor nao corresponde a nenhuma rota HTTP:
-- o navegador o resolvia contra a origem do frontend e o fallback SPA do Nginx devolvia o
-- index.html (tela de login) no lugar do PDF. O servico agora grava a rota do
-- ArquivoRelatorioController; esta migracao reescreve os registros antigos para o mesmo formato,
-- preservando o nome do arquivo (que continua valido no volume de storage).

-- A classe de caracteres e escrita como [\\/] e nao [/\]: nas expressoes regulares do Postgres a
-- barra invertida continua sendo escape DENTRO dos colchetes, entao [/\] escaparia o ']' e a
-- expressao ficaria com o bracket aberto ("brackets [] not balanced").

UPDATE relatorio_aula
SET pdf_url = '/api/v1/arquivos/relatorios/' || regexp_replace(pdf_url, '^.*[\\/]', '')
WHERE pdf_url IS NOT NULL
  AND pdf_url NOT LIKE '/api/v1/arquivos/relatorios/%';

UPDATE relatorio_semestral
SET pdf_url = '/api/v1/arquivos/relatorios/' || regexp_replace(pdf_url, '^.*[\\/]', '')
WHERE pdf_url IS NOT NULL
  AND pdf_url NOT LIKE '/api/v1/arquivos/relatorios/%';
