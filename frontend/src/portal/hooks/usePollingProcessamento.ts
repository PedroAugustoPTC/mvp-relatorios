import { useCallback, useEffect, useRef, useState } from 'react';
import { consultarRelatorioAula, RelatorioAulaPortal } from '../services/relatorioPortalService';

/**
 * Acompanha o processamento assincrono de um relatorio de aula (T043).
 *
 * Transcricao e estruturacao por IA levam alguns segundos, entao a tela nunca pode ficar parada sem
 * explicacao (research.md secao 8): este hook expoe o `passo` atual em texto — anunciado via
 * `aria-live` pela pagina — junto com o relatorio ja carregado.
 *
 * O polling para sozinho assim que o relatorio chega a um estado que exige acao do professor
 * (perguntas pendentes, PDF pronto para revisao, aprovado ou cancelado), para nao ficar batendo no
 * backend indefinidamente.
 */

const INTERVALO_MS = 2000;

export type PassoProcessamento =
  | 'ocioso'
  | 'enviando'
  | 'processando'
  | 'aguardando-informacao'
  | 'pronto'
  | 'erro';

const ROTULOS: Record<PassoProcessamento, string> = {
  ocioso: '',
  enviando: 'Enviando o áudio...',
  processando: 'Transcrevendo o áudio e estruturando o relatório...',
  'aguardando-informacao': 'Faltam algumas informações para concluir o relatório.',
  pronto: 'Relatório pronto para conferência.',
  erro: 'Não foi possível processar o áudio.',
};

/** Estados terminais: nao ha mais nada acontecendo em segundo plano. */
function estadoFinal(relatorio: RelatorioAulaPortal): boolean {
  if (relatorio.perguntasPendentes.length > 0) {
    return true;
  }
  return ['PENDENTE_REVISAO', 'APROVADO', 'CANCELADO'].includes(relatorio.status);
}

export interface UsePollingProcessamento {
  passo: PassoProcessamento;
  rotuloPasso: string;
  relatorio: RelatorioAulaPortal | null;
  erro: string | null;
  iniciar: (relatorioId: string) => void;
  definirRelatorio: (relatorio: RelatorioAulaPortal) => void;
  definirPasso: (passo: PassoProcessamento) => void;
  definirErro: (mensagem: string | null) => void;
}

export function usePollingProcessamento(): UsePollingProcessamento {
  const [passo, setPasso] = useState<PassoProcessamento>('ocioso');
  const [relatorio, setRelatorio] = useState<RelatorioAulaPortal | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [relatorioIdEmPolling, setRelatorioIdEmPolling] = useState<string | null>(null);
  const cancelado = useRef(false);

  useEffect(() => {
    cancelado.current = false;
    return () => {
      cancelado.current = true;
    };
  }, []);

  const aplicarRelatorio = useCallback((atual: RelatorioAulaPortal): boolean => {
    setRelatorio(atual);
    if (atual.perguntasPendentes.length > 0) {
      setPasso('aguardando-informacao');
    } else if (estadoFinal(atual)) {
      setPasso('pronto');
    }
    return estadoFinal(atual);
  }, []);

  useEffect(() => {
    if (!relatorioIdEmPolling) {
      return undefined;
    }

    let ativo = true;
    const temporizador = setInterval(() => {
      void (async () => {
        try {
          const atual = await consultarRelatorioAula(relatorioIdEmPolling);
          if (!ativo || cancelado.current) {
            return;
          }
          if (aplicarRelatorio(atual)) {
            setRelatorioIdEmPolling(null);
          }
        } catch (e) {
          if (!ativo) {
            return;
          }
          setErro(e instanceof Error ? e.message : ROTULOS.erro);
          setPasso('erro');
          setRelatorioIdEmPolling(null);
        }
      })();
    }, INTERVALO_MS);

    return () => {
      ativo = false;
      clearInterval(temporizador);
    };
  }, [relatorioIdEmPolling, aplicarRelatorio]);

  const iniciar = useCallback((relatorioId: string): void => {
    setErro(null);
    setPasso('processando');
    setRelatorioIdEmPolling(relatorioId);
  }, []);

  return {
    passo,
    rotuloPasso: ROTULOS[passo],
    relatorio,
    erro,
    iniciar,
    definirRelatorio: (atual: RelatorioAulaPortal) => {
      aplicarRelatorio(atual);
    },
    definirPasso: setPasso,
    definirErro: setErro,
  };
}
