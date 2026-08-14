import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import HistoricoAluno from '../src/pages/HistoricoAluno';
import * as alunoService from '../src/services/alunoService';
import * as historicoService from '../src/services/historicoService';

vi.mock('../src/services/alunoService');
vi.mock('../src/services/historicoService');

describe('HistoricoAluno', () => {
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
          pdfUrl: 'http://exemplo.com/aula.pdf',
          aprovadoEm: '2026-03-11T10:00:00Z',
        },
      ],
    });

    render(<HistoricoAluno />);

    await screen.findByText('Joao Pedro');
    await userEvent.selectOptions(screen.getByLabelText(/aluno/i), 'aluno-1');

    const botaoItem = await screen.findByRole('button', { name: /relatorio de aula - 2026-03-10/i });
    await userEvent.click(botaoItem);

    expect(await screen.findByText(/abrir pdf do relatorio/i)).toBeInTheDocument();
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
