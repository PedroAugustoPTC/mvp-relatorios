import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import SessaoProfessorProvider from '../components/SessaoProfessorProvider';
import VincularCodigo from '../pages/VincularCodigo';
import { ApiError } from '../services/professorApiClient';
import * as professorAuthService from '../services/professorAuthService';

/**
 * Testes da tela de entrada do portal (T061): sucesso, codigo invalido e bloqueio temporario.
 * O foco esta nas mensagens de erro — elas precisam dizer ao professor o que fazer a seguir.
 */
describe('VincularCodigo', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    localStorage.clear();
  });

  function renderizar(): void {
    render(
      <MemoryRouter initialEntries={['/portal/entrar']}>
        <SessaoProfessorProvider>
          <Routes>
            <Route path="/portal/entrar" element={<VincularCodigo />} />
            <Route path="/portal" element={<p>Painel do portal</p>} />
          </Routes>
        </SessaoProfessorProvider>
      </MemoryRouter>,
    );
  }

  async function informarCodigo(codigo = 'ABC123'): Promise<void> {
    await userEvent.type(screen.getByLabelText(/código de vinculação/i), codigo);
    await userEvent.click(screen.getByRole('button', { name: /entrar/i }));
  }

  it('autentica e leva ao painel quando o codigo e valido', async () => {
    vi.spyOn(professorAuthService, 'vincularPorCodigo').mockResolvedValue({
      token: 'jwt-do-professor',
      expiraEm: new Date(Date.now() + 3600_000).toISOString(),
      professor: { id: '1', nome: 'Ana Souza' },
    });

    renderizar();
    await informarCodigo();

    expect(await screen.findByText('Painel do portal')).toBeInTheDocument();
    expect(professorAuthService.vincularPorCodigo).toHaveBeenCalledWith('ABC123');
  });

  it('orienta a pedir um novo codigo quando ele e invalido ou expirou', async () => {
    vi.spyOn(professorAuthService, 'vincularPorCodigo').mockRejectedValue(
      new ApiError('CODIGO_INVALIDO', 'Código inválido', 401),
    );

    renderizar();
    await informarCodigo('ERRADO');

    expect(await screen.findByRole('alert')).toHaveTextContent(
      /peça um novo código à secretaria/i,
    );
    expect(screen.queryByText('Painel do portal')).not.toBeInTheDocument();
  });

  it('informa o tempo de espera quando as tentativas foram bloqueadas', async () => {
    vi.spyOn(professorAuthService, 'vincularPorCodigo').mockRejectedValue(
      new ApiError('BLOQUEADO_TEMPORARIAMENTE', 'Muitas tentativas', 429, 900),
    );

    renderizar();
    await informarCodigo('ERRADO');

    expect(await screen.findByRole('alert')).toHaveTextContent(/tente novamente em 15 minutos/i);
  });

  it('mostra uma mensagem generica util quando a rede falha', async () => {
    vi.spyOn(professorAuthService, 'vincularPorCodigo').mockRejectedValue(new TypeError('offline'));

    renderizar();
    await informarCodigo();

    expect(await screen.findByRole('alert')).toHaveTextContent(/verifique sua conexão/i);
  });
});
