import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import SessaoProfessorProvider from '../components/SessaoProfessorProvider';
import PortalLayout from '../layout/PortalLayout';
import * as professorApiClient from '../services/professorApiClient';

/**
 * Testes do shell do portal (T079): navegacao com rotulos acessiveis, item ativo anunciado e busca
 * que so navega no submit.
 */
describe('PortalLayout', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    localStorage.setItem('escolaMusica.portalToken', 'token-de-teste');
    vi.spyOn(professorApiClient.professorApiClient, 'get').mockResolvedValue({
      id: '1',
      nome: 'Ana Souza',
      sessaoExpiraEm: new Date(Date.now() + 3600_000).toISOString(),
    });
  });

  function renderizar(): void {
    render(
      <MemoryRouter initialEntries={['/portal']}>
        <SessaoProfessorProvider>
          <Routes>
            <Route path="/portal" element={<PortalLayout />}>
              <Route index element={<p>Conteúdo do painel</p>} />
            </Route>
            <Route path="/portal/alunos" element={<p>Lista de alunos</p>} />
          </Routes>
        </SessaoProfessorProvider>
      </MemoryRouter>,
    );
  }

  it('expoe a navegacao e os botoes de icone com nomes acessiveis', async () => {
    renderizar();

    expect(screen.getByRole('navigation', { name: /navegação principal/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Painel' })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByRole('button', { name: 'Alunos' })).not.toHaveAttribute('aria-current');
    expect(screen.getByRole('button', { name: /encerrar sessão/i })).toBeInTheDocument();
  });

  it('mostra o professor autenticado no header', async () => {
    renderizar();

    expect(await screen.findByText('Ana Souza')).toBeInTheDocument();
  });

  it('so navega na busca quando o professor confirma, e nao a cada tecla', async () => {
    renderizar();

    const campo = screen.getByLabelText(/buscar aluno/i);
    await userEvent.type(campo, 'Arthur');

    // Ainda na mesma pagina depois de digitar.
    expect(screen.getByText('Conteúdo do painel')).toBeInTheDocument();

    await userEvent.type(campo, '{Enter}');

    expect(await screen.findByText('Lista de alunos')).toBeInTheDocument();
  });
});
