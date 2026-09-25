import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '../../services/apiClient';
import Professores from '../Professores';
import * as professorService from '../../services/professorService';

/** Testes da tela de listagem de professores do admin. */
describe('Professores', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  function renderizar(): void {
    render(
      <MemoryRouter>
        <Professores />
      </MemoryRouter>,
    );
  }

  it('lista os professores com o status de vinculacao do Telegram', async () => {
    vi.spyOn(professorService, 'listarProfessores').mockResolvedValue([
      {
        id: '1',
        nome: 'Professor Teste',
        email: 'professor@escola.local',
        vinculadoTelegram: true,
      },
      { id: '2', nome: 'Outro Professor', email: 'outro@escola.local', vinculadoTelegram: false },
    ]);

    renderizar();

    expect(await screen.findByText('Professor Teste')).toBeInTheDocument();
    expect(screen.getByText('professor@escola.local')).toBeInTheDocument();
    expect(screen.getByText('Vinculado')).toBeInTheDocument();
    expect(screen.getByText('Não vinculado')).toBeInTheDocument();
  });

  it('orienta o admin quando ainda nao ha professor cadastrado', async () => {
    vi.spyOn(professorService, 'listarProfessores').mockResolvedValue([]);

    renderizar();

    expect(await screen.findByText('Nenhum professor cadastrado ainda.')).toBeInTheDocument();
  });

  it('mostra uma mensagem de erro quando a listagem falha', async () => {
    vi.spyOn(professorService, 'listarProfessores').mockRejectedValue(new Error('falha de rede'));

    renderizar();

    expect(
      await screen.findByText('Nao foi possivel carregar a lista de professores.'),
    ).toBeInTheDocument();
  });

  it('gera um novo codigo de vinculacao e exibe o valor e a validade', async () => {
    vi.spyOn(professorService, 'listarProfessores').mockResolvedValue([
      {
        id: '1',
        nome: 'Professor Teste',
        email: 'professor@escola.local',
        vinculadoTelegram: true,
      },
    ]);
    vi.spyOn(professorService, 'reemitirCodigoVinculacao').mockResolvedValue({
      id: '1',
      nome: 'Professor Teste',
      email: 'professor@escola.local',
      codigoVinculacao: 'NOVO-CODIGO',
      codigoVinculacaoExpiraEm: '2026-10-01T12:00:00Z',
    });
    const usuario = userEvent.setup();

    renderizar();
    await screen.findByText('Professor Teste');
    await usuario.click(screen.getByRole('button', { name: 'Gerar novo código' }));

    expect(await screen.findByText('NOVO-CODIGO')).toBeInTheDocument();
    expect(professorService.reemitirCodigoVinculacao).toHaveBeenCalledWith('1');
  });

  it('mostra a mensagem de erro da API quando a reemissao de codigo falha', async () => {
    vi.spyOn(professorService, 'listarProfessores').mockResolvedValue([
      {
        id: '1',
        nome: 'Professor Teste',
        email: 'professor@escola.local',
        vinculadoTelegram: false,
      },
    ]);
    vi.spyOn(professorService, 'reemitirCodigoVinculacao').mockRejectedValue(
      new ApiError('PROFESSOR_NAO_ENCONTRADO', 'Professor nao encontrado', 404),
    );
    const usuario = userEvent.setup();

    renderizar();
    await screen.findByText('Professor Teste');
    await usuario.click(screen.getByRole('button', { name: 'Gerar novo código' }));

    expect(await screen.findByText('Professor nao encontrado')).toBeInTheDocument();
  });
});
