import { useEffect, useRef, useState } from 'react';

/**
 * Gravacao de audio da aula pelo navegador (T041), com fallback automatico para upload de arquivo.
 *
 * Usa as APIs nativas `MediaRecorder` + `getUserMedia` (research.md secao 5) — sem biblioteca
 * externa. Quando o navegador nao suporta `MediaRecorder` OU o professor nega a permissao de
 * microfone, o componente cai para o modo de upload e diz o que aconteceu com uma mensagem
 * acionavel, em vez de simplesmente falhar (research.md secao 8).
 *
 * Todos os estados (gravando/parado/erro) sao anunciados por `aria-live` para leitores de tela.
 */

export interface GravadorAudioProps {
  /** Chamado com o audio pronto para envio (blob gravado ou arquivo escolhido). */
  onAudioPronto: (audio: Blob, nomeArquivo: string) => void;
  desabilitado?: boolean;
}

type EstadoGravacao = 'inativo' | 'gravando' | 'gravado';

function suportaGravacao(): boolean {
  return (
    typeof window !== 'undefined' &&
    typeof window.MediaRecorder !== 'undefined' &&
    typeof navigator !== 'undefined' &&
    navigator.mediaDevices !== undefined &&
    typeof navigator.mediaDevices.getUserMedia === 'function'
  );
}

function GravadorAudio({ onAudioPronto, desabilitado = false }: GravadorAudioProps): JSX.Element {
  const [estado, setEstado] = useState<EstadoGravacao>('inativo');
  const [somenteUpload, setSomenteUpload] = useState<boolean>(() => !suportaGravacao());
  const [aviso, setAviso] = useState<string | null>(() =>
    suportaGravacao()
      ? null
      : 'Este navegador não permite gravar áudio. Envie um arquivo de áudio da aula.',
  );
  const [nomeSelecionado, setNomeSelecionado] = useState<string | null>(null);

  const gravadorRef = useRef<MediaRecorder | null>(null);
  const pedacosRef = useRef<Blob[]>([]);
  const trilhasRef = useRef<MediaStream | null>(null);

  // Libera o microfone se o componente sair da tela no meio de uma gravacao.
  useEffect(
    () => () => {
      trilhasRef.current?.getTracks().forEach((trilha) => trilha.stop());
    },
    [],
  );

  async function iniciarGravacao(): Promise<void> {
    setAviso(null);
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      trilhasRef.current = stream;
      pedacosRef.current = [];

      const gravador = new MediaRecorder(stream);
      gravador.ondataavailable = (evento: BlobEvent) => {
        if (evento.data.size > 0) {
          pedacosRef.current.push(evento.data);
        }
      };
      gravador.onstop = () => {
        const blob = new Blob(pedacosRef.current, { type: 'audio/webm' });
        stream.getTracks().forEach((trilha) => trilha.stop());
        setEstado('gravado');
        setNomeSelecionado('gravacao-da-aula.webm');
        onAudioPronto(blob, 'gravacao-da-aula.webm');
      };

      gravadorRef.current = gravador;
      gravador.start();
      setEstado('gravando');
    } catch {
      // Permissao negada ou microfone indisponivel: em vez de travar, oferece o caminho alternativo.
      setSomenteUpload(true);
      setEstado('inativo');
      setAviso('Microfone bloqueado ou indisponível. Envie um arquivo de áudio da aula.');
    }
  }

  function pararGravacao(): void {
    gravadorRef.current?.stop();
    gravadorRef.current = null;
  }

  function selecionarArquivo(evento: React.ChangeEvent<HTMLInputElement>): void {
    const arquivo = evento.target.files?.[0];
    if (!arquivo) {
      return;
    }
    setNomeSelecionado(arquivo.name);
    setEstado('gravado');
    onAudioPronto(arquivo, arquivo.name);
  }

  const rotuloEstado =
    estado === 'gravando'
      ? 'Gravando o áudio da aula.'
      : estado === 'gravado'
        ? `Áudio pronto para envio: ${nomeSelecionado ?? ''}`
        : 'Nenhum áudio selecionado.';

  return (
    <div className="portal-card">
      <h3>Áudio da aula</h3>

      {!somenteUpload && (
        <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
          {estado !== 'gravando' ? (
            <button
              type="button"
              className="portal-botao"
              onClick={() => void iniciarGravacao()}
              disabled={desabilitado}
            >
              Gravar pelo navegador
            </button>
          ) : (
            <button type="button" className="portal-botao" onClick={pararGravacao}>
              Parar gravação
            </button>
          )}
        </div>
      )}

      <div style={{ marginTop: 12 }}>
        <label className="portal-rotulo" htmlFor="portal-audio-arquivo">
          {somenteUpload ? 'Arquivo de áudio da aula' : 'Ou envie um arquivo de áudio'}
        </label>
        <input
          id="portal-audio-arquivo"
          type="file"
          accept="audio/*"
          onChange={selecionarArquivo}
          disabled={desabilitado || estado === 'gravando'}
        />
      </div>

      <p role="status" aria-live="polite" style={{ marginTop: 12 }}>
        {rotuloEstado}
      </p>

      {aviso && (
        <p className="portal-erro" role="alert">
          {aviso}
        </p>
      )}
    </div>
  );
}

export default GravadorAudio;
