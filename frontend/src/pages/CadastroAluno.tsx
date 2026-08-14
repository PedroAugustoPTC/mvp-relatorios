import { ChangeEvent, FormEvent, useEffect, useState } from 'react';
import { ApiError } from '../services/apiClient';
import { Aluno, cadastrarAluno, isMenorDeIdade } from '../services/alunoService';
import { listarProfessores, ProfessorListaItem } from '../services/professorService';
import './cadastro-aluno.css';

/** Iniciais para o avatar da confirmacao, no mesmo formato dos avatares do portal. */
function iniciais(nome: string): string {
  return nome
    .split(' ')
    .filter((parte) => parte.length > 0)
    .slice(0, 2)
    .map((parte) => parte[0].toUpperCase())
    .join('');
}

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
    <section>
      <h1>Cadastrar aluno</h1>
      <p>Escolha os professores que acompanham o aluno para que ele apareça no portal deles.</p>

      <form className="adm-cartao adm-formulario adm-cadastro" onSubmit={handleSubmit}>
        {erro && (
          <p className="adm-alerta" role="alert">
            {erro}
          </p>
        )}

        <div className="adm-campo">
          <label className="adm-rotulo" htmlFor="nome">
            Nome
          </label>
          <input
            className="adm-entrada"
            id="nome"
            name="nome"
            type="text"
            required
            value={nome}
            onChange={(event) => setNome(event.target.value)}
          />
        </div>
        <div className="adm-campo">
          <label className="adm-rotulo" htmlFor="dataNascimento">
            Data de nascimento
          </label>
          <input
            className="adm-entrada"
            id="dataNascimento"
            name="dataNascimento"
            type="date"
            required
            value={dataNascimento}
            onChange={(event) => setDataNascimento(event.target.value)}
          />
        </div>
        <div className="adm-campo">
          <label className="adm-rotulo" htmlFor="cpf">
            CPF
          </label>
          <input
            className="adm-entrada"
            id="cpf"
            name="cpf"
            type="text"
            required
            value={cpf}
            onChange={(event) => setCpf(event.target.value)}
          />
        </div>
        {menorDeIdade && (
          <div className="adm-campo">
            <label className="adm-rotulo" htmlFor="nomeResponsavel">
              Nome do responsavel
            </label>
            <input
              className="adm-entrada"
              id="nomeResponsavel"
              name="nomeResponsavel"
              type="text"
              value={nomeResponsavel}
              onChange={(event) => setNomeResponsavel(event.target.value)}
            />
            <p className="adm-ajuda">Obrigatório para alunos menores de 18 anos.</p>
          </div>
        )}
        <div className="adm-campo">
          <label className="adm-rotulo" htmlFor="professorIds">
            Professores
          </label>
          <select
            className="adm-entrada"
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
          <p className="adm-ajuda">Segure Ctrl (ou Cmd) para escolher mais de um professor.</p>
        </div>
        <button className="adm-botao" type="submit" disabled={enviando}>
          {enviando ? 'Cadastrando...' : 'Cadastrar'}
        </button>
      </form>

      {alunoCriado && (
        <section className="adm-cartao adm-confirmacao" aria-label="Aluno cadastrado">
          <span className="adm-avatar" aria-hidden="true">
            {iniciais(alunoCriado.nome)}
          </span>
          <div>
            <h2>Aluno cadastrado com sucesso</h2>
            <p>{alunoCriado.nome}</p>
          </div>
        </section>
      )}
    </section>
  );
}

export default CadastroAluno;
