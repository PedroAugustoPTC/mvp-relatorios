-- Suporte ao fluxo consolidado do bot do Telegram (canal exclusivo de registro de aula, spec de
-- redesenho da integracao Telegram): o Telegram Trigger do n8n so pode ter uma execucao por
-- mensagem recebida, sem memoria da mensagem anterior. Para o professor poder escolher o aluno numa
-- mensagem e enviar o audio na proxima, essa selecao precisa ficar persistida no backend (unica
-- fonte de verdade) em vez de em algum estado dentro do n8n.

ALTER TABLE vinculo_telegram
  ADD COLUMN aluno_selecionado_id UUID,
  ADD COLUMN aluno_selecionado_em TIMESTAMPTZ;
