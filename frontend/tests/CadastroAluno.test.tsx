import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import CadastroAluno from '../src/pages/CadastroAluno';
import * as alunoService from '../src/services/alunoService';
import * as professorService from '../src/services/professorService';

vi.mock('../src/services/alunoService', async () => {
  const real = await vi.importActual<typeof import('../src/services/alunoService')>(
    '../src/services/alunoService',
  );
  return { ...real, cadastrarAluno: vi.fn() };
});
vi.mock('../src/services/professorService');

describe('CadastroAluno', () => {
  it('nao exige nome do responsavel para aluno maior de idade', async () => {
    vi.mocked(professorService.listarProfessores).mockResolvedValue([]);
    vi.mocked(alunoService.cadastrarAluno).mockResolvedValue({
      id: 'uuid-aluno',
      nome: 'Joao Adulto',
      dataNascimento: '1990-01-01',
      nomeResponsavel: null,
      menorDeIdade: false,
    });

    render(<CadastroAluno />);

    await userEvent.type(screen.getByLabelText(/nome/i), 'Joao Adulto');
    await userEvent.type(screen.getByLabelText(/data de nascimento/i), '1990-01-01');
    await userEvent.type(screen.getByLabelText(/cpf/i), '11122233344');

    expect(screen.queryByLabelText(/nome do responsavel/i)).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: /cadastrar/i }));

    expect(await screen.findByText(/cadastrado com sucesso/i)).toBeInTheDocument();
  });

  it('exige nome do responsavel quando o aluno e menor de idade', async () => {
    vi.mocked(professorService.listarProfessores).mockResolvedValue([]);

    const anoMenor = new Date().getFullYear() - 10;
    render(<CadastroAluno />);

    await userEvent.type(screen.getByLabelText(/nome/i), 'Crianca');
    await userEvent.type(screen.getByLabelText(/data de nascimento/i), `${anoMenor}-01-01`);
    await userEvent.type(screen.getByLabelText(/cpf/i), '55566677788');

    expect(screen.getByLabelText(/nome do responsavel/i)).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: /cadastrar/i }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      /nome do responsavel e obrigatorio/i,
    );
    expect(alunoService.cadastrarAluno).not.toHaveBeenCalled();
  });
});
