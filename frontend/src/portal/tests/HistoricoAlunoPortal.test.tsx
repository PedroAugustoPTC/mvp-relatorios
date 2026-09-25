import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import HistoricoAlunoPortal from '../pages/HistoricoAlunoPortal';
import * as professorPortalService from '../services/professorPortalService';

/**
 * Testes do historico unificado do portal (T047), com foco na correcao do bug de fuso horario:
 * datas "puras" (LocalDate do backend, ex. dataAula) nao podem passar por `new Date(...)`, que as
 * interpreta como meia-noite UTC e pode exibir o dia anterior em fusos negativos (Brasil).
 */
describe('HistoricoAlunoPortal', () => {
  const alunoId = '11111111-1111-1111-1111-111111111111';

  beforeEach(() => {
    vi.restoreAllMocks();
  });

  function renderizar(): void {
    render(
      <MemoryRouter initialEntries={[`/portal/alunos/${alunoId}/historico`]}>
        <Routes>
          <Route path="/portal/alunos/:alunoId/historico" element={<HistoricoAlunoPortal />} />
        </Routes>
      </MemoryRouter>,
    );
  }

  it('exibe a data da aula sem deslocar um dia para tras por causa do fuso horario', async () => {
    vi.spyOn(professorPortalService, 'consultarHistorico').mockResolvedValue({
      aluno: { id: alunoId, nome: 'Aluno Teste' },
      relatorios: [
        {
          tipo: 'AULA',
          id: 'r1',
          dataAula: '2026-09-07',
          periodoInicio: null,
          periodoFim: null,
          status: 'APROVADO',
          pdfUrl: null,
          aprovadoEm: null,
        },
      ],
    });

    renderizar();

    expect(await screen.findByText('Aula de 07/09/2026')).toBeInTheDocument();
  });

  it('exibe o periodo do relatorio semestral formatado dd/mm/aaaa a partir da data pura', async () => {
    vi.spyOn(professorPortalService, 'consultarHistorico').mockResolvedValue({
      aluno: { id: alunoId, nome: 'Aluno Teste' },
      relatorios: [
        {
          tipo: 'SEMESTRAL',
          id: 'r2',
          dataAula: null,
          periodoInicio: '2026-02-01',
          periodoFim: '2026-06-30',
          status: 'APROVADO',
          pdfUrl: null,
          aprovadoEm: null,
        },
      ],
    });

    renderizar();

    expect(await screen.findByText('Semestral (01/02/2026 a 30/06/2026)')).toBeInTheDocument();
  });

  it('mostra travessao quando aprovadoEm ou pdfUrl vem nulo', async () => {
    vi.spyOn(professorPortalService, 'consultarHistorico').mockResolvedValue({
      aluno: { id: alunoId, nome: 'Aluno Teste' },
      relatorios: [
        {
          tipo: 'AULA',
          id: 'r3',
          dataAula: '2026-09-07',
          periodoInicio: null,
          periodoFim: null,
          status: 'APROVADO',
          pdfUrl: null,
          aprovadoEm: null,
        },
      ],
    });

    renderizar();

    await screen.findByText('Aula de 07/09/2026');
    expect(screen.getAllByText('—')).toHaveLength(2);
  });

  it('orienta quando o aluno ainda nao tem relatorios aprovados', async () => {
    vi.spyOn(professorPortalService, 'consultarHistorico').mockResolvedValue({
      aluno: { id: alunoId, nome: 'Aluno Teste' },
      relatorios: [],
    });

    renderizar();

    expect(
      await screen.findByText('Este aluno ainda não tem relatórios aprovados.'),
    ).toBeInTheDocument();
  });

  it('mostra uma mensagem de erro quando a consulta ao historico falha', async () => {
    vi.spyOn(professorPortalService, 'consultarHistorico').mockRejectedValue(
      new Error('Sessão expirada'),
    );

    renderizar();

    expect(await screen.findByText('Sessão expirada')).toBeInTheDocument();
  });
});
