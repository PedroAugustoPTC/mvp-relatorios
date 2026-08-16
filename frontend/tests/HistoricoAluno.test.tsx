import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeAll, describe, expect, it, vi } from 'vitest';
import HistoricoAluno from '../src/pages/HistoricoAluno';
import * as alunoService from '../src/services/alunoService';
import { apiClient } from '../src/services/apiClient';
import * as historicoService from '../src/services/historicoService';

vi.mock('../src/services/alunoService');
vi.mock('../src/services/historicoService');

describe('HistoricoAluno', () => {
  // O PDF e buscado por fetch autenticado (a rota exige Authorization, que um iframe nao envia) e
  // exibido a partir de uma blob: URL. O jsdom nao implementa a API de object URL.
  beforeAll(() => {
    Object.assign(URL, {
      createObjectURL: vi.fn(() => 'blob:fake-pdf'),
      revokeObjectURL: vi.fn(),
    });
  });

  it('lista alunos e exibe o historico apos selecao', async () => {
    vi.mocked(alunoService.listarAlunos).mockResolvedValue([
      {
        id: 'aluno-1',
        nome: 'Joao Pedro',
        dataNascimento: '2010-01-01',
        nomeResponsavel: 'Ana Pedro',
        menorDeIdade: true,
      },
    ]);
    vi.mocked(historicoService.consultarHistorico).mockResolvedValue({
      aluno: { id: 'aluno-1', nome: 'Joao Pedro' },
      relatorios: [
        {
          tipo: 'AULA',
          id: 'rel-1',
          dataAula: '2026-03-10',
          periodoInicio: null,
          periodoFim: null,
          status: 'APROVADO',
          pdfUrl: 'http://exemplo.com/aula.pdf',
          aprovadoEm: '2026-03-11T10:00:00Z',
        },
        {
          tipo: 'SEMESTRAL',
          id: 'rel-2',
          dataAula: null,
          periodoInicio: '2026-01-01',
          periodoFim: '2026-06-30',
          status: 'APROVADO',
          pdfUrl: 'http://exemplo.com/semestral.pdf',
          aprovadoEm: '2026-07-01T10:00:00Z',
        },
      ],
    });

    render(<HistoricoAluno />);

    await screen.findByText('Joao Pedro');

    await userEvent.selectOptions(screen.getByLabelText(/aluno/i), 'aluno-1');

    expect(await screen.findByText(/relatorios de joao pedro/i)).toBeInTheDocument();
    expect(screen.getByText(/relatorio de aula - 2026-03-10/i)).toBeInTheDocument();
    expect(screen.getByText(/relatorio semestral - 2026-01-01 a 2026-06-30/i)).toBeInTheDocument();
  });

  it('exibe detalhe e PDF ao clicar em um item do historico', async () => {
    vi.mocked(alunoService.listarAlunos).mockResolvedValue([
      {
        id: 'aluno-1',
        nome: 'Joao Pedro',
        dataNascimento: '2010-01-01',
        nomeResponsavel: 'Ana Pedro',
        menorDeIdade: true,
      },
    ]);
    vi.mocked(historicoService.consultarHistorico).mockResolvedValue({
      aluno: { id: 'aluno-1', nome: 'Joao Pedro' },
      relatorios: [
        {
          tipo: 'AULA',
          id: 'rel-1',
          dataAula: '2026-03-10',
          periodoInicio: null,
          periodoFim: null,
          status: 'APROVADO',
          pdfUrl: '/api/v1/arquivos/relatorios/relatorio-aula-rel-1-v1.pdf',
          aprovadoEm: '2026-03-11T10:00:00Z',
        },
      ],
    });

    const baixarArquivo = vi
      .spyOn(apiClient, 'baixarArquivo')
      .mockResolvedValue(new Blob(['%PDF-1.4'], { type: 'application/pdf' }));

    render(<HistoricoAluno />);

    await screen.findByText('Joao Pedro');
    await userEvent.selectOptions(screen.getByLabelText(/aluno/i), 'aluno-1');

    const botaoItem = await screen.findByRole('button', { name: /relatorio de aula - 2026-03-10/i });
    await userEvent.click(botaoItem);

    expect(await screen.findByRole('link', { name: /abrir pdf do relatorio/i })).toHaveAttribute(
      'href',
      'blob:fake-pdf',
    );
    expect(baixarArquivo).toHaveBeenCalledWith(
      '/api/v1/arquivos/relatorios/relatorio-aula-rel-1-v1.pdf',
    );
  });

  it('exibe mensagem quando o historico esta vazio', async () => {
    vi.mocked(alunoService.listarAlunos).mockResolvedValue([
      {
        id: 'aluno-1',
        nome: 'Joao Pedro',
        dataNascimento: '2010-01-01',
        nomeResponsavel: 'Ana Pedro',
        menorDeIdade: true,
      },
    ]);
    vi.mocked(historicoService.consultarHistorico).mockResolvedValue({
      aluno: { id: 'aluno-1', nome: 'Joao Pedro' },
      relatorios: [],
    });

    render(<HistoricoAluno />);

    await screen.findByText('Joao Pedro');
    await userEvent.selectOptions(screen.getByLabelText(/aluno/i), 'aluno-1');

    expect(
      await screen.findByText(/nenhum relatorio aprovado encontrado/i),
    ).toBeInTheDocument();
  });
});
