import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import GravadorAudio from '../components/GravadorAudio';

/**
 * Testes do gravador de audio (T049), com foco no fallback exigido pela spec: navegadores sem
 * `MediaRecorder` (jsdom, por exemplo) e permissao de microfone negada precisam levar o professor
 * ao upload de arquivo, nunca a um beco sem saida.
 */
describe('GravadorAudio', () => {
  const mediaRecorderOriginal = (window as unknown as { MediaRecorder?: unknown }).MediaRecorder;

  beforeEach(() => {
    vi.restoreAllMocks();
  });

  afterEach(() => {
    (window as unknown as { MediaRecorder?: unknown }).MediaRecorder = mediaRecorderOriginal;
  });

  it('cai para upload de arquivo quando o navegador nao suporta gravacao', () => {
    // jsdom nao implementa MediaRecorder — exatamente o cenario do fallback.
    render(<GravadorAudio onAudioPronto={vi.fn()} />);

    expect(screen.queryByRole('button', { name: /gravar pelo navegador/i })).not.toBeInTheDocument();
    expect(screen.getByLabelText(/arquivo de áudio da aula/i)).toBeInTheDocument();
    expect(screen.getByRole('alert')).toHaveTextContent(/não permite gravar áudio/i);
  });

  it('entrega o arquivo escolhido pelo professor', async () => {
    const onAudioPronto = vi.fn();
    render(<GravadorAudio onAudioPronto={onAudioPronto} />);

    const arquivo = new File(['conteudo-do-audio'], 'aula.mp3', { type: 'audio/mpeg' });
    await userEvent.upload(screen.getByLabelText(/arquivo de áudio da aula/i), arquivo);

    expect(onAudioPronto).toHaveBeenCalledWith(arquivo, 'aula.mp3');
    expect(screen.getByRole('status')).toHaveTextContent('aula.mp3');
  });

  it('oferece a gravacao quando o navegador suporta MediaRecorder', () => {
    (window as unknown as { MediaRecorder: unknown }).MediaRecorder = class {};
    Object.defineProperty(navigator, 'mediaDevices', {
      configurable: true,
      value: { getUserMedia: vi.fn() },
    });

    render(<GravadorAudio onAudioPronto={vi.fn()} />);

    expect(screen.getByRole('button', { name: /gravar pelo navegador/i })).toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('cai para upload com mensagem acionavel quando a permissao de microfone e negada', async () => {
    (window as unknown as { MediaRecorder: unknown }).MediaRecorder = class {};
    Object.defineProperty(navigator, 'mediaDevices', {
      configurable: true,
      value: { getUserMedia: vi.fn().mockRejectedValue(new Error('NotAllowedError')) },
    });

    render(<GravadorAudio onAudioPronto={vi.fn()} />);
    await userEvent.click(screen.getByRole('button', { name: /gravar pelo navegador/i }));

    expect(await screen.findByRole('alert')).toHaveTextContent(/microfone bloqueado/i);
    expect(screen.getByLabelText(/arquivo de áudio da aula/i)).toBeInTheDocument();
  });
});
