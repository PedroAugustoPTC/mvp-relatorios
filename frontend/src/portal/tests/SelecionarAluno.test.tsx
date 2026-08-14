import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import SelecionarAluno from '../pages/SelecionarAluno';
import * as professorPortalService from '../services/professorPortalService';

/** Testes da selecao de aluno do portal (T049). */
describe('SelecionarAluno', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  function renderizar(rota = '/portal/alunos'): void {
    render(
      <MemoryRouter initialEntries={[rota]}>
        <SelecionarAluno />
      </MemoryRouter>,
    );
  }

  it('lista os alunos do professor com as acoes disponiveis', async () => {
    vi.spyOn(professorPortalService, 'listarAlunos').mockResolvedValue([
      { id: '11111111-1111-1111-1111-111111111111', nome: 'Arthur Caldas' },
    ]);

    renderizar();

    expect(await screen.findByText('Arthur Caldas')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Novo relatório de aula' })).toHaveAttribute(
      'href',
      '/portal/alunos/11111111-1111-1111-1111-111111111111/novo-relatorio',
    );
    expect(screen.getByRole('link', { name: 'Histórico' })).toBeInTheDocument();
  });

  it('filtra os alunos pelo termo de busca vindo da URL', async () => {
    vi.spyOn(professorPortalService, 'listarAlunos').mockResolvedValue([
      { id: '1', nome: 'Arthur Caldas' },
      { id: '2', nome: 'Beatriz Lima' },
    ]);

    renderizar('/portal/alunos?busca=beatriz');

    expect(await screen.findByText('Beatriz Lima')).toBeInTheDocument();
    expect(screen.queryByText('Arthur Caldas')).not.toBeInTheDocument();
  });

  it('orienta o professor quando ele ainda nao tem alunos associados', async () => {
    vi.spyOn(professorPortalService, 'listarAlunos').mockResolvedValue([]);

    renderizar();

    expect(await screen.findByText(/ainda não tem alunos associados/i)).toBeInTheDocument();
  });

  it('mostra uma mensagem de erro quando a listagem falha', async () => {
    vi.spyOn(professorPortalService, 'listarAlunos').mockRejectedValue(
      new Error('Sessão expirada'),
    );

    renderizar();

    expect(await screen.findByText('Sessão expirada')).toBeInTheDocument();
  });
});
