import './cartao-estatistica.css';

/**
 * Card de destaque com metrica agregada (T075), equivalente ao card inferior direito da imagem de
 * referencia ("+278k Outsourced employees"): mesmo gradiente roxo->laranja e mesma tipografia
 * grande, com o conteudo adaptado ao dominio (relatorios do professor).
 */
export interface CartaoEstatisticaProps {
  valor: string | number;
  rotulo: string;
  destaque?: boolean;
}

function CartaoEstatistica({ valor, rotulo, destaque = false }: CartaoEstatisticaProps): JSX.Element {
  return (
    <article className={`cartao-estatistica${destaque ? ' cartao-estatistica--destaque' : ''}`}>
      <strong className="cartao-estatistica__valor">{valor}</strong>
      <span className="cartao-estatistica__rotulo">{rotulo}</span>
    </article>
  );
}

export default CartaoEstatistica;
