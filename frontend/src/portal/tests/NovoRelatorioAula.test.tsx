import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import NovoRelatorioAula from '../pages/NovoRelatorioAula';
import * as professorPortalService from '../services/professorPortalService';
import * as relatorioPortalService from '../services/relatorioPortalService';

/** Testes do fluxo de novo relatorio de aula pelo portal (T049). */
describe('NovoRelatorioAula', () => {
  const alunoId = '11111111-1111-1111-1111-111111111111';
  const relatorioId = '22222222-2222-2222-2222-222222222222';

  beforeEach(() => {
    vi.restoreAllMocks();
    vi.spyOn(professorPortalService, 'consultarRascunhoPendente').mockResolvedValue({
      existeRascunho: false,
      tipo: null,
      relatorioId: null,
      status: null,
      canalOrigem: null,
      atualizadoEm: null,
    });
  });

  function renderizar(): void {
    render(
      <MemoryRouter initialEntries={[`/portal/alunos/${alunoId}/novo-relatorio`]}>
        <Routes>
          <Route path="/portal/alunos/:alunoId/novo-relatorio" element={<NovoRelatorioAula />} />
          <Route path="/portal/alunos/:alunoId/retomar-rascunho" element={<p>Retomar rascunho</p>} />
        </Routes>
      </MemoryRouter>,
    );
  }

  async function enviarArquivo(): Promise<void> {
    const arquivo = new File(['audio'], 'aula.mp3', { type: 'audio/mpeg' });
    await userEvent.upload(screen.getByLabelText(/arquivo de áudio da aula/i), arquivo);
    await userEvent.click(screen.getByRole('button', { name: /enviar áudio/i }));
  }

  it('envia o audio e apresenta o relatorio estruturado para conferencia', async () => {
    vi.spyOn(relatorioPortalService, 'enviarAudio').mockResolvedValue({
      relatorioId,
      status: 'PENDENTE_REVISAO',
      perguntasPendentes: [],
    });
    vi.spyOn(relatorioPortalService, 'consultarRelatorioAula').mockResolvedValue({
      relatorioId,
      status: 'PENDENTE_REVISAO',
      conteudosTrabalhados: ['Escala de Sol'],
      evolucao: 'Boa evolução',
      dificuldades: [],
      atividadesPropostas: [],
      observacoes: '',
      perguntasPendentes: [],
      pdfUrl: '/storage/pdfs/relatorio.pdf',
      versao: 1,
    });

    renderizar();
    await enviarArquivo();

    expect(
      await screen.findByRole('button', { name: /aprovar relatório/i }, { timeout: 5000 }),
    ).toBeInTheDocument();
    expect(screen.getByText('Versão 1')).toBeInTheDocument();
  });

  it('pede as informacoes faltantes antes de gerar o PDF', async () => {
    vi.spyOn(relatorioPortalService, 'enviarAudio').mockResolvedValue({
      relatorioId,
      status: 'RASCUNHO',
      perguntasPendentes: ['Qual peça foi trabalhada?'],
    });
    vi.spyOn(relatorioPortalService, 'consultarRelatorioAula').mockResolvedValue({
      relatorioId,
      status: 'RASCUNHO',
      conteudosTrabalhados: [],
      evolucao: null,
      dificuldades: [],
      atividadesPropostas: [],
      observacoes: null,
      perguntasPendentes: ['Qual peça foi trabalhada?'],
      pdfUrl: null,
      versao: 1,
    });

    renderizar();
    await enviarArquivo();

    expect(
      await screen.findByText('Qual peça foi trabalhada?', undefined, { timeout: 5000 }),
    ).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /aprovar relatório/i })).not.toBeInTheDocument();
  });

  it('mostra erro acionavel quando o audio nao pode ser processado', async () => {
    vi.spyOn(relatorioPortalService, 'enviarAudio').mockRejectedValue(
      new Error('Áudio corrompido ou em silêncio'),
    );

    renderizar();
    await enviarArquivo();

    expect(await screen.findByText('Áudio corrompido ou em silêncio')).toBeInTheDocument();
  });

  it('leva para a retomada quando ja existe um rascunho em aberto em outro canal', async () => {
    vi.spyOn(professorPortalService, 'consultarRascunhoPendente').mockResolvedValue({
      existeRascunho: true,
      tipo: 'AULA',
      relatorioId,
      status: 'PENDENTE_REVISAO',
      canalOrigem: 'TELEGRAM',
      atualizadoEm: new Date().toISOString(),
    });

    renderizar();

    expect(await screen.findByText('Retomar rascunho')).toBeInTheDocument();
  });
});
