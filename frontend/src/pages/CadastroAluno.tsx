import { ChangeEvent, FormEvent, useEffect, useState } from 'react';
import { ApiError } from '../services/apiClient';
import { Aluno, cadastrarAluno, isMenorDeIdade } from '../services/alunoService';
import { listarProfessores, ProfessorListaItem } from '../services/professorService';

/** Cadastro de aluno pela interface administrativa (T070, FR-001/FR-001a). */
function CadastroAluno(): JSX.Element {
  const [nome, setNome] = useState('');
  const [dataNascimento, setDataNascimento] = useState('');
  const [cpf, setCpf] = useState('');
  const [nomeResponsavel, setNomeResponsavel] = useState('');
  const [professorIds, setProfessorIds] = useState<string[]>([]);
  const [professores, setProfessores] = useState<ProfessorListaItem[]>([]);
  const [erro, setErro] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);
  const [alunoCriado, setAlunoCriado] = useState<Aluno | null>(null);

  const menorDeIdade = isMenorDeIdade(dataNascimento);

  useEffect(() => {
    listarProfessores()
      .then(setProfessores)
      .catch(() => setProfessores([]));
  }, []);

  function handleProfessoresChange(event: ChangeEvent<HTMLSelectElement>): void {
    const selecionados = Array.from(event.target.selectedOptions).map((option) => option.value);
    setProfessorIds(selecionados);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault();
    setErro(null);

    if (menorDeIdade && !nomeResponsavel.trim()) {
      setErro('Nome do responsavel e obrigatorio para alunos menores de idade.');
      return;
    }

    setEnviando(true);
    try {
      const aluno = await cadastrarAluno({
        nome,
        dataNascimento,
        cpf,
        nomeResponsavel: menorDeIdade ? nomeResponsavel : nomeResponsavel || undefined,
        professorIds,
      });
      setAlunoCriado(aluno);
      setNome('');
      setDataNascimento('');
      setCpf('');
      setNomeResponsavel('');
      setProfessorIds([]);
    } catch (erroCapturado) {
      if (erroCapturado instanceof ApiError) {
        setErro(erroCapturado.message);
      } else {
        setErro('Nao foi possivel cadastrar o aluno. Tente novamente.');
      }
    } finally {
      setEnviando(false);
    }
  }

  return (
    <main>
      <h1>Cadastrar aluno</h1>
      <form onSubmit={handleSubmit}>
        <div>
          <label htmlFor="nome">Nome</label>
          <input
            id="nome"
            name="nome"
            type="text"
            required
            value={nome}
            onChange={(event) => setNome(event.target.value)}
          />
        </div>
        <div>
          <label htmlFor="dataNascimento">Data de nascimento</label>
          <input
            id="dataNascimento"
            name="dataNascimento"
            type="date"
            required
            value={dataNascimento}
            onChange={(event) => setDataNascimento(event.target.value)}
          />
        </div>
        <div>
          <label htmlFor="cpf">CPF</label>
          <input
            id="cpf"
            name="cpf"
            type="text"
            required
            value={cpf}
            onChange={(event) => setCpf(event.target.value)}
          />
        </div>
        {menorDeIdade && (
          <div>
            <label htmlFor="nomeResponsavel">Nome do responsavel</label>
            <input
              id="nomeResponsavel"
              name="nomeResponsavel"
              type="text"
              value={nomeResponsavel}
              onChange={(event) => setNomeResponsavel(event.target.value)}
            />
          </div>
        )}
        <div>
          <label htmlFor="professorIds">Professores</label>
          <select
            id="professorIds"
            name="professorIds"
            multiple
            value={professorIds}
            onChange={handleProfessoresChange}
          >
            {professores.map((professor) => (
              <option key={professor.id} value={professor.id}>
                {professor.nome}
              </option>
            ))}
          </select>
        </div>
        {erro && <p role="alert">{erro}</p>}
        <button type="submit" disabled={enviando}>
          {enviando ? 'Cadastrando...' : 'Cadastrar'}
        </button>
      </form>

      {alunoCriado && (
        <section aria-label="Aluno cadastrado">
          <h2>Aluno cadastrado com sucesso</h2>
          <p>{alunoCriado.nome}</p>
        </section>
      )}
    </main>
  );
}

export default CadastroAluno;
