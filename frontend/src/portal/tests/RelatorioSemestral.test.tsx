import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import RelatorioSemestral from '../pages/RelatorioSemestral';
import { ApiError } from '../services/professorApiClient';
import * as relatorioPortalService from '../services/relatorioPortalService';

/** Testes do fluxo de relatorio semestral pelo portal (T074). */
describe('RelatorioSemestral', () => {
  const alunoId = '11111111-1111-1111-1111-111111111111';
  const relatorioId = '33333333-3333-3333-3333-333333333333';

  beforeEach(() => {
    vi.restoreAllMocks();
    vi.spyOn(relatorioPortalService, 'listarSemestresDisponiveis').mockResolvedValue([
      { chave: '2025-2', rotulo: '2º semestre 2025', inicio: '2025-07-01', fim: '2025-12-31' },
      { chave: '2026-1', rotulo: '1º semestre 2026', inicio: '2026-01-01', fim: '2026-06-30' },
    ]);
  });

  function renderizar(): void {
    render(
      <MemoryRouter initialEntries={[`/portal/alunos/${alunoId}/relatorio-semestral`]}>
        <Routes>
          <Route
            path="/portal/alunos/:alunoId/relatorio-semestral"
            element={<RelatorioSemestral />}
          />
        </Routes>
      </MemoryRouter>,
    );
  }

  it('apresenta os semestres disponiveis como opcoes selecionaveis', async () => {
    renderizar();

    expect(await screen.findByRole('button', { name: '1º semestre 2026' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '2º semestre 2025' })).toBeInTheDocument();
    // O semestre mais recente ja vem pre-selecionado.
    expect(screen.getByRole('button', { name: '1º semestre 2026' })).toHaveAttribute(
      'aria-pressed',
      'true',
    );
  });

  it('informa quantos relatorios de aula alimentaram a consolidacao', async () => {
    vi.spyOn(relatorioPortalService, 'gerarRelatorioSemestral').mockResolvedValue({
      quantidadeRelatoriosEncontrados: 12,
      relatorioSemestralId: relatorioId,
      status: 'PENDENTE_REVISAO',
      pdfUrl: '/storage/pdfs/semestral.pdf',
      versao: 1,
    });

    renderizar();
    await userEvent.click(await screen.findByRole('button', { name: /gerar relatório semestral/i }));

    expect(await screen.findByText(/12 relatórios de aula foram considerados/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /aprovar relatório/i })).toBeInTheDocument();
  });

  it('explica o que fazer quando nao ha relatorios de aula no periodo', async () => {
    vi.spyOn(relatorioPortalService, 'gerarRelatorioSemestral').mockRejectedValue(
      new ApiError('SEM_DADOS_NO_PERIODO', 'Sem dados', 409),
    );

    renderizar();
    await userEvent.click(await screen.findByRole('button', { name: /gerar relatório semestral/i }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      /registre as aulas do semestre antes de gerar/i,
    );
    expect(screen.queryByRole('button', { name: /aprovar relatório/i })).not.toBeInTheDocument();
  });

  it('conclui o fluxo apos a aprovacao do relatorio', async () => {
    vi.spyOn(relatorioPortalService, 'gerarRelatorioSemestral').mockResolvedValue({
      quantidadeRelatoriosEncontrados: 1,
      relatorioSemestralId: relatorioId,
      status: 'PENDENTE_REVISAO',
      pdfUrl: '/storage/pdfs/semestral.pdf',
      versao: 1,
    });
    vi.spyOn(relatorioPortalService, 'aprovarRelatorioSemestral').mockResolvedValue({
      relatorioId,
      status: 'APROVADO',
      aprovadoEm: new Date().toISOString(),
    });

    renderizar();
    await userEvent.click(await screen.findByRole('button', { name: /gerar relatório semestral/i }));
    await userEvent.click(await screen.findByRole('button', { name: /aprovar relatório/i }));

    expect(await screen.findByText(/relatório semestral aprovado/i)).toBeInTheDocument();
    expect(relatorioPortalService.aprovarRelatorioSemestral).toHaveBeenCalledWith(relatorioId, 1);
  });
});
