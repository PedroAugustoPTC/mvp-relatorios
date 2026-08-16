-- V12: perguntas pendentes do relatorio de aula (FR-007).
--
-- Ate aqui as perguntas de acompanhamento retornadas pelo LLM so existiam na resposta HTTP da
-- chamada que as gerou (POST .../audio e POST .../revisar). O portal web, porem, acompanha o
-- relatorio por polling em GET .../relatorios-aula/{id} — que nao tinha de onde le-las e devolvia
-- sempre lista vazia. Resultado: um relatorio que ficou em RASCUNHO aguardando resposta do
-- professor nunca exibia as perguntas e o polling nao terminava nunca.
--
-- Persistir a lista torna o GET a fonte de verdade real do andamento, como o contrato ja previa.
-- O DEFAULT '[]' faz o backfill das linhas existentes sem migracao em duas etapas.
ALTER TABLE relatorio_aula
    ADD COLUMN perguntas_pendentes JSONB NOT NULL DEFAULT '[]'::jsonb;
