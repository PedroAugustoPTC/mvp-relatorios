import { Link } from 'react-router-dom';
import './lista-lateral.css';

/**
 * Lista lateral compacta (T075), equivalente as listas "Starting calls" / "Break" da imagem de
 * referencia: avatar com iniciais, nome e um badge a direita. Usada no painel do portal para
 * rascunhos pendentes e alunos recentes.
 */

export interface ItemListaLateral {
  id: string;
  titulo: string;
  detalhe?: string;
  badge?: string;
  para?: string;
}

export interface ListaLateralProps {
  titulo: string;
  itens: ItemListaLateral[];
  vazioMensagem: string;
}

function iniciais(texto: string): string {
  return texto
    .split(' ')
    .filter((parte) => parte.length > 0)
    .slice(0, 2)
    .map((parte) => parte[0].toUpperCase())
    .join('');
}

function ListaLateral({ titulo, itens, vazioMensagem }: ListaLateralProps): JSX.Element {
  return (
    <section className="lista-lateral">
      <h2>{titulo}</h2>

      {itens.length === 0 ? (
        <p className="lista-lateral__vazio">{vazioMensagem}</p>
      ) : (
        <ul className="lista-lateral__itens">
          {itens.map((item) => {
            const conteudo = (
              <>
                <span className="portal-avatar" aria-hidden="true">
                  {iniciais(item.titulo)}
                </span>
                <span className="lista-lateral__texto">
                  <strong>{item.titulo}</strong>
                  {item.detalhe && <span>{item.detalhe}</span>}
                </span>
                {item.badge && <span className="lista-lateral__badge">{item.badge}</span>}
              </>
            );

            return (
              <li key={item.id} className="lista-lateral__item">
                {item.para ? (
                  <Link className="lista-lateral__link" to={item.para}>
                    {conteudo}
                  </Link>
                ) : (
                  <div className="lista-lateral__link">{conteudo}</div>
                )}
              </li>
            );
          })}
        </ul>
      )}
    </section>
  );
}

export default ListaLateral;
