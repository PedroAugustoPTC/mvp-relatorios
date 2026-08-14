import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import Login from '../src/pages/Login';
import { ApiError } from '../src/services/apiClient';
import * as authService from '../src/services/authService';

vi.mock('../src/services/authService');

describe('Login', () => {
  it('renderiza os campos do formulario', () => {
    render(
      <MemoryRouter>
        <Login />
      </MemoryRouter>,
    );

    expect(screen.getByLabelText(/e-mail/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/senha/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /entrar/i })).toBeInTheDocument();
  });

  it('envia as credenciais informadas ao efetuar login', async () => {
    vi.mocked(authService.login).mockResolvedValue({
      token: 'jwt-fake',
      expiraEm: '2026-08-13T20:00:00Z',
    });

    render(
      <MemoryRouter>
        <Login />
      </MemoryRouter>,
    );

    await userEvent.type(screen.getByLabelText(/e-mail/i), 'admin@escola.com');
    await userEvent.type(screen.getByLabelText(/senha/i), 'senha123');
    await userEvent.click(screen.getByRole('button', { name: /entrar/i }));

    expect(authService.login).toHaveBeenCalledWith({
      email: 'admin@escola.com',
      senha: 'senha123',
    });
  });

  it('exibe mensagem de erro quando as credenciais sao invalidas', async () => {
    vi.mocked(authService.login).mockRejectedValue(
      new ApiError('CREDENCIAIS_INVALIDAS', 'E-mail ou senha invalidos.', 401),
    );

    render(
      <MemoryRouter>
        <Login />
      </MemoryRouter>,
    );

    await userEvent.type(screen.getByLabelText(/e-mail/i), 'admin@escola.com');
    await userEvent.type(screen.getByLabelText(/senha/i), 'errada');
    await userEvent.click(screen.getByRole('button', { name: /entrar/i }));

    expect(await screen.findByRole('alert')).toHaveTextContent('E-mail ou senha invalidos.');
  });
});
