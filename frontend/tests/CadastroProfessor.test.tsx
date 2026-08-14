import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import CadastroProfessor from '../src/pages/CadastroProfessor';
import { ApiError } from '../src/services/apiClient';
import * as professorService from '../src/services/professorService';

vi.mock('../src/services/professorService');

describe('CadastroProfessor', () => {
  it('cadastra um professor e exibe o codigo de vinculacao gerado', async () => {
    vi.mocked(professorService.cadastrarProfessor).mockResolvedValue({
      id: 'uuid-1',
      nome: 'Maria Silva',
      email: 'maria@escola.com',
      codigoVinculacao: 'AB12CD34',
      codigoVinculacaoExpiraEm: '2026-08-14T00:00:00Z',
    });

    render(<CadastroProfessor />);

    await userEvent.type(screen.getByLabelText(/nome/i), 'Maria Silva');
    await userEvent.type(screen.getByLabelText(/e-mail/i), 'maria@escola.com');
    await userEvent.click(screen.getByRole('button', { name: /cadastrar/i }));

    expect(await screen.findByText('AB12CD34')).toBeInTheDocument();
    expect(professorService.cadastrarProfessor).toHaveBeenCalledWith({
      nome: 'Maria Silva',
      email: 'maria@escola.com',
    });
  });

  it('exibe mensagem de erro quando o e-mail ja esta cadastrado', async () => {
    vi.mocked(professorService.cadastrarProfessor).mockRejectedValue(
      new ApiError('PROFESSOR_EMAIL_DUPLICADO', 'Ja existe um professor com este e-mail.', 409),
    );

    render(<CadastroProfessor />);

    await userEvent.type(screen.getByLabelText(/nome/i), 'Maria Silva');
    await userEvent.type(screen.getByLabelText(/e-mail/i), 'maria@escola.com');
    await userEvent.click(screen.getByRole('button', { name: /cadastrar/i }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Ja existe um professor com este e-mail.',
    );
  });
});
