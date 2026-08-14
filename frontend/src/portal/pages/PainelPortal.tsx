import { useEffect, useState } from 'react';
import CartaoEstatistica from '../components/CartaoEstatistica';
import ListaLateral, { ItemListaLateral } from '../components/ListaLateral';
import { useSessaoProfessor } from '../hooks/useSessaoProfessor';
import { AlunoResumo, listarAlunos } from '../services/professorPortalService';
import './painel-portal.css';

/**
 * Painel inicial do portal (T075), reproduzindo a composicao da imagem de referencia: area de
 * estatisticas no topo com card de destaque em gradiente e listas laterais a direita — com o
 * conteudo adaptado ao dominio (alunos e relatorios do professor).
 */
function PainelPortal(): JSX.Element {
  const { professor } = useSessaoProfessor();
  const [alunos, setAlunos] = useState<AlunoResumo[]>([]);
  const [erro, setErro] = useState<string | null>(null);

  useEffect(() => {
    let ativo = true;
    listarAlunos()
      .then((lista) => {
        if (ativo) {
          setAlunos(lista);
        }
      })
      .catch((e: unknown) => {
        if (ativo) {
          setErro(e instanceof Error ? e.message : 'Não foi possível carregar seus alunos.');
        }
      });
    return () => {
      ativo = false;
    };
  }, []);

  const alunosRecentes: ItemListaLateral[] = alunos.slice(0, 5).map((aluno) => ({
    id: aluno.id,
    titulo: aluno.nome,
    detalhe: 'Registrar aula ou ver histórico',
    para: `/portal/alunos/${aluno.id}/novo-relatorio`,
  }));

  return (
    <section>
      <h1>Olá, {professor?.nome ?? 'professor'}</h1>
      <p>Registre uma aula por áudio ou gere o relatório semestral de um aluno.</p>

      {erro && (
        <p className="portal-erro" role="alert">
          {erro}
        </p>
      )}

      <div className="painel-grade">
        <div className="painel-estatisticas">
          <CartaoEstatistica valor={alunos.length} rotulo="Alunos associados a você" destaque />
          <CartaoEstatistica
            valor="Áudio"
            rotulo="Grave ou envie o áudio da aula e o relatório é montado para você"
          />
        </div>

        <ListaLateral
          titulo="Seus alunos"
          itens={alunosRecentes}
          vazioMensagem="Você ainda não tem alunos associados. Fale com a secretaria da escola."
        />
      </div>
    </section>
  );
}

export default PainelPortal;
